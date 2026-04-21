# Claude guidance for this repo

Personal, single-user Android workout tracker. Hobby project. No commercial claims — don't write marketing copy, don't invent competitive positioning, don't use language like "enterprise-grade", "production-ready", "military-grade", or "pioneer". Write like the hobbyist this is.

## Stack

Kotlin 2.1, Jetpack Compose, Material 3, Room, DataStore, Navigation Compose, Vico charts, Google Drive API. Manual DI via ViewModel factories wired in `MainActivity`. minSdk 26 / target 35.

## Ground truth

- **`docs/STATE.md`** is the single source of truth for current status (works / broken / unfinished). Update it when reality changes. Don't create parallel status docs.
- Prior docs were deleted because they were inflated and contradictory. Don't recreate that pattern.

## Known-broken areas (see STATE.md for details)

1. **Mesocycle / program management** is ~40% done (per prior audit on the `claude/evaluate-app-rewrite-Eqoyq` branch). This is the priority when restarting feature work.
2. **Workout session timer + edit/resume** has been patched many times and still has state-contamination edge cases.
3. **Test coverage is thin outside known-fragile areas.** 201 JVM tests cover workout logger, cycle progress, history filtering, program-editor helpers, progression hints/projection, cycle-phase classification, PR detection (`PrService.detectNewPRs`), and all `AnalyticsRepository` Flow surfaces (`compareCycles`, `getWeeklyVolumeSummary`, `getMuscleGroupVolumeDistribution`, `getPersonalRecordProgress`, `getExercisePerformanceTrend`); dashboard widgets, `AnalyticsRepository` `suspend` dashboard aggregates, import/export, and cloud backup are still validated only by running the app.

## Already-resolved (do not re-open without reading STATE.md)

- **Room destructive fallback.** Replaced. Schema is at v25; `MIGRATION_21_22` through `MIGRATION_24_25` are defined in `WorkoutDatabase.kt`; destructive fallback is scoped to legacy dev versions 1–20 via `fallbackToDestructiveMigrationFrom(*LEGACY_DEV_VERSIONS)`; `exportSchema = true` with JSONs under `app/schemas/`; `WorkoutDatabaseMigrationTest` covers each step. Any schema bump past v25 must add a new `Migration` + extend the test.
- **Flat package.** Files are under `data/`, `ui/`, `viewmodel/`, `util/` subpackages (`MainActivity`, `WorkoutApplication`, `AppContainer` stay at the root because the manifest references them as `.Name`).
- **Monolithic screen files.** All four split (`DashboardScreen`, `ProgramManagementScreens`, `HistoryScreens`, `WorkoutLoggerScreens`) — see STATE.md for extraction details.

## Conventions (current codebase — follow for consistency)

- DB operations: `Dispatchers.IO`. Writes use `suspend`. Flow reads are non-suspend (Room generates the coroutine). Non-suspend snapshot reads exist only where Compose or sync call sites require them.
- Don't add `suspend` to `@Query` methods that return `Flow<T>` or `LiveData<T>`.
- Composables are capitalized; ViewModels use `StateFlow`; prefer `val`.
- Material 3 components and theming.

## Working style

- Don't restate history in code comments. Don't write "added for issue X" or "fix for Y".
- Keep changes small. Don't bundle refactors with feature work.
- When a doc or status claim contradicts the code, trust the code and update the doc.
- If you change DB schema, add a `Migration` object and a migration test — do not bump the version and rely on destructive fallback.

## Reference branch

`origin/claude/evaluate-app-rewrite-Eqoyq` contains an earlier audit (`TECH_STACK_EVALUATION.md`) that honestly assessed feature completeness and recommended a PWA rewrite. Keep as historical reference; don't delete without discussion.
