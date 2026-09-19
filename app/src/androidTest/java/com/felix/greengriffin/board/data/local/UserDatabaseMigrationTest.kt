package com.felix.greengriffin.board.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline for the exported `app/schemas/...UserDatabase/1.json`. Once a version 2 exists this is
 * where `helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)` goes.
 */
@RunWith(AndroidJUnit4::class)
class UserDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        UserDatabase::class.java,
    )

    @Test
    fun version1SchemaMatchesTheExportedSchema() {
        helper.createDatabase(TEST_DB, 1).use { database ->
            database.execSQL(
                "INSERT INTO game_state (game_mode_id, level, game_state_json, updated_at) " +
                    "VALUES (1, 0, '{}', 42)"
            )
            database.execSQL("INSERT INTO completed_levels (game_mode_id, level_index) VALUES (1, 0)")
            database.query("SELECT COUNT(*) FROM game_state").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM completed_levels").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "user-database-migration-test"
    }
}
