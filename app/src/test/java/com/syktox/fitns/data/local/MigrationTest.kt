package com.syktox.fitns.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FitNsDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private companion object {
        const val TEST_DB = "migration-test"
    }

    @Test
    fun migrate2To3_preservesData() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO food_entries (
                    id, name, mealType, grams, caloriesKcal, proteinGrams,
                    carbohydratesGrams, sugarGrams, fatGrams, saturatedFatGrams,
                    fiberGrams, saltGrams, consumedAt, notes, dataQuality,
                    createdAt, updatedAt, syncStatus
                ) VALUES (
                    'food-1', 'Oatmeal', 'Breakfast', 100.0, 370.0, 13.0,
                    64.0, 1.0, 7.0, 1.2,
                    10.0, 0.1, 1700000000000, '', 'Verified',
                    1700000000000, 1700000000000, 'Synced'
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO nutrition_goals (
                    id, userProfileId, caloriesKcal, proteinGrams, carbohydrateGrams,
                    fatGrams, fiberGrams, waterMilliliters, validFrom,
                    createdAt, updatedAt, syncStatus
                ) VALUES (
                    'goal-1', 'default', 2400.0, 150.0, 260.0,
                    75.0, 30.0, 2500.0, 1700000000000,
                    1700000000000, 1700000000000, 'Synced'
                )
                """.trimIndent()
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migration2To3)

        migrated.use { db ->
            db.query(
                "SELECT micronutrientsJson FROM food_entries WHERE id = 'food-1'"
            ).use { cursor ->
                cursor.moveToFirst()
                assert(cursor.count == 1)
            }
            db.query(
                "SELECT validTo FROM nutrition_goals WHERE id = 'goal-1'"
            ).use { cursor ->
                cursor.moveToFirst()
                assert(cursor.isNull(0))
            }
            db.query("SELECT COUNT(*) FROM nutrient_targets").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getLong(0) == 0L)
            }
        }
    }

    @Test
    fun migrate2To3_createsExpectedSchema() {
        helper.createDatabase(TEST_DB, 2).use { }
        helper.runMigrationsAndValidate(TEST_DB, 3, true, Migration2To3)
    }

    @Test
    fun migrate1To3_runsChainedMigrations() {
        helper.createDatabase(TEST_DB, 1).use { }
        helper.runMigrationsAndValidate(TEST_DB, 3, true, Migration1To2, Migration2To3)
    }
}
