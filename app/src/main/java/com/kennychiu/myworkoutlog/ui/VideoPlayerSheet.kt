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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * URI string back to the caller along with optional `reps` (from the `+1` tap-along
 * counter) and `secs` (from mark-start/end for holds) when those helpers were used.
 *
 * Pre-fills with the most-recent video in `DCIM/Camera` (within ~90s) when available;
 * otherwise the body is the gallery-picker fallback.
 *
 * Helper visibility:
 *  - `+1 rep` counter is shown only when [showWeightReps] is true.
 *  - `[Mark start] / [Mark end]` is shown only when [showSecs] is true.
 *  - Frame stepper `± 0.1s` is always shown when the sheet has attach wiring.
 *  - Playback speed (0.5x / 1x / 2x) is shown whenever a clip is loaded — both attach and review modes.
 *  - In review mode ([onAttach] is null) the counter, hold-mark, and frame-stepper helpers are hidden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerSheet(
    setNumber: Int?,
    initialUri: String?,
    onDismiss: () -> Unit,
    onAttach: ((String, Int?, Int?) -> Unit)?,
    showWeightReps: Boolean = false,
    showSecs: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var permissionGranted by remember { mutableStateOf(hasRecentClipsPermission(context)) }
    var clips by remember { mutableStateOf<List<RecentClip>>(emptyList()) }
    var selectedUri by remember { mutableStateOf(initialUri) }

    var repCount by remember { mutableStateOf<Int?>(null) }
    var holdStartMs by remember { mutableStateOf<Long?>(null) }
    var holdEndMs by remember { mutableStateOf<Long?>(null) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    LaunchedEffect(selectedUri) {
        val uri = selectedUri
        if (uri.isNullOrBlank()) {
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            player.prepare()
        }
    }
    LaunchedEffect(playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed)
    }

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

    val holdDurationSecs: Float? = run {
        val start = holdStartMs
        val end = holdEndMs
        if (start != null && end != null && end > start) (end - start) / 1000f else null
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
                player = player,
                hasMedia = !selectedUri.isNullOrBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            )

            if (onAttach != null && !selectedUri.isNullOrBlank()) {
                FrameStepperRow(
                    onStepBack = { player.seekTo((player.currentPosition - 100L).coerceAtLeast(0L)) },
                    onStepForward = {
                        val max = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        player.seekTo((player.currentPosition + 100L).coerceAtMost(max))
                    }
                )
            }

            if (!selectedUri.isNullOrBlank()) {
                PlaybackSpeedRow(
                    speed = playbackSpeed,
                    onSpeedChange = { playbackSpeed = it }
                )
            }

            if (onAttach != null && showWeightReps) {
                RepCounterRow(
                    repCount = repCount,
                    onIncrement = { repCount = (repCount ?: 0) + 1 },
                    onReset = { repCount = null }
                )
            }

            if (onAttach != null && showSecs) {
                HoldMarkRow(
                    durationSecs = holdDurationSecs,
                    hasStart = holdStartMs != null,
                    hasEnd = holdEndMs != null,
                    onMarkStart = { holdStartMs = player.currentPosition; holdEndMs = null },
                    onMarkEnd = { if (holdStartMs != null) holdEndMs = player.currentPosition },
                    onReset = { holdStartMs = null; holdEndMs = null }
                )
            }

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
                            val attachedSecs = holdDurationSecs?.let { kotlin.math.max(1, kotlin.math.round(it).toInt()) }
                            onAttach(uri, repCount, attachedSecs)
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

@Composable
private fun RepCounterRow(
    repCount: Int?,
    onIncrement: () -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalButton(
            onClick = onIncrement,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("+1 rep")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (repCount != null) "Counted: $repCount reps" else "Tap along with the video to count reps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (repCount != null) {
                TextButton(onClick = onReset) { Text("Reset") }
            }
        }
    }
}

@Composable
private fun HoldMarkRow(
    durationSecs: Float?,
    hasStart: Boolean,
    hasEnd: Boolean,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onMarkStart,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (hasStart) "Start ✓" else "Mark start")
            }
            FilledTonalButton(
                onClick = onMarkEnd,
                enabled = hasStart,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (hasEnd) "End ✓" else "Mark end")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    durationSecs != null -> "Duration: %.1fs".format(durationSecs)
                    hasStart -> "Scrub to the end of the hold, then tap Mark end"
                    else -> "Scrub to the start of the hold, then tap Mark start"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasStart || hasEnd) {
                TextButton(onClick = onReset) { Text("Reset") }
            }
        }
    }
}

@Composable
private fun FrameStepperRow(
    onStepBack: () -> Unit,
    onStepForward: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onStepBack,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("0.1s")
        }
        OutlinedButton(
            onClick = onStepForward,
            modifier = Modifier.weight(1f)
        ) {
            Text("0.1s")
            Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PlaybackSpeedRow(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val options = listOf(0.5f to "0.5x", 1f to "1x", 2f to "2x")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = speed == value,
                onClick = { onSpeedChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label)
            }
        }
    }
}

/**
 * Media3 [PlayerView] host. The [ExoPlayer] lifecycle is owned by the caller (so the
 * helper buttons above can read `currentPosition` and drive `seekTo`). When
 * [hasMedia] is false, a placeholder icon fills the surface instead.
 */
@Composable
private fun VideoPlayerSurface(
    player: ExoPlayer,
    hasMedia: Boolean,
    modifier: Modifier = Modifier
) {
    if (!hasMedia) {
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
