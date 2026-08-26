package com.raysix.fitns.feature.workout

import androidx.lifecycle.SavedStateHandle
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.ActiveWorkoutSession
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutPlan
import com.raysix.fitns.domain.model.WorkoutPlanExercise
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.raysix.fitns.domain.usecase.BuildActiveWorkoutSessionUseCase
import com.raysix.fitns.domain.usecase.EstimatedOneRepMaxCalculator
import com.raysix.fitns.domain.usecase.PersonalRecordDetector
import com.raysix.fitns.domain.usecase.WorkoutProgressionCalculator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun discardIsIgnoredWhileCompletedWorkoutIsBeingSaved() = runTest(mainDispatcher) {
        val repository = BlockingWorkoutRepository()
        val viewModel = WorkoutViewModel(
            workoutRepository = repository,
            progressionCalculator = WorkoutProgressionCalculator(),
            buildActiveWorkoutSession = BuildActiveWorkoutSessionUseCase(),
            personalRecordDetector = PersonalRecordDetector(EstimatedOneRepMaxCalculator()),
            savedStateHandle = SavedStateHandle(),
            moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        val exercise = Exercise(
            id = "bench",
            name = "Bench Press",
            muscleGroup = "Chest",
            machineType = "Barbell"
        )
        val plan = WorkoutPlan(
            id = "push",
            name = "Push",
            focus = "Strength",
            estimatedMinutes = 30,
            exercises = listOf(
                WorkoutPlanExercise(
                    exercise = exercise,
                    targetSets = 1,
                    targetRepMin = 8,
                    targetRepMax = 12,
                    restSeconds = 0
                )
            )
        )

        viewModel.startWorkoutPlan(plan)
        runCurrent()
        val activeExercise = requireNotNull(viewModel.uiState.value.activeSession).exercises.single()
        viewModel.toggleSetCompleted(activeExercise.id, activeExercise.sets.single().id)
        viewModel.finishActiveWorkout()
        viewModel.finishActiveWorkout()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSavingActiveSession)
        assertNotNull(viewModel.uiState.value.activeSession)
        viewModel.discardActiveWorkout()
        assertNotNull(viewModel.uiState.value.activeSession)

        repository.allowSaveToFinish.complete(Unit)
        runCurrent()

        assertFalse(viewModel.uiState.value.isSavingActiveSession)
        assertEquals(1, repository.savedSessions.size)
        assertNull(viewModel.uiState.value.activeSession)
    }
}

private class BlockingWorkoutRepository : WorkoutRepository {
    private val exercises = MutableStateFlow<List<Exercise>>(emptyList())
    private val history = MutableStateFlow<List<WorkoutLogEntry>>(emptyList())
    private val plans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    val allowSaveToFinish = CompletableDeferred<Unit>()
    val savedSessions = mutableListOf<ActiveWorkoutSession>()

    override fun observeExercises(): Flow<List<Exercise>> = exercises
    override fun observeHistory(): Flow<List<WorkoutLogEntry>> = history
    override fun observeWorkoutPlans(): Flow<List<WorkoutPlan>> = plans
    override suspend fun addExercise(exercise: Exercise): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun addWorkout(entry: WorkoutLogEntry): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun saveWorkoutPlan(plan: WorkoutPlan): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteWorkoutPlan(plan: WorkoutPlan): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteWorkout(entry: WorkoutLogEntry): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun saveWorkoutSession(session: ActiveWorkoutSession): AppResult<Unit> {
        savedSessions += session
        allowSaveToFinish.await()
        return AppResult.Success(Unit)
    }
}
