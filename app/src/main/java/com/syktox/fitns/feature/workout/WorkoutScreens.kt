package com.syktox.fitns.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.syktox.fitns.domain.model.Exercise
import com.syktox.fitns.domain.model.WorkoutLogEntry
import com.syktox.fitns.domain.model.WorkoutPlan
import com.syktox.fitns.domain.model.WorkoutPlanExercise
import com.syktox.fitns.domain.model.WorkoutTemplate
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Start Workout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Fast entry for machines and exercises")
                }
                OutlinedButton(onClick = onShowHistory) {
                    Text("History")
                }
            }
        }
        item {
            WeeklyTrainingCard(stats = uiState.weeklyStats)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saved Plans", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showPlanBuilder = !showPlanBuilder }) {
                    Text(if (showPlanBuilder) "Close" else "Create Plan")
                }
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
                Text("Workout Templates", fontWeight = FontWeight.SemiBold)
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
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Exercises", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { showAddExercise = !showAddExercise }) {
                            Text(if (showAddExercise) "Close" else "Add Exercise")
                        }
                    }
                    if (showAddExercise) {
                        OutlinedTextField(
                            value = exerciseName,
                            onValueChange = { exerciseName = it },
                            label = { Text("Exercise name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = muscleGroup,
                            onValueChange = { muscleGroup = it },
                            label = { Text("Muscle group") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = equipmentType,
                            onValueChange = { equipmentType = it },
                            label = { Text("Equipment type") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = gym,
                            onValueChange = { gym = it },
                            label = { Text("Gym") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onAddExercise(exerciseName, muscleGroup, equipmentType, gym)
                                exerciseName = ""
                                muscleGroup = ""
                                equipmentType = "Machine"
                                gym = ""
                                showAddExercise = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Exercise")
                        }
                    }
                }
            }
        }
        if (uiState.errorMessage != null) {
            item {
                Card {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.error
                    )
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
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(selectedExercise?.name ?: "Choose exercise", fontWeight = FontWeight.SemiBold)
                    NumericField(value = weight, onValueChange = { weight = it }, label = "Weight kg")
                    NumericField(value = reps, onValueChange = { reps = it }, label = "Reps")
                    NumericField(value = sets, onValueChange = { sets = it }, label = "Sets")
                    NumericField(value = rpe, onValueChange = { rpe = it }, label = "RPE")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Machine settings / notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Set")
                    }
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
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rest Timer", fontWeight = FontWeight.SemiBold)
                Text(seconds.formatTimer(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) {
                    Text(if (seconds > 0) "Restart 90s" else "Start 90s")
                }
                OutlinedButton(onClick = onAddTime, enabled = seconds > 0) {
                    Text("+30s")
                }
                OutlinedButton(onClick = onReset, enabled = seconds > 0) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun WeeklyTrainingCard(stats: WorkoutWeeklyStats) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("This Week", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${stats.workoutCount} workouts")
                Text("${stats.setCount} sets")
                Text("${stats.volumeKg.roundToInt()} kg")
            }
            Text(stats.topExercise?.let { "Top exercise: $it" } ?: "Log a workout to build your weekly trend.")
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Plan Builder", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = planName,
                onValueChange = onPlanNameChange,
                label = { Text("Plan name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = planFocus,
                onValueChange = onPlanFocusChange,
                label = { Text("Focus") },
                modifier = Modifier.fillMaxWidth()
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
            Button(
                onClick = onSave,
                enabled = selectedExerciseIds.isNotEmpty() && planName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Plan")
            }
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(plan.name, fontWeight = FontWeight.SemiBold)
                    Text(plan.focus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${plan.exercises.size} exercises · ${plan.estimatedMinutes} min")
                }
                Text(if (selected) "Active" else "Saved", fontWeight = FontWeight.Medium)
            }
            Text(
                text = plan.exercises.joinToString { "${it.exercise.name} ${it.targetSets}x${it.targetRepMin}-${it.targetRepMax}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) {
                    Text(if (selected) "Resume" else "Start")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Delete")
                }
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Active Plan", fontWeight = FontWeight.SemiBold)
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("${completedExerciseIds.size}/${plan.exercises.size}")
            }
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            plan.exercises.forEachIndexed { index, item ->
                val completed = item.exercise.id in completedExerciseIds
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${index + 1}. ${item.exercise.name}", fontWeight = FontWeight.Medium)
                        Text(
                            "${item.targetSets} sets · ${item.targetRepMin}-${item.targetRepMax} reps · ${item.restSeconds}s rest",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onChooseExercise(item) }) {
                            Text("Log")
                        }
                        OutlinedButton(onClick = { onToggleComplete(item.exercise) }) {
                            Text(if (completed) "Done" else "Mark")
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
    onSave: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(260.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(template.name, fontWeight = FontWeight.SemiBold)
            Text(template.focus, style = MaterialTheme.typography.bodyMedium)
            Text("${template.exercises.size} exercises · ${template.estimatedMinutes} min")
            Text(
                text = template.exercises.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClick) {
                    Text("Preview")
                }
                OutlinedButton(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ActiveTemplateCard(
    template: WorkoutTemplate,
    onChooseExercise: (Exercise) -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Plan: ${template.name}", fontWeight = FontWeight.SemiBold)
            template.exercises.forEachIndexed { index, exercise ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${index + 1}. ${exercise.name}", fontWeight = FontWeight.Medium)
                        Text(exercise.muscleGroup, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { onChooseExercise(exercise) }) {
                        Text("Log")
                    }
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Workout History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workout.exercise.name, fontWeight = FontWeight.SemiBold)
                        Text(workout.loggedAt.formatDate())
                    }
                    workout.sets.firstOrNull()?.let { set ->
                        Text("${set.weightKg.roundToInt()} kg x ${set.repetitions} reps x ${set.sets} sets")
                    }
                    Text("Volume: ${workout.volumeKg.roundToInt()} kg")
                    Text(progressionHint(workout))
                    if (workout.notes.isNotBlank()) {
                        Text(workout.notes)
                    }
                    OutlinedButton(onClick = { pendingDelete = workout }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, message: String) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExerciseCard(exercise: Exercise, selected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                Text(if (selected) "Active" else exercise.muscleGroup)
            }
            Text("Last workout: ${exercise.lastWeightKg?.roundToInt() ?: 0} kg x ${exercise.lastRepetitions ?: 0} x ${exercise.lastSets ?: 0}")
            Text("Personal best: ${exercise.personalBestKg?.roundToInt() ?: 0} kg")
            Text("Est. 1RM: ${exercise.estimatedOneRepMax()} kg · ${exercise.nextTarget()}")
        }
    }
}

@Composable
private fun NumericField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
