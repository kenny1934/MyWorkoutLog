@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import android.os.Bundle
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myworkoutlog.ui.theme.MyWorkoutLogTheme

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
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.personalRecordDao(),
            (application as WorkoutApplication).database.exerciseDao(),
            (application as WorkoutApplication).database.activeCycleDao()
        )
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.activeCycleDao(),
            (application as WorkoutApplication).database.programTemplateDao()
        )
    }

    private val programViewModel: ProgramViewModel by viewModels {
        ProgramViewModelFactory(
            (application as WorkoutApplication).database.programTemplateDao(),
            (application as WorkoutApplication).database.workoutTemplateDao()
        )
    }

    private val activeCycleViewModel: ActiveCycleViewModel by viewModels {
        ActiveCycleViewModelFactory((application as WorkoutApplication).database.activeCycleDao())
    }

    private val prViewModel: PrViewModel by viewModels {
        PrViewModelFactory((application as WorkoutApplication).database.personalRecordDao())
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((application as WorkoutApplication).appSettingsRepository)
    }

    private val volumeViewModel: VolumeViewModel by viewModels {
        VolumeViewModelFactory(
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.exerciseDao(),
            (application as WorkoutApplication).database.programTemplateDao(),
            (application as WorkoutApplication).database.activeCycleDao()
        )
    }

    private val analyticsViewModel: AnalyticsViewModel by viewModels {
        val analyticsRepository = AnalyticsRepository(
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.activeCycleDao(),
            (application as WorkoutApplication).database.personalRecordDao(),
            (application as WorkoutApplication).database.exerciseDao()
        )
        AnalyticsViewModelFactory(
            analyticsRepository,
            (application as WorkoutApplication).database.exerciseDao(),
            (application as WorkoutApplication).database.activeCycleDao()
        )
    }

    private val exportViewModel: ExportViewModel by viewModels {
        val exportRepository = ExportRepository(
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.exerciseDao(),
            (application as WorkoutApplication).database.personalRecordDao(),
            (application as WorkoutApplication).database.programTemplateDao(),
            (application as WorkoutApplication).database.activeCycleDao()
        )
        ExportViewModelFactory(exportRepository)
    }

    private val importViewModel: ImportViewModel by viewModels {
        val importRepository = ImportRepository(
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.exerciseDao(),
            (application as WorkoutApplication).database.personalRecordDao(),
            (application as WorkoutApplication).database.programTemplateDao(),
            (application as WorkoutApplication).database.workoutTemplateDao(),
            (application as WorkoutApplication).database.activeCycleDao()
        )
        ImportViewModelFactory(importRepository, this)
    }

    private val cloudBackupViewModel: CloudBackupViewModel by viewModels {
        val cloudProvider = GoogleDriveCloudProvider(this)
        val cloudBackupRepository = CloudBackupRepository(
            this,
            ExportRepository(
                (application as WorkoutApplication).database.loggedWorkoutDao(),
                (application as WorkoutApplication).database.exerciseDao(),
                (application as WorkoutApplication).database.personalRecordDao(),
                (application as WorkoutApplication).database.programTemplateDao(),
                (application as WorkoutApplication).database.activeCycleDao()
            ),
            ImportRepository(
                (application as WorkoutApplication).database.loggedWorkoutDao(),
                (application as WorkoutApplication).database.exerciseDao(),
                (application as WorkoutApplication).database.personalRecordDao(),
                (application as WorkoutApplication).database.programTemplateDao(),
                (application as WorkoutApplication).database.workoutTemplateDao(),
                (application as WorkoutApplication).database.activeCycleDao()
            ),
            cloudProvider
        )
        CloudBackupViewModelFactory(cloudBackupRepository)
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        val analyticsRepository = AnalyticsRepository(
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.activeCycleDao(),
            (application as WorkoutApplication).database.personalRecordDao(),
            (application as WorkoutApplication).database.exerciseDao()
        )
        val widgetRepository = WidgetRepositorySimplified(
            analyticsRepository,
            (application as WorkoutApplication).database.personalRecordDao(),
            (application as WorkoutApplication).database.loggedWorkoutDao(),
            (application as WorkoutApplication).database.activeCycleDao(),
            (application as WorkoutApplication).database.programTemplateDao(),
            (application as WorkoutApplication).database.workoutTemplateDao()
        )
        val preferencesManager = DashboardPreferencesManager(this)
        DashboardViewModelFactory(
            widgetRepository,
            (application as WorkoutApplication).database.activeCycleDao(),
            analyticsRepository,
            preferencesManager
        )
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
                historyViewModel = historyViewModel,
                activeCycleViewModel = activeCycleViewModel,
                programViewModel = programViewModel,
                navController = navController,
                dashboardViewModel = dashboardViewModel
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
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
            ManageProgramsScreen(
                programViewModel = programViewModel,
                activeCycleViewModel = activeCycleViewModel,
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
        composable(Screen.PersonalRecords.route) {
            PersonalRecordsScreen(
                viewModel = prViewModel,
                onNavigateToWorkout = { workoutId ->
                    navController.navigate(Screen.HistoryDetail.createRoute(workoutId))
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
            VolumeAnalysisScreen(viewModel = volumeViewModel)
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(viewModel = analyticsViewModel)
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