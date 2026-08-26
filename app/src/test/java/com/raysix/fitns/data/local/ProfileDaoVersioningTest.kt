package com.raysix.fitns.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.raysix.fitns.data.local.entity.NutritionGoalEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileDaoVersioningTest {
    private lateinit var database: FitNsDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitNsDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replacingGoal_closesLegacyDuplicatesAndKeepsExactlyOneOpenVersion() = runBlocking {
        val dao = database.profileDao()
        dao.upsertNutritionGoal(goal(id = "legacy-1", validFrom = 1L))
        dao.upsertNutritionGoal(goal(id = "legacy-2", validFrom = 2L))

        dao.replaceOpenNutritionGoal(goal(id = "current", validFrom = 3L), validTo = 3L)

        val history = dao.observeNutritionGoalHistory(UserId).first()
        assertEquals(1, history.count { it.validTo == null })
        assertNull(history.single { it.id == "current" }.validTo)
        assertEquals(3L, history.single { it.id == "legacy-1" }.validTo)
        assertEquals(3L, history.single { it.id == "legacy-2" }.validTo)
    }

    @Test
    fun concurrentGoalReplacements_leaveOneOpenVersion() = runBlocking {
        val dao = database.profileDao()

        (1L..20L).map { version ->
            async(Dispatchers.Default) {
                dao.replaceOpenNutritionGoal(
                    goal(id = "goal-$version", validFrom = version),
                    validTo = version
                )
            }
        }.awaitAll()

        val history = dao.observeNutritionGoalHistory(UserId).first()
        assertEquals(1, history.count { it.validTo == null })
    }

    private fun goal(id: String, validFrom: Long) = NutritionGoalEntity(
        id = id,
        userProfileId = UserId,
        caloriesKcal = 2_200.0,
        proteinGrams = 150.0,
        carbohydrateGrams = 250.0,
        fatGrams = 70.0,
        fiberGrams = 30.0,
        waterMilliliters = 2_500.0,
        validFrom = validFrom,
        validTo = null,
        createdAt = validFrom,
        updatedAt = validFrom,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )

    private companion object {
        const val UserId = "default"
    }
}
