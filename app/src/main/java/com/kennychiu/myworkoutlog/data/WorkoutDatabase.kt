package com.kennychiu.myworkoutlog.data

import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Exercise::class,
        WorkoutTemplate::class,
        LoggedWorkout::class,
        ProgramTemplate::class,
        ActiveProgramCycle::class,
        PersonalRecord::class,
        BodyweightEntry::class,
    ],
    version = 26,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun loggedWorkoutDao(): LoggedWorkoutDao
    abstract fun programTemplateDao(): ProgramTemplateDao
    abstract fun activeCycleDao(): ActiveCycleDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun bodyweightDao(): BodyweightDao

    companion object {
        // v21 → v22: added ProgramWeekDefinition.isDeloadWeek. The field lives inside
        // the JSON blob stored in program_template_table.weeks (and inside cycleProgram
        // on active_program_cycle_table), so there is no SQL column to add. Gson reads
        // missing booleans as false, which matches the Kotlin default.
        val MIGRATION_21_22: Migration = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op: schema unchanged; only the JSON shape inside an existing TEXT column.
            }
        }

        // v22 → v23: added ProgramWeekDefinition.targetRir (nullable String).
        // Same JSON-blob-only story as v21→v22 — Gson leaves missing fields null.
        val MIGRATION_22_23: Migration = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op: schema unchanged; only the JSON shape inside an existing TEXT column.
            }
        }

        // v23 → v24: added TemplateExerciseSet.targetWeight and LoggedSet.targetWeight
        // (both nullable String). Same JSON-blob-only story — lives inside the TEXT
        // columns workout_template_table.templateExercises and
        // logged_workout_table.loggedExercises.
        val MIGRATION_23_24: Migration = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op: schema unchanged; only the JSON shape inside an existing TEXT column.
            }
        }

        // v24 → v25: added TemplateExercise.progressionScheme (+ per-scheme params).
        // Same JSON-blob-only story as v23→v24 — Gson leaves missing fields null, so
        // old templates open cleanly with no scheme configured.
        val MIGRATION_24_25: Migration = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op: schema unchanged; only the JSON shape inside an existing TEXT column.
            }
        }

        // v25 → v26: added LoggedExercise.originalExerciseId / originalExerciseName so
        // a substituted exercise records what it replaced. Same JSON-blob-only story —
        // lives inside logged_workout_table.loggedExercises.
        val MIGRATION_25_26: Migration = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op: schema unchanged; only the JSON shape inside an existing TEXT column.
            }
        }

        // Migrations from v21 onwards MUST be added here. No new schema changes are
        // allowed without a Migration object — prior dev history wiped user data on
        // every schema bump and that is not acceptable going forward.
        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24,
            MIGRATION_24_25,
            MIGRATION_25_26,
        )

        // Versions 1–20 were only ever opened under fallbackToDestructiveMigration.
        // Any device still holding one of those versions would have been wiped anyway,
        // so we explicitly allow a destructive upgrade from them and only from them.
        private val LEGACY_DEV_VERSIONS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database",
                )
                    .addMigrations(*MIGRATIONS)
                    .fallbackToDestructiveMigrationFrom(*LEGACY_DEV_VERSIONS)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
