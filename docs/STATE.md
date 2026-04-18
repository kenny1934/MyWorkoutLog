# State of the repo

This is the single source of truth for what works, what is known-broken, and what is unfinished. Update it when reality changes. If any other doc contradicts this one, that doc is wrong.

Last updated: 2026-04-18 (during cleanup from 7-month stall).

## Next session — start here

As of 2026-04-18 the Linux Android SDK is installed, `./gradlew assembleDebug` is green, the subpackage restructure has landed, and all four monolith splits are done: `DashboardScreen.kt` 2,612 → 995, `ProgramManagementScreens.kt` 2,091 → 461, `HistoryScreens.kt` 1,808 → 481, and `WorkoutLoggerScreens.kt` 1,618 → 766 (split into `WorkoutLoggerSetRow.kt` 381, `WorkoutLoggerDialogs.kt` 363, `WorkoutLoggerTimer.kt` 163). The Phase 3 `WorkoutLoggerViewModel` tests pinned the timer/edit/resume state transitions before the split, and unit tests remain green after.

**Phase 3 kick-off landed 2026-04-18:** 20 JVM unit tests across `ActiveCycleViewModel` (4), `HistoryViewModel` (5), and `WorkoutLoggerViewModel` (11) pin the fragile timer/edit/resume flows, cycle UUID generation, and history cycle filtering. A Room `MigrationTestHelper` smoke test for schema v21 is committed under `app/src/androidTest/` but requires an emulator or device to run — run it from Windows Android Studio.

The SDK setup section below is kept as a reference for reinstalling on a fresh machine.

### 1. Install Linux-side Android SDK (reference — already done on this machine)

```bash
sudo apt-get update && sudo apt-get install -y unzip
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
cd ~

# Persist env (idempotent)
grep -q ANDROID_HOME ~/.bashrc || cat >> ~/.bashrc <<'EOF'
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
EOF
source ~/.bashrc

# Install the exact versions app/build.gradle.kts declares
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Use JDK 17 (`/usr/lib/jvm/java-17-openjdk-amd64`) — it has the full toolchain including `javac`. The `java-21-openjdk-amd64` package installed on this machine is JRE-only and Gradle rejects it with "does not provide the required capabilities: [JAVA_COMPILER]". Export `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` before running `./gradlew`.

### 2. Verify the 4 unpushed commits build

```bash
cd /home/kenny/projects/MyWorkoutLog
./gradlew assembleDebug
```

If green: `git push origin master`. If not: fix the failures before continuing. Expected potential issues:
- Missed reference to the old `com.example.myworkoutlog` package somewhere not caught by the sed pass (shouldn't happen, but possible).
- Room schema JSON generation for v21 will write to `app/schemas/com.kennychiu.myworkoutlog.WorkoutDatabase/21.json` on first compile — commit that file afterwards.

### 3. Deploying to device (once build works)

USB device via `usbipd-win` on Windows is usually easier than emulator-in-WSL. Or enable wireless debugging on the device and `adb connect <ip>:<port>` from WSL. The canonical gym-use setup here is a real device, so this only needs to work enough to run the app manually and watch `adb logcat`.

### 4. Resume the cleanup plan

See the **Cleanup plan** section below. The next items in order are: Phase 2 monolith splits, Phase 2 subpackage restructure, Phase 3 tests, Phase 4 mesocycle UX (the actual unfinished feature).

---

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

4. ~~Placeholder `applicationId`.~~ Renamed from `com.example.myworkoutlog` to `com.kennychiu.myworkoutlog`.

## Structural issues (code health)

1. ~~**Flat package.**~~ Fixed. Files now live under `data/`, `ui/`, `viewmodel/`, `util/` subpackages. `MainActivity`, `WorkoutApplication`, `AppContainer` stay at the root because `AndroidManifest.xml` references them as `.Name`. Every subpackage file has star imports for the other three to cover cross-package references — a follow-up pass can tighten to specific imports if desired.

2. **Monolithic screen files.** All four done. `DashboardScreen.kt` is now 995 lines (down from 2,612) after extracting `DashboardWidgetCards.kt` (1,149), `DashboardChartCards.kt` (358), `DashboardCycleViews.kt` (150). `ProgramManagementScreens.kt` is now 461 lines (down from 2,091) after extracting `ProgramListScreen.kt` (263), `ProgramEditorScreen.kt` (762, includes `SessionCard` and `EnhancedProgramEditor`), `ProgramDetailViews.kt` (316), `ProgramCardsAndDialogs.kt` (359). `HistoryScreens.kt` is now 481 lines (down from 1,808) after extracting `HistoryComponents.kt` (581, shared components + helpers), `HistoryCycleViews.kt` (543), `HistoryDetailScreen.kt` (263). `WorkoutLoggerScreens.kt` is now 766 lines (down from 1,618) after extracting `WorkoutLoggerSetRow.kt` (381, the `LoggedSetRow` composable), `WorkoutLoggerDialogs.kt` (363, add-exercise / exercise-context-menu / substitute-exercise / duration-edit dialogs), `WorkoutLoggerTimer.kt` (163, `TimerBar` + duration parse/format helpers). Every extracted function was already package-level — no visibility bumps needed.

3. **Manual DI duplication.** `MainActivity` wires ~14 ViewModel factories with the same `(application as WorkoutApplication).database.xDao()` pattern repeated. Should be centralized in an `AppContainer`.

4. **Test coverage is still thin but no longer zero.** The wizard defaults were removed 2026-04-18. There are now 20 ViewModel unit tests plus a Room v21 migration smoke test. Coverage targets the known-fragile areas: workout timer + edit/resume, active cycle UUID flow, and history cycle filtering. Everything else (dashboard, PRs, import/export, cloud backup, volume, analytics) is still validated only by running the app.

5. **Room DAO convention was unstable.** Recent commits flipped back and forth on `suspend` modifiers for `@Query` / `@Delete`. The current convention (see `CLAUDE.md`) is: suspend for writes, non-suspend for `Flow`/`LiveData` returns, non-suspend snapshot reads only where sync call sites require them.

6. **TODOs.** Three unresolved, all about reading weight unit from user preferences (`ui/DashboardScreen.kt:2608`, `viewmodel/DashboardViewModel.kt:723`, `ui/WorkoutLoggerScreens.kt:503`).

## Branch state

Only one non-master branch remains on the remote:

- `origin/claude/evaluate-app-rewrite-Eqoyq` — **keep.** Contains the honest audit (`TECH_STACK_EVALUATION.md`) and a Next.js mockup exploring a rewrite direction. Historical reference.

The four stale `feature/*` branches (dashboard-enhancements, enhanced-history-display, reorderable-library-migration, workout-logger-ui-improvements) were deleted during cleanup.

## Cleanup plan

- **Phase 0 — Truth reset (docs).** Done. Inflated docs removed (~14K lines); README and CLAUDE.md rewritten; this file is the new single source of truth.
- **Phase 1 — Build and DB safety.** Partially done.
  - Done: removed open-ended `fallbackToDestructiveMigration()`. `WorkoutDatabase` now declares an empty `MIGRATIONS` array, scopes destructive fallback to legacy dev versions 1–20 only, and enables schema export (`exportSchema = true` with `room.schemaLocation` via KSP). Any schema bump past v21 must add a `Migration` object.
  - Done: renamed `applicationId` / namespace to `com.kennychiu.myworkoutlog` and moved all source files accordingly.
  - Pending: prune the 4 stale remote feature branches.
- **Phase 2 — Structural cleanup.** Partially done.
  - Done: manual DI extracted into `AppContainer`. `MainActivity` dropped from ~160 lines of repeated factory wiring to 14 one-liners. `WorkoutApplication` exposes `container`; all DAO/repository/factory construction lives in one place.
  - Done (2026-04-18): package restructure into `data/ui/viewmodel/util`. 61 files moved, every cross-package file gets star imports for the other three subpackages. Build verified green.
  - Done (2026-04-18): split `DashboardScreen.kt` (2,612 → 995) into `DashboardWidgetCards.kt`, `DashboardChartCards.kt`, `DashboardCycleViews.kt`. Build verified green.
  - Done (2026-04-18): split `ProgramManagementScreens.kt` (2,091 → 461) into `ProgramListScreen.kt`, `ProgramEditorScreen.kt`, `ProgramDetailViews.kt`, `ProgramCardsAndDialogs.kt`. Bumped 7 private composables to package-level so callers across files can reach them. Build verified green.
  - Done (2026-04-18): split `HistoryScreens.kt` (1,808 → 481) into `HistoryComponents.kt`, `HistoryCycleViews.kt`, `HistoryDetailScreen.kt`. Bumped `WorkoutSummaryCard`, `ProgramContextCard`, `formatTimestampToTime`, `formatHistoryWeight` from private to package-level; other helpers kept file-scoped with their only callers. Build verified green.
  - Done (2026-04-18): split `WorkoutLoggerScreens.kt` (1,618 → 766) into `WorkoutLoggerSetRow.kt` (`LoggedSetRow`), `WorkoutLoggerDialogs.kt` (`AddExerciseToWorkoutDialog`, `ExerciseSelectorContent`, `ExerciseContextMenuDialog`, `SubstituteExerciseDialog`, `DurationEditDialog`), and `WorkoutLoggerTimer.kt` (`TimerBar` + `formatTime` / `formatSecondsToDisplay` / `parseDurationToSeconds` / `validateDurationInput`). All extracted functions were already package-level (no `private` modifier), so no visibility bumps were needed. The Phase 3 `WorkoutLoggerViewModelTest` suite was run before and after the split — no regressions. Build verified green.
- **Phase 3 — Tests for broken areas.** In progress.
  - Done (2026-04-18): test infra added (`kotlinx-coroutines-test`, `turbine`, `mockk`, `androidx.arch.core:core-testing`, `androidx.room:room-testing`). `MainDispatcherRule` helper under `app/src/test/`.
  - Done (2026-04-18): 20 JVM unit tests. `ActiveCycleViewModelTest` (4) covers the cycle UUID generation and start/end DAO flow. `HistoryViewModelTest` (5) pins that `activeCycleWorkouts` only emits workouts whose `activeProgramCycleId` matches the active cycle, that `completedCycles` groups and sorts correctly, and that orphaned-workouts semantics mean "no cycle id" rather than "from ended cycle". `WorkoutLoggerViewModelTest` (11) covers init cleanup, template-based fresh start, resume-existing-in-progress, force-fresh cleanup, edit mode load, edit-finish preserves id + startTimestamp + resets edit flags, new-workout finish flips `isInProgress=false` and clears state, cycle `completedSessions` gets `weekId_sessionId` → workoutId, cancel on new workout marks completed, cancel in edit mode does NOT mark completed, and `updateSet` only mutates the target set. Tests use real `Dispatchers.IO` with mockk `verify(timeout = …)` and a local `waitUntil` helper (the VM doesn't abstract dispatchers, so advancing a `TestScheduler` isn't enough).
  - Done (2026-04-18): `WorkoutDatabaseMigrationTest` at `app/src/androidTest/.../data/` uses `MigrationTestHelper` to confirm the v21 schema opens cleanly. It does not run under `./gradlew test` — it's an instrumented test. Run from Android Studio with a connected device/emulator. When the first real `Migration` is added, extend this file with a `runMigrationsAndValidate` case.
  - Not started: tests for other ViewModels (dashboard, PRs, analytics, export/import). Not gating.
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
