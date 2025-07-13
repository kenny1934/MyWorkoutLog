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
    CALVES, ABS, FOREARMS, TRAPS, LATS, OTHER
}

enum class Equipment {
    BARBELL, DUMBBELL, KETTLEBELL, MACHINE, CABLE, BANDS, RINGS, PARALLETTES, OTHER
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

    @TypeConverter
    fun fromProgramTemplate(value: ProgramTemplate): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toProgramTemplate(value: String): ProgramTemplate {
        return gson.fromJson(value, ProgramTemplate::class.java)
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
    val usesBodyweight: Boolean = false,
    val targetMuscleGroups: List<MuscleGroup>,
    val equipment: List<Equipment>,
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
    val targetMuscleGroups: List<MuscleGroup>,
    val equipment: List<Equipment>,
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
    val bands: String? = null,
    val notes: String? = null,
    val restTimeSeconds: Int? = null, // Actual rest time taken after this set
    val videoReference: String? = null, // Path to video file for form reference
    // ADD THESE TWO PROPERTIES TO SNAPSHOT THE TARGETS
    val targetReps: String? = null,
    val targetSecs: String? = null
)

data class LoggedExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val targetMuscleGroups: List<MuscleGroup>,
    val equipment: List<Equipment>,
    val sets: List<LoggedSet>,
    val isSubstitute: Boolean? = false,
    val notes: String? = null
)

@Entity(tableName = "logged_workout_table")
data class LoggedWorkout(
    @PrimaryKey val id: String,
    val date: String,
    val name: String? = null,
    val overallComments: String? = null,
    val startTimestamp: Long?,
    val endTimestamp: Long?,
    val bodyweight: Double? = null,
    val performedWeightUnit: String?,

    val activeProgramCycleId: String? = null,
    val programWeekDefinitionId: String? = null,
    val programSessionDefinitionId: String? = null,
    val userCycleName: String? = null,

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
@TypeConverters(Converters::class)
data class ActiveProgramCycle(
    @PrimaryKey val id: Int = 1,
    val cycleUuid: String, // Unique identifier for this specific cycle instance
    val programTemplateId: String, // Keep for reference
    val programTemplateName: String,
    val userCycleName: String, // e.g., "My Hypertrophy Cycle"
    val startDate: String,
    // Map of "weekId_sessionId" to "loggedWorkoutId"
    val completedSessions: Map<String, String>,
    // Snapshot of the program template at cycle creation time
    val cycleProgram: ProgramTemplate
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
    val weight: Double?, // Total effective weight (bodyweight + external for bodyweight exercises)
    val durationSecs: Int?,
    // NEW: Bodyweight exercise breakdown fields
    val bodyweightUsed: Double? = null, // User's bodyweight at time of PR
    val externalWeight: Double? = null, // External/added weight only
    val usesBodyweight: Boolean = false // Flag indicating if exercise uses bodyweight
)

// Data class for cycle workout count query results
data class CycleWorkoutCount(
    val activeProgramCycleId: String,
    val workoutCount: Int
)

// Data class for smart pre-fill suggestions
data class PerformanceSuggestion(
    val suggestedWeight: Double? = null,
    val suggestedReps: Int? = null,
    val suggestedSecs: Int? = null, // Duration for time-based exercises like L-sits
    val suggestedRir: Int? = null,
    val confidence: Float = 0f, // 0.0 to 1.0 confidence level
    val basedonLastWorkout: Boolean = false,
    val daysAgo: Int? = null,
    val progressionType: ProgressionType = ProgressionType.MAINTAIN
)

enum class ProgressionType {
    INCREASE, // Suggest slight increase from last performance
    MAINTAIN, // Suggest same as last performance
    DECREASE  // Suggest slight decrease (deload/recovery)
}

// Analytics Data Classes for Tier 2 Advanced Features

// Volume progression data point for charting
data class VolumeDataPoint(
    val date: String,
    val totalVolume: Double, // weight * reps * sets
    val workoutName: String? = null,
    val cycleId: String? = null
)

// Exercise performance data point for trends
data class ExercisePerformancePoint(
    val date: String,
    val exerciseId: String,
    val exerciseName: String,
    val bestWeight: Double? = null,
    val bestReps: Int? = null,
    val totalVolume: Double? = null,
    val estimated1RM: Double? = null,
    val workoutId: String,
    val cycleId: String? = null,
    // Bodyweight breakdown for better display
    val usesBodyweight: Boolean = false,
    val bodyweight: Double? = null,
    val externalWeight: Double? = null
)

// Personal Record tracking
data class PersonalRecordProgress(
    val exerciseId: String,
    val exerciseName: String,
    val currentPR: PersonalRecord,
    val previousPR: PersonalRecord? = null,
    val improvement: Double? = null, // percentage or absolute improvement
    val improvementType: PRImprovementType
)

enum class PRImprovementType {
    WEIGHT_INCREASE,
    REP_INCREASE,
    DURATION_INCREASE,
    NEW_PR,
    NO_IMPROVEMENT
}

// Cycle comparison data
data class CycleComparison(
    val currentCycleId: String,
    val previousCycleId: String? = null,
    val programTemplateName: String,
    val totalVolumeChange: Double? = null, // percentage change
    val strengthGains: List<ExerciseStrengthGain>,
    val completionRate: Double, // percentage of planned workouts completed
    val averageWorkoutDuration: Long? = null // in minutes
)

data class ExerciseStrengthGain(
    val exerciseId: String,
    val exerciseName: String,
    val strengthGainPercentage: Double? = null,
    val weightIncrease: Double? = null,
    val repIncrease: Int? = null
)

// Weekly/Monthly volume summary
data class VolumeSummary(
    val periodLabel: String, // "Week 1", "January 2024"
    val startDate: String,
    val endDate: String,
    val totalVolume: Double,
    val workoutCount: Int,
    val averageVolumePerWorkout: Double,
    val exerciseBreakdown: List<ExerciseVolumeBreakdown>
)

data class ExerciseVolumeBreakdown(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroups: List<MuscleGroup>,
    val totalVolume: Double,
    val setCount: Int,
    val averageWeight: Double? = null
)

// Muscle group volume distribution
data class MuscleGroupVolume(
    val muscleGroup: MuscleGroup,
    val totalVolume: Double,
    val percentage: Double, // of total weekly/monthly volume
    val exerciseCount: Int
)

// Performance trend analysis
data class PerformanceTrend(
    val exerciseId: String,
    val exerciseName: String,
    val trendDirection: TrendDirection,
    val trendStrength: Double, // 0.0 to 1.0, how strong the trend is
    val dataPoints: List<ExercisePerformancePoint>,
    val recommendedAction: String? = null
)

enum class TrendDirection {
    STRONGLY_IMPROVING,
    SLIGHTLY_IMPROVING,
    STABLE,
    SLIGHTLY_DECLINING,
    STRONGLY_DECLINING,
    INSUFFICIENT_DATA
}