package com.raysix.fitns.core.sync

import com.squareup.moshi.Moshi
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.WorkoutLogEntry
import javax.inject.Inject

class SyncPayloadFactory @Inject constructor(
    moshi: Moshi
) {
    private val foodAdapter = moshi.adapter(FoodEntrySyncPayload::class.java)
    private val workoutAdapter = moshi.adapter(WorkoutSyncPayload::class.java)
    private val bodyWeightAdapter = moshi.adapter(BodyWeightSyncPayload::class.java)

    fun foodEntry(entry: FoodLogEntry, operation: String): String {
        return foodAdapter.toJson(
            FoodEntrySyncPayload(
                operation = operation,
                generatedAt = System.currentTimeMillis(),
                foodEntry = entry
            )
        )
    }

    fun workout(entry: WorkoutLogEntry, operation: String): String {
        return workoutAdapter.toJson(
            WorkoutSyncPayload(
                operation = operation,
                generatedAt = System.currentTimeMillis(),
                workout = entry
            )
        )
    }

    fun bodyWeight(entry: BodyWeightLogEntry, operation: String): String {
        return bodyWeightAdapter.toJson(
            BodyWeightSyncPayload(
                operation = operation,
                generatedAt = System.currentTimeMillis(),
                bodyWeightEntry = entry
            )
        )
    }
}

data class FoodEntrySyncPayload(
    val entityType: String = "FoodEntry",
    val operation: String,
    val generatedAt: Long,
    val foodEntry: FoodLogEntry
)

data class WorkoutSyncPayload(
    val entityType: String = "Workout",
    val operation: String,
    val generatedAt: Long,
    val workout: WorkoutLogEntry
)

data class BodyWeightSyncPayload(
    val entityType: String = "BodyWeight",
    val operation: String,
    val generatedAt: Long,
    val bodyWeightEntry: BodyWeightLogEntry
)

