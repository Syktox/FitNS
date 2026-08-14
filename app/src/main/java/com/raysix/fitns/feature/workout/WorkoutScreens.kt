package com.raysix.fitns.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.AdaptiveColumn
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.FitNsDimens
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ProgressRing
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.core.design.StatCard
import com.raysix.fitns.core.design.TagChip
import com.raysix.fitns.core.design.isWideScreen
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
        },
        main = {
            WeeklyTrainingCard(stats = uiState.weeklyStats)
            if (uiState.personalRecords.isNotEmpty()) {
                ModernCard(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text("New PR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    uiState.personalRecords.forEach { record ->
                        Text("${record.exerciseName}: ${record.type.label} ${record.value.roundToInt()} ${record.unit}")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Saved Plans")
                ActionPill(
                    text = if (showPlanBuilder) "Close" else "Create Plan",
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
                    title = if (editingPlan == null) "Plan Builder" else "Edit Plan",
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
                        val targetIndex = (currentIndex + direction).coerceIn(0, selectedPlanExerciseIds.lastIndex)
                        if (currentIndex >= 0 && currentIndex != targetIndex) {
                            selectedPlanExerciseIds = selectedPlanExerciseIds.toMutableList().also { list ->
                                val item = list.removeAt(currentIndex)
                                list.add(targetIndex, item)
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
                    title = "No saved plans yet.",
                    message = "Create a reusable routine or save a generated template as a plan."
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
            if (uiState.templates.isNotEmpty()) {
                SectionTitle("Workout Templates")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.templates) { template ->
                        WorkoutTemplateCard(
                            template = template,
                            selected = activeTemplate?.id == template.id,
                            onClick = {
                                activeTemplateId = template.id
                            },
                            onStart = { onStartTemplate(template) }
                        )
                    }
                }
            }
            GradientHeroCard(brush = BrandGradient) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Exercises",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    HeroPillSmall(text = "View", filled = true, onClick = onViewExercises)
                }
            }
            if (uiState.errorMessage != null) {
                ModernCard(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                    Text(uiState.errorMessage, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        side = {}
    )
}

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
    SectionCard(
        title = "Rest Timer",
        subtitle = if (seconds > 0) "Resting between sets" else "Pause after each set",
        trailing = {
            ActionPill(
                text = if (seconds > 0) "Reset" else "Start 90s",
                filled = true,
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
                ActionPill(text = "+15s", filled = false, onClick = onAdd15Seconds, enabled = seconds > 0)
                ActionPill(text = "+30s", filled = false, onClick = onAdd30Seconds, enabled = seconds > 0)
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
    ModernCard(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(FitNsDimens.SectionSpacing)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${index + 1}. ${exercise.name}", fontWeight = FontWeight.SemiBold)
                        Text(exercise.muscleGroup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeroPillSmall(text = "Up", filled = false, onClick = { onMoveSelectedExercise(exerciseId, -1) })
                        HeroPillSmall(text = "Down", filled = false, onClick = { onMoveSelectedExercise(exerciseId, 1) })
                    }
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
                ActionPill(text = "Edit", filled = false, onClick = onEdit)
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
    onStart: () -> Unit
) {
    ModernCard(
        modifier = Modifier
            .width(260.dp)
            .height(300.dp),
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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            SecondaryPillButton(text = "Preview", onClick = onClick)
            PrimaryPillButton(text = "Start Workout", onClick = onStart)
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    template.focus,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${template.exercises.size} exercises · ${template.estimatedMinutes} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(template.exercises, key = { it.id }) { exercise ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(exercise.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        exercise.muscleGroup,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                SecondaryPillButton(text = "Close", onClick = onDismiss)
                PrimaryPillButton(text = "Start Workout", onClick = onStart)
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
    val visibleHistory = history.take(100)
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

    val mid = (visibleHistory.size + 1) / 2
    if (isWideScreen()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Workout History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Back") }
            }
            if (history.isEmpty()) {
                EmptyStateCard(
                    title = "No workouts logged yet.",
                    message = "Save your first set to unlock progression hints and training volume."
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        visibleHistory.take(mid).forEach { workout ->
                            HistoryEntryCard(workout = workout, progressionHint = progressionHint, onDelete = { pendingDelete = workout })
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        visibleHistory.drop(mid).forEach { workout ->
                            HistoryEntryCard(workout = workout, progressionHint = progressionHint, onDelete = { pendingDelete = workout })
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Workout History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Back") }
            }
            if (history.isEmpty()) {
                EmptyStateCard(
                    title = "No workouts logged yet.",
                    message = "Save your first set to unlock progression hints and training volume."
                )
            }
            visibleHistory.forEach { workout ->
                HistoryEntryCard(workout = workout, progressionHint = progressionHint, onDelete = { pendingDelete = workout })
            }
            if (history.size > visibleHistory.size) Text("Showing the 100 most recent exercises", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryEntryCard(workout: WorkoutLogEntry, progressionHint: (WorkoutLogEntry) -> String, onDelete: () -> Unit) {
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
                ActionPill(text = "Delete", filled = false, onClick = onDelete, danger = true)
            }
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
    var restSeconds by rememberSaveable { mutableStateOf(0) }
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
                    logWeight.toDoubleOrNull() ?: 0.0,
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
        ScreenHeader(
            title = "Exercises",
            subtitle = "Browse your saved exercise library.",
            actions = {
                TextButton(
                    onClick = {
                        selectedExerciseId = null
                        showAddExercise = !showAddExercise
                    }
                ) {
                    Text(if (showAddExercise) "Close" else "Add")
                }
                TextButton(onClick = onBack) { Text("Back") }
            }
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
                errorMessage?.let { ExerciseLibraryError(it) }
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
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Log ${exercise.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Enter the completed set details.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    NumericField(value = weight, onValueChange = onWeightChange, label = "Weight kg")
                    NumericField(value = reps, onValueChange = onRepsChange, label = "Reps")
                    NumericField(value = sets, onValueChange = onSetsChange, label = "Sets")
                    NumericField(value = rpe, onValueChange = onRpeChange, label = "RPE")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        label = { Text("Machine settings / notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    SecondaryPillButton(text = "Cancel", onClick = onDismiss)
                    PrimaryPillButton(
                        text = "Save Log",
                        enabled = reps.toIntOrNull() != null && sets.toIntOrNull() != null,
                        onClick = onSave
                    )
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
    SectionCard(title = "Your exercises", subtitle = "${exercises.size} saved") {
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
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
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
    SectionCard(title = "Add exercise") {
        OutlinedTextField(
            value = exerciseName,
            onValueChange = onExerciseNameChange,
            label = { Text("Exercise name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = muscleGroup,
            onValueChange = onMuscleGroupChange,
            label = { Text("Muscle group") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        EquipmentTypeDropdown(
            value = equipmentType,
            onValueChange = onEquipmentTypeChange,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = gym,
            onValueChange = onGymChange,
            label = { Text("Gym") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        PrimaryPillButton(
            text = "Save Exercise",
            enabled = exerciseName.isNotBlank() && muscleGroup.isNotBlank(),
            onClick = onSave
        )
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
    GradientHeroCard(brush = BrandGradient) {
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Exercise overview",
                style = MaterialTheme.typography.labelMedium,
                color = onPrimary.copy(alpha = 0.82f)
            )
            Text(
                exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onPrimary
            )
            Text(
                exercise.nextTarget(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onPrimary
            )
        }
    }
    SectionCard(title = "Exercise") {
        ExerciseDetailRow("Muscle group", exercise.muscleGroup.ifBlank { "Not specified" })
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
