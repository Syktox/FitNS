package com.raysix.fitns.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.ActiveWorkoutExercise
import com.raysix.fitns.domain.model.ActiveWorkoutSession
import com.raysix.fitns.domain.model.ActiveWorkoutSet
import com.raysix.fitns.domain.model.PersonalRecordEvent
import com.raysix.fitns.domain.model.PreviousPerformance
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutPlan
import com.raysix.fitns.domain.model.WorkoutPlanExercise
import com.raysix.fitns.domain.model.WorkoutSetInput
import com.raysix.fitns.domain.model.WorkoutSetType
import com.raysix.fitns.domain.model.WorkoutTemplate
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.raysix.fitns.domain.usecase.BuildActiveWorkoutSessionUseCase
import com.raysix.fitns.domain.usecase.PersonalRecordDetector
import com.raysix.fitns.domain.usecase.WorkoutProgressionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.squareup.moshi.Moshi
import javax.inject.Inject

data class WorkoutUiState(
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val plans: List<WorkoutPlan> = emptyList(),
    val history: List<WorkoutLogEntry> = emptyList(),
    val activeSession: ActiveWorkoutSession? = null,
    val restTimer: RestTimerUiState = RestTimerUiState(),
    val personalRecords: List<PersonalRecordEvent> = emptyList(),
    val weeklyStats: WorkoutWeeklyStats = WorkoutWeeklyStats(),
    val errorMessage: String? = null
)

data class WorkoutWeeklyStats(
    val volumeKg: Double = 0.0,
    val setCount: Int = 0,
    val workoutCount: Int = 0,
    val topExercise: String? = null
)

data class RestTimerUiState(
    val secondsRemaining: Int = 0,
    val targetSeconds: Int = 90,
    val isRunning: Boolean = false
)

private data class WorkoutRepositorySnapshot(
    val exercises: List<Exercise>,
    val plans: List<WorkoutPlan>,
    val history: List<WorkoutLogEntry>
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val progressionCalculator: WorkoutProgressionCalculator,
    private val buildActiveWorkoutSession: BuildActiveWorkoutSessionUseCase,
    private val personalRecordDetector: PersonalRecordDetector,
    private val savedStateHandle: SavedStateHandle,
    moshi: Moshi
) : ViewModel() {
    private val activeSessionAdapter = moshi.adapter(ActiveWorkoutSession::class.java)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val activeSession = MutableStateFlow(
        savedStateHandle.get<String>(ActiveSessionKey)?.let { json ->
            runCatching { activeSessionAdapter.fromJson(json) }.getOrNull()
        }
    )
    private val restTimer = MutableStateFlow(RestTimerUiState())
    private val personalRecords = MutableStateFlow<List<PersonalRecordEvent>>(emptyList())
    private var timerJob: Job? = null
    private var sessionPersistenceJob: Job? = null

    private val repositorySnapshot = combine(
        workoutRepository.observeExercises(),
        workoutRepository.observeWorkoutPlans(),
        workoutRepository.observeHistory()
    ) { exercises, plans, history ->
        WorkoutRepositorySnapshot(
            exercises = exercises,
            plans = plans,
            history = history
        )
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        repositorySnapshot,
        activeSession,
        restTimer,
        personalRecords,
        errorMessage
    ) { snapshot, session, timer, records, error ->
        WorkoutUiState(
            exercises = snapshot.exercises,
            templates = snapshot.exercises.toWorkoutTemplates(),
            plans = snapshot.plans,
            history = snapshot.history,
            activeSession = session,
            restTimer = timer,
            personalRecords = records,
            weeklyStats = snapshot.history.toWeeklyStats(),
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
        saveWorkoutPlanInternal(
            id = null,
            name = name,
            focus = focus,
            exercises = exercises,
            targetSets = targetSets,
            targetRepMin = targetRepMin,
            targetRepMax = targetRepMax,
            restSeconds = restSeconds
        )
    }

    fun updateWorkoutPlan(plan: WorkoutPlan, name: String, focus: String, exercises: List<Exercise>, targetSets: Int, targetRepMin: Int, targetRepMax: Int, restSeconds: Int) {
        saveWorkoutPlanInternal(
            id = plan.id,
            name = name,
            focus = focus,
            exercises = exercises,
            targetSets = targetSets,
            targetRepMin = targetRepMin,
            targetRepMax = targetRepMax,
            restSeconds = restSeconds
        )
    }

    private fun saveWorkoutPlanInternal(id: String?, name: String, focus: String, exercises: List<Exercise>, targetSets: Int, targetRepMin: Int, targetRepMax: Int, restSeconds: Int) {
        viewModelScope.launch {
            val result = workoutRepository.saveWorkoutPlan(
                WorkoutPlan(
                    id = id ?: java.util.UUID.randomUUID().toString(),
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

    fun startWorkoutPlan(plan: WorkoutPlan) {
        setActiveSession(buildActiveWorkoutSession.fromPlan(plan, uiState.value.history), persistImmediately = true)
        personalRecords.value = emptyList()
        skipRestTimer()
    }

    fun startWorkoutTemplate(template: WorkoutTemplate) {
        val plan = WorkoutPlan(
            id = template.id,
            name = template.name,
            focus = template.focus,
            estimatedMinutes = template.estimatedMinutes,
            exercises = template.exercises.map { exercise ->
                WorkoutPlanExercise(
                    exercise = exercise,
                    targetSets = 3,
                    targetRepMin = 8,
                    targetRepMax = 12,
                    restSeconds = 90
                )
            }
        )
        startWorkoutPlan(plan)
    }

    fun addExerciseToActiveSession(exercise: Exercise) {
        updateActiveSession { session ->
            if (session.exercises.any { it.exercise.id == exercise.id }) return@updateActiveSession session
            val nextOrder = session.exercises.size
            session.copy(
                exercises = session.exercises + ActiveWorkoutExercise(
                    exercise = exercise,
                    sortOrder = nextOrder,
                    targetRepMin = 8,
                    targetRepMax = 12,
                    restSeconds = 90,
                    sets = listOf(
                        ActiveWorkoutSet(
                            setNumber = 1,
                            weightKg = exercise.lastWeightKg ?: 0.0,
                            repetitions = exercise.lastRepetitions ?: 8,
                            previousPerformance = exercise.lastWeightKg?.let {
                                PreviousPerformance(
                                    exerciseId = exercise.id,
                                    weightKg = it,
                                    repetitions = exercise.lastRepetitions ?: 0,
                                    loggedAt = System.currentTimeMillis()
                                )
                            }
                        )
                    )
                )
            ).normalizeExerciseOrder()
        }
    }

    fun removeExerciseFromActiveSession(activeExerciseId: String) {
        updateActiveSession { session ->
            session.copy(exercises = session.exercises.filterNot { it.id == activeExerciseId }).normalizeExerciseOrder()
        }
    }

    fun moveExerciseInActiveSession(activeExerciseId: String, direction: Int) {
        updateActiveSession { session ->
            val currentIndex = session.exercises.indexOfFirst { it.id == activeExerciseId }
            val targetIndex = (currentIndex + direction).coerceIn(0, session.exercises.lastIndex)
            if (currentIndex < 0 || currentIndex == targetIndex) return@updateActiveSession session
            val mutable = session.exercises.toMutableList()
            val item = mutable.removeAt(currentIndex)
            mutable.add(targetIndex, item)
            session.copy(exercises = mutable).normalizeExerciseOrder()
        }
    }

    fun addSetToActiveExercise(activeExerciseId: String) {
        updateActiveExercise(activeExerciseId) { activeExercise ->
            val template = activeExercise.sets.lastOrNull()
            activeExercise.copy(
                sets = activeExercise.sets + ActiveWorkoutSet(
                    setNumber = activeExercise.sets.size + 1,
                    weightKg = template?.weightKg ?: 0.0,
                    repetitions = template?.repetitions ?: activeExercise.targetRepMin,
                    rpe = template?.rpe,
                    rir = template?.rir,
                    setType = template?.setType ?: WorkoutSetType.Normal,
                    previousPerformance = template?.previousPerformance,
                    restSeconds = activeExercise.restSeconds
                )
            )
        }
    }

    fun deleteSetFromActiveExercise(activeExerciseId: String, setId: String) {
        updateActiveExercise(activeExerciseId) { activeExercise ->
            activeExercise.copy(
                sets = activeExercise.sets
                    .filterNot { it.id == setId }
                    .mapIndexed { index, set -> set.copy(setNumber = index + 1) }
            )
        }
    }

    fun updateActiveSet(
        activeExerciseId: String,
        setId: String,
        weightKg: Double? = null,
        repetitions: Int? = null,
        rpe: Int? = null,
        rir: Int? = null,
        setType: WorkoutSetType? = null
    ) {
        updateActiveExercise(activeExerciseId) { activeExercise ->
            activeExercise.copy(
                sets = activeExercise.sets.map { set ->
                    if (set.id != setId) {
                        set
                    } else {
                        set.copy(
                            weightKg = weightKg ?: set.weightKg,
                            repetitions = repetitions ?: set.repetitions,
                            rpe = rpe,
                            rir = rir,
                            setType = setType ?: set.setType
                        )
                    }
                }
            )
        }
    }

    fun toggleSetCompleted(activeExerciseId: String, setId: String) {
        var restSecondsToStart: Int? = null
        updateActiveExercise(activeExerciseId) { activeExercise ->
            activeExercise.copy(
                sets = activeExercise.sets.map { set ->
                    if (set.id != setId) {
                        set
                    } else if (set.completedAt == null) {
                        restSecondsToStart = set.restSeconds
                        set.copy(completedAt = System.currentTimeMillis())
                    } else {
                        set.copy(completedAt = null)
                    }
                }
            )
        }
        restSecondsToStart?.let { startRestTimer(it) }
    }

    fun finishActiveWorkout(onFinished: () -> Unit = {}) {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            val records = personalRecordDetector.detect(session, uiState.value.history)
            val result = workoutRepository.saveWorkoutSession(session)
            errorMessage.value = when (result) {
                is AppResult.Success -> {
                    setActiveSession(null)
                    personalRecords.value = records
                    skipRestTimer()
                    onFinished()
                    null
                }
                is AppResult.Failure -> result.error.toMessage("Workout could not be saved.")
            }
        }
    }

    fun discardActiveWorkout() {
        setActiveSession(null)
        skipRestTimer()
    }

    fun startRestTimer(seconds: Int = 90) {
        timerJob?.cancel()
        restTimer.value = RestTimerUiState(secondsRemaining = seconds, targetSeconds = seconds, isRunning = true)
        timerJob = viewModelScope.launch {
            while (restTimer.value.secondsRemaining > 0 && restTimer.value.isRunning) {
                delay(1000)
                restTimer.value = restTimer.value.copy(secondsRemaining = (restTimer.value.secondsRemaining - 1).coerceAtLeast(0))
            }
            if (restTimer.value.secondsRemaining == 0) {
                restTimer.value = restTimer.value.copy(isRunning = false)
            }
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = restTimer.value
        val nextSeconds = (current.secondsRemaining + deltaSeconds).coerceAtLeast(0)
        restTimer.value = current.copy(secondsRemaining = nextSeconds, targetSeconds = current.targetSeconds.coerceAtLeast(nextSeconds))
    }

    fun pauseRestTimer() {
        restTimer.value = restTimer.value.copy(isRunning = false)
        timerJob?.cancel()
    }

    fun resumeRestTimer() {
        val current = restTimer.value
        if (current.secondsRemaining > 0) startRestTimer(current.secondsRemaining)
    }

    fun skipRestTimer() {
        timerJob?.cancel()
        restTimer.value = RestTimerUiState()
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

    private fun updateActiveSession(reducer: (ActiveWorkoutSession) -> ActiveWorkoutSession) {
        setActiveSession(activeSession.value?.let(reducer))
    }

    private fun setActiveSession(session: ActiveWorkoutSession?, persistImmediately: Boolean = false) {
        activeSession.value = session
        sessionPersistenceJob?.cancel()
        if (session == null) {
            savedStateHandle.remove<String>(ActiveSessionKey)
        } else {
            sessionPersistenceJob = viewModelScope.launch {
                if (!persistImmediately) delay(SessionPersistenceDebounceMillis)
                val json = withContext(Dispatchers.Default) { activeSessionAdapter.toJson(session) }
                savedStateHandle[ActiveSessionKey] = json
            }
        }
    }

    private fun updateActiveExercise(
        activeExerciseId: String,
        reducer: (ActiveWorkoutExercise) -> ActiveWorkoutExercise
    ) {
        updateActiveSession { session ->
            session.copy(
                exercises = session.exercises.map { activeExercise ->
                    if (activeExercise.id == activeExerciseId) reducer(activeExercise) else activeExercise
                }
            )
        }
    }

    private fun ActiveWorkoutSession.normalizeExerciseOrder(): ActiveWorkoutSession {
        return copy(exercises = exercises.mapIndexed { index, exercise -> exercise.copy(sortOrder = index) })
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

    private companion object {
        const val ActiveSessionKey = "active_workout_session"
        const val SessionPersistenceDebounceMillis = 300L
    }
}
