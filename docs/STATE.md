# State of the repo

This is the single source of truth for what works, what is known-broken, and what is unfinished. Update it when reality changes. If any other doc contradicts this one, that doc is wrong.

Last updated: 2026-04-18 (during cleanup from 7-month stall).

## What works

These features exist in code and appear to be functional based on the screen and ViewModel implementations. They have not been re-verified by running the app during this cleanup pass.

- **Exercise library**: CRUD, muscle-group tagging, bodyweight flag (`ExerciseManagementScreens.kt`)
- **Workout templates**: CRUD, set targets (`TemplateManagementScreens.kt`)
- **Workout logging**: set entry with reps/weight/RIR/bands/notes, rest timer, video reference attachment (`WorkoutLoggerScreens.kt`)
- **History**: completed workout list, detail view, edit, delete (`HistoryScreens.kt`)
- **Personal records**: automatic detection on save (`PrService.kt`, `PersonalRecordsScreen.kt`)
- **Volume analysis**: per-muscle-group charts (`VolumeAnalysisScreen.kt`)
- **Analytics**: per-exercise progression (`AnalyticsScreen.kt`)
- **Dashboard**: widget grid with customization mode (`DashboardScreen.kt`)
- **Settings**: units, bodyweight, preferences (`SettingsScreen.kt`)
- **Export / import**: JSON and CSV (`ExportScreen.kt`, `ImportScreen.kt`)
- **Cloud backup**: Google Drive (`CloudBackupScreen.kt`, `GoogleDriveCloudProvider.kt`)
- **Bodyweight tracking**: standalone entries (`BodyweightEntryDialog.kt`, added 2026-01)

## What is known-broken

1. **Mesocycle / program management is incomplete.** A prior audit on the `origin/claude/evaluate-app-rewrite-Eqoyq` branch (`TECH_STACK_EVALUATION.md`) estimated this at ~40% complete. The data model exists (`ActiveProgramCycle`, `ProgramTemplate`); the UX for planning and navigating a mesocycle is the gap.

2. **Workout session timer + edit/resume.** The recent commit chain has repeatedly patched symptoms: negative timer values, frozen timer after editing, duplicate workout creation on edit-then-resume. Edge cases remain in state transitions between in-progress → edit → save → resume.

3. **Destructive DB migration.** `WorkoutDatabase` uses `fallbackToDestructiveMigration()`. Any schema change wipes all user data. Schema is at version 21 with no `Migration` objects defined. This must be fixed before further schema changes.

4. **Placeholder `applicationId`.** Still `com.example.myworkoutlog` from the Android Studio wizard default.

## Structural issues (code health)

1. **Flat package.** All 66 Kotlin files live directly in `com.example.myworkoutlog`. No `data/`, `ui/`, `viewmodel/` subpackages.

2. **Monolithic screen files.** `DashboardScreen.kt` is 2,612 lines, `ProgramManagementScreens.kt` 2,088, `HistoryScreens.kt` 1,805, `WorkoutLoggerScreens.kt` 1,618. Each contains many Composables that should be separate files.

3. **Manual DI duplication.** `MainActivity` wires ~14 ViewModel factories with the same `(application as WorkoutApplication).database.xDao()` pattern repeated. Should be centralized in an `AppContainer`.

4. **No real tests.** `ExampleUnitTest.kt` and `ExampleInstrumentedTest.kt` are the wizard defaults. The codebase has no coverage — the recent history of "fix Room compilation" commits is visible symptomatic evidence that changes are being validated by running the app and hitting errors.

5. **Room DAO convention was unstable.** Recent commits flipped back and forth on `suspend` modifiers for `@Query` / `@Delete`. The current convention (see `CLAUDE.md`) is: suspend for writes, non-suspend for `Flow`/`LiveData` returns, non-suspend snapshot reads only where sync call sites require them.

6. **TODOs.** Three unresolved, all about reading weight unit from user preferences (`DashboardScreen.kt:2608`, `DashboardViewModel.kt:723`, `WorkoutLoggerScreens.kt:503`).

## Branch state

Master is 4 commits ahead of where the app was last actually developed (2025-09); those 4 commits were a later touch-up of bodyweight + timer. There are 5 other remote branches, most 7–10 months old:

- `origin/claude/evaluate-app-rewrite-Eqoyq` — **keep.** Contains the honest audit (`TECH_STACK_EVALUATION.md`) and a Next.js mockup exploring a rewrite direction. Historical reference.
- `origin/feature/dashboard-enhancements` — stale (9 months), check for salvage then delete.
- `origin/feature/enhanced-history-display` — stale (10 months), check for salvage then delete.
- `origin/feature/reorderable-library-migration` — stale (10 months), check for salvage then delete.
- `origin/feature/workout-logger-ui-improvements` — stale, this is what master was built on.

## Cleanup plan

- **Phase 0 — Truth reset (docs).** Done. Inflated docs removed (~14K lines); README and CLAUDE.md rewritten; this file is the new single source of truth.
- **Phase 1 — Build and DB safety.** Partially done.
  - Done: removed open-ended `fallbackToDestructiveMigration()`. `WorkoutDatabase` now declares an empty `MIGRATIONS` array, scopes destructive fallback to legacy dev versions 1–20 only, and enables schema export (`exportSchema = true` with `room.schemaLocation` via KSP). Any schema bump past v21 must add a `Migration` object.
  - Pending user input: rename `applicationId` off `com.example.myworkoutlog` (needs the user to pick a real package name), and prune the 4 stale remote feature branches (needs confirmation since remote branch deletion is destructive to shared git state).
- **Phase 2 — Structural cleanup.** Partially done.
  - Done: manual DI extracted into `AppContainer`. `MainActivity` dropped from ~160 lines of repeated factory wiring to 14 one-liners. `WorkoutApplication` exposes `container`; all DAO/repository/factory construction lives in one place.
  - Not started: package restructure into `data/ui/viewmodel/util` (bundled with the `applicationId` rename to avoid double-touching all 66 files); splitting the 4 monolithic screen files (`DashboardScreen.kt` etc.). Both require a working local build to verify — Android Studio on Windows is currently the only working build path.
- **Phase 3 — Tests for broken areas.** Not started. Room migration tests; ViewModel unit tests for workout logger timer, active cycle, history cycle filtering.
- **Phase 4 — Resume feature work.** Complete mesocycle / program management UX.

## Deleted during cleanup

For the record, these files were removed during Phase 0 because they were inflated, contradictory, or aspirational for a single-user hobby project:

- `MISSING_FEATURES.md`
- `docs/NEXT_TASK.md`, `docs/README.md`
- `docs/architecture/`, `docs/claude-code/`, `docs/contributing/`, `docs/development/`, `docs/features/`, `docs/legal/`, `docs/product/`, `docs/technical/`
- Root-level `package-lock.json` (not an Android concern)
- Root-level `jbr` symlink (machine-specific)
- `.claude-context` (superseded by `CLAUDE.md`)
- `sample_calisthenics_data.json` moved into `samples/`

If any of that content was actually useful it still exists in git history.
