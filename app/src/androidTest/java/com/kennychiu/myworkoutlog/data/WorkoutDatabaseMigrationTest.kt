package com.kennychiu.myworkoutlog.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDatabaseMigrationTest {

    private val dbName = "migration-smoke-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WorkoutDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    // Smoke test: the v21 schema JSON is exported under app/schemas/ and
    // MigrationTestHelper can open a fresh DB at that version cleanly.
    // When a real Migration is added later, this file gets a runMigrationsAndValidate() test.
    @Test
    fun canOpenSchemaAtVersion21() {
        helper.createDatabase(dbName, 21).close()

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            WorkoutDatabase::class.java,
            dbName,
        )
            .addMigrations(*WorkoutDatabase.MIGRATIONS)
            .build()
        db.openHelper.writableDatabase.use { }
        db.close()
    }
}
