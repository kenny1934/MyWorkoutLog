# State of the repo

This is the single source of truth for what works, what is known-broken, and what is unfinished. Update it when reality changes. If any other doc contradicts this one, that doc is wrong.

Last updated: 2026-04-19 (Phase 4 slice 6 landed; per-set target weight on TemplateExerciseSet / LoggedSet, v23 → v24).

## Next session — start here

As of 2026-04-18 the Linux Android SDK is installed, `./gradlew assembleDebug` is green, the subpackage restructure has landed, and all four monolith splits are done: `DashboardScreen.kt` 2,612 → 995, `ProgramManagementScreens.kt` 2,091 → 461, `HistoryScreens.kt` 1,808 → 481, and `WorkoutLoggerScreens.kt` 1,618 → 766 (split into `WorkoutLoggerSetRow.kt` 381, `WorkoutLoggerDialogs.kt` 363, `WorkoutLoggerTimer.kt` 163). The Phase 3 `WorkoutLoggerViewModel` tests pinned the timer/edit/resume state transitions before the split, and unit tests remain green after.

**Phase 3 kick-off landed 2026-04-18:** 20 JVM unit tests across `ActiveCycleViewModel` (4), `HistoryViewModel` (5), and `WorkoutLoggerViewModel` (11) pin the fragile timer/edit/resume flows, cycle UUID generation, and history cycle filtering. A Room `MigrationTestHelper` smoke test for schema v21 is committed under `app/src/androidTest/` and has been exercised on device via `./gradlew :app:connectedDebugAndroidTest` — it runs cleanly over adb-over-Tailscale once duplicate device entries (e.g. the mDNS discovery of the same phone) are disconnected.

**Phase 4 slice 1 + cleanup landed 2026-04-18** (two commits pushed, device-verified):
- `ee5d915` — slice 1. `util/CycleProgress.kt` adds a pure `cycleProgress(ActiveProgramCycle)` helper returning `CycleProgressInfo` (ordered weeks, current week + index, next session, completed/total session counts, start date, planned end date = `startDate + weeks.size` weeks, `isComplete` flag). 8 JVM tests in `util/CycleProgressTest.kt`. `ui/DashboardWidgetCards.kt::SimpleCycleProgressWidgetCard` consumes the helper — the live "Cycle Progress" widget now shows a "Started … · Planned end …" line and the primary button reads `Start <next session name>` with a correct cycleId/weekId/sessionId/templateId route. No schema change.
- `c348f75` — cleanup. `LegacyDashboardScreen`, `ActiveCycleDashboard`, `NoActiveCycleDashboard`, and the whole `ui/DashboardCycleViews.kt` file were unreachable in the current `MainActivity` wiring (`dashboardViewModel` is always non-null). Deleted. `DashboardScreen` now just wraps `EnhancedDashboardScreen` and takes only the two params it actually needs. In `data/WidgetRepositorySimplified.kt`, `calculateBasicCycleProgress` / `calculateCycleProgressText` duplicated the helper's logic — replaced with one `cycleProgress(activeCycle)` call, preserved the existing widget text format, deleted both private functions. Net -310 LOC, no behavior change.

**Phase 4 slice 2 landed 2026-04-18** (device-verified):
- `ProgramWeekDefinition` gains `isDeloadWeek: Boolean = false` (last field, defaulted). The field lives inside the JSON blob stored in `program_template_table.weeks` (and inside the `cycleProgram` snapshot on `active_program_cycle_table`), so no SQL column was added. Gson instantiates via `Unsafe`, which sets missing primitive booleans to `false` — matching the Kotlin default — so old stored cycles read cleanly.
- **First real Room migration.** DB version bumped 21 → 22. `WorkoutDatabase.MIGRATION_21_22` is a no-op `Migration` (schema unchanged apart from a user_version bump) registered via the existing `MIGRATIONS` array. Legacy destructive fallback is still scoped to v1–20 only. Schema JSON exported at `app/schemas/com.kennychiu.myworkoutlog.data.WorkoutDatabase/22.json` — byte-identical to `21.json` except the version field.
- **Migration test extended.** `WorkoutDatabaseMigrationTest.migrate21To22()` calls `runMigrationsAndValidate(dbName, 22, true, MIGRATION_21_22)`. Both that test and the pre-existing `canOpenSchemaAtVersion21` pass on device via `./gradlew :app:connectedDebugAndroidTest`.
- **Packaging gotcha.** `app/build.gradle.kts` now excludes `META-INF/LICENSE.md` and `META-INF/LICENSE-notice.md` from packaging. Without this, six JUnit5 jars (pulled in transitively by `mockk-android`) collided and broke the androidTest APK build. Pre-existing landmine; surfaced when the instrumented test suite was first actually packaged.
- **Program editor UI.** A "Deload week" `FilterChip` sits under the week label in both `ProgramEditorScreen` and `EnhancedProgramEditor` week cards. Tap to toggle; persists on Save.
- **Dashboard widget UI.** `SimpleCycleProgressWidgetCard` shows a "Deload" badge next to the "Cycle Progress" title when `cycleProgress(cycle).currentWeek?.isDeloadWeek == true`. Uses `tertiaryContainer` / `onTertiaryContainer` theming.
- JVM test count unchanged at 28; instrumented test count now 2.

**Phase 4 slice 3 landed 2026-04-18** (device-verified):
- `ProgramWeekDefinition` gains `targetRir: String? = null` (last field, defaulted). Freeform string — accepts "3", "2-3", "RPE 7-8", etc. Like `isDeloadWeek`, it lives inside the JSON blob in `program_template_table.weeks` and the `cycleProgram` snapshot on `active_program_cycle_table`, so no SQL column was added. Old stored cycles read back as `targetRir = null`.
- **Second real Room migration.** DB version bumped 22 → 23. `MIGRATION_22_23` is another no-op (schema unchanged apart from `user_version`) added to the `MIGRATIONS` array. Schema exported at `app/schemas/com.kennychiu.myworkoutlog.data.WorkoutDatabase/23.json`, byte-identical to `22.json` except the version field.
- **Migration test extended.** `WorkoutDatabaseMigrationTest.migrate22To23()` calls `runMigrationsAndValidate(dbName, 23, true, MIGRATION_22_23)`. All three instrumented tests (`canOpenSchemaAtVersion21`, `migrate21To22`, `migrate22To23`) pass on device.
- **Program editor UI.** Each week card now has a `Row` with the "Deload week" `FilterChip` on the left and a "Target RIR" `OutlinedTextField` taking the remaining width. Blank input persists as `null`. Implemented in both `ProgramEditorScreen` and `EnhancedProgramEditor` in `ui/ProgramEditorScreen.kt`.
- **Dashboard widget UI.** `ui/DashboardWidgetCards.kt::SimpleCycleProgressWidgetCard` shows an "RIR X" badge (secondaryContainer / onSecondaryContainer) next to the "Cycle Progress" title when `info.currentWeek?.targetRir` is non-blank. Renders alongside the existing Deload badge when both are set.
- JVM test count unchanged at 28; instrumented test count 2 → 3.

**Phase 4 slice 4 landed 2026-04-18** (build-verified, no schema change):
- `WorkoutLoggerScreen` now matches the active workout's `programWeekDefinitionId` against `activeCycle.cycleProgram.weeks` (gated on `cycleUuid == activeProgramCycleId`) and exposes the resulting `ProgramWeekDefinition` as `currentCycleWeek`.
- New private `CycleContextBanner` composable at the bottom of `ui/WorkoutLoggerScreens.kt`. Renders as the first `LazyColumn` item in the compact layout when the current week has `isDeloadWeek == true` or a non-blank `targetRir`. Shows the week label plus "Deload" (`tertiaryContainer`) and/or "RIR X" (`secondaryContainer`) badges — same theming as the dashboard widget so the two surfaces agree visually.
- Skipped for the master-detail (`shouldUseWorkoutMasterDetail()`) layout for now — phone usage is the primary gym flow.
- No data/schema change; reads existing fields added in slices 2/3. JVM tests still 28.

**Phase 4 slice 6 landed 2026-04-19** (build + JVM tests green; third real Room migration):
- `TemplateExerciseSet` gains `targetWeight: String? = null` as its last field. Freeform (e.g. "60", "60-65", "BW+20"). Null = no prescribed load. Lives inside the JSON blob in `workout_template_table.templateExercises` — no SQL column.
- `LoggedSet` gains a matching `targetWeight: String? = null` for set-level snapshotting, consistent with the existing `targetReps` / `targetSecs` snapshot pattern. Lives inside the JSON blob in `logged_workout_table.loggedExercises`.
- **Third real Room migration.** DB version 23 → 24. `WorkoutDatabase.MIGRATION_23_24` is another no-op (only the JSON shape changed), registered in the `MIGRATIONS` array. Schema exported at `app/schemas/com.kennychiu.myworkoutlog.data.WorkoutDatabase/24.json` — byte-identical to `23.json` except the version field.
- **Migration test extended.** `WorkoutDatabaseMigrationTest.migrate23To24()` calls `runMigrationsAndValidate(dbName, 24, true, MIGRATION_23_24)`. All four instrumented tests (`canOpenSchemaAtVersion21`, `migrate21To22`, `migrate22To23`, `migrate23To24`) wire through the chained `MIGRATIONS` array.
- **Template editor UI.** `TemplateSetEditorRow` in `ui/TemplateManagementScreens.kt` now has three fields per row: Reps / Secs / Weight. Entering text in Weight no longer nulls the Reps/Secs fields (unlike Reps/Secs which are mutually exclusive). Blank input persists as `null`. `formatSetTarget` now suffixes " @ XXkg" to the per-set summary line in the template detail card when a target weight is set.
- **Workout logger UI.** `LoggedSetRow` in `ui/WorkoutLoggerSetRow.kt` shows the target weight as a placeholder (`"→ 60"`) on the Weight `OutlinedTextField` when `set.targetWeight` is non-blank. Placeholder disappears once the user starts typing — doesn't interfere with the existing smart-pre-fill chip or the debounced auto-save.
- **Snapshot.** `WorkoutLoggerViewModel` copies `templateSet.targetWeight` into the newly-created `LoggedSet` at template load (line ~317). The ad-hoc "add exercise" and "add set" sites default to `null` (freeform string, no meaningful default).
- JVM test count unchanged at 36; instrumented test count 3 → 4.

**Phase 4 slice 5b landed 2026-04-18** (build + JVM tests green, no schema change):
- New `util/CycleAggregates.kt` — pure helper taking `ActiveProgramCycle`, `List<LoggedWorkout>`, and `List<PersonalRecord>`, returning per-week totals (workout count, set count, total volume, total duration in ms), PRs attached to their `programWeekDefinitionId`, and the most-common `performedWeightUnit` across the cycle's workouts. Volume convention matches `AnalyticsRepository.calculateTotalWorkoutVolume` — sum `(weight ?: 0.0) * (reps ?: 0)` across every logged set, ignoring unit. Mixed-unit cycles are a fluke in practice but the per-cycle `weightUnit` is surfaced so the UI picks a sensible label.
- New `viewmodel/CycleDetailViewModel.kt` — combines `activeCycleDao.getActiveCycle()` with `loggedWorkoutDao.getWorkoutsByCycle(cycleUuid)` and `personalRecordDao.getAllPRs()` via `flatMapLatest` + `combine`, exposes a single `StateFlow<CycleDetailUiState>`. Registered in `AppContainer.cycleDetailViewModelFactory()`, held by `MainActivity`, threaded through `MainApp` / `AppNavHost` and the `Screen.CycleDetail` composable.
- `ui/CycleDetailScreen.kt` swapped off `ActiveCycleViewModel` — now takes `CycleDetailViewModel`. Each week card renders a 3-up row of chips (Sets / Volume / Time) under the week label when the cycle has logged workouts for that week; time shows `Xh Ym` or `Ym`. A new "PRs this cycle" card renders above the week list when there are any, with a count badge and clickable rows that route to `Screen.HistoryDetail(loggedWorkoutId)`. Nothing changes about the existing session-row routing (completed → HistoryDetail, pending → TemplateDetail).
- 8 new JVM tests in `util/CycleAggregatesTest.kt`: empty list; workouts outside the cycle filtered by `cycleUuid`; per-week totals sum correctly; missing `programWeekDefinitionId` drops a workout from aggregates; null / zero-width timestamps contribute no duration; PRs attached to their week and sorted by date desc; most-common `weightUnit` wins; all-null weight units yield `null`. JVM test count 28 → 36.

**Phase 4 slice 5a landed 2026-04-18** (build-verified, no schema change):
- New `ui/CycleDetailScreen.kt`: read-only detail view of the currently active program cycle. Scaffold + back nav, header card (user cycle name, program name, start/planned-end, linear progress bar with "done / total" label, "Cycle complete" chip when finished), and a `LazyColumn` of week cards ordered by `week.order`.
- Each week card shows the week label, a "Current" pill for `cycleProgress().currentWeek`, plus "Deload" and "RIR X" badges (matches dashboard widget + logger banner theming). Session rows underneath show a filled `CheckCircle` when `completedSessions["${weekId}_${sessionId}"]` is set, else an outlined `RadioButtonUnchecked`.
- New `Screen.CycleDetail` route (no args) wired in `AppNavigation.kt`. `MainActivity` composable entry calls `CycleDetailScreen(activeCycleViewModel, onNavigateUp)`.
- Dashboard widget `SimpleCycleProgressWidgetCard` is now clickable — tapping anywhere on the card navigates to `Screen.CycleDetail`. The existing "Start <next session>" button still works since it lives in its own onClick.
- Sub-slice 5b (PRs hit during cycle + per-week volume/duration aggregates) deferred — current data flow already supports this via `personalRecordDao` + `loggedWorkoutDao.getByCycleId`, but the UI wiring is its own slice.
- Two follow-up fixes shipped on top (`e2b6bad`, `7d0311b`):
  - Session rows are tappable. Completed → `Screen.HistoryDetail(workoutId)`; pending → `Screen.TemplateDetail(templateId)` (review-only, does NOT start a workout — "Start next session" on the dashboard widget remains the single entry point to actually begin one).
  - Nested Scaffold was reserving the bottom system-nav inset that `MainActivity`'s outer Scaffold already consumes via its bottom nav bar, leaving a blank strip above the nav. Fixed with `contentWindowInsets = WindowInsets(0)` on the inner Scaffold. This is the canonical fix pattern for any inner Scaffold added inside `AppNavHost` screens.

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

3. ~~**Destructive DB migration.**~~ Resolved. Schema is at v22; destructive fallback is scoped to legacy dev versions 1–20 only via `fallbackToDestructiveMigrationFrom(*LEGACY_DEV_VERSIONS)`. `MIGRATION_21_22` is the first real migration. Further schema changes must add a new `Migration` object and extend `WorkoutDatabaseMigrationTest`.

4. ~~Placeholder `applicationId`.~~ Renamed from `com.example.myworkoutlog` to `com.kennychiu.myworkoutlog`.

## Structural issues (code health)

1. ~~**Flat package.**~~ Fixed. Files now live under `data/`, `ui/`, `viewmodel/`, `util/` subpackages. `MainActivity`, `WorkoutApplication`, `AppContainer` stay at the root because `AndroidManifest.xml` references them as `.Name`. Every subpackage file has star imports for the other three to cover cross-package references — a follow-up pass can tighten to specific imports if desired.

2. **Monolithic screen files.** All four done. `DashboardScreen.kt` is now 995 lines (down from 2,612) after extracting `DashboardWidgetCards.kt` (1,149), `DashboardChartCards.kt` (358), `DashboardCycleViews.kt` (150). `ProgramManagementScreens.kt` is now 461 lines (down from 2,091) after extracting `ProgramListScreen.kt` (263), `ProgramEditorScreen.kt` (762, includes `SessionCard` and `EnhancedProgramEditor`), `ProgramDetailViews.kt` (316), `ProgramCardsAndDialogs.kt` (359). `HistoryScreens.kt` is now 481 lines (down from 1,808) after extracting `HistoryComponents.kt` (581, shared components + helpers), `HistoryCycleViews.kt` (543), `HistoryDetailScreen.kt` (263). `WorkoutLoggerScreens.kt` is now 766 lines (down from 1,618) after extracting `WorkoutLoggerSetRow.kt` (381, the `LoggedSetRow` composable), `WorkoutLoggerDialogs.kt` (363, add-exercise / exercise-context-menu / substitute-exercise / duration-edit dialogs), `WorkoutLoggerTimer.kt` (163, `TimerBar` + duration parse/format helpers). Every extracted function was already package-level — no visibility bumps needed.

3. **Manual DI duplication.** `MainActivity` wires ~14 ViewModel factories with the same `(application as WorkoutApplication).database.xDao()` pattern repeated. Should be centralized in an `AppContainer`.

4. **Test coverage is still thin but no longer zero.** The wizard defaults were removed 2026-04-18. There are now 36 JVM unit tests (20 ViewModel + 8 for `CycleProgress` + 8 for `CycleAggregates`) plus three instrumented migration tests (v21 open, v21→22, v22→23). Coverage targets the known-fragile areas: workout timer + edit/resume, active cycle UUID flow, history cycle filtering, and cycle progress derivation. Everything else (dashboard widgets, PRs, import/export, cloud backup, volume, analytics) is still validated only by running the app.

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
  - Done (2026-04-18, slice 1 + follow-up cleanup): current week / next session / planned end date live on the dashboard widget via the shared `cycleProgress()` helper. Legacy dashboard path deleted; cycle-progress calculations deduped.
  - Done (2026-04-18, slice 2): deload-week flag on `ProgramWeekDefinition`, first real `Migration(21, 22)`, program-editor toggle, dashboard widget "Deload" badge, instrumented migration test. See the slice 2 section above.
  - Done (2026-04-18, slice 3): per-week `targetRir` on `ProgramWeekDefinition`, second real `Migration(22, 23)`, program-editor TextField, dashboard widget "RIR X" badge, instrumented migration test extended. See the slice 3 section above.
  - Done (2026-04-18, slice 4): cycle context banner at the top of the workout logger (compact layout). Surfaces deload flag + target RIR from the current cycle week.
  - Done (2026-04-18, slice 5a): read-only cycle-detail screen with week-by-week breakdown and session completion state. Tappable from the dashboard widget.
  - Done (2026-04-18, slice 5b): per-week aggregates (sets / volume / duration) + PRs-hit-this-cycle card on the cycle-detail screen. New `CycleDetailViewModel` combines cycle + logged workouts + PRs. Pure `util/CycleAggregates.kt` with 8 JVM tests.
  - Done (2026-04-19, slice 6): per-set `targetWeight` on `TemplateExerciseSet` + `LoggedSet`. Third real Room migration (v23 → v24, no-op). Template editor gets a Weight field per set; workout logger shows it as a placeholder hint on the Weight input.
  - Further candidates: mirror the slice 4 banner into the master-detail logger layout; per-exercise progression scheme (linear / double / RPE-based — separate from the per-set target).

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
