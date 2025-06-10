// In SettingsViewModel.kt
package com.example.myworkoutlog

// --- IMPORTS ---
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val appSettingsRepository: AppSettingsRepository) : ViewModel() {

    // Expose the weight unit as a StateFlow for the UI to collect
    val weightUnit: StateFlow<String> = appSettingsRepository.weightUnitFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "kg" // Default value
        )

    // Function for the UI to call when the user changes the setting
    fun setWeightUnit(unit: String) {
        viewModelScope.launch {
            appSettingsRepository.saveWeightUnit(unit)
        }
    }
}

class SettingsViewModelFactory(
    private val repository: AppSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}