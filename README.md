# MyWorkoutLog

A personal Android app for logging workouts and tracking training programs. Single-user, offline-first, local SQLite storage with optional Google Drive backup.

This is a hobby project, not a commercial product.

## Stack

- Kotlin 2.1, Jetpack Compose, Material 3
- Room (SQLite), DataStore
- Navigation Compose, Vico charts
- Google Drive API for optional backup
- Manual DI via ViewModel factories
- minSdk 26, targetSdk/compileSdk 35

## Building

Requires Android Studio or JDK 17+ with Android SDK.

```bash
./gradlew assembleDebug        # build debug APK
./gradlew installDebug         # install on connected device
./gradlew test                 # unit tests (currently ~none)
./gradlew lint                 # lint
```

## Repo layout

```
app/src/main/java/com/example/myworkoutlog/   # all Kotlin source (flat package, to be restructured)
app/src/main/res/                              # resources
docs/STATE.md                                  # honest status: works / broken / unfinished
samples/                                       # sample data for manual testing
```

## Status

See `docs/STATE.md` for an honest accounting of what works, what's broken, and what's unfinished. The short version: core logging works, mesocycle/program management is incomplete, the workout session timer has been patched repeatedly and still has edge cases, and there are effectively no automated tests.

## License

MIT (see `LICENSE`).
