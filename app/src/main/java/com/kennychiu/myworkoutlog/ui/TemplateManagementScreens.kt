@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*

@Composable
fun ManageTemplatesScreen(
    viewModel: WorkoutTemplateViewModel,
    onNavigateToTemplate: (String) -> Unit,
    onStartWorkout: (String) -> Unit
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    
    if (layoutInfo.useMasterDetail) {
        // Large screen: Master-detail layout
        TemplateManagementMasterDetailView(
            viewModel = viewModel,
            layoutInfo = layoutInfo,
            onNavigateToTemplate = onNavigateToTemplate,
            onStartWorkout = onStartWorkout
        )
    } else {
        // Small screen: Original single-column layout
        TemplateManagementSingleColumnView(
            viewModel = viewModel,
            onNavigateToTemplate = onNavigateToTemplate,
            onStartWorkout = onStartWorkout
        )
    }
}

@Composable
private fun TemplateManagementSingleColumnView(
    viewModel: WorkoutTemplateViewModel,
    onNavigateToTemplate: (String) -> Unit,
    onStartWorkout: (String) -> Unit
) {
    val templates by viewModel.allTemplates.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create new template")
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("Workout Templates", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (templates.isEmpty()) {
                Text(
                    "No templates yet. Click the '+' button to create one.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates) { template ->
                        var showOverflowMenu by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToTemplate(template.id) }
                                        .padding(vertical = 16.dp)
                                ) {
                                    Text(template.name)
                                }
                                // OVERFLOW MENU
                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "More options"
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Template") },
                                            onClick = {
                                                showOverflowMenu = false
                                                onNavigateToTemplate(template.id)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Edit,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    "Delete Template",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showOverflowMenu = false
                                                templateToDelete = template
                                                showDeleteConfirmation = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        )
                                    }
                                }
                                // START button
                                IconButton(
                                    onClick = { onStartWorkout(template.id) },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Start Workout"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                TemplateCreateDialog(
                    templateName = templateName,
                    onTemplateNameChange = { templateName = it },
                    onDismiss = { 
                        showDialog = false
                        templateName = ""
                    },
                    onConfirm = {
                        viewModel.insert(templateName, null)
                        templateName = ""
                        showDialog = false
                    }
                )
            }
            
            // Delete confirmation dialog
            if (showDeleteConfirmation && templateToDelete != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showDeleteConfirmation = false
                        templateToDelete = null
                    },
                    title = {
                        Text(
                            text = "Delete Template",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to delete \"${templateToDelete?.name}\"? This action cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                templateToDelete?.let { template ->
                                    viewModel.deleteById(template.id)
                                }
                                showDeleteConfirmation = false
                                templateToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showDeleteConfirmation = false
                                templateToDelete = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TemplateManagementMasterDetailView(
    viewModel: WorkoutTemplateViewModel,
    layoutInfo: AdaptiveLayoutInfo,
    onNavigateToTemplate: (String) -> Unit,
    onStartWorkout: (String) -> Unit
) {
    val templates by viewModel.allTemplates.collectAsStateWithLifecycle()
    val allExercises by viewModel.allMasterExercises.collectAsStateWithLifecycle()
    
    var selectedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }
    
    // Auto-select first template when data loads or when selected template is no longer available
    LaunchedEffect(templates) {
        when {
            selectedTemplate == null && templates.isNotEmpty() -> {
                selectedTemplate = templates.first()
            }
            selectedTemplate != null && templates.none { it.id == selectedTemplate?.id } -> {
                // Selected template was deleted, select first available or clear selection
                selectedTemplate = templates.firstOrNull()
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workout Templates",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Template",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Templates list
                if (templates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "No Templates",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No templates yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Create your first template",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { template ->
                            TemplateListItem(
                                template = template,
                                isSelected = selectedTemplate?.id == template.id,
                                onTemplateSelected = { selectedTemplate = template },
                                onStartWorkout = { onStartWorkout(template.id) },
                                onEditTemplate = { onNavigateToTemplate(template.id) },
                                onDeleteTemplate = { 
                                    templateToDelete = template
                                    showDeleteConfirmation = true 
                                }
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
            TemplateDetailPanel(
                selectedTemplate = selectedTemplate,
                allExercises = allExercises,
                onStartWorkout = onStartWorkout,
                onEditTemplate = onNavigateToTemplate,
                onDeleteTemplate = { templateId ->
                    val template = templates.find { it.id == templateId }
                    if (template != null) {
                        templateToDelete = template
                        showDeleteConfirmation = true
                    }
                }
            )
        }
    }
    
    // Create template dialog
    if (showCreateDialog) {
        TemplateCreateDialog(
            templateName = templateName,
            onTemplateNameChange = { templateName = it },
            onDismiss = { 
                showCreateDialog = false
                templateName = ""
            },
            onConfirm = {
                viewModel.insert(templateName, null)
                templateName = ""
                showCreateDialog = false
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation && templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                templateToDelete = null
            },
            title = {
                Text(
                    text = "Delete Template",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${templateToDelete?.name}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        templateToDelete?.let { template ->
                            viewModel.deleteById(template.id)
                            // Clear selection if deleted template was selected
                            if (selectedTemplate?.id == template.id) {
                                selectedTemplate = null
                            }
                        }
                        showDeleteConfirmation = false
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteConfirmation = false
                        templateToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TemplateListItem(
    template: WorkoutTemplate,
    isSelected: Boolean,
    onTemplateSelected: () -> Unit,
    onStartWorkout: () -> Unit,
    onEditTemplate: () -> Unit,
    onDeleteTemplate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTemplateSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "${template.templateExercises.size} exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditTemplate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    
                    Button(
                        onClick = onStartWorkout,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                }
                
                OutlinedButton(
                    onClick = onDeleteTemplate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun TemplateDetailPanel(
    selectedTemplate: WorkoutTemplate?,
    allExercises: List<Exercise>,
    onStartWorkout: (String) -> Unit,
    onEditTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit
) {
    if (selectedTemplate == null) {
        // No template selected - show placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Select Template",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a template to view details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Template selected - show detailed view
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with template name and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTemplate.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedTemplate.templateExercises.size} exercises • ${selectedTemplate.templateExercises.sumOf { it.sets.size }} sets",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditTemplate(selectedTemplate.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Template",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Template")
                    }
                    
                    Button(
                        onClick = { onStartWorkout(selectedTemplate.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Workout",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Workout")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onDeleteTemplate(selectedTemplate.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Template",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Template")
                    }
                }
            }
            
            // Exercise breakdown
            if (selectedTemplate.templateExercises.isNotEmpty()) {
                item {
                    Text(
                        text = "Exercise Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(selectedTemplate.templateExercises) { exercise ->
                    TemplateExerciseCard(
                        exercise = exercise,
                        allExercises = allExercises
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "No Exercises",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No exercises added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Edit template to add exercises",
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
private fun TemplateExerciseCard(
    exercise: TemplateExercise,
    allExercises: List<Exercise>
) {
    val masterExercise = allExercises.find { it.id == exercise.exerciseId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Exercise metadata
                    masterExercise?.let { master ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (master.usesBodyweight) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        text = "Bodyweight",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            val primaryEquipment = master.equipment.firstOrNull { it != Equipment.OTHER } ?: master.equipment.firstOrNull()
                            if (primaryEquipment != null) {
                                Text(
                                    text = if (primaryEquipment == Equipment.OTHER) {
                                        "Other equipment"
                                    } else {
                                        primaryEquipment.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = "${exercise.sets.size} sets",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Sets breakdown
            if (exercise.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    exercise.sets.forEachIndexed { index, set ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Set ${index + 1}: ${formatSetTarget(set)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
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
private fun TemplateCreateDialog(
    templateName: String,
    onTemplateNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Workout Template") },
        text = {
            OutlinedTextField(
                value = templateName,
                onValueChange = onTemplateNameChange,
                label = { Text("Template Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = templateName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatSetTarget(set: TemplateExerciseSet): String {
    val weight = set.targetWeight?.takeIf { it.isNotBlank() }?.let { " @ $it" } ?: ""
    return when {
        !set.targetReps.isNullOrBlank() -> "${set.targetReps} reps$weight"
        !set.targetSecs.isNullOrBlank() -> "${set.targetSecs}s$weight"
        weight.isNotEmpty() -> "Weight$weight"
        else -> "Open set"
    }
}

@Composable
fun TemplateDetailScreen(
    templateId: String,
    viewModel: WorkoutTemplateViewModel,
    onNavigateUp: () -> Unit
) {
    val templateFromDb by viewModel.getTemplateById(templateId).collectAsState(initial = null)
    val allExercises by viewModel.allMasterExercises.collectAsStateWithLifecycle()

    var editedName by remember { mutableStateOf("") }
    var editedExercises by remember { mutableStateOf<List<TemplateExercise>>(emptyList()) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(templateFromDb) {
        templateFromDb?.let {
            editedName = it.name
            editedExercises = it.templateExercises
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editedName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = {
                        templateFromDb?.let {
                            val updatedTemplate = it.copy(
                                name = editedName,
                                templateExercises = editedExercises
                            )
                            viewModel.update(updatedTemplate)
                        }
                        onNavigateUp()
                    }) {
                        Text("Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExerciseDialog = true }) {
                Icon(Icons.Filled.Add, "Add Exercise")
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Template Name") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(editedExercises, key = { it.id }) { templateExercise ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(templateExercise.exerciseName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Loop through each set and make its targets editable
                            templateExercise.sets.forEachIndexed { setIndex, set ->
                                TemplateSetEditorRow(
                                    set = set,
                                    setNumber = setIndex + 1,
                                    onSetChange = { updatedSet ->
                                        val newSets = templateExercise.sets.toMutableList()
                                        newSets[setIndex] = updatedSet
                                        val newExercise = templateExercise.copy(sets = newSets)
                                        editedExercises = editedExercises.map { if (it.id == newExercise.id) newExercise else it }
                                    },
                                    onDelete = {
                                        val newSets = templateExercise.sets.toMutableList()
                                        newSets.removeAt(setIndex)
                                        val newExercise = templateExercise.copy(sets = newSets)
                                        editedExercises = editedExercises.map { if (it.id == newExercise.id) newExercise else it }
                                    }
                                )
                            }

                            TextButton(
                                onClick = {
                                    val newSet = TemplateExerciseSet(id = UUID.randomUUID().toString(), targetReps = null, targetSecs = null)
                                    val newExercise = templateExercise.copy(sets = templateExercise.sets + newSet)
                                    editedExercises = editedExercises.map { if (it.id == newExercise.id) newExercise else it }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Add Set") }
                        }
                    }
                }
            }
        }

        if (showAddExerciseDialog) {
            AlertDialog(
                onDismissRequest = { showAddExerciseDialog = false },
                title = { Text("Add Exercise to Template") },
                text = {
                    LazyColumn {
                        items(allExercises) { exercise ->
                            Text(
                                text = exercise.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newTemplateExercise = TemplateExercise(
                                            id = UUID.randomUUID().toString(),
                                            exerciseId = exercise.id,
                                            exerciseName = exercise.name,
                                            targetMuscleGroups = exercise.targetMuscleGroups,
                                            equipment = exercise.equipment,
                                            order = (editedExercises.maxOfOrNull { it.order } ?: 0) + 1,
                                            sets = listOf(TemplateExerciseSet(id = UUID.randomUUID().toString()))
                                        )
                                        editedExercises = editedExercises + newTemplateExercise
                                        showAddExerciseDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAddExerciseDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun TemplateSetEditorRow(
    set: TemplateExerciseSet,
    setNumber: Int,
    onSetChange: (TemplateExerciseSet) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Set $setNumber", modifier = Modifier.width(60.dp))
        OutlinedTextField(
            value = set.targetReps ?: "",
            onValueChange = { onSetChange(set.copy(targetReps = it, targetSecs = null)) },
            label = { Text("Reps") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = set.targetSecs ?: "",
            onValueChange = { onSetChange(set.copy(targetSecs = it, targetReps = null)) },
            label = { Text("Secs") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = set.targetWeight ?: "",
            onValueChange = { onSetChange(set.copy(targetWeight = it.ifBlank { null })) },
            label = { Text("Weight") },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Set")
        }
    }
}