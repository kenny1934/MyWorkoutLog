// In AppSettingsRepository.kt
package com.example.myworkoutlog

// --- IMPORTS ---
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define the DataStore file name
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(private val context: Context) {

    // This object holds the keys for our preferences.
    private object Keys {
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    }

    // This Flow will emit the current weight unit whenever it changes.
    // If it's not set, it defaults to "kg".
    val weightUnitFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[Keys.WEIGHT_UNIT] ?: "kg"
        }

    // This function saves a new weight unit preference.
    suspend fun saveWeightUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.WEIGHT_UNIT] = unit
        }
    }
}