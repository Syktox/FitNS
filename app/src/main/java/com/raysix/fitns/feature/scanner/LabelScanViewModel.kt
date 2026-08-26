package com.raysix.fitns.feature.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.Micronutrients
import com.raysix.fitns.domain.usecase.LabelNutritionParser
import com.raysix.fitns.feature.nutrition.ManualFoodInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class LabelScanPhase { Capture, Review }

data class LabelScanUiState(
    val phase: LabelScanPhase = LabelScanPhase.Capture,
    val previewBitmap: ImageBitmap? = null,
    val rawText: String = "",
    val name: String = "",
    val basisGrams: String = "100",
    val perPortion: Boolean = false,
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val sugar: String = "",
    val fat: String = "",
    val saturatedFat: String = "",
    val fiber: String = "",
    val salt: String = "",
    val sodium: String = "",
    val micronutrients: Micronutrients = Micronutrients(),
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null,
    val loading: Boolean = false
)

@HiltViewModel
class LabelScanViewModel @Inject constructor(
    private val parser: LabelNutritionParser
) : ViewModel() {

    private val state = MutableStateFlow(LabelScanUiState())
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val uiState: StateFlow<LabelScanUiState> = state

    override fun onCleared() {
        recognizer.close()
    }

    fun onImageCaptured(bytes: ByteArray) {
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, errorMessage = null)
            val bitmap = withContext(Dispatchers.Default) { decodeScaled(bytes) }
            if (bitmap == null) {
                state.value = state.value.copy(loading = false, errorMessage = "The captured image could not be read.")
                return@launch
            }
            state.value = state.value.copy(previewBitmap = bitmap.asImageBitmap())

            val recognized = try {
                withContext(Dispatchers.Default) {
                    recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }

            if (recognized.isNullOrBlank()) {
                state.value = state.value.copy(
                    loading = false,
                    errorMessage = "No readable text was found. Retake the photo with a clearly visible nutrition table."
                )
                return@launch
            }

            val result = parser.parse(recognized)
            state.value = LabelScanUiState(
                phase = LabelScanPhase.Review,
                previewBitmap = state.value.previewBitmap,
                rawText = recognized,
                name = result.detectedName.orEmpty(),
                basisGrams = if (result.perPortion) "" else "100",
                perPortion = result.perPortion,
                calories = result.nutrition.caloriesKcal.formatPlain(),
                protein = result.nutrition.proteinGrams.formatPlain(),
                carbs = result.nutrition.carbohydratesGrams.formatPlain(),
                sugar = result.nutrition.sugarGrams.formatPlain(),
                fat = result.nutrition.fatGrams.formatPlain(),
                saturatedFat = result.nutrition.saturatedFatGrams.formatPlain(),
                fiber = result.nutrition.fiberGrams.formatPlain(),
                salt = result.nutrition.saltGrams.formatPlain(),
                sodium = result.nutrition.sodiumMilligrams?.formatPlain().orEmpty(),
                micronutrients = result.micronutrients,
                warnings = result.warnings
            )
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value) }
    fun onBasisGramsChange(value: String) = state.update { it.copy(basisGrams = value) }
    fun onCaloriesChange(value: String) = state.update { it.copy(calories = value) }
    fun onProteinChange(value: String) = state.update { it.copy(protein = value) }
    fun onCarbsChange(value: String) = state.update { it.copy(carbs = value) }
    fun onSugarChange(value: String) = state.update { it.copy(sugar = value) }
    fun onFatChange(value: String) = state.update { it.copy(fat = value) }
    fun onSaturatedFatChange(value: String) = state.update { it.copy(saturatedFat = value) }
    fun onFiberChange(value: String) = state.update { it.copy(fiber = value) }
    fun onSaltChange(value: String) = state.update { it.copy(salt = value) }
    fun onSodiumChange(value: String) = state.update { it.copy(sodium = value) }

    fun retake() {
        state.value = LabelScanUiState()
    }

    fun apply(onApplied: (ManualFoodInput) -> Unit) {
        val snapshot = state.value
        val grams = snapshot.basisGrams.toUserDecimalOrNull() ?: 100.0
        val input = ManualFoodInput(
            name = snapshot.name.ifBlank { "Scanned product" },
            brand = null,
            grams = grams,
            calories = snapshot.calories.toUserDecimalOrNull() ?: 0.0,
            protein = snapshot.protein.toUserDecimalOrNull() ?: 0.0,
            carbohydrates = snapshot.carbs.toUserDecimalOrNull() ?: 0.0,
            sugar = snapshot.sugar.toUserDecimalOrNull() ?: 0.0,
            fat = snapshot.fat.toUserDecimalOrNull() ?: 0.0,
            saturatedFat = snapshot.saturatedFat.toUserDecimalOrNull() ?: 0.0,
            fiber = snapshot.fiber.toUserDecimalOrNull() ?: 0.0,
            salt = snapshot.salt.toUserDecimalOrNull() ?: 0.0,
            sodiumMilligrams = snapshot.sodium.toUserDecimalOrNull(),
            mealType = MealType.Snack,
            notes = buildString {
                append("Values from nutrition label OCR.")
                if (snapshot.perPortion) append(" Table is per portion.")
            },
            micronutrients = snapshot.micronutrients,
            dataQuality = DataQuality.Estimated
        )
        onApplied(input)
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        var sampleSize = 1
        val maxDimension = 2560
        while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener {
            if (continuation.isActive) continuation.resume(it)
        }
        addOnFailureListener { e ->
            if (continuation.isActive) {
                if (e is CancellationException) continuation.cancel(e) else continuation.resumeWithException(e)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
}

private fun MutableStateFlow<LabelScanUiState>.update(transform: (LabelScanUiState) -> LabelScanUiState) {
    value = transform(value)
}

private fun Double.formatPlain(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
