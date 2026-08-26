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
import kotlinx.coroutines.CancellationException
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
    val isSavingActiveSession: Boolean = false,
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

private data class WorkoutOperationState(
    val isSavingActiveSession: Boolean,
    val errorMessage: String?
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
    private val isSavingActiveSession = MutableStateFlow(false)
    private val activeSession = MutableStateFlow(
        savedStateHandle.get<String>(ActiveSessionKey)?.let { json ->
            runCatching { activeSessionAdapter.fromJson(json) }.getOrNull()
        }
    )
    private val restTimer = MutableStateFlow(restoreRestTimer())
    private val personalRecords = MutableStateFlow<List<PersonalRecordEvent>>(emptyList())
    private var timerJob: Job? = null
    private var sessionPersistenceJob: Job? = null

    init {
        val restored = restTimer.value
        val deadline = savedStateHandle.get<Long>(RestTimerDeadlineKey)
        if (restored.isRunning && restored.secondsRemaining > 0 && deadline != null) {
            launchRestTimerTicker(deadline)
        } else if (restored.secondsRemaining <= 0) {
            clearPersistedRestTimer()
        }
    }

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

    private val operationState = combine(isSavingActiveSession, errorMessage) { saving, error ->
        WorkoutOperationState(isSavingActiveSession = saving, errorMessage = error)
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        repositorySnapshot,
        activeSession,
        restTimer,
        personalRecords,
        operationState
    ) { snapshot, session, timer, records, operation ->
        WorkoutUiState(
            exercises = snapshot.exercises,
            templates = snapshot.exercises.toWorkoutTemplates(),
            plans = snapshot.plans,
            history = snapshot.history,
            activeSession = session,
            restTimer = timer,
            personalRecords = records,
            weeklyStats = snapshot.history.toWeeklyStats(),
            isSavingActiveSession = operation.isSavingActiveSession,
            errorMessage = operation.errorMessage
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
        if (isSavingActiveSession.value) return
        setActiveSession(buildActiveWorkoutSession.fromPlan(plan, uiState.value.history), persistImmediately = true)
        personalRecords.value = emptyList()
        errorMessage.value = null
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
        if (isSavingActiveSession.value) return
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
        if (isSavingActiveSession.value) return
        updateActiveSession { session ->
            session.copy(exercises = session.exercises.filterNot { it.id == activeExerciseId }).normalizeExerciseOrder()
        }
    }

    fun moveExerciseInActiveSession(activeExerciseId: String, direction: Int) {
        if (isSavingActiveSession.value) return
        updateActiveSession { session ->
            val currentIndex = session.exercises.indexOfFirst { it.id == activeExerciseId }
            if (currentIndex < 0) return@updateActiveSession session
            val targetIndex = (currentIndex + direction).coerceIn(0, session.exercises.lastIndex)
            if (currentIndex == targetIndex) return@updateActiveSession session
            val mutable = session.exercises.toMutableList()
            val item = mutable.removeAt(currentIndex)
            mutable.add(targetIndex, item)
            session.copy(exercises = mutable).normalizeExerciseOrder()
        }
    }

    fun addSetToActiveExercise(activeExerciseId: String) {
        if (isSavingActiveSession.value) return
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
        if (isSavingActiveSession.value) return
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
        weightKg: Double?,
        repetitions: Int?,
        rpe: Int?,
        rir: Int?,
        setType: WorkoutSetType?
    ) {
        if (isSavingActiveSession.value) return
        updateActiveExercise(activeExerciseId) { activeExercise ->
            activeExercise.copy(
                sets = activeExercise.sets.map { set ->
                    if (set.id != setId) {
                        set
                    } else {
                        val requiredValuesAreValid = weightKg?.let { it.isFinite() && it >= 0.0 } == true &&
                            repetitions?.let { it > 0 } == true
                        val effortValuesAreValid = (rpe == null || rpe in 1..10) &&
                            (rir == null || rir in 0..10)
                        set.copy(
                            weightKg = weightKg?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                            repetitions = repetitions?.takeIf { it >= 0 } ?: 0,
                            rpe = rpe,
                            rir = rir,
                            setType = setType ?: set.setType,
                            completedAt = set.completedAt.takeIf {
                                requiredValuesAreValid && effortValuesAreValid
                            }
                        )
                    }
                }
            )
        }
    }

    fun toggleSetCompleted(activeExerciseId: String, setId: String) {
        if (isSavingActiveSession.value) return
        var restSecondsToStart: Int? = null
        updateActiveExercise(activeExerciseId) { activeExercise ->
            activeExercise.copy(
                sets = activeExercise.sets.map { set ->
                    if (set.id != setId) {
                        set
                    } else if (set.completedAt == null) {
                        val validationMessage = set.completionValidationMessage()
                        if (validationMessage == null) {
                            errorMessage.value = null
                            restSecondsToStart = set.restSeconds
                            set.copy(completedAt = System.currentTimeMillis())
                        } else {
                            errorMessage.value = validationMessage
                            set
                        }
                    } else {
                        set.copy(completedAt = null)
                    }
                }
            )
        }
        restSecondsToStart?.let { startRestTimer(it) }
    }

    fun finishActiveWorkout(onFinished: () -> Unit = {}) {
        if (!isSavingActiveSession.compareAndSet(expect = false, update = true)) return
        val session = activeSession.value
        if (session == null) {
            isSavingActiveSession.value = false
            return
        }
        viewModelScope.launch {
            try {
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
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                errorMessage.value = "Workout could not be saved. Your active session is still available."
            } finally {
                isSavingActiveSession.value = false
            }
        }
    }

    fun discardActiveWorkout() {
        if (isSavingActiveSession.value) return
        setActiveSession(null)
        errorMessage.value = null
        skipRestTimer()
    }

    fun startRestTimer(seconds: Int = 90) {
        startRestTimer(seconds = seconds, targetSeconds = seconds)
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = restTimer.value
        val nextSeconds = (current.secondsRemaining + deltaSeconds).coerceAtLeast(0)
        val next = current.copy(
            secondsRemaining = nextSeconds,
            targetSeconds = current.targetSeconds.coerceAtLeast(nextSeconds),
            isRunning = current.isRunning && nextSeconds > 0
        )
        restTimer.value = next
        timerJob?.cancel()
        if (next.isRunning) {
            val deadline = deadlineAfter(next.secondsRemaining)
            persistRestTimer(next, deadline)
            launchRestTimerTicker(deadline)
        } else {
            persistRestTimer(next, deadline = null)
        }
    }

    fun pauseRestTimer() {
        timerJob?.cancel()
        val deadline = savedStateHandle.get<Long>(RestTimerDeadlineKey)
        val remaining = deadline?.let(::secondsUntil) ?: restTimer.value.secondsRemaining
        restTimer.value = restTimer.value.copy(secondsRemaining = remaining, isRunning = false)
        persistRestTimer(restTimer.value, deadline = null)
    }

    fun resumeRestTimer() {
        val current = restTimer.value
        if (current.secondsRemaining > 0) {
            startRestTimer(current.secondsRemaining, current.targetSeconds)
        }
    }

    fun skipRestTimer() {
        timerJob?.cancel()
        restTimer.value = RestTimerUiState()
        clearPersistedRestTimer()
    }

    private fun startRestTimer(seconds: Int, targetSeconds: Int) {
        timerJob?.cancel()
        if (seconds <= 0) {
            skipRestTimer()
            return
        }
        val state = RestTimerUiState(seconds, targetSeconds.coerceAtLeast(seconds), true)
        val deadline = deadlineAfter(seconds)
        restTimer.value = state
        persistRestTimer(state, deadline)
        launchRestTimerTicker(deadline)
    }

    private fun launchRestTimerTicker(deadline: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (restTimer.value.isRunning) {
                val remaining = secondsUntil(deadline)
                if (remaining <= 0) {
                    restTimer.value = restTimer.value.copy(secondsRemaining = 0, isRunning = false)
                    persistRestTimer(restTimer.value, deadline = null)
                    break
                }
                restTimer.value = restTimer.value.copy(secondsRemaining = remaining)
                delay(RestTimerTickMillis)
            }
        }
    }

    private fun restoreRestTimer(): RestTimerUiState {
        val savedRemaining = savedStateHandle.get<Int>(RestTimerRemainingKey) ?: 0
        val target = savedStateHandle.get<Int>(RestTimerTargetKey) ?: 90
        val wasRunning = savedStateHandle.get<Boolean>(RestTimerRunningKey) ?: false
        val deadline = savedStateHandle.get<Long>(RestTimerDeadlineKey)
        val remaining = if (wasRunning && deadline != null) secondsUntil(deadline) else savedRemaining
        return RestTimerUiState(
            secondsRemaining = remaining.coerceAtLeast(0),
            targetSeconds = target.coerceAtLeast(remaining),
            isRunning = wasRunning && remaining > 0
        )
    }

    private fun persistRestTimer(state: RestTimerUiState, deadline: Long?) {
        savedStateHandle[RestTimerRemainingKey] = state.secondsRemaining
        savedStateHandle[RestTimerTargetKey] = state.targetSeconds
        savedStateHandle[RestTimerRunningKey] = state.isRunning
        if (deadline == null) {
            savedStateHandle.remove<Long>(RestTimerDeadlineKey)
        } else {
            savedStateHandle[RestTimerDeadlineKey] = deadline
        }
    }

    private fun clearPersistedRestTimer() {
        savedStateHandle.remove<Int>(RestTimerRemainingKey)
        savedStateHandle.remove<Int>(RestTimerTargetKey)
        savedStateHandle.remove<Boolean>(RestTimerRunningKey)
        savedStateHandle.remove<Long>(RestTimerDeadlineKey)
    }

    private fun deadlineAfter(seconds: Int): Long {
        return restTimerDeadline(
            nowMillis = System.currentTimeMillis(),
            seconds = seconds
        )
    }

    private fun secondsUntil(deadline: Long): Int {
        return remainingRestTimerSeconds(
            deadlineMillis = deadline,
            nowMillis = System.currentTimeMillis()
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
                if (!persistImmediately) {
                    delay(SessionPersistenceDebounceMillis)
                }
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
        const val RestTimerRemainingKey = "rest_timer_remaining"
        const val RestTimerTargetKey = "rest_timer_target"
        const val RestTimerRunningKey = "rest_timer_running"
        const val RestTimerDeadlineKey = "rest_timer_deadline"
        const val RestTimerTickMillis = 250L
    }
}

internal fun ActiveWorkoutSet.completionValidationMessage(): String? {
    return when {
        !weightKg.isFinite() || weightKg < 0.0 -> "Enter a valid, non-negative weight before completing the set."
        repetitions < 1 -> "Enter at least one repetition before completing the set."
        rpe != null && rpe !in 1..10 -> "RPE must be between 1 and 10."
        rir != null && rir !in 0..10 -> "RIR must be between 0 and 10."
        else -> null
    }
}

internal fun restTimerDeadline(nowMillis: Long, seconds: Int): Long {
    return nowMillis + seconds.coerceAtLeast(0).toLong() * 1_000L
}

internal fun remainingRestTimerSeconds(deadlineMillis: Long, nowMillis: Long): Int {
    val remainingMillis = (deadlineMillis - nowMillis).coerceAtLeast(0L)
    return ((remainingMillis + 999L) / 1_000L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
}
