package com.raysix.fitns.feature.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.LabeledProgress
import com.raysix.fitns.core.design.LocalFloatingNavigationClearance
import com.raysix.fitns.core.design.MetricProgressBar
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.PillButton
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.core.design.WhaleTailMark
import com.raysix.fitns.core.design.isCompactHeight
import com.raysix.fitns.core.design.isWideScreen
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.CustomFood
import com.raysix.fitns.domain.model.FoodSearchSections
import com.raysix.fitns.domain.model.FoodFavoritePreset
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.NutrientAggregate
import com.raysix.fitns.domain.model.SavedMeal
import kotlin.math.abs
import kotlin.math.roundToInt

private val NutritionMealOrder = listOf(
    MealType.Breakfast,
    MealType.Lunch,
    MealType.Dinner,
    MealType.Snack,
    MealType.Custom
)

private enum class CompactNutritionSheet {
    Library,
    Tools
}

private sealed interface PendingMealLogAction {
    val itemName: String

    data class Duplicate(val entry: FoodLogEntry) : PendingMealLogAction {
        override val itemName: String = entry.name
    }

    data class Favorite(val favorite: FoodFavoritePreset) : PendingMealLogAction {
        override val itemName: String = favorite.name
    }

    data class Custom(val customFood: CustomFood) : PendingMealLogAction {
        override val itemName: String = customFood.name
    }

    data class SavedMealLog(val meal: SavedMeal, val scaleFactor: Double) : PendingMealLogAction {
        override val itemName: String = meal.name
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDayScreen(
    dashboard: DailyNutritionDashboard,
    foodHistory: List<FoodLogEntry>,
    foodFavorites: List<FoodFavoritePreset>,
    foodSearch: FoodSearchSections,
    savedMeals: List<SavedMeal>,
    micronutrients: List<NutrientAggregate>,
    errorMessage: String?,
    confirmationMessage: String?,
    onAddFood: () -> Unit,
    onFoodSearchQueryChange: (String) -> Unit,
    onDuplicateFood: (FoodLogEntry, MealType) -> Unit,
    onDeleteFood: (FoodLogEntry) -> Unit,
    onUseFavorite: (FoodFavoritePreset, MealType) -> Unit,
    onSaveFavorite: (FoodLogEntry) -> Unit,
    onDeleteFavorite: (FoodFavoritePreset) -> Unit,
    onUseCustomFood: (CustomFood, MealType) -> Unit,
    onSaveCustomFood: (FoodLogEntry) -> Unit,
    onDeleteCustomFood: (CustomFood) -> Unit,
    onSaveTodayAsMeal: (String) -> Unit,
    onLogSavedMeal: (SavedMeal, Double, MealType) -> Unit,
    onDeleteSavedMeal: (SavedMeal) -> Unit,
    onCopyYesterday: () -> Unit,
    onCopyPreviousMeal: (MealType) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<FoodLogEntry?>(null) }
    var pendingFavoriteDelete by remember { mutableStateOf<FoodFavoritePreset?>(null) }
    var pendingCustomDelete by remember { mutableStateOf<CustomFood?>(null) }
    var pendingSavedMealDelete by remember { mutableStateOf<SavedMeal?>(null) }
    var showSaveMealDialog by rememberSaveable { mutableStateOf(false) }
    var mealName by rememberSaveable { mutableStateOf("Today's meal") }
    var selectedMealName by rememberSaveable { mutableStateOf<String?>(null) }
    var compactSheetName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMealLogAction by remember { mutableStateOf<PendingMealLogAction?>(null) }
    var pendingMealSelection by remember { mutableStateOf<MealType?>(null) }
    val wide = isWideScreen()
    val compactHeight = isCompactHeight()
    val bottomClearance = LocalFloatingNavigationClearance.current
    val selectedMeal = selectedMealName?.let { name -> NutritionMealOrder.firstOrNull { it.name == name } }
    val visibleEntries = if (selectedMeal == null) {
        dashboard.entries
    } else {
        dashboard.entries.filter { it.mealType == selectedMeal }
    }
    val requestMealDestination: (PendingMealLogAction) -> Unit = { action ->
        pendingMealSelection = null
        pendingMealLogAction = action
        compactSheetName = null
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Food Entry") },
            text = { Text("Remove ${entry.name} from today's nutrition log?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFood(entry)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingFavoriteDelete?.let { favorite ->
        AlertDialog(
            onDismissRequest = { pendingFavoriteDelete = null },
            title = { Text("Delete Favorite") },
            text = { Text("Remove ${favorite.name} from your saved foods?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFavorite(favorite)
                        pendingFavoriteDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFavoriteDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingCustomDelete?.let { customFood ->
        AlertDialog(
            onDismissRequest = { pendingCustomDelete = null },
            title = { Text("Delete Custom Food") },
            text = { Text("Remove ${customFood.name} from your custom foods?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomFood(customFood)
                        pendingCustomDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCustomDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingSavedMealDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { pendingSavedMealDelete = null },
            title = { Text("Delete Saved Meal") },
            text = { Text("Remove ${meal.name} from saved meals?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSavedMeal(meal)
                        pendingSavedMealDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSavedMealDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSaveMealDialog) {
        AlertDialog(
            onDismissRequest = { showSaveMealDialog = false },
            title = { Text("Save Meal") },
            text = {
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Meal name") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = mealName.isNotBlank(),
                    onClick = {
                        onSaveTodayAsMeal(mealName.trim())
                        showSaveMealDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveMealDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingMealLogAction?.let { action ->
        MealDestinationDialog(
            selected = pendingMealSelection,
            onSelected = { pendingMealSelection = it },
            onDismiss = {
                pendingMealLogAction = null
                pendingMealSelection = null
            },
            onConfirm = { mealType ->
                when (action) {
                    is PendingMealLogAction.Duplicate -> onDuplicateFood(action.entry, mealType)
                    is PendingMealLogAction.Favorite -> onUseFavorite(action.favorite, mealType)
                    is PendingMealLogAction.Custom -> onUseCustomFood(action.customFood, mealType)
                    is PendingMealLogAction.SavedMealLog -> {
                        onLogSavedMeal(action.meal, action.scaleFactor, mealType)
                    }
                }
                pendingMealLogAction = null
                pendingMealSelection = null
            },
            title = "Log to which meal?",
            description = "Choose where the meal should appear in today's diary.",
            confirmLabel = "Log to diary"
        )
    }

    if (!wide && compactSheetName == CompactNutritionSheet.Library.name) {
        ModalBottomSheet(onDismissRequest = { compactSheetName = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                FoodLibraryCard(
                    sections = foodSearch,
                    foodHistory = foodHistory,
                    foodFavorites = foodFavorites,
                    onQueryChange = onFoodSearchQueryChange,
                    onUseRecent = {
                        requestMealDestination(PendingMealLogAction.Duplicate(it))
                    },
                    onSaveRecentFavorite = onSaveFavorite,
                    onUseFavorite = {
                        requestMealDestination(PendingMealLogAction.Favorite(it))
                    },
                    onDeleteFavorite = { pendingFavoriteDelete = it },
                    onUseCustomFood = {
                        requestMealDestination(PendingMealLogAction.Custom(it))
                    },
                    onDeleteCustomFood = { pendingCustomDelete = it }
                )
            }
        }
    }

    if (!wide && compactSheetName == CompactNutritionSheet.Tools.name) {
        ModalBottomSheet(onDismissRequest = { compactSheetName = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DayToolsCard(
                    hasEntries = dashboard.entries.isNotEmpty(),
                    onCopyYesterday = onCopyYesterday,
                    onCopyPreviousMeal = onCopyPreviousMeal,
                    onSaveTodayAsMeal = {
                        compactSheetName = null
                        showSaveMealDialog = true
                    }
                )
                SavedMealsCard(
                    meals = savedMeals,
                    onRequestLogSavedMeal = { meal, scaleFactor ->
                        requestMealDestination(PendingMealLogAction.SavedMealLog(meal, scaleFactor))
                    },
                    onDeleteSavedMeal = { pendingSavedMealDelete = it }
                )
                MicronutrientsCard(micronutrients = micronutrients)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NutritionDayHeader(
                foodCount = dashboard.entries.size,
                showAddButton = wide,
                onAddFood = onAddFood
            )
            NutritionStatusStrip(
                errorMessage = errorMessage,
                confirmationMessage = confirmationMessage,
                horizontalPadding = if (wide) 24.dp else 16.dp
            )

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = bottomClearance + 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MealFilterChips(
                            selectedMeal = selectedMeal,
                            onSelectedMealChange = { selectedMealName = it?.name }
                        )
                        DiarySections(
                            entries = visibleEntries,
                            selectedMeal = selectedMeal,
                            onAddFood = onAddFood,
                            onOpenLibrary = null,
                            onDuplicateFood = {
                                requestMealDestination(PendingMealLogAction.Duplicate(it))
                            },
                            onSaveFavorite = onSaveFavorite,
                            onSaveCustomFood = onSaveCustomFood,
                            onDeleteFood = { pendingDelete = it }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = bottomClearance + 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CompactNutritionSummary(dashboard = dashboard, dense = compactHeight)
                        FoodLibraryCard(
                            sections = foodSearch,
                            foodHistory = foodHistory,
                            foodFavorites = foodFavorites,
                            onQueryChange = onFoodSearchQueryChange,
                            onUseRecent = {
                                requestMealDestination(PendingMealLogAction.Duplicate(it))
                            },
                            onSaveRecentFavorite = onSaveFavorite,
                            onUseFavorite = {
                                requestMealDestination(PendingMealLogAction.Favorite(it))
                            },
                            onDeleteFavorite = { pendingFavoriteDelete = it },
                            onUseCustomFood = {
                                requestMealDestination(PendingMealLogAction.Custom(it))
                            },
                            onDeleteCustomFood = { pendingCustomDelete = it }
                        )
                        DayToolsCard(
                            hasEntries = dashboard.entries.isNotEmpty(),
                            onCopyYesterday = onCopyYesterday,
                            onCopyPreviousMeal = onCopyPreviousMeal,
                            onSaveTodayAsMeal = { showSaveMealDialog = true }
                        )
                        SavedMealsCard(
                            meals = savedMeals,
                            onRequestLogSavedMeal = { meal, scaleFactor ->
                                requestMealDestination(PendingMealLogAction.SavedMealLog(meal, scaleFactor))
                            },
                            onDeleteSavedMeal = { pendingSavedMealDelete = it }
                        )
                        MicronutrientsCard(micronutrients = micronutrients)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                        .padding(bottom = bottomClearance + 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactNutritionSummary(dashboard = dashboard, dense = false)
                    CompactQuickAccess(
                        onOpenLibrary = { compactSheetName = CompactNutritionSheet.Library.name },
                        onOpenTools = { compactSheetName = CompactNutritionSheet.Tools.name }
                    )
                    MealFilterChips(
                        selectedMeal = selectedMeal,
                        onSelectedMealChange = { selectedMealName = it?.name }
                    )
                    DiarySections(
                        entries = visibleEntries,
                        selectedMeal = selectedMeal,
                        onAddFood = onAddFood,
                        onOpenLibrary = { compactSheetName = CompactNutritionSheet.Library.name },
                        onDuplicateFood = {
                            requestMealDestination(PendingMealLogAction.Duplicate(it))
                        },
                        onSaveFavorite = onSaveFavorite,
                        onSaveCustomFood = onSaveCustomFood,
                        onDeleteFood = { pendingDelete = it }
                    )
                }
            }
        }

        if (!wide) {
            PersistentAddFoodButton(
                onClick = onAddFood,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomClearance + 10.dp)
            )
        }
    }
}

@Composable
private fun NutritionDayHeader(
    foodCount: Int,
    showAddButton: Boolean,
    onAddFood: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (showAddButton) 24.dp else 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WhaleTailMark(
                modifier = Modifier.requiredSize(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Nutrition",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (foodCount == 1) "Today · 1 food" else "Today · $foodCount foods",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (showAddButton) {
                Surface(
                    onClick = onAddFood,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Add food", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionStatusStrip(
    errorMessage: String?,
    confirmationMessage: String?,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    if (errorMessage == null && confirmationMessage == null) return
    Column(
        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        errorMessage?.let {
            ErrorBanner(
                message = it,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
            )
        }
        confirmationMessage?.let {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactNutritionSummary(dashboard: DailyNutritionDashboard, dense: Boolean) {
    val target = dashboard.goal.caloriesKcal
    val consumed = dashboard.total.caloriesKcal
    val hasTarget = target > 0.0
    val delta = target - consumed
    val overTarget = hasTarget && delta < 0.0
    val statusColor = if (overTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val statusLabel = when {
        !hasTarget -> "Calories logged"
        overTarget -> "Over target"
        delta == 0.0 -> "Target reached"
        else -> "Calories remaining"
    }
    val statusValue = if (hasTarget) abs(delta) else consumed

    ModernCard(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (dense) 8.dp else 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (overTarget) statusColor else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "${statusValue.roundToInt()} kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (hasTarget) "${consumed.roundToInt()} / ${target.roundToInt()}" else "${consumed.roundToInt()} total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
            }
            MetricProgressBar(
                progress = if (hasTarget) (consumed / target).toFloat() else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (hasTarget) {
                            "${consumed.roundToInt()} of ${target.roundToInt()} calories. $statusLabel ${statusValue.roundToInt()} calories."
                        } else {
                            "${consumed.roundToInt()} calories logged. No calorie target."
                        }
                    },
                color = statusColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (dense) 8.dp else 12.dp)
            ) {
                CompactMacroTarget(
                    label = "Protein",
                    current = dashboard.total.proteinGrams,
                    target = dashboard.goal.proteinGrams,
                    dense = dense,
                    modifier = Modifier.weight(1f)
                )
                CompactMacroTarget(
                    label = "Carbs",
                    current = dashboard.total.carbohydratesGrams,
                    target = dashboard.goal.carbohydrateGrams,
                    dense = dense,
                    modifier = Modifier.weight(1f)
                )
                CompactMacroTarget(
                    label = "Fat",
                    current = dashboard.total.fatGrams,
                    target = dashboard.goal.fatGrams,
                    dense = dense,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactMacroTarget(
    label: String,
    current: Double,
    target: Double,
    dense: Boolean,
    modifier: Modifier = Modifier
) {
    val over = target > 0.0 && current > target
    val color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.semantics {
            contentDescription = if (target > 0.0) {
                "$label, ${current.roundToInt()} of ${target.roundToInt()} grams${if (over) ", over target" else ""}."
            } else {
                "$label, ${current.roundToInt()} grams, no target."
            }
        },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2
        )
        Text(
            text = if (target > 0.0) "${current.roundToInt()} / ${target.roundToInt()} g" else "${current.roundToInt()} g",
            style = if (dense) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!dense) {
            MetricProgressBar(
                progress = if (target > 0.0) (current / target).toFloat() else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
        }
    }
}

@Composable
private fun CompactQuickAccess(onOpenLibrary: () -> Unit, onOpenTools: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionPill(
            text = "Food library",
            filled = false,
            modifier = Modifier.weight(1f),
            onClick = onOpenLibrary
        )
        ActionPill(
            text = "Day tools",
            filled = false,
            modifier = Modifier.weight(1f),
            onClick = onOpenTools
        )
    }
}

@Composable
private fun DiarySections(
    entries: List<FoodLogEntry>,
    selectedMeal: MealType?,
    onAddFood: () -> Unit,
    onOpenLibrary: (() -> Unit)?,
    onDuplicateFood: (FoodLogEntry) -> Unit,
    onSaveFavorite: (FoodLogEntry) -> Unit,
    onSaveCustomFood: (FoodLogEntry) -> Unit,
    onDeleteFood: (FoodLogEntry) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(if (selectedMeal == null) "Today's meals" else selectedMeal.displayName())
        TagChip(
            text = if (entries.size == 1) "1 food" else "${entries.size} foods",
            accent = entries.isNotEmpty()
        )
    }

    if (entries.isEmpty()) {
        DiaryEmptyState(
            selectedMeal = selectedMeal,
            onAddFood = onAddFood,
            onOpenLibrary = onOpenLibrary
        )
        return
    }

    val meals = if (selectedMeal == null) NutritionMealOrder else listOf(selectedMeal)
    meals.forEach { mealType ->
        val mealEntries = entries.filter { it.mealType == mealType }
        if (mealEntries.isNotEmpty()) {
            MealSection(
                mealType = mealType,
                entries = mealEntries,
                onDuplicateFood = onDuplicateFood,
                onSaveFavorite = onSaveFavorite,
                onSaveCustomFood = onSaveCustomFood,
                onDeleteFood = onDeleteFood
            )
        }
    }
}

@Composable
private fun DiaryEmptyState(
    selectedMeal: MealType?,
    onAddFood: () -> Unit,
    onOpenLibrary: (() -> Unit)?
) {
    ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = selectedMeal?.let { "No ${it.displayName().lowercase()} logged" } ?: "Nothing logged yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = selectedMeal?.let { "Add a food to start this meal." }
                    ?: "Add your first food or open the library to reuse something familiar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PillButton(
                text = if (onOpenLibrary == null) "Add food" else "Browse food library",
                onClick = onOpenLibrary ?: onAddFood,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MealSection(
    mealType: MealType,
    entries: List<FoodLogEntry>,
    onDuplicateFood: (FoodLogEntry) -> Unit,
    onSaveFavorite: (FoodLogEntry) -> Unit,
    onSaveCustomFood: (FoodLogEntry) -> Unit,
    onDeleteFood: (FoodLogEntry) -> Unit
) {
    ModernCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = mealType.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (entries.size == 1) "1 food" else "${entries.size} foods",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${entries.sumOf { it.nutrition.caloriesKcal }.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            entries.forEachIndexed { index, entry ->
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
                FoodEntryRow(
                    entry = entry,
                    onDuplicate = { onDuplicateFood(entry) },
                    onSaveFavorite = { onSaveFavorite(entry) },
                    onSaveCustomFood = { onSaveCustomFood(entry) },
                    onDelete = { onDeleteFood(entry) },
                    modifier = Modifier.padding(top = if (index == 0) 4.dp else 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FoodEntryRow(
    entry: FoodLogEntry,
    onDuplicate: () -> Unit,
    onSaveFavorite: () -> Unit,
    onSaveCustomFood: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (expanded) "Hide nutrition details" else "Show nutrition details",
                onClick = { expanded = !expanded }
            )
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        entry.brand?.takeIf { it.isNotBlank() },
                        "${entry.grams.roundToInt()} g"
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${entry.nutrition.caloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.semantics {
                        contentDescription = "More actions for ${entry.name}"
                    }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        },
                        modifier = Modifier.semantics { contentDescription = "Duplicate ${entry.name}" }
                    )
                    DropdownMenuItem(
                        text = { Text("Save as favorite") },
                        onClick = {
                            menuExpanded = false
                            onSaveFavorite()
                        },
                        modifier = Modifier.semantics { contentDescription = "Save ${entry.name} as favorite" }
                    )
                    DropdownMenuItem(
                        text = { Text("Save as custom food") },
                        onClick = {
                            menuExpanded = false
                            onSaveCustomFood()
                        },
                        modifier = Modifier.semantics { contentDescription = "Save ${entry.name} as custom food" }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        modifier = Modifier.semantics { contentDescription = "Delete ${entry.name}" }
                    )
                }
            }
        }
        Text(
            text = "Protein ${entry.nutrition.proteinGrams.roundToInt()} g  ·  Carbs ${entry.nutrition.carbohydratesGrams.roundToInt()} g  ·  Fat ${entry.nutrition.fatGrams.roundToInt()} g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = "Fiber ${entry.nutrition.fiberGrams.roundToInt()} g · Sugar ${entry.nutrition.sugarGrams.roundToInt()} g · Salt ${entry.nutrition.saltGrams} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.notes.isNotBlank()) {
                    Text(entry.notes, style = MaterialTheme.typography.bodySmall)
                }
                if (entry.dataQuality != DataQuality.Verified) {
                    TagChip(text = entry.dataQuality.label(), accent = entry.dataQuality == DataQuality.Missing)
                }
            }
        }
    }
}

@Composable
private fun PersistentAddFoodButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = "Add food",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun FoodLibraryCard(
    sections: FoodSearchSections,
    foodHistory: List<FoodLogEntry>,
    foodFavorites: List<FoodFavoritePreset>,
    onQueryChange: (String) -> Unit,
    onUseRecent: (FoodLogEntry) -> Unit,
    onSaveRecentFavorite: (FoodLogEntry) -> Unit,
    onUseFavorite: (FoodFavoritePreset) -> Unit,
    onDeleteFavorite: (FoodFavoritePreset) -> Unit,
    onUseCustomFood: (CustomFood) -> Unit,
    onDeleteCustomFood: (CustomFood) -> Unit
) {
    val queryActive = sections.query.isNotBlank()
    val recentOrMatches = if (queryActive) {
        (sections.recent + sections.searchResults).distinctBy { it.id }
    } else {
        foodHistory.take(5).ifEmpty { sections.recent.take(5) }
    }
    val favorites = if (queryActive) sections.favorites else foodFavorites
    val customFoods = sections.customFoods

    SectionCard(
        title = "Food library",
        subtitle = if (queryActive) "Matches across your saved foods" else "Recent, favorite, and custom foods"
    ) {
        OutlinedTextField(
            value = sections.query,
            onValueChange = onQueryChange,
            label = { Text("Search foods") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (sections.query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (recentOrMatches.isEmpty() && favorites.isEmpty() && customFoods.isEmpty()) {
            Text(
                text = if (queryActive) "No foods match “${sections.query}”." else "Your food library is empty.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LibrarySection(title = if (queryActive) "Matches" else "Recent", visible = recentOrMatches.isNotEmpty()) {
            recentOrMatches.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                LibraryFoodRow(
                    name = entry.name,
                    detail = "${entry.grams.roundToInt()} g · ${entry.nutrition.caloriesKcal.roundToInt()} kcal",
                    onUse = { onUseRecent(entry) },
                    secondaryActionLabel = "Save as favorite",
                    onSecondaryAction = { onSaveRecentFavorite(entry) }
                )
            }
        }
        LibrarySection(title = "Favorites", visible = favorites.isNotEmpty()) {
            favorites.forEachIndexed { index, favorite ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                val servingCalories = favorite.nutritionPer100g.caloriesKcal * favorite.servingSizeGrams / 100.0
                LibraryFoodRow(
                    name = favorite.name,
                    detail = "${favorite.servingSizeGrams.roundToInt()} g · ${servingCalories.roundToInt()} kcal",
                    onUse = { onUseFavorite(favorite) },
                    secondaryActionLabel = "Delete favorite",
                    secondaryActionDanger = true,
                    onSecondaryAction = { onDeleteFavorite(favorite) }
                )
            }
        }
        LibrarySection(title = "Custom foods", visible = customFoods.isNotEmpty()) {
            customFoods.forEachIndexed { index, custom ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                val servingCalories = custom.nutritionPer100g.caloriesKcal * custom.servingSizeGrams / 100.0
                LibraryFoodRow(
                    name = custom.name,
                    detail = "${custom.servingSizeGrams.roundToInt()} g · ${servingCalories.roundToInt()} kcal",
                    onUse = { onUseCustomFood(custom) },
                    secondaryActionLabel = "Delete custom food",
                    secondaryActionDanger = true,
                    onSecondaryAction = { onDeleteCustomFood(custom) }
                )
            }
        }
    }
}

@Composable
private fun LibrarySection(title: String, visible: Boolean, content: @Composable () -> Unit) {
    if (!visible) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun LibraryFoodRow(
    name: String,
    detail: String,
    onUse: () -> Unit,
    secondaryActionLabel: String,
    secondaryActionDanger: Boolean = false,
    onSecondaryAction: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        ActionPill(text = "Log", filled = true, onClick = onUse)
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.semantics { contentDescription = "More actions for $name" }
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            secondaryActionLabel,
                            color = if (secondaryActionDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = if (secondaryActionDanger) {
                        { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    onClick = {
                        menuExpanded = false
                        onSecondaryAction()
                    }
                )
            }
        }
    }
}

@Composable
private fun DayToolsCard(
    hasEntries: Boolean,
    onCopyYesterday: () -> Unit,
    onCopyPreviousMeal: (MealType) -> Unit,
    onSaveTodayAsMeal: () -> Unit
) {
    SectionCard(title = "Day tools", subtitle = "Reuse food without rebuilding the day") {
        ActionPill(
            text = "Copy yesterday",
            filled = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCopyYesterday
        )
        Text(
            text = "Copy a previous meal",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutritionMealOrder.forEach { meal ->
                ActionPill(
                    text = meal.displayName(),
                    filled = false,
                    onClick = { onCopyPreviousMeal(meal) }
                )
            }
        }
        if (hasEntries) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ActionPill(
                text = "Save today as a meal",
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onSaveTodayAsMeal
            )
        }
    }
}

private fun MealType.displayName(): String = when (this) {
    MealType.Breakfast -> "Breakfast"
    MealType.Lunch -> "Lunch"
    MealType.Dinner -> "Dinner"
    MealType.Snack -> "Snack"
    MealType.Custom -> "Custom"
}

@Composable
private fun MicronutrientsCard(micronutrients: List<NutrientAggregate>) {
    if (micronutrients.isEmpty()) {
        ModernCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Micronutrients")
                Text(
                    "No micronutrient data yet. Foods logged with vitamin and mineral values will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    SectionCard(
        title = "Micronutrients",
        trailing = {
            TagChip(
                text = "${micronutrients.count { it.percent >= 1f }}/${micronutrients.size} met",
                accent = true
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            micronutrients.forEach { aggregate ->
                LabeledProgress(
                    label = aggregate.label,
                    current = "${aggregate.consumed?.roundToInt() ?: 0} / ${aggregate.target?.roundToInt() ?: 0} ${aggregate.unit}",
                    progress = aggregate.percent,
                    barColor = if (aggregate.percent >= 1f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                )
            }
            if (micronutrients.none { it.percent < 1f }) {
                Text(
                    "All logged targets are covered today.",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealFilterChips(selectedMeal: MealType?, onSelectedMealChange: (MealType?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedMeal == null,
            onClick = { onSelectedMealChange(null) },
            label = { Text("All") }
        )
        NutritionMealOrder.forEach { meal ->
            FilterChip(
                selected = selectedMeal == meal,
                onClick = { onSelectedMealChange(meal) },
                label = { Text(meal.displayName()) }
            )
        }
    }
}

@Composable
private fun SavedMealsCard(
    meals: List<SavedMeal>,
    onRequestLogSavedMeal: (SavedMeal, Double) -> Unit,
    onDeleteSavedMeal: (SavedMeal) -> Unit
) {
    SectionCard(title = "Saved Meals", subtitle = "Scale and log reusable meals") {
        if (meals.isEmpty()) {
            Text("Save today's foods as a meal to reuse them later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        meals.forEach { meal ->
            SavedMealRow(
                meal = meal,
                onRequestLogSavedMeal = onRequestLogSavedMeal,
                onDeleteSavedMeal = onDeleteSavedMeal
            )
        }
    }
}

@Composable
private fun SavedMealRow(
    meal: SavedMeal,
    onRequestLogSavedMeal: (SavedMeal, Double) -> Unit,
    onDeleteSavedMeal: (SavedMeal) -> Unit
) {
    var scale by rememberSaveable(meal.id) { mutableStateOf("1.0") }
    val scaleValue = scale.toUserDecimalOrNull()
    val scaleValid = scaleValue != null && scaleValue > 0.0
    ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        meal.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${meal.items.size} foods · ${meal.caloriesKcal.roundToInt()} kcal · ${meal.proteinGrams.roundToInt()} g protein",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                IconButton(
                    onClick = { onDeleteSavedMeal(meal) },
                    modifier = Modifier.semantics { contentDescription = "Delete saved meal ${meal.name}" }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedTextField(
                value = scale,
                onValueChange = { scale = it },
                label = { Text("Scale") },
                supportingText = if (!scaleValid) {
                    { Text("Enter a number greater than zero") }
                } else {
                    {
                        Text(
                            "${(meal.caloriesKcal * (scaleValue ?: 1.0)).roundToInt()} kcal after scaling"
                        )
                    }
                },
                isError = !scaleValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5, 1.0, 1.5, 2.0).forEach { factor ->
                    FilterChip(
                        selected = scaleValue == factor,
                        onClick = { scale = factor.toString() },
                        label = { Text("${factor}x") }
                    )
                }
            }
            PillButton(
                text = "Log ${meal.name}",
                modifier = Modifier.fillMaxWidth(),
                enabled = scaleValid,
                onClick = { onRequestLogSavedMeal(meal, scaleValue ?: return@PillButton) }
            )
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val color = when {
        danger -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (filled) color else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else color
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

private fun DataQuality.label(): String {
    return when (this) {
        DataQuality.Verified -> "Verified"
        DataQuality.Estimated -> "Estimated"
        DataQuality.Missing -> "Needs review"
    }
}
