package com.raysix.fitns.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.SectionTitle
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(uiState: ProgressUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(FitNsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(FitNsDimens.ContentSpacing)
    ) {
        item {
            ScreenHeader(
                title = "Progress",
                subtitle = "Trends across nutrition, weight, and workouts."
            )
        }
        item {
            ProgressSummaryCard(uiState.summary)
        }
        item {
            TrendCard(
                title = "Calories",
                valueLabel = "kcal",
                points = uiState.calories,
                emptyMessage = "Log meals for several days to see calorie trends."
            )
        }
        item {
            TrendCard(
                title = "Body Weight",
                valueLabel = "kg",
                points = uiState.bodyWeight,
                emptyMessage = "Log body weight entries to see your trend."
            )
        }
        item {
            TrendCard(
                title = "Workout Volume",
                valueLabel = "kg",
                points = uiState.workoutVolume,
                emptyMessage = "Log workouts to see weekly volume patterns."
            )
        }
    }
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
