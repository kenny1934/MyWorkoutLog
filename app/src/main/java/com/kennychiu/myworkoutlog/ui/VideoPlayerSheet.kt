package com.kennychiu.myworkoutlog.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kennychiu.myworkoutlog.util.RecentClip
import com.kennychiu.myworkoutlog.util.hasRecentClipsPermission
import com.kennychiu.myworkoutlog.util.queryRecentCameraClips
import com.kennychiu.myworkoutlog.util.recentClipsPermission
import com.kennychiu.myworkoutlog.util.rememberVideoPickLauncher
import com.kennychiu.myworkoutlog.util.rememberVideoThumbnail

/**
 * Bottom sheet wrapping a Media3 [PlayerView] for inline video review. When [setNumber]
 * is non-null and [onAttach] is wired, an "Attach to Set N" action commits the chosen
 * URI string back to the caller.
 *
 * Pre-fills with the most-recent video in `DCIM/Camera` (within ~90s) when available;
 * otherwise the body is the gallery-picker fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerSheet(
    setNumber: Int?,
    initialUri: String?,
    onDismiss: () -> Unit,
    onAttach: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var permissionGranted by remember { mutableStateOf(hasRecentClipsPermission(context)) }
    var clips by remember { mutableStateOf<List<RecentClip>>(emptyList()) }
    var selectedUri by remember { mutableStateOf(initialUri) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            if (context.checkSelfPermission(recentClipsPermission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(recentClipsPermission)
            }
            return@LaunchedEffect
        }
        clips = queryRecentCameraClips(context)
        if (selectedUri == null) {
            selectedUri = clips.firstOrNull()?.uri?.toString()
        }
    }

    val galleryPicker = rememberVideoPickLauncher { picked ->
        selectedUri = picked
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (setNumber != null) "Attach video to Set $setNumber" else "Review video",
                style = MaterialTheme.typography.titleMedium
            )

            VideoPlayerSurface(
                uri = selectedUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            )

            if (clips.size > 1) {
                Text(
                    text = "Recent clips",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clips) { clip ->
                        val uriString = clip.uri.toString()
                        ClipThumbnail(
                            uriString = uriString,
                            isSelected = uriString == selectedUri,
                            onClick = { selectedUri = uriString }
                        )
                    }
                }
            }

            if (permissionGranted && clips.isEmpty() && selectedUri == null) {
                Text(
                    text = "No recent clips in DCIM/Camera. Pick from gallery instead:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!permissionGranted) {
                Text(
                    text = "Grant media permission to see your most recent camera clips, or pick from the gallery directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { galleryPicker() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Gallery")
                }
                if (onAttach != null) {
                    Button(
                        onClick = {
                            val uri = selectedUri ?: return@Button
                            onAttach(uri)
                        },
                        enabled = selectedUri != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (setNumber != null) "Attach to Set $setNumber" else "Attach")
                    }
                }
            }
        }
    }
}

/**
 * Persistent ExoPlayer wrapper. Releases the player on disposal; swaps the media item
 * in place when [uri] changes so the surface doesn't re-attach.
 */
@Composable
private fun VideoPlayerSurface(uri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(uri) {
        if (uri.isNullOrBlank()) {
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            player.prepare()
        }
    }

    if (uri.isNullOrBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            update = { view -> view.player = player }
        )
    }
}

@Composable
private fun ClipThumbnail(
    uriString: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val thumbnail = rememberVideoThumbnail(uriString)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = "Recent clip",
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
