package com.raysix.fitns.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(uiState: ProgressUiState, onBack: () -> Unit = {}) {
    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Deep progress",
                subtitle = "See the currents connecting nutrition, weight, and training.",
                actions = { TextButton(onClick = onBack) { Text("Back") } }
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
            ProgressCompassCard(uiState)
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = onPrimary.copy(alpha = 0.16f),
                    contentColor = onPrimary
                ) {
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.padding(9.dp).size(23.dp))
                }
                Column {
                    Text("Your current", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onPrimary)
                    Text("A compact view of the signals moving together", style = MaterialTheme.typography.bodySmall, color = onPrimary.copy(alpha = 0.8f))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 390.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroMetric("Daily energy", summary.averageCalories?.roundToInt()?.let { "$it kcal" } ?: "—", onPrimary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            HeroMetric("Latest weight", summary.latestWeightKg?.let { "${it.formatOne()} kg" } ?: "—", onPrimary, Modifier.weight(1f))
                            HeroMetric("7-day volume", "${summary.weeklyVolumeKg.roundToInt()} kg", onPrimary, Modifier.weight(1f))
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HeroMetric("Daily energy", summary.averageCalories?.roundToInt()?.let { "$it kcal" } ?: "—", onPrimary, Modifier.weight(1f))
                        HeroMetric("Latest weight", summary.latestWeightKg?.let { "${it.formatOne()} kg" } ?: "—", onPrimary, Modifier.weight(1f))
                        HeroMetric("7-day volume", "${summary.weeklyVolumeKg.roundToInt()} kg", onPrimary, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCompassCard(uiState: ProgressUiState) {
    val activeStreams = listOf(
        uiState.calories.isNotEmpty(),
        uiState.bodyWeight.isNotEmpty(),
        uiState.workoutVolume.isNotEmpty()
    ).count { it }
    SectionCard(
        title = "Progress compass",
        subtitle = "Consistency makes each chart more useful.",
        accent = true
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$activeStreams of 3 streams active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    when (activeStreams) {
                        3 -> "Nutrition, weight, and training are all in view."
                        0 -> "Start anywhere; your baseline builds one log at a time."
                        else -> "Keep logging to connect more of the picture."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
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
    SectionCard(title = "Weight depth", subtitle = "Smoothed direction beyond daily movement.") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsRow("Raw weight", analytics.latestWeightKg?.let { "${it.formatOne()} kg" } ?: "-")
            AnalyticsRow("7-day average", analytics.sevenDayAverageKg?.let { "${it.formatOne()} kg" } ?: "-")
            AnalyticsRow("30-day change", analytics.thirtyDayChangeKg?.signedKg() ?: "-")
            AnalyticsRow("Distance to target", analytics.distanceToTargetKg?.signedKg() ?: "-")
            AnalyticsRow("Weekly rate", analytics.estimatedWeeklyRateKg?.signedKg() ?: "-")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NutritionAnalyticsCard(windows: List<NutritionAnalyticsWindow>) {
    if (windows.isEmpty()) {
        EmptyStateCard(
            title = "Nutrition insights",
            message = "Log your first meal to see daily calorie and macro averages."
        )
        return
    }

    var selectedDays by rememberSaveable { mutableIntStateOf(windows.first().days) }
    val window = windows.firstOrNull { it.days == selectedDays } ?: windows.first()
    val loggedDayLabel = if (window.loggedDays == 1) "1 logged day" else "${window.loggedDays} logged days"

    SectionCard(
        title = "Nutrition current",
        subtitle = "Daily averages based on $loggedDayLabel"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            windows.forEach { option ->
                FilterChip(
                    selected = option.days == window.days,
                    onClick = { selectedDays = option.days },
                    label = { Text("${option.days} days") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Daily calorie average",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    window.averageCalories.roundToInt().toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    " kcal per day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        if (window.calorieGoal > 0.0) {
            val calorieDifference = window.averageCalories - window.calorieGoal
            LinearProgressIndicator(
                progress = { (window.averageCalories / window.calorieGoal).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                gapSize = 6.dp
            )
            Text(
                text = when {
                    abs(calorieDifference) < 1.0 -> "Right on your ${window.calorieGoal.roundToInt()} kcal goal"
                    calorieDifference > 0.0 -> "${calorieDifference.roundToInt()} kcal above your ${window.calorieGoal.roundToInt()} kcal goal"
                    else -> "${abs(calorieDifference).roundToInt()} kcal below your ${window.calorieGoal.roundToInt()} kcal goal"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Set a calorie goal in Profile to compare your average.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NutritionMetric("Protein", window.averageProtein, Modifier.weight(1f))
            NutritionMetric("Carbs", window.averageCarbs, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NutritionMetric("Fat", window.averageFat, Modifier.weight(1f))
            NutritionMetric("Fiber", window.averageFiber, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NutritionMetric(label: String, grams: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${grams.roundToInt()} g",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "daily average",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MuscleGroupVolumeCard(groups: List<MuscleGroupVolumeAnalytics>) {
    SectionCard(title = "Training tide", subtitle = "Current week compared with the previous wave") {
        if (groups.isEmpty()) {
            Text("Log workouts to compare weekly volume by muscle group.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        groups.forEach { group ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.muscleGroup, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${group.weeklySets} sets · ${group.weeklyVolumeKg.roundToInt()} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        group.changePercent?.let { "${it.roundToInt()}%" } ?: "New",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthProgressCard(exercises: List<StrengthExerciseAnalytics>) {
    SectionCard(title = "Strength current", subtitle = "How your strongest movements are travelling.") {
        if (exercises.isEmpty()) {
            Text("Log sets to see exercise-level strength progress.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        exercises.forEach { exercise ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 360.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontWeight = FontWeight.Medium)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.Medium)
                Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
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
            .height(112.dp)
            .semantics {
                val first = points.first()
                val last = points.last()
                contentDescription =
                    "Trend chart from ${first.value.roundToInt()} on ${first.label} to ${last.value.roundToInt()} on ${last.label}."
            }
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
