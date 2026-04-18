package com.kennychiu.myworkoutlog

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Repository for cloud backup and restore operations
 * Orchestrates encryption, cloud storage, and local import/export operations
 */
class CloudBackupRepository(
    private val context: Context,
    private val exportRepository: ExportRepository,
    private val importRepository: ImportRepository,
    private val cloudProvider: CloudStorageProvider
) {
    
    /**
     * Creates and uploads a cloud backup
     */
    suspend fun createCloudBackup(
        backupType: CloudBackupType = CloudBackupType.MANUAL,
        dateRange: DateRange? = null
    ): Flow<CloudResult<String>> = flow {
        try {
            emit(CloudResult.Loading(0.1f))
            
            // Step 1: Check authentication
            val authStatus = cloudProvider.getAuthStatus()
            if (!authStatus.isSignedIn) {
                emit(CloudResult.Error(Exception("Not authenticated"), "Please sign in to continue"))
                return@flow
            }
            
            emit(CloudResult.Loading(0.2f))
            
            // Step 2: Export data to JSON
            val exportOptions = ExportOptions(
                format = ExportFormat.JSON,
                dataType = ExportDataType.COMPLETE_BACKUP,
                dateRange = dateRange,
                includeMetadata = true
            )
            
            val exportResult = exportRepository.exportData(exportOptions)
            if (!exportResult.success) {
                emit(CloudResult.Error(
                    Exception("Export failed"), 
                    exportResult.error ?: "Failed to export data"
                ))
                return@flow
            }
            
            emit(CloudResult.Loading(0.4f))
            
            // Step 3: Encrypt the backup data
            val backupData = exportResult.fileContent ?: throw Exception("No export data generated")
            val encryptionResult = EncryptionUtils.encryptBackupData(context, backupData)
            
            val encryptedData = when (encryptionResult) {
                is EncryptionResult.Success -> encryptionResult.encryptedData
                is EncryptionResult.Error -> {
                    emit(CloudResult.Error(
                        Exception("Encryption failed"), 
                        encryptionResult.message
                    ))
                    return@flow
                }
            }
            
            val dataHash = if (encryptionResult is EncryptionResult.Success) {
                encryptionResult.dataHash
            } else {
                ""
            }
            
            emit(CloudResult.Loading(0.6f))
            
            // Step 4: Create backup metadata
            val backupId = UUID.randomUUID().toString()
            val deviceId = EncryptionUtils.generateDeviceId(context)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            
            val exportSummary = exportRepository.getExportSummary()
            val backupMetadata = CloudBackupMetadata(
                totalWorkouts = exportSummary.totalWorkouts,
                totalExercises = exportSummary.totalExercises,
                totalPersonalRecords = exportSummary.totalPersonalRecords,
                totalPrograms = exportSummary.totalPrograms,
                dateRangeStart = exportSummary.dateRangeStart,
                dateRangeEnd = exportSummary.dateRangeEnd,
                backupType = backupType,
                compressionUsed = false
            )
            
            val cloudBackup = CloudBackup(
                id = backupId,
                userId = authStatus.userEmail ?: "unknown",
                deviceId = deviceId,
                deviceName = deviceName,
                createdAt = System.currentTimeMillis(),
                appVersion = "1.0.0",
                schemaVersion = "19",
                dataHash = dataHash,
                encryptedData = encryptedData,
                metadata = backupMetadata,
                sizeBytes = encryptedData.toByteArray().size.toLong()
            )
            
            emit(CloudResult.Loading(0.8f))
            
            // Step 5: Upload to cloud storage
            val uploadResult = cloudProvider.uploadBackup(cloudBackup)
            when (uploadResult) {
                is CloudResult.Success -> {
                    emit(CloudResult.Loading(1.0f))
                    emit(CloudResult.Success(uploadResult.data))
                }
                is CloudResult.Error -> {
                    emit(CloudResult.Error(uploadResult.exception, uploadResult.message))
                }
                is CloudResult.Loading -> {
                    emit(CloudResult.Loading(0.8f + (uploadResult.progress * 0.2f)))
                }
            }
            
        } catch (e: Exception) {
            emit(CloudResult.Error(e, "Backup creation failed: ${e.message}"))
        }
    }
    
    /**
     * Downloads and restores a cloud backup
     */
    suspend fun restoreCloudBackup(
        backupId: String,
        importMode: ImportMode = ImportMode.MERGE,
        skipDuplicates: Boolean = true
    ): Flow<CloudResult<CloudRestoreResult>> = flow {
        try {
            emit(CloudResult.Loading(0.1f))
            
            // Step 1: Check authentication
            val authStatus = cloudProvider.getAuthStatus()
            if (!authStatus.isSignedIn) {
                emit(CloudResult.Error(Exception("Not authenticated"), "Please sign in to continue"))
                return@flow
            }
            
            emit(CloudResult.Loading(0.2f))
            
            // Step 2: Download backup from cloud
            val downloadResult = cloudProvider.downloadBackup(backupId)
            val cloudBackup = when (downloadResult) {
                is CloudResult.Success -> downloadResult.data
                is CloudResult.Error -> {
                    emit(CloudResult.Error(downloadResult.exception, downloadResult.message))
                    return@flow
                }
                is CloudResult.Loading -> {
                    emit(CloudResult.Loading(0.2f + (downloadResult.progress * 0.3f)))
                    return@flow
                }
            }
            
            emit(CloudResult.Loading(0.5f))
            
            // Step 3: Decrypt backup data
            val decryptionResult = EncryptionUtils.decryptBackupData(context, cloudBackup.encryptedData)
            val backupData = when (decryptionResult) {
                is DecryptionResult.Success -> decryptionResult.decryptedData
                is DecryptionResult.Error -> {
                    emit(CloudResult.Error(
                        Exception("Decryption failed"), 
                        decryptionResult.message
                    ))
                    return@flow
                }
            }
            
            emit(CloudResult.Loading(0.6f))
            
            // Step 4: Verify data integrity
            if (!EncryptionUtils.verifyDataIntegrity(backupData, cloudBackup.dataHash)) {
                emit(CloudResult.Error(
                    Exception("Data integrity check failed"), 
                    "Backup data may be corrupted"
                ))
                return@flow
            }
            
            emit(CloudResult.Loading(0.7f))
            
            // Step 5: Create temporary file for import
            val tempFile = createTempBackupFile(backupData)
            
            try {
                // Step 6: Import backup data
                val importOptions = ImportOptions(
                    mode = importMode,
                    dataType = ImportDataType.COMPLETE_BACKUP,
                    filePath = tempFile.absolutePath,
                    allowSchemaUpgrade = true,
                    skipDuplicates = skipDuplicates,
                    validateBeforeImport = true
                )
                
                val importResult = importRepository.importData(context, importOptions)
                
                emit(CloudResult.Loading(1.0f))
                
                // Create backup item for result
                val backupItem = CloudBackupItem(
                    id = cloudBackup.id,
                    deviceName = cloudBackup.deviceName,
                    createdAt = cloudBackup.createdAt,
                    sizeBytes = cloudBackup.sizeBytes,
                    metadata = cloudBackup.metadata,
                    isCompatible = true,
                    needsUpgrade = false
                )
                
                val restoreResult = CloudRestoreResult(
                    success = importResult.success,
                    importedRecords = importResult.importedRecords,
                    skippedRecords = importResult.skippedRecords,
                    errorRecords = importResult.errorRecords,
                    warnings = importResult.warnings,
                    errors = importResult.errors,
                    backupInfo = backupItem
                )
                
                emit(CloudResult.Success(restoreResult))
                
            } finally {
                // Clean up temporary file
                tempFile.delete()
            }
            
        } catch (e: Exception) {
            emit(CloudResult.Error(e, "Restore failed: ${e.message}"))
        }
    }
    
    /**
     * Lists all available cloud backups
     */
    suspend fun listCloudBackups(): CloudResult<List<CloudBackupItem>> {
        return cloudProvider.listBackups()
    }
    
    /**
     * Deletes a cloud backup
     */
    suspend fun deleteCloudBackup(backupId: String): CloudResult<Unit> {
        return cloudProvider.deleteBackup(backupId)
    }
    
    /**
     * Gets cloud storage information
     */
    suspend fun getCloudStorageInfo(): CloudResult<CloudStorageInfo> {
        return cloudProvider.getStorageInfo()
    }
    
    /**
     * Authenticates with the cloud provider
     */
    suspend fun authenticate(): CloudResult<CloudAuthStatus> {
        return cloudProvider.authenticate()
    }
    
    /**
     * Signs out from the cloud provider
     */
    suspend fun signOut(): CloudResult<Unit> {
        return cloudProvider.signOut()
    }
    
    /**
     * Gets current authentication status
     */
    suspend fun getAuthStatus(): CloudAuthStatus {
        return cloudProvider.getAuthStatus()
    }
    
    /**
     * Validates a cloud backup without importing it
     */
    suspend fun validateCloudBackup(backupId: String): CloudResult<ValidationReport> {
        return try {
            // Download and decrypt backup
            val downloadResult = cloudProvider.downloadBackup(backupId)
            val cloudBackup = when (downloadResult) {
                is CloudResult.Success -> downloadResult.data
                is CloudResult.Error -> return CloudResult.Error(downloadResult.exception, downloadResult.message)
                is CloudResult.Loading -> return CloudResult.Error(Exception("Unexpected state"), "Download in progress")
            }
            
            val decryptionResult = EncryptionUtils.decryptBackupData(context, cloudBackup.encryptedData)
            val backupData = when (decryptionResult) {
                is DecryptionResult.Success -> decryptionResult.decryptedData
                is DecryptionResult.Error -> return CloudResult.Error(Exception("Decryption failed"), decryptionResult.message)
            }
            
            // Verify data integrity
            if (!EncryptionUtils.verifyDataIntegrity(backupData, cloudBackup.dataHash)) {
                return CloudResult.Error(Exception("Data integrity check failed"), "Backup data may be corrupted")
            }
            
            // Create temporary file and validate
            val tempFile = createTempBackupFile(backupData)
            try {
                val validationResult = importRepository.validateImportFile(tempFile.absolutePath, ImportDataType.COMPLETE_BACKUP)
                CloudResult.Success(validationResult)
            } finally {
                tempFile.delete()
            }
            
        } catch (e: Exception) {
            CloudResult.Error(e, "Validation failed: ${e.message}")
        }
    }
    
    /**
     * Creates a temporary file for backup data
     */
    private suspend fun createTempBackupFile(data: String): java.io.File = withContext(Dispatchers.IO) {
        val tempFile = java.io.File.createTempFile("cloud_backup_", ".json", context.cacheDir)
        tempFile.writeText(data)
        tempFile
    }
    
    /**
     * Cleanup old backups based on retention policy
     */
    suspend fun cleanupOldBackups(keepCount: Int = 10): CloudResult<Int> {
        return try {
            val backupsResult = listCloudBackups()
            val backups = when (backupsResult) {
                is CloudResult.Success -> backupsResult.data
                is CloudResult.Error -> return CloudResult.Error(backupsResult.exception, backupsResult.message)
                is CloudResult.Loading -> return CloudResult.Error(Exception("Unexpected state"), "List operation in progress")
            }
            
            // Sort by creation date (newest first) and remove excess
            val sortedBackups = backups.sortedByDescending { it.createdAt }
            val backupsToDelete = sortedBackups.drop(keepCount)
            
            var deletedCount = 0
            for (backup in backupsToDelete) {
                val deleteResult = deleteCloudBackup(backup.id)
                if (deleteResult is CloudResult.Success) {
                    deletedCount++
                }
            }
            
            CloudResult.Success(deletedCount)
        } catch (e: Exception) {
            CloudResult.Error(e, "Cleanup failed: ${e.message}")
        }
    }
}