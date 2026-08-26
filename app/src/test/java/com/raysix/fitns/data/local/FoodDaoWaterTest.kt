package com.raysix.fitns.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.raysix.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoodDaoWaterTest {
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
    fun concurrentWaterChanges_doNotLoseUpdatesAndClampAtZero() = runBlocking {
        val dao = database.foodDao()
        val dayStartMillis = 1_700_000_000_000L

        (1..40).map { index ->
            async(Dispatchers.Default) {
                val now = dayStartMillis + index
                dao.addWaterAtomically(
                    dayStartMillis = dayStartMillis,
                    milliliters = 25.0,
                    updatedAt = now,
                    summaryIfMissing = emptySummary(
                        id = UUID.randomUUID().toString(),
                        dayStartMillis = dayStartMillis,
                        waterMilliliters = 25.0,
                        now = now
                    )
                )
            }
        }.awaitAll()

        assertEquals(1_000.0, dao.findDailySummary(dayStartMillis)?.waterMilliliters ?: -1.0, 0.001)

        (1..40).map { index ->
            async(Dispatchers.Default) {
                dao.removeWaterAtomically(
                    dayStartMillis = dayStartMillis,
                    milliliters = 30.0,
                    updatedAt = dayStartMillis + 100 + index
                )
            }
        }.awaitAll()

        assertEquals(0.0, dao.findDailySummary(dayStartMillis)?.waterMilliliters ?: -1.0, 0.001)
    }

    @Test
    fun legacyDuplicateSummaries_areCountedAndConsolidatedOnNextUpdate() = runBlocking {
        val dao = database.foodDao()
        val dayStartMillis = 1_700_000_000_000L
        dao.upsertDailySummary(
            emptySummary(
                id = "older-summary",
                dayStartMillis = dayStartMillis,
                waterMilliliters = 300.0,
                now = dayStartMillis
            )
        )
        dao.upsertDailySummary(
            emptySummary(
                id = "newer-summary",
                dayStartMillis = dayStartMillis,
                waterMilliliters = 250.0,
                now = dayStartMillis + 1L
            )
        )

        assertEquals(550.0, dao.observeDailyWaterTotal(dayStartMillis).first(), 0.001)

        dao.addWaterAtomically(
            dayStartMillis = dayStartMillis,
            milliliters = 100.0,
            updatedAt = dayStartMillis + 2L,
            summaryIfMissing = emptySummary(
                id = "unused-summary",
                dayStartMillis = dayStartMillis,
                waterMilliliters = 100.0,
                now = dayStartMillis + 2L
            )
        )

        assertEquals(1, dao.findDailySummaries(dayStartMillis).size)
        assertEquals(650.0, dao.findDailySummary(dayStartMillis)?.waterMilliliters ?: -1.0, 0.001)
    }

    private fun emptySummary(
        id: String,
        dayStartMillis: Long,
        waterMilliliters: Double,
        now: Long
    ) = DailyNutritionSummaryEntity(
        id = id,
        dayStartMillis = dayStartMillis,
        caloriesKcal = 0.0,
        proteinGrams = 0.0,
        carbohydratesGrams = 0.0,
        fatGrams = 0.0,
        fiberGrams = 0.0,
        waterMilliliters = waterMilliliters,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}
