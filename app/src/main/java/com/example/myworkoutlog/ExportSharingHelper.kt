package com.example.myworkoutlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.io.IOException

// Sharing helper for export functionality
class ExportSharingHelper(private val context: Context) {

    // Save export content to file and return sharing intent
    fun createSharingIntent(exportResult: ExportResult): Intent? {
        if (!exportResult.success || exportResult.fileContent == null || exportResult.fileName == null) {
            return null
        }

        return try {
            // Create file in cache directory
            val file = createTempFile(exportResult.fileName, exportResult.fileContent)
            
            // Create content URI using FileProvider
            val contentUri = getFileUri(file)
            
            // Create sharing intent
            createShareIntent(contentUri, exportResult.fileName, file.length())
            
        } catch (e: Exception) {
            null
        }
    }

    // Save export to downloads folder
    fun saveToDownloads(exportResult: ExportResult): Boolean {
        if (!exportResult.success || exportResult.fileContent == null || exportResult.fileName == null) {
            return false
        }

        return try {
            // For Android 10+ we would use MediaStore, but for now save to external cache
            val downloadsDir = File(context.externalCacheDir, "MyWorkoutLog")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, exportResult.fileName)
            FileWriter(file).use { writer ->
                writer.write(exportResult.fileContent)
            }
            
            true
        } catch (e: IOException) {
            false
        }
    }

    // Create email intent with export as attachment
    fun createEmailIntent(exportResult: ExportResult, recipientEmail: String = ""): Intent? {
        if (!exportResult.success || exportResult.fileContent == null || exportResult.fileName == null) {
            return null
        }

        return try {
            val file = createTempFile(exportResult.fileName, exportResult.fileContent)
            val contentUri = getFileUri(file)
            
            Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(exportResult.fileName)
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, "MyWorkoutLog Export - ${exportResult.fileName}")
                putExtra(
                    Intent.EXTRA_TEXT, 
                    buildEmailBody(exportResult)
                )
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Get export summary for sharing
    fun getExportSummaryText(exportResult: ExportResult): String {
        return buildString {
            appendLine("MyWorkoutLog Export Summary")
            appendLine("==============================")
            exportResult.fileName?.let { appendLine("File: $it") }
            appendLine("Records: ${exportResult.recordCount}")
            if (exportResult.fileSize > 0) {
                appendLine("Size: ${formatFileSize(exportResult.fileSize)}")
            }
            appendLine("Generated: ${getCurrentTimestamp()}")
            appendLine("")
            appendLine("Export your fitness data and track your progress with MyWorkoutLog!")
        }
    }

    // Private helper functions

    private fun createTempFile(fileName: String, content: String): File {
        val tempDir = File(context.cacheDir, "exports")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        
        val file = File(tempDir, fileName)
        FileWriter(file).use { writer ->
            writer.write(content)
        }
        
        return file
    }

    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun createShareIntent(contentUri: Uri, fileName: String, fileSize: Long): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(fileName)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "MyWorkoutLog Export - $fileName")
            putExtra(
                Intent.EXTRA_TEXT,
                "Sharing my workout data from MyWorkoutLog.\n\n" +
                "File: $fileName\n" +
                "Size: ${formatFileSize(fileSize)}\n\n" +
                "Generated with MyWorkoutLog - Track your fitness journey!"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun buildEmailBody(exportResult: ExportResult): String {
        return buildString {
            appendLine("Hi!")
            appendLine("")
            appendLine("I'm sharing my workout data export from MyWorkoutLog.")
            appendLine("")
            appendLine("Export Details:")
            exportResult.fileName?.let { appendLine("• File: $it") }
            appendLine("• Records: ${exportResult.recordCount}")
            if (exportResult.fileSize > 0) {
                appendLine("• Size: ${formatFileSize(exportResult.fileSize)}")
            }
            appendLine("• Generated: ${getCurrentTimestamp()}")
            appendLine("")
            appendLine("The file is attached to this email.")
            appendLine("")
            appendLine("Best regards!")
            appendLine("")
            appendLine("---")
            appendLine("Sent from MyWorkoutLog")
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
            fileName.endsWith(".json", ignoreCase = true) -> "application/json"
            else -> "text/plain"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    private fun getCurrentTimestamp(): String {
        return java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", 
            java.util.Locale.getDefault()
        ).format(java.util.Date())
    }

    // Clean up temporary files (call this periodically)
    fun cleanupTempFiles() {
        try {
            val tempDir = File(context.cacheDir, "exports")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < System.currentTimeMillis() - 86400000) { // 24 hours
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}

// File provider configuration helper
object ExportFileProviderHelper {
    
    // Generates the required file_paths.xml content for FileProvider
    fun getFileProviderPaths(): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <paths xmlns:android="http://schemas.android.com/apk/res/android">
                <cache-path name="export_files" path="exports/" />
                <external-cache-path name="external_export_files" path="MyWorkoutLog/" />
            </paths>
        """.trimIndent()
    }
    
    // Manifest entry for FileProvider
    fun getManifestProviderEntry(): String {
        return """
            <provider
                android:name="androidx.core.content.FileProvider"
                android:authorities="${"\${applicationId}"}.fileprovider"
                android:exported="false"
                android:grantUriPermissions="true">
                <meta-data
                    android:name="android.support.FILE_PROVIDER_PATHS"
                    android:resource="@xml/file_paths" />
            </provider>
        """.trimIndent()
    }
}