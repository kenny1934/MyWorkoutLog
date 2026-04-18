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
3. **Room DB still uses `fallbackToDestructiveMigration()`** — every schema change wipes user data. Must be replaced before further schema changes.
4. **Flat package `com.kennychiu.myworkoutlog`** with ~65 files and multi-thousand-line screen files. Structural cleanup (subpackages, monolith splits) is scheduled.
5. **No real tests.** `ExampleUnitTest.kt` and `ExampleInstrumentedTest.kt` are the Android Studio wizard defaults.

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
