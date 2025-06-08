package com.example.myworkoutlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myworkoutlog.ui.theme.MyWorkoutLogTheme
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    private val exerciseViewModel: ExerciseViewModel by viewModels {
        ExerciseViewModelFactory((application as WorkoutApplication).database.exerciseDao())
    }
    private val workoutTemplateViewModel: WorkoutTemplateViewModel by viewModels {
        WorkoutTemplateViewModelFactory(
            (application as WorkoutApplication).database.workoutTemplateDao(),
            (application as WorkoutApplication).database.exerciseDao()
        )
    }

    private val workoutLoggerViewModel: WorkoutLoggerViewModel by viewModels {
        WorkoutLoggerViewModelFactory(
            (application as WorkoutApplication).database.workoutTemplateDao(),
            (application as WorkoutApplication).database.loggedWorkoutDao()
        )
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory((application as WorkoutApplication).database.loggedWorkoutDao())
    }

    private val programViewModel: ProgramViewModel by viewModels {
        ProgramViewModelFactory((application as WorkoutApplication).database.programTemplateDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyWorkoutLogTheme {
                MainApp(
                    exerciseViewModel = exerciseViewModel,
                    templateViewModel = workoutTemplateViewModel,
                    loggerViewModel = workoutLoggerViewModel,
                    historyViewModel = historyViewModel,
                    programViewModel = programViewModel
                )
            }
        }
    }
}

@Composable
fun MainApp(exerciseViewModel: ExerciseViewModel,
            templateViewModel: WorkoutTemplateViewModel,
            loggerViewModel: WorkoutLoggerViewModel,
            historyViewModel: HistoryViewModel,
            programViewModel: ProgramViewModel
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { AppBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            exerciseViewModel = exerciseViewModel,
            templateViewModel = templateViewModel,
            loggerViewModel = loggerViewModel,
            historyViewModel = historyViewModel,
            programViewModel = programViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    exerciseViewModel: ExerciseViewModel,
    templateViewModel: WorkoutTemplateViewModel,
    loggerViewModel: WorkoutLoggerViewModel,
    historyViewModel: HistoryViewModel,
    programViewModel: ProgramViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                historyViewModel = historyViewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                // Pass a navigation action to the history screen
                onNavigateToWorkout = { workoutId ->
                    navController.navigate(Screen.HistoryDetail.createRoute(workoutId))
                }
            )
        }
        composable(Screen.HistoryDetail.route) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
            HistoryDetailScreen(
                workoutId = workoutId,
                viewModel = historyViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.ManageExercises.route) {
            ManageExercisesScreen(viewModel = exerciseViewModel)
        }
        composable(Screen.ManageTemplates.route) {
            ManageTemplatesScreen(
                viewModel = templateViewModel,
                onNavigateToTemplate = { templateId ->
                    navController.navigate(Screen.TemplateDetail.createRoute(templateId))
                },
                onStartWorkout = { templateId ->
                    navController.navigate(Screen.WorkoutLogger.createRoute(templateId))
                }
            )
        }
        composable(Screen.TemplateDetail.route) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            TemplateDetailScreen(
                templateId = templateId,
                viewModel = templateViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.WorkoutLogger.route) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            WorkoutLoggerScreen(
                templateId = templateId,
                viewModel = loggerViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.ManagePrograms.route) {
            ManageProgramsScreen(viewModel = programViewModel)
        }
    }
}



@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Dashboard,
        Screen.History,
        Screen.Library
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    // This 'when' block is now exhaustive
                    when (screen) {
                        Screen.Dashboard -> Icon(Icons.Filled.Home, contentDescription = "Dashboard")
                        Screen.History -> Icon(Icons.Filled.DateRange, contentDescription = "History")
                        Screen.Library -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library")
                        // The 'else' branch handles any other cases, satisfying the compiler.
                        // We need this even though our 'items' list is specific.
                        else -> {} // Do nothing for other screens not in the nav bar
                    }
                },
                label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}


// --- Our Screens ---

@Composable
fun DashboardScreen(
    historyViewModel: HistoryViewModel,
    onNavigate: (String) -> Unit
) {
    val loggedWorkouts by historyViewModel.allLoggedWorkouts.collectAsStateWithLifecycle()
    val latestWorkout = loggedWorkouts.firstOrNull()

    // --- NEW: Prepare data and labels for the chart ---
    val chartData = loggedWorkouts
        .filter { it.bodyweight != null && it.bodyweight > 0 }
        .sortedBy { it.date }

    val bodyweightEntries = chartData.mapIndexed { index, workout ->
        entryOf(index.toFloat(), workout.bodyweight!!.toFloat())
    }

    val chartModelProducer = ChartEntryModelProducer(bodyweightEntries)

    // --- NEW: Create a custom formatter for the bottom axis ---
    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        // We use the integer part of the value as an index to get the date
        val index = value.toInt()
        if (index in chartData.indices) {
            // Format the date string like "06/08" from "2025-06-08"
            val date = chartData[index].date
            date.substring(5).replace('-', '/')
        } else {
            ""
        }
    }

    // --- NEW: Dynamic Greeting Logic ---
    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning!"
        in 12..16 -> "Good afternoon!"
        else -> "Good evening!"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Use the new dynamic greeting
                    Text(greeting, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ready for your next session?", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // The chart now has its axes configured with the new formatter
        if (bodyweightEntries.isNotEmpty()) {
            item {
                Text("Bodyweight Trend", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Chart(
                        chart = lineChart(),
                        chartModelProducer = chartModelProducer,
                        startAxis = rememberStartAxis(
                            title = "Bodyweight" // Add a title to the Y-axis
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = bottomAxisValueFormatter, // Use our custom date formatter
                            guideline = null // Hide vertical guidelines for a cleaner look
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }


        item {
            Text("Start a new workout:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onNavigate(Screen.Library.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose from Template")
            }
        }

        if (latestWorkout != null) {
            item {
                Text("Last Workout", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Navigate to the detail screen for this specific workout
                            onNavigate(Screen.HistoryDetail.createRoute(latestWorkout.id))
                        },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(latestWorkout.name ?: "Workout", fontWeight = FontWeight.Bold)
                        Text(latestWorkout.date, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${latestWorkout.loggedExercises.size} exercises performed.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToWorkout: (String) -> Unit // New parameter for navigation
) {
    val loggedWorkouts by viewModel.allLoggedWorkouts.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Workout History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (loggedWorkouts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workouts logged yet.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(loggedWorkouts) { workout ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            // In the future, this will navigate to a detail view
                            .clickable { onNavigateToWorkout(workout.id) },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(workout.name ?: "Workout", fontWeight = FontWeight.Bold)
                            Text(workout.date, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    workoutId: String,
    viewModel: HistoryViewModel,
    onNavigateUp: () -> Unit
) {
    val workout by viewModel.getLoggedWorkoutById(workoutId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (workout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(workout!!.date, style = MaterialTheme.typography.headlineSmall)
                    if (!workout!!.overallComments.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(workout!!.overallComments!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                items(workout!!.loggedExercises) { exercise ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(exercise.exerciseName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            // Display the header for the sets table
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Set", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall)
                                Text("Weight", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                Text("Reps", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            // Display each logged set
                            exercise.sets.forEachIndexed { index, set ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text((index + 1).toString(), modifier = Modifier.width(60.dp))
                                    Text(set.weight?.toString() ?: "--", modifier = Modifier.weight(1f))
                                    Text(set.reps?.toString() ?: "--", modifier = Modifier.weight(1f))
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
fun LibraryScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Library", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.ManageExercises.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Manage Exercises", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.ManageTemplates.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Manage Templates", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        // NEW BUTTON
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.ManagePrograms.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Manage Program Blueprints", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProgramsScreen(viewModel: ProgramViewModel) {
    val programs by viewModel.allPrograms.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var programName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create new program")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("Program Blueprints", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (programs.isEmpty()) {
                Text(
                    "No programs yet. Click the '+' button to create one.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(programs) { program ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* TODO: Navigate to program editor */ },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Text(program.name, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("New Program Blueprint") },
                    text = {
                        OutlinedTextField(
                            value = programName,
                            onValueChange = { programName = it },
                            label = { Text("Program Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (programName.isNotBlank()) {
                                    viewModel.insert(programName, null)
                                    programName = ""
                                    showDialog = false
                                }
                            }
                        ) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTemplatesScreen(
    viewModel: WorkoutTemplateViewModel,
    onNavigateToTemplate: (String) -> Unit,
    onStartWorkout: (String) -> Unit
) {
    val templates by viewModel.allTemplates.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create new template")
            }
        }
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
                                // ADDED "START" BUTTON
                                IconButton(
                                    onClick = { onStartWorkout(template.id) },
                                    modifier = Modifier.padding(16.dp)
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
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("New Workout Template") },
                    text = {
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Template Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (templateName.isNotBlank()) {
                                    viewModel.insert(templateName, null)
                                    templateName = ""
                                    showDialog = false
                                }
                            }
                        ) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLoggerScreen(
    templateId: String,
    viewModel: WorkoutLoggerViewModel,
    onNavigateUp: () -> Unit
) {
    // LaunchedEffect runs a coroutine when the composable first appears.
    // We use it to tell the ViewModel to load our template.
    // The 'key1 = templateId' means it will only re-run if the templateId changes.
    LaunchedEffect(key1 = templateId) {
        viewModel.startWorkoutFromTemplate(templateId)
    }

    // Collect the active workout state from the ViewModel.
    // The UI will automatically recompose whenever this state changes.
    val activeWorkout by viewModel.activeWorkoutState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activeWorkout?.name ?: "Log Workout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = {
                        viewModel.finishWorkout()
                        onNavigateUp()
                    }) {
                        Text("Finish")
                    }
                }
            )
        }
    ) { paddingValues ->
        // If the workout is not loaded yet, show a loading indicator.
        if (activeWorkout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Once loaded, display the list of exercises.
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // NEW: Add a bodyweight input field as the first item in the list
                item {
                    OutlinedTextField(
                        value = activeWorkout!!.bodyweight?.toString() ?: "",
                        onValueChange = { newBw ->
                            viewModel.updateBodyweight(newBw)
                        },
                        label = { Text("Bodyweight") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(activeWorkout!!.loggedExercises) { exercise ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(exercise.exerciseName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            // For each exercise, display its sets
                            exercise.sets.forEachIndexed { index, set ->
                                LoggedSetRow(
                                    set = set,
                                    setNumber = index + 1,
                                    onRepsChange = { newReps ->
                                        viewModel.updateSet(exercise.id, set.id, newReps, set.weight?.toString() ?: "")
                                    },
                                    onWeightChange = { newWeight ->
                                        viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", newWeight)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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

    // This effect now runs only when the template from the database (templateFromDb)
    // changes, and it updates the local, editable state.
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
                        // When saving, use the current state of our editable variables
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
        }
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

            // This LazyColumn now shows the exercises and their sets
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(editedExercises) { templateExercise ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(templateExercise.exerciseName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))

                            // Display a row for each set of this exercise
                            templateExercise.sets.forEachIndexed { setIndex, set ->
                                TemplateExerciseSetRow(
                                    set = set,
                                    onRepsChange = { newReps ->
                                        // Update the reps for this specific set
                                        val updatedSets = templateExercise.sets.toMutableList()
                                        updatedSets[setIndex] = set.copy(targetReps = newReps)
                                        val updatedExercise = templateExercise.copy(sets = updatedSets)
                                        editedExercises = editedExercises.map {
                                            if (it.id == templateExercise.id) updatedExercise else it
                                        }
                                    },
                                    onDelete = {
                                        // Delete this specific set
                                        val updatedSets = templateExercise.sets.toMutableList()
                                        updatedSets.removeAt(setIndex)
                                        val updatedExercise = templateExercise.copy(sets = updatedSets)
                                        editedExercises = editedExercises.map {
                                            if (it.id == templateExercise.id) updatedExercise else it
                                        }
                                    }
                                )
                            }

                            // Button to add a new set to this exercise
                            TextButton(
                                onClick = {
                                    val newSet = TemplateExerciseSet(id = UUID.randomUUID().toString(), targetReps = "")
                                    val updatedSets = templateExercise.sets + newSet
                                    val updatedExercise = templateExercise.copy(sets = updatedSets)
                                    editedExercises = editedExercises.map {
                                        if (it.id == templateExercise.id) updatedExercise else it
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Add Set")
                            }
                        }
                    }
                }
            }
        }

        // Dialog for adding an exercise (remains unchanged)
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
                                            order = (editedExercises.maxOfOrNull { it.order } ?: 0) + 1,
                                            sets = listOf(
                                                TemplateExerciseSet(
                                                    id = UUID.randomUUID().toString(),
                                                    targetReps = "8-12"
                                                )
                                            )
                                        )
                                        editedExercises = editedExercises + newTemplateExercise
                                        showAddExerciseDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddExerciseDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun TemplateExerciseSetRow(
    set: TemplateExerciseSet,
    onRepsChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = set.targetReps ?: "",
            onValueChange = onRepsChange,
            label = { Text("Reps / Secs") },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Set")
        }
    }
}

@Composable
fun LoggedSetRow(
    set: LoggedSet,
    setNumber: Int,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Set $setNumber",
            modifier = Modifier.width(60.dp),
            fontWeight = FontWeight.Bold
        )
        // Text field for weight
        OutlinedTextField(
            value = set.weight?.toString() ?: "",
            onValueChange = onWeightChange,
            label = { Text("Weight") },
            modifier = Modifier.weight(1f)
        )
        // Text field for reps
        OutlinedTextField(
            value = set.reps?.toString() ?: "",
            onValueChange = onRepsChange,
            label = { Text("Reps") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ManageExercisesScreen(viewModel: ExerciseViewModel) {
    val exercises by viewModel.allExercises.collectAsStateWithLifecycle()
    var newExerciseName by remember { mutableStateOf("") }
    var newExerciseEquipment by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Manage Exercises", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(
            value = newExerciseName,
            onValueChange = { newExerciseName = it },
            label = { Text("Exercise Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newExerciseEquipment,
            onValueChange = { newExerciseEquipment = it },
            label = { Text("Equipment (e.g., DUMBBELL)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (newExerciseName.isNotBlank() && newExerciseEquipment.isNotBlank()) {
                    viewModel.insert(newExerciseName, newExerciseEquipment)
                    newExerciseName = ""
                    newExerciseEquipment = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Exercise")
        }
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn {
            items(exercises) { exercise ->
                ExerciseItem(exercise = exercise)
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Equipment: ${exercise.equipment.joinToString()}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}