package com.kennychiu.myworkoutlog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TimerBar(
    isRunning: Boolean,
    currentTime: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onAddTime: () -> Unit
) {
    AnimatedVisibility(visible = isRunning || currentTime > 0) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentTime / 60}:${String.format("%02d", currentTime % 60)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Dynamic Play/Pause Button
                IconButton(onClick = { if (isRunning) onPause() else onResume() }) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.PauseCircle else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "Pause Timer" else "Resume Timer",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Button(onClick = onAddTime) { Text("+15s") }

                IconButton(onClick = onReset) {
                    Icon(Icons.Filled.Replay, contentDescription = "Reset Timer")
                }

                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop Timer")
                }
            }
        }
    }
}

// A helper function to format seconds into MM:SS format
fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

// Format seconds to display format (supports MM:SS and H:MM:SS)
fun formatSecondsToDisplay(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    } else {
        "%02d:%02d".format(minutes, remainingSeconds)
    }
}

// Parse various duration formats to seconds
fun parseDurationToSeconds(input: String): Int? {
    if (input.isBlank()) return null

    val trimmed = input.trim().lowercase()

    try {
        // Handle seconds format: "2730s"
        if (trimmed.endsWith("s") && !trimmed.contains(":") && !trimmed.contains("m") && !trimmed.contains("h")) {
            val secondsStr = trimmed.dropLast(1)
            return secondsStr.toIntOrNull()
        }

        // Handle time format: "45:30" or "1:15:30"
        if (trimmed.contains(":")) {
            val parts = trimmed.split(":")
            return when (parts.size) {
                2 -> { // MM:SS
                    val minutes = parts[0].toIntOrNull() ?: return null
                    val seconds = parts[1].toIntOrNull() ?: return null
                    if (seconds >= 60) return null
                    minutes * 60 + seconds
                }
                3 -> { // H:MM:SS
                    val hours = parts[0].toIntOrNull() ?: return null
                    val minutes = parts[1].toIntOrNull() ?: return null
                    val seconds = parts[2].toIntOrNull() ?: return null
                    if (minutes >= 60 || seconds >= 60) return null
                    hours * 3600 + minutes * 60 + seconds
                }
                else -> null
            }
        }

        // Handle mixed format: "1h 15m 30s", "45m 30s", "1h 15m"
        var totalSeconds = 0
        var remaining = trimmed

        // Extract hours
        if (remaining.contains("h")) {
            val hoursMatch = Regex("(\\d+)h").find(remaining)
            if (hoursMatch != null) {
                val hours = hoursMatch.groupValues[1].toIntOrNull() ?: return null
                totalSeconds += hours * 3600
                remaining = remaining.replace(hoursMatch.value, "").trim()
            }
        }

        // Extract minutes
        if (remaining.contains("m")) {
            val minutesMatch = Regex("(\\d+)m").find(remaining)
            if (minutesMatch != null) {
                val minutes = minutesMatch.groupValues[1].toIntOrNull() ?: return null
                totalSeconds += minutes * 60
                remaining = remaining.replace(minutesMatch.value, "").trim()
            }
        }

        // Extract seconds
        if (remaining.contains("s")) {
            val secondsMatch = Regex("(\\d+)s").find(remaining)
            if (secondsMatch != null) {
                val seconds = secondsMatch.groupValues[1].toIntOrNull() ?: return null
                totalSeconds += seconds
                remaining = remaining.replace(secondsMatch.value, "").trim()
            }
        }

        // If there's still content, parsing failed
        if (remaining.isNotEmpty() && totalSeconds == 0) return null

        return if (totalSeconds > 0) totalSeconds else null

    } catch (e: Exception) {
        return null
    }
}

// Validate duration input format
fun validateDurationInput(input: String): Boolean {
    return parseDurationToSeconds(input) != null
}
