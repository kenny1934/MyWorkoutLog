# Database Design Document

## MyWorkoutLog - Database Architecture & Schema Design

### Version: 1.0
### Date: September 2025
### Database Version: 20

---

## 1. Overview

MyWorkoutLog utilizes **Room Persistence Library** built on SQLite for local data storage. The database design follows normalized principles while using JSON serialization for complex data structures to balance performance and maintainability.

### 1.1 Design Philosophy
- **Hybrid Approach**: Normalized core entities with JSON fields for complex relationships
- **Performance Optimized**: Strategic indexing and query optimization
- **Type Safety**: Compile-time query validation with Room
- **Extensible**: Version management with migration strategies

### 1.2 Database Configuration
```kotlin
@Database(
    entities = [
        Exercise::class,
        WorkoutTemplate::class, 
        LoggedWorkout::class,
        ProgramTemplate::class,
        ActiveProgramCycle::class,
        PersonalRecord::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase()
```

---

## 2. Entity-Relationship Diagram

```
┌─────────────────────┐         ┌─────────────────────┐
│      Exercise       │         │  WorkoutTemplate    │
├─────────────────────┤         ├─────────────────────┤
│ id (PK) String      │◄────┐   │ id (PK) String      │
│ name String         │     │   │ name String         │
│ usesBodyweight Bool │     │   │ description String? │
│ targetMuscleGroups  │     └───┤ templateExercises   │◄─────┐
│   List<MuscleGroup> │         │   List<TemplateExer>│      │
│ equipment           │         └─────────────────────┘      │
│   List<Equipment>   │                                      │
│ notes String?       │         ┌─────────────────────┐      │
│ videoLink String?   │         │   LoggedWorkout     │      │
└─────────────────────┘         ├─────────────────────┤      │
                                │ id (PK) String      │      │
┌─────────────────────┐         │ date String         │      │
│   PersonalRecord    │         │ name String?        │      │
├─────────────────────┤         │ overallComments     │      │
│ id (PK) String      │         │   String?           │      │
│ exerciseId String   │◄────────┤ startTimestamp Long?│      │
│ exerciseName String │         │ endTimestamp Long?  │      │
│ date String         │         │ bodyweight Double?  │      │
│ loggedWorkoutId     │◄────────┤ loggedExercises     │      │
│   String            │         │   List<LoggedExer>  │      │
│ type PRType         │         │ workoutTemplateId   │◄─────┘
│ weight Double?      │         │   String?           │
│ reps Int?           │         │ isInProgress Bool   │
│ durationSecs Int?   │         │ activeProgramCycle  │
│ bodyweightUsed      │         │   Id String?        │◄──┐
│   Double?           │         └─────────────────────┘   │
│ externalWeight      │                                   │
│   Double?           │         ┌─────────────────────┐   │
│ usesBodyweight Bool │         │  ProgramTemplate    │   │
└─────────────────────┘         ├─────────────────────┤   │
                                │ id (PK) String      │   │
┌─────────────────────┐         │ name String         │   │
│ ActiveProgramCycle  │         │ description String? │   │
├─────────────────────┤         │ weeks List<Program  │   │
│ id (PK) Int         │◄────────│   WeekDefinition>   │   │
│ cycleUuid String    │         └─────────────────────┘   │
│ programTemplateId   │◄────────────────────────────────┘
│   String            │
│ programTemplateName │
│   String            │
│ userCycleName       │
│   String            │
│ startDate String    │
│ completedSessions   │
│   Map<String,String>│
│ cycleProgram        │
│   ProgramTemplate   │
└─────────────────────┘
```

---

## 3. Core Entities

### 3.1 Exercise Table
**Purpose**: Master exercise library with all exercise definitions

```sql
CREATE TABLE exercise_table (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    usesBodyweight INTEGER NOT NULL DEFAULT 0,
    targetMuscleGroups TEXT NOT NULL,  -- JSON List<MuscleGroup>
    equipment TEXT NOT NULL,           -- JSON List<Equipment>
    notes TEXT,
    videoLink TEXT
);
```

**Key Features**:
- **Primary Key**: UUID string for cross-platform compatibility
- **Muscle Groups**: JSON-serialized enum list for flexible targeting
- **Equipment**: Multi-equipment exercise support
- **Bodyweight Flag**: Critical for PR calculations and volume analysis

### 3.2 WorkoutTemplate Table
**Purpose**: Reusable workout blueprints

```sql
CREATE TABLE workout_template_table (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    templateExercises TEXT NOT NULL  -- JSON List<TemplateExercise>
);
```

**Embedded TemplateExercise Structure**:
```kotlin
data class TemplateExercise(
    val id: String,
    val exerciseId: String,         -- References Exercise.id
    val exerciseName: String,       -- Denormalized for performance
    val targetMuscleGroups: List<MuscleGroup>,
    val equipment: List<Equipment>,
    val sets: List<TemplateExerciseSet>,
    val order: Int,
    val notes: String?
)

data class TemplateExerciseSet(
    val id: String,
    val targetReps: String?,        -- Flexible: "8-12", "15", "AMRAP"
    val targetSecs: String?,        -- For time-based exercises
    val targetRIR: String?,         -- Rate of Perceived Exertion
    val notes: String?
)
```

### 3.3 LoggedWorkout Table
**Purpose**: Actual workout performance data

```sql
CREATE TABLE logged_workout_table (
    id TEXT PRIMARY KEY,
    date TEXT NOT NULL,
    name TEXT,
    overallComments TEXT,
    startTimestamp INTEGER,
    endTimestamp INTEGER,
    bodyweight REAL,
    performedWeightUnit TEXT,
    activeProgramCycleId TEXT,
    programWeekDefinitionId TEXT,
    programSessionDefinitionId TEXT,
    userCycleName TEXT,
    loggedExercises TEXT NOT NULL,  -- JSON List<LoggedExercise>
    workoutTemplateId TEXT,
    isInProgress INTEGER NOT NULL DEFAULT 0,
    
    FOREIGN KEY (workoutTemplateId) REFERENCES workout_template_table(id),
    FOREIGN KEY (activeProgramCycleId) REFERENCES active_program_cycle_table(id)
);
```

**Embedded LoggedExercise Structure**:
```kotlin
data class LoggedExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val targetMuscleGroups: List<MuscleGroup>,
    val equipment: List<Equipment>,
    val sets: List<LoggedSet>,
    val isSubstitute: Boolean?,
    val notes: String?
)

data class LoggedSet(
    val id: String,
    val reps: Int?,
    val secs: Int?,
    val weight: Double?,
    val rir: Int?,
    val bands: String?,
    val notes: String?,
    val restTimeSeconds: Int?,        -- Actual rest time tracking
    val videoReference: String?,      -- Content URI for form videos
    val targetReps: String?,          -- Snapshot of original targets
    val targetSecs: String?
)
```

### 3.4 PersonalRecord Table
**Purpose**: PR tracking with bodyweight exercise support

```sql
CREATE TABLE personal_record_table (
    id TEXT PRIMARY KEY,
    exerciseId TEXT NOT NULL,
    exerciseName TEXT NOT NULL,
    date TEXT NOT NULL,
    loggedWorkoutId TEXT NOT NULL,
    type TEXT NOT NULL,              -- PRType enum
    weightUnit TEXT,
    reps INTEGER,
    weight REAL,                     -- Total effective weight
    durationSecs INTEGER,
    bodyweightUsed REAL,             -- User's bodyweight at PR time
    externalWeight REAL,             -- Added weight only
    usesBodyweight INTEGER NOT NULL DEFAULT 0,
    
    FOREIGN KEY (exerciseId) REFERENCES exercise_table(id),
    FOREIGN KEY (loggedWorkoutId) REFERENCES logged_workout_table(id)
);
```

### 3.5 ProgramTemplate Table
**Purpose**: Multi-week program blueprints

```sql
CREATE TABLE program_template_table (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    weeks TEXT NOT NULL             -- JSON List<ProgramWeekDefinition>
);
```

**Embedded Program Structure**:
```kotlin
data class ProgramWeekDefinition(
    val id: String,
    val weekLabel: String,          -- "Week 1: RIR 3"
    val sessions: List<ProgramSessionDefinition>,
    val order: Int
)

data class ProgramSessionDefinition(
    val id: String,
    val sessionName: String,        -- "Day 1: Push Day"
    val workoutTemplateId: String,  -- References WorkoutTemplate.id
    val order: Int
)
```

### 3.6 ActiveProgramCycle Table
**Purpose**: Current program state and progress tracking

```sql
CREATE TABLE active_program_cycle_table (
    id INTEGER PRIMARY KEY DEFAULT 1,  -- Singleton table
    cycleUuid TEXT NOT NULL,
    programTemplateId TEXT NOT NULL,
    programTemplateName TEXT NOT NULL,
    userCycleName TEXT NOT NULL,
    startDate TEXT NOT NULL,
    completedSessions TEXT NOT NULL,    -- JSON Map<String, String>
    cycleProgram TEXT NOT NULL,         -- JSON ProgramTemplate snapshot
    
    FOREIGN KEY (programTemplateId) REFERENCES program_template_table(id)
);
```

---

## 4. Data Access Objects (DAOs)

### 4.1 ExerciseDao
**Purpose**: Exercise library management

```kotlin
@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(exercise: Exercise)
    
    @Query("SELECT * FROM exercise_table ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercise_table WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<Exercise?>
    
    @Update
    fun updateExercise(exercise: Exercise)
    
    @Delete
    fun deleteExercise(exercise: Exercise)
}
```

### 4.2 LoggedWorkoutDao
**Purpose**: Workout data and analytics queries

```kotlin
@Dao
interface LoggedWorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(loggedWorkout: LoggedWorkout)
    
    // History and retrieval
    @Query("SELECT * FROM logged_workout_table ORDER BY startTimestamp DESC, date DESC")
    fun getAllLoggedWorkouts(): Flow<List<LoggedWorkout>>
    
    // Analytics queries
    @Query("SELECT * FROM logged_workout_table WHERE date BETWEEN :startDate AND :endDate")
    fun getWorkoutsByDateRange(startDate: String, endDate: String): Flow<List<LoggedWorkout>>
    
    // Program cycle tracking
    @Query("SELECT * FROM logged_workout_table WHERE activeProgramCycleId = :cycleId")
    fun getWorkoutsByCycle(cycleId: String): Flow<List<LoggedWorkout>>
    
    // Session persistence
    @Query("SELECT * FROM logged_workout_table WHERE isInProgress = 1")
    fun getWorkoutsInProgress(): Flow<List<LoggedWorkout>>
    
    @Query("UPDATE logged_workout_table SET isInProgress = 0 WHERE id = :workoutId")
    fun markWorkoutCompleted(workoutId: String)
    
    // Complex analytics queries
    @Query("""
        SELECT lw.* FROM logged_workout_table lw
        WHERE lw.loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%'
        AND lw.date BETWEEN :startDate AND :endDate
    """)
    fun getWorkoutsWithExerciseInDateRange(
        exerciseId: String, 
        startDate: String, 
        endDate: String
    ): Flow<List<LoggedWorkout>>
}
```

### 4.3 PersonalRecordDao
**Purpose**: PR tracking and comparison

```kotlin
@Dao  
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(personalRecord: PersonalRecord)
    
    @Query("SELECT * FROM personal_record_table WHERE exerciseId = :exerciseId")
    fun getPersonalRecordsForExercise(exerciseId: String): Flow<List<PersonalRecord>>
    
    @Query("SELECT * FROM personal_record_table ORDER BY date DESC")
    fun getAllPersonalRecords(): Flow<List<PersonalRecord>>
    
    @Query("SELECT * FROM personal_record_table ORDER BY date DESC LIMIT :limit")
    fun getRecentPersonalRecords(limit: Int): Flow<List<PersonalRecord>>
}
```

---

## 5. Type Converters

### 5.1 Complex Type Serialization
Room requires type converters for complex data types:

```kotlin
class Converters {
    private val gson = Gson()
    
    // Enum Lists
    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String = gson.toJson(value)
    
    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> {
        val listType = object : TypeToken<List<MuscleGroup>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    // Complex Objects
    @TypeConverter
    fun fromTemplateExerciseList(value: List<TemplateExercise>): String = gson.toJson(value)
    
    @TypeConverter
    fun toTemplateExerciseList(value: String): List<TemplateExercise> {
        val listType = object : TypeToken<List<TemplateExercise>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    // Maps for program progress
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String = gson.toJson(value)
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }
}
```

### 5.2 Performance Considerations
- **JSON Serialization**: Balance between flexibility and query performance
- **Denormalization**: Cached exercise names for display performance
- **Indexing Strategy**: Primary keys and frequently queried fields

---

## 6. Query Optimization

### 6.1 Complex Analytics Queries
The database supports sophisticated analytics through carefully crafted queries:

**Volume Analysis by Date Range**:
```sql
SELECT 
    date,
    SUM(CASE WHEN json_extract(set.value, '$.weight') IS NOT NULL 
         THEN json_extract(set.value, '$.weight') * json_extract(set.value, '$.reps')
         ELSE 0 END) as totalVolume
FROM logged_workout_table,
     json_each(json_extract(loggedExercises, '$')) as exercise,
     json_each(json_extract(exercise.value, '$.sets')) as set
WHERE date BETWEEN ? AND ?
GROUP BY date
ORDER BY date;
```

**Exercise Performance Trends**:
```sql
SELECT 
    lw.date,
    lw.id as workoutId,
    json_extract(exercise.value, '$.exerciseId') as exerciseId,
    MAX(json_extract(set.value, '$.weight')) as bestWeight,
    MAX(json_extract(set.value, '$.reps')) as bestReps
FROM logged_workout_table lw,
     json_each(json_extract(lw.loggedExercises, '$')) as exercise,
     json_each(json_extract(exercise.value, '$.sets')) as set
WHERE json_extract(exercise.value, '$.exerciseId') = ?
GROUP BY lw.date, lw.id
ORDER BY lw.date DESC;
```

### 6.2 Indexing Strategy
```sql
-- Primary performance indexes
CREATE INDEX idx_logged_workout_date ON logged_workout_table(date);
CREATE INDEX idx_logged_workout_cycle ON logged_workout_table(activeProgramCycleId);
CREATE INDEX idx_personal_record_exercise ON personal_record_table(exerciseId);
CREATE INDEX idx_personal_record_date ON personal_record_table(date);

-- Composite indexes for common queries
CREATE INDEX idx_logged_workout_date_cycle ON logged_workout_table(date, activeProgramCycleId);
CREATE INDEX idx_personal_record_exercise_type ON personal_record_table(exerciseId, type);
```

---

## 7. Data Relationships

### 7.1 Foreign Key Relationships
While Room supports foreign keys, the hybrid design uses both explicit FKs and JSON references:

**Explicit Foreign Keys**:
- `LoggedWorkout.workoutTemplateId` → `WorkoutTemplate.id`
- `LoggedWorkout.activeProgramCycleId` → `ActiveProgramCycle.id`
- `PersonalRecord.exerciseId` → `Exercise.id`
- `PersonalRecord.loggedWorkoutId` → `LoggedWorkout.id`

**JSON References** (for flexibility):
- `TemplateExercise.exerciseId` → `Exercise.id` (within JSON)
- `ProgramSessionDefinition.workoutTemplateId` → `WorkoutTemplate.id` (within JSON)

### 7.2 Data Integrity
- **Cascade Deletes**: Handled in application logic for JSON references
- **Referential Integrity**: Enforced through DAO validation
- **Data Validation**: Type safety through Kotlin data classes

---

## 8. Migration Strategy

### 8.1 Version Management
```kotlin
@Database(version = 20)
abstract class WorkoutDatabase : RoomDatabase() {
    companion object {
        fun getDatabase(context: Context): WorkoutDatabase {
            return Room.databaseBuilder(context, WorkoutDatabase::class.java, "workout_database")
                .fallbackToDestructiveMigration()  // For development
                // .addMigrations(MIGRATION_19_20)  // For production
                .build()
        }
    }
}
```

### 8.2 Migration Examples
```kotlin
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE logged_workout_table 
            ADD COLUMN isInProgress INTEGER NOT NULL DEFAULT 0
        """)
    }
}
```

---

## 9. Performance Characteristics

### 9.1 Query Performance
- **Simple Queries**: Sub-millisecond response for primary key lookups
- **Analytics Queries**: Optimized with strategic indexing for <100ms response
- **JSON Queries**: Efficient for small-to-medium datasets (typical user has <1000 workouts)

### 9.2 Storage Efficiency
- **Typical Database Size**: 5-50MB for active users
- **JSON Overhead**: ~20% compared to fully normalized design
- **Compression**: SQLite built-in compression for text fields

### 9.3 Memory Usage
- **Room Caching**: Automatic query result caching
- **Flow Objects**: Reactive updates with minimal memory overhead
- **Large Datasets**: Lazy loading with pagination for history views

---

## 10. Backup and Recovery

### 10.1 Export Format
The database supports comprehensive export to JSON:
```kotlin
data class DatabaseExport(
    val version: String,
    val exportDate: String,
    val exercises: List<Exercise>,
    val workoutTemplates: List<WorkoutTemplate>,
    val loggedWorkouts: List<LoggedWorkout>,
    val programTemplates: List<ProgramTemplate>,
    val personalRecords: List<PersonalRecord>
)
```

### 10.2 Cloud Backup Integration
- **Encryption**: AES encryption before cloud upload
- **Compression**: GZIP compression for bandwidth efficiency
- **Incremental Sync**: Delta changes for large datasets
- **Version Compatibility**: Cross-version import/export support

---

## 11. Security Considerations

### 11.1 Data Protection
- **Local Encryption**: SQLite encryption at rest (optional)
- **Content URIs**: Temporary video access without persistent storage
- **API Keys**: Secure credential management for cloud services
- **Input Validation**: SQL injection prevention through parameterized queries

### 11.2 Privacy
- **No Analytics**: All user data remains local unless explicitly backed up
- **User Control**: Full control over cloud backup and data sharing
- **Data Anonymization**: Export options for anonymized data sharing

---

## 12. Future Considerations

### 12.1 Scalability Improvements
- **Partitioning**: Date-based partitioning for large workout histories
- **Archiving**: Automated archiving of old workout data
- **Caching Layers**: Redis-like caching for frequently accessed data

### 12.2 Advanced Features
- **Full-Text Search**: SQLite FTS for exercise and workout search
- **Spatial Queries**: Gym location tracking and workout mapping
- **Time-Series Optimization**: Specialized storage for analytics data

---

This database design successfully balances flexibility, performance, and maintainability while supporting the complex requirements of a comprehensive fitness tracking application. The hybrid approach of normalized core entities with JSON complex types provides the optimal solution for MyWorkoutLog's diverse data requirements.

---

*This document serves as the definitive database design reference for MyWorkoutLog, including schema definitions, query patterns, and optimization strategies.*