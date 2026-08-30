package com.raysix.fitns.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raysix.fitns.R
import com.raysix.fitns.core.design.AccentAmber
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.BrandGradientViolet
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.LabeledProgress
import com.raysix.fitns.core.design.MetricProgressBar
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.OceanBackdrop
import com.raysix.fitns.core.design.PillButton
import com.raysix.fitns.core.design.ProgressRing
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.core.design.StatCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.core.design.WhaleTailMark
import com.raysix.fitns.core.design.isCompactHeight
import com.raysix.fitns.core.design.isWideScreen
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
    onOpenSettings: () -> Unit
) {
    val wide = isWideScreen()
    val compactHeight = isCompactHeight()
    var showDetails by rememberSaveable { mutableStateOf(false) }

    OceanBackdrop {
        AdaptiveTwoColumn(
            header = {
                ScreenHeader(
                    title = "Today",
                    subtitle = if (compactHeight) "Your daily current" else "Nutrition, movement, and the next useful step",
                    actions = {
                        Surface(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Open settings",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                )
            },
            main = {
                CalorieHeroCard(dashboard, onAddFood, onStartWorkout)
                MacroGoalsCard(dashboard)
                WaterCard(dashboard, message, onAddWater)
                WorkoutSummaryCard(workoutSummary, onStartWorkout)
                if (mealBreakdown.isNotEmpty() && (wide || showDetails)) {
                    MealBreakdownCard(mealBreakdown)
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
                    DailyCoachCard(coach)
                    ReadinessCard(readiness, onStartWorkout)
                    MicronutrientCard(micronutrients)
                }
            }
        )
    }
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
    val onHero = MaterialTheme.colorScheme.onPrimary
    val track = onHero.copy(alpha = 0.24f)

    GradientHeroCard(brush = BrandGradient) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WhaleTailMark(Modifier.size(28.dp), tint = onHero.copy(alpha = 0.9f))
                Text(
                    "TODAY'S CURRENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = onHero.copy(alpha = 0.82f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Keep your rhythm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = onHero
                    )
                    Text(
                        "${dashboard.total.caloriesKcal.roundToInt()} of ${dashboard.goal.caloriesKcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onHero.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
                ProgressRing(
                    progress = percent,
                    modifier = Modifier.size(96.dp),
                    stroke = 11.dp,
                    color = onHero,
                    trackColor = track
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            dashboard.remainingCalories.roundToInt().toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = onHero
                        )
                        Text(
                            "remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = onHero.copy(alpha = 0.82f)
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = onHero,
                trackColor = track,
                strokeCap = StrokeCap.Round,
                gapSize = 6.dp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroAction("Add food", true, Modifier.weight(1f), onAddFood)
                HeroAction("Start workout", false, Modifier.weight(1f), onStartWorkout)
            }
        }
    }
}

@Composable
private fun HeroAction(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val onHero = MaterialTheme.colorScheme.onPrimary
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (filled) onHero else Color.Transparent,
        contentColor = if (filled) MaterialTheme.colorScheme.primary else onHero,
        border = if (filled) null else BorderStroke(1.5.dp, onHero.copy(alpha = 0.86f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 13.dp)
        )
    }
}

private data class MacroGoalItem(
    val label: String,
    val value: Double,
    val target: Double,
    val color: Color
)

@Composable
private fun MacroGoalsCard(dashboard: DailyNutritionDashboard) {
    val goals = listOf(
        MacroGoalItem("Protein", dashboard.total.proteinGrams, dashboard.goal.proteinGrams, MaterialTheme.colorScheme.tertiary),
        MacroGoalItem("Carbs", dashboard.total.carbohydratesGrams, dashboard.goal.carbohydrateGrams, MaterialTheme.colorScheme.primary),
        MacroGoalItem("Fat", dashboard.total.fatGrams, dashboard.goal.fatGrams, AccentAmber),
        MacroGoalItem("Fiber", dashboard.total.fiberGrams, dashboard.goal.fiberGrams, MaterialTheme.colorScheme.secondary)
    )

    ModernCard {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Daily macros")
                TagChip("Live", accent = true)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 420.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        goals.chunked(2).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowItems.forEach { goal ->
                                    MacroGoalTile(goal, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        goals.forEach { goal ->
                            MacroGoalTile(goal, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroGoalTile(goal: MacroGoalItem, modifier: Modifier = Modifier) {
    val percent = if (goal.target > 0) (goal.value / goal.target).toFloat() else 0f
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                goal.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${goal.value.roundToInt()} / ${goal.target.roundToInt()} g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MetricProgressBar(percent, Modifier.fillMaxWidth(), goal.color)
        }
    }
}

@Composable
private fun WaterCard(
    dashboard: DailyNutritionDashboard,
    message: String?,
    onAddWater: (Double) -> Unit
) {
    val percent = if (dashboard.goal.waterMilliliters > 0) {
        (dashboard.waterMilliliters / dashboard.goal.waterMilliliters).toFloat()
    } else 0f

    ModernCard {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hydration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${dashboard.waterMilliliters.roundToInt()} of ${dashboard.goal.waterMilliliters.roundToInt()} ml",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                ProgressRing(
                    progress = percent,
                    modifier = Modifier.size(72.dp),
                    stroke = 9.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        "${(percent * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            MetricProgressBar(percent, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WaterAction("+250", Modifier.weight(1f)) { onAddWater(250.0) }
                WaterAction("+500", Modifier.weight(1f)) { onAddWater(500.0) }
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun WaterAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 13.dp)
        )
    }
}

@Composable
private fun ReadinessCard(readiness: DashboardReadiness, onStartWorkout: () -> Unit) {
    SectionCard(title = "Readiness", trailing = { TagChip(readiness.status, accent = true) }) {
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
            PillButton("Open workout", Modifier.fillMaxWidth(), onStartWorkout)
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    workoutSummary: DashboardWorkoutSummary,
    onStartWorkout: () -> Unit
) {
    SectionCard(
        title = "Today's training",
        subtitle = workoutSummary.latestExerciseName?.let { "Latest: $it" } ?: "No workout logged yet",
        trailing = {
            Text(
                "${workoutSummary.workoutCount} total",
                style = MaterialTheme.typography.labelMedium,
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
            PillButton("Start workout", Modifier.fillMaxWidth(), onStartWorkout, filled = false)
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
                    "Log foods with vitamin and mineral data to see coverage against your targets.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    SectionCard(
        title = "Micronutrients",
        trailing = { TagChip("${micronutrients.count { it.percent >= 1f }}/${micronutrients.size} met") }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            micronutrients.take(6).forEach { aggregate ->
                LabeledProgress(
                    label = aggregate.label,
                    current = "${aggregate.consumed?.roundToInt() ?: 0} / ${aggregate.target?.roundToInt() ?: 0} ${aggregate.unit}",
                    progress = aggregate.percent,
                    barColor = if (aggregate.percent >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
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
    val onHero = MaterialTheme.colorScheme.onPrimary
    GradientHeroCard(brush = BrandGradientViolet) {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WhaleTailMark(Modifier.size(25.dp), tint = onHero.copy(alpha = 0.9f))
                Text(
                    "BLUE WHALE COACH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onHero.copy(alpha = 0.82f),
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    "${coach.score}/100",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = onHero,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    textAlign = TextAlign.End
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        coach.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onHero
                    )
                    Text(
                        coach.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHero.copy(alpha = 0.9f)
                    )
                }
                Image(
                    painter = painterResource(R.drawable.whale_coach),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp).size(82.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = onHero.copy(alpha = 0.15f),
                contentColor = onHero
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
    SectionCard(title = "Meal balance") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            meals.forEach { meal ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            meal.label,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${meal.entryCount} items · Protein ${meal.proteinGrams.roundToInt()} g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
