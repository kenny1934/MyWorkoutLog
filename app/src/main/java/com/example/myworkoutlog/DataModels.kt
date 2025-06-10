package com.example.myworkoutlog // Make sure this matches your package name!

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson // We'll add this dependency next
import com.google.gson.reflect.TypeToken

// --- Enums are the same as before ---
enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS, GLUTES,
    CALVES, ABS, FOREARMS, TRAPS, LATS, FULL_BODY, UPPER_BODY,
    LOWER_BODY, PUSH, PULL, LEGS, CORE, SKILL_STATIC, SKILL_DYNAMIC, OTHER
}

enum class Equipment {
    BARBELL, DUMBBELL, KETTLEBELL, MACHINE, CABLE, BANDS, BODYWEIGHT, RINGS, TRX, OTHER
}

// --- Type Converters ---
// Room can only store simple types like String, Int, etc.
// These functions teach Room how to convert our complex types (like a List)
// into a simple type (String) and back again.
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> {
        val listType = object : TypeToken<List<MuscleGroup>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromEquipmentList(value: List<Equipment>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toEquipmentList(value: String): List<Equipment> {
        val listType = object : TypeToken<List<Equipment>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromTemplateExerciseList(value: List<TemplateExercise>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toTemplateExerciseList(value: String): List<TemplateExercise> {
        val listType = object : TypeToken<List<TemplateExercise>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromLoggedExerciseList(value: List<LoggedExercise>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLoggedExerciseList(value: String): List<LoggedExercise> {
        val listType = object : TypeToken<List<LoggedExercise>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromProgramWeekDefinitionList(value: List<ProgramWeekDefinition>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toProgramWeekDefinitionList(value: String): List<ProgramWeekDefinition> {
        val listType = object : TypeToken<List<ProgramWeekDefinition>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromPRType(value: PRType): String = value.name

    @TypeConverter
    fun toPRType(value: String): PRType = PRType.valueOf(value)
}


// --- ENTITY CLASS ---
// @Entity tells Room to create a database table for this class.
@Entity(tableName = "exercise_table")
@TypeConverters(Converters::class) // Tell Room to use our converters for this table
data class Exercise(
    // @PrimaryKey tells Room that 'id' is the unique key for each row.
    @PrimaryKey val id: String,
    val name: String,
    val targetMuscleGroups: List<MuscleGroup>,
    val equipment: List<Equipment>,
    val preferredRepRange: String? = null,
    val notes: String? = null,
    val videoLink: String? = null
)


// Represents a single set within a template, e.g., "3 sets of 8-12 reps"
data class TemplateExerciseSet(
    val id: String,
    val targetReps: String? = null,
    val targetSecs: String? = null,
    val targetRIR: String? = null, // Using String for simplicity for now
    val notes: String? = null
)

// Represents an exercise within a template, linking to a master exercise
data class TemplateExercise(
    val id: String,
    val exerciseId: String, // The ID of the exercise from our master exercise_table
    val exerciseName: String, // Stored for easy display
    val sets: List<TemplateExerciseSet>,
    val order: Int,
    val notes: String? = null
)

// This is the main database table for our workout templates
@Entity(tableName = "workout_template_table")
data class WorkoutTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    // We will store the list of exercises as a single JSON string in the database.
    // This is simpler for now than creating complex database relations.
    val templateExercises: List<TemplateExercise>
)

data class LoggedSet(
    val id: String,
    val reps: Int? = null,
    val secs: Int? = null,
    val weight: Double? = null,
    val rir: Int? = null,
    val notes: String? = null
)

data class LoggedExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<LoggedSet>,
    val notes: String? = null
)

@Entity(tableName = "logged_workout_table")
data class LoggedWorkout(
    @PrimaryKey val id: String,
    val date: String,
    val name: String? = null,
    val overallComments: String? = null,
    val durationMinutes: Int? = null,
    val bodyweight: Double? = null,
    val performedWeightUnit: String?,
    // We will store this list as a single JSON string
    val loggedExercises: List<LoggedExercise>,
    val workoutTemplateId: String? = null
)

// Represents a single session within a week, like "Day 1: Push Day"
data class ProgramSessionDefinition(
    val id: String,
    val sessionName: String,
    val workoutTemplateId: String, // Links to a WorkoutTemplate
    val order: Int
)

// Represents a single week within a program
data class ProgramWeekDefinition(
    val id: String,
    val weekLabel: String, // e.g., "Week 1: RIR 3"
    val sessions: List<ProgramSessionDefinition>,
    val order: Int
)

// This is the main database table for our program blueprints
@Entity(tableName = "program_template_table")
data class ProgramTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    // We will store the list of weeks as a single JSON string
    val weeks: List<ProgramWeekDefinition>
)

// Using a simple ID for the primary key, since we'll only have one row.
@Entity(tableName = "active_program_cycle_table")
data class ActiveProgramCycle(
    @PrimaryKey val id: Int = 1,
    val programTemplateId: String,
    val programTemplateName: String,
    val userCycleName: String, // e.g., "My Hypertrophy Cycle"
    val startDate: String,
    // Map of "weekId_sessionId" to "loggedWorkoutId"
    val completedSessions: Map<String, String>
)

// An enum to define the type of PR
enum class PRType {
    MAX_WEIGHT_FOR_REPS,
    MAX_REPS_AT_WEIGHT,
    DURATION
}

// The database table for storing personal records
@Entity(tableName = "personal_record_table")
data class PersonalRecord(
    // We'll create a unique ID for each PR based on the exercise and type
    @PrimaryKey val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val date: String,
    val loggedWorkoutId: String,
    val type: PRType,
    val weightUnit: String?,
    // These values will be set depending on the PR type
    val reps: Int?,
    val weight: Double?,
    val durationSecs: Int?
)