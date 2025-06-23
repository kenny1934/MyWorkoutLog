package com.example.myworkoutlog

import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.BufferedReader

// Import modes
enum class ImportMode {
    MERGE, // Add new data, keep existing
    REPLACE, // Replace existing data
    VALIDATE_ONLY // Only validate, don't import
}

// Import data types
enum class ImportDataType {
    WORKOUTS, EXERCISES, PERSONAL_RECORDS, PROGRAM_TEMPLATES, COMPLETE_BACKUP, AUTO_DETECT
}

// Import result data class
data class ImportResult(
    val success: Boolean,
    val importedRecords: Int = 0,
    val skippedRecords: Int = 0,
    val errorRecords: Int = 0,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val validationReport: ValidationReport? = null
)

// Validation report
data class ValidationReport(
    val isValid: Boolean,
    val dataType: ImportDataType,
    val totalRecords: Int,
    val schemaVersion: String?,
    val appVersion: String?,
    val exportDate: String?,
    val issues: List<ValidationIssue> = emptyList()
)

data class ValidationIssue(
    val type: IssueType,
    val message: String,
    val recordIndex: Int? = null,
    val fieldName: String? = null
)

enum class IssueType {
    ERROR, WARNING, INFO
}

// Import options configuration
data class ImportOptions(
    val mode: ImportMode,
    val dataType: ImportDataType = ImportDataType.AUTO_DETECT,
    val filePath: String,
    val allowSchemaUpgrade: Boolean = true,
    val skipDuplicates: Boolean = true,
    val validateBeforeImport: Boolean = true
)

class ImportRepository(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val personalRecordDao: PersonalRecordDao,
    private val programDao: ProgramTemplateDao,
    private val activeCycleDao: ActiveCycleDao
) {

    private val gson = Gson()

    // MAIN IMPORT FUNCTIONS

    suspend fun importData(options: ImportOptions): ImportResult {
        return try {
            // First validate the file
            val validationResult = validateImportFile(options.filePath, options.dataType)
            
            if (!validationResult.isValid) {
                return ImportResult(
                    success = false,
                    errors = validationResult.issues.filter { it.type == IssueType.ERROR }.map { it.message },
                    warnings = validationResult.issues.filter { it.type == IssueType.WARNING }.map { it.message },
                    validationReport = validationResult
                )
            }

            // If validation only, return validation result
            if (options.mode == ImportMode.VALIDATE_ONLY) {
                return ImportResult(
                    success = true,
                    validationReport = validationResult
                )
            }

            // Perform the actual import
            when (validationResult.dataType) {
                ImportDataType.WORKOUTS -> importWorkouts(options)
                ImportDataType.EXERCISES -> importExercises(options)
                ImportDataType.PERSONAL_RECORDS -> importPersonalRecords(options)
                ImportDataType.PROGRAM_TEMPLATES -> importProgramTemplates(options)
                ImportDataType.COMPLETE_BACKUP -> importCompleteBackup(options)
                ImportDataType.AUTO_DETECT -> {
                    return ImportResult(
                        success = false,
                        errors = listOf("Auto-detection failed. Please specify data type manually.")
                    )
                }
            }

        } catch (e: Exception) {
            ImportResult(
                success = false,
                errors = listOf("Import failed: ${e.message}")
            )
        }
    }

    // VALIDATION FUNCTIONS

    suspend fun validateImportFile(filePath: String, expectedType: ImportDataType = ImportDataType.AUTO_DETECT): ValidationReport {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return ValidationReport(
                    isValid = false,
                    dataType = ImportDataType.AUTO_DETECT,
                    totalRecords = 0,
                    schemaVersion = null,
                    appVersion = null,
                    exportDate = null,
                    issues = listOf(ValidationIssue(IssueType.ERROR, "File not found: $filePath"))
                )
            }

            when {
                filePath.endsWith(".json", ignoreCase = true) -> validateJSONFile(file, expectedType)
                filePath.endsWith(".csv", ignoreCase = true) -> validateCSVFile(file, expectedType)
                else -> ValidationReport(
                    isValid = false,
                    dataType = ImportDataType.AUTO_DETECT,
                    totalRecords = 0,
                    schemaVersion = null,
                    appVersion = null,
                    exportDate = null,
                    issues = listOf(ValidationIssue(IssueType.ERROR, "Unsupported file format. Only JSON and CSV are supported."))
                )
            }

        } catch (e: Exception) {
            ValidationReport(
                isValid = false,
                dataType = ImportDataType.AUTO_DETECT,
                totalRecords = 0,
                schemaVersion = null,
                appVersion = null,
                exportDate = null,
                issues = listOf(ValidationIssue(IssueType.ERROR, "Validation failed: ${e.message}"))
            )
        }
    }

    private fun validateJSONFile(file: File, expectedType: ImportDataType): ValidationReport {
        return try {
            val jsonContent = file.readText()
            val jsonObject = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
            
            // Extract metadata
            val metadata = jsonObject["metadata"] as? Map<String, Any>
            val exportType = metadata?.get("exportType") as? String
            val schemaVersion = metadata?.get("schemaVersion") as? String
            val appVersion = metadata?.get("appVersion") as? String
            val exportDate = metadata?.get("exportDate") as? String
            
            // Determine data type
            val detectedType = when (exportType) {
                "workouts" -> ImportDataType.WORKOUTS
                "exercises" -> ImportDataType.EXERCISES
                "personal_records" -> ImportDataType.PERSONAL_RECORDS
                "program_templates" -> ImportDataType.PROGRAM_TEMPLATES
                "complete_backup" -> ImportDataType.COMPLETE_BACKUP
                else -> ImportDataType.AUTO_DETECT
            }

            // Validate data type matches expected
            val issues = mutableListOf<ValidationIssue>()
            if (expectedType != ImportDataType.AUTO_DETECT && detectedType != expectedType) {
                issues.add(ValidationIssue(
                    IssueType.WARNING,
                    "Expected $expectedType but found $detectedType"
                ))
            }

            // Check schema version compatibility
            val currentSchemaVersion = "19"
            if (schemaVersion != null && schemaVersion != currentSchemaVersion) {
                if (schemaVersion.toIntOrNull() ?: 0 > currentSchemaVersion.toInt()) {
                    issues.add(ValidationIssue(
                        IssueType.ERROR,
                        "Schema version $schemaVersion is newer than supported version $currentSchemaVersion"
                    ))
                } else {
                    issues.add(ValidationIssue(
                        IssueType.WARNING,
                        "Schema version $schemaVersion is older than current version $currentSchemaVersion. Data migration may be required."
                    ))
                }
            }

            // Count records
            val recordCount = when (detectedType) {
                ImportDataType.WORKOUTS -> (jsonObject["workouts"] as? List<*>)?.size ?: 0
                ImportDataType.EXERCISES -> (jsonObject["exercises"] as? List<*>)?.size ?: 0
                ImportDataType.PERSONAL_RECORDS -> (jsonObject["personalRecords"] as? List<*>)?.size ?: 0
                ImportDataType.PROGRAM_TEMPLATES -> (jsonObject["programTemplates"] as? List<*>)?.size ?: 0
                ImportDataType.COMPLETE_BACKUP -> {
                    val workouts = (jsonObject["workouts"] as? List<*>)?.size ?: 0
                    val exercises = (jsonObject["exercises"] as? List<*>)?.size ?: 0
                    val prs = (jsonObject["personalRecords"] as? List<*>)?.size ?: 0
                    val programs = (jsonObject["programTemplates"] as? List<*>)?.size ?: 0
                    workouts + exercises + prs + programs
                }
                else -> 0
            }

            ValidationReport(
                isValid = issues.none { it.type == IssueType.ERROR },
                dataType = detectedType,
                totalRecords = recordCount,
                schemaVersion = schemaVersion,
                appVersion = appVersion,
                exportDate = exportDate,
                issues = issues
            )

        } catch (e: JsonSyntaxException) {
            ValidationReport(
                isValid = false,
                dataType = ImportDataType.AUTO_DETECT,
                totalRecords = 0,
                schemaVersion = null,
                appVersion = null,
                exportDate = null,
                issues = listOf(ValidationIssue(IssueType.ERROR, "Invalid JSON format: ${e.message}"))
            )
        }
    }

    private fun validateCSVFile(file: File, expectedType: ImportDataType): ValidationReport {
        return try {
            val reader = BufferedReader(FileReader(file))
            val firstLine = reader.readLine()
            reader.close()

            if (firstLine == null) {
                return ValidationReport(
                    isValid = false,
                    dataType = ImportDataType.AUTO_DETECT,
                    totalRecords = 0,
                    schemaVersion = null,
                    appVersion = null,
                    exportDate = null,
                    issues = listOf(ValidationIssue(IssueType.ERROR, "CSV file is empty"))
                )
            }

            // Detect CSV type based on headers
            val detectedType = when {
                firstLine.contains("Date,Workout Name,Start Time") -> ImportDataType.WORKOUTS
                firstLine.contains("Exercise Name,Target Muscle Groups") -> ImportDataType.EXERCISES
                firstLine.contains("Exercise Name,PR Type,Weight,Reps") -> ImportDataType.PERSONAL_RECORDS
                firstLine.contains("Program Name,Week Label") -> ImportDataType.PROGRAM_TEMPLATES
                firstLine.contains("=== WORKOUTS ===") -> ImportDataType.COMPLETE_BACKUP
                else -> ImportDataType.AUTO_DETECT
            }

            // Count lines (approximate record count)
            val lineCount = file.readLines().size - 1 // Subtract header

            ValidationReport(
                isValid = detectedType != ImportDataType.AUTO_DETECT,
                dataType = detectedType,
                totalRecords = lineCount.coerceAtLeast(0),
                schemaVersion = null,
                appVersion = null,
                exportDate = null,
                issues = if (detectedType == ImportDataType.AUTO_DETECT) {
                    listOf(ValidationIssue(IssueType.ERROR, "Could not detect CSV data type from headers"))
                } else {
                    emptyList()
                }
            )

        } catch (e: Exception) {
            ValidationReport(
                isValid = false,
                dataType = ImportDataType.AUTO_DETECT,
                totalRecords = 0,
                schemaVersion = null,
                appVersion = null,
                exportDate = null,
                issues = listOf(ValidationIssue(IssueType.ERROR, "CSV validation failed: ${e.message}"))
            )
        }
    }

    // IMPORT IMPLEMENTATION FUNCTIONS

    private suspend fun importWorkouts(options: ImportOptions): ImportResult {
        try {
            val jsonContent = File(options.filePath).readText()
            val jsonObject = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
            val workoutsData = jsonObject["workouts"] as List<Map<String, Any>>
            
            var imported = 0
            var skipped = 0
            var errors = 0
            val errorMessages = mutableListOf<String>()

            workoutsData.forEach { workoutMap ->
                try {
                    val workout = gson.fromJson(gson.toJson(workoutMap), LoggedWorkout::class.java)
                    
                    if (options.skipDuplicates) {
                        // Check if workout already exists
                        val existing = loggedWorkoutDao.getLoggedWorkoutById(workout.id).first()
                        if (existing != null) {
                            skipped++
                            return@forEach
                        }
                    }
                    
                    when (options.mode) {
                        ImportMode.MERGE -> {
                            loggedWorkoutDao.insert(workout)
                            imported++
                        }
                        ImportMode.REPLACE -> {
                            loggedWorkoutDao.insert(workout) // Room will replace due to OnConflictStrategy.REPLACE
                            imported++
                        }
                        ImportMode.VALIDATE_ONLY -> {
                            // Already validated, nothing to do
                        }
                    }
                } catch (e: Exception) {
                    errors++
                    errorMessages.add("Workout import error: ${e.message}")
                }
            }

            return ImportResult(
                success = errors == 0,
                importedRecords = imported,
                skippedRecords = skipped,
                errorRecords = errors,
                errors = errorMessages
            )

        } catch (e: Exception) {
            return ImportResult(
                success = false,
                errors = listOf("Workout import failed: ${e.message}")
            )
        }
    }

    private suspend fun importExercises(options: ImportOptions): ImportResult {
        try {
            val jsonContent = File(options.filePath).readText()
            val jsonObject = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
            val exercisesData = jsonObject["exercises"] as List<Map<String, Any>>
            
            var imported = 0
            var skipped = 0
            var errors = 0
            val errorMessages = mutableListOf<String>()

            exercisesData.forEach { exerciseMap ->
                try {
                    val exercise = gson.fromJson(gson.toJson(exerciseMap), Exercise::class.java)
                    
                    if (options.skipDuplicates) {
                        // Check if exercise already exists by name
                        val allExercises = exerciseDao.getAllExercises().first()
                        val existing = allExercises.find { it.name == exercise.name }
                        if (existing != null) {
                            skipped++
                            return@forEach
                        }
                    }
                    
                    when (options.mode) {
                        ImportMode.MERGE -> {
                            exerciseDao.insert(exercise)
                            imported++
                        }
                        ImportMode.REPLACE -> {
                            exerciseDao.insert(exercise) // Room will replace due to OnConflictStrategy.REPLACE
                            imported++
                        }
                        ImportMode.VALIDATE_ONLY -> {
                            // Already validated, nothing to do
                        }
                    }
                } catch (e: Exception) {
                    errors++
                    errorMessages.add("Exercise import error: ${e.message}")
                }
            }

            return ImportResult(
                success = errors == 0,
                importedRecords = imported,
                skippedRecords = skipped,
                errorRecords = errors,
                errors = errorMessages
            )

        } catch (e: Exception) {
            return ImportResult(
                success = false,
                errors = listOf("Exercise import failed: ${e.message}")
            )
        }
    }

    private suspend fun importPersonalRecords(options: ImportOptions): ImportResult {
        try {
            val jsonContent = File(options.filePath).readText()
            val jsonObject = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
            val prsData = jsonObject["personalRecords"] as List<Map<String, Any>>
            
            var imported = 0
            var skipped = 0
            var errors = 0
            val errorMessages = mutableListOf<String>()

            prsData.forEach { prMap ->
                try {
                    val pr = gson.fromJson(gson.toJson(prMap), PersonalRecord::class.java)
                    
                    if (options.skipDuplicates) {
                        // Check if PR already exists (same exercise, type, date)
                        val existing = personalRecordDao.getAllPRs().first().find { existingPR ->
                            existingPR.exerciseName == pr.exerciseName &&
                            existingPR.type == pr.type &&
                            existingPR.date == pr.date
                        }
                        if (existing != null) {
                            skipped++
                            return@forEach
                        }
                    }
                    
                    when (options.mode) {
                        ImportMode.MERGE -> {
                            personalRecordDao.upsert(pr)
                            imported++
                        }
                        ImportMode.REPLACE -> {
                            personalRecordDao.upsert(pr) // Room will replace due to upsert behavior
                            imported++
                        }
                        ImportMode.VALIDATE_ONLY -> {
                            // Already validated, nothing to do
                        }
                    }
                } catch (e: Exception) {
                    errors++
                    errorMessages.add("Personal record import error: ${e.message}")
                }
            }

            return ImportResult(
                success = errors == 0,
                importedRecords = imported,
                skippedRecords = skipped,
                errorRecords = errors,
                errors = errorMessages
            )

        } catch (e: Exception) {
            return ImportResult(
                success = false,
                errors = listOf("Personal records import failed: ${e.message}")
            )
        }
    }

    private suspend fun importProgramTemplates(options: ImportOptions): ImportResult {
        try {
            val jsonContent = File(options.filePath).readText()
            val jsonObject = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
            val programsData = jsonObject["programTemplates"] as List<Map<String, Any>>
            
            var imported = 0
            var skipped = 0
            var errors = 0
            val errorMessages = mutableListOf<String>()

            programsData.forEach { programMap ->
                try {
                    val program = gson.fromJson(gson.toJson(programMap), ProgramTemplate::class.java)
                    
                    if (options.skipDuplicates) {
                        // Check if program already exists by name
                        val allPrograms = programDao.getAllPrograms().first()
                        val existing = allPrograms.find { it.name == program.name }
                        if (existing != null) {
                            skipped++
                            return@forEach
                        }
                    }
                    
                    when (options.mode) {
                        ImportMode.MERGE -> {
                            programDao.insert(program)
                            imported++
                        }
                        ImportMode.REPLACE -> {
                            programDao.insert(program) // Room will replace due to OnConflictStrategy.REPLACE
                            imported++
                        }
                        ImportMode.VALIDATE_ONLY -> {
                            // Already validated, nothing to do
                        }
                    }
                } catch (e: Exception) {
                    errors++
                    errorMessages.add("Program template import error: ${e.message}")
                }
            }

            return ImportResult(
                success = errors == 0,
                importedRecords = imported,
                skippedRecords = skipped,
                errorRecords = errors,
                errors = errorMessages
            )

        } catch (e: Exception) {
            return ImportResult(
                success = false,
                errors = listOf("Program templates import failed: ${e.message}")
            )
        }
    }

    private suspend fun importCompleteBackup(options: ImportOptions): ImportResult {
        try {
            val jsonContent = File(options.filePath).readText()
            val jsonObject = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
            
            var totalImported = 0
            var totalSkipped = 0
            var totalErrors = 0
            val allErrorMessages = mutableListOf<String>()

            // Import workouts if present
            (jsonObject["workouts"] as? List<Map<String, Any>>)?.let { workoutsData ->
                val workoutOptions = options.copy(dataType = ImportDataType.WORKOUTS)
                val workoutResult = importWorkoutsFromData(workoutsData, workoutOptions)
                totalImported += workoutResult.importedRecords
                totalSkipped += workoutResult.skippedRecords
                totalErrors += workoutResult.errorRecords
                allErrorMessages.addAll(workoutResult.errors)
            }

            // Import exercises if present
            (jsonObject["exercises"] as? List<Map<String, Any>>)?.let { exercisesData ->
                val exerciseOptions = options.copy(dataType = ImportDataType.EXERCISES)
                val exerciseResult = importExercisesFromData(exercisesData, exerciseOptions)
                totalImported += exerciseResult.importedRecords
                totalSkipped += exerciseResult.skippedRecords
                totalErrors += exerciseResult.errorRecords
                allErrorMessages.addAll(exerciseResult.errors)
            }

            // Import personal records if present
            (jsonObject["personalRecords"] as? List<Map<String, Any>>)?.let { prsData ->
                val prOptions = options.copy(dataType = ImportDataType.PERSONAL_RECORDS)
                val prResult = importPersonalRecordsFromData(prsData, prOptions)
                totalImported += prResult.importedRecords
                totalSkipped += prResult.skippedRecords
                totalErrors += prResult.errorRecords
                allErrorMessages.addAll(prResult.errors)
            }

            // Import program templates if present
            (jsonObject["programTemplates"] as? List<Map<String, Any>>)?.let { programsData ->
                val programOptions = options.copy(dataType = ImportDataType.PROGRAM_TEMPLATES)
                val programResult = importProgramTemplatesFromData(programsData, programOptions)
                totalImported += programResult.importedRecords
                totalSkipped += programResult.skippedRecords
                totalErrors += programResult.errorRecords
                allErrorMessages.addAll(programResult.errors)
            }

            return ImportResult(
                success = totalErrors == 0,
                importedRecords = totalImported,
                skippedRecords = totalSkipped,
                errorRecords = totalErrors,
                errors = allErrorMessages
            )

        } catch (e: Exception) {
            return ImportResult(
                success = false,
                errors = listOf("Complete backup import failed: ${e.message}")
            )
        }
    }

    // HELPER FUNCTIONS FOR DATA IMPORT

    private suspend fun importWorkoutsFromData(workoutsData: List<Map<String, Any>>, options: ImportOptions): ImportResult {
        var imported = 0
        var skipped = 0
        var errors = 0
        val errorMessages = mutableListOf<String>()

        workoutsData.forEach { workoutMap ->
            try {
                val workout = gson.fromJson(gson.toJson(workoutMap), LoggedWorkout::class.java)
                
                if (options.skipDuplicates) {
                    val existing = loggedWorkoutDao.getLoggedWorkoutById(workout.id).first()
                    if (existing != null) {
                        skipped++
                        return@forEach
                    }
                }
                
                when (options.mode) {
                    ImportMode.MERGE -> {
                        loggedWorkoutDao.insert(workout)
                        imported++
                    }
                    ImportMode.REPLACE -> {
                        loggedWorkoutDao.insert(workout)
                        imported++
                    }
                    ImportMode.VALIDATE_ONLY -> { /* Nothing to do */ }
                }
            } catch (e: Exception) {
                errors++
                errorMessages.add("Workout import error: ${e.message}")
            }
        }

        return ImportResult(
            success = errors == 0,
            importedRecords = imported,
            skippedRecords = skipped,
            errorRecords = errors,
            errors = errorMessages
        )
    }

    private suspend fun importExercisesFromData(exercisesData: List<Map<String, Any>>, options: ImportOptions): ImportResult {
        var imported = 0
        var skipped = 0
        var errors = 0
        val errorMessages = mutableListOf<String>()

        exercisesData.forEach { exerciseMap ->
            try {
                val exercise = gson.fromJson(gson.toJson(exerciseMap), Exercise::class.java)
                
                if (options.skipDuplicates) {
                    val allExercises = exerciseDao.getAllExercises().first()
                    val existing = allExercises.find { it.name == exercise.name }
                    if (existing != null) {
                        skipped++
                        return@forEach
                    }
                }
                
                when (options.mode) {
                    ImportMode.MERGE -> {
                        exerciseDao.insert(exercise)
                        imported++
                    }
                    ImportMode.REPLACE -> {
                        exerciseDao.insert(exercise)
                        imported++
                    }
                    ImportMode.VALIDATE_ONLY -> { /* Nothing to do */ }
                }
            } catch (e: Exception) {
                errors++
                errorMessages.add("Exercise import error: ${e.message}")
            }
        }

        return ImportResult(
            success = errors == 0,
            importedRecords = imported,
            skippedRecords = skipped,
            errorRecords = errors,
            errors = errorMessages
        )
    }

    private suspend fun importPersonalRecordsFromData(prsData: List<Map<String, Any>>, options: ImportOptions): ImportResult {
        var imported = 0
        var skipped = 0
        var errors = 0
        val errorMessages = mutableListOf<String>()

        prsData.forEach { prMap ->
            try {
                val pr = gson.fromJson(gson.toJson(prMap), PersonalRecord::class.java)
                
                if (options.skipDuplicates) {
                    val existing = personalRecordDao.getAllPRs().first().find { existingPR ->
                        existingPR.exerciseName == pr.exerciseName &&
                        existingPR.type == pr.type &&
                        existingPR.date == pr.date
                    }
                    if (existing != null) {
                        skipped++
                        return@forEach
                    }
                }
                
                when (options.mode) {
                    ImportMode.MERGE -> {
                        personalRecordDao.upsert(pr)
                        imported++
                    }
                    ImportMode.REPLACE -> {
                        personalRecordDao.upsert(pr)
                        imported++
                    }
                    ImportMode.VALIDATE_ONLY -> { /* Nothing to do */ }
                }
            } catch (e: Exception) {
                errors++
                errorMessages.add("Personal record import error: ${e.message}")
            }
        }

        return ImportResult(
            success = errors == 0,
            importedRecords = imported,
            skippedRecords = skipped,
            errorRecords = errors,
            errors = errorMessages
        )
    }

    private suspend fun importProgramTemplatesFromData(programsData: List<Map<String, Any>>, options: ImportOptions): ImportResult {
        var imported = 0
        var skipped = 0
        var errors = 0
        val errorMessages = mutableListOf<String>()

        programsData.forEach { programMap ->
            try {
                val program = gson.fromJson(gson.toJson(programMap), ProgramTemplate::class.java)
                
                if (options.skipDuplicates) {
                    val allPrograms = programDao.getAllPrograms().first()
                    val existing = allPrograms.find { it.name == program.name }
                    if (existing != null) {
                        skipped++
                        return@forEach
                    }
                }
                
                when (options.mode) {
                    ImportMode.MERGE -> {
                        programDao.insert(program)
                        imported++
                    }
                    ImportMode.REPLACE -> {
                        programDao.insert(program)
                        imported++
                    }
                    ImportMode.VALIDATE_ONLY -> { /* Nothing to do */ }
                }
            } catch (e: Exception) {
                errors++
                errorMessages.add("Program template import error: ${e.message}")
            }
        }

        return ImportResult(
            success = errors == 0,
            importedRecords = imported,
            skippedRecords = skipped,
            errorRecords = errors,
            errors = errorMessages
        )
    }

    // UTILITY FUNCTIONS

    suspend fun getImportSummary(filePath: String): ImportSummary? {
        return try {
            val validation = validateImportFile(filePath)
            if (validation.isValid) {
                ImportSummary(
                    dataType = validation.dataType,
                    totalRecords = validation.totalRecords,
                    schemaVersion = validation.schemaVersion,
                    appVersion = validation.appVersion,
                    exportDate = validation.exportDate,
                    isCompatible = validation.issues.none { it.type == IssueType.ERROR }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Import summary data class
data class ImportSummary(
    val dataType: ImportDataType,
    val totalRecords: Int,
    val schemaVersion: String?,
    val appVersion: String?,
    val exportDate: String?,
    val isCompatible: Boolean
)