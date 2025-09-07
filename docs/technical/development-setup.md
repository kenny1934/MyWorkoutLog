# Development Setup Guide

## MyWorkoutLog - Development Environment Configuration

### Version: 1.0
### Date: September 2025
### Target Environment: Android Development

---

## 1. Prerequisites

### 1.1 System Requirements

#### Minimum Requirements
- **Operating System**: Windows 10/11, macOS 10.15+, or Ubuntu 18.04+ LTS
- **RAM**: 8GB (16GB recommended for optimal performance)
- **Storage**: 4GB free space for Android Studio + 2GB for SDKs
- **Display**: 1920x1080 resolution recommended

#### Recommended Requirements
- **RAM**: 16GB+ for smooth development experience
- **CPU**: Multi-core processor (Intel i5/AMD Ryzen 5 or better)
- **Storage**: SSD for faster build times
- **Display**: 2560x1440+ for large screen development

### 1.2 Required Software

#### Core Development Tools
1. **Android Studio**: Latest stable version (Koala Feature Drop | 2024.1.2+)
2. **Java Development Kit (JDK)**: JDK 11 or higher
3. **Git**: Version control system
4. **Android SDK**: API levels 26-35 (managed through Android Studio)

#### Optional but Recommended
- **Genymotion**: Alternative Android emulator
- **Scrcpy**: Android device screen mirroring
- **ADB**: Android Debug Bridge (included with Android Studio)
- **Fastboot**: Android flashing tool

---

## 2. Android Studio Setup

### 2.1 Installation

#### Download and Install
1. Download Android Studio from [developer.android.com](https://developer.android.com/studio)
2. Run the installer with administrator privileges
3. Follow the setup wizard, ensuring to install:
   - Android SDK Platform-Tools
   - Android SDK Build-Tools
   - Android Emulator
   - Intel x86 Emulator Accelerator (HAXM)

#### Initial Configuration
```bash
# Verify Android Studio installation
which android-studio  # macOS/Linux
where android-studio   # Windows

# Set ANDROID_HOME environment variable
export ANDROID_HOME=$HOME/Android/Sdk  # macOS/Linux
# Windows: Add to System Environment Variables
# ANDROID_HOME = C:\Users\%USERNAME%\AppData\Local\Android\Sdk
```

### 2.2 SDK Configuration

#### Required SDK Components
```bash
# Required API Levels
API Level 26 (Android 8.0) - Minimum supported
API Level 35 (Android 15) - Target and compile SDK

# Additional Tools
- Android SDK Build-Tools 35.0.0
- Android SDK Platform-Tools
- Android SDK Tools
- Google USB Driver (Windows)
```

#### SDK Installation via Command Line
```bash
# Using sdkmanager
sdkmanager --list
sdkmanager "platforms;android-26"
sdkmanager "platforms;android-35"
sdkmanager "build-tools;35.0.0"
sdkmanager "platform-tools"
```

---

## 3. Project Setup

### 3.1 Repository Clone

#### Clone the Repository
```bash
# Using HTTPS
git clone https://github.com/user/MyWorkoutLog.git
cd MyWorkoutLog

# Using SSH (recommended for development)
git clone git@github.com:user/MyWorkoutLog.git
cd MyWorkoutLog

# Verify project structure
ls -la
```

#### Branch Management
```bash
# Check current branch and available branches
git branch -a

# The main development branch
git checkout feature/workout-logger-ui-improvements

# Create a new feature branch
git checkout -b feature/your-feature-name
```

### 3.2 Project Structure Overview

```
MyWorkoutLog/
├── app/
│   ├── build.gradle.kts              # App-level build configuration
│   ├── proguard-rules.pro            # Code obfuscation rules
│   └── src/
│       ├── main/
│       │   ├── java/com/example/myworkoutlog/  # Kotlin source files
│       │   ├── res/                  # Resources (layouts, strings, etc.)
│       │   └── AndroidManifest.xml   # App manifest
│       ├── test/                     # Unit tests
│       └── androidTest/              # Instrumentation tests
├── docs/                             # Documentation
│   ├── technical/                    # Technical documentation
│   ├── product/                      # Product documentation  
│   └── legal/                        # Legal and compliance docs
├── gradle/
│   └── libs.versions.toml            # Version catalog
├── build.gradle.kts                  # Project-level build configuration
├── settings.gradle.kts               # Gradle settings
├── gradle.properties                 # Gradle properties
└── CLAUDE.md                         # Development context
```

### 3.3 Build Configuration Analysis

#### Gradle Version Catalog (libs.versions.toml)
```toml
[versions]
agp = "8.11.1"                        # Android Gradle Plugin
kotlin = "2.1.21"                     # Kotlin version
composeBom = "2025.06.00"             # Compose BOM version
ksp = "2.1.21-2.0.1"                  # Kotlin Symbol Processing

[libraries]
# Core Android libraries
androidx-core-ktx = "1.16.0"
lifecycle-runtime-ktx = "2.9.1"
activity-compose = "1.10.1"

# Compose UI
compose-bom = "2025.06.00"
material3 = { group = "androidx.compose.material3" }
```

#### App-Level Build Configuration (app/build.gradle.kts)
```kotlin
android {
    namespace = "com.example.myworkoutlog"
    compileSdk = 35                    # Latest Android SDK
    
    defaultConfig {
        applicationId = "com.example.myworkoutlog"
        minSdk = 26                    # Android 8.0+
        targetSdk = 35                 # Latest Android
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"               # Java 11 target
    }
    
    buildFeatures {
        compose = true                 # Enable Jetpack Compose
    }
}
```

---

## 4. Dependencies & Libraries

### 4.1 Core Dependencies

#### Android Architecture Components
```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.16.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
implementation("androidx.activity:activity-compose:1.10.1")

// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2025.06.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended:1.7.3")
```

#### Room Database
```kotlin
val room_version = "2.6.1"
implementation("androidx.room:room-runtime:$room_version")
implementation("androidx.room:room-ktx:$room_version")
ksp("androidx.room:room-compiler:$room_version")  // Annotation processing
```

#### ViewModel & Coroutines
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
```

#### Navigation & Data Storage
```kotlin
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.datastore:datastore-preferences:1.1.1")
implementation("com.google.code.gson:gson:2.10.1")
```

### 4.2 Feature-Specific Dependencies

#### Charts & Analytics
```kotlin
implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")
```

#### Cloud Backup (Google Drive)
```kotlin
implementation("com.google.android.gms:play-services-auth:21.0.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "com.google.guava")
}
implementation("com.google.api-client:google-api-client-android:2.2.0") {
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "com.google.guava")
}
```

#### Background Processing
```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

### 4.3 Testing Dependencies
```kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")

// Android Testing
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.00"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")

// Debug Tools
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

---

## 5. IDE Configuration

### 5.1 Android Studio Plugins

#### Essential Plugins
1. **Kotlin Multiplatform Mobile** (built-in)
2. **Android WiFi ADB** - Wireless debugging
3. **ADB Idea** - ADB commands integration
4. **Rainbow Brackets** - Code readability
5. **GitToolBox** - Enhanced Git integration

#### Installation
```
File → Settings → Plugins → Marketplace → Search and Install
```

### 5.2 Code Style Configuration

#### Kotlin Code Style
```
File → Settings → Editor → Code Style → Kotlin
- Set to "Official" Kotlin code style
- Tab size: 4
- Indent: 4
- Continuation indent: 4
```

#### Import Organization
```kotlin
// Kotlin/Java imports
import android.*
import androidx.*
import com.example.myworkoutlog.*
import com.google.*
import java.*
import kotlin.*
import kotlinx.*
```

### 5.3 Live Templates (Code Snippets)

#### Custom Compose Templates
```kotlin
// Composable function template
@Composable
fun $NAME$(
    modifier: Modifier = Modifier
) {
    $CONTENT$
}

// ViewModel template  
class $NAME$ViewModel(
    private val repository: $REPOSITORY$Repository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow($STATE$())
    val uiState: StateFlow<$STATE$> = _uiState.asStateFlow()
    
}
```

---

## 6. Build & Run Configuration

### 6.1 Build Variants

#### Debug Configuration
```kotlin
android {
    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
}
```

#### Release Configuration
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 6.2 Build Commands

#### Using Android Studio
1. **Build → Make Project** (Ctrl+F9 / Cmd+F9)
2. **Build → Rebuild Project** (Ctrl+Shift+F9 / Cmd+Shift+F9)
3. **Build → Clean Project**

#### Using Command Line
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Install debug APK to connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Run tests
./gradlew test
```

### 6.3 Gradle Configuration Optimization

#### gradle.properties Optimizations
```properties
# JVM Arguments
org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8

# Gradle Daemon
org.gradle.daemon=true

# Parallel builds
org.gradle.parallel=true

# Configure only on demand
org.gradle.configureondemand=true

# Use Gradle build cache
org.gradle.caching=true

# AndroidX
android.useAndroidX=true
android.nonTransitiveRClass=true

# Kotlin
kotlin.code.style=official
```

---

## 7. Device Setup & Testing

### 7.1 Physical Device Setup

#### Enable Developer Options
1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times
3. Return to **Settings → System → Developer Options**
4. Enable **USB Debugging**
5. Enable **Stay Awake**
6. Enable **Don't keep activities** (for testing)

#### USB Connection Setup
```bash
# Verify device connection
adb devices

# Should show something like:
# List of devices attached
# ABC123DEF456    device
```

#### Wireless Debugging (Android 11+)
```bash
# Enable wireless debugging on device
# Settings → Developer Options → Wireless Debugging

# Connect via ADB
adb connect <device_ip>:5555
```

### 7.2 Emulator Setup

#### Create AVD (Android Virtual Device)
1. **Tools → AVD Manager → Create Virtual Device**
2. **Select Device**: Pixel 7 Pro or Galaxy Z Fold 6 (for large screen testing)
3. **System Image**: API 35 (Android 15) with Google APIs
4. **Advanced Settings**:
   - RAM: 4GB
   - VM Heap: 512MB  
   - Internal Storage: 8GB
   - Graphics: Hardware - GLES 2.0

#### Recommended Emulator Configurations

##### Phone Testing (Pixel 7)
```
Device: Pixel 7
Resolution: 2400 x 1080
Density: 420 dpi
RAM: 3GB
API Level: 35 (Android 15)
```

##### Large Screen Testing (Galaxy Z Fold 6)
```
Device: Galaxy Z Fold 6 (Unfolded)
Resolution: 2176 x 1812  
Density: 374 dpi
RAM: 4GB
API Level: 35 (Android 15)
```

##### Tablet Testing (Pixel Tablet)
```
Device: Pixel Tablet
Resolution: 2560 x 1600
Density: 276 dpi  
RAM: 4GB
API Level: 35 (Android 15)
```

---

## 8. Database Development

### 8.1 Room Database Inspection

#### Database Inspector (Android Studio)
1. **View → Tool Windows → App Inspection**
2. Run app on device/emulator
3. Select **Database Inspector** tab
4. Explore tables: `exercise_table`, `logged_workout_table`, etc.

#### Export Database for Analysis
```bash
# Using ADB
adb shell "run-as com.example.myworkoutlog.debug cat /data/data/com.example.myworkoutlog.debug/databases/workout_database" > workout_database.db

# Open with DB Browser for SQLite or similar tool
```

### 8.2 Database Version Management

#### Schema Changes Workflow
1. Modify entity classes in `DataModels.kt`
2. Increment database version in `WorkoutDatabase.kt`
3. Create migration script if needed
4. Test migration with existing data

#### Migration Example
```kotlin
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE exercise_table 
            ADD COLUMN difficulty_rating INTEGER DEFAULT 0 NOT NULL
        """)
    }
}
```

---

## 9. Testing Setup

### 9.1 Unit Testing Configuration

#### Test Directory Structure
```
app/src/test/java/com/example/myworkoutlog/
├── viewmodel/          # ViewModel tests
├── repository/         # Repository tests  
├── service/           # Service object tests
├── util/              # Utility function tests
└── data/              # Data model tests
```

#### Example Unit Test
```kotlin
class PrServiceTest {
    @Test
    fun `detectNewPRs should identify weight PR correctly`() {
        // Given
        val workout = TestDataFactory.createTestWorkout()
        val existingPRs = emptyList<PersonalRecord>()
        val exercises = listOf(TestDataFactory.createTestExercise())
        
        // When
        val newPRs = PrService.detectNewPRs(workout, existingPRs, exercises)
        
        // Then
        assertTrue(newPRs.isNotEmpty())
        assertEquals(PRType.MAX_WEIGHT_FOR_REPS, newPRs.first().type)
    }
}
```

### 9.2 Integration Testing

#### Database Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: WorkoutDatabase
    private lateinit var exerciseDao: ExerciseDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exerciseDao = database.exerciseDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun insertAndGetExercise() = runTest {
        val exercise = TestDataFactory.createTestExercise()
        exerciseDao.insert(exercise)
        
        val retrieved = exerciseDao.getExerciseById(exercise.id).first()
        assertEquals(exercise.name, retrieved?.name)
    }
}
```

### 9.3 UI Testing Setup

#### Compose Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dashboardDisplaysCorrectly() {
        composeTestRule.setContent {
            MyWorkoutLogTheme {
                DashboardScreen()
            }
        }
        
        composeTestRule.onNodeWithText("Dashboard")
            .assertIsDisplayed()
    }
}
```

---

## 10. Performance Optimization

### 10.1 Build Performance

#### Gradle Performance Tuning
```properties
# gradle.properties optimizations
org.gradle.jvmargs=-Xmx6g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.caching=true
android.builder.sdkDownload=true
```

#### Dependency Management
```kotlin
// Use implementation instead of api when possible
implementation("androidx.room:room-runtime:$room_version")

// Use specific versions instead of '+'
implementation("androidx.compose.material3:material3:1.1.2")
// NOT: implementation("androidx.compose.material3:material3:+")
```

### 10.2 Runtime Performance

#### Memory Profiling Setup
1. **View → Tool Windows → Profiler**
2. Run app on device
3. Select **Memory** profiler
4. Monitor memory usage during UI testing

#### Performance Monitoring
```kotlin
// Enable composition tracking in debug builds
android {
    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_COMPOSE_COMPILER_REPORTS", "true")
        }
    }
}
```

---

## 11. Debugging & Troubleshooting

### 11.1 Common Issues & Solutions

#### Build Issues

**Problem**: "Cannot resolve symbol" errors
```bash
# Solution
File → Invalidate Caches and Restart
# OR
./gradlew clean
./gradlew build
```

**Problem**: Room compilation errors
```bash
# Check KSP is properly configured
plugins {
    id("com.google.devtools.ksp")
}

# Ensure Room compiler is using KSP
ksp("androidx.room:room-compiler:2.6.1")
# NOT: kapt("androidx.room:room-compiler:2.6.1")
```

**Problem**: Compose navigation issues
```bash
# Ensure consistent navigation-compose version
implementation("androidx.navigation:navigation-compose:2.7.7")
```

#### Runtime Issues

**Problem**: Database crashes on startup
```kotlin
// Add fallbackToDestructiveMigration() for development
Room.databaseBuilder(context, WorkoutDatabase::class.java, "workout_database")
    .fallbackToDestructiveMigration()
    .build()
```

**Problem**: Memory leaks in ViewModels
```kotlin
// Always use viewModelScope for coroutines
viewModelScope.launch {
    // coroutine code
}

// Use StateIn with appropriate sharing behavior
data.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = InitialState
)
```

### 11.2 Debugging Tools

#### Logging Setup
```kotlin
// Use consistent logging pattern
private val tag = "WorkoutLogger"

Log.d(tag, "Starting workout session")
Log.w(tag, "Missing bodyweight data")
Log.e(tag, "Failed to save workout", exception)
```

#### Compose Layout Inspector
1. **Tools → Layout Inspector**
2. Select running app
3. Enable **Live Updates**
4. Inspect compose tree and recomposition counts

---

## 12. Deployment Preparation

### 12.1 Release Build Configuration

#### Signing Configuration
```kotlin
android {
    signingConfigs {
        release {
            storeFile file("../keystore/myworkoutlog.keystore")
            storePassword System.getenv("STORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

#### ProGuard Configuration (proguard-rules.pro)
```bash
# Keep data classes
-keep class com.example.myworkoutlog.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Gson serialization
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep Google Drive API classes
-keep class com.google.** { *; }
```

### 12.2 Quality Assurance

#### Pre-deployment Checklist
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All instrumentation tests pass (`./gradlew connectedAndroidTest`)
- [ ] No lint warnings (`./gradlew lintRelease`)
- [ ] Performance testing on target devices
- [ ] Memory leak testing
- [ ] Accessibility testing with TalkBack
- [ ] Large screen testing (tablets, foldables)
- [ ] Different Android versions testing (API 26-35)

---

## 13. Development Workflow

### 13.1 Git Workflow

#### Branch Management
```bash
# Feature development
git checkout -b feature/exercise-filtering
# Make changes, commit
git add .
git commit -m "feat: Add exercise filtering by muscle group"

# Push and create pull request
git push origin feature/exercise-filtering
```

#### Commit Message Convention
```bash
feat: Add new feature
fix: Bug fix
docs: Documentation changes
style: Code style changes
refactor: Code refactoring
test: Test changes
chore: Build process or auxiliary tool changes
```

### 13.2 Development Best Practices

#### Code Review Checklist
- [ ] Code follows Kotlin style guide
- [ ] All public functions have KDoc comments
- [ ] No hardcoded strings (use string resources)
- [ ] Database queries are optimized
- [ ] Memory leaks prevented
- [ ] Error handling implemented
- [ ] Tests cover new functionality

#### Performance Guidelines
- Use `LazyColumn` for large lists
- Implement proper state management with StateFlow
- Avoid unnecessary recomposition
- Use `remember` for expensive calculations
- Implement proper coroutine cancellation

---

## 14. Conclusion

This development setup guide provides comprehensive configuration for MyWorkoutLog development. The environment is optimized for:

- **Modern Android Development**: Latest Jetpack Compose and Material 3
- **Large Screen Optimization**: Galaxy Z Fold 6 and tablet support
- **Database-Intensive Operations**: Room with complex analytics
- **Cloud Integration**: Google Drive backup functionality
- **Production Quality**: Proper testing, performance, and deployment setup

### Quick Start Checklist
1. ✅ Install Android Studio with required SDKs
2. ✅ Clone repository and checkout development branch
3. ✅ Configure emulators for different screen sizes
4. ✅ Run `./gradlew build` to verify setup
5. ✅ Run app on device/emulator
6. ✅ Run tests to ensure environment is working
7. ✅ Set up IDE plugins and code style

With this setup, developers can efficiently work on MyWorkoutLog's advanced features including adaptive layouts, sophisticated analytics, and comprehensive fitness tracking capabilities.

---

*This guide serves as the definitive development environment setup reference for MyWorkoutLog contributors and maintainers.*