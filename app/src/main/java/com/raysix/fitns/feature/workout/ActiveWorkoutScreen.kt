package com.raysix.fitns.feature.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.core.input.toUserDecimalOrNull
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
                .verticalScroll(rememberScrollState())
                .padding(FitNsDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val colors = MaterialTheme.colorScheme
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.primary,
                contentColor = colors.onPrimary,
                shadowElevation = 6.dp
            ) {
                Box(Modifier.background(Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.primary))).padding(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ActiveHeaderIconAction(onClick = onBack, description = "Back") {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                        Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(36.dp))
                        Text("No active current", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Choose a saved plan or guided workout to open the live training deck.", color = colors.onPrimary.copy(alpha = 0.82f))
                    }
                }
            }
            EmptyStateCard(
                title = "Ready when you are",
                message = "Return to workouts and start a route; your sets, timer and progression data will appear here."
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

    val availableExercises = uiState.exercises.filterNot { exercise ->
        session.exercises.any { it.exercise.id == exercise.id }
    }

    val focusedExerciseId = session.exercises.firstOrNull { exercise ->
        exercise.sets.any { it.completedAt == null }
    }?.id

    BoxWithConstraints(modifier = Modifier.fillMaxSize().imePadding()) {
        val useLandscapeDeck = maxWidth >= 700.dp && maxWidth > maxHeight
        if (useLandscapeDeck) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActiveWorkoutHeader(
                        session = session,
                        timer = uiState.restTimer,
                        isSaving = uiState.isSavingActiveSession,
                        compactRail = true,
                        onBack = onBack,
                        onFinish = requestFinish,
                        onDiscard = { showDiscardConfirmation = true },
                        onAddRestTime = onAddRestTime,
                        onPauseTimer = onPauseTimer,
                        onResumeTimer = onResumeTimer,
                        onSkipTimer = onSkipTimer
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.errorMessage?.let { message ->
                            item(key = "active-workout-error") { ErrorBanner(message = message) }
                        }
                        if (uiState.personalRecords.isNotEmpty()) {
                            item(key = "active-records") { ActiveWorkoutRecordCard(uiState = uiState) }
                        }
                        item(key = "active-add-exercise") {
                            AddExerciseCard(exercises = availableExercises, onAddExercise = onAddExercise)
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(0.66f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "active-deck-heading") {
                        SessionDeckHeading(session = session, compact = true)
                    }
                    if (session.exercises.isEmpty()) {
                        item(key = "active-empty") {
                            EmptyStateCard(
                                title = "Your training deck is empty",
                                message = "Choose an exercise from the movement dock to begin."
                            )
                        }
                    }
                    items(session.exercises, key = { it.id }) { activeExercise ->
                        ActiveExerciseCard(
                            activeExercise = activeExercise,
                            isFocused = activeExercise.id == focusedExerciseId,
                            onRemoveExercise = onRemoveExercise,
                            onMoveExercise = onMoveExercise,
                            onAddSet = onAddSet,
                            onDeleteSet = onDeleteSet,
                            onUpdateSet = onUpdateSet,
                            onToggleSetComplete = onToggleSetComplete
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActiveWorkoutHeader(
                    session = session,
                    timer = uiState.restTimer,
                    isSaving = uiState.isSavingActiveSession,
                    compactRail = false,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    onBack = onBack,
                    onFinish = requestFinish,
                    onDiscard = { showDiscardConfirmation = true },
                    onAddRestTime = onAddRestTime,
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                    onSkipTimer = onSkipTimer
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(
                        start = FitNsDimens.ScreenPadding,
                        end = FitNsDimens.ScreenPadding,
                        bottom = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.errorMessage?.let { message ->
                        item(key = "active-workout-error") { ErrorBanner(message = message) }
                    }
                    if (uiState.personalRecords.isNotEmpty()) {
                        item(key = "active-records") { ActiveWorkoutRecordCard(uiState = uiState) }
                    }
                    item(key = "active-deck-heading") { SessionDeckHeading(session = session, compact = false) }
                    if (session.exercises.isEmpty()) {
                        item(key = "active-empty") {
                            EmptyStateCard(
                                title = "Your training deck is empty",
                                message = "Choose an exercise below to begin."
                            )
                        }
                    }
                    items(session.exercises, key = { it.id }) { activeExercise ->
                        ActiveExerciseCard(
                            activeExercise = activeExercise,
                            isFocused = activeExercise.id == focusedExerciseId,
                            onRemoveExercise = onRemoveExercise,
                            onMoveExercise = onMoveExercise,
                            onAddSet = onAddSet,
                            onDeleteSet = onDeleteSet,
                            onUpdateSet = onUpdateSet,
                            onToggleSetComplete = onToggleSetComplete
                        )
                    }
                    item(key = "active-add-exercise") {
                        AddExerciseCard(exercises = availableExercises, onAddExercise = onAddExercise)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutRecordCard(uiState: WorkoutUiState) {
    ModernCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(25.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Personal-best wave", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Fresh records from this session", style = MaterialTheme.typography.bodySmall)
            }
        }
        uiState.personalRecords.forEach { record ->
            Text(
                "${record.exerciseName} · ${record.type.label} ${record.value.formatNumber()} ${record.unit}",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SessionDeckHeading(session: ActiveWorkoutSession, compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 2.dp else 0.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(7.dp).size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Live training", style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${session.completedSetCount}/${session.totalSetCount} sets · ${session.exercises.size} movements",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Outlined.Waves, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveWorkoutHeader(
    session: ActiveWorkoutSession,
    timer: RestTimerUiState,
    isSaving: Boolean,
    compactRail: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onAddRestTime: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit
) {
    val progress = if (session.totalSetCount == 0) 0f else session.completedSetCount.toFloat() / session.totalSetCount
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 5.dp
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.primary)))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActiveHeaderIconAction(onClick = onBack, description = "Back") {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(19.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LIVE WORKOUT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.onPrimary.copy(alpha = 0.72f))
                        Text(
                            session.name,
                            style = if (compactRail) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ActiveHeaderIconAction(onClick = onDiscard, description = "Discard workout") {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(19.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (session.completedSetCount == 0) {
                                "Complete a set to finish"
                            } else {
                                "${session.completedSetCount} of ${session.totalSetCount} sets · ${session.exercises.size} moves"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimary.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CompactProgressBar(
                            progress = progress,
                            trackColor = colors.onPrimary.copy(alpha = 0.2f),
                            progressColor = colors.onPrimary
                        )
                    }
                    Surface(
                        onClick = onFinish,
                        enabled = session.completedSetCount > 0 && !isSaving,
                        shape = RoundedCornerShape(999.dp),
                        color = colors.onPrimary,
                        contentColor = colors.primary
                    ) {
                        Text(
                            if (isSaving) "Saving…" else "Finish",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                        )
                    }
                }
                RestTimerPanel(
                    timer = timer,
                    compactRail = compactRail,
                    onAddRestTime = onAddRestTime,
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                    onSkipTimer = onSkipTimer
                )
            }
        }
    }
}

@Composable
private fun CompactProgressBar(
    progress: Float,
    trackColor: androidx.compose.ui.graphics.Color,
    progressColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(trackColor, RoundedCornerShape(999.dp))
    ) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(progressColor, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun ActiveHeaderIconAction(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = description
            role = Role.Button
        },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Box(Modifier.padding(9.dp), contentAlignment = Alignment.Center) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerPanel(
    timer: RestTimerUiState,
    compactRail: Boolean,
    onAddRestTime: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit
) {
    val timerProgress = if (timer.targetSeconds <= 0) 0f else timer.secondsRemaining.toFloat() / timer.targetSeconds
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.13f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val stackControls = compactRail || maxWidth < 330.dp
            if (timer.secondsRemaining <= 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Rest timer ready", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Starts after a set",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rest ${timer.secondsRemaining.formatTimer()}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                            CompactProgressBar(
                                progress = timerProgress,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                progressColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        TimerIconButton(
                            contentDescription = if (timer.isRunning) "Pause rest timer" else "Resume rest timer",
                            text = if (timer.isRunning) "Pause" else "Play",
                            icon = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            onClick = if (timer.isRunning) onPauseTimer else onResumeTimer
                        )
                        TimerIconButton(
                            contentDescription = "Skip rest timer",
                            text = "Skip",
                            icon = Icons.Filled.SkipNext,
                            onClick = onSkipTimer
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (stackControls) Arrangement.End else Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!stackControls) {
                            Text(
                                if (timer.isRunning) "Recovery in progress" else "Paused",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                            )
                        }
                        TimerIconButton(
                            contentDescription = "Subtract 15 seconds",
                            text = "−15",
                            enabled = timer.secondsRemaining > 0,
                            onClick = { onAddRestTime(-15) }
                        )
                        Spacer(Modifier.width(6.dp))
                        TimerIconButton("Add 15 seconds", "+15", onClick = { onAddRestTime(15) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerIconButton(
    contentDescription: String,
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Button
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (icon == null) 8.dp else 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
            if (icon == null) {
                Text(text = text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveExerciseCard(
    activeExercise: ActiveWorkoutExercise,
    isFocused: Boolean,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onAddSet: (String) -> Unit,
    onDeleteSet: (String, String) -> Unit,
    onUpdateSet: (String, String, Double?, Int?, Int?, Int?, WorkoutSetType?) -> Unit,
    onToggleSetComplete: (String, String) -> Unit
) {
    val completedSets = activeExercise.sets.count { it.completedAt != null }
    val progress = if (activeExercise.sets.isEmpty()) 0f else completedSets.toFloat() / activeExercise.sets.size
    val complete = activeExercise.sets.isNotEmpty() && completedSets == activeExercise.sets.size
    var showExerciseMenu by remember { mutableStateOf(false) }
    val containerColor = when {
        complete -> MaterialTheme.colorScheme.primaryContainer
        isFocused -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = if (complete) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = if (isFocused && !complete) 2.dp else 1.dp,
            color = if (isFocused && !complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (isFocused && !complete) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (complete) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(7.dp).size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            activeExercise.exercise.name,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isFocused && !complete) {
                            Text("NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        "${activeExercise.targetRepMin}-${activeExercise.targetRepMax} reps · ${activeExercise.restSeconds}s rest",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TagChip(text = "$completedSets/${activeExercise.sets.size}", accent = complete)
                Surface(
                    onClick = { onAddSet(activeExercise.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Set", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Box {
                    IconButton(onClick = { showExerciseMenu = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Exercise actions")
                    }
                    DropdownMenu(expanded = showExerciseMenu, onDismissRequest = { showExerciseMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Move up") },
                            leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
                            onClick = {
                                showExerciseMenu = false
                                onMoveExercise(activeExercise.id, -1)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                            onClick = {
                                showExerciseMenu = false
                                onMoveExercise(activeExercise.id, 1)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove exercise", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showExerciseMenu = false
                                onRemoveExercise(activeExercise.id)
                            }
                        )
                    }
                }
            }
            CompactProgressBar(
                progress = progress,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                progressColor = MaterialTheme.colorScheme.primary
            )
            if (activeExercise.sets.isEmpty()) {
                Text(
                    "No sets yet — add one to start logging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activeExercise.sets.forEach { set ->
                    ActiveSetRow(
                        activeExerciseId = activeExercise.id,
                        set = set,
                        onUpdateSet = onUpdateSet,
                        onDeleteSet = onDeleteSet,
                        onToggleSetComplete = onToggleSetComplete
                    )
                }
            }
        }
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
    val completed = set.completedAt != null
    var showDetails by rememberSaveable(set.id) {
        mutableStateOf(set.rpe != null || set.rir != null || set.setType != WorkoutSetType.Normal)
    }

    val weightField: @Composable (Modifier) -> Unit = { modifier ->
        SetNumberField(
            value = weight,
            label = "KG",
            isError = weight.isNotBlank() && !weightIsValid,
            modifier = modifier,
            onValueChange = {
                val normalized = it.normalizedUnsignedDecimalInputOrNull() ?: return@SetNumberField
                weight = normalized
                onUpdateSet(activeExerciseId, set.id, normalized.toUserDecimalOrNull(), reps.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), set.setType)
            }
        )
    }
    val repsField: @Composable (Modifier) -> Unit = { modifier ->
        SetNumberField(
            value = reps,
            label = "REPS",
            isError = reps.isNotBlank() && !repsAreValid,
            keyboardType = KeyboardType.Number,
            modifier = modifier,
            onValueChange = {
                if (!it.isUnsignedIntegerInput()) return@SetNumberField
                reps = it
                onUpdateSet(activeExerciseId, set.id, parsedWeight, it.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), set.setType)
            }
        )
    }
    val rpeField: @Composable (Modifier) -> Unit = { modifier ->
        SetNumberField(
            value = rpe,
            label = "RPE",
            isError = !rpeIsValid,
            keyboardType = KeyboardType.Number,
            modifier = modifier,
            onValueChange = {
                if (!it.isUnsignedIntegerInput()) return@SetNumberField
                rpe = it
                onUpdateSet(activeExerciseId, set.id, parsedWeight, reps.toIntOrNull(), it.toIntOrNull(), rir.toIntOrNull(), set.setType)
            }
        )
    }
    val rirField: @Composable (Modifier) -> Unit = { modifier ->
        SetNumberField(
            value = rir,
            label = "RIR",
            isError = !rirIsValid,
            keyboardType = KeyboardType.Number,
            modifier = modifier,
            onValueChange = {
                if (!it.isUnsignedIntegerInput()) return@SetNumberField
                rir = it
                onUpdateSet(activeExerciseId, set.id, parsedWeight, reps.toIntOrNull(), rpe.toIntOrNull(), it.toIntOrNull(), set.setType)
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = if (completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (completed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${set.setNumber}", fontWeight = FontWeight.Black)
                    }
                }
                weightField(Modifier.weight(1.15f))
                repsField(Modifier.weight(0.9f))
                IconButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (showDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (showDetails) "Hide set details" else "Edit effort and set type"
                    )
                }
                Surface(
                    onClick = { onToggleSetComplete(activeExerciseId, set.id) },
                    enabled = completed || canComplete,
                    modifier = Modifier.size(44.dp).semantics {
                        contentDescription = if (completed) "Mark set incomplete" else "Mark set complete"
                        role = Role.Button
                    },
                    shape = RoundedCornerShape(13.dp),
                    color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(21.dp))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    set.previousPerformance?.let { "Prev ${it.weightKg.formatNumber()} kg × ${it.repetitions}" } ?: "First log",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (set.setType != WorkoutSetType.Normal || set.rpe != null || set.rir != null) {
                    Text(
                        buildString {
                            if (set.setType != WorkoutSetType.Normal) append(set.setType.label)
                            set.rpe?.let { if (isNotEmpty()) append(" · "); append("RPE $it") }
                            set.rir?.let { if (isNotEmpty()) append(" · "); append("RIR $it") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showDetails) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth >= 350.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rpeField(Modifier.weight(0.8f))
                            rirField(Modifier.weight(0.8f))
                            SetTypeSelector(
                                selectedType = set.setType,
                                modifier = Modifier.weight(1.5f),
                                onSelect = { type ->
                                    onUpdateSet(activeExerciseId, set.id, parsedWeight, reps.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), type)
                                }
                            )
                            IconButton(onClick = { onDeleteSet(activeExerciseId, set.id) }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete set", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            rpeField(Modifier.weight(1f))
                            rirField(Modifier.weight(1f))
                            IconButton(onClick = { onDeleteSet(activeExerciseId, set.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete set", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        SetTypeSelector(
                            selectedType = set.setType,
                            modifier = Modifier.fillMaxWidth(),
                            onSelect = { type ->
                                onUpdateSet(activeExerciseId, set.id, parsedWeight, reps.toIntOrNull(), rpe.toIntOrNull(), rir.toIntOrNull(), type)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetTypeSelector(
    selectedType: WorkoutSetType,
    modifier: Modifier = Modifier,
    onSelect: (WorkoutSetType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(selectedType.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WorkoutSetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label, fontWeight = if (type == selectedType) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        expanded = false
                        onSelect(type)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseCard(exercises: List<Exercise>, onAddExercise: (Exercise) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(7.dp).size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add movement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (exercises.isEmpty()) "All exercises are already aboard" else "${exercises.size} available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
                if (exercises.isNotEmpty()) {
                    Surface(
                        onClick = { expanded = !expanded },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (expanded) "Close" else "Choose", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Icon(
                                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            if (expanded && exercises.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                exercises.sortedBy { it.name.lowercase() }.forEach { exercise ->
                    FilterChip(
                        selected = false,
                        onClick = { onAddExercise(exercise) },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(exercise.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
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
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.height(48.dp),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = textColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    innerTextField()
                }
            }
        }
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
