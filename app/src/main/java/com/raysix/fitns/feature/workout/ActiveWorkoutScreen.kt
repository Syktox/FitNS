package com.raysix.fitns.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.PillButton
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.domain.model.ActiveWorkoutExercise
import com.raysix.fitns.domain.model.ActiveWorkoutSession
import com.raysix.fitns.domain.model.ActiveWorkoutSet
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutSetType
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveWorkoutScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onAddSet: (String) -> Unit,
    onDeleteSet: (String, String) -> Unit,
    onUpdateSet: (String, String, Double?, Int?, Int?, Int?, WorkoutSetType?) -> Unit,
    onToggleSetComplete: (String, String) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onAddRestTime: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit
) {
    val session = uiState.activeSession
    if (session == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FitNsDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            EmptyStateCard(
                title = "No active workout.",
                message = "Start a saved plan or template to open the live training view."
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FitNsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ActiveWorkoutHeader(
                session = session,
                timer = uiState.restTimer,
                onBack = onBack,
                onFinish = onFinish,
                onDiscard = onDiscard,
                onAddRestTime = onAddRestTime,
                onPauseTimer = onPauseTimer,
                onResumeTimer = onResumeTimer,
                onSkipTimer = onSkipTimer
            )
        }
        if (uiState.personalRecords.isNotEmpty()) {
            item {
                ModernCard(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text("New PR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    uiState.personalRecords.forEach { record ->
                        Text("${record.exerciseName}: ${record.type.label} ${record.value.formatNumber()} ${record.unit}")
                    }
                }
            }
        }
        items(session.exercises, key = { it.id }) { activeExercise ->
            ActiveExerciseCard(
                activeExercise = activeExercise,
                onRemoveExercise = onRemoveExercise,
                onMoveExercise = onMoveExercise,
                onAddSet = onAddSet,
                onDeleteSet = onDeleteSet,
                onUpdateSet = onUpdateSet,
                onToggleSetComplete = onToggleSetComplete
            )
        }
        item {
            AddExerciseCard(
                exercises = uiState.exercises.filterNot { exercise ->
                    session.exercises.any { it.exercise.id == exercise.id }
                },
                onAddExercise = onAddExercise
            )
        }
    }
}

@Composable
private fun ActiveWorkoutHeader(
    session: ActiveWorkoutSession,
    timer: RestTimerUiState,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onAddRestTime: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit
) {
    val progress = if (session.totalSetCount == 0) 0f else session.completedSetCount.toFloat() / session.totalSetCount
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(session.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${session.completedSetCount}/${session.totalSetCount} sets complete", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(text = "Discard", filled = false, onClick = onDiscard)
                PillButton(text = "Finish", onClick = onFinish)
            }
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        RestTimerPanel(
            timer = timer,
            onAddRestTime = onAddRestTime,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer,
            onSkipTimer = onSkipTimer
        )
    }
}

@Composable
private fun RestTimerPanel(
    timer: RestTimerUiState,
    onAddRestTime: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit
) {
    SectionCard(
        title = "Rest Timer",
        subtitle = if (timer.secondsRemaining > 0) timer.secondsRemaining.formatTimer() else "Ready"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TimerIconButton("Add 15 seconds", "+15", onClick = { onAddRestTime(15) })
            TimerIconButton("Subtract 15 seconds", "-15", onClick = { onAddRestTime(-15) })
            IconButton(onClick = if (timer.isRunning) onPauseTimer else onResumeTimer) {
                Icon(
                    imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (timer.isRunning) "Pause rest timer" else "Resume rest timer"
                )
            }
            IconButton(onClick = onSkipTimer) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Skip rest timer")
            }
        }
    }
}

@Composable
private fun TimerIconButton(contentDescription: String, text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveExerciseCard(
    activeExercise: ActiveWorkoutExercise,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onAddSet: (String) -> Unit,
    onDeleteSet: (String, String) -> Unit,
    onUpdateSet: (String, String, Double?, Int?, Int?, Int?, WorkoutSetType?) -> Unit,
    onToggleSetComplete: (String, String) -> Unit
) {
    SectionCard(
        title = activeExercise.exercise.name,
        subtitle = "${activeExercise.targetRepMin}-${activeExercise.targetRepMax} reps | ${activeExercise.restSeconds}s rest",
        trailing = {
            Row {
                IconButton(onClick = { onMoveExercise(activeExercise.id, -1) }) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move exercise up")
                }
                IconButton(onClick = { onMoveExercise(activeExercise.id, 1) }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move exercise down")
                }
                IconButton(onClick = { onRemoveExercise(activeExercise.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove exercise")
                }
            }
        }
    ) {
        activeExercise.sets.forEach { set ->
            ActiveSetRow(
                activeExerciseId = activeExercise.id,
                set = set,
                onUpdateSet = onUpdateSet,
                onDeleteSet = onDeleteSet,
                onToggleSetComplete = onToggleSetComplete
            )
        }
        PillButton(text = "Add Set", filled = false, onClick = { onAddSet(activeExercise.id) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveSetRow(
    activeExerciseId: String,
    set: ActiveWorkoutSet,
    onUpdateSet: (String, String, Double?, Int?, Int?, Int?, WorkoutSetType?) -> Unit,
    onDeleteSet: (String, String) -> Unit,
    onToggleSetComplete: (String, String) -> Unit
) {
    var weight by remember(set.id, set.weightKg) { mutableStateOf(set.weightKg.formatNumber()) }
    var reps by remember(set.id, set.repetitions) { mutableStateOf(set.repetitions.toString()) }
    var rpe by remember(set.id, set.rpe) { mutableStateOf(set.rpe?.toString().orEmpty()) }
    var rir by remember(set.id, set.rir) { mutableStateOf(set.rir?.toString().orEmpty()) }

    ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Set ${set.setNumber}", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = set.previousPerformance?.let { "Previous: ${it.weightKg.formatNumber()} kg x ${it.repetitions}" }
                            ?: "Previous: none",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { onToggleSetComplete(activeExerciseId, set.id) }) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = if (set.completedAt == null) "Mark set complete" else "Mark set incomplete",
                            tint = if (set.completedAt == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onDeleteSet(activeExerciseId, set.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete set")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SetNumberField(
                    value = weight,
                    label = "Weight",
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        weight = it
                        onUpdateSet(activeExerciseId, set.id, it.toDoubleOrNull(), reps.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), set.setType)
                    }
                )
                SetNumberField(
                    value = reps,
                    label = "Reps",
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        reps = it
                        onUpdateSet(activeExerciseId, set.id, weight.toDoubleOrNull(), it.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), set.setType)
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SetNumberField(
                    value = rpe,
                    label = "RPE",
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        rpe = it
                        onUpdateSet(activeExerciseId, set.id, weight.toDoubleOrNull(), reps.toIntOrNull(), it.toIntOrNull(), rir.toIntOrNull(), set.setType)
                    }
                )
                SetNumberField(
                    value = rir,
                    label = "RIR",
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        rir = it
                        onUpdateSet(activeExerciseId, set.id, weight.toDoubleOrNull(), reps.toIntOrNull(), rpe.toIntOrNull(), it.toIntOrNull(), set.setType)
                    }
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutSetType.entries.forEach { type ->
                    FilterChip(
                        selected = set.setType == type,
                        onClick = {
                            onUpdateSet(activeExerciseId, set.id, weight.toDoubleOrNull(), reps.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), type)
                        },
                        label = { Text(type.label) }
                    )
                }
            }
            if (set.completedAt != null) {
                TagChip(text = "Completed", accent = true)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseCard(exercises: List<Exercise>, onAddExercise: (Exercise) -> Unit) {
    SectionCard(title = "Add Exercise", subtitle = "Add movement during the workout") {
        if (exercises.isEmpty()) {
            Text("Every saved exercise is already in this workout.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEach { exercise ->
                    FilterChip(
                        selected = false,
                        onClick = { onAddExercise(exercise) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text(exercise.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetNumberField(
    value: String,
    label: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}

private fun Int.formatTimer(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun Double.formatNumber(): String {
    return if (this == roundToInt().toDouble()) {
        roundToInt().toString()
    } else {
        ((this * 10.0).roundToInt() / 10.0).toString()
    }
}
