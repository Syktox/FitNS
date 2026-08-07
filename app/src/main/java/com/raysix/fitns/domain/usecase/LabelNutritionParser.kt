package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.Micronutrients
import com.raysix.fitns.domain.model.MicronutrientValue
import com.raysix.fitns.domain.model.NutrientKey
import com.raysix.fitns.domain.model.NutritionFacts

data class LabelParseResult(
    val detectedName: String?,
    val nutrition: NutritionFacts,
    val micronutrients: Micronutrients,
    val perPortion: Boolean,
    val warnings: List<String>
)

class LabelNutritionParser {

    fun parse(rawText: String): LabelParseResult {
        val warnings = mutableListOf<String>()
        val lines = rawText
            .replace("\u00A0", " ")
            .split('\n', '\r')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val perPortion = lines.any { line ->
            Regex("(pro|per)\\s+(portion|serving|mahlzeit)|pro\\s+100\\s*ml").containsMatchIn(line.lowercase())
        }

        var calories: Double? = null
        var protein: Double? = null
        var carbs: Double? = null
        var sugar: Double? = null
        var fat: Double? = null
        var saturatedFat: Double? = null
        var fiber: Double? = null
        var salt: Double? = null
        var sodiumMg: Double? = null
        val micronutrients = mutableMapOf<NutrientKey, MicronutrientValue>()

        lines.forEach { line ->
            val lower = line.lowercase()

            if (lower.contains("kcal") || lower.contains("energie") || lower.contains("energy") ||
                lower.contains("brennwert") || lower.contains("kj")
            ) {
                findUnitValue(line, listOf("kcal", "kj"))?.let { (unit, value) ->
                    if (calories == null) {
                        calories = if (unit == "kj") value / 4.184 else value
                    }
                }
            }
            if (lower.contains("protein") || lower.contains("eiweiß") || lower.contains("protein")) {
                if (protein == null) protein = findValueBeforeDavon(line)
            }
            if (lower.contains("kohlenhydrate") || lower.contains("carbohydrates") || lower.contains("carbs")) {
                if (carbs == null) carbs = findValueBeforeDavon(line)
            }
            if (lower.contains("zucker") || lower.contains("sugar")) {
                if (sugar == null) sugar = findValueAfterDavon(line)
            }
            if (lower.contains("fett") || lower.contains("fat") || lower.contains("gesättigt") ||
                lower.contains("saturated") || lower.contains("saturat")
            ) {
                if (lower.contains("gesättigt") || lower.contains("saturated") || lower.contains("saturat")) {
                    if (saturatedFat == null) saturatedFat = findValueAfterDavon(line)
                } else {
                    if (fat == null) fat = findValueBeforeDavon(line)
                }
            }
            if (lower.contains("ballaststoffe") || lower.contains("fiber")) {
                if (fiber == null) fiber = findValueBeforeDavon(line)
            }
            if (lower.contains("salz") || lower.contains("salt")) {
                if (salt == null) salt = findValueBeforeDavon(line)
            }
            if (lower.contains("natrium") || lower.contains("sodium")) {
                if (sodiumMg == null) sodiumMg = findValueBeforeDavon(line)
            }

            nutrientKeywords.forEach { (key, keywords) ->
                if (micronutrients.containsKey(key)) return@forEach
                if (keywords.any { lower.contains(it) }) {
                    findValueBeforeDavon(line)?.let { amount ->
                        micronutrients[key] = MicronutrientValue(
                            amount = amount,
                            dataQuality = DataQuality.Estimated,
                            source = "OCR label"
                        )
                    }
                }
            }
        }

        val names = lines.takeWhile { line ->
            line.length in 3..60 && !line.any(Char::isDigit) &&
                !line.contains("nährwert", ignoreCase = true) && !line.contains("nutrition", ignoreCase = true) &&
                !line.contains("durchschnittlich", ignoreCase = true)
        }
        val detectedName = names.firstOrNull { it.any(Char::isLetter) }

        if (calories == null) warnings += "No calorie value was recognized. Check the nutrition table manually."
        if (protein == null) warnings += "No protein value was recognized."
        if (carbs == null) warnings += "No carbohydrate value was recognized."
        if (fat == null) warnings += "No fat value was recognized."

        return LabelParseResult(
            detectedName = detectedName,
            nutrition = NutritionFacts(
                caloriesKcal = calories ?: 0.0,
                proteinGrams = protein ?: 0.0,
                carbohydratesGrams = carbs ?: 0.0,
                sugarGrams = sugar ?: 0.0,
                fatGrams = fat ?: 0.0,
                saturatedFatGrams = saturatedFat ?: 0.0,
                fiberGrams = fiber ?: 0.0,
                saltGrams = salt ?: 0.0,
                sodiumMilligrams = sodiumMg
            ),
            micronutrients = Micronutrients(micronutrients),
            perPortion = perPortion,
            warnings = warnings
        )
    }

    private fun findValueBeforeDavon(line: String): Double? {
        val lower = line.lowercase()
        val subordinateIndex = subordinateIndex(lower)
        val endIndex = if (subordinateIndex >= 0) subordinateIndex else line.length
        return firstNumber(line, endIndex)?.value
    }

    private fun findValueAfterDavon(line: String): Double? {
        val lower = line.lowercase()
        val subordinateIndex = subordinateIndex(lower)
        if (subordinateIndex < 0) return null
        return firstNumber(line, endIndex = line.length, startAt = subordinateIndex)?.value
    }

    private fun subordinateIndex(lower: String): Int {
        val davon = lower.indexOf("davon")
        val ofWhich = lower.indexOf("of which")
        return if (davon >= 0 && (ofWhich < 0 || davon < ofWhich)) davon else ofWhich
    }

    private fun findUnitValue(line: String, units: List<String>): Pair<String, Double>? {
        val regex = Regex("(-?\\d+[.,]?\\d*)\\s*(${units.joinToString("|")})", RegexOption.IGNORE_CASE)
        return regex.findAll(line).map { match ->
            match.groupValues[2].lowercase() to match.groupValues[1].toDoubleValue()
        }.lastOrNull()
    }

    private data class NumberMatch(val value: Double, val start: Int, val end: Int)

    private fun firstNumber(line: String, endIndex: Int, startAt: Int = 0): NumberMatch? {
        val regex = Regex("(-?\\d+[.,]?\\d*)")
        return regex.findAll(line).firstOrNull { match ->
            match.range.first >= startAt && match.range.last <= endIndex
        }?.let { match ->
            NumberMatch(match.groupValues[1].toDoubleValue(), match.range.first, match.range.last)
        }
    }

    private fun String.toDoubleValue(): Double {
        return replace(',', '.').toDoubleOrNull() ?: 0.0
    }

    private val nutrientKeywords: List<Pair<NutrientKey, List<String>>> = listOf(
        NutrientKey.Calcium to listOf("calcium"),
        NutrientKey.Magnesium to listOf("magnesium"),
        NutrientKey.Potassium to listOf("kalium", "potassium"),
        NutrientKey.Iron to listOf("eisen", "iron"),
        NutrientKey.Zinc to listOf("zink", "zinc"),
        NutrientKey.Phosphorus to listOf("phosphor", "phosphorus"),
        NutrientKey.Selenium to listOf("selen", "selenium"),
        NutrientKey.Copper to listOf("kupfer", "copper"),
        NutrientKey.Manganese to listOf("mangan", "manganese"),
        NutrientKey.Iodine to listOf("jod", "iodine"),
        NutrientKey.VitaminA to listOf("vitamin a", "retinol"),
        NutrientKey.VitaminB1 to listOf("vitamin b1", "thiamin"),
        NutrientKey.VitaminB2 to listOf("vitamin b2", "riboflavin"),
        NutrientKey.VitaminB3 to listOf("vitamin b3", "niacin"),
        NutrientKey.VitaminB5 to listOf("vitamin b5", "pantothensäure"),
        NutrientKey.VitaminB6 to listOf("vitamin b6"),
        NutrientKey.VitaminB7 to listOf("vitamin b7", "biotin"),
        NutrientKey.VitaminB9 to listOf("vitamin b9", "folsäure", "folic"),
        NutrientKey.VitaminB12 to listOf("vitamin b12", "cobalamin"),
        NutrientKey.VitaminC to listOf("vitamin c", "ascorbinsäure"),
        NutrientKey.VitaminD to listOf("vitamin d"),
        NutrientKey.VitaminE to listOf("vitamin e"),
        NutrientKey.VitaminK to listOf("vitamin k")
    )
}
