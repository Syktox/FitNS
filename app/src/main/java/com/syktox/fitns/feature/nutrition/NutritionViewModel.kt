package com.syktox.fitns.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syktox.fitns.core.model.AppError
import com.syktox.fitns.core.model.AppResult
import com.syktox.fitns.domain.model.DailyNutritionDashboard
import com.syktox.fitns.domain.model.DataQuality
import com.syktox.fitns.domain.model.FoodFavoritePreset
import com.syktox.fitns.domain.model.FoodLogEntry
import com.syktox.fitns.domain.model.FoodProductLookup
import com.syktox.fitns.domain.model.MealType
import com.syktox.fitns.domain.model.NutrientAggregate
import com.syktox.fitns.domain.model.NutritionFacts
import com.syktox.fitns.domain.model.NutritionGoal
import com.syktox.fitns.domain.repository.N8nRepository
import com.syktox.fitns.domain.repository.NutritionRepository
import com.syktox.fitns.domain.repository.ProfileRepository
import com.syktox.fitns.domain.repository.SettingsRepository
import com.syktox.fitns.domain.usecase.NutrientAggregator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NutritionUiState(
    val dashboard: DailyNutritionDashboard = EmptyNutritionDashboard,
    val foodHistory: List<FoodLogEntry> = emptyList(),
    val foodFavorites: List<FoodFavoritePreset> = emptyList(),
    val micronutrients: List<NutrientAggregate> = emptyList(),
    val errorMessage: String? = null,
    val barcodeLookup: BarcodeLookupUiState = BarcodeLookupUiState()
)

private data class NutritionCore(
    val dashboard: DailyNutritionDashboard,
    val foodHistory: List<FoodLogEntry>,
    val foodFavorites: List<FoodFavoritePreset>,
    val micronutrients: List<NutrientAggregate>
)

data class BarcodeLookupUiState(
    val barcode: String = "",
    val loading: Boolean = false,
    val statusMessage: String? = null,
    val prefillInput: ManualFoodInput? = null
)

private val EmptyNutritionDashboard = DailyNutritionDashboard(
    goal = NutritionGoal(
        caloriesKcal = 2300.0,
        proteinGrams = 150.0,
        carbohydrateGrams = 250.0,
        fatGrams = 75.0,
        fiberGrams = 30.0,
        waterMilliliters = 2500.0
    ),
    total = NutritionFacts(),
    waterMilliliters = 0.0,
    entries = emptyList()
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val n8nRepository: N8nRepository,
    private val settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository,
    aggregator: NutrientAggregator
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val barcodeLookup = MutableStateFlow(BarcodeLookupUiState())

    val uiState: StateFlow<NutritionUiState> = combine(
        combine(
            nutritionRepository.observeToday(),
            nutritionRepository.observeFoodHistory().map { entries -> entries.distinctBy { it.name to it.brand }.take(30) },
            nutritionRepository.observeFoodFavorites(),
            profileRepository.observeNutrientTargets()
        ) { dashboard, history, favorites, targets ->
            NutritionCore(
                dashboard = dashboard,
                foodHistory = history,
                foodFavorites = favorites,
                micronutrients = aggregator.aggregate(dashboard.entries, targets)
                    .filter { it.hasData && it.hasTarget }
                    .sortedBy { it.key.label }
            )
        },
        errorMessage,
        barcodeLookup
    ) { core, error, lookup ->
        NutritionUiState(
            dashboard = core.dashboard,
            foodHistory = core.foodHistory,
            foodFavorites = core.foodFavorites,
            micronutrients = core.micronutrients,
            errorMessage = error,
            barcodeLookup = lookup
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState()
    )

    fun updateBarcode(barcode: String) {
        barcodeLookup.value = barcodeLookup.value.copy(
            barcode = barcode,
            statusMessage = null
        )
    }

    fun onBarcodeScanned(barcode: String) {
        updateBarcode(barcode)
        lookupBarcode()
    }

    fun applyLabelValues(input: ManualFoodInput) {
        barcodeLookup.value = barcodeLookup.value.copy(
            barcode = "",
            loading = false,
            statusMessage = "Nutrition label values were filled in. Review them before saving.",
            prefillInput = input
        )
    }

    fun lookupBarcode() {
        val barcode = barcodeLookup.value.barcode.trim()
        if (barcode.isBlank()) {
            barcodeLookup.value = barcodeLookup.value.copy(statusMessage = "Enter a barcode first.")
            return
        }
        viewModelScope.launch {
            barcodeLookup.value = barcodeLookup.value.copy(
                loading = true,
                statusMessage = "Looking up barcode...",
                prefillInput = null
            )
            val settings = settingsRepository.observeN8nSettings().first()
            val result = n8nRepository.findProductByBarcode(
                baseUrl = settings.baseUrl,
                bearerToken = null,
                barcode = barcode
            )
            barcodeLookup.value = when (result) {
                is AppResult.Success -> BarcodeLookupUiState(
                    barcode = barcode,
                    loading = false,
                    statusMessage = "Product found. Review the details before saving.",
                    prefillInput = result.value.toManualFoodInput()
                )
                is AppResult.Failure -> barcodeLookup.value.copy(
                    loading = false,
                    statusMessage = result.error.toLookupMessage(),
                    prefillInput = null
                )
            }
        }
    }

    fun clearBarcodePrefill() {
        barcodeLookup.value = barcodeLookup.value.copy(prefillInput = null)
    }

    fun addFood(input: ManualFoodInput, onSaved: () -> Unit) {
        viewModelScope.launch {
            val result = nutritionRepository.addFood(
                FoodLogEntry(
                    name = input.name.ifBlank { "Manual entry" },
                    brand = input.brand,
                    mealType = input.mealType,
                    grams = input.grams,
                    nutrition = NutritionFacts(
                        caloriesKcal = input.calories,
                        proteinGrams = input.protein,
                        carbohydratesGrams = input.carbohydrates,
                        sugarGrams = input.sugar,
                        fatGrams = input.fat,
                        saturatedFatGrams = input.saturatedFat,
                        fiberGrams = input.fiber,
                        saltGrams = input.salt,
                        sodiumMilligrams = input.sodiumMilligrams
                    ),
                    dataQuality = DataQuality.Verified,
                    notes = input.notes,
                    micronutrients = input.micronutrients
                )
            )
            when (result) {
                is AppResult.Success -> {
                    errorMessage.value = null
                    onSaved()
                }
                is AppResult.Failure -> errorMessage.value = "Entry could not be saved."
            }
        }
    }

    fun updateFood(entry: FoodLogEntry, input: ManualFoodInput, onSaved: () -> Unit) {
        viewModelScope.launch {
            val result = nutritionRepository.updateFood(
                entry.copy(
                    name = input.name.ifBlank { "Manual entry" },
                    brand = input.brand,
                    mealType = input.mealType,
                    grams = input.grams,
                    nutrition = NutritionFacts(
                        caloriesKcal = input.calories,
                        proteinGrams = input.protein,
                        carbohydratesGrams = input.carbohydrates,
                        sugarGrams = input.sugar,
                        fatGrams = input.fat,
                        saturatedFatGrams = input.saturatedFat,
                        fiberGrams = input.fiber,
                        saltGrams = input.salt,
                        sodiumMilligrams = input.sodiumMilligrams
                    ),
                    notes = input.notes,
                    micronutrients = input.micronutrients
                )
            )
            when (result) {
                is AppResult.Success -> {
                    errorMessage.value = null
                    onSaved()
                }
                is AppResult.Failure -> errorMessage.value = "Entry could not be updated."
            }
        }
    }

    fun duplicateFood(entry: FoodLogEntry) {
        viewModelScope.launch {
            val result = nutritionRepository.addFood(
                entry.copy(
                    id = UUID.randomUUID().toString(),
                    consumedAt = System.currentTimeMillis()
                )
            )
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Entry could not be duplicated."
            }
        }
    }

    fun useFavorite(favorite: FoodFavoritePreset) {
        viewModelScope.launch {
            val result = nutritionRepository.addFood(favorite.toFoodLogEntry())
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Favorite could not be logged."
            }
        }
    }

    fun saveFavorite(entry: FoodLogEntry) {
        viewModelScope.launch {
            val result = nutritionRepository.saveFavorite(entry)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> result.error.toFavoriteMessage("Favorite could not be saved.")
            }
        }
    }

    fun deleteFavorite(favorite: FoodFavoritePreset) {
        viewModelScope.launch {
            val result = nutritionRepository.deleteFavorite(favorite)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Favorite could not be deleted."
            }
        }
    }

    fun deleteFood(entry: FoodLogEntry) {
        viewModelScope.launch {
            val result = nutritionRepository.deleteFood(entry)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Entry could not be deleted."
            }
        }
    }

    private fun FoodProductLookup.toManualFoodInput(): ManualFoodInput {
        val grams = servingSizeGrams ?: 100.0
        val factor = grams / 100.0
        return ManualFoodInput(
            name = name,
            brand = brand,
            grams = grams,
            calories = nutritionPer100g.caloriesKcal * factor,
            protein = nutritionPer100g.proteinGrams * factor,
            carbohydrates = nutritionPer100g.carbohydratesGrams * factor,
            sugar = nutritionPer100g.sugarGrams * factor,
            fat = nutritionPer100g.fatGrams * factor,
            saturatedFat = nutritionPer100g.saturatedFatGrams * factor,
            fiber = nutritionPer100g.fiberGrams * factor,
            salt = nutritionPer100g.saltGrams * factor,
            sodiumMilligrams = nutritionPer100g.sodiumMilligrams?.times(factor),
            mealType = MealType.Snack,
            notes = barcode?.let { "Barcode: $it" }.orEmpty()
        )
    }

    private fun FoodFavoritePreset.toFoodLogEntry(): FoodLogEntry {
        val factor = servingSizeGrams / 100.0
        return FoodLogEntry(
            name = name,
            brand = brand,
            mealType = MealType.Snack,
            grams = servingSizeGrams,
            nutrition = NutritionFacts(
                caloriesKcal = nutritionPer100g.caloriesKcal * factor,
                proteinGrams = nutritionPer100g.proteinGrams * factor,
                carbohydratesGrams = nutritionPer100g.carbohydratesGrams * factor,
                sugarGrams = nutritionPer100g.sugarGrams * factor,
                fatGrams = nutritionPer100g.fatGrams * factor,
                saturatedFatGrams = nutritionPer100g.saturatedFatGrams * factor,
                fiberGrams = nutritionPer100g.fiberGrams * factor,
                saltGrams = nutritionPer100g.saltGrams * factor,
                sodiumMilligrams = nutritionPer100g.sodiumMilligrams?.times(factor)
            ),
            dataQuality = DataQuality.Verified,
            notes = notes
        )
    }

    private fun AppError.toFavoriteMessage(fallback: String): String {
        return when (this) {
            is AppError.Validation -> message
            else -> fallback
        }
    }

    private fun AppError.toLookupMessage(): String {
        return when (this) {
            AppError.Offline -> "n8n is unreachable or there is no connection."
            AppError.Timeout -> "Barcode lookup timed out."
            AppError.Unauthorized -> "Authentication failed."
            AppError.NotFound -> "No product was found for this barcode."
            is AppError.Remote -> message
            is AppError.Validation -> message
            is AppError.Unknown -> message
        }
    }
}
