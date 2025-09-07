# API Documentation

## MyWorkoutLog - Internal APIs & Data Structures

### Version: 1.0  
### Date: September 2025
### Scope: Internal Application APIs

---

## 1. Overview

This document provides comprehensive documentation for MyWorkoutLog's internal APIs, including Repository patterns, Service objects, and data transformation interfaces. The application follows clean architecture principles with well-defined API contracts between layers.

### 1.1 API Architecture Layers
```
┌─────────────────────────────────────────────────┐
│                  UI Layer                       │
│              (Compose Functions)                │
└─────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────┐
│               ViewModel APIs                    │
│         (StateFlow & Coroutine-based)          │
└─────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────┐
│              Repository APIs                    │
│       (Data Access Abstraction Layer)          │
└─────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────┐
│             Service & Utility APIs              │
│        (Business Logic & Data Processing)      │
└─────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────┐
│              Data Access APIs                   │
│           (DAO Interfaces & Database)          │
└─────────────────────────────────────────────────┘
```

---

## 2. Repository APIs

### 2.1 AnalyticsRepository

**Purpose**: Advanced analytics and data aggregation services

#### 2.1.1 Volume Analysis
```kotlin
class AnalyticsRepository(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val activeCycleDao: ActiveCycleDao,
    private val personalRecordDao: PersonalRecordDao,
    private val exerciseDao: ExerciseDao? = null
) {
    fun getVolumeProgressionData(
        startDate: String,
        endDate: String,
        exerciseId: String? = null
    ): Flow<List<VolumeDataPoint>>
    
    fun getWeeklyVolumeSummary(weekStartDate: String): Flow<VolumeSummary>
    
    fun getMuscleGroupVolumeDistribution(
        startDate: String,
        endDate: String
    ): Flow<List<MuscleGroupVolume>>
}
```

**Return Types**:
```kotlin
data class VolumeDataPoint(
    val date: String,
    val totalVolume: Double,
    val workoutName: String? = null,
    val cycleId: String? = null
)

data class VolumeSummary(
    val periodLabel: String,
    val startDate: String,
    val endDate: String,
    val totalVolume: Double,
    val workoutCount: Int,
    val averageVolumePerWorkout: Double,
    val exerciseBreakdown: List<ExerciseVolumeBreakdown>
)
```

#### 2.1.2 Performance Analysis
```kotlin
interface AnalyticsRepository {
    fun getExercisePerformanceData(
        exerciseId: String,
        startDate: String,
        endDate: String
    ): Flow<List<ExercisePerformancePoint>>
    
    fun getPerformanceTrends(
        exerciseIds: List<String>,
        timeframeWeeks: Int
    ): Flow<List<PerformanceTrend>>
    
    fun getCycleComparison(
        currentCycleId: String,
        previousCycleId: String?
    ): Flow<CycleComparison>
}
```

### 2.2 CloudBackupRepository

**Purpose**: Cloud backup and synchronization services

#### 2.2.1 Core Operations
```kotlin
class CloudBackupRepository(
    private val cloudProvider: CloudProvider,
    private val database: WorkoutDatabase
) {
    suspend fun createBackup(
        backupType: CloudBackupType = CloudBackupType.MANUAL
    ): CloudResult<CloudBackup>
    
    suspend fun getAvailableBackups(): CloudResult<List<CloudBackupItem>>
    
    suspend fun restoreFromBackup(
        backupId: String,
        overwriteExisting: Boolean = false
    ): CloudResult<RestoreResult>
    
    suspend fun deleteBackup(backupId: String): CloudResult<Unit>
}
```

**Result Types**:
```kotlin
sealed class CloudResult<T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error<T>(val exception: Exception, val message: String) : CloudResult<T>()
    data class Loading<T>(val progress: Float = 0f) : CloudResult<T>()
}

data class RestoreResult(
    val workoutsRestored: Int,
    val exercisesRestored: Int,
    val personalRecordsRestored: Int,
    val conflictsResolved: Int,
    val warnings: List<String> = emptyList()
)
```

#### 2.2.2 Authentication & User Management
```kotlin
interface CloudBackupRepository {
    suspend fun authenticateUser(): CloudResult<CloudUser>
    suspend fun signOut(): CloudResult<Unit>
    suspend fun getCurrentUser(): CloudResult<CloudUser?>
    suspend fun getUserQuota(): CloudResult<CloudQuota>
}

data class CloudUser(
    val id: String,
    val email: String,
    val displayName: String,
    val profilePictureUrl: String?
)

data class CloudQuota(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val backupCount: Int,
    val maxBackupCount: Int
)
```

### 2.3 ExportRepository

**Purpose**: Data export and serialization services

#### 2.3.1 Export Configuration
```kotlin
class ExportRepository(private val database: WorkoutDatabase) {
    suspend fun exportData(config: ExportConfiguration): ExportResult
    
    suspend fun getExportPreview(config: ExportConfiguration): ExportPreview
    
    suspend fun getAvailableFormats(): List<ExportFormat>
}

data class ExportConfiguration(
    val format: ExportFormat,
    val includeWorkouts: Boolean,
    val includeExercises: Boolean,
    val includePersonalRecords: Boolean,
    val includePrograms: Boolean,
    val dateRange: DateRange?,
    val includeImages: Boolean = false,
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM
)

enum class ExportFormat {
    JSON, CSV, EXCEL, PDF
}

enum class CompressionLevel {
    NONE, LOW, MEDIUM, HIGH
}
```

#### 2.3.2 Export Results
```kotlin
data class ExportResult(
    val success: Boolean,
    val filePath: String?,
    val format: ExportFormat,
    val sizeBytes: Long,
    val itemsExported: ExportItemCounts,
    val exportTime: Long,
    val errors: List<String> = emptyList()
)

data class ExportItemCounts(
    val workouts: Int,
    val exercises: Int,
    val personalRecords: Int,
    val programs: Int,
    val templates: Int
)
```

### 2.4 ImportRepository

**Purpose**: Data import and validation services

#### 2.4.1 Import Operations
```kotlin
class ImportRepository(private val database: WorkoutDatabase) {
    suspend fun validateImportFile(fileUri: String): ImportValidationResult
    
    suspend fun importData(
        fileUri: String,
        config: ImportConfiguration
    ): ImportResult
    
    suspend fun getImportPreview(fileUri: String): ImportPreview
}

data class ImportConfiguration(
    val overwriteExisting: Boolean = false,
    val mergeStrategy: MergeStrategy = MergeStrategy.SKIP_DUPLICATES,
    val validateData: Boolean = true,
    val createBackup: Boolean = true
)

enum class MergeStrategy {
    OVERWRITE, SKIP_DUPLICATES, MERGE_FIELDS, CREATE_COPIES
}
```

#### 2.4.2 Validation & Results  
```kotlin
data class ImportValidationResult(
    val isValid: Boolean,
    val format: ExportFormat?,
    val version: String?,
    val itemCounts: ExportItemCounts?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val isCompatible: Boolean
)

data class ImportResult(
    val success: Boolean,
    val itemsImported: ExportItemCounts,
    val itemsSkipped: ExportItemCounts,
    val importTime: Long,
    val backupCreated: String?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)
```

---

## 3. Service APIs

### 3.1 PrService

**Purpose**: Personal Record detection and management

#### 3.1.1 PR Detection
```kotlin
object PrService {
    fun detectNewPRs(
        workout: LoggedWorkout,
        existingPRs: List<PersonalRecord>,
        allMasterExercises: List<Exercise>
    ): List<PersonalRecord>
    
    fun calculateEstimated1RM(weight: Double, reps: Int): Double
    
    fun comparePRs(
        current: PersonalRecord,
        previous: PersonalRecord
    ): PRComparison
}

data class PRComparison(
    val improvementPercentage: Double,
    val improvementType: PRImprovementType,
    val weightDifference: Double?,
    val repDifference: Int?,
    val durationDifference: Int?
)

enum class PRImprovementType {
    WEIGHT_INCREASE,
    REP_INCREASE,
    DURATION_INCREASE,
    NEW_PR,
    NO_IMPROVEMENT
}
```

#### 3.1.2 Bodyweight Exercise Support
```kotlin
interface PrService {
    fun calculateEffectiveWeight(
        exercise: Exercise,
        addedWeight: Double,
        userBodyweight: Double
    ): Double
    
    fun splitBodyweightComponents(
        personalRecord: PersonalRecord
    ): BodyweightBreakdown
}

data class BodyweightBreakdown(
    val totalWeight: Double,
    val bodyweightComponent: Double,
    val externalWeight: Double,
    val usesBodyweight: Boolean
)
```

### 3.2 PerformanceSuggestionService

**Purpose**: Smart pre-fill and progression recommendations

#### 3.2.1 Suggestion Generation
```kotlin
class PerformanceSuggestionService(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val personalRecordDao: PersonalRecordDao
) {
    suspend fun generateSuggestions(
        exerciseId: String,
        userBodyweight: Double?
    ): List<PerformanceSuggestion>
    
    suspend fun getProgressionRecommendation(
        exerciseId: String,
        currentPerformance: LoggedSet
    ): ProgressionRecommendation
}

data class PerformanceSuggestion(
    val suggestedWeight: Double? = null,
    val suggestedReps: Int? = null,
    val suggestedSecs: Int? = null,
    val suggestedRir: Int? = null,
    val confidence: Float = 0f,
    val basedonLastWorkout: Boolean = false,
    val daysAgo: Int? = null,
    val progressionType: ProgressionType = ProgressionType.MAINTAIN
)

enum class ProgressionType {
    INCREASE, MAINTAIN, DECREASE
}
```

### 3.3 WorkoutSessionService  

**Purpose**: Session persistence and recovery

#### 3.3.1 Session Management
```kotlin
class WorkoutSessionService(private val loggedWorkoutDao: LoggedWorkoutDao) {
    suspend fun getActiveSession(): WorkoutSessionStatus
    
    suspend fun persistSession(workout: LoggedWorkout)
    
    suspend fun clearSession(workoutId: String)
    
    suspend fun recoverSession(workoutId: String): LoggedWorkout?
}

sealed class WorkoutSessionStatus {
    object None : WorkoutSessionStatus()
    data class InProgress(
        val workout: LoggedWorkout, 
        val hoursAgo: Int
    ) : WorkoutSessionStatus()
}
```

---

## 4. Data Access APIs

### 4.1 Core DAO Interfaces

#### 4.1.1 ExerciseDao
```kotlin
@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise)
    
    @Query("SELECT * FROM exercise_table ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercise_table WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<Exercise?>
    
    @Update
    suspend fun updateExercise(exercise: Exercise)
    
    @Delete  
    suspend fun deleteExercise(exercise: Exercise)
    
    @Query("""
        SELECT * FROM exercise_table 
        WHERE name LIKE '%' || :searchQuery || '%' 
        ORDER BY name ASC
    """)
    fun searchExercises(searchQuery: String): Flow<List<Exercise>>
}
```

#### 4.1.2 LoggedWorkoutDao
```kotlin
@Dao
interface LoggedWorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loggedWorkout: LoggedWorkout)
    
    @Query("SELECT * FROM logged_workout_table ORDER BY startTimestamp DESC, date DESC")
    fun getAllLoggedWorkouts(): Flow<List<LoggedWorkout>>
    
    @Query("SELECT * FROM logged_workout_table WHERE id = :workoutId")
    fun getLoggedWorkoutById(workoutId: String): Flow<LoggedWorkout?>
    
    // Analytics-specific queries
    @Query("SELECT * FROM logged_workout_table WHERE date BETWEEN :startDate AND :endDate")
    fun getWorkoutsByDateRange(startDate: String, endDate: String): Flow<List<LoggedWorkout>>
    
    @Query("SELECT * FROM logged_workout_table WHERE activeProgramCycleId = :cycleId")
    fun getWorkoutsByCycle(cycleId: String): Flow<List<LoggedWorkout>>
    
    // Session persistence
    @Query("SELECT * FROM logged_workout_table WHERE isInProgress = 1")
    fun getWorkoutsInProgress(): Flow<List<LoggedWorkout>>
    
    @Query("UPDATE logged_workout_table SET isInProgress = 0 WHERE id = :workoutId") 
    suspend fun markWorkoutCompleted(workoutId: String)
}
```

#### 4.1.3 PersonalRecordDao
```kotlin
@Dao
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(personalRecord: PersonalRecord)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personalRecords: List<PersonalRecord>)
    
    @Query("SELECT * FROM personal_record_table WHERE exerciseId = :exerciseId")
    fun getPersonalRecordsForExercise(exerciseId: String): Flow<List<PersonalRecord>>
    
    @Query("SELECT * FROM personal_record_table ORDER BY date DESC")
    fun getAllPersonalRecords(): Flow<List<PersonalRecord>>
    
    @Query("SELECT * FROM personal_record_table ORDER BY date DESC LIMIT :limit")
    fun getRecentPersonalRecords(limit: Int): Flow<List<PersonalRecord>>
}
```

### 4.2 Complex Query APIs

#### 4.2.1 Analytics Queries
```kotlin
interface LoggedWorkoutDao {
    @Query("""
        SELECT lw.* FROM logged_workout_table lw
        WHERE lw.loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%'
        AND lw.date BETWEEN :startDate AND :endDate
        ORDER BY lw.date DESC
    """)
    fun getWorkoutsWithExerciseInDateRange(
        exerciseId: String,
        startDate: String, 
        endDate: String
    ): Flow<List<LoggedWorkout>>
    
    @Query("""
        SELECT activeProgramCycleId, COUNT(*) as workoutCount
        FROM logged_workout_table
        WHERE activeProgramCycleId IS NOT NULL
        GROUP BY activeProgramCycleId
    """)
    fun getCycleWorkoutCounts(): Flow<List<CycleWorkoutCount>>
    
    @Query("SELECT COUNT(*) FROM logged_workout_table")
    fun getTotalWorkoutCount(): Flow<Int>
}

data class CycleWorkoutCount(
    val activeProgramCycleId: String,
    val workoutCount: Int
)
```

---

## 5. UI Component APIs

### 5.1 Adaptive Layout APIs

#### 5.1.1 Layout Information
```kotlin
@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo

data class AdaptiveLayoutInfo(
    val screenSize: ScreenSize,
    val isLandscape: Boolean,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val useTwoColumns: Boolean,
    val useNavigationRail: Boolean,
    val useMasterDetail: Boolean,
    val contentPadding: Dp
)

enum class ScreenSize {
    COMPACT,    // < 600dp
    MEDIUM,     // 600dp - 840dp  
    EXPANDED    // > 840dp
}
```

#### 5.1.2 Master-Detail Components
```kotlin
@Composable
fun <T> MasterDetailLayout(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    masterContent: @Composable (T, Boolean) -> Unit,
    detailContent: @Composable (T?) -> Unit,
    modifier: Modifier = Modifier
)

@Composable  
fun <T> rememberMasterDetailState(
    items: List<T>,
    initialSelection: T? = null
): MasterDetailState<T>

class MasterDetailState<T> {
    var selectedItem: T? by mutableStateOf(initialSelection)
    fun selectItem(item: T)
    fun clearSelection()
}
```

### 5.2 Enhanced Workout Components

#### 5.2.1 Video Reference Integration
```kotlin
@Composable
fun VideoReferenceSelector(
    currentVideoUri: String?,
    onVideoSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun VideoThumbnail(
    videoUri: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
```

#### 5.2.2 Performance Input Components
```kotlin
@Composable
fun EnhancedSetRow(
    set: LoggedSet,
    targetSet: TemplateExerciseSet?,
    exerciseUsesBodyweight: Boolean,
    userBodyweight: Double?,
    onSetChanged: (LoggedSet) -> Unit,
    onDeleteSet: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun RestTimeTracker(
    isActive: Boolean,
    onTimeRecorded: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

---

## 6. State Management APIs

### 6.1 ViewModel State Patterns

#### 6.1.1 Reactive State Flow
```kotlin
abstract class BaseViewModel : ViewModel() {
    protected fun <T> StateFlow<T>.stateIn(
        initialValue: T
    ): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue
    )
}

class ExampleViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    val combinedState = combine(
        repository.getData(),
        _uiState
    ) { data, state ->
        ProcessedState(data, state)
    }.stateIn(ProcessedState.Empty)
}
```

#### 6.1.2 Event Handling
```kotlin
sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ShowSuccess(val message: String) : UiEvent()
}

class ViewModel {
    private val _events = Channel<UiEvent>()
    val events = _events.receiveAsFlow()
    
    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
}
```

### 6.2 Error Handling APIs

#### 6.2.1 Result Patterns
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Exception) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}
```

---

## 7. External Integration APIs

### 7.1 Google Drive Cloud Provider

#### 7.1.1 Cloud Provider Interface
```kotlin
interface CloudProvider {
    suspend fun authenticate(): CloudResult<CloudUser>
    suspend fun uploadBackup(backup: CloudBackup): CloudResult<String>
    suspend fun downloadBackup(backupId: String): CloudResult<CloudBackup>
    suspend fun listBackups(userId: String): CloudResult<List<CloudBackupItem>>
    suspend fun deleteBackup(backupId: String): CloudResult<Unit>
    suspend fun getQuota(): CloudResult<CloudQuota>
}

class GoogleDriveCloudProvider : CloudProvider {
    // Implementation details...
}
```

### 7.2 Android Photo Picker Integration

#### 7.2.1 Media Selection
```kotlin
class MediaSelectionHelper {
    fun createVideoPickerIntent(): Intent
    
    fun handleVideoSelection(
        resultCode: Int,
        data: Intent?
    ): String? // Returns content URI
    
    fun requestPersistentAccess(contentUri: String): Boolean
}
```

---

## 8. Data Validation APIs

### 8.1 Input Validation
```kotlin
object ValidationRules {
    fun validateWeight(weight: Double?): ValidationResult
    fun validateReps(reps: Int?): ValidationResult  
    fun validateDuration(seconds: Int?): ValidationResult
    fun validateExerciseName(name: String): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val sanitizedValue: Any? = null
)
```

### 8.2 Data Integrity
```kotlin
interface DataIntegrityService {
    suspend fun validateWorkout(workout: LoggedWorkout): List<IntegrityIssue>
    suspend fun repairData(): RepairResult
    suspend fun checkDatabaseConsistency(): ConsistencyReport
}

data class IntegrityIssue(
    val type: IssueType,
    val description: String,
    val severity: IssueSeverity,
    val autoFixable: Boolean
)
```

---

## 9. Performance APIs

### 9.1 Caching Strategies
```kotlin
interface CacheManager {
    suspend fun <T> getCachedResult(
        key: String,
        expirationMs: Long = 300_000, // 5 minutes
        provider: suspend () -> T
    ): T
    
    suspend fun invalidateCache(pattern: String)
    suspend fun clearAllCache()
}
```

### 9.2 Background Processing
```kotlin
interface BackgroundTaskManager {
    suspend fun schedulePerformanceAnalysis()
    suspend fun schedulePRDetection(workoutId: String)
    suspend fun scheduleCloudBackup()
}
```

---

## 10. Testing APIs

### 10.1 Test Utilities
```kotlin
object TestDataFactory {
    fun createTestExercise(): Exercise
    fun createTestWorkout(): LoggedWorkout
    fun createTestPersonalRecord(): PersonalRecord
}

class DatabaseTestRule : TestWatcher() {
    val database: WorkoutDatabase
        get() = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
```

---

## 11. Migration & Versioning APIs

### 11.1 Data Migration
```kotlin
interface MigrationStrategy {
    suspend fun migrateFrom(version: Int): MigrationResult
    suspend fun rollback(toVersion: Int): MigrationResult
}

data class MigrationResult(
    val success: Boolean,
    val fromVersion: Int,
    val toVersion: Int,
    val changes: List<String>,
    val warnings: List<String> = emptyList()
)
```

---

This comprehensive API documentation covers all major internal interfaces and contracts used throughout MyWorkoutLog. The APIs are designed for maintainability, testability, and extensibility while providing clean abstractions between architectural layers.

---

*This document serves as the definitive API reference for MyWorkoutLog's internal architecture and integration patterns.*