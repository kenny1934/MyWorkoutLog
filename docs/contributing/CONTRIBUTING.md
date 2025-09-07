# Contributing to MyWorkoutLog

Thank you for your interest in contributing to MyWorkoutLog! This document provides guidelines and instructions for contributing to our fitness tracking application.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Documentation](#documentation)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct:

- **Be respectful**: Treat all contributors with respect and professionalism
- **Be inclusive**: Welcome contributors of all backgrounds and experience levels
- **Be constructive**: Provide helpful feedback and suggestions
- **Be patient**: Remember that everyone is learning and growing
- **Be professional**: Maintain a professional tone in all communications

## Getting Started

### Prerequisites

- **Android Studio**: Latest stable version (Hedgehog 2023.1.1 or newer)
- **JDK**: Java 11 or higher
- **Git**: For version control
- **Android SDK**: API level 26+ (minSdk), API level 35 (compileSdk/targetSdk)
- **Device/Emulator**: Android 8.0+ for testing

### Development Environment Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/MyWorkoutLog.git
   cd MyWorkoutLog
   ```

2. **Open in Android Studio**:
   - Import the project
   - Wait for Gradle sync to complete
   - Ensure all dependencies are downloaded

3. **Build the project**:
   ```bash
   ./gradlew build
   ```

4. **Run tests**:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

### Project Structure Familiarization

Review the following documentation to understand the codebase:
- [Architecture Overview](../architecture/overview.md)
- [API Documentation](../technical/api-documentation.md)
- [Database Schema](../technical/database-schema.md)
- [Code Style Guide](code-style-guide.md)

## Development Workflow

### Git Workflow

We use a **feature branch workflow** with the following conventions:

#### Branch Naming

- `feature/description` - New features (e.g., `feature/workout-logger-ui-improvements`)
- `bugfix/description` - Bug fixes (e.g., `bugfix/rest-timer-accuracy`)
- `hotfix/description` - Critical production fixes
- `refactor/description` - Code refactoring
- `docs/description` - Documentation updates

#### Branch Management

1. **Create feature branch** from `master`:
   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/your-feature-name
   ```

2. **Regular commits** with clear messages:
   ```bash
   git add .
   git commit -m "feat: Add rest timer functionality to workout logger"
   ```

3. **Keep branch updated**:
   ```bash
   git checkout master
   git pull origin master
   git checkout feature/your-feature-name
   git rebase master
   ```

4. **Push and create PR**:
   ```bash
   git push -u origin feature/your-feature-name
   ```

### Commit Message Standards

Follow **Conventional Commits** specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

#### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, semicolons, etc.)
- `refactor`: Code refactoring
- `test`: Adding or modifying tests
- `chore`: Build process or auxiliary tool changes

#### Examples
```bash
feat: Add video reference selector to workout logging
fix: Resolve rest timer accuracy issue in workout sessions
docs: Update API documentation for exercise endpoints
refactor: Extract common UI components for better reusability
```

## Code Standards

### Kotlin Code Style

Follow the [Kotlin Code Style Guide](code-style-guide.md) for detailed standards. Key principles:

#### Naming Conventions
- **Classes**: PascalCase (`WorkoutRepository`, `ExerciseViewModel`)
- **Functions/Variables**: camelCase (`calculateOneRepMax`, `restTimeSeconds`)
- **Constants**: UPPER_SNAKE_CASE (`DEFAULT_REST_TIME`, `MAX_SETS_PER_EXERCISE`)
- **Compose Functions**: PascalCase (`WorkoutLoggerScreen`, `ExerciseCard`)

#### Code Organization
```kotlin
// 1. Package declaration
package com.example.myworkoutlog.ui.screens

// 2. Imports (alphabetical, grouped)
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

// 3. Constants
private const val DEFAULT_ANIMATION_DURATION = 300

// 4. Composable functions
@Composable
fun WorkoutLoggerScreen(/* parameters */) {
    // Implementation
}
```

### Jetpack Compose Standards

#### State Management
```kotlin
// ✅ Good: Proper state hoisting
@Composable
fun ExerciseCard(
    exercise: Exercise,
    onExerciseClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}

// ❌ Avoid: State management inside reusable components
@Composable
fun ExerciseCard(exercise: Exercise) {
    var isExpanded by remember { mutableStateOf(false) } // Should be hoisted
    // ...
}
```

#### Performance Optimization
```kotlin
// ✅ Use remember for expensive calculations
@Composable
fun WorkoutAnalytics(workouts: List<Workout>) {
    val analytics = remember(workouts) {
        calculateWorkoutAnalytics(workouts)
    }
    // ...
}

// ✅ Stable data classes for better recomposition
@Stable
data class WorkoutUiState(
    val workouts: List<Workout>,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### Architecture Standards

#### MVVM Pattern
```kotlin
// ViewModel
class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()
    
    fun logWorkout(workout: Workout) {
        viewModelScope.launch {
            // Implementation
        }
    }
}

// Repository
class WorkoutRepository(
    private val dao: WorkoutDao,
    private val api: WorkoutApiService
) {
    suspend fun saveWorkout(workout: Workout) {
        // Implementation
    }
}
```

## Testing Requirements

### Testing Strategy

We follow the **testing pyramid** approach:

#### Unit Tests (70%)
- **Location**: `src/test/java/`
- **Target**: ViewModels, Repositories, Utilities
- **Framework**: JUnit 4, Mockito, Coroutines Test

```kotlin
@Test
fun `calculateOneRepMax returns correct value for valid input`() {
    // Arrange
    val weight = 100.0
    val reps = 5
    
    // Act
    val result = calculateOneRepMax(weight, reps)
    
    // Assert
    assertEquals(112.5, result, 0.1)
}
```

#### Integration Tests (20%)
- **Location**: `src/test/java/`
- **Target**: Repository + Database interactions
- **Framework**: Room Testing, Coroutines Test

```kotlin
@Test
fun `repository saves and retrieves workout correctly`() = runTest {
    // Arrange
    val workout = createTestWorkout()
    
    // Act
    repository.saveWorkout(workout)
    val retrieved = repository.getWorkout(workout.id)
    
    // Assert
    assertEquals(workout, retrieved)
}
```

#### UI Tests (10%)
- **Location**: `src/androidTest/java/`
- **Target**: Compose UI, User interactions
- **Framework**: Compose Testing, Espresso

```kotlin
@Test
fun workoutLoggerDisplaysCorrectExerciseCount() {
    composeTestRule.setContent {
        WorkoutLoggerScreen(/* test parameters */)
    }
    
    composeTestRule
        .onNodeWithText("3 exercises")
        .assertIsDisplayed()
}
```

### Test Coverage Requirements

- **Minimum coverage**: 80% overall
- **Critical path coverage**: 95% (workout logging, data persistence)
- **New features**: 85% minimum coverage required

### Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew testDebugUnitTest

# UI tests (requires emulator/device)
./gradlew connectedAndroidTest

# Coverage report
./gradlew jacocoTestReport
```

## Pull Request Process

### Before Creating a PR

1. **Run all tests** and ensure they pass
2. **Run code formatting** (`./gradlew ktlintFormat`)
3. **Update documentation** if necessary
4. **Test on multiple screen sizes** (phone, tablet, foldable)
5. **Verify build succeeds** (`./gradlew build`)

### PR Requirements

#### PR Title Format
```
<type>: <description>

Examples:
feat: Add video reference integration to workout logging
fix: Resolve rest timer accuracy in workout sessions
docs: Update contributing guidelines for new developers
```

#### PR Description Template
```markdown
## Summary
Brief description of changes made.

## Type of Change
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] UI tests added/updated (if applicable)
- [ ] Manual testing completed on multiple screen sizes

## Screenshots/Videos
Include screenshots for UI changes or screen recordings for complex interactions.

## Checklist
- [ ] My code follows the project's style guidelines
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes
```

### Review Process

#### Reviewer Checklist
- [ ] **Code Quality**: Follows style guidelines and best practices
- [ ] **Functionality**: Feature works as intended
- [ ] **Testing**: Adequate test coverage and all tests pass
- [ ] **Performance**: No significant performance regressions
- [ ] **Security**: No security vulnerabilities introduced
- [ ] **Documentation**: Code is well-documented and user docs updated
- [ ] **Accessibility**: UI changes follow accessibility guidelines

#### Approval Requirements
- **Minimum**: 1 approval from project maintainer
- **Major features**: 2 approvals including architecture review
- **Breaking changes**: All maintainer approvals required

### Merge Strategy

- **Feature branches**: Squash and merge for clean history
- **Hotfixes**: Merge commit to preserve urgency context
- **Documentation**: Squash and merge for clean history

## Issue Reporting

### Before Creating an Issue

1. **Search existing issues** to avoid duplicates
2. **Check documentation** for known limitations
3. **Test with latest version** to ensure issue persists
4. **Gather system information** (Android version, device model, app version)

### Issue Templates

Use the appropriate template:
- [Bug Report](.github/ISSUE_TEMPLATE/bug_report.md)
- [Feature Request](.github/ISSUE_TEMPLATE/feature_request.md)
- [Performance Issue](.github/ISSUE_TEMPLATE/performance_issue.md)
- [Documentation Improvement](.github/ISSUE_TEMPLATE/documentation.md)

### Issue Labels

#### Type Labels
- `bug` - Something isn't working correctly
- `feature` - New feature request
- `enhancement` - Improvement to existing feature
- `documentation` - Documentation related
- `performance` - Performance optimization
- `security` - Security-related issue

#### Priority Labels
- `critical` - Critical issue affecting core functionality
- `high` - High priority, affects many users
- `medium` - Medium priority, standard development timeline
- `low` - Low priority, nice-to-have improvements

#### Component Labels
- `ui/compose` - Jetpack Compose UI issues
- `data/room` - Database and data layer issues
- `analytics` - Analytics and charting functionality
- `cloud-backup` - Cloud backup and sync features
- `large-screen` - Large screen and foldable device support

## Documentation

### Documentation Requirements

All contributions should include appropriate documentation updates:

#### Code Documentation
- **Public APIs**: Comprehensive KDoc comments
- **Complex algorithms**: Inline comments explaining logic
- **Configuration**: Document all configuration options

#### User Documentation
- **New features**: Update user guides and feature documentation
- **API changes**: Update API documentation
- **Breaking changes**: Update migration guides

#### Architecture Documentation
- **New components**: Update architecture diagrams
- **Data flow changes**: Update system design documentation
- **Performance impacts**: Document performance considerations

### Documentation Style Guide

#### KDoc Standards
```kotlin
/**
 * Calculates the one-rep max for a given weight and repetitions.
 * 
 * Uses the Brzycki formula: weight / (1.0278 - (0.0278 * reps))
 * 
 * @param weight The weight lifted in the specified unit
 * @param reps The number of repetitions performed (1-12 recommended)
 * @return The estimated one-rep max in the same weight unit
 * @throws IllegalArgumentException if reps is less than 1 or greater than 50
 * 
 * @sample calculateOneRepMax(100.0, 5) returns approximately 112.5
 */
fun calculateOneRepMax(weight: Double, reps: Int): Double {
    // Implementation
}
```

#### Markdown Standards
- Use clear headings hierarchy (H1 for main title, H2 for sections, etc.)
- Include table of contents for long documents
- Use code blocks with language specification
- Include examples and screenshots where helpful
- Link to related documentation

## Getting Help

### Communication Channels

- **Issues**: GitHub Issues for bugs and feature requests
- **Discussions**: GitHub Discussions for questions and ideas
- **Wiki**: Project wiki for detailed documentation

### Resources

- [Architecture Overview](../architecture/overview.md)
- [Code Style Guide](code-style-guide.md)
- [Testing Standards](testing-standards.md)
- [Release Management](release-management.md)

### Mentorship

New contributors are welcome! Look for issues labeled `good-first-issue` or `help-wanted`. Don't hesitate to ask questions in issue comments or discussions.

---

Thank you for contributing to MyWorkoutLog! Your efforts help make fitness tracking accessible and enjoyable for everyone.