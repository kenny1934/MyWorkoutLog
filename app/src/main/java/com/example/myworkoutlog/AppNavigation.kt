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

    // The route for the workout logger screen
    data object WorkoutLogger : Screen("workout_logger/{templateId}?cycleId={cycleId}&weekId={weekId}&sessionId={sessionId}") {
        // This function is for starting an ad-hoc workout from a template
        fun createRoute(templateId: String) = "workout_logger/$templateId"

        // This function is for starting a workout from a program cycle
        fun createRoute(templateId: String, cycleId: String, weekId: String, sessionId: String): String {
            return "workout_logger/$templateId?cycleId=$cycleId&weekId=$weekId&sessionId=$sessionId"
        }
    }

    data object HistoryDetail : Screen("history_detail/{workoutId}") {
        fun createRoute(workoutId: String) = "history_detail/$workoutId"
    }

    // The route for managing program blueprints
    data object ManagePrograms : Screen("manage_programs")

    // Route for the program editor screen
    data object ProgramEditor : Screen("program_editor/{programId}") {
        fun createRoute(programId: String) = "program_editor/$programId"
    }

    // The route for viewing personal records
    data object PersonalRecords : Screen("personal_records")

    // The route for the settings screen
    data object Settings : Screen("settings")

    data object VolumeAnalysis : Screen("volume_analysis")
}