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