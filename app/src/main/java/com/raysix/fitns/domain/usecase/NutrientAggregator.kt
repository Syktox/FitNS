package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.NutrientAggregate
import com.raysix.fitns.domain.model.NutrientKey
import com.raysix.fitns.domain.model.NutrientTarget
import javax.inject.Inject

class NutrientAggregator @Inject constructor() {
    fun aggregate(
        entries: List<FoodLogEntry>,
        targets: List<NutrientTarget> = emptyList()
    ): List<NutrientAggregate> {
        if (entries.isEmpty()) {
            return NutrientKey.entries.map { key ->
                NutrientAggregate(
                    key = key,
                    consumed = null,
                    target = targets.targetFor(key),
                    dataQuality = DataQuality.Missing,
                    source = null,
                    entriesWithData = 0,
                    totalEntries = 0
                )
            }
        }

        val targetMap = targets.associateBy { it.key }
        return NutrientKey.entries.map { key ->
            val present = entries.mapNotNull { entry ->
                entry.micronutrients.values[key]?.let { value -> value to entry }
            }
            val consumed = if (present.isEmpty()) {
                null
            } else {
                present.sumOf { it.first.amount }
            }
            val quality = if (present.isEmpty()) {
                DataQuality.Missing
            } else {
                present.map { it.first.dataQuality }.reduceOrNull { acc, quality ->
                    weakest(acc, quality)
                } ?: DataQuality.Missing
            }
            val sources = present.map { it.first.source }.filterNotNull().distinct()
            NutrientAggregate(
                key = key,
                consumed = consumed,
                target = targetMap[key]?.targetAmount,
                dataQuality = quality,
                source = sources.joinToString(", ").ifBlank { null },
                entriesWithData = present.size,
                totalEntries = entries.size
            )
        }
    }

    fun coveredNutrients(entries: List<FoodLogEntry>): List<NutrientKey> {
        return NutrientKey.entries.filter { key ->
            entries.any { entry -> entry.micronutrients.values.containsKey(key) }
        }
    }

    private fun weakest(a: DataQuality, b: DataQuality): DataQuality {
        val rank = mapOf(DataQuality.Verified to 0, DataQuality.Estimated to 1, DataQuality.Missing to 2)
        return if (rank[a]!! >= rank[b]!!) a else b
    }

    private fun List<NutrientTarget>.targetFor(key: NutrientKey): Double? {
        return firstOrNull { it.key == key }?.targetAmount
    }
}
