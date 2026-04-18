package com.kennychiu.myworkoutlog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

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
    version = 21,
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
        // Migrations from v21 onwards MUST be added here. No new schema changes are
        // allowed without a Migration object — prior dev history wiped user data on
        // every schema bump and that is not acceptable going forward.
        val MIGRATIONS: Array<Migration> = emptyArray()

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
