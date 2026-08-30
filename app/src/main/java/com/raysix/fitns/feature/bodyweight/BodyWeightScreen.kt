package com.raysix.fitns.feature.bodyweight

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveGutterLayout
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ProgressRing
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun BodyWeightScreen(
    uiState: BodyWeightUiState,
    onAddEntry: (Double, String) -> Unit,
    onDeleteEntry: (BodyWeightLogEntry) -> Unit,
    onBack: () -> Unit = {}
) {
    var weight by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDelete = uiState.entries.firstOrNull { it.id == pendingDeleteId }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete Weight Entry") },
            text = { Text("Remove the ${entry.weightKg.formatKg()} entry from ${entry.measuredAt.formatDate()}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntry(entry)
                        pendingDeleteId = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    AdaptiveGutterLayout(
        header = {
            ScreenHeader(
                title = "Weight current",
                subtitle = "Follow the direction, not the noise of a single weigh-in.",
                actions = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        gutter = {
            NewEntryCard(
                weight = weight,
                onWeightChange = { weight = it },
                notes = notes,
                onNotesChange = { notes = it },
                onSave = {
                    onAddEntry(weight.toUserDecimalOrNull() ?: 0.0, notes)
                    weight = ""
                    notes = ""
                },
                errorMessage = uiState.errorMessage
            )
        },
        main = {
            TrendSummaryCard(uiState)
            WeightProgressCard(progress = uiState.progress)
            if (uiState.entries.isEmpty()) {
                EmptyStateCard(
                    title = "Your first marker is waiting",
                    message = "Log a weigh-in to begin a steadier, tide-like view of your progress."
                )
            } else {
                uiState.entries.take(50).forEach { entry ->
                    BodyWeightEntryCard(entry, onDelete = { pendingDeleteId = entry.id })
                }
                if (uiState.entries.size > 50) {
                    Text("Showing the 50 most recent entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
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
    val parsedWeight = weight.toUserDecimalOrNull()
    val weightIsValid = parsedWeight != null && parsedWeight > 0.0 && parsedWeight <= 500.0

    SectionCard(
        title = "Log a weigh-in",
        subtitle = "A consistent rhythm gives the clearest signal.",
        accent = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Outlined.MonitorWeight,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }
                Text(
                    "Same time and conditions make trends easier to trust.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Weight kg") },
                isError = weight.isNotBlank() && !weightIsValid,
                supportingText = if (weight.isNotBlank() && !weightIsValid) {
                    { Text("Enter a weight between 0 and 500 kg.") }
                } else {
                    null
                },
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
                enabled = weightIsValid,
                shape = RoundedCornerShape(999.dp),
                color = if (weightIsValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (weightIsValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Add to the current",
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = onPrimary.copy(alpha = 0.16f),
                    contentColor = onPrimary
                ) {
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.padding(9.dp).size(23.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Your weight tide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onPrimary)
                    Text("Smoothed across recent check-ins", style = MaterialTheme.typography.bodySmall, color = onPrimary.copy(alpha = 0.8f))
                }
                ProgressRing(
                    progress = percent,
                    modifier = Modifier.size(72.dp),
                    stroke = 9.dp,
                    color = onPrimary,
                    trackColor = onPrimary.copy(alpha = 0.28f)
                ) {
                    Text(
                        "${(percent * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onPrimary
                    )
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 390.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrendHeroMetric("Current", uiState.progress.currentKg?.formatKg() ?: "No data", onPrimary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TrendHeroMetric("7-day average", uiState.sevenDayAverageKg?.formatKg() ?: "No data", onPrimary, Modifier.weight(1f))
                            TrendHeroMetric("Check-ins", uiState.entries.size.toString(), onPrimary, Modifier.weight(1f))
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TrendHeroMetric("Current", uiState.progress.currentKg?.formatKg() ?: "No data", onPrimary, Modifier.weight(1f))
                        TrendHeroMetric("7-day average", uiState.sevenDayAverageKg?.formatKg() ?: "No data", onPrimary, Modifier.weight(1f))
                        TrendHeroMetric("Check-ins", uiState.entries.size.toString(), onPrimary, Modifier.weight(0.7f))
                    }
                }
            }
            BodyWeightTrendChart(entries = uiState.entries, onPrimary = onPrimary, track = onPrimary.copy(alpha = 0.28f))
        }
    }
}

@Composable
private fun TrendHeroMetric(label: String, value: String, onPrimary: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.85f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onPrimary)
    }
}

@Composable
private fun WeightProgressCard(progress: BodyWeightProgress) {
    SectionCard(title = "Course to goal", subtitle = "Long-term direction from your logged trend.") {
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
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(progress.summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MonitorWeight, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.weightKg.formatKg(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(entry.measuredAt.formatDate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.notes.isNotBlank()) {
                    Text(entry.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete weight entry", tint = MaterialTheme.colorScheme.error)
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
            .height(96.dp)
            .padding(top = 8.dp)
            .semantics {
                contentDescription = if (points.isEmpty()) {
                    "Body weight trend. No measurements yet."
                } else {
                    "Body weight trend with ${points.size} measurements, from ${points.first().formatKg()} to ${points.last().formatKg()}."
                }
            }
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
