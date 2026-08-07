package com.raysix.fitns.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutPlan
import com.raysix.fitns.domain.model.WorkoutPlanExercise
import com.raysix.fitns.domain.model.WorkoutSetInput
import com.raysix.fitns.domain.model.WorkoutTemplate
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.raysix.fitns.domain.usecase.WorkoutProgressionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val plans: List<WorkoutPlan> = emptyList(),
    val history: List<WorkoutLogEntry> = emptyList(),
    val weeklyStats: WorkoutWeeklyStats = WorkoutWeeklyStats(),
    val errorMessage: String? = null
)

data class WorkoutWeeklyStats(
    val volumeKg: Double = 0.0,
    val setCount: Int = 0,
    val workoutCount: Int = 0,
    val topExercise: String? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val progressionCalculator: WorkoutProgressionCalculator
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WorkoutUiState> = combine(
        workoutRepository.observeExercises(),
        workoutRepository.observeWorkoutPlans(),
        workoutRepository.observeHistory(),
        errorMessage
    ) { exercises, plans, history, error ->
        WorkoutUiState(
            exercises = exercises,
            templates = exercises.toWorkoutTemplates(),
            plans = plans,
            history = history,
            weeklyStats = history.toWeeklyStats(),
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState()
    )

    fun addWorkout(exercise: Exercise, weight: Double, reps: Int, sets: Int, rpe: Int?, notes: String) {
        viewModelScope.launch {
            val result = workoutRepository.addWorkout(
                WorkoutLogEntry(
                    exercise = exercise,
                    sets = listOf(
                        WorkoutSetInput(
                            weightKg = weight,
                            repetitions = reps,
                            sets = sets,
                            rpe = rpe
                        )
                    ),
                    notes = notes
                )
            )
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Workout could not be saved."
            }
        }
    }

    fun addExercise(name: String, muscleGroup: String, equipmentType: String, gym: String) {
        viewModelScope.launch {
            val result = workoutRepository.addExercise(
                Exercise(
                    name = name.trim(),
                    muscleGroup = muscleGroup.trim(),
                    machineType = equipmentType.trim(),
                    gym = gym.trim()
                )
            )
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> result.error.toMessage("Exercise could not be saved.")
            }
        }
    }

    fun deleteWorkout(entry: WorkoutLogEntry) {
        viewModelScope.launch {
            val result = workoutRepository.deleteWorkout(entry)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Workout could not be deleted."
            }
        }
    }

    fun saveWorkoutPlan(name: String, focus: String, exercises: List<Exercise>, targetSets: Int, targetRepMin: Int, targetRepMax: Int, restSeconds: Int) {
        viewModelScope.launch {
            val result = workoutRepository.saveWorkoutPlan(
                WorkoutPlan(
                    name = name.trim(),
                    focus = focus.trim(),
                    estimatedMinutes = (exercises.size * (targetSets * (restSeconds / 60.0 + 1.0))).toInt().coerceAtLeast(20),
                    exercises = exercises.distinctBy { it.id }.map { exercise ->
                        WorkoutPlanExercise(
                            exercise = exercise,
                            targetSets = targetSets,
                            targetRepMin = targetRepMin,
                            targetRepMax = targetRepMax,
                            restSeconds = restSeconds
                        )
                    }
                )
            )
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> result.error.toMessage("Plan could not be saved.")
            }
        }
    }

    fun saveTemplateAsPlan(template: WorkoutTemplate) {
        saveWorkoutPlan(
            name = template.name,
            focus = template.focus,
            exercises = template.exercises,
            targetSets = 3,
            targetRepMin = 8,
            targetRepMax = 12,
            restSeconds = 90
        )
    }

    fun deleteWorkoutPlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            val result = workoutRepository.deleteWorkoutPlan(plan)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Plan could not be deleted."
            }
        }
    }

    fun progressionHint(entry: WorkoutLogEntry): String {
        return progressionCalculator.recommend(
            completedSets = entry.sets,
            targetRepMin = 8,
            targetRepMax = 12
        ).reason
    }

    private fun AppError.toMessage(fallback: String): String {
        return when (this) {
            is AppError.Validation -> message
            else -> fallback
        }
    }

    private fun List<WorkoutLogEntry>.toWeeklyStats(): WorkoutWeeklyStats {
        val now = System.currentTimeMillis()
        val weekAgo = now - 7L * 24L * 60L * 60L * 1000L
        val week = filter { it.loggedAt >= weekAgo }
        val topExercise = week
            .groupBy { it.exercise.name }
            .maxByOrNull { (_, entries) -> entries.sumOf { it.volumeKg } }
            ?.key
        return WorkoutWeeklyStats(
            volumeKg = week.sumOf { it.volumeKg },
            setCount = week.sumOf { it.sets.sumOf { set -> set.sets } },
            workoutCount = week.distinctBy { it.id }.size,
            topExercise = topExercise
        )
    }

    private fun List<Exercise>.toWorkoutTemplates(): List<WorkoutTemplate> {
        if (isEmpty()) return emptyList()

        fun pick(vararg names: String): List<Exercise> {
            return names.mapNotNull { name ->
                firstOrNull { exercise -> exercise.name.equals(name, ignoreCase = true) }
            }.distinctBy { it.id }
        }

        fun byMuscle(vararg groups: String): List<Exercise> {
            val normalizedGroups = groups.map { it.lowercase() }.toSet()
            return filter { it.muscleGroup.lowercase() in normalizedGroups }
                .distinctBy { it.id }
        }

        val candidates = listOf(
            WorkoutTemplate(
                id = "template-full-body",
                name = "Full Body Strength",
                focus = "Balanced machine session",
                estimatedMinutes = 45,
                exercises = pick("Leg Press", "Chest Press", "Lat Pulldown", "Seated Row", "Shoulder Press")
                    .ifEmpty { take(5) }
            ),
            WorkoutTemplate(
                id = "template-push",
                name = "Push Day",
                focus = "Chest, shoulders, triceps",
                estimatedMinutes = 35,
                exercises = (byMuscle("Chest", "Shoulders", "Triceps") + pick("Chest Press", "Shoulder Press"))
                    .distinctBy { it.id }
                    .take(5)
            ),
            WorkoutTemplate(
                id = "template-pull",
                name = "Pull Day",
                focus = "Back and biceps",
                estimatedMinutes = 35,
                exercises = (byMuscle("Back", "Biceps") + pick("Lat Pulldown", "Seated Row"))
                    .distinctBy { it.id }
                    .take(5)
            ),
            WorkoutTemplate(
                id = "template-lower",
                name = "Lower Body",
                focus = "Leg strength",
                estimatedMinutes = 35,
                exercises = (byMuscle("Legs", "Glutes") + pick("Leg Press", "Leg Curl", "Leg Extension"))
                    .distinctBy { it.id }
                    .take(5)
            )
        )

        return candidates.filter { it.exercises.isNotEmpty() }
    }
}
