package com.syktox.fitns.domain.model

enum class NutrientKey(val label: String, val unit: String, val type: NutrientType) {
    Calcium("Calcium", "mg", NutrientType.Mineral),
    Magnesium("Magnesium", "mg", NutrientType.Mineral),
    Potassium("Potassium", "mg", NutrientType.Mineral),
    Sodium("Sodium", "mg", NutrientType.Mineral),
    Iron("Iron", "mg", NutrientType.Mineral),
    Zinc("Zinc", "mg", NutrientType.Mineral),
    Phosphorus("Phosphorus", "mg", NutrientType.Mineral),
    Selenium("Selenium", "µg", NutrientType.Mineral),
    Copper("Copper", "mg", NutrientType.Mineral),
    Manganese("Manganese", "mg", NutrientType.Mineral),
    Iodine("Iodine", "µg", NutrientType.Mineral),
    VitaminA("Vitamin A", "µg", NutrientType.Vitamin),
    VitaminB1("Vitamin B1", "mg", NutrientType.Vitamin),
    VitaminB2("Vitamin B2", "mg", NutrientType.Vitamin),
    VitaminB3("Vitamin B3", "mg", NutrientType.Vitamin),
    VitaminB5("Vitamin B5", "mg", NutrientType.Vitamin),
    VitaminB6("Vitamin B6", "mg", NutrientType.Vitamin),
    VitaminB7("Vitamin B7", "µg", NutrientType.Vitamin),
    VitaminB9("Vitamin B9", "µg", NutrientType.Vitamin),
    VitaminB12("Vitamin B12", "µg", NutrientType.Vitamin),
    VitaminC("Vitamin C", "mg", NutrientType.Vitamin),
    VitaminD("Vitamin D", "µg", NutrientType.Vitamin),
    VitaminE("Vitamin E", "mg", NutrientType.Vitamin),
    VitaminK("Vitamin K", "µg", NutrientType.Vitamin)
}

enum class NutrientType {
    Mineral,
    Vitamin
}

data class MicronutrientValue(
    val amount: Double,
    val dataQuality: DataQuality = DataQuality.Verified,
    val source: String? = null
)

data class Micronutrients(
    val values: Map<NutrientKey, MicronutrientValue> = emptyMap()
) {
    operator fun plus(other: Micronutrients): Micronutrients {
        if (other.values.isEmpty()) return this
        if (this.values.isEmpty()) return other
        val merged = LinkedHashMap<NutrientKey, MicronutrientValue>()
        val allKeys = values.keys + other.values.keys
        allKeys.forEach { key ->
            val left = values[key]
            val right = other.values[key]
            merged[key] = when {
                left == null -> right!!
                right == null -> left!!
                else -> MicronutrientValue(
                    amount = left.amount + right.amount,
                    dataQuality = weakestDataQuality(left.dataQuality, right.dataQuality),
                    source = mergeSources(left.source, right.source)
                )
            }
        }
        return Micronutrients(merged)
    }
}

data class NutrientTarget(
    val key: NutrientKey,
    val targetAmount: Double,
    val unit: String,
    val source: String = "Reference value"
)

data class NutrientAggregate(
    val key: NutrientKey,
    val consumed: Double?,
    val target: Double?,
    val dataQuality: DataQuality,
    val source: String?,
    val entriesWithData: Int,
    val totalEntries: Int
) {
    val hasTarget: Boolean get() = target != null && target > 0.0
    val hasData: Boolean get() = consumed != null
    val percent: Float
        get() = if (!hasData || !hasTarget) 0f else (consumed!! / target!!).coerceIn(0.0, 1.0).toFloat()
    val remaining: Double?
        get() = if (!hasData || !hasTarget) null else (target!! - consumed!!).coerceAtLeast(0.0)
    val label: String get() = key.label
    val unit: String get() = key.unit
}

data class NutrientTrendPoint(
    val dayStartMillis: Long,
    val consumed: Double?
)

private fun weakestDataQuality(a: DataQuality, b: DataQuality): DataQuality {
    val rank = mapOf(DataQuality.Verified to 0, DataQuality.Estimated to 1, DataQuality.Missing to 2)
    return if (rank[a]!! >= rank[b]!!) a else b
}

private fun mergeSources(a: String?, b: String?): String? {
    val unique = listOfNotNull(a, b).distinct()
    return if (unique.size == 1) unique.first() else unique.joinToString(", ")
}
