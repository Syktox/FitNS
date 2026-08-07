package com.syktox.fitns.data.local

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
