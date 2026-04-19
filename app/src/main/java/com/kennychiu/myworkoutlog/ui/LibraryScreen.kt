package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.ui.theme.Dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun LibraryScreen(onNavigate: (String) -> Unit) {
    ScreenScaffold(title = "Library") { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
        ) {
            LibraryHubCard(
                icon = Icons.Filled.FitnessCenter,
                label = "Manage Exercises",
                onClick = { onNavigate(Screen.ManageExercises.route) }
            )
            LibraryHubCard(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Manage Templates",
                onClick = { onNavigate(Screen.ManageTemplates.route) }
            )
            LibraryHubCard(
                icon = Icons.Filled.Schedule,
                label = "Program Blueprints",
                onClick = { onNavigate(Screen.Programs.route) }
            )
            LibraryHubCard(
                icon = Icons.Filled.EmojiEvents,
                label = "Personal Records",
                onClick = { onNavigate(Screen.PersonalRecords.route) }
            )
            LibraryHubCard(
                icon = Icons.Filled.Insights,
                label = "Volume Analysis",
                onClick = { onNavigate(Screen.VolumeAnalysis.route) }
            )
            LibraryHubCard(
                icon = Icons.Filled.Analytics,
                label = "Advanced Analytics",
                onClick = { onNavigate(Screen.Analytics.defaultRoute) }
            )
            LibraryHubCard(
                icon = Icons.Filled.Settings,
                label = "Settings",
                onClick = { onNavigate(Screen.Settings.route) }
            )
        }
    }
}

@Composable
private fun LibraryHubCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(Dimens.elevationCard)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.spacing16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing16)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
