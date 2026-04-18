@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kennychiu.myworkoutlog.ui.theme.MyWorkoutLogTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as WorkoutApplication).container

    private val exerciseViewModel: ExerciseViewModel by viewModels { container.exerciseViewModelFactory() }
    private val workoutTemplateViewModel: WorkoutTemplateViewModel by viewModels { container.workoutTemplateViewModelFactory() }
    private val workoutLoggerViewModel: WorkoutLoggerViewModel by viewModels { container.workoutLoggerViewModelFactory() }
    private val historyViewModel: HistoryViewModel by viewModels { container.historyViewModelFactory() }
    private val programViewModel: ProgramViewModel by viewModels { container.programViewModelFactory() }
    private val activeCycleViewModel: ActiveCycleViewModel by viewModels { container.activeCycleViewModelFactory() }
    private val prViewModel: PrViewModel by viewModels { container.prViewModelFactory() }
    private val settingsViewModel: SettingsViewModel by viewModels { container.settingsViewModelFactory() }
    private val volumeViewModel: VolumeViewModel by viewModels { container.volumeViewModelFactory() }
    private val analyticsViewModel: AnalyticsViewModel by viewModels { container.analyticsViewModelFactory() }
    private val exportViewModel: ExportViewModel by viewModels { container.exportViewModelFactory() }
    private val importViewModel: ImportViewModel by viewModels { container.importViewModelFactory() }
    private val cloudBackupViewModel: CloudBackupViewModel by viewModels { container.cloudBackupViewModelFactory() }
    private val dashboardViewModel: DashboardViewModel by viewModels { container.dashboardViewModelFactory() }

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
                    programViewModel = programViewModel,
                    activeCycleViewModel = activeCycleViewModel,
                    prViewModel = prViewModel,
                    settingsViewModel = settingsViewModel,
                    volumeViewModel = volumeViewModel,
                    analyticsViewModel = analyticsViewModel,
                    exportViewModel = exportViewModel,
                    importViewModel = importViewModel,
                    cloudBackupViewModel = cloudBackupViewModel,
                    dashboardViewModel = dashboardViewModel
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
            programViewModel: ProgramViewModel,
            activeCycleViewModel: ActiveCycleViewModel,
            prViewModel: PrViewModel,
            settingsViewModel: SettingsViewModel,
            volumeViewModel: VolumeViewModel,
            analyticsViewModel: AnalyticsViewModel,
            exportViewModel: ExportViewModel,
            importViewModel: ImportViewModel,
            cloudBackupViewModel: CloudBackupViewModel,
            dashboardViewModel: DashboardViewModel
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
            activeCycleViewModel = activeCycleViewModel,
            prViewModel = prViewModel,
            settingsViewModel = settingsViewModel,
            volumeViewModel = volumeViewModel,
            analyticsViewModel = analyticsViewModel,
            exportViewModel = exportViewModel,
            importViewModel = importViewModel,
            cloudBackupViewModel = cloudBackupViewModel,
            dashboardViewModel = dashboardViewModel,
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
    activeCycleViewModel: ActiveCycleViewModel,
    prViewModel: PrViewModel,
    settingsViewModel: SettingsViewModel,
    volumeViewModel: VolumeViewModel,
    analyticsViewModel: AnalyticsViewModel,
    exportViewModel: ExportViewModel,
    importViewModel: ImportViewModel,
    cloudBackupViewModel: CloudBackupViewModel,
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val weightUnit by settingsViewModel.weightUnit.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                navController = navController
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateToWorkout = { workoutId ->
                    navController.navigate(Screen.HistoryDetail.createRoute(workoutId))
                },
                onNavigateToEdit = { workoutId ->
                    navController.navigate(Screen.EditWorkout.createRoute(workoutId))
                }
            )
        }
        composable(Screen.HistoryDetail.route) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
            HistoryDetailScreen(
                workoutId = workoutId,
                viewModel = historyViewModel,
                onNavigateUp = { navController.navigateUp() },
                onNavigateToEdit = { editWorkoutId ->
                    navController.navigate(Screen.EditWorkout.createRoute(editWorkoutId))
                }
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
            val cycleId = backStackEntry.arguments?.getString("cycleId")
            val weekId = backStackEntry.arguments?.getString("weekId")
            val sessionId = backStackEntry.arguments?.getString("sessionId")

            WorkoutLoggerScreen(
                templateId = templateId,
                cycleId = cycleId,
                weekId = weekId,
                sessionId = sessionId,
                viewModel = loggerViewModel,
                activeCycleViewModel = activeCycleViewModel,
                weightUnit = weightUnit,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.EditWorkout.route) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
            EditWorkoutScreen(
                workoutId = workoutId,
                viewModel = loggerViewModel,
                activeCycleViewModel = activeCycleViewModel,
                weightUnit = weightUnit,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.Programs.route) {
            AdaptiveProgramManagementScreen(
                programViewModel = programViewModel,
                activeCycleViewModel = activeCycleViewModel,
                templateViewModel = templateViewModel,
                onNavigateToProgram = { programId ->
                    navController.navigate(Screen.ProgramEditor.createRoute(programId))
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ProgramEditor.route) { backStackEntry ->
            val programId = backStackEntry.arguments?.getString("programId") ?: ""
            ProgramEditorScreen(
                programId = programId,
                programViewModel = programViewModel,
                templateViewModel = templateViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.CycleDetail.route) {
            CycleDetailScreen(
                activeCycleViewModel = activeCycleViewModel,
                onNavigateUp = { navController.navigateUp() },
            )
        }
        composable(Screen.PersonalRecords.route) {
            PersonalRecordsScreen(
                viewModel = prViewModel,
                onNavigateToWorkout = { workoutId ->
                    navController.navigate(Screen.HistoryDetail.createRoute(workoutId))
                },
                onNavigateToExerciseAnalytics = { exerciseId ->
                    navController.navigate(Screen.Analytics.createRouteWithTab("Performance", exerciseId))
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToExport = {
                    navController.navigate("export")
                },
                onNavigateToImport = {
                    navController.navigate("import")
                },
                onNavigateToCloudBackup = {
                    navController.navigate("cloud_backup")
                }
            )
        }
        composable("export") {
            ExportScreen(
                viewModel = exportViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable("import") {
            ImportScreen(
                viewModel = importViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable("cloud_backup") {
            CloudBackupScreen(
                viewModel = cloudBackupViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.VolumeAnalysis.route) {
            VolumeAnalysisScreen(
                viewModel = volumeViewModel,
                onNavigateToMuscleGroupAnalytics = { muscleGroup ->
                    Log.d("MainActivity", "Navigating to muscle group analytics: $muscleGroup")
                    val route = Screen.Analytics.createRouteWithMuscleGroup(muscleGroup)
                    navController.navigate(route)
                }
            )
        }
        composable(
            route = "analytics?exerciseId={exerciseId}&tab={tab}&cycleId={cycleId}&muscleGroup={muscleGroup}",
            arguments = listOf(
                navArgument("exerciseId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("tab") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("cycleId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("muscleGroup") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId")
            val tab = backStackEntry.arguments?.getString("tab")
            val cycleId = backStackEntry.arguments?.getString("cycleId")
            val muscleGroup = backStackEntry.arguments?.getString("muscleGroup")
            
            if (muscleGroup != null) {
                Log.d("MainActivity", "Analytics opened with muscle group filter: $muscleGroup")
            }
            
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                preSelectedExerciseId = exerciseId,
                preSelectedTab = tab,
                preSelectedCycleId = cycleId,
                preSelectedMuscleGroup = muscleGroup
            )
        }
        
        // Default Analytics route without parameters
        composable("analytics") { backStackEntry ->
            Log.d("MainActivity", "=== DEFAULT ANALYTICS ROUTE ===")
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                preSelectedExerciseId = null,
                preSelectedTab = null,
                preSelectedCycleId = null,
                preSelectedMuscleGroup = null
            )
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
                    when (screen) {
                        Screen.Dashboard -> Icon(Icons.Filled.Home, contentDescription = "Dashboard")
                        Screen.History -> Icon(Icons.Filled.DateRange, contentDescription = "History")
                        Screen.Library -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library")
                        else -> {}
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