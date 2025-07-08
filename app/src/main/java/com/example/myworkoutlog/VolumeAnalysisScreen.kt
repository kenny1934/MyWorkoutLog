@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Log
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import java.util.*

@Composable
fun VolumeAnalysisScreen(
    viewModel: VolumeViewModel,
    onNavigateToMuscleGroupAnalytics: (String) -> Unit = {}
) {
    val volumeData by viewModel.volumeData.collectAsStateWithLifecycle()
    val weeksInCycle by viewModel.weeksInActiveCycle.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()

    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Automatically select the first week when the screen loads
    LaunchedEffect(weeksInCycle) {
        if (selectedWeek == null) {
            viewModel.onWeekSelected(weeksInCycle.firstOrNull())
        }
    }

    val sortedVolumeList = volumeData.entries.toList().sortedByDescending { it.value }

    val chartModelProducer = remember { ChartEntryModelProducer() }
    val chartScrollState = rememberChartScrollState()

    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        sortedVolumeList.getOrNull(value.toInt())?.key?.name
            ?.replace("_", " ")?.lowercase()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            ?: ""
    }

    val startAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        // Only display a label if the value is a whole number
        if (value % 1 == 0f) {
            value.toInt().toString()
        } else {
            "" // Return an empty string for decimal values
        }
    }

    LaunchedEffect(sortedVolumeList) {
        val entries = sortedVolumeList.mapIndexed { index, entry ->
            entryOf(index.toFloat(), entry.value)
        }
        chartModelProducer.setEntries(entries)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Volume Analysis", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Dropdown menu for selecting a training week
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedWeek?.weekLabel ?: "Select a Week",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                weeksInCycle.forEach { week ->
                    DropdownMenuItem(
                        text = { Text(week.weekLabel) },
                        onClick = {
                            viewModel.onWeekSelected(week)
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (volumeData.isEmpty()) {
            Text("No workout data for the selected week to analyze.")
        } else {
            Card(elevation = CardDefaults.cardElevation(2.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Chart(
                        chart = columnChart(
                            columns = listOf(
                                lineComponent(
                                    color = MaterialTheme.colorScheme.primary,
                                    thickness = 12.dp,
                                    dynamicShader = verticalGradient(
                                        arrayOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                                    )
                                )
                            ),
                            spacing = 16.dp
                        ),
                        chartModelProducer = chartModelProducer,
                        startAxis = rememberStartAxis(
                            title = "Total Sets",
                            valueFormatter = startAxisValueFormatter
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = bottomAxisValueFormatter,
                            guideline = null
                        ),
                        modifier = Modifier.height(300.dp),
                        chartScrollState = chartScrollState,
                        isZoomEnabled = false
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display muscle group breakdown with clickable navigation to Analytics
                        items(sortedVolumeList) { (muscleGroup, setCount) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        Log.d("VolumeAnalysis", "=== MUSCLE GROUP CLICK DEBUG ===")
                                        Log.d("VolumeAnalysis", "Muscle group clicked: ${muscleGroup.name}")
                                        Log.d("VolumeAnalysis", "Muscle group type: ${muscleGroup::class.simpleName}")
                                        Log.d("VolumeAnalysis", "onNavigateToMuscleGroupAnalytics function: $onNavigateToMuscleGroupAnalytics")
                                        try {
                                            Log.d("VolumeAnalysis", "Calling navigation with: ${muscleGroup.name}")
                                            onNavigateToMuscleGroupAnalytics(muscleGroup.name)
                                            Log.d("VolumeAnalysis", "Navigation call completed successfully")
                                        } catch (e: Exception) {
                                            Log.e("VolumeAnalysis", "Navigation failed: ${e.message}", e)
                                        }
                                        Log.d("VolumeAnalysis", "=== END MUSCLE GROUP CLICK DEBUG ===")
                                    }
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        muscleGroup.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.titlecase() },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("$setCount sets", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Icon(
                                            imageVector = Icons.Default.Analytics,
                                            contentDescription = "View ${muscleGroup.name} analytics",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}