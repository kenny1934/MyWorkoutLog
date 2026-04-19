# State of the repo

This is the single source of truth for what works, what is known-broken, and what is unfinished. Update it when reality changes. If any other doc contradicts this one, that doc is wrong.

Last updated: 2026-04-19 (Phase 5 slices 25–36 — UI/UX/perf overhaul: design tokens, ScreenScaffold + follow-ups, logger ergonomics, program-editor declutter, perf pass, dialogs → bottom sheets, dashboard first-impression, truncation sweep, dashboard CTA surfacing).

## Next session — start here

**Next task: TBD.** Slice 36 landed this session. Candidate follow-ups from the backlog:

- **Device verification** of the Phase 5 slices installed this session (CTA needs eyeballing on the Z Fold — inner + outer screens; make sure the new primaryContainer card reads cleanly against the existing dashboard grid and that the Deload/RIR badges don't push the session name off-screen on narrow widths). Verification backlog now spans slices 5b–36.
- **Promote-or-demote the old widget button.** The slice-36 CTA keeps the original ElevatedButton inside `SimpleCycleProgressWidgetCard` as a fallback. With the CTA row above, the in-widget button is now visually redundant and competes for attention — could collapse it (leave only the "Analytics" / "Cycle complete" branch) once slice 36 has been lived with.
- **Design-token migration.** Most `.dp` / `fontSize = N.sp` literals remain across 30+ files — tokens are landed; screens migrate as they're touched.

---

**Phase 5 slice 36 landed 2026-04-19** (build + JVM tests green on retry; no schema change):

- **Dashboard CTA surfacing.** "Start next session" was buried 3 taps deep inside the expanded `SimpleCycleProgressWidgetCard`; the prior-session audit flagged it as the biggest dashboard UX miss. New `ui/DashboardWidgetCards.kt::NextSessionCtaCard(cycle, navController)` — primary-container `Card` with a square PlayArrow chip + "Start next session" label + session name + week label, plus Deload / Target-RIR badges mirroring the widget's existing treatment (tertiaryContainer / secondaryContainer). Click fires `Screen.WorkoutLogger.createRoute(templateId = nextSession.workoutTemplateId, cycleId = cycle.cycleUuid, weekId = nextWeek.id, sessionId = nextSession.id)`. Renders nothing when the active cycle has no current week / no next session — the `cycleProgress(cycle)` helper (slice 1) returns null for completed cycles.
- **Wired into both dashboard paths.** `ui/DashboardScreen.kt::EnhancedDashboardScreen` (compact `LazyColumn`) adds a new `item {}` between the header row and the error/loading branches, gated on `dashboardState.widgets.filterIsInstance<CycleProgressWidget>().firstOrNull()?.cycle.takeIf { cycleProgress(it).nextSession != null }`. Same gate in the `AdaptiveDashboardContent` tablet path — sits between the "Dashboard" title row and `AdaptiveWidgetGrid`. Sources the cycle from the existing `CycleProgressWidget` so no new VM plumbing / factory change is needed.
- **Widget fallback kept.** `SimpleCycleProgressWidgetCard`'s ElevatedButton is unchanged — still reads "Start <nextSession.sessionName>" in the expanded widget view. Two surfaces is fine for a hobby app; a dedicated slice can collapse the widget button once the CTA row has been lived with on-device.
- No new tests (pure UI wiring over the existing `cycleProgress` helper — already covered by `CycleProgressTest`). JVM count unchanged at 103.

**Phase 5 slices 34 + 35 landed 2026-04-19** (build + JVM tests green, commits pushed):

- `f42b1ec` slice 34 — **title + list-row text truncation.** Long user-entered names were clipping into icons / off-screen in several spots that never had `maxLines` / `overflow` set. TopAppBar titles with dynamic names (ScreenScaffold, WorkoutLoggerScreens, HistoryDetailScreen, TemplateManagementScreens TemplateDetailScreen, ProgramEditorScreen) now use `maxLines = 1, overflow = Ellipsis, modifier = Modifier.basicMarquee()` — the full name scrolls into view when it exceeds the bar's remaining width. List row names (ProgramListScreen program card, TemplateManagementScreens template card, HistoryComponents WorkoutCard header, AdaptiveWorkoutComponents ExerciseListItem master panel, ProgramEditorScreen template-picker DropdownMenuItem) use `maxLines = 1, overflow = Ellipsis` without marquee — a list of 20 simultaneously-scrolling titles would be visual chaos. Dialog bodies with interpolated names (Delete "X"? etc.) left as-is — they naturally wrap.

- `33dec0f` slice 35 — **second-pass truncation sweep.** Defensive pass catching sites the slice-34 audit missed. Same class of bug. `ProgramEditorScreen` SessionCard session + template names (weighted Column next to overflow menu), `DashboardWidgetCards` Performance-Trends top-exercise row (weighted Column next to trend Icon), `HistoryCycleViews` CycleCard + CycleCardMaster user-entered cycle names (next to Rename pencil), `HistoryCycleViews` ActiveCycleSection + master variant `programTemplateName`, `TemplateManagementScreens` TemplateDetailScreen exercise card header (18sp bold). Label-style lines in dialogs (identifier, not wrappable sentences): `WorkoutLoggerDialogs` "Exercise: <name>" + "Current: <name>" + "Substitute with: <name>", `ProgramEditorScreen` "Move '<sessionName>' to:" heading. Defensive caps where wrapping is correct but could run away: `ExerciseManagementScreens` AddExercise + EditExercise "Target Muscles: a, b, c, ..." joined list at `maxLines = 3`, `ProgramCardsAndDialogs` StartCycleSheet OutlinedTextField placeholder at `maxLines = 1`.

**Phase 5 slices 32 + 33 landed 2026-04-19** (build + JVM tests green, commits pushed):

- `6382b47` slice 32 — **inner TopAppBar double-top-inset fix.** MainActivity's outer `Scaffold` has no `topBar` and uses default `contentWindowInsets = WindowInsets.systemBars`, so `innerPadding.top` already reserves the status bar before `NavHost` content renders. Every inner `Scaffold`'s `TopAppBar` still carried its default `windowInsets = statusBars` and re-reserved that inset on top, leaving an extra status-bar-height of padding above each title. Zeroed `windowInsets = WindowInsets(0)` on all 10 inner `TopAppBar`s — `ScreenScaffold.kt` plus 7 hand-rolled Scaffolds (`CloudBackupScreen` × 2, `CycleDetailScreen`, `ExportScreen`, `HistoryDetailScreen`, `ImportScreen`, `ProgramEditorScreen`, `TemplateManagementScreens` TemplateDetailScreen, `WorkoutLoggerScreens`). Mirror of the existing `contentWindowInsets = WindowInsets(0)` pattern on those same Scaffolds for the bottom system-nav inset.

- `2a642b9` slice 33 — **master-detail screens → ScreenScaffold + back arrow.** Slice 26 only migrated single-column paths; the four master-detail variants were still `Row{masterCard + detailCard}` with no top bar and a hand-rolled title inside the master card. Wrapped each in `ScreenScaffold(title, onNavigateUp)`, hoisted the Add affordance into the `TopAppBar` `actions` slot where present, and dropped the redundant internal title: `ExerciseManagementMasterDetailView` (+ Add exercise in actions), `TemplateManagementMasterDetailView` (+ Create template in actions), `PersonalRecordsMasterDetailView` (no add; title + back only; `items(exerciseGroups)` now keyed by `exerciseId`), and `ProgramMasterDetailLayout` (title matches the single-column path; bottom "Create New Program" Button stays — text-labeled, more prominent than an overflow icon). `AdaptiveProgramManagementScreen` now threads `onNavigateUp` through to the master-detail layout as well as the single-column one. Each master-detail body picks up the slice-32 top-inset fix automatically.

**Phase 5 slices 25–31 landed 2026-04-19** (build + JVM tests green at each slice, all 7 commits pushed):

- `4729208` slice 25 — **design tokens.** `ui/theme/Dimens.kt` (spacing4…spacing64, icon sizes, elevations, touch target). `ui/theme/Shapes.kt` wired into `MaterialTheme.shapes`. `ui/theme/Type.kt` expanded from bodyLarge-only to the full M3 scale (15 slots). `ui/theme/ExtendedColors.kt` — semantic success/warning/info/accent tokens + 8-colour chart palette behind `LocalExtendedColors` / `MaterialTheme.extendedColors`, light + dark variants. `Theme.kt` provides them via `CompositionLocalProvider`. `SettingsScreen.kt` migrated as the reference pattern; subsequent slices pick up tokens opportunistically as screens are touched.

- `d3e8df5` slice 26 — **ScreenScaffold + 6-screen migration.** New `ui/ScreenScaffold.kt` wraps `TopAppBar` (with optional back button) around screen content and includes the canonical `contentWindowInsets = WindowInsets(0)` fix for nested scaffolds. Migrated screens that hand-rolled `fontSize = 24.sp, Bold` headers: `LibraryScreen` (added icons + `LibraryHubCard` helper — no longer a list of 7 plain-text cards), `SettingsScreen`, `ManageProgramsScreen`, single-column `ManageExercisesScreen`, single-column `ManageTemplatesScreen`, single-column `PersonalRecordsScreen`. `MainActivity.AppNavHost` threads `onNavigateUp = { navController.navigateUp() }` to each. Master-detail variants still hand-roll internal headers — separate slice if we want to unify them.

- `73b0dc9` slice 27 — **logger ergonomics.** `WorkoutLoggerSetRow` — Weight/Reps/Secs/RIR now chain through the keyboard. Weight = `Decimal + Next`, Reps/Secs = `Number + Next`, RIR = `Number + Done` (clears focus). All four are `singleLine = true`. `focusManager.moveFocus(FocusDirection.Next)` advances through whichever fields render for this set (time-based sets skip Reps). `WorkoutLoggerTimer.TimerBar` fires `HapticFeedbackType.LongPress` the instant `currentTime` ticks from non-zero to zero — tracked via remembered prev-value, no VM-event plumbing. Phone is usually silent in the gym, so haptic is the only rest-over signal that reaches the user.

- `96e767f` slice 28 — **program editor declutter.** The week/session cards were a button museum. Each week had Column{DragHandle, ArrowUp, ArrowDown} + Duplicate + Delete; each session card had Column{DragHandle, ArrowUp, ArrowDown} + SwapHoriz + Edit + Delete. Drag handle alone covers ordering — arrows were noise. New private `ReorderableCollectionItemScope.WeekCardHeader` — drag handle + label field + single overflow menu (Duplicate / Delete); shared by compact `ProgramEditorScreen` and master-detail `EnhancedProgramEditor`. `SessionCard` signature shrinks by 4 params (`canMoveUp`, `canMoveDown`, `onMoveUp`, `onMoveDown`); arrow Column removed; Edit / SwapHoriz / Delete consolidated into one overflow menu. Underlying helpers (`moveWeek`, `moveSessionWithinWeek`, `duplicateWeekInto`, `moveSessionToWeek`) unchanged — pure UI cleanup.

- `c5e8994` slice 29 — **performance pass.** `VolumeAnalysisScreen.kt` — `sortedVolumeList` now wrapped in `remember(volumeData)` at both sites (was rebuilt every recomposition); the chart-update `LaunchedEffect` now keys on `volumeData` (was keyed on the unstable derived list, so chart state thrashed every frame). Same memoization for the master-detail `sortedVolumeList` + auto-select `LaunchedEffect`. `ProgramEditorScreen.kt` — `week.sessions.sortedBy { it.order }` now `remember(week.sessions)` at both editor sites; removes allocation jank during drag. `WorkoutLoggerDialogs.kt` — `filteredExercises` memoized via `remember(exercises, searchText)` (had to lift out of the LazyColumn scope to satisfy the @Composable-context rule). `InteractiveChartComponents.kt:1004` sparkline — killed the per-point `animateFloatAsState` inside `data.map { }` that allocated N animation objects per recomposition with no stable identity; Canvas now reads the raw list directly. Added `key = { it.id }` / `key = { it.cycleId }` to 13 hot-path `items()` calls that were missing them (workout logger exercises, history exercises + cycles + workouts, dashboard insights + quick actions, program list, template list, exercise list, cloud backup list).

- `7b39eef` slice 30 — **data-entry dialogs → bottom sheets.** AlertDialog is wrong for forms — small modal, wastes horizontal real estate, no graceful keyboard handling. `ProgramCardsAndDialogs.kt` — `CreateProgramDialog` / `StartCycleDialog` rewritten as `CreateProgramSheet` / `StartCycleSheet` using `ModalBottomSheet`. New `AddSessionSheet` replaces the AlertDialog block that was DUPLICATED in both `ProgramEditorScreen` and `EnhancedProgramEditor` (each with an embedded 150dp LazyColumn of templates); now shared, 300dp template list, shows "N exercises" per row, handles the no-templates case. Call sites updated in `ProgramManagementScreens`, `ProgramListScreen` (was inline — now uses the shared sheets), `ProgramDetailViews`, `ProgramEditorScreen` (both editors). Destructive Delete confirmations intentionally kept as AlertDialog — one-glance yes/no, not data entry.

- `aee1033` slice 31 — **dashboard first-impression.** Loading a blank dashboard with a single centered spinner was jarring; a fresh install showed that same spinner then an empty screen with no guidance. New `ui/DashboardPlaceholders.kt` with `DashboardSkeletonGrid` (three shimmer-animated placeholder cards, alpha pulses 0.3 ↔ 0.6 on a 900ms tween) and `DashboardEmptyState` (icon + headline + body + "Go to Library" CTA). `DashboardScreen.AdaptiveDashboardContent` and the compact `EnhancedDashboardScreen` path both dispatch isLoading-with-no-widgets → skeleton, !loading-with-no-widgets → empty state, otherwise grid.

### Still to do / not in Phase 5

- **Device verification.** Backlog now spans slices 5b–35. Slices 32 + 34 + 35 were installed this session (Tailscale adb over WSL) and the top-inset fix was eyeballed; the truncation pass is untested beyond "it builds and runs". Prior-session priorities still carry: drag-reorder after slice 28, bottom sheets (slice 30), logger IME chain (slice 27).
- **Dashboard CTA surfacing.** Audit flagged "Start next session" as being buried 3 taps deep inside an expandable widget. Slice 31 addressed loading/empty states but didn't promote the CTA — a dedicated slice could hoist the primary action out of the widget and into a persistent row at the top of the dashboard.
- **Design-token migration opportunistic.** Most `.dp` / `fontSize = N.sp` literals remain in place across 30+ files — tokens are landed, screens migrate as they're touched. No big-bang rewrite scheduled.

## Reference: earlier Phases 0–4 notes below

(Content below is the pre-Phase-5 history; kept for context.)

### Prior Phase 4 notes (through 2026-04-19, slice 24)

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

**Bottom-padding fix landed 2026-04-19** (build + JVM tests green, no schema change):
- The "nested-Scaffold double-reserves system-nav inset" bug documented in slice 5a was only patched on `CycleDetailScreen`. Every other `AppNavHost`-nested Scaffold had the same bug — content clipped above the bottom nav because the inner Scaffold re-reserved the system-nav inset that `MainActivity`'s outer Scaffold had already consumed. Kenny flagged it on the workout logger during an actual workout.
- Canonical fix (`contentWindowInsets = WindowInsets(0)`) applied to all remaining inner Scaffolds: `WorkoutLoggerScreens.kt:270`, `HistoryDetailScreen.kt:33`, `ExerciseManagementScreens.kt:52`, `ProgramListScreen.kt:39`, `CloudBackupScreen.kt:74 + :925`, `ImportScreen.kt:51`, `ExportScreen.kt:52`, `ProgramEditorScreen.kt:48`, `TemplateManagementScreens.kt:68 + :899`.
- Secondary fix on the compact WorkoutLogger `LazyColumn`: `.padding(paddingValues).padding(16.dp)` → `.padding(paddingValues)` with `contentPadding = PaddingValues(16.dp)`. This lets the last set scroll fully into view instead of being pinned inside a shrunk viewport. Same anti-pattern also exists in several other screens' `Column(.padding(paddingValues).padding(16.dp))` wrappers but isn't clipping anything visible there (Columns aren't scrollable), so left alone.
- Dashboard compact-layout `LazyColumn`: `.padding(layoutInfo.contentPadding)` → `contentPadding = PaddingValues(layoutInfo.contentPadding)` so widgets scroll through the bottom padding instead of the whole list being pinned.
- JVM tests still 36; no new tests (layout bug).

**Phase 4 slice 24 landed 2026-04-19** (build + JVM tests green on retry; no schema change):
- `data/CloudBackupRepository.kt:78` — folded the redundant `if (encryptionResult is EncryptionResult.Success)` branch into the existing `when`. The preceding `when` already `return@flow`s on `EncryptionResult.Error`, so the later instance check was always true. `encryptedData` and `dataHash` are now both assigned inside the `when`'s Success branch (one `val` declaration each outside the `when` for scope).
- Behaviour unchanged — on Error the flow still emits `CloudResult.Error` and returns.

**Phase 4 slice 23 landed 2026-04-19** (build + JVM tests green on retry; no schema change):
- `data/ImportRepository.kt` — 25 unchecked-cast + 6 Java-type-mismatch warnings cleared.
- Added three private helpers on the repo: `jsonMapType = TypeToken<Map<String, Any>>() {}`, `parseJsonAsMap(json)`, `Map<String, Any>.mapField(key)`, `Map<String, Any>.listOfMapsField(key)`. The two extension helpers carry `@Suppress("UNCHECKED_CAST")` — that suppress now lives in exactly two places in the codebase instead of 17 scattered throughout every import function.
- Both `validateJSONFile` overloads and all six `importX` functions (workouts, exercises, PRs, program templates, workout templates, complete backup) now go through `parseJsonAsMap` + `listOfMapsField` / `mapField`.
- `options.filePath!!` replaces `options.filePath` in the six `File(…)` constructor call sites — `ImportOptions.filePath` is `String?` but the Java `File(String)` constructor is non-null. Existing runtime behaviour (NPE on null) preserved; the warning is cleared.
- Behaviour unchanged. No new tests — purely cosmetic refactor of already-warning-suppressed cast patterns.
- Net warning count: build went from 26 → 0. Remaining Kotlin warnings: none.

**Phase 4 slice 22 landed 2026-04-19** (build + JVM tests green; no schema change):
- `data/GoogleDriveCloudProvider.kt` migrated off the deprecated `com.google.api.client.extensions.android.http.AndroidHttp` wrapper. `AndroidHttp.newCompatibleTransport()` at the `Drive.Builder` call site is now `NetHttpTransport()` (from `com.google.api.client.http.javanet.NetHttpTransport`). Single call site; import swap + one-line constructor swap. `google-http-client-android` already pulled this transport in transitively, so no dependency change.
- Removes the last two non-Compose deprecation warnings from the build. Remaining warnings are all in `ImportRepository` (unchecked Gson `Map<String, Any>` casts) and one always-true instance check in `CloudBackupRepository` — both out of scope.
- Behaviour parity: `NetHttpTransport` is the standard JVM/Android HTTP transport Google recommends since `AndroidHttp` was deprecated. The only thing `AndroidHttp.newCompatibleTransport()` did on modern Android was return `NetHttpTransport` anyway (the Gingerbread branch it once had is long gone).

**Phase 4 slice 21 landed 2026-04-19** (build + JVM tests green; no schema change):
- `viewmodel/AnalyticsViewModel.kt` gets a file-level `@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)`. Covers all six `flatMapLatest` call sites in one annotation — silences the six opt-in warnings the build had been carrying since the ViewModel was first written.
- No behaviour change, no new tests. Last Kotlin opt-in warning in the project.

**Phase 4 slice 20 landed 2026-04-19** (build + JVM tests green; no schema change):
- Added `sh.calvin.reorderable:reorderable:3.0.0` in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- `ui/ProgramEditorScreen.kt` — both editors (`ProgramEditorScreen` compact + `EnhancedProgramEditor` master-detail) now use `rememberReorderableLazyListState` around the week `LazyColumn`. Each week card lives inside a `ReorderableItem(reorderState, key = week.id)`. A new `DragHandle` `IconButton` sits above the existing up/down arrows in the leading column and carries `Modifier.draggableHandle()` from the `ReorderableCollectionItemScope`. Keys on `itemsIndexed` switched to `{ _, w -> w.id }` so item identity survives reorders.
- Sessions inside each week card migrated from a plain `Column { forEachIndexed }` to `ReorderableColumn(list = sortedSessions, onSettle = { from, to -> editedWeeks = moveSessionWithinWeek(editedWeeks, week.id, from, to) })`. Each session is wrapped in `key(session.id) { ReorderableItem { ... } }` and the drag handle is passed into `SessionCard` via a new optional `dragHandle` slot.
- `SessionCard` gains `dragHandle: (@Composable () -> Unit)? = null`. When non-null, it renders above the existing up/down arrows in the leading column — same 24.dp touch target, primary-tinted 16.dp icon. All other call sites default `dragHandle` to null, so the arrow + "Move to Week" dialog affordances remain unchanged.
- The arrow-button callbacks on `SessionCard` were consolidated to call `moveSessionWithinWeek(editedWeeks, week.id, index, index ± 1)` instead of the previous inline swap + renumber. Identical output, less duplication.
- New pure helper `util/ProgramEditorHelpers.kt::moveSessionWithinWeek(weeks, weekId, fromIndex, toIndex)` — indexes into `week.sessions.sortedBy { order }`, moves the entry, renumbers `order` to match list position, no-ops for equal indices / missing week / out-of-bounds. 6 new JVM tests; JVM count 97 → 103.

**Phase 4 slice 19 landed 2026-04-19** (build + JVM tests green on retry; no schema change):
- `util/ProgressionChip.kt::suggestForScheme` — pure helper that returns a `ChipSuggestion(weight, reps, rir, label)` given the exercise's progression scheme, its params, and the representative last-session set. Scheme rules: LINEAR adds `progressionIncrement` (null falls back to maintain); DOUBLE bumps reps when `lastReps < maxReps`, else bumps weight by `progressionIncrement` (default 2.5) and resets to `minReps`; RPE keeps last weight/reps and derives RIR from the target (ranges like "7-8" use the lower bound); TOP_SET set 1 adds `progressionIncrement` (default 2.5), backoff sets copy last. Labels include a scheme-specific suffix: `(next)`, `(top)`, `(backoff)`, or `@ RPE X`. 18 JVM tests; count 79 → 97.
- `data/DataModels.kt::PerformanceSuggestion` gains `suggestionLabel: String? = null`. When non-null, the chip renders it verbatim instead of the legacy "Wkg Nr (Xd ago)" format. In-memory data class — no schema change.
- `WorkoutLoggerViewModel` caches per-exercise `TemplateExercise` (in `_templateExercisesByExerciseId`, populated alongside `_progressionHints` from the same template lookup) and the representative recent set (in `_recentRepresentatives`, populated alongside `_performanceSuggestions` from the same DB lookup). New public `getChipSuggestion(exerciseId, setNumber): PerformanceSuggestion?` consults both caches, runs `suggestForScheme`, and wraps the result in a `PerformanceSuggestion` with `confidence = 1f` and the scheme label; falls through to the cached legacy suggestion when no scheme is configured or the helper has nothing to offer.
- Pull-apart refactor inside the VM: `buildSuggestionFromRecent` split into `pickRecentRepresentative` (the filter + pick logic) + `buildSuggestionFromRepresentative` (legacy progression math). The picker is now the single source of truth feeding both the scheme-aware chip and the legacy path. Also extracts `RecentRepresentative` data class with the set + working-set count + daysAgo.
- `ui/WorkoutLoggerSetRow.kt` swaps `viewModel.getPerformanceSuggestion(exerciseId)` → `viewModel.getChipSuggestion(exerciseId, setNumber)` and renders `suggestionLabel` when non-null. Legacy path now also reads `weightUnit` from the row param (was hardcoded "kg").
- Template editor gets two new inputs to complete the round trip:
  - DOUBLE: a "Weight bump at max" field (writes `progressionIncrement`) so the chip has a number to bump by when reps hit max.
  - TOP_SET: a "Top-set bump" field (writes `progressionIncrement`) so the top-set bump is configurable.
  - Scheme-switch param-strip logic extended: `progressionIncrement` now survives switches into LINEAR, DOUBLE, or TOP_SET (previously only LINEAR).

**Phase 4 slice 18 landed 2026-04-19** (build + JVM tests green first try; fourth real Room migration):
- `TemplateExercise` gains `progressionScheme: ProgressionScheme?` (enum: LINEAR / DOUBLE / RPE / TOP_SET / NONE) plus four per-scheme params (`progressionIncrement: Double?`, `progressionMinReps: Int?`, `progressionMaxReps: Int?`, `progressionTargetRpe: String?`). All default null. Lives inside the JSON blob in `workout_template_table.templateExercises` — no SQL column.
- **Fourth real Room migration.** DB version 24 → 25. `MIGRATION_24_25` is another no-op (user_version bump only), registered in `MIGRATIONS`. Schema exported at `app/schemas/com.kennychiu.myworkoutlog.data.WorkoutDatabase/25.json` — byte-identical to `24.json` except the version field.
- **Migration test extended.** `WorkoutDatabaseMigrationTest.migrate24To25()` calls `runMigrationsAndValidate(dbName, 25, true, MIGRATION_24_25)`. All five instrumented tests wire through the chained `MIGRATIONS` array.
- New `util/ProgressionHint.kt::formatProgressionHint` — pure helper that returns a short label like `"Linear +2.5kg/wk"`, `"Double 8–12 reps"`, `"RPE 8"`, `"Top set + backoffs"`, or null for NONE/unconfigured. Integer increments render without trailing `.0` (mirrors the same rounding in `LastPerformance.kt`). Takes an optional `weightUnit` param so LINEAR can render `"+5lb/wk"` when the user prefers pounds. 15 JVM tests; count 64 → 79.
- **Template editor UI.** `TemplateDetailScreen` renders a new `ProgressionSchemePicker` per exercise card between the exercise name divider and the sets list. `ExposedDropdownMenu` for the scheme (reuses `MenuAnchorType.PrimaryNotEditable` from slice 15) plus scheme-dependent param fields: LINEAR → one increment field, DOUBLE → min + max reps Row, RPE → target field, TOP_SET/NONE → no params. Switching schemes clears params that don't apply to the new scheme so the stored JSON stays honest.
- **Template preview UI.** `TemplateExerciseCard` (read-only) shows the passive hint above the sets breakdown when a scheme is configured — same `primary`-tinted `bodySmall` styling used in the logger.
- **Workout logger UI.** Both compact (`EnhancedExerciseCard`) and master-detail (`ExerciseListItem`) exercise rows show the passive hint under the exercise name. `EnhancedExerciseCard` gets a new `progressionHint: String?` param; `MasterDetailWorkoutView` gets a `progressionHintFor: (String) -> String?` lambda matching the slice-17 `lastPerformanceFor` pattern. Master-detail `ExerciseListItem` gets a matching optional param with the same selection-aware coloring.
- **VM plumbing.** `WorkoutLoggerViewModel._progressionHints: MutableStateFlow<Map<String, String>>` + `getProgressionHint(exerciseId): String?` mirror `getLastPerformance`. New private `refreshProgressionHints()` fetches the workout's template via `templateDao.getTemplateByIdSnapshot`, iterates its `templateExercises`, and caches the formatted hint map. Called from `initializePerformanceSuggestions()` — one call site covers all three workout-load entry points (fresh start, in-progress resume, edit). Ad-hoc workouts with no `workoutTemplateId` yield an empty map.
- **Chip behavior unchanged in this slice — deliberate.** The smart-pre-fill chip is still the "copy last session" shortcut. Slice 19 will make the chip scheme-aware (LINEAR adds the weekly increment, DOUBLE climbs reps, etc.).

**Phase 4 slice 17 landed 2026-04-19** (build + JVM tests green on retry, no schema change):
- `lastPerformance` wired into the master-detail logger's master panel. Each row in the exercise list now shows "Last: N × reps @ Wunit (days-ago)" under the "X/Y" sets counter, matching the compact-layout `EnhancedExerciseCard` treatment.
- `ui/AdaptiveWorkoutComponents.kt::ExerciseListItem` gains an optional `lastPerformance: String? = null` param. Rendered as a `labelSmall` Text (to match the existing "X/Y" counter density), colored `onPrimaryContainer` when the row is selected and `onSurfaceVariant` otherwise — same coloring pattern as the name/count already uses.
- `MasterDetailWorkoutView` gains a `lastPerformanceFor: (String) -> String? = { null }` lambda param and threads the exerciseId through to each `ExerciseListItem`. Default no-op preserves the previous behaviour for any other call site.
- `WorkoutLoggerScreens.kt` call site passes `lastPerformanceFor = { exerciseId -> viewModel.getLastPerformance(exerciseId) }`. Same VM method the compact layout already uses — no new plumbing.
- No schema change, no new tests (pure UI wiring over an existing VM accessor). JVM count stays 64.

**Phase 4 slice 16 landed 2026-04-19** (build + JVM tests green, no schema change):
- `WidgetRepositorySimplified.isCycleCompleted` folded into a call to `cycleProgress(activeCycle).isComplete`. Only caller was one branch in `getBasicQuickActions`. Private helper deleted (-6 LOC). Minor semantic tightening in the empty-program edge case — the old check (0 completed >= 0 total) flagged empty programs as complete; `cycleProgress` guards against that with `totalSessionCount > 0`. Existing `CycleProgressTest` coverage applies.

**Phase 4 slice 15 landed 2026-04-19** (build + JVM tests green, no schema change):
- Mechanical Compose deprecation sweep across the UI layer. No behavior change, no new tests.
- `Modifier.menuAnchor()` → `.menuAnchor(MenuAnchorType.PrimaryNotEditable)` in `ProgramEditorScreen.kt`, `VolumeAnalysisScreen.kt` (x2), `AnalyticsScreen.kt`. All sites are `readOnly = true` `OutlinedTextField`s driving `ExposedDropdownMenu`.
- `LinearProgressIndicator(progress: Float, …)` → lambda overload (`progress = { pct }`) in `CycleDetailScreen.kt`, `DashboardWidgetComponents.kt`, `AdaptiveWorkoutComponents.kt`. Same lambda-conversion for `CircularProgressIndicator` in `DashboardWidgetCards.kt`.
- `Icons.Default.*` (alias for `Icons.Filled.*`) migrated to `Icons.AutoMirrored.Filled.*` for every icon that has an AutoMirrored equivalent: `TrendingUp` / `TrendingDown` / `TrendingFlat` / `ShowChart` / `DirectionsRun` across `DashboardChartCards.kt`, `DashboardWidgetCards.kt`, `DashboardWidgetComponents.kt`, `InteractiveChartComponents.kt`. `Icons.Filled.List` → `AutoMirrored.Filled.List` in `ExportScreen.kt`, `HistoryComponents.kt`. `Icons.Outlined.Assignment` → `AutoMirrored.Outlined.Assignment` in `ProgramDetailViews.kt`. `Icons.Filled.Logout` → `AutoMirrored.Filled.Logout` in `CloudBackupScreen.kt`. `Icons.Default.Launch` → `AutoMirrored.Filled.Launch` in `DashboardHelpers.kt`. `Icons.Default.ArrowForward` → `AutoMirrored.Filled.ArrowForward` in `DashboardScreen.kt`.
- Files using AutoMirrored variants got a named import added alongside the existing `filled.*` / `outlined.*` star import — the nested `AutoMirrored.Filled` object isn't pulled in by `filled.*`.
- Only remaining deprecations in the build are in `data/GoogleDriveCloudProvider.kt` on Google's `AndroidHttp` Java class — out of scope.

**Phase 4 slice 14 landed 2026-04-19** (build + JVM tests green on retry, no schema change):
- `lastPerformance` TODO at `ui/WorkoutLoggerScreens.kt:551` replaced by a real summary.
- New `util/LastPerformance.kt::summarizeLastPerformance(workout, exercise, today)` — pure helper. Picks the heaviest completed set (highest `weight`, requires `reps > 0`) and renders `"N × reps @ Wunit (days-ago)"`, where unit defaults to "kg" when `performedWeightUnit` is null. Time-based fallback `"N × Xs"` when the exercise has completed secs-only sets. Integer weights render without decimal (`60kg`); fractional render as decimal (`22.5lb`). Days-ago suffix: `"today"` / `"yesterday"` / `"Nd ago"`; unparseable date → suffix omitted. Returns null when no completed sets.
- `WorkoutLoggerViewModel` now caches summaries in `_lastPerformanceSummaries: StateFlow<Map<String, String>>` populated alongside `_performanceSuggestions` in one DB-lookup loop. The lookup logic (same-template first, global fallback) was extracted into `findRecentWorkoutForExercise` + a refactored `buildSuggestionFromRecent` so both consumers share one fetch. Public getter `getLastPerformance(exerciseId): String?` mirrors `getPerformanceSuggestion`.
- 8 new JVM tests in `LastPerformanceTest`: heaviest-set pick across 3 sets, integer / fractional weight rendering, time-based fallback, null unit default, empty-set null return, zero-reps skip, unparseable-date suffix drop. JVM count 56 → 64.

**Phase 4 slice 13 landed 2026-04-19** (build + JVM tests green on retry, no schema change):
- Thread `AppSettingsRepository.weightUnitFlow` through `DashboardViewModel`. New ctor param `appSettingsRepository`; new `weightUnit: StateFlow<String>` exposure (`stateIn` with default `"kg"`, parallel to `SettingsViewModel`).
- `DashboardViewModel.saveBodyweightEntry` — `weightUnit = "kg" // TODO: …` → `weightUnit = weightUnit.value`. `BodyweightEntry.weightUnit` now reflects the user preference.
- `EnhancedDashboardScreen` collects `dashboardViewModel.weightUnit` and passes it into `BodyweightEntryDialog(weightUnit = weightUnit)` — same TODO gone.
- `AppContainer.dashboardViewModelFactory()` + `DashboardViewModelFactory` ctor updated to thread the repo. No other call sites existed.
- Two of three TODOs resolved; the third one flagged at `WorkoutLoggerScreens.kt:503` is a different TODO (`lastPerformance = null // TODO: Implement based on available data`), not a weight-unit one — reclassified. No new tests needed (preference flow is VM plumbing; values are surfaced by existing `SettingsViewModel` logic).

**Phase 4 slice 12 landed 2026-04-19** (build + JVM tests green on retry, no schema change):
- New `util/ProgramEditorHelpers.kt::moveSessionToWeek(weeks, fromWeekId, sessionId, toWeekId)` — pure helper. Removes the session from its source week (renumbering remaining sessions' `order = listIndex + 1`), appends it to the target week with `order = target.sessions.size + 1`. No-op for same-week moves, missing weeks, or missing session. Session id / name / workoutTemplateId preserved.
- `SessionCard` gains two optional params: `otherWeeks: List<ProgramWeekDefinition>` and `onMoveToWeek: ((String) -> Unit)?`. When set and `otherWeeks` is non-empty, a small `SwapHoriz` IconButton renders before the Edit button; tapping it opens a new `AlertDialog` listing other weeks' labels as tappable rows. Tapping a row calls `onMoveToWeek(targetWeekId)` and closes the dialog.
- Both editors (`ProgramEditorScreen` compact + `EnhancedProgramEditor` master-detail) wire `otherWeeks = editedWeeks.filter { it.id != week.id }` and `onMoveToWeek = { editedWeeks = moveSessionToWeek(editedWeeks, week.id, session.id, it) }`.
- 7 new JVM tests in `ProgramEditorHelpersTest`: appends with correct order on target; renumbers source after removal; no-op for same-week / missing source / missing target / missing session; preserves session identity fields. JVM test count 49 → 56.

**Phase 4 slice 11 landed 2026-04-19** (build + JVM tests green, no schema change):
- `RenameCycleDialog` lifted out of `CycleDetailScreen.kt` into a shared `ui/RenameCycleDialog.kt` (top-level `public` composable). `CycleDetailScreen` now calls the shared one.
- `HistoryViewModel.renameCompletedCycle(cycleId, newName)` — trims input, early-return on blank; dispatches on `Dispatchers.IO` and calls `LoggedWorkoutDao.renameLoggedWorkoutsByCycle` (DAO method already landed in slice 10). Ended cycles don't have a row in `active_program_cycle_table`, so only the logged-workout snapshots need updating.
- `ui/HistoryCycleViews.kt::CycleCard` and `CycleCardMaster` each get an optional `onRenameClick: () -> Unit = {}` parameter and an `Icons.Filled.Edit` IconButton on the right side of the cycle-name Row. `MesocycleHistoryView` and `MesocycleHistoryMasterView` each hold a `renameTarget: CycleWithWorkouts?` state — the Edit pencil sets it, and the shared `RenameCycleDialog` is rendered below the `LazyColumn` when non-null.
- Coverage: any ended cycle in the "Completed Cycles" section on the history screen can now be renamed. The DAO UPDATE also fires for workouts of the active cycle if a caller passes its cycleUuid, but the UI never does so from the history screen (the active cycle lives in its own section and isn't rendered as a `CycleCard`). No schema change; no new tests. JVM test count stays 49.

**Phase 4 slice 10 landed 2026-04-19** (build + JVM tests green, no schema change):
- `ActiveCycleDao.renameActiveCycle(newName)` — single-row UPDATE on the active cycle (`WHERE id = 1`).
- `LoggedWorkoutDao.renameLoggedWorkoutsByCycle(cycleId, newName)` — UPDATE that backfills the `userCycleName` snapshot on every workout in the given cycle. `HistoryViewModel` and the completed-cycle rollup both read this snapshot field, so history picks up the new name without schema work.
- `CycleDetailViewModel.renameActiveCycle(newName)` — trims input, no-ops on blank or when there's no active cycle, then dispatches both DAO writes on `Dispatchers.IO`. `CycleDetailViewModel` now holds `activeCycleDao` / `loggedWorkoutDao` as `private val` (was local ctor params).
- `ui/CycleDetailScreen.kt::CycleHeaderCard` — Edit pencil `IconButton` next to the cycle name. Tapping opens a new private `RenameCycleDialog` composable (AlertDialog + single-line `OutlinedTextField` prefilled with the current name). Save is disabled until the trimmed input is non-blank AND different from the current name.
- Semantics: rename works for the currently active cycle only. Once a cycle ends, its name is frozen on `LoggedWorkout.userCycleName` snapshots — no rename UX for ended cycles yet.
- No schema change; no new tests (VM test would be a pure delegate over two DAOs — low value). JVM test count stays 49.

**Phase 4 slice 9 landed 2026-04-19** (build + JVM tests green, no schema change):
- New `util/ProgramEditorHelpers.kt::moveWeek(weeks, fromIndex, toIndex)` — pure helper. Moves the week at `fromIndex` to `toIndex` and renumbers every week's `order` to match list position. No-op if `fromIndex == toIndex`, or if either index is out of bounds. Sessions and all other week fields are preserved on the moved week.
- Both editors (`ProgramEditorScreen` compact + `EnhancedProgramEditor` master-detail) now iterate weeks with `itemsIndexed` and render a vertical Column of `KeyboardArrowUp` / `KeyboardArrowDown` IconButtons at the left edge of each week card. Disabled (dimmed) at the list boundaries. Same 24.dp / 16.dp sizing and tint pattern as the existing session reorder arrows in `SessionCard`.
- 7 new JVM tests in `util/ProgramEditorHelpersTest.kt`: swaps upward; swaps downward; renumbers `order` across the list; no-op when indices are equal; no-op on out-of-bounds `fromIndex`; no-op on out-of-bounds `toIndex`; preserves sessions + deload flag + targetRir on the moved week. JVM test count 42 → 49.

**Phase 4 slice 8 landed 2026-04-19** (build + JVM tests green, no schema change):
- New `util/ProgramEditorHelpers.kt::duplicateWeekInto(weeks, source, idGenerator)` — pure helper. Finds the source week by id, builds a copy with a fresh UUID (via injected generator, defaults to `UUID.randomUUID`), fresh UUIDs on every nested `ProgramSessionDefinition`, label prefixed with "Copy of ", inserts it immediately after the source in the list, and renumbers every week's `order` to match list position. If the source id is absent, returns the list unchanged.
- Both editors (`ProgramEditorScreen` compact + `EnhancedProgramEditor` master-detail) gain a `ContentCopy` IconButton between the Week Label field and the Delete button. Tapping duplicates the week. Existing delete behaviour is unchanged.
- 6 new JVM tests in `util/ProgramEditorHelpersTest.kt`: copy is inserted in the right position; `order` gets renumbered across the whole list; nested sessions get fresh ids but preserve names/order; deload flag and `targetRir` carry through; label gets the "Copy of " prefix; unknown source id leaves the list unchanged. JVM test count 36 → 42.
- Session `order` inside a week is preserved as-is on the copy (sessions already sorted by their own `order` field; see `CycleProgress.kt`, `CycleDetailScreen.kt`, `WidgetRepositorySimplified.kt`). No renumber needed there.

**Phase 4 slice 7 landed 2026-04-19** (build + JVM tests green, no schema change):
- `MasterDetailWorkoutView` in `ui/AdaptiveWorkoutComponents.kt` gains an optional `contextBanner: (@Composable () -> Unit)? = null` slot. When non-null, it renders full-width above the master + detail Row, inside the content column (right of the navigation rail, inside the same padding). Layout rearranged from `Row { rail; Row { masterCard; detailCard } }` to `Row { rail; Column { banner?; Row(weight=1f) { masterCard; detailCard } } }` so the banner and panels share horizontal space without the panels collapsing.
- `WorkoutLoggerScreens.kt` passes `contextBanner = { CycleContextBanner(...) }` into the `MasterDetailWorkoutView` call when `currentCycleWeek?.isDeloadWeek` or `targetRir` is non-blank — same gating as the compact-layout `LazyColumn` banner. The composable itself is reused as-is (still file-scoped `private`; both call sites are in `WorkoutLoggerScreens.kt`).
- Motivation: the prior "skip unless tablet used" gating was based on a bad assumption. Kenny's daily device is a Samsung Z Fold — the inner screen held horizontally trips `shouldUseWorkoutMasterDetail()`, so compact-only features were invisible to him during workouts on the unfolded device. Any new workout-logger UI should target both layouts from the start.
- No data/schema change. JVM tests still 36.

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

4. **Test coverage is still thin but no longer zero.** The wizard defaults were removed 2026-04-18. There are now 103 JVM unit tests (20 ViewModel + 8 for `CycleProgress` + 8 for `CycleAggregates` + 26 for `ProgramEditorHelpers` + 8 for `LastPerformance` + 15 for `ProgressionHint` + 18 for `ProgressionChip`) plus five instrumented migration tests (v21 open, v21→22, v22→23, v23→24, v24→25). Coverage targets the known-fragile areas: workout timer + edit/resume, active cycle UUID flow, history cycle filtering, cycle progress derivation, week-duplicate + session-reorder integrity, last-performance summary formatting, progression-hint formatting, and scheme-aware chip suggestions. Everything else (dashboard widgets, PRs, import/export, cloud backup, volume, analytics) is still validated only by running the app.

5. **Room DAO convention was unstable.** Recent commits flipped back and forth on `suspend` modifiers for `@Query` / `@Delete`. The current convention (see `CLAUDE.md`) is: suspend for writes, non-suspend for `Flow`/`LiveData` returns, non-suspend snapshot reads only where sync call sites require them.

6. **TODOs.** The two weight-unit TODOs were resolved in slice 13, and the `lastPerformance` TODO at `WorkoutLoggerScreens.kt:551` was resolved in slice 14. No tracked TODOs remain in the workout-logger path.

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
  - Done (2026-04-19, slice 7): cycle-context banner mirrored into the master-detail logger layout so the Z Fold's tablet layout shows the same deload/RIR context as the compact layout. Banner slot added to `MasterDetailWorkoutView`.
  - Done (2026-04-19, slice 8): bulk-copy-week action on each week card in both the compact and master-detail program editors. Pure helper `util/ProgramEditorHelpers.kt::duplicateWeekInto` with 6 JVM tests. No schema change.
  - Done (2026-04-19, slice 9): up/down reorder arrows on each week card in both program editors. Pure helper `util/ProgramEditorHelpers.kt::moveWeek` with 7 JVM tests. No schema change.
  - Done (2026-04-19, slice 10): rename the active cycle from the cycle-detail screen. Two DAO updates (`ActiveCycleDao.renameActiveCycle`, `LoggedWorkoutDao.renameLoggedWorkoutsByCycle`) + `CycleDetailViewModel.renameActiveCycle` + Edit pencil + dialog. Backfills the `userCycleName` snapshot on every workout in the cycle so history picks up the new name. No schema change.
  - Done (2026-04-19, slice 11): rename ENDED cycles from the history screen. Shared `ui/RenameCycleDialog.kt` + `HistoryViewModel.renameCompletedCycle` + Edit pencil on both `CycleCard` and `CycleCardMaster`. Reuses the slice 10 DAO method. No schema change.
  - Done (2026-04-19, slice 12): move-session-across-weeks in the program editor. Pure helper `moveSessionToWeek` + 7 JVM tests + `SwapHoriz` IconButton + week-picker dialog on `SessionCard`. No schema change.
  - Done (2026-04-19, slice 13): weight-unit TODO cleanup. `DashboardViewModel` now holds `AppSettingsRepository` and exposes `weightUnit: StateFlow<String>`. `saveBodyweightEntry` and `BodyweightEntryDialog` both read from it. No schema change.
  - Done (2026-04-19, slice 14): `lastPerformance` wiring. Pure helper `util/LastPerformance.kt::summarizeLastPerformance` with 8 JVM tests; `WorkoutLoggerViewModel` caches summaries alongside `performanceSuggestions`. `EnhancedExerciseCard` on the workout logger now shows the last session's top set. No schema change.
  - Done (2026-04-19, slice 15): Compose deprecation sweep. `Modifier.menuAnchor()` → `MenuAnchorType.PrimaryNotEditable` overload in 4 sites; `LinearProgressIndicator`/`CircularProgressIndicator` `progress: Float` → lambda in 4 sites; every auto-mirrored icon migrated (TrendingUp/Down/Flat, ShowChart, DirectionsRun, List, Assignment, Logout, Launch, ArrowForward). Only remaining deprecation in the build is in Google's `AndroidHttp` Java class — out of scope. No behavior change, no new tests.
  - Done (2026-04-19, slice 16): `isCycleCompleted` dedup. `WidgetRepositorySimplified` now calls `cycleProgress(cycle).isComplete`; private helper deleted. Existing `CycleProgressTest` coverage applies. Minor semantic fix: empty programs are no longer flagged as complete.
  - Done (2026-04-19, slice 17): `lastPerformance` in the master-detail logger's master panel. `ExerciseListItem` gains an optional `lastPerformance: String?` param; `MasterDetailWorkoutView` exposes a `lastPerformanceFor` lambda the `WorkoutLoggerScreens` call site fills from `viewModel.getLastPerformance`. Same VM accessor as the compact layout — no new plumbing, no schema change.
  - Done (2026-04-19, slice 18): per-exercise progression scheme (LINEAR/DOUBLE/RPE/TOP_SET/NONE) + optional per-scheme params on `TemplateExercise`. Fourth real Room migration (v24 → v25, no-op). Template editor picker, template preview hint, and workout logger passive hint on both layouts. Pure `util/ProgressionHint.kt::formatProgressionHint` with 15 JVM tests. Smart-pre-fill chip stays scheme-agnostic in this slice; slice 19 makes it scheme-aware.
  - Done (2026-04-19, slice 19): scheme-aware smart-pre-fill chip. Pure `util/ProgressionChip.kt::suggestForScheme` with 18 JVM tests. `PerformanceSuggestion` gains an optional `suggestionLabel`; `WorkoutLoggerViewModel` caches per-exercise `TemplateExercise` + representative recent set and exposes `getChipSuggestion(exerciseId, setNumber)` that runs the helper and falls back to legacy. Template editor gains "Weight bump at max" input for DOUBLE and "Top-set bump" input for TOP_SET.
  - Done (2026-04-19, slice 20): true drag-reorder via `sh.calvin.reorderable:3.0.0`. `ReorderableLazyColumn` for weeks, `ReorderableColumn` for sessions in both program editors. Drag handle renders above the existing arrow buttons; move-to-week dialog and AlertDialog stay as fallbacks. New `SessionCard.dragHandle` slot + new `util/ProgramEditorHelpers.kt::moveSessionWithinWeek` helper with 6 JVM tests (97 → 103).
  - Done (2026-04-19, slice 21): file-level `@OptIn(ExperimentalCoroutinesApi)` on `AnalyticsViewModel`. Six `flatMapLatest` call sites; last Kotlin opt-in warning cleared.
  - Done (2026-04-19, slice 22): `GoogleDriveCloudProvider` migrated from deprecated `AndroidHttp.newCompatibleTransport()` to `NetHttpTransport()`. Last non-Compose deprecation warning cleared.
  - Done (2026-04-19, slice 23): `ImportRepository` unchecked-cast cleanup via three private helpers (`parseJsonAsMap`, `Map.mapField`, `Map.listOfMapsField`); `@Suppress("UNCHECKED_CAST")` now lives in two central spots instead of 17 call sites. `options.filePath!!` clears the six Java-type-mismatch warnings. Build now warning-clean.
  - Done (2026-04-19, slice 24): `CloudBackupRepository` always-true instance check folded into the existing `when` on `EncryptionResult`.
  - Further candidates: none gating. Workout-logger and program-management paths are feature-complete for now; device-verification sweep across slices 5b–24 is the natural next session.

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
