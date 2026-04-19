package com.kennychiu.myworkoutlog.data

import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * Google Drive implementation of CloudStorageProvider
 * Handles authentication, file upload/download, and backup management
 */
class GoogleDriveCloudProvider(private val context: Context) : CloudStorageProvider {
    
    private val gson = Gson()
    private var googleSignInClient: GoogleSignInClient
    private var driveService: Drive? = null
    
    companion object {
        private const val APP_FOLDER_NAME = "MyWorkoutLog Backups"
        private const val BACKUP_FILE_PREFIX = "workout_backup_"
        private const val BACKUP_FILE_EXTENSION = ".mwl" // MyWorkoutLog file extension
        private const val METADATA_KEY = "myworkoutlog_metadata"
    }
    
    init {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA)
            )
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
    }
    
    override suspend fun authenticate(): CloudResult<CloudAuthStatus> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && !GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
                return@withContext CloudResult.Error(
                    Exception("Insufficient permissions"), 
                    "Google Drive permissions not granted"
                )
            }
            
            if (account != null) {
                initializeDriveService(account)
                val authStatus = CloudAuthStatus(
                    isSignedIn = true,
                    userEmail = account.email,
                    userName = account.displayName,
                    provider = CloudProvider.GOOGLE_DRIVE,
                    hasPermissions = true
                )
                CloudResult.Success(authStatus)
            } else {
                val authStatus = CloudAuthStatus(
                    isSignedIn = false,
                    userEmail = null,
                    userName = null,
                    provider = CloudProvider.GOOGLE_DRIVE,
                    hasPermissions = false
                )
                CloudResult.Success(authStatus)
            }
        } catch (e: Exception) {
            CloudResult.Error(e, "Authentication failed: ${e.message}")
        }
    }
    
    override suspend fun signOut(): CloudResult<Unit> = withContext(Dispatchers.IO) {
        try {
            googleSignInClient.signOut()
            driveService = null
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            CloudResult.Error(e, "Sign out failed: ${e.message}")
        }
    }
    
    override suspend fun getAuthStatus(): CloudAuthStatus {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null) {
            CloudAuthStatus(
                isSignedIn = true,
                userEmail = account.email,
                userName = account.displayName,
                provider = CloudProvider.GOOGLE_DRIVE,
                hasPermissions = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
            )
        } else {
            CloudAuthStatus(
                isSignedIn = false,
                userEmail = null,
                userName = null,
                provider = CloudProvider.GOOGLE_DRIVE,
                hasPermissions = false
            )
        }
    }
    
    override suspend fun uploadBackup(backup: CloudBackup): CloudResult<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext CloudResult.Error(
                Exception("Not authenticated"), 
                "Please sign in to Google Drive first"
            )
            
            // Ensure app folder exists
            val folderId = getOrCreateAppFolder(drive)
            
            // Create file metadata
            val fileName = "${BACKUP_FILE_PREFIX}${backup.id}${BACKUP_FILE_EXTENSION}"
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
                description = "MyWorkoutLog backup created on ${Date(backup.createdAt)}"
                // Store backup metadata in file properties
                properties = mapOf(
                    METADATA_KEY to gson.toJson(backup.metadata),
                    "backup_id" to backup.id,
                    "device_id" to backup.deviceId,
                    "device_name" to backup.deviceName,
                    "created_at" to backup.createdAt.toString(),
                    "app_version" to backup.appVersion,
                    "schema_version" to backup.schemaVersion,
                    "data_hash" to backup.dataHash
                )
            }
            
            // Upload file content
            val backupJson = gson.toJson(backup)
            val mediaContent = com.google.api.client.http.InputStreamContent(
                "application/json",
                ByteArrayInputStream(backupJson.toByteArray())
            )
            mediaContent.length = backupJson.toByteArray().size.toLong()
            
            val file = drive.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            
            CloudResult.Success(file.id)
        } catch (e: Exception) {
            CloudResult.Error(e, "Upload failed: ${e.message}")
        }
    }
    
    override suspend fun downloadBackup(backupId: String): CloudResult<CloudBackup> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext CloudResult.Error(
                Exception("Not authenticated"), 
                "Please sign in to Google Drive first"
            )
            
            // Find backup file by backup_id property
            val fileList = drive.files().list()
                .setQ("properties has { key='backup_id' and value='$backupId' }")
                .setFields("files(id,name,properties)")
                .execute()
            
            val file = fileList.files.firstOrNull() ?: return@withContext CloudResult.Error(
                Exception("Backup not found"), 
                "Backup with ID $backupId not found"
            )
            
            // Download file content
            val outputStream = ByteArrayOutputStream()
            drive.files().get(file.id).executeMediaAndDownloadTo(outputStream)
            val backupJson = outputStream.toString()
            
            val backup = gson.fromJson(backupJson, CloudBackup::class.java)
            CloudResult.Success(backup)
        } catch (e: Exception) {
            CloudResult.Error(e, "Download failed: ${e.message}")
        }
    }
    
    override suspend fun listBackups(): CloudResult<List<CloudBackupItem>> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext CloudResult.Error(
                Exception("Not authenticated"), 
                "Please sign in to Google Drive first"
            )
            
            val folderId = getOrCreateAppFolder(drive)
            
            // List all backup files in app folder
            val fileList = drive.files().list()
                .setQ("'$folderId' in parents and name contains '$BACKUP_FILE_PREFIX'")
                .setFields("files(id,name,size,createdTime,properties)")
                .setOrderBy("createdTime desc")
                .execute()
            
            val backupItems = fileList.files.mapNotNull { file ->
                try {
                    val properties = file.properties ?: return@mapNotNull null
                    val metadataJson = properties[METADATA_KEY] ?: return@mapNotNull null
                    val metadata = gson.fromJson(metadataJson, CloudBackupMetadata::class.java)
                    
                    val backupId = properties["backup_id"] ?: return@mapNotNull null
                    val deviceName = properties["device_name"] ?: "Unknown Device"
                    val createdAt = properties["created_at"]?.toLongOrNull() ?: 0L
                    
                    // Check compatibility based on schema version
                    val schemaVersion = properties["schema_version"]?.toIntOrNull() ?: 0
                    val currentSchemaVersion = 19 // Current app schema version
                    val isCompatible = schemaVersion <= currentSchemaVersion
                    val needsUpgrade = schemaVersion < currentSchemaVersion
                    
                    CloudBackupItem(
                        id = backupId,
                        deviceName = deviceName,
                        createdAt = createdAt,
                        sizeBytes = (file.size ?: 0L).toLong(),
                        metadata = metadata,
                        isCompatible = isCompatible,
                        needsUpgrade = needsUpgrade
                    )
                } catch (e: Exception) {
                    null // Skip malformed backup files
                }
            }
            
            CloudResult.Success(backupItems)
        } catch (e: Exception) {
            CloudResult.Error(e, "Failed to list backups: ${e.message}")
        }
    }
    
    override suspend fun deleteBackup(backupId: String): CloudResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext CloudResult.Error(
                Exception("Not authenticated"), 
                "Please sign in to Google Drive first"
            )
            
            // Find backup file by backup_id property
            val fileList = drive.files().list()
                .setQ("properties has { key='backup_id' and value='$backupId' }")
                .setFields("files(id)")
                .execute()
            
            val file = fileList.files.firstOrNull() ?: return@withContext CloudResult.Error(
                Exception("Backup not found"), 
                "Backup with ID $backupId not found"
            )
            
            drive.files().delete(file.id).execute()
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            CloudResult.Error(e, "Delete failed: ${e.message}")
        }
    }
    
    override suspend fun getStorageInfo(): CloudResult<CloudStorageInfo> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext CloudResult.Error(
                Exception("Not authenticated"), 
                "Please sign in to Google Drive first"
            )
            
            val about = drive.about().get()
                .setFields("storageQuota")
                .execute()
            
            val quota = about.storageQuota
            val totalBytes = quota.limit ?: Long.MAX_VALUE
            val usedBytes = quota.usage ?: 0L
            val availableBytes = totalBytes - usedBytes
            
            // Calculate backup space usage
            val folderId = getOrCreateAppFolder(drive)
            val backupFiles = drive.files().list()
                .setQ("'$folderId' in parents")
                .setFields("files(size)")
                .execute()
            
            val backupSpaceUsed = backupFiles.files.sumOf { (it.size ?: 0L).toLong() }
            
            val storageInfo = CloudStorageInfo(
                totalSpaceBytes = totalBytes,
                usedSpaceBytes = usedBytes,
                availableSpaceBytes = availableBytes,
                backupSpaceUsedBytes = backupSpaceUsed
            )
            
            CloudResult.Success(storageInfo)
        } catch (e: Exception) {
            CloudResult.Error(e, "Failed to get storage info: ${e.message}")
        }
    }
    
    /**
     * Initialize Drive service with authenticated account
     */
    private fun initializeDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account.account
        
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("MyWorkoutLog")
            .build()
    }
    
    /**
     * Get or create the app-specific folder in Google Drive
     */
    private fun getOrCreateAppFolder(drive: Drive): String {
        // Search for existing app folder
        val query = "name='$APP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder'"
        val result = drive.files().list()
            .setQ(query)
            .setFields("files(id)")
            .execute()
        
        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            // Create new app folder
            val folderMetadata = File().apply {
                name = APP_FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
            }
            
            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            folder.id
        }
    }
    
    /**
     * Get GoogleSignInClient for external authentication flow
     */
    fun getSignInClient(): GoogleSignInClient = googleSignInClient
}