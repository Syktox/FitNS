package com.raysix.fitns.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.SectionTitle
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(uiState: ProgressUiState) {
    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Progress",
                subtitle = "Trends across nutrition, weight, and workouts."
            )
        },
        main = {
            ProgressSummaryCard(uiState.summary)
            BodyWeightAnalyticsCard(uiState.bodyWeightAnalytics)
            TrendCard(
                title = "Calories",
                valueLabel = "kcal",
                points = uiState.calories,
                emptyMessage = "Log meals for several days to see calorie trends."
            )
            NutritionAnalyticsCard(uiState.nutritionAnalytics)
            TrendCard(
                title = "Workout Volume",
                valueLabel = "kg",
                points = uiState.workoutVolume,
                emptyMessage = "Log workouts to see weekly volume patterns."
            )
            MuscleGroupVolumeCard(uiState.muscleGroupVolume)
        },
        side = {
            TrendCard(
                title = "Body Weight",
                valueLabel = "kg",
                points = uiState.bodyWeight,
                emptyMessage = "Log body weight entries to see your trend."
            )
            TrendCard(
                title = "7-Day Weight Average",
                valueLabel = "kg",
                points = uiState.bodyWeightMovingAverage,
                emptyMessage = "Log at least a few body-weight entries to see a moving average."
            )
            StrengthProgressCard(uiState.strengthProgress)
        }
    )
}

@Composable
private fun ProgressSummaryCard(summary: ProgressSummary) {
    GradientHeroCard(brush = BrandGradient) {
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Snapshot",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = onPrimary.copy(alpha = 0.85f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroMetric("Avg Calories", summary.averageCalories?.roundToInt()?.toString() ?: "-", onPrimary, Modifier.weight(1f))
                HeroMetric("Weight", summary.latestWeightKg?.let { "${it.formatOne()} kg" } ?: "-", onPrimary, Modifier.weight(1f))
                HeroMetric("7d Volume", "${summary.weeklyVolumeKg.roundToInt()} kg", onPrimary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, onPrimary: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.85f))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onPrimary
        )
    }
}

@Composable
private fun BodyWeightAnalyticsCard(analytics: BodyWeightAnalytics) {
    SectionCard(title = "Body Weight Analytics") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsRow("Raw weight", analytics.latestWeightKg?.let { "${it.formatOne()} kg" } ?: "-")
            AnalyticsRow("7-day average", analytics.sevenDayAverageKg?.let { "${it.formatOne()} kg" } ?: "-")
            AnalyticsRow("30-day change", analytics.thirtyDayChangeKg?.signedKg() ?: "-")
            AnalyticsRow("Distance to target", analytics.distanceToTargetKg?.signedKg() ?: "-")
            AnalyticsRow("Weekly rate", analytics.estimatedWeeklyRateKg?.signedKg() ?: "-")
        }
    }
}

@Composable
private fun NutritionAnalyticsCard(windows: List<NutritionAnalyticsWindow>) {
    SectionCard(title = "Nutrition Analytics") {
        if (windows.isEmpty()) {
            Text("Log meals to see nutrition averages.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        windows.forEach { window ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${window.days} days", fontWeight = FontWeight.SemiBold)
                Text(
                    "${window.averageCalories.roundToInt()} kcal | P ${window.averageProtein.roundToInt()} g | C ${window.averageCarbs.roundToInt()} g | F ${window.averageFat.roundToInt()} g | Fiber ${window.averageFiber.roundToInt()} g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Goal adherence ${window.goalAdherencePercent.roundToInt()}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MuscleGroupVolumeCard(groups: List<MuscleGroupVolumeAnalytics>) {
    SectionCard(title = "Training Volume", subtitle = "Current week vs previous week") {
        if (groups.isEmpty()) {
            Text("Log workouts to compare weekly volume by muscle group.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        groups.forEach { group ->
            AnalyticsRow(
                label = group.muscleGroup,
                value = "${group.weeklySets} sets | ${group.weeklyVolumeKg.roundToInt()} kg | ${group.changePercent?.let { "${it.roundToInt()}%" } ?: "new"}"
            )
        }
    }
}

@Composable
private fun StrengthProgressCard(exercises: List<StrengthExerciseAnalytics>) {
    SectionCard(title = "Strength Progress") {
        if (exercises.isEmpty()) {
            Text("Log sets to see exercise-level strength progress.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        exercises.forEach { exercise ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exercise.exerciseName, fontWeight = FontWeight.SemiBold)
                Text(
                    "Est. 1RM ${exercise.estimatedOneRepMax.lastOrNull()?.value?.roundToInt() ?: 0} kg | Max ${exercise.maxWeightKg.roundToInt()} kg | Best ${exercise.bestReps} reps",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Volume ${exercise.volumeKg.roundToInt()} kg | ${exercise.workoutFrequency} sessions",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exercise.estimatedOneRepMax.size > 1) {
                    TrendLine(points = exercise.estimatedOneRepMax, lineColor = MaterialTheme.colorScheme.tertiary)
                }
                if (exercise.recentSessions.isNotEmpty()) {
                    Text(
                        "Recent: ${exercise.recentSessions.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TrendCard(title: String, valueLabel: String, points: List<TrendPoint>, emptyMessage: String) {
    if (points.isEmpty() || points.all { it.value == 0.0 }) {
        EmptyStateCard(title = title, message = emptyMessage)
        return
    }
    SectionCard(
        title = title,
        trailing = {
            Text(
                "${points.last().value.roundToInt()} $valueLabel",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrendLine(points = points, lineColor = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(points.first().label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(points.last().label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TrendLine(points: List<TrendPoint>, lineColor: Color) {
    val fillColor = lineColor.copy(alpha = 0.12f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
    ) {
        val values = points.map { it.value }
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = if (points.size <= 1) size.width else size.width / (points.size - 1)
        val mapped = values.mapIndexed { index, value ->
            val x = stepX * index
            val y = size.height - (((value - min) / range).toFloat() * size.height)
            Offset(x, y)
        }
        val path = Path().apply {
            moveTo(0f, size.height)
            mapped.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(
            path = path,
            color = fillColor
        )
        mapped.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
        }
        mapped.forEach { point ->
            drawCircle(color = lineColor, radius = 8f, center = point)
            drawCircle(
                color = surfaceColor,
                radius = 4f,
                center = point,
                style = Stroke(width = 2f)
            )
        }
    }
}

private fun Double.formatOne(): String {
    return "${(this * 10.0).roundToInt() / 10.0}"
}

private fun Double.signedKg(): String {
    val value = formatOne()
    return if (this > 0.0) "+$value kg" else "$value kg"
}
