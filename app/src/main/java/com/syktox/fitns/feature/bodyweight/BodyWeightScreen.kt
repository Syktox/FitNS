package com.syktox.fitns.feature.bodyweight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.EmptyStateCard
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.core.design.SectionTitle
import com.syktox.fitns.domain.model.BodyWeightLogEntry
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Body Weight",
                subtitle = "Trend evaluation uses a moving average."
            )
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("New Entry", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onAddEntry(weight.toDoubleOrNull() ?: 0.0, notes)
                            weight = ""
                            notes = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }
                    uiState.errorMessage?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("Trend")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TrendMetric("Current", uiState.progress.currentKg?.formatKg() ?: "No data")
                        TrendMetric("7-day avg", uiState.sevenDayAverageKg?.formatKg() ?: "No data")
                        TrendMetric("Entries", uiState.entries.size.toString())
                    }
                    BodyWeightTrendChart(entries = uiState.entries)
                }
            }
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
private fun WeightProgressCard(progress: BodyWeightProgress) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Goal Progress")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Current")
                Text(progress.currentKg?.formatKg() ?: "No data")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Target")
                Text(progress.targetKg?.formatKg() ?: "Not set")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("30-day change")
                Text(progress.thirtyDayChangeKg?.formatSignedKg() ?: "No data")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total change")
                Text(progress.totalChangeKg?.formatSignedKg() ?: "No data")
            }
            Text(progress.summary, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BodyWeightEntryCard(entry: BodyWeightLogEntry, onDelete: () -> Unit) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.measuredAt.formatDate(), fontWeight = FontWeight.SemiBold)
                Text(entry.weightKg.formatKg())
            }
            if (entry.notes.isNotBlank()) {
                Text(entry.notes)
            }
            OutlinedButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun TrendMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BodyWeightTrendChart(entries: List<BodyWeightLogEntry>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
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
                strokeWidth = 2f
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
            strokeWidth = 1f
        )
        coordinates.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 4f
            )
        }
        coordinates.forEach { point ->
            drawCircle(color = lineColor, radius = 5f, center = point)
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
