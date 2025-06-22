package com.example.myworkoutlog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        // JSON export implementation would go here
        // For now, return a placeholder
        return ExportResult(
            success = false,
            error = "JSON export not yet implemented"
        )
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