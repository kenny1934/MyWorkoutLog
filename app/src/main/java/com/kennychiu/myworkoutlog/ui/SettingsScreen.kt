package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import com.kennychiu.myworkoutlog.ui.theme.Dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateUp: (() -> Unit)? = null,
    onNavigateToExport: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    onNavigateToCloudBackup: () -> Unit = {}
) {
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val options = listOf("kg", "lb")

    ScreenScaffold(title = "Settings", onNavigateUp = onNavigateUp) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(Dimens.screenPadding)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
            ) {
                Column(modifier = Modifier.padding(Dimens.spacing16)) {
                    Text(
                        "Preferences",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Weight Unit", style = MaterialTheme.typography.bodyLarge)
                        SingleChoiceSegmentedButtonRow {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = { viewModel.setWeightUnit(label) },
                                    selected = weightUnit == label
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing16))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
            ) {
                Column(modifier = Modifier.padding(Dimens.spacing16)) {
                    Text(
                        "Data Management",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                    SettingsNavRow(
                        icon = Icons.Filled.Download,
                        iconDescription = "Export Data",
                        title = "Export Data",
                        subtitle = "Export workouts, exercises, and personal records",
                        onClick = onNavigateToExport
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Dimens.spacing8),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    SettingsNavRow(
                        icon = Icons.Filled.Upload,
                        iconDescription = "Import Data",
                        title = "Import Data",
                        subtitle = "Import workouts, exercises, and personal records",
                        onClick = onNavigateToImport
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Dimens.spacing8),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    SettingsNavRow(
                        icon = Icons.Filled.Cloud,
                        iconDescription = "Cloud Backup & Restore",
                        title = "Cloud Backup & Restore",
                        subtitle = "Back up to Google Drive and restore on any device",
                        onClick = onNavigateToCloudBackup
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing16))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
            ) {
                Column(modifier = Modifier.padding(Dimens.spacing16)) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "App Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing16))
                        Column {
                            Text(
                                "MyWorkoutLog",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Personal workout tracker",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(Dimens.spacing16))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
