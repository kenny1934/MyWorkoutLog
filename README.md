# MyWorkoutLog - Native Android App

This is a native Android workout logger application built in Kotlin using Jetpack Compose. The project is a port and enhancement of an original React-based web application, designed to provide a robust, offline-first experience for tracking structured workout programs.

## Current State of the App

The application is in a highly functional state, with a complete end-to-end user experience for planning, executing, and reviewing workouts.

### Key Features:
- **Comprehensive Planning:**
  - Manage a detailed list of custom exercises, including tagging them with target muscle groups and bodyweight-usage properties.
  - Create reusable, multi-set workout templates.
  - Build complex, multi-week program blueprints by assigning workout templates to specific sessions within each week.
- **Intelligent Execution:**
  - Start and manage an "Active Program Cycle" from a blueprint.
  - A dynamic dashboard shows the current week and session progress, with shortcuts to start or review workouts.
  - Log live workout sessions, recording reps, weight, and daily bodyweight with a unit-aware system.
  - An integrated **rest timer** with pause/resume/add-time controls and a **total session stopwatch**.
- **In-Depth Review & Analysis:**
  - Browse a complete, clickable history of all logged workouts with detailed session timings.
  - Automatic Personal Record (PR) detection for max weight and max reps, with e1RM (Estimated 1-Rep Max) calculation that correctly accounts for bodyweight exercises.
  - An interactive **Volume Analysis** screen with charts to track training volume (total sets) per muscle group, filterable by training week.
- **Polished UI/UX:**
  - A full multi-screen navigation system and a custom, persistent dark/light theme.
  - Responsive layouts that adapt to different screen sizes.

---

## Development Roadmap

This section outlines the planned enhancements and new features for the application.

### Tier 1: Core Logger Experience
- [x] **Workout Timers:** Implement a rest timer between sets and a stopwatch for timed exercises.
- [ ] **Detailed Set Logging:** Add fields for Duration (secs), RIR, Bands, and Notes to each set in the logger.
- [ ] **In-Session Flexibility:**
  - [ ] Add ad-hoc exercises to a workout on the fly.
  - [ ] Substitute exercises during a session.
- [ ] **Smart Pre-fill:** Automatically populate sets with the performance data from the last time the exercise was performed.
- [ ] **Editing Historical Workouts:** Add an "Edit" button to the `HistoryDetailScreen` that opens a past workout in the logger, allowing for corrections.

### Tier 2: Advanced Analytics & Insights
- [ ] **Complete Volume Analysis:** Enhance the Volume Analysis screen with a "Drill-Down" feature to see which specific exercises contributed to a muscle group's volume.
- [ ] **Progression Suggestions:** Implement a system to analyze workout performance and suggest weight or rep increases for the next session to automate progressive overload.

### Tier 3: Data Management & Long-Term Features
- [ ] **Import/Export Data:** Implement backup and restore functionality for all app data from a JSON file.
- [ ] **Full Cycle History:** Build a screen to view and compare past completed program cycles.
- [ ] **Dashboard Enhancements:** Further improve the dashboard to show more detailed cycle compliance or progress charts.

---

## Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose & Material 3
* **Architecture:** MVVM (Model-View-ViewModel)
* **Asynchronicity:** Kotlin Coroutines & Flow
* **Database:** Room Persistence Library
* **User Preferences:** Jetpack DataStore
* **Charting:** Vico Charting Library
* **Dependency Injection:** Manual (via ViewModel Factories)

---
*This README is actively being updated. Last update: June 2025.*