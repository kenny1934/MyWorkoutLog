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
    val layoutInfo = rememberAdaptiveLayoutInfo()
    
    if (layoutInfo.useMasterDetail) {
        // Large screen: Master-detail layout
        VolumeAnalysisMasterDetailView(
            viewModel = viewModel,
            layoutInfo = layoutInfo,
            onNavigateToMuscleGroupAnalytics = onNavigateToMuscleGroupAnalytics
        )
    } else {
        // Small screen: Original single-column layout
        VolumeAnalysisSingleColumnView(
            viewModel = viewModel,
            onNavigateToMuscleGroupAnalytics = onNavigateToMuscleGroupAnalytics
        )
    }
}

@Composable
private fun VolumeAnalysisSingleColumnView(
    viewModel: VolumeViewModel,
    onNavigateToMuscleGroupAnalytics: (String) -> Unit
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
                                        Log.d("VolumeAnalysis", "Muscle group clicked: ${muscleGroup.name}")
                                        onNavigateToMuscleGroupAnalytics(muscleGroup.name)
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

@Composable
private fun VolumeAnalysisMasterDetailView(
    viewModel: VolumeViewModel,
    layoutInfo: AdaptiveLayoutInfo,
    onNavigateToMuscleGroupAnalytics: (String) -> Unit
) {
    val volumeData by viewModel.volumeData.collectAsStateWithLifecycle()
    val weeksInCycle by viewModel.weeksInActiveCycle.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()
    
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    
    // Automatically select the first week when the screen loads
    LaunchedEffect(weeksInCycle) {
        if (selectedWeek == null) {
            viewModel.onWeekSelected(weeksInCycle.firstOrNull())
        }
    }
    
    // Auto-select first muscle group when data loads or when selected group is no longer available
    val sortedVolumeList = volumeData.entries.toList().sortedByDescending { it.value }
    LaunchedEffect(sortedVolumeList) {
        when {
            selectedMuscleGroup == null && sortedVolumeList.isNotEmpty() -> {
                selectedMuscleGroup = sortedVolumeList.first().key
            }
            selectedMuscleGroup != null && sortedVolumeList.none { it.key == selectedMuscleGroup } -> {
                // Selected muscle group no longer has data, select first available or clear selection
                selectedMuscleGroup = sortedVolumeList.firstOrNull()?.key
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(layoutInfo.contentPadding)
    ) {
        // Master Panel (Left side - 40%)
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.4f)
                .heightIn(min = 400.dp), // Ensure consistent minimum height
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Volume Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Week selection dropdown
                VolumeWeekSelector(
                    weeksInCycle = weeksInCycle,
                    selectedWeek = selectedWeek,
                    onWeekSelected = viewModel::onWeekSelected
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Muscle groups list
                if (sortedVolumeList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No workout data for the selected week.")
                    }
                } else {
                    Text(
                        text = "Muscle Groups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedVolumeList) { (muscleGroup, setCount) ->
                            MuscleGroupListItem(
                                muscleGroup = muscleGroup,
                                setCount = setCount,
                                isSelected = selectedMuscleGroup == muscleGroup,
                                onMuscleGroupSelected = { selectedMuscleGroup = muscleGroup },
                                onAnalyticsClick = { onNavigateToMuscleGroupAnalytics(muscleGroup.name) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Detail Panel (Right side - 60%)
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.6f)
                .heightIn(min = 400.dp), // Ensure consistent minimum height
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            VolumeAnalysisDetailPanel(
                selectedMuscleGroup = selectedMuscleGroup,
                volumeData = volumeData,
                sortedVolumeList = sortedVolumeList.map { entry -> entry.key to entry.value },
                selectedWeek = selectedWeek,
                onNavigateToMuscleGroupAnalytics = onNavigateToMuscleGroupAnalytics
            )
        }
    }
}

@Composable
private fun VolumeWeekSelector(
    weeksInCycle: List<ProgramWeekDefinition>,
    selectedWeek: ProgramWeekDefinition?,
    onWeekSelected: (ProgramWeekDefinition?) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = isDropdownExpanded,
        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
    ) {
        OutlinedTextField(
            value = selectedWeek?.weekLabel ?: "Select a Week",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text("Training Week") }
        )
        ExposedDropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            weeksInCycle.forEach { week ->
                DropdownMenuItem(
                    text = { Text(week.weekLabel) },
                    onClick = {
                        onWeekSelected(week)
                        isDropdownExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MuscleGroupListItem(
    muscleGroup: MuscleGroup,
    setCount: Int,
    isSelected: Boolean,
    onMuscleGroupSelected: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMuscleGroupSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = muscleGroup.name.replace("_", " ").lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "$setCount sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onAnalyticsClick) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "View ${muscleGroup.name} analytics",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun VolumeAnalysisDetailPanel(
    selectedMuscleGroup: MuscleGroup?,
    volumeData: Map<MuscleGroup, Int>,
    sortedVolumeList: List<Pair<MuscleGroup, Int>>,
    selectedWeek: ProgramWeekDefinition?,
    onNavigateToMuscleGroupAnalytics: (String) -> Unit
) {
    if (selectedMuscleGroup == null) {
        // No muscle group selected - show placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Select Muscle Group",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a muscle group to view detailed volume analysis",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Muscle group selected - show detailed analysis
        val selectedSetCount = volumeData[selectedMuscleGroup] ?: 0
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with muscle group name and analytics button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedMuscleGroup.name.replace("_", " ").lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Week: ${selectedWeek?.weekLabel ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = { onNavigateToMuscleGroupAnalytics(selectedMuscleGroup.name) }) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "View detailed analytics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                // Volume statistics card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Volume Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            VolumeStatItem(
                                label = "Total Sets",
                                value = selectedSetCount.toString(),
                                icon = Icons.Default.Analytics
                            )
                            
                            val totalSets = volumeData.values.sum()
                            val percentage = if (totalSets > 0 && selectedSetCount > 0) {
                                (selectedSetCount * 100.0 / totalSets).coerceIn(0.0, 100.0)
                            } else {
                                0.0
                            }
                            VolumeStatItem(
                                label = "% of Total",
                                value = "${percentage.toInt()}%",
                                icon = Icons.Default.Analytics
                            )
                            
                            val rank = sortedVolumeList.indexOfFirst { it.first == selectedMuscleGroup } + 1
                            VolumeStatItem(
                                label = "Rank",
                                value = "#$rank",
                                icon = Icons.Default.Analytics
                            )
                        }
                    }
                }
            }

            item {
                // Enhanced chart display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Volume Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        VolumeComparisonChart(
                            sortedVolumeList = sortedVolumeList,
                            selectedMuscleGroup = selectedMuscleGroup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun VolumeComparisonChart(
    sortedVolumeList: List<Pair<MuscleGroup, Int>>,
    selectedMuscleGroup: MuscleGroup
) {
    val chartModelProducer = remember { ChartEntryModelProducer() }
    val chartScrollState = rememberChartScrollState()

    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        sortedVolumeList.getOrNull(value.toInt())?.first?.name
            ?.replace("_", " ")?.lowercase()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            ?: ""
    }

    val startAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        if (value % 1 == 0f) {
            value.toInt().toString()
        } else {
            ""
        }
    }

    LaunchedEffect(sortedVolumeList) {
        val entries = sortedVolumeList.mapIndexed { index, entry ->
            entryOf(index.toFloat(), entry.second.toFloat())
        }
        chartModelProducer.setEntries(entries)
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                lineComponent(
                    color = MaterialTheme.colorScheme.primary,
                    thickness = 16.dp,
                    dynamicShader = verticalGradient(
                        arrayOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    )
                )
            ),
            spacing = 20.dp
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
        modifier = Modifier.height(400.dp),
        chartScrollState = chartScrollState,
        isZoomEnabled = false
    )
}