package com.raysix.fitns.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.LabeledProgress
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.core.design.StatCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.NutrientAggregate
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    dashboard: DailyNutritionDashboard,
    workoutSummary: DashboardWorkoutSummary,
    readiness: DashboardReadiness,
    coach: DashboardCoach,
    mealBreakdown: List<MealBreakdown>,
    micronutrients: List<NutrientAggregate>,
    message: String?,
    onAddFood: () -> Unit,
    onStartWorkout: () -> Unit,
    onAddWater: (Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "FitNS",
                subtitle = "Daily nutrition and strength workout status"
            )
        }
        item {
            DailyCoachCard(coach = coach)
        }
        item {
            CalorieCard(
                dashboard = dashboard,
                onAddFood = onAddFood,
                onStartWorkout = onStartWorkout
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroMetric("Protein", dashboard.total.proteinGrams, dashboard.goal.proteinGrams, "g", Modifier.weight(1f))
                MacroMetric("Carbs", dashboard.total.carbohydratesGrams, dashboard.goal.carbohydrateGrams, "g", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroMetric("Fat", dashboard.total.fatGrams, dashboard.goal.fatGrams, "g", Modifier.weight(1f))
                MacroMetric("Fiber", dashboard.total.fiberGrams, dashboard.goal.fiberGrams, "g", Modifier.weight(1f))
            }
        }
        item {
            WaterCard(dashboard = dashboard, message = message, onAddWater = onAddWater)
        }
        item {
            ReadinessCard(readiness = readiness, onStartWorkout = onStartWorkout)
        }
        item {
            WorkoutSummaryCard(workoutSummary = workoutSummary, onStartWorkout = onStartWorkout)
        }
        item {
            MicronutrientCard(micronutrients = micronutrients)
        }
        if (mealBreakdown.isNotEmpty()) {
            item {
                MealBreakdownCard(meals = mealBreakdown)
            }
        }
        item {
            SectionTitle("Today's Entries")
        }
        if (dashboard.entries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No foods logged yet.",
                    message = "Add a meal to start seeing calories, macros, and micronutrient coverage."
                )
            }
        }
        items(dashboard.entries) { entry ->
            FoodEntryCard(entry)
        }
    }
}

@Composable
private fun CalorieCard(
    dashboard: DailyNutritionDashboard,
    onAddFood: () -> Unit,
    onStartWorkout: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Calories", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${dashboard.total.caloriesKcal.roundToInt()} / ${dashboard.goal.caloriesKcal.roundToInt()} kcal",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    "${dashboard.remainingCalories.roundToInt()} kcal left",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            LabeledProgress(
                label = "Daily goal",
                current = "${(dashboard.total.caloriesKcal / dashboard.goal.caloriesKcal * 100).roundToInt()}%",
                progress = (dashboard.total.caloriesKcal / dashboard.goal.caloriesKcal).toFloat()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddFood) {
                    Text("Add Food")
                }
                OutlinedButton(onClick = onStartWorkout) {
                    Text("Workout")
                }
            }
        }
    }
}

@Composable
private fun WaterCard(dashboard: DailyNutritionDashboard, message: String?, onAddWater: (Double) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Water", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${dashboard.waterMilliliters.roundToInt()} / ${dashboard.goal.waterMilliliters.roundToInt()} ml",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAddWater(250.0) }) {
                        Text("+250 ml")
                    }
                    OutlinedButton(onClick = { onAddWater(500.0) }) {
                        Text("+500 ml")
                    }
                }
            }
            LabeledProgress(
                label = "Hydration",
                current = "${(dashboard.waterMilliliters / dashboard.goal.waterMilliliters * 100).roundToInt()}%",
                progress = (dashboard.waterMilliliters / dashboard.goal.waterMilliliters).toFloat()
            )
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ReadinessCard(readiness: DashboardReadiness, onStartWorkout: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(readiness.title, fontWeight = FontWeight.SemiBold)
                    Text(readiness.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TagChip(text = readiness.status, accent = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("7-day sets", readiness.weeklySetCount.toString(), Modifier.weight(1f))
                StatCard(
                    "Last workout",
                    readiness.daysSinceLastWorkout?.let { "$it days ago" } ?: "No data",
                    Modifier.weight(1f)
                )
            }
            OutlinedButton(onClick = onStartWorkout, modifier = Modifier.fillMaxWidth()) {
                Text("Open Workout")
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(workoutSummary: DashboardWorkoutSummary, onStartWorkout: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Workout Today", fontWeight = FontWeight.SemiBold)
                    Text(workoutSummary.latestExerciseName?.let { "Latest: $it" } ?: "No workout logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${workoutSummary.workoutCount} workouts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Sets", workoutSummary.setCount.toString(), Modifier.weight(1f))
                StatCard("Volume", "${workoutSummary.volumeKg.roundToInt()} kg", Modifier.weight(1f))
            }
            OutlinedButton(onClick = onStartWorkout, modifier = Modifier.fillMaxWidth()) {
                Text("Start Workout")
            }
        }
    }
}

@Composable
private fun MicronutrientCard(micronutrients: List<NutrientAggregate>) {
    if (micronutrients.isEmpty()) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Micronutrients")
                Text(
                    "Log foods with micronutrient data to see vitamin and mineral coverage against your targets.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Micronutrients")
                TagChip(text = "${micronutrients.count { it.percent >= 1f }}/${micronutrients.size} met")
            }
            micronutrients.take(6).forEach { aggregate ->
                LabeledProgress(
                    label = aggregate.label,
                    current = aggregate.consumed?.roundToInt().let { "$it / ${aggregate.target?.roundToInt()} ${aggregate.unit}" },
                    progress = aggregate.percent,
                    barColor = if (aggregate.percent >= 1f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                )
            }
            if (micronutrients.size > 6) {
                Text(
                    "+${micronutrients.size - 6} more in Nutrition",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyCoachCard(coach: DashboardCoach) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(coach.title, fontWeight = FontWeight.SemiBold)
                    Text(coach.summary)
                }
                Text("${coach.score}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            LabeledProgress(label = "Day score", current = "${coach.score}/100", progress = coach.score / 100f)
            Text(coach.focus, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun MealBreakdownCard(meals: List<MealBreakdown>) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Meal Balance")
            meals.forEach { meal ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(meal.label, fontWeight = FontWeight.SemiBold)
                        Text("${meal.entryCount} items · Protein ${meal.proteinGrams.roundToInt()} g")
                    }
                    Text("${meal.caloriesKcal.roundToInt()} kcal")
                }
            }
        }
    }
}

@Composable
private fun MacroMetric(label: String, value: Double, target: Double, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text("${value.roundToInt()}/${target.roundToInt()} $unit", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LabeledProgress(
                label = "Goal",
                current = "${(value / target * 100).roundToInt()}%",
                progress = (value / target).toFloat()
            )
        }
    }
}

@Composable
private fun FoodEntryCard(entry: FoodLogEntry) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.name, fontWeight = FontWeight.SemiBold)
                Text("${entry.nutrition.caloriesKcal.roundToInt()} kcal")
            }
            Text("${entry.grams.roundToInt()} g · ${entry.mealType}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Protein ${entry.nutrition.proteinGrams.roundToInt()} g · Carbs ${entry.nutrition.carbohydratesGrams.roundToInt()} g · Fat ${entry.nutrition.fatGrams.roundToInt()} g")
        }
    }
}
