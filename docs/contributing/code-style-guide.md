# Code Style Guide

This document defines the coding standards and best practices for MyWorkoutLog. Following these guidelines ensures consistent, maintainable, and readable code across the entire codebase.

## Table of Contents

- [General Principles](#general-principles)
- [Kotlin Style Guidelines](#kotlin-style-guidelines)
- [Jetpack Compose Standards](#jetpack-compose-standards)
- [Architecture Patterns](#architecture-patterns)
- [File Organization](#file-organization)
- [Naming Conventions](#naming-conventions)
- [Documentation Standards](#documentation-standards)
- [Performance Guidelines](#performance-guidelines)
- [Testing Conventions](#testing-conventions)

## General Principles

### Code Philosophy
1. **Readability First**: Code is read more often than it's written
2. **Consistency**: Follow established patterns throughout the codebase
3. **Simplicity**: Choose simple solutions over complex ones
4. **Performance Awareness**: Consider performance implications of design decisions
5. **Maintainability**: Write code that can be easily modified and extended

### Quality Standards
- **No warnings**: Code should compile without warnings
- **Type safety**: Leverage Kotlin's type system for safety
- **Null safety**: Use nullable types appropriately and handle nulls safely
- **Immutability**: Prefer immutable data structures and val over var
- **Thread safety**: Ensure proper handling of concurrent access

## Kotlin Style Guidelines

### Language Features

#### Null Safety
```kotlin
// ✅ Good: Use safe calls and elvis operator
val name = user?.profile?.name ?: "Unknown"

// ✅ Good: Use let for null-safe operations
workout?.let { w ->
    processWorkout(w)
}

// ❌ Avoid: Force unwrapping unless absolutely certain
val name = user!!.profile!!.name // Dangerous
```

#### Data Classes
```kotlin
// ✅ Good: Use data classes for data containers
data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: Equipment? = null,
    val notes: String = "",
    val videoReference: String? = null
)

// ✅ Good: Use @Stable for Compose performance
@Stable
data class WorkoutUiState(
    val workout: Workout? = null,
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

#### Extension Functions
```kotlin
// ✅ Good: Meaningful extensions that add clarity
fun Double.toFormattedWeight(): String = 
    if (this % 1.0 == 0.0) "${this.toInt()}" else String.format("%.1f", this)

fun List<LoggedSet>.totalVolume(): Double = 
    sumOf { it.weight * it.reps }

// ❌ Avoid: Extensions that don't add semantic value
fun String.isNotEmpty(): Boolean = this.isNotEmpty() // Redundant
```

#### Coroutines and Flow
```kotlin
// ✅ Good: Proper coroutine handling
class WorkoutRepository(
    private val dao: WorkoutDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun getWorkoutsFlow(): Flow<List<Workout>> = dao.getAllWorkoutsFlow()
    
    suspend fun saveWorkout(workout: Workout) = withContext(ioDispatcher) {
        dao.insertWorkout(workout)
    }
}

// ✅ Good: StateFlow for UI state
class WorkoutViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()
    
    fun updateWorkout(workout: Workout) {
        viewModelScope.launch {
            _uiState.update { it.copy(workout = workout) }
        }
    }
}
```

### Code Structure

#### Function Organization
```kotlin
class ExerciseService {
    // 1. Public functions first
    fun calculateOneRepMax(weight: Double, reps: Int): Double {
        return weight / (1.0278 - (0.0278 * reps))
    }
    
    fun getIntensityLevel(oneRepMax: Double, weight: Double): IntensityLevel {
        val percentage = (weight / oneRepMax) * 100
        return classifyIntensity(percentage)
    }
    
    // 2. Private functions after public ones
    private fun classifyIntensity(percentage: Double): IntensityLevel {
        return when {
            percentage >= 90 -> IntensityLevel.HIGH
            percentage >= 70 -> IntensityLevel.MEDIUM
            else -> IntensityLevel.LOW
        }
    }
}
```

#### Property Organization
```kotlin
class WorkoutSession {
    // 1. Constructor properties
    val id: Long,
    val startTime: Instant,
    val exercises: List<Exercise>
    
    // 2. Computed properties
    val duration: Duration
        get() = endTime?.let { Duration.between(startTime, it) } ?: Duration.ZERO
    
    val totalVolume: Double
        get() = exercises.sumOf { it.totalVolume }
    
    // 3. Mutable state (minimize)
    var endTime: Instant? = null
        private set
    
    // 4. Private properties
    private val _loggedSets = mutableListOf<LoggedSet>()
}
```

## Jetpack Compose Standards

### Composable Function Organization

#### Function Structure
```kotlin
@Composable
fun WorkoutCard(
    // 1. Required parameters
    workout: Workout,
    onClick: (Workout) -> Unit,
    // 2. Optional parameters with defaults
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showAnalyticsButton: Boolean = true
) {
    // 3. Remember statements
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        label = "card_elevation"
    )
    
    // 4. Derived state
    val formattedDuration = remember(workout.duration) {
        formatDuration(workout.duration)
    }
    
    // 5. UI content
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        onClick = { onClick(workout) }
    ) {
        // Content...
    }
}
```

#### State Management
```kotlin
// ✅ Good: State hoisting
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    WorkoutContent(
        uiState = uiState,
        onStartWorkout = viewModel::startWorkout,
        onCompleteWorkout = viewModel::completeWorkout
    )
}

@Composable
private fun WorkoutContent(
    uiState: WorkoutUiState,
    onStartWorkout: () -> Unit,
    onCompleteWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // UI implementation
}

// ❌ Avoid: State inside reusable components
@Composable
fun WorkoutCard(workout: Workout) {
    var isExpanded by remember { mutableStateOf(false) } // Should be hoisted
    // ...
}
```

#### Performance Optimization
```kotlin
// ✅ Good: Stable parameters and remember for expensive operations
@Composable
fun WorkoutAnalytics(
    workouts: List<Workout>, // Should be @Stable or @Immutable
    modifier: Modifier = Modifier
) {
    val analytics = remember(workouts) {
        calculateWorkoutAnalytics(workouts) // Expensive calculation
    }
    
    val chartData = remember(analytics) {
        analytics.toChartData() // Derived expensive data
    }
    
    LazyColumn(modifier = modifier) {
        items(
            items = analytics,
            key = { it.id } // Stable keys for better performance
        ) { analytic ->
            AnalyticItem(analytic = analytic)
        }
    }
}

// ✅ Good: Use derivedStateOf for computed state
@Composable
fun ExerciseList(exercises: List<Exercise>, searchQuery: String) {
    val filteredExercises by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) exercises
            else exercises.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    LazyColumn {
        items(filteredExercises, key = { it.id }) { exercise ->
            ExerciseItem(exercise = exercise)
        }
    }
}
```

### UI Component Structure

#### Reusable Components
```kotlin
// ✅ Good: Generic, reusable component
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

#### Screen Components
```kotlin
// ✅ Good: Clear screen structure
@Composable
fun WorkoutLoggerScreen(
    onNavigateToExercises: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: WorkoutLoggerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when {
        uiState.isLoading -> WorkoutLoggerLoadingContent()
        uiState.error != null -> WorkoutLoggerErrorContent(
            error = uiState.error,
            onRetry = viewModel::retry
        )
        else -> WorkoutLoggerContent(
            uiState = uiState,
            onAddExercise = onNavigateToExercises,
            onCompleteWorkout = viewModel::completeWorkout,
            onLogSet = viewModel::logSet,
            onUpdateRestTime = viewModel::updateRestTime
        )
    }
}

@Composable
private fun WorkoutLoggerContent(
    uiState: WorkoutLoggerUiState,
    onAddExercise: () -> Unit,
    onCompleteWorkout: () -> Unit,
    onLogSet: (LoggedSet) -> Unit,
    onUpdateRestTime: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}
```

## Architecture Patterns

### MVVM Implementation

#### ViewModel Structure
```kotlin
class WorkoutLoggerViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    // 1. Private mutable state
    private val _uiState = MutableStateFlow(WorkoutLoggerUiState())
    
    // 2. Public immutable state
    val uiState: StateFlow<WorkoutLoggerUiState> = _uiState.asStateFlow()
    
    // 3. Initialization
    init {
        loadCurrentWorkout()
    }
    
    // 4. Public functions (user actions)
    fun startWorkout() {
        viewModelScope.launch {
            try {
                val workout = workoutRepository.createWorkout()
                _uiState.update { it.copy(currentWorkout = workout) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    
    fun logSet(exercise: Exercise, set: LoggedSet) {
        viewModelScope.launch {
            try {
                workoutRepository.logSet(exercise.id, set)
                updateCurrentWorkoutState()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    
    // 5. Private functions
    private fun loadCurrentWorkout() {
        viewModelScope.launch {
            workoutRepository.getCurrentWorkoutFlow()
                .catch { handleError(it) }
                .collect { workout ->
                    _uiState.update { it.copy(currentWorkout = workout) }
                }
        }
    }
    
    private fun handleError(error: Throwable) {
        _uiState.update { 
            it.copy(error = error.message ?: "Unknown error occurred") 
        }
    }
}
```

#### Repository Pattern
```kotlin
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val apiService: WorkoutApiService,
    private val preferencesManager: PreferencesManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    // 1. Data streams
    fun getAllWorkoutsFlow(): Flow<List<Workout>> = workoutDao.getAllWorkoutsFlow()
    
    fun getCurrentWorkoutFlow(): Flow<Workout?> = workoutDao.getCurrentWorkoutFlow()
    
    // 2. Suspend functions for operations
    suspend fun createWorkout(): Workout = withContext(ioDispatcher) {
        val workout = Workout(
            id = 0, // Room will generate
            startTime = Instant.now(),
            exercises = emptyList()
        )
        val id = workoutDao.insertWorkout(workout)
        workout.copy(id = id)
    }
    
    suspend fun completeWorkout(workoutId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            val workout = workoutDao.getWorkout(workoutId)
                ?: return@withContext Result.failure(IllegalArgumentException("Workout not found"))
            
            val completedWorkout = workout.copy(
                endTime = Instant.now(),
                isCompleted = true
            )
            
            workoutDao.updateWorkout(completedWorkout)
            
            // Sync to cloud if enabled
            if (preferencesManager.isCloudSyncEnabled()) {
                syncWorkoutToCloud(completedWorkout)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 3. Private helper functions
    private suspend fun syncWorkoutToCloud(workout: Workout) {
        // Cloud sync implementation
    }
}
```

### Data Layer Patterns

#### Room Database
```kotlin
// ✅ Good: Entity design
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Instant,
    val endTime: Instant? = null,
    val notes: String = "",
    val isCompleted: Boolean = false
)

// ✅ Good: DAO design with Flow
@Dao
interface WorkoutDao {
    
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun getAllWorkoutsFlow(): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE isCompleted = 0 LIMIT 1")
    fun getCurrentWorkoutFlow(): Flow<WorkoutEntity?>
    
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long
    
    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
    
    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Long): WorkoutWithExercises?
}

// ✅ Good: Type converters for complex types
class Converters {
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()
    
    @TypeConverter
    fun toInstant(timestamp: Long?): Instant? = 
        timestamp?.let { Instant.ofEpochMilli(it) }
        
    @TypeConverter
    fun fromMuscleGroupList(muscleGroups: List<MuscleGroup>): String =
        Gson().toJson(muscleGroups)
        
    @TypeConverter
    fun toMuscleGroupList(json: String): List<MuscleGroup> =
        Gson().fromJson(json, object : TypeToken<List<MuscleGroup>>() {}.type)
}
```

## File Organization

### Package Structure
```
src/main/java/com/example/myworkoutlog/
├── data/
│   ├── database/
│   │   ├── dao/           # Room DAOs
│   │   ├── entities/      # Database entities
│   │   └── converters/    # Type converters
│   ├── repository/        # Repository implementations
│   ├── api/              # API service interfaces
│   └── preferences/       # SharedPreferences/DataStore
├── domain/
│   ├── models/           # Domain models
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Business logic use cases
├── ui/
│   ├── screens/          # Screen composables
│   ├── components/       # Reusable UI components
│   ├── theme/           # Material 3 theme
│   ├── navigation/      # Navigation setup
│   └── utils/           # UI utilities
├── utils/
│   ├── extensions/      # Extension functions
│   ├── formatters/      # Data formatters
│   └── constants/       # App constants
└── MainActivity.kt
```

### File Naming Conventions
```
WorkoutLoggerScreen.kt          # Screen composables
WorkoutLoggerViewModel.kt       # ViewModels
WorkoutRepository.kt            # Repositories
WorkoutDao.kt                  # Room DAOs
WorkoutEntity.kt               # Database entities
Workout.kt                     # Domain models
WorkoutApiService.kt           # API services
WorkoutCard.kt                 # Reusable components
Extensions.kt                  # Extension functions
Constants.kt                   # Constants
```

## Naming Conventions

### Classes and Interfaces
```kotlin
// ✅ Classes: PascalCase
class WorkoutRepository
class ExerciseService
class OneRepMaxCalculator

// ✅ Interfaces: Adjective or noun
interface Cacheable
interface WorkoutDataSource
interface AnalyticsTracker

// ✅ Data classes: Descriptive nouns
data class WorkoutSession
data class ExerciseStats
data class UserPreferences

// ✅ Sealed classes: Descriptive hierarchy
sealed class WorkoutState {
    object Loading : WorkoutState()
    data class Success(val workout: Workout) : WorkoutState()
    data class Error(val message: String) : WorkoutState()
}
```

### Functions and Variables
```kotlin
// ✅ Functions: Verbs describing action
fun calculateOneRepMax(weight: Double, reps: Int): Double
fun formatDuration(duration: Duration): String
fun validateWorkoutData(workout: Workout): ValidationResult

// ✅ Boolean functions: is/has/can/should prefix
fun isWorkoutCompleted(workout: Workout): Boolean
fun hasVideoReference(exercise: Exercise): Boolean
fun canPerformExercise(exercise: Exercise, equipment: List<Equipment>): Boolean

// ✅ Variables: Descriptive nouns
val totalWorkoutTime: Duration
val completedSets: List<LoggedSet>
val userPreferences: UserPreferences
var currentRestTime: Int

// ✅ Collections: Plural nouns
val exercises: List<Exercise>
val workoutSessions: List<WorkoutSession>
val muscleGroups: Set<MuscleGroup>
```

### Constants
```kotlin
object WorkoutConstants {
    const val DEFAULT_REST_TIME_SECONDS = 120
    const val MAX_SETS_PER_EXERCISE = 10
    const val MIN_WEIGHT_KG = 0.5
    const val MAX_WEIGHT_KG = 500.0
    
    const val DATABASE_NAME = "workout_database"
    const val DATABASE_VERSION = 1
    
    const val PREF_KEY_REST_TIME = "default_rest_time"
    const val PREF_KEY_WEIGHT_UNIT = "weight_unit"
}

// ✅ Enums: Descriptive values
enum class WeightUnit(val displayName: String, val abbreviation: String) {
    KILOGRAMS("Kilograms", "kg"),
    POUNDS("Pounds", "lb")
}

enum class IntensityLevel(val displayName: String, val colorId: Int) {
    LOW("Low", R.color.intensity_low),
    MEDIUM("Medium", R.color.intensity_medium),
    HIGH("High", R.color.intensity_high)
}
```

### Compose Naming
```kotlin
// ✅ Composable functions: PascalCase
@Composable
fun WorkoutLoggerScreen()

@Composable
fun ExerciseCard()

@Composable
fun RestTimerDialog()

// ✅ State variables: descriptive with 'State' suffix if needed
@Composable
fun WorkoutScreen() {
    var isLoading by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var restTimeDialogVisible by remember { mutableStateOf(false) }
    
    val workouts by viewModel.workouts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
}
```

## Documentation Standards

### KDoc Documentation
```kotlin
/**
 * Calculates the estimated one-repetition maximum (1RM) for a given weight and repetitions.
 * 
 * This function uses the Brzycki formula, which is most accurate for repetitions
 * in the 1-12 range. For higher repetitions, the accuracy decreases significantly.
 * 
 * Formula: weight / (1.0278 - (0.0278 × reps))
 * 
 * @param weight The weight lifted in the desired unit (kg or lbs)
 * @param reps The number of repetitions performed (recommended: 1-12)
 * @return The estimated one-repetition maximum in the same weight unit
 * @throws IllegalArgumentException if reps is less than 1 or greater than 50
 * 
 * @sample
 * val oneRM = calculateOneRepMax(weight = 100.0, reps = 5)
 * // Returns approximately 112.5
 * 
 * @see [Brzycki Formula](https://en.wikipedia.org/wiki/One-repetition_maximum)
 */
fun calculateOneRepMax(weight: Double, reps: Int): Double {
    require(reps in 1..50) { 
        "Repetitions must be between 1 and 50, got: $reps" 
    }
    
    return weight / (1.0278 - (0.0278 * reps))
}
```

### Inline Comments
```kotlin
class WorkoutAnalyzer {
    
    fun analyzeWorkoutTrend(workouts: List<Workout>): WorkoutTrend {
        // Group workouts by week to identify weekly patterns
        val weeklyWorkouts = workouts.groupBy { workout ->
            val instant = workout.startTime
            val week = instant.atZone(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            week.toLocalDate()
        }
        
        // Calculate volume progression over time
        val volumeProgression = weeklyWorkouts.map { (week, weekWorkouts) ->
            val totalVolume = weekWorkouts.sumOf { it.totalVolume }
            VolumeDataPoint(week, totalVolume)
        }.sortedBy { it.week }
        
        // Determine trend direction using linear regression
        val trend = when {
            volumeProgression.size < 2 -> TrendDirection.INSUFFICIENT_DATA
            calculateSlope(volumeProgression) > 0.1 -> TrendDirection.INCREASING
            calculateSlope(volumeProgression) < -0.1 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        return WorkoutTrend(
            direction = trend,
            volumeProgression = volumeProgression,
            confidenceLevel = calculateConfidenceLevel(volumeProgression)
        )
    }
}
```

## Performance Guidelines

### Memory Management
```kotlin
// ✅ Good: Use appropriate collection types
class ExerciseCache {
    // Use LinkedHashMap for ordered caching with O(1) access
    private val exerciseCache = LinkedHashMap<Long, Exercise>(
        /* initialCapacity */ 16,
        /* loadFactor */ 0.75f,
        /* accessOrder */ true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Exercise>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    
    companion object {
        private const val MAX_CACHE_SIZE = 100
    }
}

// ✅ Good: Use sequences for large data processing
fun calculateMuscleGroupStatistics(workouts: List<Workout>): Map<MuscleGroup, Int> {
    return workouts.asSequence()
        .flatMap { it.exercises }
        .flatMap { it.muscleGroups }
        .groupingBy { it }
        .eachCount()
}
```

### Compose Performance
```kotlin
// ✅ Good: Use stable keys and avoid recomposition
@Composable
fun ExerciseList(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(
            items = exercises,
            key = { exercise -> exercise.id } // Stable key prevents unnecessary recomposition
        ) { exercise ->
            ExerciseItem(
                exercise = exercise,
                onClick = { onExerciseClick(exercise) }
            )
        }
    }
}

// ✅ Good: Minimize lambda recreations
@Composable
fun WorkoutCard(
    workout: Workout,
    onWorkoutClick: (Long) -> Unit, // Pass ID instead of full object
    modifier: Modifier = Modifier
) {
    val onClick = remember(workout.id) {
        { onWorkoutClick(workout.id) }
    }
    
    Card(
        modifier = modifier.clickable { onClick() }
    ) {
        // Content
    }
}
```

## Testing Conventions

### Test Organization
```kotlin
class OneRepMaxCalculatorTest {
    
    // 1. Test class setup
    private lateinit var calculator: OneRepMaxCalculator
    
    @Before
    fun setup() {
        calculator = OneRepMaxCalculator()
    }
    
    // 2. Happy path tests
    @Test
    fun `calculateOneRepMax returns correct value for typical input`() {
        // Arrange
        val weight = 100.0
        val reps = 5
        
        // Act
        val result = calculator.calculateOneRepMax(weight, reps)
        
        // Assert
        assertEquals(112.5, result, 0.1)
    }
    
    @Test
    fun `calculateOneRepMax handles single rep correctly`() {
        // Arrange
        val weight = 100.0
        val reps = 1
        
        // Act
        val result = calculator.calculateOneRepMax(weight, reps)
        
        // Assert
        assertEquals(100.0, result, 0.1)
    }
    
    // 3. Edge cases
    @Test
    fun `calculateOneRepMax throws exception for invalid reps`() {
        // Arrange
        val weight = 100.0
        val invalidReps = 0
        
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            calculator.calculateOneRepMax(weight, invalidReps)
        }
    }
    
    // 4. Boundary tests
    @Test
    fun `calculateOneRepMax handles maximum reps boundary`() {
        // Arrange
        val weight = 100.0
        val maxReps = 50
        
        // Act
        val result = calculator.calculateOneRepMax(weight, maxReps)
        
        // Assert
        assertTrue("Result should be positive", result > 0)
        assertTrue("Result should be reasonable", result < weight * 5)
    }
}
```

### Test Naming
```kotlin
// ✅ Good: Descriptive test names using backticks
@Test
fun `saveWorkout persists workout to database successfully`()

@Test
fun `saveWorkout throws exception when workout is invalid`()

@Test
fun `calculateVolume returns zero for empty exercise list`()

@Test
fun `getPersonalRecord returns highest weight for given exercise`()

// ✅ Good: Given-When-Then structure in names
@Test
fun `given valid workout data when saving workout then returns success result`()

@Test
fun `given invalid exercise when calculating volume then throws IllegalArgumentException`()
```

### Mock Usage
```kotlin
class WorkoutRepositoryTest {
    
    @Mock
    private lateinit var workoutDao: WorkoutDao
    
    @Mock
    private lateinit var apiService: WorkoutApiService
    
    private lateinit var repository: WorkoutRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = WorkoutRepository(workoutDao, apiService)
    }
    
    @Test
    fun `saveWorkout calls dao insert with correct workout`() = runTest {
        // Arrange
        val workout = createTestWorkout()
        whenever(workoutDao.insertWorkout(workout)).thenReturn(1L)
        
        // Act
        val result = repository.saveWorkout(workout)
        
        // Assert
        verify(workoutDao).insertWorkout(workout)
        assertEquals(1L, result.workoutId)
    }
}
```

---

Following these style guidelines ensures that MyWorkoutLog maintains a consistent, readable, and maintainable codebase. When in doubt, prioritize clarity and consistency with existing code patterns.