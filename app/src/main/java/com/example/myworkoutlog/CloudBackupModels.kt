package com.example.myworkoutlog

import java.text.SimpleDateFormat
import java.util.*

// Cloud backup core data models
data class CloudBackup(
    val id: String,
    val userId: String,
    val deviceId: String,
    val deviceName: String,
    val createdAt: Long,
    val appVersion: String,
    val schemaVersion: String,
    val dataHash: String, // SHA-256 hash for integrity checking
    val encryptedData: String, // Encrypted JSON export data
    val metadata: CloudBackupMetadata,
    val sizeBytes: Long
)

data class CloudBackupMetadata(
    val totalWorkouts: Int,
    val totalExercises: Int,
    val totalPersonalRecords: Int,
    val totalPrograms: Int,
    val dateRangeStart: String?,
    val dateRangeEnd: String?,
    val backupType: CloudBackupType,
    val compressionUsed: Boolean = false
)

enum class CloudBackupType {
    MANUAL, // User-initiated backup
    AUTOMATIC, // Scheduled backup
    EXPORT // Created from export feature
}

// Cloud operation results
sealed class CloudResult<T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error<T>(val exception: Exception, val message: String) : CloudResult<T>()
    data class Loading<T>(val progress: Float = 0f) : CloudResult<T>()
}

// Cloud backup list item for UI
data class CloudBackupItem(
    val id: String,
    val deviceName: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val metadata: CloudBackupMetadata,
    val isCompatible: Boolean,
    val needsUpgrade: Boolean
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(createdAt))
    
    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "${sizeBytes}B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024}KB"
            else -> "${"%.1f".format(sizeBytes / (1024f * 1024f))}MB"
        }
}

// Cloud service authentication status
data class CloudAuthStatus(
    val isSignedIn: Boolean,
    val userEmail: String?,
    val userName: String?,
    val provider: CloudProvider,
    val hasPermissions: Boolean
)

enum class CloudProvider {
    GOOGLE_DRIVE,
    NONE
}

// Cloud backup operation progress
data class CloudBackupProgress(
    val stage: CloudBackupStage,
    val message: String,
    val progress: Float = 0f,
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L
)

enum class CloudBackupStage {
    PREPARING,
    ENCRYPTING,
    UPLOADING,
    DOWNLOADING,
    DECRYPTING,
    VALIDATING,
    IMPORTING,
    COMPLETED,
    ERROR
}

// Cloud restore operation result
data class CloudRestoreResult(
    val success: Boolean,
    val importedRecords: Int = 0,
    val skippedRecords: Int = 0,
    val errorRecords: Int = 0,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val backupInfo: CloudBackupItem? = null
)

// Cloud storage interface
interface CloudStorageProvider {
    suspend fun authenticate(): CloudResult<CloudAuthStatus>
    suspend fun signOut(): CloudResult<Unit>
    suspend fun getAuthStatus(): CloudAuthStatus
    
    suspend fun uploadBackup(backup: CloudBackup): CloudResult<String>
    suspend fun downloadBackup(backupId: String): CloudResult<CloudBackup>
    suspend fun listBackups(): CloudResult<List<CloudBackupItem>>
    suspend fun deleteBackup(backupId: String): CloudResult<Unit>
    
    suspend fun getStorageInfo(): CloudResult<CloudStorageInfo>
}

data class CloudStorageInfo(
    val totalSpaceBytes: Long,
    val usedSpaceBytes: Long,
    val availableSpaceBytes: Long,
    val backupSpaceUsedBytes: Long
) {
    val usagePercentage: Float
        get() = if (totalSpaceBytes > 0) (usedSpaceBytes.toFloat() / totalSpaceBytes) * 100f else 0f
    
    val formattedTotalSpace: String
        get() = formatBytes(totalSpaceBytes)
    
    val formattedUsedSpace: String
        get() = formatBytes(usedSpaceBytes)
    
    val formattedAvailableSpace: String
        get() = formatBytes(availableSpaceBytes)
    
    val formattedBackupSpace: String
        get() = formatBytes(backupSpaceUsedBytes)
    
    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024f)}KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024f * 1024f))}MB"
        else -> "${"%.2f".format(bytes / (1024f * 1024f * 1024f))}GB"
    }
}

// Cloud backup preferences
data class CloudBackupPreferences(
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: AutoBackupFrequency = AutoBackupFrequency.WEEKLY,
    val backupOnlyOnWifi: Boolean = true,
    val keepLocalBackups: Int = 5, // Number of local backups to keep
    val keepCloudBackups: Int = 10, // Number of cloud backups to keep
    val encryptionEnabled: Boolean = true
)

enum class AutoBackupFrequency(val displayName: String, val intervalHours: Long) {
    DAILY("Daily", 24),
    WEEKLY("Weekly", 24 * 7),
    MONTHLY("Monthly", 24 * 30)
}