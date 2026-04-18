package com.kennychiu.myworkoutlog

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import android.app.Application

class WorkoutApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    // Kept for source-compatibility with existing call sites. Prefer `container` in new code.
    val database: WorkoutDatabase get() = container.database
    val appSettingsRepository: AppSettingsRepository get() = container.appSettingsRepository
}
