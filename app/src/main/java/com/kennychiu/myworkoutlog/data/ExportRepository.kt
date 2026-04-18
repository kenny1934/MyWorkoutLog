package com.kennychiu.myworkoutlog.data

import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.*

// Data export formats
enum class ExportFormat {
    CSV, JSON
}

// Export data types
enum class ExportDataType {
    WORKOUTS, EXERCISES, PERSONAL_RECORDS, PROGRAM_TEMPLATES, COMPLETE_BACKUP
}

// Export result data class
data class ExportResult(
    val success: Boolean,
    val fileName: String? = null,
    val filePath: String? = null,
    val fileContent: String? = null,
    val error: String? = null,
    val recordCount: Int = 0,
    val fileSize: Long = 0L
)

// Export options configuration
data class ExportOptions(
    val format: ExportFormat,
    val dataType: ExportDataType,
    val dateRange: DateRange? = null,
    val includeMetadata: Boolean = true,
    val compressData: Boolean = false
)

data class DateRange(
    val startDate: String,
    val endDate: String
)

class ExportRepository(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val personalRecordDao: PersonalRecordDao,
    private val programDao: ProgramTemplateDao,
    private val activeCycleDao: ActiveCycleDao
) {

    // MAIN EXPORT FUNCTIONS

    suspend fun exportData(options: ExportOptions): ExportResult {
        return try {
            when (options.format) {
                ExportFormat.CSV -> exportToCSV(options)
                ExportFormat.JSON -> exportToJSON(options)
            }
        } catch (e: Exception) {
            ExportResult(
                success = false,
                error = "Export failed: ${e.message}"
            )
        }
    }

    // CSV EXPORT FUNCTIONS

    private suspend fun exportToCSV(options: ExportOptions): ExportResult {
        val csvContent = when (options.dataType) {
            ExportDataType.WORKOUTS -> exportWorkoutsToCSV(options.dateRange)
            ExportDataType.EXERCISES -> exportExercisesToCSV()
            ExportDataType.PERSONAL_RECORDS -> exportPersonalRecordsToCSV(options.dateRange)
            ExportDataType.PROGRAM_TEMPLATES -> exportProgramTemplatesToCSV()
            ExportDataType.COMPLETE_BACKUP -> exportCompleteBackupToCSV(options.dateRange)
        }

        val fileName = generateFileName(options.format, options.dataType)
        
        return ExportResult(
            success = true,
            fileName = fileName,
            fileContent = csvContent,
            recordCount = csvContent.lines().size - 1 // Subtract header row
        )
    }

    private suspend fun exportWorkoutsToCSV(dateRange: DateRange?): String {
        val workouts = if (dateRange != null) {
            loggedWorkoutDao.getWorkoutsByDateRange(dateRange.startDate, dateRange.endDate).first()
        } else {
            loggedWorkoutDao.getAllLoggedWorkouts().first()
        }

        val csvBuilder = StringBuilder()
        
        // CSV Header
        csvBuilder.appendLine("Date,Workout Name,Start Time,End Time,Duration (min),Bodyweight,Weight Unit,Cycle,Exercise,Set Number,Weight,Reps,Duration (sec),RIR,Bands,Notes")

        // Export each workout
        workouts.forEach { workout ->
            val startTime = workout.startTimestamp?.let { formatTimestamp(it) } ?: ""
            val endTime = workout.endTimestamp?.let { formatTimestamp(it) } ?: ""
            val durationMin = if (workout.startTimestamp != null && workout.endTimestamp != null) {
                ((workout.endTimestamp - workout.startTimestamp) / 60000).toString()
            } else ""

            workout.loggedExercises.forEach { exercise ->
                exercise.sets.forEachIndexed { setIndex, set ->
                    csvBuilder.appendLine(
                        "${csvEscape(workout.date)}," +
                        "${csvEscape(workout.name ?: "")}," +
                        "${csvEscape(startTime)}," +
                        "${csvEscape(endTime)}," +
                        "${csvEscape(durationMin)}," +
                        "${workout.bodyweight ?: ""}," +
                        "${csvEscape(workout.performedWeightUnit ?: "")}," +
                        "${csvEscape(workout.userCycleName ?: "")}," +
                        "${csvEscape(exercise.exerciseName)}," +
                        "${setIndex + 1}," +
                        "${set.weight ?: ""}," +
                        "${set.reps ?: ""}," +
                        "${set.secs ?: ""}," +
                        "${set.rir ?: ""}," +
                        "${csvEscape(set.bands ?: "")}," +
                        "${csvEscape(set.notes ?: "")}"
                    )
                }
            }
        }

        return csvBuilder.toString()
    }

    private suspend fun exportPersonalRecordsToCSV(dateRange: DateRange?): String {
        val personalRecords = personalRecordDao.getAllPRs().first()
        
        val filteredRecords = if (dateRange != null) {
            personalRecords.filter { pr ->
                pr.date >= dateRange.startDate && pr.date <= dateRange.endDate
            }
        } else {
            personalRecords
        }

        val csvBuilder = StringBuilder()
        
        // CSV Header
        csvBuilder.appendLine("Exercise Name,PR Type,Weight,Reps,Duration (sec),Bodyweight Used,External Weight,Date Achieved,Workout ID")

        filteredRecords.forEach { pr ->
            csvBuilder.appendLine(
                "${csvEscape(pr.exerciseName)}," +
                "${pr.type}," +
                "${pr.weight ?: ""}," +
                "${pr.reps ?: ""}," +
                "${pr.durationSecs ?: ""}," +
                "${pr.bodyweightUsed ?: ""}," +
                "${pr.externalWeight ?: ""}," +
                "${csvEscape(pr.date)}," +
                "${csvEscape(pr.loggedWorkoutId)}"
            )
        }

        return csvBuilder.toString()
    }

    private suspend fun exportExercisesToCSV(): String {
        val exercises = exerciseDao.getAllExercises().first()

        val csvBuilder = StringBuilder()
        
        // CSV Header
        csvBuilder.appendLine("Exercise Name,Target Muscle Groups,Equipment,Uses Bodyweight,Notes")

        exercises.forEach { exercise ->
            csvBuilder.appendLine(
                "${csvEscape(exercise.name)}," +
                "${exercise.targetMuscleGroups.joinToString(";") { it.name }}," +
                "${exercise.equipment.joinToString(";") { it.name }}," +
                "${exercise.usesBodyweight}," +
                "${csvEscape(exercise.notes ?: "")}"
            )
        }

        return csvBuilder.toString()
    }

    private suspend fun exportProgramTemplatesToCSV(): String {
        val programs = programDao.getAllPrograms().first()

        val csvBuilder = StringBuilder()
        
        // CSV Header
        csvBuilder.appendLine("Program Name,Week Label,Week Order,Session Name,Session Order,Workout Template ID")

        programs.forEach { program ->
            program.weeks.forEach { week ->
                week.sessions.forEach { session ->
                    csvBuilder.appendLine(
                        "${csvEscape(program.name)}," +
                        "${csvEscape(week.weekLabel)}," +
                        "${week.order}," +
                        "${csvEscape(session.sessionName)}," +
                        "${session.order}," +
                        "${csvEscape(session.workoutTemplateId)}"
                    )
                }
            }
        }

        return csvBuilder.toString()
    }

    private suspend fun exportCompleteBackupToCSV(dateRange: DateRange?): String {
        val csvBuilder = StringBuilder()
        
        // Add metadata header
        csvBuilder.appendLine("# MyWorkoutLog Complete Backup")
        csvBuilder.appendLine("# Export Date: ${getCurrentDate()}")
        csvBuilder.appendLine("# Date Range: ${dateRange?.let { "${it.startDate} to ${it.endDate}" } ?: "All Data"}")
        csvBuilder.appendLine("")
        
        // Export each data type as separate sections
        csvBuilder.appendLine("=== WORKOUTS ===")
        csvBuilder.appendLine(exportWorkoutsToCSV(dateRange))
        csvBuilder.appendLine("")
        
        csvBuilder.appendLine("=== PERSONAL RECORDS ===")
        csvBuilder.appendLine(exportPersonalRecordsToCSV(dateRange))
        csvBuilder.appendLine("")
        
        csvBuilder.appendLine("=== EXERCISES ===")
        csvBuilder.appendLine(exportExercisesToCSV())
        csvBuilder.appendLine("")
        
        csvBuilder.appendLine("=== PROGRAM TEMPLATES ===")
        csvBuilder.appendLine(exportProgramTemplatesToCSV())

        return csvBuilder.toString()
    }

    // JSON EXPORT FUNCTIONS

    private suspend fun exportToJSON(options: ExportOptions): ExportResult {
        val gson = GsonBuilder().setPrettyPrinting().create()
        
        val jsonContent = when (options.dataType) {
            ExportDataType.WORKOUTS -> exportWorkoutsToJSON(options.dateRange, gson)
            ExportDataType.EXERCISES -> exportExercisesToJSON(gson)
            ExportDataType.PERSONAL_RECORDS -> exportPersonalRecordsToJSON(options.dateRange, gson)
            ExportDataType.PROGRAM_TEMPLATES -> exportProgramTemplatesToJSON(gson)
            ExportDataType.COMPLETE_BACKUP -> exportCompleteBackupToJSON(options.dateRange, gson)
        }

        val fileName = generateFileName(options.format, options.dataType)
        val recordCount = when (options.dataType) {
            ExportDataType.WORKOUTS -> getWorkoutCount(options.dateRange)
            ExportDataType.EXERCISES -> exerciseDao.getAllExercises().first().size
            ExportDataType.PERSONAL_RECORDS -> getPRCount(options.dateRange)
            ExportDataType.PROGRAM_TEMPLATES -> programDao.getAllPrograms().first().size
            ExportDataType.COMPLETE_BACKUP -> getTotalRecordCount(options.dateRange)
        }
        
        return ExportResult(
            success = true,
            fileName = fileName,
            fileContent = jsonContent,
            recordCount = recordCount,
            fileSize = jsonContent.toByteArray().size.toLong()
        )
    }

    private suspend fun exportWorkoutsToJSON(dateRange: DateRange?, gson: Gson): String {
        val workouts = if (dateRange != null) {
            loggedWorkoutDao.getWorkoutsByDateRange(dateRange.startDate, dateRange.endDate).first()
        } else {
            loggedWorkoutDao.getAllLoggedWorkouts().first()
        }

        val exportData = mapOf(
            "metadata" to createMetadata("workouts", dateRange),
            "workouts" to workouts
        )

        return gson.toJson(exportData)
    }

    private suspend fun exportPersonalRecordsToJSON(dateRange: DateRange?, gson: Gson): String {
        val personalRecords = personalRecordDao.getAllPRs().first()
        
        val filteredRecords = if (dateRange != null) {
            personalRecords.filter { pr ->
                pr.date >= dateRange.startDate && pr.date <= dateRange.endDate
            }
        } else {
            personalRecords
        }

        val exportData = mapOf(
            "metadata" to createMetadata("personal_records", dateRange),
            "personalRecords" to filteredRecords
        )

        return gson.toJson(exportData)
    }

    private suspend fun exportExercisesToJSON(gson: Gson): String {
        val exercises = exerciseDao.getAllExercises().first()

        val exportData = mapOf(
            "metadata" to createMetadata("exercises", null),
            "exercises" to exercises
        )

        return gson.toJson(exportData)
    }

    private suspend fun exportProgramTemplatesToJSON(gson: Gson): String {
        val programs = programDao.getAllPrograms().first()

        val exportData = mapOf(
            "metadata" to createMetadata("program_templates", null),
            "programTemplates" to programs
        )

        return gson.toJson(exportData)
    }

    private suspend fun exportCompleteBackupToJSON(dateRange: DateRange?, gson: Gson): String {
        val workouts = if (dateRange != null) {
            loggedWorkoutDao.getWorkoutsByDateRange(dateRange.startDate, dateRange.endDate).first()
        } else {
            loggedWorkoutDao.getAllLoggedWorkouts().first()
        }
        
        val personalRecords = personalRecordDao.getAllPRs().first()
        val filteredPRs = if (dateRange != null) {
            personalRecords.filter { pr ->
                pr.date >= dateRange.startDate && pr.date <= dateRange.endDate
            }
        } else {
            personalRecords
        }
        
        val exercises = exerciseDao.getAllExercises().first()
        val programs = programDao.getAllPrograms().first()
        val activeCycle = activeCycleDao.getActiveCycle().first()

        val exportData = mapOf(
            "metadata" to createMetadata("complete_backup", dateRange),
            "workouts" to workouts,
            "personalRecords" to filteredPRs,
            "exercises" to exercises,
            "programTemplates" to programs,
            "activeCycle" to activeCycle
        )

        return gson.toJson(exportData)
    }

    // Helper functions for JSON export
    private fun createMetadata(dataType: String, dateRange: DateRange?): Map<String, Any> {
        return mapOf(
            "exportType" to dataType,
            "exportDate" to getCurrentDate(),
            "appVersion" to "1.0.0",
            "schemaVersion" to "19", // Current database version
            "dateRange" to if (dateRange != null) {
                mapOf(
                    "startDate" to dateRange.startDate,
                    "endDate" to dateRange.endDate
                )
            } else {
                "all_data"
            }
        )
    }

    private suspend fun getWorkoutCount(dateRange: DateRange?): Int {
        return if (dateRange != null) {
            loggedWorkoutDao.getWorkoutsByDateRange(dateRange.startDate, dateRange.endDate).first().size
        } else {
            loggedWorkoutDao.getAllLoggedWorkouts().first().size
        }
    }

    private suspend fun getPRCount(dateRange: DateRange?): Int {
        val personalRecords = personalRecordDao.getAllPRs().first()
        return if (dateRange != null) {
            personalRecords.filter { pr ->
                pr.date >= dateRange.startDate && pr.date <= dateRange.endDate
            }.size
        } else {
            personalRecords.size
        }
    }

    private suspend fun getTotalRecordCount(dateRange: DateRange?): Int {
        val workouts = getWorkoutCount(dateRange)
        val prs = getPRCount(dateRange)
        val exercises = exerciseDao.getAllExercises().first().size
        val programs = programDao.getAllPrograms().first().size
        return workouts + prs + exercises + programs
    }

    // UTILITY FUNCTIONS

    private fun generateFileName(format: ExportFormat, dataType: ExportDataType): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val typeStr = dataType.name.lowercase().replace("_", "-")
        val extension = format.name.lowercase()
        
        return "myworkoutlog-${typeStr}-${timestamp}.${extension}"
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun csvEscape(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\"" // Escape quotes and wrap in quotes
    }

    // GET EXPORT SUMMARY

    suspend fun getExportSummary(): ExportSummary {
        val totalWorkouts = loggedWorkoutDao.getAllLoggedWorkouts().first().size
        val totalExercises = exerciseDao.getAllExercises().first().size
        val totalPRs = personalRecordDao.getAllPRs().first().size
        val totalPrograms = programDao.getAllPrograms().first().size
        
        val oldestWorkout = loggedWorkoutDao.getAllLoggedWorkouts().first()
            .minByOrNull { it.date }?.date
        val newestWorkout = loggedWorkoutDao.getAllLoggedWorkouts().first()
            .maxByOrNull { it.date }?.date

        return ExportSummary(
            totalWorkouts = totalWorkouts,
            totalExercises = totalExercises,
            totalPersonalRecords = totalPRs,
            totalPrograms = totalPrograms,
            dateRangeStart = oldestWorkout,
            dateRangeEnd = newestWorkout
        )
    }
}

// Export summary data class
data class ExportSummary(
    val totalWorkouts: Int,
    val totalExercises: Int,
    val totalPersonalRecords: Int,
    val totalPrograms: Int,
    val dateRangeStart: String?,
    val dateRangeEnd: String?
)