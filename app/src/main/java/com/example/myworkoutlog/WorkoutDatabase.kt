package com.example.myworkoutlog // Your package name

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// @Database marks this as the main database class.
// 'entities' lists all the Entity classes (tables) for this database.
// 'version' is important. If you change your table structure later, you must increase this number.
@Database(entities = [Exercise::class, WorkoutTemplate::class, LoggedWorkout::class, ProgramTemplate::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class) // Also tell the database about our converters
abstract class WorkoutDatabase : RoomDatabase() {

    // The database needs to know about each DAO
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun loggedWorkoutDao(): LoggedWorkoutDao
    abstract fun programTemplateDao(): ProgramTemplateDao

    // This 'companion object' block makes it so there's only ONE instance
    // of the database in the whole app (a singleton pattern).
    companion object {
        // @Volatile means that writes to this field are immediately made visible to other threads.
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            // Return the existing instance if it exists, otherwise create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}