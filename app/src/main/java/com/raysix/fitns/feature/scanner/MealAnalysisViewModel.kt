package com.raysix.fitns.feature.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.FoodLogEntry
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

enum class MealAnalysisPhase { Capture, Analyzing, Review, Saving, Error }

enum class MealAnalysisFailure { PrivacyDisabled, InvalidImage, NoFoodDetected, Remote }

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
    val phase: MealAnalysisPhase = MealAnalysisPhase.Capture,
    val previewBitmap: ImageBitmap? = null,
    val mealType: MealType = defaultMealType(),
    val items: List<EditableMealItem> = emptyList(),
    val disclaimer: String? = null,
    val errorMessage: String? = null,
    val failure: MealAnalysisFailure? = null,
    val loading: Boolean = false
) {
    val totals: MealAnalysisTotals
        get() = calculateMealAnalysisTotals(items)

    val canSave: Boolean
        get() = items.isNotEmpty() && items.all(EditableMealItem::isValid)
}

data class MealAnalysisTotals(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val fat: Double = 0.0
)

@HiltViewModel
class MealAnalysisViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val n8nRepository: N8nRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val state = MutableStateFlow(MealAnalysisUiState())
    private var lastCapturedBytes: ByteArray? = null

    val uiState: StateFlow<MealAnalysisUiState> = state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MealAnalysisUiState()
    )

    fun onImageCaptured(bytes: ByteArray) {
        lastCapturedBytes = bytes.copyOf()
        analyze(bytes)
    }

    fun retryAnalysis() {
        val snapshot = state.value
        if (snapshot.loading || snapshot.phase != MealAnalysisPhase.Error) return

        val bytes = lastCapturedBytes
        if (bytes == null) {
            state.value = snapshot.copy(
                phase = MealAnalysisPhase.Error,
                failure = MealAnalysisFailure.InvalidImage,
                errorMessage = "The original photo is no longer available. Take another photo."
            )
            return
        }
        analyze(bytes)
    }

    fun retake() {
        lastCapturedBytes = null
        state.value = MealAnalysisUiState(mealType = state.value.mealType)
    }

    private fun analyze(bytes: ByteArray) {
        state.value = state.value.copy(
            loading = true,
            errorMessage = null,
            failure = null,
            items = emptyList(),
            disclaimer = null,
            phase = MealAnalysisPhase.Analyzing
        )
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) { decodeScaled(bytes) }
            if (bitmap == null) {
                state.value = state.value.copy(
                    loading = false,
                    phase = MealAnalysisPhase.Error,
                    previewBitmap = null,
                    failure = MealAnalysisFailure.InvalidImage,
                    errorMessage = "The photo could not be read. Try taking it again."
                )
                return@launch
            }
            state.value = state.value.copy(previewBitmap = bitmap.asImageBitmap())

            val uploadEnabled = settingsRepository.observeMealPhotoAnalysisEnabled().first()
            if (!uploadEnabled) {
                state.value = state.value.copy(
                    loading = false,
                    phase = MealAnalysisPhase.Error,
                    failure = MealAnalysisFailure.PrivacyDisabled,
                    errorMessage = "Enable meal photo analysis in Settings → Privacy & Data before scanning."
                )
                return@launch
            }

            val base64 = withContext(Dispatchers.Default) { compressToBase64(bitmap) }

            val settings = settingsRepository.observeN8nSettings().first()
            val token = settingsRepository.readBearerToken()
            val result = n8nRepository.analyzeMealImage(
                baseUrl = settings.baseUrl,
                bearerToken = token,
                imageBase64 = base64,
                consentGranted = true
            )
            state.value = when (result) {
                is AppResult.Success -> {
                    val detectedItems = result.value.items.filter { it.name.isNotBlank() }
                    if (detectedItems.isEmpty()) {
                        state.value.copy(
                            loading = false,
                            phase = MealAnalysisPhase.Error,
                            items = emptyList(),
                            disclaimer = null,
                            failure = MealAnalysisFailure.NoFoodDetected,
                            errorMessage = "No food was detected. Try another angle or choose a clearer photo."
                        )
                    } else {
                        state.value.copy(
                            phase = MealAnalysisPhase.Review,
                            loading = false,
                            items = detectedItems.mapIndexed { index, item ->
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
                            disclaimer = result.value.disclaimer,
                            failure = null,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Failure -> state.value.copy(
                    loading = false,
                    phase = MealAnalysisPhase.Error,
                    items = emptyList(),
                    disclaimer = null,
                    failure = MealAnalysisFailure.Remote,
                    errorMessage = result.error.toUserMessage()
                )
            }
        }
    }

    fun onMealTypeChange(mealType: MealType) {
        state.value = state.value.copy(mealType = mealType)
    }

    fun updateItem(
        id: String,
        name: String,
        grams: String,
        calories: String,
        protein: String,
        carbs: String,
        fat: String
    ) {
        state.value = state.value.copy(
            items = state.value.items.map { item ->
                if (item.id == id) {
                    item.copy(
                        name = name,
                        grams = grams,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat
                    )
                } else {
                    item
                }
            },
            errorMessage = null
        )
    }

    fun removeItem(id: String) {
        state.value = state.value.copy(
            items = state.value.items.filterNot { it.id == id },
            errorMessage = null
        )
    }

    fun addItem() {
        val nextIndex = state.value.items.size + 1
        state.value = state.value.copy(
            phase = MealAnalysisPhase.Review,
            items = state.value.items + EditableMealItem(
                id = "manual-${java.util.UUID.randomUUID()}",
                name = "Food item $nextIndex",
                grams = "100",
                confidence = 0.0,
                calories = "",
                protein = "",
                carbs = "",
                fat = ""
            ),
            errorMessage = null
        )
    }

    fun save(onSaved: () -> Unit) {
        val snapshot = state.value
        if (snapshot.loading || snapshot.phase != MealAnalysisPhase.Review) return
        if (!snapshot.canSave) {
            state.value = snapshot.copy(errorMessage = "Review the highlighted fields before logging this meal.")
            return
        }
        state.value = snapshot.copy(loading = true, errorMessage = null, phase = MealAnalysisPhase.Saving)
        viewModelScope.launch {
            val entries = snapshot.items.map { item ->
                val grams = requireNotNull(item.grams.toUserDecimalOrNull())
                FoodLogEntry(
                    name = item.name.trim(),
                    brand = null,
                    mealType = snapshot.mealType,
                    grams = grams,
                    nutrition = NutritionFacts(
                        caloriesKcal = item.calories.toUserDecimalOrNull() ?: 0.0,
                        proteinGrams = item.protein.toUserDecimalOrNull() ?: 0.0,
                        carbohydratesGrams = item.carbs.toUserDecimalOrNull() ?: 0.0,
                        fatGrams = item.fat.toUserDecimalOrNull() ?: 0.0
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
            when (nutritionRepository.addFoods(entries)) {
                is AppResult.Success -> {
                    state.value = state.value.copy(loading = false)
                    onSaved()
                }
                is AppResult.Failure -> {
                    state.value = state.value.copy(
                        loading = false,
                        phase = MealAnalysisPhase.Review,
                        errorMessage = "The meal could not be logged. Nothing was added; try again."
                    )
                }
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

internal fun EditableMealItem.isValid(): Boolean {
    val validGrams = grams.toUserDecimalOrNull()?.let { it.isFinite() && it > 0.0 } == true
    val validNutrition = listOf(calories, protein, carbs, fat).all { value ->
        value.toUserDecimalOrNull()?.let { it.isFinite() && it >= 0.0 } == true
    }
    return name.isNotBlank() && validGrams && validNutrition
}

internal fun calculateMealAnalysisTotals(items: List<EditableMealItem>): MealAnalysisTotals {
    return MealAnalysisTotals(
        calories = items.sumOf { it.calories.toUserDecimalOrNull() ?: 0.0 },
        protein = items.sumOf { it.protein.toUserDecimalOrNull() ?: 0.0 },
        carbohydrates = items.sumOf { it.carbs.toUserDecimalOrNull() ?: 0.0 },
        fat = items.sumOf { it.fat.toUserDecimalOrNull() ?: 0.0 }
    )
}

private fun defaultMealType(): MealType {
    return when (java.time.LocalTime.now().hour) {
        in 5..10 -> MealType.Breakfast
        in 11..15 -> MealType.Lunch
        in 16..21 -> MealType.Dinner
        else -> MealType.Snack
    }
}

private fun Double.formatPlain(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
