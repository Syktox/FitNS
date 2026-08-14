package com.raysix.fitns.feature.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.MealAnalysisResult
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.NutritionFacts
import com.raysix.fitns.domain.repository.N8nRepository
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class MealAnalysisPhase { Idle, Analyzing, Review, Saving }

data class EditableMealItem(
    val id: String,
    val name: String,
    val grams: String,
    val confidence: Double,
    val calories: String,
    val protein: String,
    val carbs: String,
    val fat: String
)

data class MealAnalysisUiState(
    val phase: MealAnalysisPhase = MealAnalysisPhase.Idle,
    val previewBitmap: ImageBitmap? = null,
    val consentGranted: Boolean = false,
    val mealType: MealType = MealType.Snack,
    val items: List<EditableMealItem> = emptyList(),
    val disclaimer: String? = null,
    val errorMessage: String? = null,
    val loading: Boolean = false
)

@HiltViewModel
class MealAnalysisViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val n8nRepository: N8nRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val state = MutableStateFlow(MealAnalysisUiState())
    private var pendingBase64: String? = null

    val uiState: StateFlow<MealAnalysisUiState> = state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MealAnalysisUiState()
    )

    fun onImageCaptured(bytes: ByteArray) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) { decodeScaled(bytes) } ?: return@launch
            pendingBase64 = withContext(Dispatchers.Default) { compressToBase64(bitmap) }
            state.value = state.value.copy(
                previewBitmap = bitmap.asImageBitmap(),
                errorMessage = null,
                phase = MealAnalysisPhase.Idle
            )
        }
    }

    fun onConsentChange(granted: Boolean) {
        state.value = state.value.copy(consentGranted = granted)
    }

    fun onMealTypeChange(mealType: MealType) {
        state.value = state.value.copy(mealType = mealType)
    }

    fun updateItem(id: String, grams: String, calories: String, protein: String, carbs: String, fat: String) {
        state.value = state.value.copy(
            items = state.value.items.map { item ->
                if (item.id == id) {
                    item.copy(grams = grams, calories = calories, protein = protein, carbs = carbs, fat = fat)
                } else {
                    item
                }
            }
        )
    }

    fun removeItem(id: String) {
        state.value = state.value.copy(items = state.value.items.filterNot { it.id == id })
    }

    fun analyze() {
        val snapshot = state.value
        if (!snapshot.consentGranted) {
            state.value = snapshot.copy(errorMessage = "Confirm consent before uploading the photo.")
            return
        }
        val base64 = pendingBase64
        if (base64.isNullOrBlank()) {
            state.value = snapshot.copy(errorMessage = "Capture a photo of the meal first.")
            return
        }
        state.value = snapshot.copy(loading = true, errorMessage = null, phase = MealAnalysisPhase.Analyzing)

        viewModelScope.launch {
            val settings = settingsRepository.observeN8nSettings().first()
            val token = settingsRepository.readBearerToken()
            val result = n8nRepository.analyzeMealImage(
                baseUrl = settings.baseUrl,
                bearerToken = token,
                imageBase64 = base64,
                consentGranted = true
            )
            state.value = when (result) {
                is AppResult.Success -> MealAnalysisUiState(
                    phase = MealAnalysisPhase.Review,
                    previewBitmap = state.value.previewBitmap,
                    consentGranted = true,
                    mealType = state.value.mealType,
                    items = result.value.items.mapIndexed { index, item ->
                        EditableMealItem(
                            id = "item-$index",
                            name = item.name,
                            grams = item.estimatedGrams.formatPlain(),
                            confidence = item.confidence,
                            calories = item.nutrition.caloriesKcal.formatPlain(),
                            protein = item.nutrition.proteinGrams.formatPlain(),
                            carbs = item.nutrition.carbohydratesGrams.formatPlain(),
                            fat = item.nutrition.fatGrams.formatPlain()
                        )
                    },
                    disclaimer = result.value.disclaimer
                )
                is AppResult.Failure -> state.value.copy(
                    loading = false,
                    phase = MealAnalysisPhase.Idle,
                    errorMessage = result.error.toUserMessage()
                )
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        val snapshot = state.value
        if (snapshot.items.isEmpty()) {
            state.value = snapshot.copy(errorMessage = "There are no items to save.")
            return
        }
        state.value = snapshot.copy(loading = true, errorMessage = null, phase = MealAnalysisPhase.Saving)
        viewModelScope.launch {
            val entries = snapshot.items.mapNotNull { item ->
                val grams = item.grams.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return@mapNotNull null
                FoodLogEntry(
                    name = item.name.ifBlank { "Analyzed meal item" },
                    brand = null,
                    mealType = snapshot.mealType,
                    grams = grams,
                    nutrition = NutritionFacts(
                        caloriesKcal = item.calories.toDoubleOrNull() ?: 0.0,
                        proteinGrams = item.protein.toDoubleOrNull() ?: 0.0,
                        carbohydratesGrams = item.carbs.toDoubleOrNull() ?: 0.0,
                        fatGrams = item.fat.toDoubleOrNull() ?: 0.0
                    ),
                    dataQuality = DataQuality.Estimated,
                    notes = buildString {
                        snapshot.disclaimer?.let {
                            if (it.isNotBlank()) append(it)
                        }
                        if (item.confidence > 0.0) {
                            if (isNotEmpty()) append(" ")
                            append("Confidence ${(item.confidence * 100).toInt()}%")
                        }
                    }
                )
            }
            if (entries.isEmpty()) {
                state.value = state.value.copy(loading = false, errorMessage = "No valid items to save.")
                return@launch
            }
            val errors = mutableListOf<String>()
            entries.forEach { entry ->
                val result = nutritionRepository.addFood(entry)
                if (result is AppResult.Failure) {
                    errors += entry.name
                }
            }
            if (errors.isEmpty()) {
                state.value = state.value.copy(loading = false)
                onSaved()
            } else {
                state.value = state.value.copy(
                    loading = false,
                    errorMessage = "Some items could not be saved: ${errors.take(3).joinToString(", ")}"
                )
            }
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        var sampleSize = 1
        val maxDimension = 1280
        while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private fun compressToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun AppError.toUserMessage(): String {
        return when (this) {
            is AppError.Validation -> message
            is AppError.Offline -> "n8n is unreachable or there is no connection."
            is AppError.Timeout -> "Image analysis timed out."
            is AppError.Unauthorized -> "Authentication failed."
            is AppError.NotFound -> "No foods were detected in the photo."
            is AppError.Remote -> message
            is AppError.Unknown -> message
        }
    }
}

private fun Double.formatPlain(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
