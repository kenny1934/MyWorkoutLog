// In StrengthAnalytics.kt
package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
// --- IMPORTS ---
// (No imports needed for this file)

object StrengthAnalytics {
    // Calculates Estimated 1-Rep Max using the Epley formula
    fun calculateEpley1RM(weight: Double, reps: Int): Double {
        if (reps == 1) return weight
        return weight * (1 + (reps / 30.0))
    }
}