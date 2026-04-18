package com.kennychiu.myworkoutlog.data

import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages dashboard preferences persistence using SharedPreferences
 */
class DashboardPreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "dashboard_preferences"
        private const val KEY_WIDGET_CONFIGS = "widget_configs"
        private const val KEY_SHOW_MOTIVATIONAL_MESSAGES = "show_motivational_messages"
        private const val KEY_SHOW_ACHIEVEMENTS = "show_achievements"
        private const val KEY_SHOW_INSIGHTS = "show_insights"
        private const val KEY_AUTO_REFRESH = "auto_refresh"
        private const val KEY_DEFAULT_TIMEFRAME = "default_timeframe"
        private const val KEY_DISMISSED_INSIGHTS = "dismissed_insights"
    }
    
    suspend fun saveDashboardPreferences(preferences: DashboardPreferences) {
        withContext(Dispatchers.IO) {
            val editor = sharedPreferences.edit()
            
            // Save widget configurations as JSON
            val widgetConfigsJson = gson.toJson(preferences.widgetConfigs)
            editor.putString(KEY_WIDGET_CONFIGS, widgetConfigsJson)
            
            // Save boolean preferences
            editor.putBoolean(KEY_SHOW_MOTIVATIONAL_MESSAGES, preferences.showMotivationalMessages)
            editor.putBoolean(KEY_SHOW_ACHIEVEMENTS, preferences.showAchievements)
            editor.putBoolean(KEY_SHOW_INSIGHTS, preferences.showInsights)
            editor.putBoolean(KEY_AUTO_REFRESH, preferences.autoRefresh)
            
            // Save default timeframe
            editor.putString(KEY_DEFAULT_TIMEFRAME, preferences.defaultTimeframe)
            
            // Save dismissed insights as JSON
            val dismissedInsightsJson = gson.toJson(preferences.dismissedInsights.toList())
            editor.putString(KEY_DISMISSED_INSIGHTS, dismissedInsightsJson)
            
            editor.apply()
        }
    }
    
    suspend fun loadDashboardPreferences(): DashboardPreferences {
        return withContext(Dispatchers.IO) {
            // Load widget configurations from JSON
            val widgetConfigsJson = sharedPreferences.getString(KEY_WIDGET_CONFIGS, null)
            val widgetConfigs = if (widgetConfigsJson != null) {
                try {
                    val type = object : TypeToken<List<WidgetConfig>>() {}.type
                    gson.fromJson<List<WidgetConfig>>(widgetConfigsJson, type)
                } catch (e: Exception) {
                    emptyList() // Fallback to empty list if parsing fails
                }
            } else {
                emptyList()
            }
            
            // Load other preferences with defaults
            val showMotivationalMessages = sharedPreferences.getBoolean(KEY_SHOW_MOTIVATIONAL_MESSAGES, true)
            val showAchievements = sharedPreferences.getBoolean(KEY_SHOW_ACHIEVEMENTS, true)
            val showInsights = sharedPreferences.getBoolean(KEY_SHOW_INSIGHTS, true)
            val autoRefresh = sharedPreferences.getBoolean(KEY_AUTO_REFRESH, true)
            val defaultTimeframe = sharedPreferences.getString(KEY_DEFAULT_TIMEFRAME, "30days") ?: "30days"
            
            // Load dismissed insights from JSON
            val dismissedInsightsJson = sharedPreferences.getString(KEY_DISMISSED_INSIGHTS, null)
            val dismissedInsights = if (dismissedInsightsJson != null) {
                try {
                    val type = object : TypeToken<List<String>>() {}.type
                    val list = gson.fromJson<List<String>>(dismissedInsightsJson, type)
                    list.toSet()
                } catch (e: Exception) {
                    emptySet() // Fallback to empty set if parsing fails
                }
            } else {
                emptySet()
            }
            
            DashboardPreferences(
                widgetConfigs = widgetConfigs,
                showMotivationalMessages = showMotivationalMessages,
                showAchievements = showAchievements,
                showInsights = showInsights,
                autoRefresh = autoRefresh,
                defaultTimeframe = defaultTimeframe,
                dismissedInsights = dismissedInsights
            )
        }
    }
    
    suspend fun clearPreferences() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().clear().apply()
        }
    }
    
    suspend fun hasExistingPreferences(): Boolean {
        return withContext(Dispatchers.IO) {
            sharedPreferences.contains(KEY_WIDGET_CONFIGS)
        }
    }
}