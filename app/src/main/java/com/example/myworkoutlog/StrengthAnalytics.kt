// In StrengthAnalytics.kt
package com.example.myworkoutlog

// --- IMPORTS ---
// (No imports needed for this file)

object StrengthAnalytics {
    // Calculates Estimated 1-Rep Max using the Epley formula
    fun calculateEpley1RM(weight: Double, reps: Int): Double {
        if (reps == 1) return weight
        return weight * (1 + (reps / 30.0))
    }
}