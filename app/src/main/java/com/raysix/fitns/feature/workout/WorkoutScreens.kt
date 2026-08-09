package com.raysix.fitns.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.core.design.StatCard
import com.raysix.fitns.core.design.TagChip
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutStartScreen(
    uiState: WorkoutUiState,
    onAddExercise: (String, String, String, String) -> Unit,
    onAddWorkout: (Exercise, Double, Int, Int, Int?, String) -> Unit,
    onSavePlan: (String, String, List<Exercise>, Int, Int, Int, Int) -> Unit,
    onSaveTemplateAsPlan: (WorkoutTemplate) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit,
    onShowHistory: () -> Unit
) {
    val fallbackExercise = uiState.exercises.firstOrNull()
    var selectedExercise by remember(fallbackExercise?.id) { mutableStateOf(fallbackExercise) }
    var weight by remember(fallbackExercise?.id) { mutableStateOf((selectedExercise?.lastWeightKg ?: 0.0).roundToInt().toString()) }
    var reps by remember(fallbackExercise?.id) { mutableStateOf((selectedExercise?.lastRepetitions ?: 10).toString()) }
    var sets by remember(fallbackExercise?.id) { mutableStateOf((selectedExercise?.lastSets ?: 3).toString()) }
    var rpe by remember { mutableStateOf("8") }
    var notes by remember { mutableStateOf("") }
    var showAddExercise by remember { mutableStateOf(false) }
    var exerciseName by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("") }
    var equipmentType by remember { mutableStateOf("Machine") }
    var gym by remember { mutableStateOf("") }
    var restSeconds by remember { mutableStateOf(0) }
    var activeTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var activePlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var completedPlanExerciseIds by remember(activePlan?.id) { mutableStateOf(emptySet<String>()) }
    var showPlanBuilder by remember { mutableStateOf(false) }
    var planName by remember { mutableStateOf("My Workout Plan") }
    var planFocus by remember { mutableStateOf("Strength and consistency") }
    var selectedPlanExerciseIds by remember { mutableStateOf(emptySet<String>()) }
    var planSets by remember { mutableStateOf("3") }
    var planRepMin by remember { mutableStateOf("8") }
    var planRepMax by remember { mutableStateOf("12") }
    var planRestSeconds by remember { mutableStateOf("90") }

    LaunchedEffect(restSeconds) {
        if (restSeconds > 0) {
            delay(1000)
            restSeconds -= 1
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(FitNsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(FitNsDimens.ContentSpacing)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Start Workout", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Fast entry for machines and exercises", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    onClick = onShowHistory,
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        "History",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)
                    )
                }
            }
        }
        item {
            WeeklyTrainingCard(stats = uiState.weeklyStats)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Saved Plans")
                ActionPill(
                    text = if (showPlanBuilder) "Close" else "Create Plan",
                    filled = !showPlanBuilder,
                    onClick = { showPlanBuilder = !showPlanBuilder }
                )
            }
        }
        if (showPlanBuilder) {
            item {
                PlanBuilderCard(
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
                    targetSets = planSets,
                    onTargetSetsChange = { planSets = it },
                    targetRepMin = planRepMin,
                    onTargetRepMinChange = { planRepMin = it },
                    targetRepMax = planRepMax,
                    onTargetRepMaxChange = { planRepMax = it },
                    restSeconds = planRestSeconds,
                    onRestSecondsChange = { planRestSeconds = it },
                    onSave = {
                        val selectedExercises = uiState.exercises.filter { it.id in selectedPlanExerciseIds }
                        onSavePlan(
                            planName,
                            planFocus,
                            selectedExercises,
                            planSets.toIntOrNull() ?: 3,
                            planRepMin.toIntOrNull() ?: 8,
                            planRepMax.toIntOrNull() ?: 12,
                            planRestSeconds.toIntOrNull() ?: 90
                        )
                        selectedPlanExerciseIds = emptySet()
                        showPlanBuilder = false
                    }
                )
            }
        }
        if (uiState.plans.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No saved plans yet.",
                    message = "Create a reusable routine or save a generated template as a plan."
                )
            }
        } else {
            items(uiState.plans) { plan ->
                WorkoutPlanCard(
                    plan = plan,
                    selected = activePlan?.id == plan.id,
                    onStart = {
                        activePlan = plan
                        completedPlanExerciseIds = emptySet()
                        plan.exercises.firstOrNull()?.exercise?.let { exercise ->
                            selectedExercise = exercise
                            weight = (exercise.lastWeightKg ?: 0.0).roundToInt().toString()
                            reps = (exercise.lastRepetitions ?: plan.exercises.first().targetRepMin).toString()
                            sets = plan.exercises.first().targetSets.toString()
                            restSeconds = plan.exercises.first().restSeconds
                        }
                    },
                    onDelete = { onDeletePlan(plan) }
                )
            }
        }
        activePlan?.let { plan ->
            item {
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
                    onChooseExercise = { planExercise ->
                        selectedExercise = planExercise.exercise
                        weight = (planExercise.exercise.lastWeightKg ?: 0.0).roundToInt().toString()
                        reps = (planExercise.exercise.lastRepetitions ?: planExercise.targetRepMin).toString()
                        sets = planExercise.targetSets.toString()
                        restSeconds = planExercise.restSeconds
                    }
                )
            }
        }
        if (uiState.templates.isNotEmpty()) {
            item {
                SectionTitle("Workout Templates")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.templates) { template ->
                        WorkoutTemplateCard(
                            template = template,
                            selected = activeTemplate?.id == template.id,
                            onClick = {
                                activeTemplate = template
                                activePlan = null
                                template.exercises.firstOrNull()?.let { exercise ->
                                    selectedExercise = exercise
                                    weight = (exercise.lastWeightKg ?: 0.0).roundToInt().toString()
                                    reps = (exercise.lastRepetitions ?: 10).toString()
                                    sets = (exercise.lastSets ?: 3).toString()
                                }
                            },
                            onSave = { onSaveTemplateAsPlan(template) }
                        )
                    }
                }
            }
            activeTemplate?.let { template ->
                item {
                    ActiveTemplateCard(
                        template = template,
                        onChooseExercise = { exercise ->
                            selectedExercise = exercise
                            weight = (exercise.lastWeightKg ?: 0.0).roundToInt().toString()
                            reps = (exercise.lastRepetitions ?: 10).toString()
                            sets = (exercise.lastSets ?: 3).toString()
                        }
                    )
                }
            }
        }
        item {
            SectionCard(title = "Exercises", subtitle = "${uiState.exercises.size} saved exercises", trailing = {
                ActionPill(
                    text = if (showAddExercise) "Close" else "Add Exercise",
                    filled = !showAddExercise,
                    onClick = { showAddExercise = !showAddExercise }
                )
            }) {
                if (showAddExercise) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = exerciseName,
                            onValueChange = { exerciseName = it },
                            label = { Text("Exercise name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = muscleGroup,
                            onValueChange = { muscleGroup = it },
                            label = { Text("Muscle group") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = equipmentType,
                            onValueChange = { equipmentType = it },
                            label = { Text("Equipment type") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = gym,
                            onValueChange = { gym = it },
                            label = { Text("Gym") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        PrimaryPillButton(
                            text = "Save Exercise",
                            onClick = {
                                onAddExercise(exerciseName, muscleGroup, equipmentType, gym)
                                exerciseName = ""
                                muscleGroup = ""
                                equipmentType = "Machine"
                                gym = ""
                                showAddExercise = false
                            }
                        )
                    }
                }
            }
        }
        if (uiState.errorMessage != null) {
            item {
                ModernCard(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                    Text(uiState.errorMessage, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        items(uiState.exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                selected = selectedExercise?.id == exercise.id,
                onClick = {
                    selectedExercise = exercise
                    weight = (exercise.lastWeightKg ?: 0.0).roundToInt().toString()
                    reps = (exercise.lastRepetitions ?: 10).toString()
                    sets = (exercise.lastSets ?: 3).toString()
                }
            )
        }
        item {
            SectionCard(title = selectedExercise?.name ?: "Choose exercise", subtitle = "Weight, reps, and machine notes") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericField(value = weight, onValueChange = { weight = it }, label = "Weight kg")
                    NumericField(value = reps, onValueChange = { reps = it }, label = "Reps")
                    NumericField(value = sets, onValueChange = { sets = it }, label = "Sets")
                    NumericField(value = rpe, onValueChange = { rpe = it }, label = "RPE")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Machine settings / notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    PrimaryPillButton(
                        text = "Save Set",
                        onClick = {
                            selectedExercise?.let { exercise ->
                                onAddWorkout(
                                    exercise,
                                    weight.toDoubleOrNull() ?: 0.0,
                                    reps.toIntOrNull() ?: 0,
                                    sets.toIntOrNull() ?: 1,
                                    rpe.toIntOrNull(),
                                    notes
                                )
                                notes = ""
                                restSeconds = 90
                            }
                        }
                    )
                }
            }
        }
        item {
            RestTimerCard(
                seconds = restSeconds,
                onStart = { restSeconds = 90 },
                onAddTime = { restSeconds += 30 },
                onReset = { restSeconds = 0 }
            )
        }
    }
}

@Composable
private fun RestTimerCard(
    seconds: Int,
    onStart: () -> Unit,
    onAddTime: () -> Unit,
    onReset: () -> Unit
) {
    val target = 90
    val progress = if (seconds in 1..target) seconds.toFloat() / target else 0f
    SectionCard(
        title = "Rest Timer",
        subtitle = if (seconds > 0) "Resting between sets" else "Pause after each set",
        trailing = {
            ActionPill(
                text = if (seconds > 0) "Reset" else "Start 90s",
                filled = seconds > 0,
                onClick = if (seconds > 0) onReset else onStart
            )
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = progress,
                modifier = Modifier.size(96.dp),
                stroke = 11.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        seconds.formatTimer(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (seconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                Modifier.weight(1f).padding(start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionPill(text = if (seconds > 0) "Restart 90s" else "Start 90s", filled = true, onClick = onStart)
                ActionPill(text = "+30s", filled = false, onClick = onAddTime, enabled = seconds > 0)
            }
        }
    }
}

@Composable
private fun WeeklyTrainingCard(stats: WorkoutWeeklyStats) {
    SectionCard(
        title = "This Week",
        subtitle = stats.topExercise?.let { "Top exercise: $it" } ?: "Log a workout to build your weekly trend."
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Workouts", stats.workoutCount.toString(), Modifier.weight(1f))
            StatCard("Sets", stats.setCount.toString(), Modifier.weight(1f))
            StatCard("Volume", "${stats.volumeKg.roundToInt()} kg", Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlanBuilderCard(
    exercises: List<Exercise>,
    planName: String,
    onPlanNameChange: (String) -> Unit,
    planFocus: String,
    onPlanFocusChange: (String) -> Unit,
    selectedExerciseIds: Set<String>,
    onToggleExercise: (Exercise) -> Unit,
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
    ModernCard(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Text("Plan Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField(value = targetSets, onValueChange = onTargetSetsChange, label = "Sets", modifier = Modifier.weight(1f))
                NumericField(value = targetRepMin, onValueChange = onTargetRepMinChange, label = "Min reps", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField(value = targetRepMax, onValueChange = onTargetRepMaxChange, label = "Max reps", modifier = Modifier.weight(1f))
                NumericField(value = restSeconds, onValueChange = onRestSecondsChange, label = "Rest sec", modifier = Modifier.weight(1f))
            }
            PrimaryPillButton(
                text = "Save Plan",
                enabled = selectedExerciseIds.isNotEmpty() && planName.isNotBlank(),
                onClick = onSave
            )
        }
    }
}

@Composable
private fun WorkoutPlanCard(
    plan: WorkoutPlan,
    selected: Boolean,
    onStart: () -> Unit,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(plan.focus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${plan.exercises.size} exercises · ${plan.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TagChip(text = if (selected) "Active" else "Saved", accent = selected)
            }
            Text(
                text = plan.exercises.joinToString { "${it.exercise.name} ${it.targetSets}x${it.targetRepMin}-${it.targetRepMax}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(text = if (selected) "Resume" else "Start", filled = true, onClick = onStart)
                ActionPill(text = "Delete", filled = false, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun ActivePlanCard(
    plan: WorkoutPlan,
    completedExerciseIds: Set<String>,
    onToggleComplete: (Exercise) -> Unit,
    onChooseExercise: (WorkoutPlanExercise) -> Unit
) {
    val progress = if (plan.exercises.isEmpty()) 0f else completedExerciseIds.size.toFloat() / plan.exercises.size
    GradientHeroCard(brush = BrandGradient) {
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Active Plan", style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.85f))
                    Text(plan.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onPrimary)
                }
                ProgressRing(
                    progress = progress,
                    modifier = Modifier.size(64.dp),
                    stroke = 8.dp,
                    color = onPrimary,
                    trackColor = onPrimary.copy(alpha = 0.28f)
                ) {
                    Text(
                        "${completedExerciseIds.size}/${plan.exercises.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onPrimary
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = onPrimary,
                trackColor = onPrimary.copy(alpha = 0.28f)
            )
            plan.exercises.forEachIndexed { index, item ->
                val completed = item.exercise.id in completedExerciseIds
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${index + 1}. ${item.exercise.name}",
                            fontWeight = FontWeight.SemiBold,
                            color = if (completed) onPrimary.copy(alpha = 0.6f) else onPrimary
                        )
                        Text(
                            "${item.targetSets} sets · ${item.targetRepMin}-${item.targetRepMax} reps · ${item.restSeconds}s rest",
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimary.copy(alpha = 0.85f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeroPillSmall(text = "Log", filled = true, onClick = { onChooseExercise(item) })
                        HeroPillSmall(text = if (completed) "Done" else "Mark", filled = false, onClick = { onToggleComplete(item.exercise) })
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
    onSave: () -> Unit
) {
    ModernCard(
        modifier = Modifier.width(260.dp),
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(template.name, fontWeight = FontWeight.SemiBold)
            Text(template.focus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${template.exercises.size} exercises · ${template.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = template.exercises.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(text = "Preview", filled = false, onClick = onClick)
                ActionPill(text = "Save", filled = true, onClick = onSave)
            }
        }
    }
}

@Composable
private fun ActiveTemplateCard(
    template: WorkoutTemplate,
    onChooseExercise: (Exercise) -> Unit
) {
    SectionCard(title = "Plan: ${template.name}") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            template.exercises.forEachIndexed { index, exercise ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("${index + 1}. ${exercise.name}", fontWeight = FontWeight.SemiBold)
                        Text(exercise.muscleGroup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ActionPill(text = "Log", filled = true, onClick = { onChooseExercise(exercise) })
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryScreen(
    history: List<WorkoutLogEntry>,
    progressionHint: (WorkoutLogEntry) -> String,
    onDeleteWorkout: (WorkoutLogEntry) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<WorkoutLogEntry?>(null) }

    pendingDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Workout") },
            text = { Text("Remove ${workout.exercise.name} from your workout history?") },
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(FitNsDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(FitNsDimens.ContentSpacing)
    ) {
        item {
            Text("Workout History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (history.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No workouts logged yet.",
                    message = "Save your first set to unlock progression hints and training volume."
                )
            }
        }
        items(history) { workout ->
            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workout.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(workout.loggedAt.formatDate(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    workout.sets.firstOrNull()?.let { set ->
                        Text(
                            "${set.weightKg.roundToInt()} kg x ${set.repetitions} reps x ${set.sets} sets",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("Volume: ${workout.volumeKg.roundToInt()} kg", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(progressionHint(workout), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (workout.notes.isNotBlank()) {
                        Text(workout.notes)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionPill(text = "Delete", filled = false, onClick = { pendingDelete = workout }, danger = true)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExerciseCard(exercise: Exercise, selected: Boolean, onClick: () -> Unit) {
    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TagChip(text = if (selected) "Active" else exercise.muscleGroup, accent = selected)
            }
            Text(
                "Last workout: ${exercise.lastWeightKg?.roundToInt() ?: 0} kg x ${exercise.lastRepetitions ?: 0} x ${exercise.lastSets ?: 0}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Personal best: ${exercise.personalBestKg?.roundToInt() ?: 0} kg · Est. 1RM: ${exercise.estimatedOneRepMax()} kg", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                exercise.nextTarget(),
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
        }
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
        border = if (filled) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
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
private fun NumericField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
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

private fun Exercise.nextTarget(): String {
    val weight = lastWeightKg ?: return "Log first set"
    val reps = lastRepetitions ?: return "Build baseline"
    return when {
        reps >= 12 -> "Try ${((weight + 2.5) * 10.0).roundToInt() / 10.0} kg"
        reps >= 8 -> "Add reps"
        else -> "Rebuild reps"
    }
}
