package com.kennychiu.myworkoutlog.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

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

/**
 * Launcher that records a video via the system camera into app-specific external
 * storage and returns a FileProvider content URI on success.
 */
@Composable
fun rememberVideoCaptureLauncher(onCaptured: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val pendingUri = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val uri = pendingUri.value
        pendingUri.value = null
        if (success && uri != null) onCaptured(uri.toString())
    }
    return launch@{
        val uri = createCaptureOutputUri(context) ?: return@launch
        pendingUri.value = uri
        try {
            launcher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingUri.value = null
        }
    }
}

private fun createCaptureOutputUri(context: Context): Uri? {
    val dir = File(context.getExternalFilesDir(null), "videos").apply { mkdirs() }
    val file = File(dir, "form_${System.currentTimeMillis()}_${UUID.randomUUID()}.mp4")
    return try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: IllegalArgumentException) {
        null
    }
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
