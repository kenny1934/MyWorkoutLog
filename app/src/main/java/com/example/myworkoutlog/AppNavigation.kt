package com.example.myworkoutlog

// A sealed class makes our navigation logic safer and easier to read.
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object History : Screen("history")
    data object Library : Screen("library") // Renamed from ManageExercises
    data object ManageExercises : Screen("manage_exercises")
    data object ManageTemplates : Screen("manage_templates")

    // The detail screen needs to accept an ID as an argument
    data object TemplateDetail : Screen("template_detail/{templateId}") {
        // Helper function to create the route with a specific ID
        fun createRoute(templateId: String) = "template_detail/$templateId"
    }

    // NEW: The route for the workout logger screen
    data object WorkoutLogger : Screen("workout_logger/{templateId}") {
        fun createRoute(templateId: String) = "workout_logger/$templateId"
    }

    data object HistoryDetail : Screen("history_detail/{workoutId}") {
        fun createRoute(workoutId: String) = "history_detail/$workoutId"
    }
}