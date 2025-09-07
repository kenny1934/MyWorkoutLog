# Testing Standards

This document outlines the testing strategy, standards, and requirements for MyWorkoutLog. Following these guidelines ensures comprehensive test coverage, reliable code quality, and maintainable test suites.

## Table of Contents

- [Testing Philosophy](#testing-philosophy)
- [Testing Pyramid](#testing-pyramid)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [UI Testing](#ui-testing)
- [Test Coverage Requirements](#test-coverage-requirements)
- [Testing Tools and Frameworks](#testing-tools-and-frameworks)
- [Test Organization](#test-organization)
- [Mocking Strategy](#mocking-strategy)
- [Performance Testing](#performance-testing)
- [Quality Gates](#quality-gates)

## Testing Philosophy

### Core Principles

1. **Test Behavior, Not Implementation**: Tests should verify what the code does, not how it does it
2. **Fast Feedback**: Tests should run quickly to enable rapid development cycles
3. **Reliable and Deterministic**: Tests should produce consistent results across different environments
4. **Maintainable**: Tests should be easy to understand, modify, and extend
5. **Comprehensive**: Critical paths and edge cases should have thorough coverage

### Testing Mindset

- **Test-Driven Development (TDD)**: Write tests before implementation for complex business logic
- **Fail Fast**: Tests should fail quickly and clearly when something is wrong
- **Documentation**: Tests serve as living documentation of system behavior
- **Confidence**: Tests should give developers confidence to refactor and add features

## Testing Pyramid

We follow the standard testing pyramid with the following distribution:

```
        /\
       /  \
      / UI \     10% - E2E and UI Tests
     /______\
    /        \
   /Integration\ 20% - Integration Tests
  /______________\
 /                \
/   Unit Tests     \ 70% - Unit Tests
\__________________/
```

### Unit Tests (70%)
- **Purpose**: Test individual components, functions, and classes in isolation
- **Scope**: ViewModels, Repositories, Utilities, Business Logic
- **Speed**: Very fast (< 1 second per test)
- **Dependencies**: Mocked or stubbed

### Integration Tests (20%)
- **Purpose**: Test interactions between components
- **Scope**: Database operations, API interactions, Repository + DAO combinations
- **Speed**: Fast (< 5 seconds per test)
- **Dependencies**: Real databases (in-memory), fake services

### UI Tests (10%)
- **Purpose**: Test user interactions and complete user journeys
- **Scope**: Critical user paths, complex UI interactions
- **Speed**: Slower (< 30 seconds per test)
- **Dependencies**: Real UI components, may use test doubles for data

## Unit Testing

### Scope and Coverage

#### What to Test
- **ViewModels**: State management, user actions, data transformations
- **Repositories**: Data operations, caching logic, error handling
- **Business Logic**: Calculations, validations, algorithms
- **Utilities**: Formatters, extensions, helper functions
- **Domain Models**: Data transformations, validation rules

#### What Not to Test
- **Framework Code**: Android framework, Compose framework
- **Third-party Libraries**: Room, Retrofit, etc.
- **Simple Getters/Setters**: Unless they contain logic
- **Trivial Code**: One-line functions without logic

### ViewModel Testing

```kotlin
class WorkoutLoggerViewModelTest {
    
    @Mock
    private lateinit var workoutRepository: WorkoutRepository
    
    @Mock
    private lateinit var exerciseRepository: ExerciseRepository
    
    private lateinit var viewModel: WorkoutLoggerViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = WorkoutLoggerViewModel(workoutRepository, exerciseRepository)
    }
    
    @Test
    fun `startWorkout creates new workout and updates state`() = runTest {
        // Arrange
        val expectedWorkout = createTestWorkout()
        whenever(workoutRepository.createWorkout()).thenReturn(expectedWorkout)
        
        // Act
        viewModel.startWorkout()
        
        // Assert
        val currentState = viewModel.uiState.value
        assertEquals(expectedWorkout, currentState.currentWorkout)
        assertFalse(currentState.isLoading)
        assertNull(currentState.error)
    }
    
    @Test
    fun `startWorkout handles repository error gracefully`() = runTest {
        // Arrange
        val errorMessage = "Failed to create workout"
        whenever(workoutRepository.createWorkout())
            .thenThrow(RuntimeException(errorMessage))
        
        // Act
        viewModel.startWorkout()
        
        // Assert
        val currentState = viewModel.uiState.value
        assertNull(currentState.currentWorkout)
        assertEquals(errorMessage, currentState.error)
        assertFalse(currentState.isLoading)
    }
    
    @Test
    fun `logSet updates workout with new set`() = runTest {
        // Arrange
        val exercise = createTestExercise()
        val loggedSet = createTestLoggedSet()
        val workout = createTestWorkout()
        
        whenever(workoutRepository.getCurrentWorkout()).thenReturn(workout)
        whenever(workoutRepository.logSet(exercise.id, loggedSet))
            .thenReturn(Result.success(Unit))
        
        // Act
        viewModel.logSet(exercise, loggedSet)
        
        // Assert
        verify(workoutRepository).logSet(exercise.id, loggedSet)
        // Verify state updates appropriately
    }
}
```

### Repository Testing

```kotlin
class WorkoutRepositoryTest {
    
    @Mock
    private lateinit var workoutDao: WorkoutDao
    
    @Mock
    private lateinit var apiService: WorkoutApiService
    
    private lateinit var repository: WorkoutRepository
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = WorkoutRepository(workoutDao, apiService, testDispatcher)
    }
    
    @Test
    fun `getAllWorkouts returns mapped domain models`() = runTest {
        // Arrange
        val workoutEntities = listOf(createTestWorkoutEntity())
        val expectedWorkouts = listOf(createTestWorkout())
        whenever(workoutDao.getAllWorkouts()).thenReturn(workoutEntities)
        
        // Act
        val result = repository.getAllWorkouts()
        
        // Assert
        assertEquals(expectedWorkouts, result)
    }
    
    @Test
    fun `saveWorkout persists to local database`() = runTest {
        // Arrange
        val workout = createTestWorkout()
        val expectedEntity = workout.toEntity()
        whenever(workoutDao.insertWorkout(expectedEntity)).thenReturn(1L)
        
        // Act
        val result = repository.saveWorkout(workout)
        
        // Assert
        verify(workoutDao).insertWorkout(expectedEntity)
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `saveWorkout syncs to cloud when enabled`() = runTest {
        // Arrange
        val workout = createTestWorkout()
        whenever(workoutDao.insertWorkout(any())).thenReturn(1L)
        whenever(preferencesManager.isCloudSyncEnabled()).thenReturn(true)
        
        // Act
        repository.saveWorkout(workout)
        
        // Assert
        verify(apiService).syncWorkout(workout)
    }
}
```

### Business Logic Testing

```kotlin
class OneRepMaxCalculatorTest {
    
    private val calculator = OneRepMaxCalculator()
    
    @Test
    fun `calculateOneRepMax returns correct value for typical inputs`() {
        // Test multiple scenarios with known results
        val testCases = listOf(
            Triple(100.0, 5, 112.5),
            Triple(80.0, 8, 100.0),
            Triple(120.0, 3, 127.8),
            Triple(90.0, 1, 90.0)
        )
        
        testCases.forEach { (weight, reps, expected) ->
            val result = calculator.calculateOneRepMax(weight, reps)
            assertEquals(
                "Failed for weight=$weight, reps=$reps",
                expected,
                result,
                0.1
            )
        }
    }
    
    @Test
    fun `calculateOneRepMax throws exception for invalid input`() {
        assertThrows<IllegalArgumentException> {
            calculator.calculateOneRepMax(100.0, 0)
        }
        
        assertThrows<IllegalArgumentException> {
            calculator.calculateOneRepMax(100.0, -1)
        }
        
        assertThrows<IllegalArgumentException> {
            calculator.calculateOneRepMax(-10.0, 5)
        }
    }
    
    @Test
    fun `calculateOneRepMax handles boundary values`() {
        // Test minimum and maximum acceptable values
        val minResult = calculator.calculateOneRepMax(0.5, 1)
        assertEquals(0.5, minResult, 0.01)
        
        val maxResult = calculator.calculateOneRepMax(500.0, 50)
        assertTrue("Max result should be positive", maxResult > 0)
        assertTrue("Max result should be reasonable", maxResult < 2000.0)
    }
}
```

## Integration Testing

### Database Integration

```kotlin
@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {
    
    private lateinit var database: WorkoutDatabase
    private lateinit var workoutDao: WorkoutDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, WorkoutDatabase::class.java
        ).build()
        workoutDao = database.workoutDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun insertWorkoutAndGetById() = runTest {
        // Arrange
        val workout = createTestWorkoutEntity()
        
        // Act
        val insertedId = workoutDao.insertWorkout(workout)
        val retrieved = workoutDao.getWorkout(insertedId)
        
        // Assert
        assertNotNull(retrieved)
        assertEquals(workout.copy(id = insertedId), retrieved)
    }
    
    @Test
    fun getWorkoutsOrderedByDateDescending() = runTest {
        // Arrange
        val now = Instant.now()
        val workout1 = createTestWorkoutEntity(startTime = now.minusSeconds(3600))
        val workout2 = createTestWorkoutEntity(startTime = now.minusSeconds(1800))
        val workout3 = createTestWorkoutEntity(startTime = now)
        
        // Act
        workoutDao.insertWorkout(workout1)
        workoutDao.insertWorkout(workout2)
        workoutDao.insertWorkout(workout3)
        
        val workouts = workoutDao.getAllWorkouts()
        
        // Assert
        assertEquals(3, workouts.size)
        assertTrue("Workouts should be ordered by date descending",
            workouts[0].startTime.isAfter(workouts[1].startTime))
        assertTrue("Workouts should be ordered by date descending",
            workouts[1].startTime.isAfter(workouts[2].startTime))
    }
    
    @Test
    fun workoutFlowEmitsUpdatesWhenDataChanges() = runTest {
        // Arrange
        val workout = createTestWorkoutEntity()
        val workoutsFlow = workoutDao.getAllWorkoutsFlow()
        
        // Act & Assert
        workoutsFlow.test {
            // Initial state should be empty
            assertEquals(emptyList<WorkoutEntity>(), awaitItem())
            
            // Insert workout
            workoutDao.insertWorkout(workout)
            val afterInsert = awaitItem()
            assertEquals(1, afterInsert.size)
            
            // Update workout
            val updatedWorkout = afterInsert[0].copy(notes = "Updated notes")
            workoutDao.updateWorkout(updatedWorkout)
            val afterUpdate = awaitItem()
            assertEquals("Updated notes", afterUpdate[0].notes)
        }
    }
}
```

### Repository Integration Testing

```kotlin
class WorkoutRepositoryIntegrationTest {
    
    private lateinit var database: WorkoutDatabase
    private lateinit var repository: WorkoutRepository
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, WorkoutDatabase::class.java
        ).build()
        
        val fakeApiService = FakeWorkoutApiService()
        repository = WorkoutRepository(
            workoutDao = database.workoutDao(),
            apiService = fakeApiService
        )
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `saveWorkout persists data and emits through flow`() = runTest {
        // Arrange
        val workout = createTestWorkout()
        
        // Act
        repository.saveWorkout(workout)
        
        // Assert
        repository.getAllWorkoutsFlow().test {
            val workouts = awaitItem()
            assertEquals(1, workouts.size)
            assertEquals(workout.copy(id = workouts[0].id), workouts[0])
        }
    }
}
```

## UI Testing

### Compose Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class WorkoutLoggerScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockViewModel: WorkoutLoggerViewModel
    
    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
    }
    
    @Test
    fun workoutLoggerDisplaysEmptyStateWhenNoWorkout() {
        // Arrange
        every { mockViewModel.uiState } returns MutableStateFlow(
            WorkoutLoggerUiState(currentWorkout = null)
        )
        
        // Act
        composeTestRule.setContent {
            WorkoutLoggerScreen(
                viewModel = mockViewModel,
                onNavigateToExercises = {}
            )
        }
        
        // Assert
        composeTestRule
            .onNodeWithText("Start your first workout")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Add Exercise")
            .assertIsDisplayed()
    }
    
    @Test
    fun workoutLoggerDisplaysCurrentWorkoutWhenActive() {
        // Arrange
        val activeWorkout = createTestWorkout()
        every { mockViewModel.uiState } returns MutableStateFlow(
            WorkoutLoggerUiState(currentWorkout = activeWorkout)
        )
        
        // Act
        composeTestRule.setContent {
            WorkoutLoggerScreen(
                viewModel = mockViewModel,
                onNavigateToExercises = {}
            )
        }
        
        // Assert
        composeTestRule
            .onNodeWithText("Current Workout")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Complete Workout")
            .assertIsDisplayed()
    }
    
    @Test
    fun addExerciseButtonTriggersNavigation() {
        // Arrange
        var navigationTriggered = false
        val onNavigateToExercises = { navigationTriggered = true }
        
        every { mockViewModel.uiState } returns MutableStateFlow(
            WorkoutLoggerUiState()
        )
        
        // Act
        composeTestRule.setContent {
            WorkoutLoggerScreen(
                viewModel = mockViewModel,
                onNavigateToExercises = onNavigateToExercises
            )
        }
        
        composeTestRule
            .onNodeWithText("Add Exercise")
            .performClick()
        
        // Assert
        assertTrue("Navigation should be triggered", navigationTriggered)
    }
    
    @Test
    fun restTimerDisplaysCorrectTime() {
        // Arrange
        val workout = createTestWorkoutWithRestTimer(remainingTime = 90)
        every { mockViewModel.uiState } returns MutableStateFlow(
            WorkoutLoggerUiState(currentWorkout = workout)
        )
        
        // Act
        composeTestRule.setContent {
            WorkoutLoggerScreen(
                viewModel = mockViewModel,
                onNavigateToExercises = {}
            )
        }
        
        // Assert
        composeTestRule
            .onNodeWithText("1:30")
            .assertIsDisplayed()
    }
}
```

### End-to-End Testing

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class WorkoutFlowE2ETest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun completeWorkoutFlow() {
        // Start a new workout
        onView(withText("Start Workout"))
            .perform(click())
        
        // Add an exercise
        onView(withText("Add Exercise"))
            .perform(click())
        
        // Select an exercise from the list
        onView(withText("Bench Press"))
            .perform(click())
        
        // Add a set
        onView(withId(R.id.add_set_button))
            .perform(click())
        
        // Enter weight and reps
        onView(withId(R.id.weight_input))
            .perform(typeText("100"))
        
        onView(withId(R.id.reps_input))
            .perform(typeText("10"))
        
        // Log the set
        onView(withId(R.id.log_set_button))
            .perform(click())
        
        // Verify set is logged
        onView(withText("100 kg × 10"))
            .check(matches(isDisplayed()))
        
        // Complete workout
        onView(withText("Complete Workout"))
            .perform(click())
        
        // Confirm completion
        onView(withText("Complete"))
            .perform(click())
        
        // Verify navigation to workout summary
        onView(withText("Workout Complete"))
            .check(matches(isDisplayed()))
    }
}
```

## Test Coverage Requirements

### Coverage Targets

- **Overall Coverage**: Minimum 80%
- **Critical Path Coverage**: Minimum 95%
  - Workout logging and persistence
  - Exercise data management
  - Personal records calculation
  - Data export/import
- **New Features**: Minimum 85%
- **Bug Fixes**: 100% coverage of fix and related scenarios

### Critical Paths
1. **Workout Logging Flow**
   - Starting a workout
   - Adding exercises
   - Logging sets
   - Completing workout
   - Data persistence

2. **Data Management**
   - Exercise CRUD operations
   - Workout history
   - Personal records calculation
   - Analytics generation

3. **User Preferences**
   - Settings persistence
   - Theme application
   - Unit conversions

### Coverage Measurement

```bash
# Generate coverage report
./gradlew jacocoTestReport

# View HTML report
open app/build/reports/jacoco/test/html/index.html
```

### Exclusions
The following are excluded from coverage requirements:
- Generated code (Room, data classes)
- Android framework interfaces
- UI preview functions (@Preview)
- Constants and enums without logic
- Third-party library wrappers

## Testing Tools and Frameworks

### Core Testing Libraries

```kotlin
// build.gradle.kts (app module)
dependencies {
    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("app.cash.turbine:turbine:0.12.1") // Flow testing
    
    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    
    // Compose Testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
    
    // Room Testing
    testImplementation("androidx.room:room-testing:2.4.3")
    
    // MockK for Kotlin
    testImplementation("io.mockk:mockk:1.13.4")
    androidTestImplementation("io.mockk:mockk-android:1.13.4")
    
    // Architecture Testing
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // Test Coverage
    testImplementation("org.jacoco:org.jacoco.core:0.8.8")
}
```

### Test Utilities

```kotlin
// TestUtils.kt
object TestUtils {
    
    fun createTestWorkout(
        id: Long = 1L,
        exercises: List<Exercise> = emptyList(),
        startTime: Instant = Instant.now(),
        isCompleted: Boolean = false
    ): Workout {
        return Workout(
            id = id,
            exercises = exercises,
            startTime = startTime,
            endTime = if (isCompleted) startTime.plusSeconds(3600) else null,
            notes = "Test workout",
            isCompleted = isCompleted
        )
    }
    
    fun createTestExercise(
        id: Long = 1L,
        name: String = "Test Exercise",
        muscleGroups: List<MuscleGroup> = listOf(MuscleGroup.CHEST)
    ): Exercise {
        return Exercise(
            id = id,
            name = name,
            muscleGroups = muscleGroups,
            equipment = Equipment.BARBELL,
            notes = "Test exercise notes"
        )
    }
    
    fun createTestLoggedSet(
        weight: Double = 100.0,
        reps: Int = 10,
        restTime: Int = 120
    ): LoggedSet {
        return LoggedSet(
            weight = weight,
            reps = reps,
            actualRestTime = restTime,
            timestamp = Instant.now()
        )
    }
}

// TestDispatchers.kt
object TestDispatchers {
    val testDispatcher = StandardTestDispatcher()
    
    fun setTestDispatchers() {
        Dispatchers.setMain(testDispatcher)
    }
    
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}

// FlowTestUtils.kt
fun <T> Flow<T>.test(
    timeout: Duration = 5.seconds,
    testBody: suspend ReceiveTurbine<T>.() -> Unit
) {
    runTest(timeout = timeout) {
        turbineScope {
            testBody(this@test.testIn(this))
        }
    }
}
```

## Test Organization

### Directory Structure
```
src/test/java/com/example/myworkoutlog/
├── data/
│   ├── repository/        # Repository unit tests
│   └── database/         # Database unit tests
├── domain/
│   ├── models/           # Domain model tests
│   └── usecase/          # Use case tests
├── ui/
│   ├── viewmodel/        # ViewModel tests
│   └── utils/            # UI utility tests
├── utils/                # General utility tests
└── testutils/           # Test helper classes

src/androidTest/java/com/example/myworkoutlog/
├── ui/
│   ├── screens/          # Screen UI tests
│   └── components/       # Component UI tests
├── data/
│   └── database/         # Database integration tests
└── e2e/                 # End-to-end tests
```

### Test Class Naming
```kotlin
// Unit tests
WorkoutRepositoryTest.kt
OneRepMaxCalculatorTest.kt
WorkoutLoggerViewModelTest.kt

// Integration tests
WorkoutDaoIntegrationTest.kt
WorkoutRepositoryIntegrationTest.kt

// UI tests
WorkoutLoggerScreenTest.kt
ExerciseCardTest.kt

// E2E tests
WorkoutFlowE2ETest.kt
AnalyticsE2ETest.kt
```

## Mocking Strategy

### When to Mock
- **External dependencies**: APIs, databases, file systems
- **Complex collaborators**: Other repositories, services
- **Non-deterministic behavior**: Time, random number generation
- **Slow operations**: Network calls, large computations

### When Not to Mock
- **Value objects**: Data classes, enums
- **Simple utilities**: Extensions, formatters
- **Framework classes**: Usually use fakes instead
- **Classes under test**: Never mock the subject

### Mocking Examples

```kotlin
// ✅ Good: Mock external dependencies
@Mock
private lateinit var workoutDao: WorkoutDao

@Mock
private lateinit var apiService: WorkoutApiService

// ✅ Good: Mock time for deterministic tests
@Mock
private lateinit var timeProvider: TimeProvider

@Test
fun `workout duration calculated correctly`() {
    // Arrange
    val startTime = Instant.parse("2023-01-01T10:00:00Z")
    val endTime = Instant.parse("2023-01-01T11:30:00Z")
    
    whenever(timeProvider.now())
        .thenReturn(startTime)
        .thenReturn(endTime)
    
    // Act & Assert
    val workout = Workout(startTime = timeProvider.now())
    workout.complete(timeProvider.now())
    
    assertEquals(Duration.ofMinutes(90), workout.duration)
}

// ✅ Good: Use fakes for complex state
class FakeWorkoutRepository : WorkoutRepository {
    private val workouts = mutableListOf<Workout>()
    
    override suspend fun saveWorkout(workout: Workout) {
        workouts.add(workout)
    }
    
    override fun getAllWorkoutsFlow(): Flow<List<Workout>> {
        return flowOf(workouts.toList())
    }
}
```

## Performance Testing

### Load Testing
```kotlin
class WorkoutRepositoryPerformanceTest {
    
    @Test
    fun `large workout list retrieval completes within time limit`() = runTest {
        // Arrange
        val largeWorkoutList = (1..1000).map { createTestWorkout(id = it.toLong()) }
        repository.saveAllWorkouts(largeWorkoutList)
        
        // Act
        val startTime = System.currentTimeMillis()
        val result = repository.getAllWorkouts()
        val endTime = System.currentTimeMillis()
        
        // Assert
        assertEquals(1000, result.size)
        assertTrue("Query should complete within 500ms", 
            (endTime - startTime) < 500)
    }
    
    @Test
    fun `personal record calculation scales with exercise count`() = runTest {
        // Arrange
        val exercises = (1..100).map { createTestExercise(id = it.toLong()) }
        val workouts = exercises.map { exercise ->
            createTestWorkout(exercises = listOf(exercise))
        }
        
        // Act
        val startTime = System.currentTimeMillis()
        val personalRecords = calculator.calculatePersonalRecords(workouts)
        val endTime = System.currentTimeMillis()
        
        // Assert
        assertEquals(100, personalRecords.size)
        assertTrue("Calculation should complete within 1 second",
            (endTime - startTime) < 1000)
    }
}
```

### Memory Testing
```kotlin
@Test
fun `workout list does not leak memory on repeated access`() = runTest {
    val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    
    repeat(100) {
        val workouts = repository.getAllWorkouts()
        workouts.forEach { workout ->
            // Simulate processing
            workout.totalVolume
        }
    }
    
    System.gc() // Force garbage collection
    
    val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    val memoryIncrease = finalMemory - initialMemory
    
    assertTrue("Memory increase should be minimal",
        memoryIncrease < 10_000_000) // 10MB threshold
}
```

## Quality Gates

### Pre-commit Checks
```bash
#!/bin/bash
# pre-commit.sh

echo "Running pre-commit checks..."

# Run unit tests
echo "Running unit tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "Unit tests failed. Commit rejected."
    exit 1
fi

# Run lint checks
echo "Running lint checks..."
./gradlew lint
if [ $? -ne 0 ]; then
    echo "Lint checks failed. Commit rejected."
    exit 1
fi

# Check test coverage
echo "Checking test coverage..."
./gradlew jacocoTestReport
COVERAGE=$(grep -oP 'Total.*?\K\d+(?=%)' app/build/reports/jacoco/test/html/index.html)
if [ "$COVERAGE" -lt 80 ]; then
    echo "Test coverage is below 80% ($COVERAGE%). Commit rejected."
    exit 1
fi

echo "All checks passed. Commit approved."
```

### CI/CD Pipeline Checks

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run unit tests
      run: ./gradlew test
      
    - name: Run integration tests
      run: ./gradlew testDebugUnitTest
      
    - name: Generate test coverage report
      run: ./gradlew jacocoTestReport
      
    - name: Check coverage threshold
      run: |
        COVERAGE=$(grep -oP 'Total.*?\K\d+(?=%)' app/build/reports/jacoco/test/html/index.html)
        if [ "$COVERAGE" -lt 80 ]; then
          echo "Coverage $COVERAGE% is below threshold"
          exit 1
        fi
        
    - name: Upload coverage reports
      uses: codecov/codecov-action@v3
      with:
        file: app/build/reports/jacoco/test/jacocoTestReport.xml
```

### Release Quality Gates

Before any release:
1. **All tests pass**: Unit, integration, and UI tests
2. **Coverage threshold met**: Minimum 80% overall
3. **No high-severity issues**: Static analysis passes
4. **Performance benchmarks**: No regression in key metrics
5. **Manual testing**: Critical paths verified on target devices

---

Following these testing standards ensures MyWorkoutLog maintains high code quality, reliability, and user confidence. When in doubt, err on the side of more comprehensive testing rather than less.