# Claude Development Context

## Current Status
- Branch: `feature/enhanced-history-display`
- Last commit: `feat: Replace cycle UUID display with user-defined cycle names`
- Ready to fix two critical bugs in the enhanced history display

## Active Bug Fixes (High Priority)

### Bug 1: Workout Count Discrepancy
**Issue:** Completed cycles view shows only 2 workouts when 3 were actually logged (verified in "All" tab)
**Investigation needed:**
- Check query/logic in `HistoryViewModel.kt` that counts workouts for completed cycles
- Compare with "All" tab logic to identify filtering differences
- Look for conditions excluding one workout from the count

### Bug 2: Incorrect Completion Percentage
**Issue:** Cycle shows "100% Complete" despite missing sessions (3/4 workouts logged)
**Investigation needed:**
- Examine completion percentage calculation logic
- Verify if it's properly counting actual vs planned sessions
- Check cycle completion status determination logic

### Investigation Areas
1. `HistoryViewModel.kt` - completion percentage and workout counting logic
2. `ActiveCycleDao.kt` - cycle completion queries and data access
3. `DataModels.kt` - cycle completion properties and calculations
4. History display components - where values are rendered

### Fix Strategy
1. Identify root cause through code analysis of counting/calculation logic
2. Fix workout counting to match "All" tab behavior consistently
3. Fix completion percentage to accurately reflect missing sessions
4. Test both fixes together for proper integration

## Development Commands
```bash
# Build and test
./gradlew build
./gradlew test

# Run app
./gradlew installDebug
```

## Next Steps
1. Investigate both bugs by examining the identified files
2. Implement fixes for accurate workout counting and completion percentage
3. Test thoroughly before committing changes
4. Continue with roadmap items after bug fixes are complete