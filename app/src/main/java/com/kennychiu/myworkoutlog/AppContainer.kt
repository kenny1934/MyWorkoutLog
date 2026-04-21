package com.kennychiu.myworkoutlog

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import android.content.Context

// Manual DI container. Holds process-wide singletons (database, repositories,
// preferences) and produces ViewModel factories. Previously this wiring lived
// inline in MainActivity with the same `(application as WorkoutApplication).database.xDao()`
// pattern repeated ~40 times.
//
// Factories are declared as methods (not properties) because each `by viewModels { ... }`
// call in an Activity/Fragment needs its own factory instance.
class AppContainer(appContext: Context) {

    private val applicationContext: Context = appContext.applicationContext

    val database: WorkoutDatabase = WorkoutDatabase.getDatabase(applicationContext)

    // DAOs
    val exerciseDao get() = database.exerciseDao()
    val workoutTemplateDao get() = database.workoutTemplateDao()
    val loggedWorkoutDao get() = database.loggedWorkoutDao()
    val programTemplateDao get() = database.programTemplateDao()
    val activeCycleDao get() = database.activeCycleDao()
    val personalRecordDao get() = database.personalRecordDao()
    val bodyweightDao get() = database.bodyweightDao()

    // Preferences
    val appSettingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(applicationContext)
    }
    val dashboardPreferencesManager: DashboardPreferencesManager by lazy {
        DashboardPreferencesManager(applicationContext)
    }

    // Repositories (lazy — only built on first use)
    val analyticsRepository: AnalyticsRepository by lazy {
        AnalyticsRepository(loggedWorkoutDao, activeCycleDao, personalRecordDao, exerciseDao)
    }
    val exportRepository: ExportRepository by lazy {
        ExportRepository(
            loggedWorkoutDao,
            exerciseDao,
            personalRecordDao,
            programTemplateDao,
            activeCycleDao,
        )
    }
    val importRepository: ImportRepository by lazy {
        ImportRepository(
            loggedWorkoutDao,
            exerciseDao,
            personalRecordDao,
            programTemplateDao,
            workoutTemplateDao,
            activeCycleDao,
        )
    }
    val cloudBackupRepository: CloudBackupRepository by lazy {
        CloudBackupRepository(
            applicationContext,
            exportRepository,
            importRepository,
            GoogleDriveCloudProvider(applicationContext),
        )
    }
    val widgetRepository: WidgetRepositorySimplified by lazy {
        WidgetRepositorySimplified(
            analyticsRepository,
            personalRecordDao,
            loggedWorkoutDao,
            activeCycleDao,
            programTemplateDao,
            workoutTemplateDao,
            bodyweightDao,
        )
    }

    // ViewModel factories
    fun exerciseViewModelFactory() = ExerciseViewModelFactory(exerciseDao)

    fun workoutTemplateViewModelFactory() =
        WorkoutTemplateViewModelFactory(workoutTemplateDao, exerciseDao)

    fun workoutLoggerViewModelFactory() = WorkoutLoggerViewModelFactory(
        workoutTemplateDao,
        loggedWorkoutDao,
        personalRecordDao,
        exerciseDao,
        activeCycleDao,
        bodyweightDao,
    )

    fun historyViewModelFactory() =
        HistoryViewModelFactory(loggedWorkoutDao, activeCycleDao, programTemplateDao)

    fun programViewModelFactory() =
        ProgramViewModelFactory(programTemplateDao, workoutTemplateDao)

    fun activeCycleViewModelFactory() = ActiveCycleViewModelFactory(activeCycleDao)

    fun cycleDetailViewModelFactory() = CycleDetailViewModelFactory(
        activeCycleDao,
        loggedWorkoutDao,
        personalRecordDao,
        workoutTemplateDao,
    )

    fun prViewModelFactory() = PrViewModelFactory(personalRecordDao)

    fun settingsViewModelFactory() = SettingsViewModelFactory(appSettingsRepository)

    fun volumeViewModelFactory() = VolumeViewModelFactory(
        loggedWorkoutDao,
        exerciseDao,
        programTemplateDao,
        activeCycleDao,
    )

    fun analyticsViewModelFactory() =
        AnalyticsViewModelFactory(analyticsRepository, exerciseDao, activeCycleDao)

    fun exportViewModelFactory() = ExportViewModelFactory(exportRepository)

    fun importViewModelFactory() = ImportViewModelFactory(importRepository, applicationContext)

    fun cloudBackupViewModelFactory() = CloudBackupViewModelFactory(cloudBackupRepository)

    fun dashboardViewModelFactory() = DashboardViewModelFactory(
        widgetRepository,
        activeCycleDao,
        analyticsRepository,
        dashboardPreferencesManager,
        bodyweightDao,
        appSettingsRepository,
        loggedWorkoutDao,
    )
}
