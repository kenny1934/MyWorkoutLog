# Claude Code Prompt Templates for MyWorkoutLog

## Initial Setup Prompt
```
I'm working on MyWorkoutLog, an Android workout tracking app. Please review the project structure, particularly:
- The documentation in /docs/
- The .claude-context file
- The current state of MainActivity.kt and DataModels.kt

Confirm you understand the project architecture and current development priorities.
```

## Feature Implementation Prompts

### 1. Add Detailed Set Logging Fields
```
Implement detailed set logging by adding RIR (Reps in Reserve), resistance bands, and notes fields:

1. Update DataModels.kt:
   - Add to LoggedSet: rir: Int?, bands: String?, notes: String?
   - Add to TemplateExerciseSet: targetRIR: String?

2. Update MainActivity.kt:
   - Modify LoggedSetRow composable to include new input fields
   - Add OutlinedTextField for RIR (numeric, 0-10)
   - Add OutlinedTextField for bands (text)
   - Add expandable notes field

3. Update WorkoutLoggerViewModel.kt:
   - Modify updateSet function to handle new parameters
   - Ensure null safety for optional fields

4. Maintain backward compatibility with existing workout data

Please implement incrementally and test each change.
```

### 2. Add Exercise During Workout
```
Implement the ability to add exercises during an active workout session:

1. Create ExerciseSelectionDialog composable in MainActivity.kt:
   - Display list of available exercises from ExerciseViewModel
   - Allow search/filter by muscle group
   - Return selected exercise

2. Update WorkoutLoggerScreen:
   - Add FAB or button to trigger exercise addition
   - Position after the last exercise in the list

3. Update WorkoutLoggerViewModel:
   - Add function: addExerciseToActiveWorkout(exerciseId: String)
   - Create LoggedExercise with empty sets
   - Update _activeWorkoutState

4. Ensure added exercises are properly saved when workout is finished
```

### 3. Exercise Substitution
```
Implement exercise substitution during workout:

1. Update LoggedExercise in DataModels.kt:
   - Ensure isSubstitute field is properly used
   - Add originalExerciseId: String? field

2. In MainActivity.kt LoggedSetRow:
   - Add substitute button/icon for each exercise
   - Show indicator when exercise is substituted

3. Create substitution flow:
   - Open ExerciseSelectionDialog
   - Replace exercise while preserving set structure
   - Mark as substituted

4. Update workout history to show substitutions clearly
```

### 4. Smart Pre-fill Implementation
```
Implement smart pre-fill to populate sets with previous performance:

1. Update LoggedWorkoutDao.kt:
   - Add query: getLastWorkoutWithExercise(exerciseId: String): LoggedWorkout?
   - Ensure efficient query with proper indices

2. Update WorkoutLoggerViewModel:
   - In startWorkoutFromTemplate, fetch previous performance
   - Pre-populate weight/reps in LoggedSet creation
   - Store as "suggested" values, not actual

3. Update UI in LoggedSetRow:
   - Show previous performance as hint/placeholder
   - Use different styling for pre-filled vs entered data
   - Allow easy override

4. Consider user's weight unit preference when pre-filling
```

### 5. Historical Workout Editing
```
Add ability to edit completed workouts:

1. In HistoryDetailScreen (MainActivity.kt):
   - Add Edit FAB or button in the top bar
   - Create editMode state

2. Implement edit flow:
   - Navigate to WorkoutLogger with existing workout data
   - Add "isEditing" parameter to WorkoutLoggerScreen
   - Modify title to show "Edit Workout"

3. Update WorkoutLoggerViewModel:
   - Add loadExistingWorkout(workoutId: String) function
   - Modify finishWorkout to handle updates vs inserts
   - Recalculate PRs if performance changed

4. Ensure data integrity and PR recalculation
```

## Bug Fix Prompts

### Timer State Persistence
```
The rest timer loses state on configuration change. Fix by:
1. Save timer state in SavedStateHandle in ViewModel
2. Restore on recomposition
3. Test with screen rotation
```

### Database Migration
```
Prepare database migration for new fields:
1. Increment database version in WorkoutDatabase.kt
2. Add migration strategy for existing data
3. Ensure null safety for new optional fields
4. Test with existing app data
```

## Code Review Prompts

### Before Each Feature
```
Before implementing [FEATURE NAME], please:
1. Review the current implementation in affected files
2. Identify potential impacts on existing functionality
3. Suggest the implementation approach
4. Highlight any concerns or considerations
```

### After Implementation
```
Please review the implementation of [FEATURE NAME]:
1. Check for potential bugs or edge cases
2. Verify backward compatibility
3. Ensure consistent code style
4. Suggest any optimizations
5. Confirm all TODOs from README.md are addressed
```

## Best Practices Reminder
```
For this implementation, ensure:
- All database operations use Dispatchers.IO
- Proper null safety throughout
- Input validation for user-entered data
- Consistent Material 3 theming
- Proper state management with StateFlow
- Clear user feedback for actions
```