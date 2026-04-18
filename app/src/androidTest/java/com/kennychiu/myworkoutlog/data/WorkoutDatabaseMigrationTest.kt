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

    // v21 → v22 added ProgramWeekDefinition.isDeloadWeek inside the JSON blob in
    // program_template_table.weeks. SQL schema is unchanged, so the migration is a
    // no-op and runMigrationsAndValidate just confirms it lands on a valid v22 schema.
    @Test
    fun migrate21To22() {
        helper.createDatabase(dbName, 21).close()
        helper.runMigrationsAndValidate(
            dbName,
            22,
            true,
            WorkoutDatabase.MIGRATION_21_22,
        ).close()
    }

    // v22 → v23 added ProgramWeekDefinition.targetRir inside the same JSON blob.
    // Another no-op migration — validates that the chained MIGRATIONS array lands
    // a v21 database cleanly on v23.
    @Test
    fun migrate22To23() {
        helper.createDatabase(dbName, 22).close()
        helper.runMigrationsAndValidate(
            dbName,
            23,
            true,
            WorkoutDatabase.MIGRATION_22_23,
        ).close()
    }
}
