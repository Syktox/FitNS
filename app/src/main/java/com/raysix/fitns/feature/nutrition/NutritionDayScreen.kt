package com.raysix.fitns.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.LabeledProgress
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.FoodFavoritePreset
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.NutrientAggregate
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NutritionDayScreen(
    dashboard: DailyNutritionDashboard,
    foodHistory: List<FoodLogEntry>,
    foodFavorites: List<FoodFavoritePreset>,
    micronutrients: List<NutrientAggregate>,
    errorMessage: String?,
    onAddFood: () -> Unit,
    onDuplicateFood: (FoodLogEntry) -> Unit,
    onDeleteFood: (FoodLogEntry) -> Unit,
    onUseFavorite: (FoodFavoritePreset) -> Unit,
    onSaveFavorite: (FoodLogEntry) -> Unit,
    onDeleteFavorite: (FoodFavoritePreset) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<FoodLogEntry?>(null) }
    var pendingFavoriteDelete by remember { mutableStateOf<FoodFavoritePreset?>(null) }
    var selectedMeal by remember { mutableStateOf<MealType?>(null) }
    val visibleEntries = if (selectedMeal == null) {
        dashboard.entries
    } else {
        dashboard.entries.filter { it.mealType == selectedMeal }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    ScreenHeader(
                        title = "Nutrition",
                        subtitle = "${visibleEntries.size} of ${dashboard.entries.size} foods shown today"
                    )
                }
                Button(onClick = onAddFood) {
                    Text("Add Food")
                }
            }
        }
        item {
            NutritionTargetsCard(dashboard = dashboard)
        }
        item {
            MicronutrientsCard(micronutrients = micronutrients)
        }
        if (dashboard.entries.isNotEmpty()) {
            item {
                MealSummaryCard(entries = dashboard.entries)
            }
            item {
                MealFilterChips(
                    selectedMeal = selectedMeal,
                    onSelectedMealChange = { selectedMeal = it }
                )
            }
        }
        if (errorMessage != null) {
            item {
                ErrorBanner(message = errorMessage)
            }
        }
        if (foodFavorites.isNotEmpty()) {
            item {
                SectionTitle("Favorites")
            }
            items(foodFavorites) { favorite ->
                FavoriteCard(
                    favorite = favorite,
                    onUse = { onUseFavorite(favorite) },
                    onDelete = { pendingFavoriteDelete = favorite }
                )
            }
        }
        if (foodHistory.isNotEmpty()) {
            item {
                SectionTitle("Quick Reuse")
            }
            items(foodHistory.take(5)) { entry ->
                QuickReuseCard(
                    entry = entry,
                    onUse = { onDuplicateFood(entry) },
                    onSaveFavorite = { onSaveFavorite(entry) }
                )
            }
        }
        item {
            SectionTitle("Today")
        }
        if (dashboard.entries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No foods logged yet.",
                    message = "Add your first meal or reuse a recent food to start tracking today."
                )
            }
        }
        items(visibleEntries) { entry ->
            FoodEntryCard(
                entry = entry,
                onDuplicate = { onDuplicateFood(entry) },
                onSaveFavorite = { onSaveFavorite(entry) },
                onDelete = { pendingDelete = entry }
            )
        }
    }
}

@Composable
private fun NutritionTargetsCard(dashboard: DailyNutritionDashboard) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Nutrition Targets")
            TargetLine(
                label = "Calories",
                current = dashboard.total.caloriesKcal,
                target = dashboard.goal.caloriesKcal,
                unit = "kcal"
            )
            TargetLine(
                label = "Protein",
                current = dashboard.total.proteinGrams,
                target = dashboard.goal.proteinGrams,
                unit = "g"
            )
            TargetLine(
                label = "Carbs",
                current = dashboard.total.carbohydratesGrams,
                target = dashboard.goal.carbohydrateGrams,
                unit = "g"
            )
            TargetLine(
                label = "Fat",
                current = dashboard.total.fatGrams,
                target = dashboard.goal.fatGrams,
                unit = "g"
            )
        }
    }
}

@Composable
private fun MicronutrientsCard(micronutrients: List<NutrientAggregate>) {
    if (micronutrients.isEmpty()) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Micronutrients")
                Text(
                    "No micronutrient data yet. Foods logged with vitamin and mineral values will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Micronutrients")
                TagChip(
                    text = "${micronutrients.count { it.percent >= 1f }}/${micronutrients.size} met",
                    accent = true
                )
            }
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MealFilterChips(selectedMeal: MealType?, onSelectedMealChange: (MealType?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedMeal == null,
            onClick = { onSelectedMealChange(null) },
            label = { Text("All") }
        )
        listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { meal ->
            FilterChip(
                selected = selectedMeal == meal,
                onClick = { onSelectedMealChange(meal) },
                label = { Text(meal.name) }
            )
        }
    }
}

@Composable
private fun MealSummaryCard(entries: List<FoodLogEntry>) {
    val mealOrder = listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack, MealType.Custom)
    val byMeal = entries.groupBy { it.mealType }
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Meal Split")
            mealOrder.forEach { meal ->
                val items = byMeal[meal].orEmpty()
                if (items.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(meal.name)
                        Text("${items.sumOf { it.nutrition.caloriesKcal }.roundToInt()} kcal · ${items.size} items")
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetLine(label: String, current: Double, target: Double, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${current.roundToInt()} / ${target.roundToInt()} $unit")
        }
        LinearProgressIndicator(
            progress = { if (target <= 0.0) 0f else (current / target).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FavoriteCard(favorite: FoodFavoritePreset, onUse: () -> Unit, onDelete: () -> Unit) {
    val servingFactor = favorite.servingSizeGrams / 100.0
    val servingCalories = favorite.nutritionPer100g.caloriesKcal * servingFactor
    val servingProtein = favorite.nutritionPer100g.proteinGrams * servingFactor

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(favorite.name, fontWeight = FontWeight.SemiBold)
                    favorite.brand?.let { Text(it, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    Text("${favorite.servingSizeGrams.roundToInt()} g serving")
                }
                Text(
                    text = "${servingCalories.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            MacroChipRow(
                calories = servingCalories,
                protein = servingProtein,
                carbs = favorite.nutritionPer100g.carbohydratesGrams * servingFactor,
                fat = favorite.nutritionPer100g.fatGrams * servingFactor
            )
            if (favorite.notes.isNotBlank()) {
                Text(favorite.notes, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUse) {
                    Text("Log")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun QuickReuseCard(entry: FoodLogEntry, onUse: () -> Unit, onSaveFavorite: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.name, fontWeight = FontWeight.SemiBold)
                    entry.brand?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("${entry.grams.roundToInt()} g · ${entry.nutrition.caloriesKcal.roundToInt()} kcal")
                }
                OutlinedButton(onClick = onUse) {
                    Text("Log")
                }
            }
            OutlinedButton(onClick = onSaveFavorite) {
                Text("Save Favorite")
            }
            MacroChipRow(
                calories = entry.nutrition.caloriesKcal,
                protein = entry.nutrition.proteinGrams,
                carbs = entry.nutrition.carbohydratesGrams,
                fat = entry.nutrition.fatGrams
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FoodEntryCard(
    entry: FoodLogEntry,
    onDuplicate: () -> Unit,
    onSaveFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.name, fontWeight = FontWeight.SemiBold)
                    entry.brand?.let { brand ->
                        Text(brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${entry.mealType} · ${entry.grams.roundToInt()} g")
                }
                Text(
                    text = "${entry.nutrition.caloriesKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            MacroChipRow(
                calories = entry.nutrition.caloriesKcal,
                protein = entry.nutrition.proteinGrams,
                carbs = entry.nutrition.carbohydratesGrams,
                fat = entry.nutrition.fatGrams
            )
            Text(
                text = "Fiber ${entry.nutrition.fiberGrams.roundToInt()} g · Sugar ${entry.nutrition.sugarGrams.roundToInt()} g · Salt ${entry.nutrition.saltGrams} g",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.notes.isNotBlank()) {
                Text(entry.notes)
            }
            Text("Data quality: ${entry.dataQuality.label()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDuplicate) {
                    Text("Duplicate")
                }
                OutlinedButton(onClick = onSaveFavorite) {
                    Text("Favorite")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun DataQuality.label(): String {
    return when (this) {
        DataQuality.Verified -> "Verified"
        DataQuality.Estimated -> "Estimated"
        DataQuality.Missing -> "Needs review"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MacroChipRow(
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroChip(label = "Calories", value = "${calories.roundToInt()} kcal", emphasis = true)
        MacroChip(label = "Protein", value = "${protein.roundToInt()} g")
        MacroChip(label = "Carbs", value = "${carbs.roundToInt()} g")
        MacroChip(label = "Fat", value = "${fat.roundToInt()} g")
    }
}

@Composable
private fun MacroChip(label: String, value: String, emphasis: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (emphasis) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
