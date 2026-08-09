package com.raysix.fitns.feature.bodyweight

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ProgressRing
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun BodyWeightScreen(
    uiState: BodyWeightUiState,
    onAddEntry: (Double, String) -> Unit,
    onDeleteEntry: (BodyWeightLogEntry) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<BodyWeightLogEntry?>(null) }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Weight Entry") },
            text = { Text("Remove the ${entry.weightKg.formatKg()} entry from ${entry.measuredAt.formatDate()}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntry(entry)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(FitNsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(FitNsDimens.ContentSpacing)
    ) {
        item {
            ScreenHeader(
                title = "Body Weight",
                subtitle = "Trend evaluation uses a moving average."
            )
        }
        item {
            NewEntryCard(
                weight = weight,
                onWeightChange = { weight = it },
                notes = notes,
                onNotesChange = { notes = it },
                onSave = {
                    onAddEntry(weight.toDoubleOrNull() ?: 0.0, notes)
                    weight = ""
                    notes = ""
                },
                errorMessage = uiState.errorMessage
            )
        }
        item {
            TrendSummaryCard(uiState)
        }
        item {
            WeightProgressCard(progress = uiState.progress)
        }
        if (uiState.entries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No weight entries yet.",
                    message = "Log your first weigh-in to start seeing a smoother trend."
                )
            }
        }
        items(uiState.entries) { entry ->
            BodyWeightEntryCard(entry, onDelete = { pendingDelete = entry })
        }
    }
}

@Composable
private fun NewEntryCard(
    weight: String,
    onWeightChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    errorMessage: String?
) {
    SectionCard(title = "New Entry") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Weight kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                onClick = onSave,
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 13.dp)
                )
            }
            errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TrendSummaryCard(uiState: BodyWeightUiState) {
    val progress = uiState.progress
    val percent = if (progress.currentKg != null && progress.targetKg != null) {
        val change = kotlin.math.abs(progress.totalChangeKg ?: 0.0)
        val remaining = kotlin.math.abs(progress.remainingToTargetKg ?: 0.0)
        if (change + remaining <= 0.0) 1f else (change / (change + remaining)).toFloat()
    } else {
        0f
    }
    GradientHeroCard(brush = BrandGradient) {
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Trend", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = onPrimary.copy(alpha = 0.85f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TrendHeroMetric("Current", uiState.progress.currentKg?.formatKg() ?: "No data", onPrimary, Modifier.weight(1f))
                    TrendHeroMetric("7-day avg", uiState.sevenDayAverageKg?.formatKg() ?: "No data", onPrimary, Modifier.weight(1f))
                    TrendHeroMetric("Entries", uiState.entries.size.toString(), onPrimary, Modifier.weight(1f))
                }
            }
            ProgressRing(
                progress = percent,
                modifier = Modifier.size(84.dp),
                stroke = 10.dp,
                color = onPrimary,
                trackColor = onPrimary.copy(alpha = 0.28f)
            ) {
                Text(
                    "${(percent * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary
                )
            }
        }
        BodyWeightTrendChart(entries = uiState.entries, onPrimary = onPrimary, track = onPrimary.copy(alpha = 0.28f))
    }
}

@Composable
private fun TrendHeroMetric(label: String, value: String, onPrimary: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.85f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = onPrimary)
    }
}

@Composable
private fun WeightProgressCard(progress: BodyWeightProgress) {
    SectionCard(title = "Goal Progress") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalRow("Current", progress.currentKg?.formatKg() ?: "No data")
            GoalRow("Target", progress.targetKg?.formatKg() ?: "Not set")
            GoalRow("30-day change", progress.thirtyDayChangeKg?.formatSignedKg() ?: "No data")
            GoalRow("Total change", progress.totalChangeKg?.formatSignedKg() ?: "No data")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    progress.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = if (value.startsWith("+") || value.startsWith("-")) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun BodyWeightEntryCard(entry: BodyWeightLogEntry, onDelete: () -> Unit) {
    ModernCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(entry.measuredAt.formatDate(), fontWeight = FontWeight.SemiBold)
                    if (entry.notes.isNotBlank()) {
                        Text(entry.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    entry.weightKg.formatKg(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Surface(
                onClick = onDelete,
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.error
            ) {
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun BodyWeightTrendChart(
    entries: List<BodyWeightLogEntry>,
    onPrimary: androidx.compose.ui.graphics.Color,
    track: androidx.compose.ui.graphics.Color
) {
    val guideColor = track
    val points = entries
        .asReversed()
        .takeLast(30)
        .map { it.weightKg }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .padding(top = 8.dp)
    ) {
        if (points.size < 2) {
            drawLine(
                color = guideColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val min = points.minOrNull() ?: return@Canvas
        val max = points.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val coordinates = points.mapIndexed { index, value ->
            val x = if (points.size == 1) 0f else size.width * index / (points.lastIndex.toFloat())
            val normalized = ((value - min) / range).toFloat()
            val y = size.height - (normalized * size.height)
            Offset(x, y.coerceIn(0f, size.height))
        }

        drawLine(
            color = guideColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        coordinates.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = onPrimary,
                start = start,
                end = end,
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
        coordinates.forEach { point ->
            drawCircle(color = onPrimary, radius = 6f, center = point)
        }
    }
}

private fun Double.formatKg(): String {
    return "${(this * 10.0).roundToInt() / 10.0} kg"
}

private fun Double.formatSignedKg(): String {
    val rounded = (this * 10.0).roundToInt() / 10.0
    val prefix = if (rounded > 0.0) "+" else ""
    return "$prefix$rounded kg"
}

private fun Long.formatDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateFormatter)
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a")
