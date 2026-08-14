package com.raysix.fitns.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.CustomFood
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.FoodSearchSections
import com.raysix.fitns.domain.model.FoodFavoritePreset
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.FoodProductLookup
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.NutrientAggregate
import com.raysix.fitns.domain.model.NutritionFacts
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.SavedMeal
import com.raysix.fitns.domain.model.SavedMealItem
import com.raysix.fitns.domain.repository.N8nRepository
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.SettingsRepository
import com.raysix.fitns.domain.usecase.NutrientAggregator
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class NutritionUiState(
    val dashboard: DailyNutritionDashboard = EmptyNutritionDashboard,
    val foodHistory: List<FoodLogEntry> = emptyList(),
    val foodFavorites: List<FoodFavoritePreset> = emptyList(),
    val customFoods: List<CustomFood> = emptyList(),
    val savedMeals: List<SavedMeal> = emptyList(),
    val foodSearch: FoodSearchSections = FoodSearchSections(),
    val micronutrients: List<NutrientAggregate> = emptyList(),
    val errorMessage: String? = null,
    val confirmationMessage: String? = null,
    val barcodeLookup: BarcodeLookupUiState = BarcodeLookupUiState()
)

private data class NutritionCore(
    val dashboard: DailyNutritionDashboard,
    val foodHistory: List<FoodLogEntry>,
    val foodFavorites: List<FoodFavoritePreset>,
    val customFoods: List<CustomFood>,
    val savedMeals: List<SavedMeal>,
    val micronutrients: List<NutrientAggregate>
)

private data class NutritionPrimarySources(
    val dashboard: DailyNutritionDashboard,
    val foodHistory: List<FoodLogEntry>,
    val foodFavorites: List<FoodFavoritePreset>
)

private data class NutritionSecondarySources(
    val customFoods: List<CustomFood>,
    val savedMeals: List<SavedMeal>,
    val nutrientTargets: List<com.raysix.fitns.domain.model.NutrientTarget>
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
    private val confirmationMessage = MutableStateFlow<String?>(null)
    private val barcodeLookup = MutableStateFlow(BarcodeLookupUiState())
    private val foodSearchQuery = MutableStateFlow("")

    val uiState: StateFlow<NutritionUiState> = combine(
        combine(
            nutritionRepository.observeToday(),
            nutritionRepository.observeFoodHistory(),
            nutritionRepository.observeFoodFavorites()
        ) { dashboard, history, favorites ->
            NutritionPrimarySources(
                dashboard = dashboard,
                foodHistory = history,
                foodFavorites = favorites
            )
        }.combine(
            combine(
            nutritionRepository.observeCustomFoods(),
            nutritionRepository.observeSavedMeals(),
            profileRepository.observeNutrientTargets()
            ) { customFoods, savedMeals, targets ->
                NutritionSecondarySources(
                    customFoods = customFoods,
                    savedMeals = savedMeals,
                    nutrientTargets = targets
                )
            }
        ) { primary, secondary ->
            NutritionCore(
                dashboard = primary.dashboard,
                foodHistory = primary.foodHistory,
                foodFavorites = primary.foodFavorites,
                customFoods = secondary.customFoods,
                savedMeals = secondary.savedMeals,
                micronutrients = aggregator.aggregate(primary.dashboard.entries, secondary.nutrientTargets)
                    .filter { it.hasData && it.hasTarget }
                    .sortedBy { it.key.label }
            )
        },
        errorMessage,
        confirmationMessage,
        foodSearchQuery,
        barcodeLookup
    ) { core, error, confirmation, searchQuery, lookup ->
        NutritionUiState(
            dashboard = core.dashboard,
            foodHistory = core.foodHistory.distinctBy { it.name to it.brand }.take(30),
            foodFavorites = core.foodFavorites,
            customFoods = core.customFoods,
            savedMeals = core.savedMeals,
            foodSearch = core.toSearchSections(searchQuery),
            micronutrients = core.micronutrients,
            errorMessage = error,
            confirmationMessage = confirmation,
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

    fun updateFoodSearchQuery(query: String) {
        foodSearchQuery.value = query
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
                bearerToken = settingsRepository.readBearerToken(),
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
                    dataQuality = input.dataQuality,
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

    fun useCustomFood(customFood: CustomFood, mealType: MealType = MealType.Snack) {
        viewModelScope.launch {
            val result = nutritionRepository.addFood(customFood.toFoodLogEntry(mealType))
            errorMessage.value = when (result) {
                is AppResult.Success -> {
                    confirmationMessage.value = "${customFood.name} logged."
                    null
                }
                is AppResult.Failure -> "Custom food could not be logged."
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

    fun saveCustomFood(entry: FoodLogEntry) {
        viewModelScope.launch {
            val result = nutritionRepository.saveCustomFood(entry)
            errorMessage.value = when (result) {
                is AppResult.Success -> {
                    confirmationMessage.value = "${entry.name} saved as a custom food."
                    null
                }
                is AppResult.Failure -> result.error.toFavoriteMessage("Custom food could not be saved.")
            }
        }
    }

    fun deleteCustomFood(customFood: CustomFood) {
        viewModelScope.launch {
            val result = nutritionRepository.deleteCustomFood(customFood)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Custom food could not be deleted."
            }
        }
    }

    fun saveTodayAsMeal(name: String) {
        val entries = uiState.value.dashboard.entries
        viewModelScope.launch {
            val result = nutritionRepository.saveMeal(
                SavedMeal(
                    name = name.ifBlank { "Saved Meal" },
                    items = entries.map { SavedMealItem(food = it) }
                )
            )
            errorMessage.value = when (result) {
                is AppResult.Success -> {
                    confirmationMessage.value = "Meal saved."
                    null
                }
                is AppResult.Failure -> result.error.toFavoriteMessage("Meal could not be saved.")
            }
        }
    }

    fun logSavedMeal(meal: SavedMeal, scaleFactor: Double, mealType: MealType) {
        viewModelScope.launch {
            val result = nutritionRepository.logSavedMeal(meal, scaleFactor, mealType)
            errorMessage.value = when (result) {
                is AppResult.Success -> {
                    confirmationMessage.value = "${meal.name} logged."
                    null
                }
                is AppResult.Failure -> result.error.toFavoriteMessage("Meal could not be logged.")
            }
        }
    }

    fun deleteSavedMeal(meal: SavedMeal) {
        viewModelScope.launch {
            val result = nutritionRepository.deleteSavedMeal(meal)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Saved meal could not be deleted."
            }
        }
    }

    fun copyYesterday() {
        val entries = uiState.value.foodHistory.previousDayEntries()
        copyEntries(entries, successMessage = "Yesterday copied.")
    }

    fun copyPreviousMeal(mealType: MealType) {
        val entries = uiState.value.foodHistory.previousMealEntries(mealType)
        copyEntries(entries, mealType, "Previous ${mealType.name.lowercase()} copied.")
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

    private fun copyEntries(entries: List<FoodLogEntry>, mealType: MealType? = null, successMessage: String) {
        viewModelScope.launch {
            val result = nutritionRepository.copyEntries(entries, mealType)
            errorMessage.value = when (result) {
                is AppResult.Success -> {
                    confirmationMessage.value = successMessage
                    null
                }
                is AppResult.Failure -> result.error.toFavoriteMessage("Food entries could not be copied.")
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

    private fun CustomFood.toFoodLogEntry(mealType: MealType): FoodLogEntry {
        val factor = servingSizeGrams / 100.0
        return FoodLogEntry(
            name = name,
            brand = brand,
            mealType = mealType,
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
            notes = notes,
            micronutrients = micronutrients
        )
    }

    private fun NutritionCore.toSearchSections(query: String): FoodSearchSections {
        val normalized = query.trim().lowercase()
        fun String?.matchesQuery(): Boolean = normalized.isBlank() || this.orEmpty().lowercase().contains(normalized)
        fun FoodLogEntry.matches() = name.matchesQuery() || brand.matchesQuery()
        fun FoodFavoritePreset.matches() = name.matchesQuery() || brand.matchesQuery()
        fun CustomFood.matches() = name.matchesQuery() || brand.matchesQuery()
        val recent = foodHistory
            .filter { it.matches() }
            .distinctBy { it.name.lowercase() to it.brand.orEmpty().lowercase() }
            .take(8)
        return FoodSearchSections(
            query = query,
            recent = recent,
            favorites = foodFavorites.filter { it.matches() }.take(8),
            customFoods = customFoods.filter { it.matches() }.take(8),
            searchResults = if (normalized.isBlank()) {
                emptyList()
            } else {
                foodHistory.filter { it.matches() && it !in recent }.take(20)
            }
        )
    }

    private fun List<FoodLogEntry>.previousDayEntries(): List<FoodLogEntry> {
        val zone = ZoneId.systemDefault()
        val yesterday = LocalDate.now(zone).minusDays(1)
        return filter { Instant.ofEpochMilli(it.consumedAt).atZone(zone).toLocalDate() == yesterday }
    }

    private fun List<FoodLogEntry>.previousMealEntries(mealType: MealType): List<FoodLogEntry> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val grouped = filter { entry ->
            entry.mealType == mealType && Instant.ofEpochMilli(entry.consumedAt).atZone(zone).toLocalDate() < today
        }.groupBy { Instant.ofEpochMilli(it.consumedAt).atZone(zone).toLocalDate() }
        val latestDay = grouped.keys.maxOrNull() ?: return emptyList()
        return grouped[latestDay].orEmpty()
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
