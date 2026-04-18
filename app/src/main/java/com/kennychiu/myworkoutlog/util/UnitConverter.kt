// In UnitConverter.kt
package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
// --- IMPORTS ---
// (No imports needed for this file)

object UnitConverter {
    private const val KG_TO_LB_FACTOR = 2.20462

    fun toKg(weight: Double, unit: String?): Double {
        return if (unit == "lb") {
            weight / KG_TO_LB_FACTOR
        } else {
            weight // Assume kg if unit is "kg" or null
        }
    }
}