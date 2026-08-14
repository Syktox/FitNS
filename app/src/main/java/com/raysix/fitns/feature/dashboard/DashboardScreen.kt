package com.raysix.fitns.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.LabeledProgress
import com.raysix.fitns.core.design.MetricProgressBar
import com.raysix.fitns.core.design.MetricRing
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ProgressRing
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.core.design.StatCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.core.design.isWideScreen
import com.raysix.fitns.core.design.PillButton
import com.raysix.fitns.domain.model.DailyNutritionDashboard
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
    onAddWater: (Double) -> Unit,
    onRemoveWater: (Double) -> Unit,
    onOpenSettings: () -> Unit
) {
    val wide = isWideScreen()
    var showDetails by remember { mutableStateOf(false) }
    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Today",
                subtitle = "Your nutrition, movement, and next best action",
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Open settings")
                    }
                }
            )
        },
        main = {
            CalorieHeroCard(
                dashboard = dashboard,
                onAddFood = onAddFood,
                onStartWorkout = onStartWorkout
            )
            MacroRingsCard(dashboard = dashboard)
            WaterCard(dashboard = dashboard, message = message, onAddWater = onAddWater, onRemoveWater = onRemoveWater)
            WorkoutSummaryCard(workoutSummary = workoutSummary, onStartWorkout = onStartWorkout)
            if (mealBreakdown.isNotEmpty() && (wide || showDetails)) {
                MealBreakdownCard(meals = mealBreakdown)
            }
            if (!wide) {
                PillButton(
                    text = if (showDetails) "Hide insights" else "Show insights",
                    modifier = Modifier.fillMaxWidth(),
                    filled = false,
                    onClick = { showDetails = !showDetails }
                )
            }
        },
        side = {
            if (wide || showDetails) {
                DailyCoachCard(coach = coach)
                ReadinessCard(readiness = readiness, onStartWorkout = onStartWorkout)
                MicronutrientCard(micronutrients = micronutrients)
            }
        }
    )
}

@Composable
private fun CalorieHeroCard(
    dashboard: DailyNutritionDashboard,
    onAddFood: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val percent = if (dashboard.goal.caloriesKcal > 0) {
        (dashboard.total.caloriesKcal / dashboard.goal.caloriesKcal).toFloat()
    } else 0f
    val track = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f)
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    GradientHeroCard(brush = BrandGradient) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "TODAY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onPrimary.copy(alpha = 0.85f)
                    )
                    Text(
                        "Calories",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = onPrimary
                    )
                    Text(
                        "${dashboard.total.caloriesKcal.roundToInt()} of ${dashboard.goal.caloriesKcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onPrimary.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
                ProgressRing(
                    progress = percent,
                    modifier = Modifier.size(104.dp),
                    stroke = 12.dp,
                    color = onPrimary,
                    trackColor = track
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "${dashboard.remainingCalories.roundToInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = onPrimary
                        )
                        Text(
                            "left",
                            style = MaterialTheme.typography.labelMedium,
                            color = onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = onPrimary,
                trackColor = track,
                strokeCap = StrokeCap.Round,
                gapSize = 6.dp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroPillButton(
                    text = "Add Food",
                    filled = true,
                    modifier = Modifier.weight(1f),
                    onClick = onAddFood
                )
                HeroPillButton(
                    text = "Workout",
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = onStartWorkout
                )
            }
        }
    }
}

@Composable
private fun HeroPillButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (filled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        contentColor = if (filled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        border = if (filled) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
        }
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun MacroRingsCard(dashboard: DailyNutritionDashboard) {
    ModernCard {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Macros")
                TagChip(text = "Daily goals", accent = true)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroRing("Protein", dashboard.total.proteinGrams, dashboard.goal.proteinGrams, "g", Modifier.weight(1f))
                MacroRing("Carbs", dashboard.total.carbohydratesGrams, dashboard.goal.carbohydrateGrams, "g", Modifier.weight(1f))
                MacroRing("Fat", dashboard.total.fatGrams, dashboard.goal.fatGrams, "g", Modifier.weight(1f))
                MacroRing("Fiber", dashboard.total.fiberGrams, dashboard.goal.fiberGrams, "g", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroRing(
    label: String,
    value: Double,
    target: Double,
    unit: String,
    modifier: Modifier = Modifier
) {
    val percent = if (target > 0) (value / target).toFloat() else 0f
    val color = if (percent >= 1f) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    MetricRing(
        label = label,
        value = "${value.roundToInt()}/$unit",
        percent = percent,
        modifier = modifier,
        color = color
    )
}

@Composable
private fun WaterCard(
    dashboard: DailyNutritionDashboard,
    message: String?,
    onAddWater: (Double) -> Unit,
    onRemoveWater: (Double) -> Unit
) {
    val percent = if (dashboard.goal.waterMilliliters > 0) {
        (dashboard.waterMilliliters / dashboard.goal.waterMilliliters).toFloat()
    } else 0f

    ModernCard {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Hydration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${dashboard.waterMilliliters.roundToInt()} of ${dashboard.goal.waterMilliliters.roundToInt()} ml",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroPillButtonSmall("−250", { onRemoveWater(250.0) }, Modifier)
                        HeroPillButtonSmall("+250", { onAddWater(250.0) }, Modifier)
                        HeroPillButtonSmall("+500", { onAddWater(500.0) }, Modifier)
                    }
                }
                ProgressRing(
                    progress = percent,
                    modifier = Modifier.size(88.dp),
                    stroke = 11.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(percent * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            MetricProgressBar(
                progress = percent,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HeroPillButtonSmall(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReadinessCard(readiness: DashboardReadiness, onStartWorkout: () -> Unit) {
    SectionCard(title = "Readiness", trailing = { TagChip(text = readiness.status, accent = true) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(readiness.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(readiness.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("7-day sets", readiness.weeklySetCount.toString(), Modifier.weight(1f))
                StatCard(
                    "Last workout",
                    readiness.daysSinceLastWorkout?.let { "$it days ago" } ?: "No data",
                    Modifier.weight(1f)
                )
            }
            Surface(
                onClick = onStartWorkout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    "Open Workout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(workoutSummary: DashboardWorkoutSummary, onStartWorkout: () -> Unit) {
    SectionCard(
        title = "Workout Today",
        subtitle = workoutSummary.latestExerciseName?.let { "Latest: $it" } ?: "No workout logged yet.",
        trailing = {
            Text(
                "${workoutSummary.workoutCount} workouts",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Sets", workoutSummary.setCount.toString(), Modifier.weight(1f))
                StatCard("Volume", "${workoutSummary.volumeKg.roundToInt()} kg", Modifier.weight(1f))
            }
            Surface(
                onClick = onStartWorkout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    "Start Workout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun MicronutrientCard(micronutrients: List<NutrientAggregate>) {
    if (micronutrients.isEmpty()) {
        ModernCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Micronutrients")
                Text(
                    "Log foods with micronutrient data to see vitamin and mineral coverage against your targets.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    SectionCard(
        title = "Micronutrients",
        trailing = {
            TagChip(text = "${micronutrients.count { it.percent >= 1f }}/${micronutrients.size} met")
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    GradientHeroCard(brush = BrandGradient) {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        coach.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        coach.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
                ProgressRing(
                    progress = (coach.score / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.size(88.dp),
                    stroke = 10.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${coach.score}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    coach.focus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun MealBreakdownCard(meals: List<MealBreakdown>) {
    SectionCard(title = "Meal Balance") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            meals.forEach { meal ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(meal.label, fontWeight = FontWeight.SemiBold)
                        Text("${meal.entryCount} items · Protein ${meal.proteinGrams.roundToInt()} g", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${meal.caloriesKcal.roundToInt()} kcal",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
