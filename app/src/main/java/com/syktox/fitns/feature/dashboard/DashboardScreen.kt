package com.syktox.fitns.feature.dashboard

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.EmptyStateCard
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.core.design.SectionTitle
import com.syktox.fitns.domain.model.DailyNutritionDashboard
import com.syktox.fitns.domain.model.FoodLogEntry
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    dashboard: DailyNutritionDashboard,
    workoutSummary: DashboardWorkoutSummary,
    readiness: DashboardReadiness,
    coach: DashboardCoach,
    mealBreakdown: List<MealBreakdown>,
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Calories", fontWeight = FontWeight.SemiBold)
                        Text("${dashboard.total.caloriesKcal.roundToInt()} / ${dashboard.goal.caloriesKcal.roundToInt()} kcal")
                    }
                    Text(
                        "${dashboard.remainingCalories.roundToInt()} kcal remaining",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { (dashboard.total.caloriesKcal / dashboard.goal.caloriesKcal).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth()
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
        item {
            GoalMetricsCard(metrics = coach.metrics)
        }
        item {
            ReadinessCard(readiness = readiness, onStartWorkout = onStartWorkout)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Protein", dashboard.total.proteinGrams, dashboard.goal.proteinGrams, "g", Modifier.weight(1f))
                    MetricCard("Carbs", dashboard.total.carbohydratesGrams, dashboard.goal.carbohydrateGrams, "g", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Fat", dashboard.total.fatGrams, dashboard.goal.fatGrams, "g", Modifier.weight(1f))
                    MetricCard("Fiber", dashboard.total.fiberGrams, dashboard.goal.fiberGrams, "g", Modifier.weight(1f))
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Water", fontWeight = FontWeight.SemiBold)
                        Text("${dashboard.waterMilliliters.roundToInt()} / ${dashboard.goal.waterMilliliters.roundToInt()} ml")
                    }
                    LinearProgressIndicator(
                        progress = { (dashboard.waterMilliliters / dashboard.goal.waterMilliliters).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAddWater(250.0) }) {
                            Text("+250 ml")
                        }
                        OutlinedButton(onClick = { onAddWater(500.0) }) {
                            Text("+500 ml")
                        }
                    }
                    message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Workout Today", fontWeight = FontWeight.SemiBold)
                        Text("${workoutSummary.workoutCount} workouts")
                    }
                    Text("${workoutSummary.setCount} sets · ${workoutSummary.volumeKg.roundToInt()} kg volume")
                    Text(workoutSummary.latestExerciseName?.let { "Latest: $it" } ?: "No workout logged yet.")
                    OutlinedButton(onClick = onStartWorkout) {
                        Text("Start Workout")
                    }
                }
            }
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
                    message = "Add a meal to start seeing calories, macros, and trend guidance."
                )
            }
        }
        items(dashboard.entries) { entry ->
            FoodEntryCard(entry)
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Notes", fontWeight = FontWeight.SemiBold)
                    Text("Micronutrients with missing product data will be marked as 'Insufficient data' later.")
                    Text("Photo and barcode analyses are saved only after confirmation.")
                }
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
                Text(readiness.status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("7-day sets")
                Text(readiness.weeklySetCount.toString())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last workout")
                Text(readiness.daysSinceLastWorkout?.let { "$it days ago" } ?: "No data")
            }
            OutlinedButton(onClick = onStartWorkout) {
                Text("Open Workout")
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
            LinearProgressIndicator(progress = { coach.score / 100f }, modifier = Modifier.fillMaxWidth())
            Text(coach.focus, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun GoalMetricsCard(metrics: List<DashboardGoalMetric>) {
    if (metrics.isEmpty()) return
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Daily Targets")
            metrics.forEach { metric ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(metric.label)
                    Text(metric.status, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { metric.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
private fun MetricCard(label: String, value: Double, target: Double, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text("${value.roundToInt()} / ${target.roundToInt()} $unit")
            LinearProgressIndicator(
                progress = { (value / target).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth()
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
            Text("${entry.grams.roundToInt()} g · ${entry.mealType}")
            Text("Protein ${entry.nutrition.proteinGrams.roundToInt()} g · Carbs ${entry.nutrition.carbohydratesGrams.roundToInt()} g · Fat ${entry.nutrition.fatGrams.roundToInt()} g")
        }
    }
}
