package com.syktox.fitns.domain.model

object DefaultNutrientTargets {
    val targets: List<NutrientTarget> = listOf(
        NutrientTarget(NutrientKey.Calcium, 1000.0, "mg"),
        NutrientTarget(NutrientKey.Magnesium, 400.0, "mg"),
        NutrientTarget(NutrientKey.Potassium, 3500.0, "mg"),
        NutrientTarget(NutrientKey.Sodium, 1500.0, "mg"),
        NutrientTarget(NutrientKey.Iron, 12.0, "mg"),
        NutrientTarget(NutrientKey.Zinc, 10.0, "mg"),
        NutrientTarget(NutrientKey.Phosphorus, 700.0, "mg"),
        NutrientTarget(NutrientKey.Selenium, 55.0, "µg"),
        NutrientTarget(NutrientKey.Copper, 1.0, "mg"),
        NutrientTarget(NutrientKey.Manganese, 2.0, "mg"),
        NutrientTarget(NutrientKey.Iodine, 150.0, "µg"),
        NutrientTarget(NutrientKey.VitaminA, 800.0, "µg"),
        NutrientTarget(NutrientKey.VitaminB1, 1.2, "mg"),
        NutrientTarget(NutrientKey.VitaminB2, 1.3, "mg"),
        NutrientTarget(NutrientKey.VitaminB3, 16.0, "mg"),
        NutrientTarget(NutrientKey.VitaminB5, 5.0, "mg"),
        NutrientTarget(NutrientKey.VitaminB6, 1.5, "mg"),
        NutrientTarget(NutrientKey.VitaminB7, 30.0, "µg"),
        NutrientTarget(NutrientKey.VitaminB9, 300.0, "µg"),
        NutrientTarget(NutrientKey.VitaminB12, 2.4, "µg"),
        NutrientTarget(NutrientKey.VitaminC, 100.0, "mg"),
        NutrientTarget(NutrientKey.VitaminD, 20.0, "µg"),
        NutrientTarget(NutrientKey.VitaminE, 12.0, "mg"),
        NutrientTarget(NutrientKey.VitaminK, 70.0, "µg")
    )
}
