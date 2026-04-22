package com.kennychiu.myworkoutlog.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Shared plumbing for the three video-reference entry points (exercise-level demo,
// set-level form capture, record-now flow). Treats a videoLink/videoReference string
// as "either a remote URL or a content URI"; callers don't have to branch.

/** True when [value] is a remote http(s) link rather than a content URI. */
fun isRemoteVideoLink(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
}

/**
 * Open [uriOrUrl] in a system viewer — browser/YouTube for http(s), gallery/player
 * for content://. Returns false when no handler is installed or permission is gone
 * (e.g. the source file was deleted from Photos).
 */
fun launchVideoViewer(context: Context, uriOrUrl: String): Boolean {
    val trimmed = uriOrUrl.trim()
    if (trimmed.isEmpty()) return false
    return try {
        val intent = if (isRemoteVideoLink(trimmed)) {
            Intent(Intent.ACTION_VIEW, Uri.parse(trimmed))
        } else {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(trimmed), "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/**
 * Launcher that picks a single video from the gallery. Persists read permission so
 * the URI remains usable across process restarts; falls back silently when the
 * provider refuses (some cloud providers do).
 */
@Composable
fun rememberVideoPickLauncher(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Usable for the current session even without persistable permission.
        }
        onPicked(uri.toString())
    }
    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }
}

/** Permission required to query [MediaStore.Video] on the running platform. */
val recentClipsPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/** True when the app currently holds [recentClipsPermission]. */
fun hasRecentClipsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, recentClipsPermission) == PackageManager.PERMISSION_GRANTED

/**
 * One match from the recent-camera-clips MediaStore query: the playable content URI
 * plus when it was added (epoch millis), used to label the thumbnail strip.
 */
data class RecentClip(val uri: Uri, val dateAddedEpochMillis: Long)

/**
 * Snapshot the [limit] newest videos in `DCIM/Camera` added within [withinSeconds] of now,
 * matching Kenny's "film a set, walk to the phone within ~1 minute" workflow.
 *
 * Empty list when permission is missing — caller should request it first via [recentClipsPermission].
 * Pre-API-29 falls back to `_DATA LIKE 'DCIM/Camera/%'` since `RELATIVE_PATH` only exists from Q.
 */
suspend fun queryRecentCameraClips(
    context: Context,
    withinSeconds: Long = 90L,
    limit: Int = 3
): List<RecentClip> = withContext(Dispatchers.IO) {
    if (!hasRecentClipsPermission(context)) return@withContext emptyList()
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_ADDED)
    val cutoff = System.currentTimeMillis() / 1_000L - withinSeconds
    val (selection, args) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Video.Media.DATE_ADDED} > ?" to
            arrayOf("DCIM/Camera/%", cutoff.toString())
    } else {
        @Suppress("DEPRECATION")
        "${MediaStore.Video.Media.DATA} LIKE ? AND ${MediaStore.Video.Media.DATE_ADDED} > ?" to
            arrayOf("%/DCIM/Camera/%", cutoff.toString())
    }
    val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
    val results = mutableListOf<RecentClip>()
    runCatching {
        context.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext() && results.size < limit) {
                val id = cursor.getLong(idCol)
                val addedSec = cursor.getLong(addedCol)
                results += RecentClip(
                    uri = ContentUris.withAppendedId(collection, id),
                    dateAddedEpochMillis = addedSec * 1_000L
                )
            }
        }
    }
    results
}

/**
 * Extract a thumbnail frame for a local content URI. Returns null for remote links
 * or when extraction fails (source deleted, unsupported codec, permission revoked).
 * Keyed on [uriOrUrl] so the frame re-extracts when the reference changes.
 */
@Composable
fun rememberVideoThumbnail(uriOrUrl: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uriOrUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriOrUrl) {
        val source = uriOrUrl?.trim()
            ?.takeIf { it.isNotEmpty() && !isRemoteVideoLink(it) }
        if (source == null) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(source))
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.asImageBitmap()
            } catch (_: Throwable) {
                null
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }
        }
    }
    return bitmap
}
