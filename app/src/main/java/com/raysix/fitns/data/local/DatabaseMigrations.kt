package com.raysix.fitns.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_plans (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                focus TEXT NOT NULL,
                estimatedMinutes INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                serverVersion INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_plan_exercises (
                id TEXT NOT NULL PRIMARY KEY,
                planId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                targetSets INTEGER NOT NULL,
                targetRepMin INTEGER NOT NULL,
                targetRepMax INTEGER NOT NULL,
                restSeconds INTEGER NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                serverVersion INTEGER
            )
            """.trimIndent()
        )
    }
}

val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_entries ADD COLUMN micronutrientsJson TEXT")
        db.execSQL("ALTER TABLE nutrition_goals ADD COLUMN validTo INTEGER")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS nutrient_targets (
                id TEXT NOT NULL PRIMARY KEY,
                userProfileId TEXT NOT NULL,
                nutrientKey TEXT NOT NULL,
                targetAmount REAL NOT NULL,
                unit TEXT NOT NULL,
                source TEXT NOT NULL,
                validFrom INTEGER NOT NULL,
                validTo INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                serverVersion INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_nutrient_targets_userProfileId` ON `nutrient_targets` (`userProfileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_nutrition_goals_userProfileId_validFrom` ON `nutrition_goals` (`userProfileId`, `validFrom`)")
    }
}

val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN setType TEXT NOT NULL DEFAULT 'Normal'")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN completedAt INTEGER")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN restSeconds INTEGER NOT NULL DEFAULT 90")
    }
}

val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_products ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_products ADD COLUMN micronutrientsJson TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_meals (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                serverVersion INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_meal_items (
                id TEXT NOT NULL PRIMARY KEY,
                savedMealId TEXT NOT NULL,
                foodEntrySnapshotId TEXT NOT NULL,
                name TEXT NOT NULL,
                brand TEXT,
                mealType TEXT NOT NULL,
                grams REAL NOT NULL,
                caloriesKcal REAL NOT NULL,
                proteinGrams REAL NOT NULL,
                carbohydratesGrams REAL NOT NULL,
                sugarGrams REAL NOT NULL,
                fatGrams REAL NOT NULL,
                saturatedFatGrams REAL NOT NULL,
                fiberGrams REAL NOT NULL,
                saltGrams REAL NOT NULL,
                sodiumMilligrams REAL,
                micronutrientsJson TEXT,
                notes TEXT NOT NULL,
                dataQuality TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                serverVersion INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_meal_items_savedMealId` ON `saved_meal_items` (`savedMealId`)")
    }
}
