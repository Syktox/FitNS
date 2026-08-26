package com.raysix.fitns.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.PillButton
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.ActiveWorkoutExercise
import com.raysix.fitns.domain.model.ActiveWorkoutSession
import com.raysix.fitns.domain.model.ActiveWorkoutSet
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutSetType
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var showFinishConfirmation by rememberSaveable { mutableStateOf(false) }
    if (uiState.isSavingActiveSession) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Saving workout…") },
            text = { Text("Your completed sets and personal records are being saved safely.") },
            confirmButton = {}
        )
    }
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard workout?") },
            text = { Text("Completed and edited sets in this active workout will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirmation = false
                    onDiscard()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }) { Text("Keep workout") } }
        )
    }
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

    val incompleteSetCount = session.totalSetCount - session.completedSetCount
    if (showFinishConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmation = false },
            title = { Text("Finish workout?") },
            text = {
                Text(
                    "$incompleteSetCount incomplete ${if (incompleteSetCount == 1) "set" else "sets"} will not be saved to your history and will be discarded."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSavingActiveSession,
                    onClick = {
                        showFinishConfirmation = false
                        onFinish()
                    }
                ) {
                    Text("Save completed sets")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmation = false }) {
                    Text("Keep training")
                }
            }
        )
    }

    val requestFinish = {
        if (incompleteSetCount > 0 && session.completedSetCount > 0) {
            showFinishConfirmation = true
        } else {
            onFinish()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = FitNsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background) {
                ActiveWorkoutHeader(
                    session = session,
                    timer = uiState.restTimer,
                    isSaving = uiState.isSavingActiveSession,
                    onBack = onBack,
                    onFinish = requestFinish,
                    onDiscard = { showDiscardConfirmation = true },
                    onAddRestTime = onAddRestTime,
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                    onSkipTimer = onSkipTimer
                )
            }
        }
        uiState.errorMessage?.let { message ->
            item(key = "active-workout-error") {
                ErrorBanner(message = message)
            }
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
    isSaving: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onAddRestTime: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit
) {
    val progress = if (session.totalSetCount == 0) 0f else session.completedSetCount.toFloat() / session.totalSetCount
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${session.completedSetCount}/${session.totalSetCount} sets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDiscard) { Icon(Icons.Filled.Delete, contentDescription = "Discard workout", tint = MaterialTheme.colorScheme.error) }
            PillButton(
                text = if (isSaving) "Saving…" else "Finish",
                onClick = onFinish,
                enabled = !isSaving
            )
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
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (timer.secondsRemaining > 0) "Rest ${timer.secondsRemaining.formatTimer()}" else "Ready",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TimerIconButton("Subtract 15 seconds", "−15", onClick = { onAddRestTime(-15) })
            TimerIconButton("Add 15 seconds", "+15", onClick = { onAddRestTime(15) })
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
        modifier = Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Button
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
    val parsedWeight = weight.toUserDecimalOrNull()
    val parsedReps = reps.toIntOrNull()
    val parsedRpe = rpe.toIntOrNull()
    val parsedRir = rir.toIntOrNull()
    val weightIsValid = parsedWeight?.let { it.isFinite() && it >= 0.0 } == true
    val repsAreValid = parsedReps?.let { it > 0 } == true
    val rpeIsValid = rpe.isBlank() || parsedRpe?.let { it in 1..10 } == true
    val rirIsValid = rir.isBlank() || parsedRir?.let { it in 0..10 } == true
    val canComplete = weightIsValid && repsAreValid && rpeIsValid && rirIsValid

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
                    IconButton(
                        enabled = set.completedAt != null || canComplete,
                        onClick = { onToggleSetComplete(activeExerciseId, set.id) }
                    ) {
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
                    isError = weight.isNotBlank() && !weightIsValid,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        val normalized = it.normalizedUnsignedDecimalInputOrNull()
                            ?: return@SetNumberField
                        weight = normalized
                        onUpdateSet(activeExerciseId, set.id, normalized.toUserDecimalOrNull(), reps.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), set.setType)
                    }
                )
                SetNumberField(
                    value = reps,
                    label = "Reps",
                    isError = reps.isNotBlank() && !repsAreValid,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        if (!it.isUnsignedIntegerInput()) return@SetNumberField
                        reps = it
                        onUpdateSet(activeExerciseId, set.id, parsedWeight, it.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), set.setType)
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SetNumberField(
                    value = rpe,
                    label = "RPE",
                    isError = !rpeIsValid,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        if (!it.isUnsignedIntegerInput()) return@SetNumberField
                        rpe = it
                        onUpdateSet(activeExerciseId, set.id, parsedWeight, reps.toIntOrNull(), it.toIntOrNull(), rir.toIntOrNull(), set.setType)
                    }
                )
                SetNumberField(
                    value = rir,
                    label = "RIR",
                    isError = !rirIsValid,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        if (!it.isUnsignedIntegerInput()) return@SetNumberField
                        rir = it
                        onUpdateSet(activeExerciseId, set.id, parsedWeight, reps.toIntOrNull(), rpe.toIntOrNull(), it.toIntOrNull(), set.setType)
                    }
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutSetType.entries.forEach { type ->
                    FilterChip(
                        selected = set.setType == type,
                        onClick = {
                            onUpdateSet(activeExerciseId, set.id, parsedWeight, reps.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), type)
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
    isError: Boolean,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        isError = isError,
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

internal fun String.normalizedUnsignedDecimalInputOrNull(): String? {
    val normalized = replace(',', '.')
    return normalized.takeIf {
        it.isEmpty() || (it.count { character -> character == '.' } <= 1 && it.all { character -> character.isDigit() || character == '.' })
    }
}

private fun String.isUnsignedIntegerInput(): Boolean {
    return all(Char::isDigit)
}
