package com.syktox.fitns.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.EmptyStateCard
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.core.design.SectionTitle
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(uiState: ProgressUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Snapshot")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric("Avg Calories", summary.averageCalories?.roundToInt()?.toString() ?: "-")
                SummaryMetric("Weight", summary.latestWeightKg?.let { "${it.formatOne()} kg" } ?: "-")
                SummaryMetric("7d Volume", "${summary.weeklyVolumeKg.roundToInt()} kg")
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrendCard(title: String, valueLabel: String, points: List<TrendPoint>, emptyMessage: String) {
    if (points.isEmpty() || points.all { it.value == 0.0 }) {
        EmptyStateCard(title = title, message = emptyMessage)
        return
    }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle(title)
                Text("${points.last().value.roundToInt()} $valueLabel", fontWeight = FontWeight.SemiBold)
            }
            TrendLine(points = points, lineColor = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(points.first().label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(points.last().label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrendLine(points: List<TrendPoint>, lineColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
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
        mapped.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 5f
            )
        }
        mapped.forEach { point ->
            drawCircle(color = lineColor, radius = 6f, center = point)
        }
    }
}

private fun Double.formatOne(): String {
    return "${(this * 10.0).roundToInt() / 10.0}"
}
