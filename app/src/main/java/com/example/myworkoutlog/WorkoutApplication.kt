package com.example.myworkoutlog // Your package name

import android.app.Application

// A custom Application class allows us to initialize things once when the app starts.
class WorkoutApplication : Application() {
    // 'lazy' means the database will only be created when it's first needed.
    val database by lazy { WorkoutDatabase.getDatabase(this) }
}