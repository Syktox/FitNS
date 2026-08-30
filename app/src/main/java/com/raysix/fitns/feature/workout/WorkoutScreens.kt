package com.raysix.fitns.feature.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.AdaptiveColumn
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ProgressRing
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.core.design.isWideScreen
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutPlan
import com.raysix.fitns.domain.model.WorkoutPlanExercise
import com.raysix.fitns.domain.model.WorkoutTemplate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val KnownEquipmentTypes = listOf(
    "Machine",
    "Cable",
    "Smith Machine",
    "Plate Loaded",
    "Barbell",
    "Dumbbell",
    "Kettlebell",
    "Bodyweight",
    "Resistance Band",
    "Suspension Trainer",
    "Cardio Machine",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutStartScreen(
    uiState: WorkoutUiState,
    onSavePlan: (String, String, List<Exercise>, Int, Int, Int, Int) -> Unit,
    onUpdatePlan: (WorkoutPlan, String, String, List<Exercise>, Int, Int, Int, Int) -> Unit,
    onStartPlan: (WorkoutPlan) -> Unit,
    onStartTemplate: (WorkoutTemplate) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit,
    onShowHistory: () -> Unit,
    onViewExercises: () -> Unit,
    onLogExercise: (Exercise) -> Unit
) {
    var activeTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeTemplate = uiState.templates.firstOrNull { it.id == activeTemplateId }
    var activePlanId by rememberSaveable { mutableStateOf<String?>(null) }
    val activePlan = uiState.plans.firstOrNull { it.id == activePlanId }
    var completedPlanExerciseIds by rememberSaveable(activePlanId) { mutableStateOf(emptySet<String>()) }
    var showPlanBuilder by rememberSaveable { mutableStateOf(false) }
    var editingPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingPlan = uiState.plans.firstOrNull { it.id == editingPlanId }
    var planName by rememberSaveable { mutableStateOf("My Workout Plan") }
    var planFocus by rememberSaveable { mutableStateOf("Strength and consistency") }
    var selectedPlanExerciseIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var planSets by rememberSaveable { mutableStateOf("3") }
    var planRepMin by rememberSaveable { mutableStateOf("8") }
    var planRepMax by rememberSaveable { mutableStateOf("12") }
    var planRestSeconds by rememberSaveable { mutableStateOf("90") }

    activeTemplate?.let { template ->
        WorkoutTemplatePreviewDialog(
            template = template,
            onDismiss = { activeTemplateId = null },
            onStart = {
                activeTemplateId = null
                onStartTemplate(template)
            }
        )
    }

    AdaptiveTwoColumn(
        header = {
            WorkoutLaunchHero(
                stats = uiState.weeklyStats,
                onShowHistory = onShowHistory,
                onViewExercises = onViewExercises
            )
        },
        main = {
            uiState.errorMessage?.let { message ->
                ModernCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(message, fontWeight = FontWeight.SemiBold)
                }
            }
            activePlan?.let { plan ->
                ActivePlanCard(
                    plan = plan,
                    completedExerciseIds = completedPlanExerciseIds,
                    onToggleComplete = { exercise ->
                        completedPlanExerciseIds = if (exercise.id in completedPlanExerciseIds) {
                            completedPlanExerciseIds - exercise.id
                        } else {
                            completedPlanExerciseIds + exercise.id
                        }
                    },
                    onChooseExercise = { planExercise -> onLogExercise(planExercise.exercise) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Training plans", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Your route through the next session", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ActionPill(
                    text = if (showPlanBuilder) "Close" else "New plan",
                    filled = !showPlanBuilder,
                    onClick = {
                        if (showPlanBuilder) {
                            showPlanBuilder = false
                            editingPlanId = null
                        } else {
                            planName = "My Workout Plan"
                            planFocus = "Strength and consistency"
                            selectedPlanExerciseIds = emptyList()
                            planSets = "3"
                            planRepMin = "8"
                            planRepMax = "12"
                            planRestSeconds = "90"
                            editingPlanId = null
                            showPlanBuilder = true
                        }
                    }
                )
            }
            if (showPlanBuilder) {
                PlanBuilderCard(
                    title = if (editingPlan == null) "Build a training current" else "Refine your plan",
                    exercises = uiState.exercises,
                    planName = planName,
                    onPlanNameChange = { planName = it },
                    planFocus = planFocus,
                    onPlanFocusChange = { planFocus = it },
                    selectedExerciseIds = selectedPlanExerciseIds,
                    onToggleExercise = { exercise ->
                        selectedPlanExerciseIds = if (exercise.id in selectedPlanExerciseIds) {
                            selectedPlanExerciseIds - exercise.id
                        } else {
                            selectedPlanExerciseIds + exercise.id
                        }
                    },
                    onMoveSelectedExercise = { exerciseId, direction ->
                        val currentIndex = selectedPlanExerciseIds.indexOf(exerciseId)
                        if (currentIndex >= 0) {
                            val targetIndex = (currentIndex + direction)
                                .coerceIn(0, selectedPlanExerciseIds.lastIndex)
                            if (currentIndex != targetIndex) {
                                selectedPlanExerciseIds = selectedPlanExerciseIds.toMutableList().also { list ->
                                    val item = list.removeAt(currentIndex)
                                    list.add(targetIndex, item)
                                }
                            }
                        }
                    },
                    targetSets = planSets,
                    onTargetSetsChange = { planSets = it },
                    targetRepMin = planRepMin,
                    onTargetRepMinChange = { planRepMin = it },
                    targetRepMax = planRepMax,
                    onTargetRepMaxChange = { planRepMax = it },
                    restSeconds = planRestSeconds,
                    onRestSecondsChange = { planRestSeconds = it },
                    onSave = {
                        val selectedExercises = selectedPlanExerciseIds.mapNotNull { exerciseId ->
                            uiState.exercises.firstOrNull { it.id == exerciseId }
                        }
                        val planToEdit = editingPlan
                        if (planToEdit == null) {
                            onSavePlan(
                                planName,
                                planFocus,
                                selectedExercises,
                                planSets.toIntOrNull() ?: 3,
                                planRepMin.toIntOrNull() ?: 8,
                                planRepMax.toIntOrNull() ?: 12,
                                planRestSeconds.toIntOrNull() ?: 90
                            )
                        } else {
                            onUpdatePlan(
                                planToEdit,
                                planName,
                                planFocus,
                                selectedExercises,
                                planSets.toIntOrNull() ?: 3,
                                planRepMin.toIntOrNull() ?: 8,
                                planRepMax.toIntOrNull() ?: 12,
                                planRestSeconds.toIntOrNull() ?: 90
                            )
                        }
                        selectedPlanExerciseIds = emptyList()
                        editingPlanId = null
                        showPlanBuilder = false
                    }
                )
            }
            if (uiState.plans.isEmpty()) {
                EmptyStateCard(
                    title = "No route charted yet",
                    message = "Create a reusable plan and make the next workout effortless to start."
                )
            } else {
                uiState.plans.forEach { plan ->
                    WorkoutPlanCard(
                        plan = plan,
                        selected = activePlan?.id == plan.id,
                        onStart = {
                            activePlanId = plan.id
                            completedPlanExerciseIds = emptySet()
                            onStartPlan(plan)
                        },
                        onEdit = {
                            editingPlanId = plan.id
                            planName = plan.name
                            planFocus = plan.focus
                            selectedPlanExerciseIds = plan.exercises.map { it.exercise.id }
                            val firstExercise = plan.exercises.firstOrNull()
                            planSets = (firstExercise?.targetSets ?: 3).toString()
                            planRepMin = (firstExercise?.targetRepMin ?: 8).toString()
                            planRepMax = (firstExercise?.targetRepMax ?: 12).toString()
                            planRestSeconds = (firstExercise?.restSeconds ?: 90).toString()
                            showPlanBuilder = true
                        },
                        onDelete = { onDeletePlan(plan) }
                    )
                }
            }
        },
        side = {
            if (uiState.personalRecords.isNotEmpty()) {
                PersonalRecordWaveCard(uiState = uiState)
            }
            if (uiState.templates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Guided dives", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Ready-made sessions for decisive days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.templates, key = { it.id }) { template ->
                        WorkoutTemplateCard(
                            template = template,
                            selected = activeTemplate?.id == template.id,
                            onClick = { activeTemplateId = template.id },
                            onStart = { onStartTemplate(template) }
                        )
                    }
                }
            }
            ExerciseLibraryDock(onViewExercises = onViewExercises)
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutLaunchHero(
    stats: WorkoutWeeklyStats,
    onShowHistory: () -> Unit,
    onViewExercises: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(colors.primary, colors.tertiary, colors.primary)
                    )
                )
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.onPrimary.copy(alpha = 0.14f),
                        contentColor = colors.onPrimary
                    ) {
                        Icon(
                            Icons.Outlined.Waves,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp).size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "BLUE WHALE TRAINING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary.copy(alpha = 0.78f)
                        )
                        Text("Make the next set count", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(
                            stats.topExercise?.let { "Your strongest current: $it" }
                                ?: "Build depth through calm, consistent work.",
                            color = colors.onPrimary.copy(alpha = 0.86f)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OceanHeroMetric("WORKOUTS", stats.workoutCount.toString(), Modifier.weight(1f))
                    OceanHeroMetric("SETS", stats.setCount.toString(), Modifier.weight(1f))
                    OceanHeroMetric("VOLUME", "${stats.volumeKg.roundToInt()} kg", Modifier.weight(1f))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OceanHeroAction("History", Icons.Filled.History, onShowHistory)
                    OceanHeroAction("Exercise library", Icons.Filled.FitnessCenter, onViewExercises)
                }
            }
        }
    }
}

@Composable
private fun OceanHeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OceanHeroAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PersonalRecordWaveCard(uiState: WorkoutUiState) {
    ModernCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(26.dp))
            Column {
                Text("Personal-best wave", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Momentum worth remembering", style = MaterialTheme.typography.bodySmall)
            }
        }
        uiState.personalRecords.forEach { record ->
            Text(
                "${record.exerciseName} · ${record.type.label} ${record.value.roundToInt()} ${record.unit}",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExerciseLibraryDock(onViewExercises: () -> Unit) {
    ModernCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(30.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Exercise library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Log a set or tune your movement catalog", style = MaterialTheme.typography.bodySmall)
            }
            ActionPill(text = "Open", filled = true, onClick = onViewExercises)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerCard(
    seconds: Int,
    onStart: () -> Unit,
    onAdd15Seconds: () -> Unit,
    onAdd30Seconds: () -> Unit,
    onReset: () -> Unit
) {
    val target = 90
    val progress = if (seconds in 1..target) seconds.toFloat() / target else 0f
    ModernCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        BoxWithConstraints {
            val horizontal = maxWidth >= 440.dp
            val timerVisual: @Composable () -> Unit = {
                ProgressRing(
                    progress = progress,
                    modifier = Modifier.size(78.dp),
                    stroke = 9.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                ) {
                    Text(
                        seconds.formatTimer(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            val details: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (seconds > 0) "Recovery current" else "Rest timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (seconds > 0) "Breathe, reset, return strong." else "Start a 90-second recovery when you need it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ActionPill(
                            text = if (seconds > 0) "Reset" else "Start 90s",
                            filled = true,
                            onClick = if (seconds > 0) onReset else onStart
                        )
                        ActionPill(text = "+15s", filled = false, onClick = onAdd15Seconds, enabled = seconds > 0)
                        ActionPill(text = "+30s", filled = false, onClick = onAdd30Seconds, enabled = seconds > 0)
                    }
                }
            }
            if (horizontal) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    timerVisual()
                    Box(Modifier.weight(1f)) { details() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        timerVisual()
                        Text(
                            if (seconds > 0) "Hold the line. Your next set is approaching." else "Recovery is part of the work.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    details()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlanBuilderCard(
    title: String,
    exercises: List<Exercise>,
    planName: String,
    onPlanNameChange: (String) -> Unit,
    planFocus: String,
    onPlanFocusChange: (String) -> Unit,
    selectedExerciseIds: List<String>,
    onToggleExercise: (Exercise) -> Unit,
    onMoveSelectedExercise: (String, Int) -> Unit,
    targetSets: String,
    onTargetSetsChange: (String) -> Unit,
    targetRepMin: String,
    onTargetRepMinChange: (String) -> Unit,
    targetRepMax: String,
    onTargetRepMaxChange: (String) -> Unit,
    restSeconds: String,
    onRestSecondsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val setsValue = targetSets.toIntOrNull()
    val repMinValue = targetRepMin.toIntOrNull()
    val repMaxValue = targetRepMax.toIntOrNull()
    val restValue = restSeconds.toIntOrNull()
    val planValuesAreValid = setsValue?.let { it >= 1 } == true &&
        repMinValue?.let { it >= 1 } == true &&
        repMaxValue?.let { max -> max >= repMinValue } == true &&
        restValue?.let { it >= 0 } == true

    ModernCard(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose the sequence once, then flow through it in training.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                    )
                }
                TagChip(text = "${selectedExerciseIds.size} moves", accent = selectedExerciseIds.isNotEmpty())
            }
            OutlinedTextField(
                value = planName,
                onValueChange = onPlanNameChange,
                label = { Text("Plan name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = planFocus,
                onValueChange = onPlanFocusChange,
                label = { Text("Focus") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEach { exercise ->
                    FilterChip(
                        selected = exercise.id in selectedExerciseIds,
                        onClick = { onToggleExercise(exercise) },
                        label = { Text(exercise.name) }
                    )
                }
            }
            selectedExerciseIds.forEachIndexed { index, exerciseId ->
                val exercise = exercises.firstOrNull { it.id == exerciseId } ?: return@forEachIndexed
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(exercise.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                exercise.muscleGroup,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            CompactReorderButton(text = "↑", description = "Move ${exercise.name} up", onClick = { onMoveSelectedExercise(exerciseId, -1) })
                            CompactReorderButton(text = "↓", description = "Move ${exercise.name} down", onClick = { onMoveSelectedExercise(exerciseId, 1) })
                        }
                    }
                }
            }
            PlanParameterGrid(
                targetSets = targetSets,
                onTargetSetsChange = onTargetSetsChange,
                targetRepMin = targetRepMin,
                onTargetRepMinChange = onTargetRepMinChange,
                targetRepMax = targetRepMax,
                onTargetRepMaxChange = onTargetRepMaxChange,
                restSeconds = restSeconds,
                onRestSecondsChange = onRestSecondsChange
            )
            PrimaryPillButton(
                text = "Save training route",
                enabled = selectedExerciseIds.isNotEmpty() && planName.isNotBlank() && planValuesAreValid,
                onClick = onSave
            )
        }
    }
}

@Composable
private fun PlanParameterGrid(
    targetSets: String,
    onTargetSetsChange: (String) -> Unit,
    targetRepMin: String,
    onTargetRepMinChange: (String) -> Unit,
    targetRepMax: String,
    onTargetRepMaxChange: (String) -> Unit,
    restSeconds: String,
    onRestSecondsChange: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 560.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField(targetSets, onTargetSetsChange, "Sets", Modifier.weight(1f))
                NumericField(targetRepMin, onTargetRepMinChange, "Min reps", Modifier.weight(1f))
                NumericField(targetRepMax, onTargetRepMaxChange, "Max reps", Modifier.weight(1f))
                NumericField(restSeconds, onRestSecondsChange, "Rest sec", Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericField(targetSets, onTargetSetsChange, "Sets", Modifier.weight(1f))
                    NumericField(targetRepMin, onTargetRepMinChange, "Min reps", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericField(targetRepMax, onTargetRepMaxChange, "Max reps", Modifier.weight(1f))
                    NumericField(restSeconds, onRestSecondsChange, "Rest sec", Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutPlanCard(
    plan: WorkoutPlan,
    selected: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModernCard(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(plan.focus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TagChip(text = if (selected) "Active" else "Saved", accent = selected)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPlanMetric("MOVES", plan.exercises.size.toString(), Modifier.weight(1f))
                CompactPlanMetric("TIME", "${plan.estimatedMinutes} min", Modifier.weight(1f))
                CompactPlanMetric(
                    "SETS",
                    plan.exercises.sumOf { it.targetSets }.toString(),
                    Modifier.weight(1f)
                )
            }
            Text(
                text = plan.exercises.joinToString { "${it.exercise.name} ${it.targetSets}x${it.targetRepMin}-${it.targetRepMax}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(text = if (selected) "Resume" else "Start", filled = true, onClick = onStart)
                ActionPill(text = "Edit", filled = false, onClick = onEdit)
                ActionPill(text = "Delete", filled = false, onClick = onDelete, danger = true)
            }
        }
    }
}

@Composable
private fun CompactPlanMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivePlanCard(
    plan: WorkoutPlan,
    completedExerciseIds: Set<String>,
    onToggleComplete: (Exercise) -> Unit,
    onChooseExercise: (WorkoutPlanExercise) -> Unit
) {
    val progress = if (plan.exercises.isEmpty()) 0f else completedExerciseIds.size.toFloat() / plan.exercises.size
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(28.dp), color = colors.primary, contentColor = colors.onPrimary, shadowElevation = 6.dp) {
        Box(Modifier.background(Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.primary))).padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LIVE TRAINING ROUTE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.onPrimary.copy(alpha = 0.74f))
                        Text(plan.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("Keep moving through the list—one calm set at a time.", style = MaterialTheme.typography.bodySmall, color = colors.onPrimary.copy(alpha = 0.82f))
                    }
                    ProgressRing(
                        progress = progress,
                        modifier = Modifier.size(62.dp),
                        stroke = 8.dp,
                        color = colors.onPrimary,
                        trackColor = colors.onPrimary.copy(alpha = 0.24f)
                    ) {
                        Text("${completedExerciseIds.size}/${plan.exercises.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.onPrimary,
                    trackColor = colors.onPrimary.copy(alpha = 0.24f)
                )
                plan.exercises.forEachIndexed { index, item ->
                    val completed = item.exercise.id in completedExerciseIds
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.onPrimary.copy(alpha = if (completed) 0.1f else 0.16f),
                        contentColor = colors.onPrimary
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", fontWeight = FontWeight.Black, color = colors.onPrimary.copy(alpha = 0.7f))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.exercise.name, fontWeight = FontWeight.SemiBold, color = if (completed) colors.onPrimary.copy(alpha = 0.62f) else colors.onPrimary)
                                Text(
                                    "${item.targetSets} sets · ${item.targetRepMin}-${item.targetRepMax} reps · ${item.restSeconds}s rest",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                HeroPillSmall(text = "Log", filled = true, onClick = { onChooseExercise(item) })
                                HeroPillSmall(text = if (completed) "Done" else "Mark", filled = false, onClick = { onToggleComplete(item.exercise) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTemplateCard(
    template: WorkoutTemplate,
    selected: Boolean,
    onClick: () -> Unit,
    onStart: () -> Unit
) {
    ModernCard(
        modifier = Modifier
            .widthIn(min = 238.dp, max = 286.dp),
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Waves, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                template.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 44.dp)
            )
            Text(
                template.focus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 40.dp)
            )
            Text(
                "${template.exercises.size} exercises · ${template.estimatedMinutes} min",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SecondaryPillButton(text = "Preview", onClick = onClick)
            PrimaryPillButton(text = "Dive in", onClick = onStart)
        }
    }
}

@Composable
private fun WorkoutTemplatePreviewDialog(
    template: WorkoutTemplate,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val useSideBySideLayout = maxWidth >= 700.dp
            Surface(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp
            ) {
                if (useSideBySideLayout) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        WorkoutTemplatePreviewSummary(template, onDismiss, onStart, Modifier.weight(0.42f))
                        WorkoutTemplateExerciseList(template, Modifier.weight(0.58f).fillMaxHeight())
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WorkoutTemplatePreviewHeading(template)
                        WorkoutTemplateExerciseList(template, Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { SecondaryPillButton(text = "Close", onClick = onDismiss) }
                            Box(Modifier.weight(1f)) { PrimaryPillButton(text = "Start workout", onClick = onStart) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTemplatePreviewSummary(
    template: WorkoutTemplate,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WorkoutTemplatePreviewHeading(template)
        Text(
            "A clear route lets you spend your energy on the work, not the setup.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(42.dp))
                Text("Route charted", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${template.exercises.size} movements in about ${template.estimatedMinutes} minutes",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f)
                )
            }
        }
        SecondaryPillButton(text = "Close", onClick = onDismiss)
        PrimaryPillButton(text = "Start workout", onClick = onStart)
    }
}

@Composable
private fun WorkoutTemplatePreviewHeading(template: WorkoutTemplate) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.padding(11.dp).size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(template.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(template.focus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${template.exercises.size} exercises · ${template.estimatedMinutes} min",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WorkoutTemplateExerciseList(template: WorkoutTemplate, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyColumn(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(template.exercises, key = { it.id }) { exercise ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exercise.name, fontWeight = FontWeight.SemiBold)
                            Text(exercise.muscleGroup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentTypeDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Equipment type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            KnownEquipmentTypes.forEach { equipment ->
                DropdownMenuItem(
                    text = { Text(equipment) },
                    onClick = {
                        onValueChange(equipment)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun WorkoutHistoryScreen(
    history: List<WorkoutLogEntry>,
    progressionHint: (WorkoutLogEntry) -> String,
    onDeleteWorkout: (WorkoutLogEntry) -> Unit,
    onBack: () -> Unit = {}
) {
    val visibleHistory = history.sortedByDescending { it.loggedAt }.take(100)
    var pendingDelete by remember { mutableStateOf<WorkoutLogEntry?>(null) }

    pendingDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Workout") },
            text = { Text("Delete this entire workout session? All exercises recorded in the same session will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWorkout(workout)
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

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (isWideScreen()) 32.dp else 16.dp,
            vertical = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "history-header") {
            WorkoutHistoryHero(history = visibleHistory, onBack = onBack)
        }
        if (history.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "history-empty") {
                EmptyStateCard(
                    title = "The water is still calm",
                    message = "Complete your first workout to create a trail of sets, volume and progress."
                )
            }
        } else {
            itemsIndexed(
                items = visibleHistory,
                key = { index, workout -> "${workout.id}:${workout.exercise.id}:${workout.loggedAt}:$index" }
            ) { _, workout ->
                HistoryEntryCard(
                    workout = workout,
                    progressionHint = progressionHint,
                    onDelete = { pendingDelete = workout }
                )
            }
        }
        if (history.size > visibleHistory.size) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "history-limit") {
                Text(
                    "Showing the 100 most recent exercises",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistoryHero(history: List<WorkoutLogEntry>, onBack: () -> Unit) {
    val totalSets = history.sumOf { workout -> workout.sets.sumOf { it.sets } }
    val totalVolume = history.sumOf { it.volumeKg }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 6.dp
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.primary)))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        color = colors.onPrimary.copy(alpha = 0.14f),
                        contentColor = colors.onPrimary
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TRAINING LOG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.onPrimary.copy(alpha = 0.74f))
                        Text("Your wake of progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Every finished set leaves useful data behind.", color = colors.onPrimary.copy(alpha = 0.82f))
                    }
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(34.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OceanHeroMetric("ENTRIES", history.size.toString(), Modifier.weight(1f))
                    OceanHeroMetric("SETS", totalSets.toString(), Modifier.weight(1f))
                    OceanHeroMetric("VOLUME", "${totalVolume.roundToInt()} kg", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(workout: WorkoutLogEntry, progressionHint: (WorkoutLogEntry) -> String, onDelete: () -> Unit) {
    ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(9.dp).size(21.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(workout.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(workout.exercise.muscleGroup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(workout.loggedAt.formatDate(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    workout.durationMinutes?.takeIf { it > 0 }?.let { duration ->
                        Text(
                            "$duration min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPlanMetric("SETS", workout.sets.sumOf { it.sets }.toString(), Modifier.weight(1f))
                CompactPlanMetric("VOLUME", "${workout.volumeKg.roundToInt()} kg", Modifier.weight(1f))
                CompactPlanMetric("LOADS", workout.sets.size.toString(), Modifier.weight(1f))
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    workout.sets.take(5).forEachIndexed { index, set ->
                        val setPrefix = if (set.sets > 1) "${set.sets} sets" else "Set ${index + 1}"
                        val effort = listOfNotNull(set.rpe?.let { "RPE $it" }, set.rir?.let { "RIR $it" }).joinToString(" · ")
                        Text(
                            buildString {
                                append("$setPrefix  ${set.weightKg.formatExerciseWeight()} kg × ${set.repetitions}")
                                if (effort.isNotBlank()) append("  ·  $effort")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (workout.sets.size > 5) {
                        Text("+${workout.sets.size - 5} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(progressionHint(workout), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            if (workout.notes.isNotBlank()) {
                Text(workout.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ActionPill(text = "Delete entry", filled = false, onClick = onDelete, danger = true)
        }
    }
}

@Composable
fun ExerciseLibraryScreen(
    exercises: List<Exercise>,
    errorMessage: String?,
    exerciseToLogId: String?,
    onExerciseToLogConsumed: () -> Unit,
    onAddExercise: (String, String, String, String) -> Unit,
    onAddWorkout: (Exercise, Double, Int, Int, Int?, String) -> Unit,
    onBack: () -> Unit
) {
    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedExercise = exercises.firstOrNull { it.id == selectedExerciseId }
    var showAddExercise by rememberSaveable { mutableStateOf(false) }
    var exerciseName by rememberSaveable { mutableStateOf("") }
    var muscleGroup by rememberSaveable { mutableStateOf("") }
    var equipmentType by rememberSaveable { mutableStateOf("Machine") }
    var gym by rememberSaveable { mutableStateOf("") }
    var restSeconds by rememberSaveable { mutableIntStateOf(0) }
    var exerciseToLogDialogId by rememberSaveable { mutableStateOf<String?>(null) }
    val exerciseToLog = exercises.firstOrNull { it.id == exerciseToLogDialogId }
    var logWeight by rememberSaveable { mutableStateOf("0") }
    var logReps by rememberSaveable { mutableStateOf("10") }
    var logSets by rememberSaveable { mutableStateOf("3") }
    var logRpe by rememberSaveable { mutableStateOf("8") }
    var logNotes by rememberSaveable { mutableStateOf("") }
    val openLogDialog: (Exercise) -> Unit = { exercise ->
        exerciseToLogDialogId = exercise.id
        logWeight = (exercise.lastWeightKg ?: 0.0).roundToInt().toString()
        logReps = (exercise.lastRepetitions ?: 10).toString()
        logSets = (exercise.lastSets ?: 3).toString()
        logRpe = "8"
        logNotes = ""
    }
    val saveExercise = {
        onAddExercise(exerciseName, muscleGroup, equipmentType, gym)
        exerciseName = ""
        muscleGroup = ""
        equipmentType = "Machine"
        gym = ""
        showAddExercise = false
    }

    LaunchedEffect(restSeconds) {
        if (restSeconds > 0) {
            delay(1000)
            restSeconds -= 1
        }
    }

    LaunchedEffect(exerciseToLogId, exercises) {
        exercises.firstOrNull { it.id == exerciseToLogId }?.let { exercise ->
            openLogDialog(exercise)
            onExerciseToLogConsumed()
        }
    }

    exerciseToLog?.let { exercise ->
        ExerciseLogDialog(
            exercise = exercise,
            weight = logWeight,
            onWeightChange = { logWeight = it },
            reps = logReps,
            onRepsChange = { logReps = it },
            sets = logSets,
            onSetsChange = { logSets = it },
            rpe = logRpe,
            onRpeChange = { logRpe = it },
            notes = logNotes,
            onNotesChange = { logNotes = it },
            onDismiss = { exerciseToLogDialogId = null },
            onSave = {
                onAddWorkout(
                    exercise,
                    logWeight.toUserDecimalOrNull() ?: 0.0,
                    logReps.toIntOrNull() ?: 0,
                    logSets.toIntOrNull() ?: 1,
                    logRpe.toIntOrNull(),
                    logNotes
                )
                exerciseToLogDialogId = null
                logNotes = ""
                restSeconds = 90
            }
        )
    }

    val restTimer: @Composable () -> Unit = {
        RestTimerCard(
            seconds = restSeconds,
            onStart = { restSeconds = 90 },
            onAdd15Seconds = { restSeconds += 15 },
            onAdd30Seconds = { restSeconds += 30 },
            onReset = { restSeconds = 0 }
        )
    }
    val header: @Composable () -> Unit = {
        WorkoutLibraryHeader(
            exerciseCount = exercises.size,
            showAddExercise = showAddExercise,
            onToggleAdd = {
                selectedExerciseId = null
                showAddExercise = !showAddExercise
            },
            onBack = onBack
        )
    }

    if (isWideScreen()) {
        AdaptiveTwoColumn(
            header = header,
            main = {
                if (showAddExercise) {
                    ExerciseAddCard(
                        exerciseName = exerciseName,
                        onExerciseNameChange = { exerciseName = it },
                        muscleGroup = muscleGroup,
                        onMuscleGroupChange = { muscleGroup = it },
                        equipmentType = equipmentType,
                        onEquipmentTypeChange = { equipmentType = it },
                        gym = gym,
                        onGymChange = { gym = it },
                        onSave = saveExercise
                    )
                }
                errorMessage?.let { ExerciseLibraryError(it) }
                restTimer()
                ExerciseNameList(
                    exercises = exercises,
                    selectedExerciseId = selectedExerciseId,
                    onSelect = { selectedExerciseId = it.id },
                    onLog = openLogDialog
                )
            },
            side = {
                selectedExercise?.let { ExerciseDetailOverview(it) }
                    ?: EmptyStateCard(
                        title = "Select an exercise",
                        message = "Choose a name to see performance and progression details."
                    )
            }
        )
    } else {
        AdaptiveColumn {
            header()
            restTimer()
            errorMessage?.let { ExerciseLibraryError(it) }
            if (selectedExercise == null) {
                if (showAddExercise) {
                    ExerciseAddCard(
                        exerciseName = exerciseName,
                        onExerciseNameChange = { exerciseName = it },
                        muscleGroup = muscleGroup,
                        onMuscleGroupChange = { muscleGroup = it },
                        equipmentType = equipmentType,
                        onEquipmentTypeChange = { equipmentType = it },
                        gym = gym,
                        onGymChange = { gym = it },
                        onSave = saveExercise
                    )
                }
                ExerciseNameList(
                    exercises = exercises,
                    selectedExerciseId = null,
                    onSelect = { selectedExerciseId = it.id },
                    onLog = openLogDialog
                )
            } else {
                ExerciseDetailOverview(selectedExercise)
                PrimaryPillButton(
                    text = "All exercises",
                    onClick = { selectedExerciseId = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutLibraryHeader(
    exerciseCount: Int,
    showAddExercise: Boolean,
    onToggleAdd: () -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 6.dp
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.primary)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.onPrimary.copy(alpha = 0.14f),
                        contentColor = colors.onPrimary
                    ) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(11.dp).size(27.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MOVEMENT LIBRARY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.onPrimary.copy(alpha = 0.74f))
                        Text("Know every movement", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("$exerciseCount exercises ready to train", color = colors.onPrimary.copy(alpha = 0.82f))
                    }
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(32.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OceanHeroAction(if (showAddExercise) "Close editor" else "Add exercise", if (showAddExercise) Icons.Filled.CheckCircle else Icons.Filled.Add, onToggleAdd)
                    OceanHeroAction("Back", Icons.AutoMirrored.Filled.ArrowBack, onBack)
                }
            }
        }
    }
}

@Composable
private fun ExerciseLogDialog(
    exercise: Exercise,
    weight: String,
    onWeightChange: (String) -> Unit,
    reps: String,
    onRepsChange: (String) -> Unit,
    sets: String,
    onSetsChange: (String) -> Unit,
    rpe: String,
    onRpeChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val weightValue = weight.toUserDecimalOrNull()
    val repsValue = reps.toIntOrNull()
    val setsValue = sets.toIntOrNull()
    val rpeValue = rpe.toIntOrNull()
    val inputIsValid = weightValue?.let { it >= 0.0 } == true &&
        repsValue?.let { it >= 1 } == true &&
        setsValue?.let { it >= 1 } == true &&
        (rpe.isBlank() || rpeValue?.let { it in 1..10 } == true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(27.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Log ${exercise.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Capture the work, then ride the recovery timer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f)
                                )
                            }
                        }
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth >= 520.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                NumericField(weight, onWeightChange, "Weight kg", Modifier.weight(1f))
                                NumericField(reps, onRepsChange, "Reps", Modifier.weight(1f))
                                NumericField(sets, onSetsChange, "Sets", Modifier.weight(1f))
                                NumericField(rpe, onRpeChange, "RPE", Modifier.weight(1f))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    NumericField(weight, onWeightChange, "Weight kg", Modifier.weight(1f))
                                    NumericField(reps, onRepsChange, "Reps", Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    NumericField(sets, onSetsChange, "Sets", Modifier.weight(1f))
                                    NumericField(rpe, onRpeChange, "RPE", Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        label = { Text("Machine settings / notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.weight(1f)) { SecondaryPillButton(text = "Cancel", onClick = onDismiss) }
                        Box(Modifier.weight(1f)) {
                            PrimaryPillButton(text = "Save log", enabled = inputIsValid, onClick = onSave)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseNameList(
    exercises: List<Exercise>,
    selectedExerciseId: String?,
    onSelect: (Exercise) -> Unit,
    onLog: (Exercise) -> Unit
) {
    if (exercises.isEmpty()) {
        EmptyStateCard(
            title = "No exercises yet",
            message = "Use Add above to create your first exercise."
        )
        return
    }
    SectionCard(title = "Movement map", subtitle = "${exercises.size} saved · tap a row for progression") {
        exercises.sortedBy { it.name.lowercase() }.forEach { exercise ->
            val selected = exercise.id == selectedExerciseId
            Surface(
                onClick = { onSelect(exercise) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(exercise.muscleGroup, exercise.machineType).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ActionPill(
                        text = "Log",
                        filled = true,
                        onClick = { onLog(exercise) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseAddCard(
    exerciseName: String,
    onExerciseNameChange: (String) -> Unit,
    muscleGroup: String,
    onMuscleGroupChange: (String) -> Unit,
    equipmentType: String,
    onEquipmentTypeChange: (String) -> Unit,
    gym: String,
    onGymChange: (String) -> Unit,
    onSave: () -> Unit
) {
    ModernCard(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Chart a new exercise", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Add it once, then log it in a few taps.", style = MaterialTheme.typography.bodySmall)
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 560.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(exerciseName, onExerciseNameChange, Modifier.weight(1f), label = { Text("Exercise name") }, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(muscleGroup, onMuscleGroupChange, Modifier.weight(1f), label = { Text("Muscle group") }, shape = RoundedCornerShape(16.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        EquipmentTypeDropdown(equipmentType, onEquipmentTypeChange, Modifier.weight(1f))
                        OutlinedTextField(gym, onGymChange, Modifier.weight(1f), label = { Text("Gym") }, shape = RoundedCornerShape(16.dp))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(exerciseName, onExerciseNameChange, Modifier.fillMaxWidth(), label = { Text("Exercise name") }, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(muscleGroup, onMuscleGroupChange, Modifier.fillMaxWidth(), label = { Text("Muscle group") }, shape = RoundedCornerShape(16.dp))
                    EquipmentTypeDropdown(equipmentType, onEquipmentTypeChange, Modifier.fillMaxWidth())
                    OutlinedTextField(gym, onGymChange, Modifier.fillMaxWidth(), label = { Text("Gym") }, shape = RoundedCornerShape(16.dp))
                }
            }
        }
        PrimaryPillButton(text = "Save exercise", enabled = exerciseName.isNotBlank() && muscleGroup.isNotBlank(), onClick = onSave)
    }
}

@Composable
private fun ExerciseLibraryError(message: String) {
    ModernCard(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(message, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExerciseDetailOverview(exercise: Exercise) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(28.dp), color = colors.primary, contentColor = colors.onPrimary, shadowElevation = 6.dp) {
        Box(Modifier.background(Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.primary))).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(28.dp))
                    Text("NEXT CURRENT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary.copy(alpha = 0.8f))
                }
                Text(exercise.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(exercise.nextTarget(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    SectionCard(title = "Setup") {
        ExerciseDetailRow("Muscle group", exercise.muscleGroup.ifBlank { "Not specified" })
        ExerciseDetailRow("Equipment", exercise.machineType.ifBlank { "Not specified" })
        ExerciseDetailRow("Gym", exercise.gym.ifBlank { "Any gym" })
    }
    SectionCard(title = "Performance") {
        val lastWorkout = if (
            exercise.lastWeightKg != null ||
            exercise.lastRepetitions != null ||
            exercise.lastSets != null
        ) {
            "${exercise.lastWeightKg?.formatExerciseWeight() ?: "-"} kg · ${exercise.lastRepetitions ?: "-"} reps · ${exercise.lastSets ?: "-"} sets"
        } else {
            "Not logged yet"
        }
        ExerciseDetailRow("Last workout", lastWorkout)
        ExerciseDetailRow("Personal best", exercise.personalBestKg?.let { "${it.formatExerciseWeight()} kg" } ?: "Not set")
        ExerciseDetailRow("Estimated 1RM", exercise.estimatedOneRepMax().takeIf { it > 0 }?.let { "$it kg" } ?: "Not available")
    }
}

@Composable
private fun ExerciseDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val color = when {
        danger -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        color = if (filled) color else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else color
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun HeroPillSmall(text: String, filled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (filled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
        },
        contentColor = if (filled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun CompactReorderButton(text: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = description
            role = Role.Button
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PrimaryPillButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 13.dp)
        )
    }
}

@Composable
private fun SecondaryPillButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 13.dp)
        )
    }
}

@Composable
private fun NumericField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    )
}

private fun Long.formatDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateFormatter)
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd h:mm a")

private fun Int.formatTimer(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun Exercise.estimatedOneRepMax(): Int {
    val weight = lastWeightKg ?: personalBestKg ?: 0.0
    val reps = lastRepetitions ?: 0
    if (weight <= 0.0 || reps <= 0) return 0
    return (weight * (1.0 + reps / 30.0)).roundToInt()
}

private fun Double.formatExerciseWeight(): String {
    val rounded = (this * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun Exercise.nextTarget(): String {
    val weight = lastWeightKg ?: return "Log first set"
    val reps = lastRepetitions ?: return "Build baseline"
    return when {
        reps >= 12 -> "Try ${((weight + 2.5) * 10.0).roundToInt() / 10.0} kg"
        reps >= 8 -> "Add reps"
        else -> "Rebuild reps"
    }
}
