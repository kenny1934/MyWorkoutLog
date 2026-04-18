package com.kennychiu.myworkoutlog.viewmodel

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for cloud backup and restore operations
 * Manages authentication, backup/restore operations, and UI state
 */
class CloudBackupViewModel(
    private val cloudBackupRepository: CloudBackupRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = _uiState.asStateFlow()

    // Authentication state
    private val _authStatus = MutableStateFlow(CloudAuthStatus(
        isSignedIn = false,
        userEmail = null,
        userName = null,
        provider = CloudProvider.GOOGLE_DRIVE,
        hasPermissions = false
    ))
    val authStatus: StateFlow<CloudAuthStatus> = _authStatus.asStateFlow()

    // Backup list
    private val _backups = MutableStateFlow<List<CloudBackupItem>>(emptyList())
    val backups: StateFlow<List<CloudBackupItem>> = _backups.asStateFlow()

    // Storage info
    private val _storageInfo = MutableStateFlow<CloudStorageInfo?>(null)
    val storageInfo: StateFlow<CloudStorageInfo?> = _storageInfo.asStateFlow()

    init {
        checkAuthStatus()
    }

    // Authentication Operations

    fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                val status = cloudBackupRepository.getAuthStatus()
                _authStatus.value = status
                
                if (status.isSignedIn) {
                    loadBackups()
                    loadStorageInfo()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to check authentication: ${e.message}"
                )
            }
        }
    }

    fun authenticate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = cloudBackupRepository.authenticate()
                when (result) {
                    is CloudResult.Success -> {
                        _authStatus.value = result.data
                        if (result.data.isSignedIn) {
                            loadBackups()
                            loadStorageInfo()
                        }
                    }
                    is CloudResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message
                        )
                    }
                    is CloudResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Authentication failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = cloudBackupRepository.signOut()
                when (result) {
                    is CloudResult.Success -> {
                        _authStatus.value = CloudAuthStatus(
                            isSignedIn = false,
                            userEmail = null,
                            userName = null,
                            provider = CloudProvider.GOOGLE_DRIVE,
                            hasPermissions = false
                        )
                        _backups.value = emptyList()
                        _storageInfo.value = null
                    }
                    is CloudResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message
                        )
                    }
                    is CloudResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Sign out failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Backup Operations

    fun createBackup(backupType: CloudBackupType = CloudBackupType.MANUAL) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBackupInProgress = true,
                backupProgress = null,
                error = null
            )
            
            try {
                cloudBackupRepository.createCloudBackup(backupType).collect { result ->
                    when (result) {
                        is CloudResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isBackupInProgress = false,
                                backupProgress = null,
                                lastBackupId = result.data
                            )
                            loadBackups() // Refresh backup list
                            loadStorageInfo() // Refresh storage info
                        }
                        is CloudResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isBackupInProgress = false,
                                backupProgress = null,
                                error = result.message
                            )
                        }
                        is CloudResult.Loading -> {
                            val stage = when {
                                result.progress < 0.2f -> CloudBackupStage.PREPARING
                                result.progress < 0.4f -> CloudBackupStage.PREPARING
                                result.progress < 0.6f -> CloudBackupStage.ENCRYPTING
                                result.progress < 0.8f -> CloudBackupStage.UPLOADING
                                else -> CloudBackupStage.UPLOADING
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                backupProgress = CloudBackupProgress(
                                    stage = stage,
                                    message = getStageMessage(stage),
                                    progress = result.progress
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBackupInProgress = false,
                    backupProgress = null,
                    error = "Backup failed: ${e.message}"
                )
            }
        }
    }

    fun restoreBackup(
        backupId: String,
        importMode: ImportMode = ImportMode.MERGE,
        skipDuplicates: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRestoreInProgress = true,
                restoreProgress = null,
                restoreResult = null,
                error = null
            )
            
            try {
                cloudBackupRepository.restoreCloudBackup(backupId, importMode, skipDuplicates).collect { result ->
                    when (result) {
                        is CloudResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isRestoreInProgress = false,
                                restoreProgress = null,
                                restoreResult = result.data
                            )
                        }
                        is CloudResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isRestoreInProgress = false,
                                restoreProgress = null,
                                error = result.message
                            )
                        }
                        is CloudResult.Loading -> {
                            val stage = when {
                                result.progress < 0.2f -> CloudBackupStage.PREPARING
                                result.progress < 0.5f -> CloudBackupStage.DOWNLOADING
                                result.progress < 0.6f -> CloudBackupStage.DECRYPTING
                                result.progress < 0.7f -> CloudBackupStage.VALIDATING
                                else -> CloudBackupStage.IMPORTING
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                restoreProgress = CloudBackupProgress(
                                    stage = stage,
                                    message = getStageMessage(stage),
                                    progress = result.progress
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoreInProgress = false,
                    restoreProgress = null,
                    error = "Restore failed: ${e.message}"
                )
            }
        }
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = cloudBackupRepository.deleteCloudBackup(backupId)
                when (result) {
                    is CloudResult.Success -> {
                        loadBackups() // Refresh backup list
                        loadStorageInfo() // Refresh storage info
                    }
                    is CloudResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message
                        )
                    }
                    is CloudResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Delete failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Data Loading

    fun loadBackups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingBackups = true, error = null)
            
            try {
                val result = cloudBackupRepository.listCloudBackups()
                when (result) {
                    is CloudResult.Success -> {
                        _backups.value = result.data
                    }
                    is CloudResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message
                        )
                    }
                    is CloudResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load backups: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingBackups = false)
            }
        }
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                val result = cloudBackupRepository.getCloudStorageInfo()
                when (result) {
                    is CloudResult.Success -> {
                        _storageInfo.value = result.data
                    }
                    is CloudResult.Error -> {
                        // Silently handle storage info errors
                        _storageInfo.value = null
                    }
                    is CloudResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                // Silently handle storage info errors
                _storageInfo.value = null
            }
        }
    }

    // UI Actions

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearRestoreResult() {
        _uiState.value = _uiState.value.copy(restoreResult = null)
    }

    // Helper Functions

    private fun getStageMessage(stage: CloudBackupStage): String {
        return when (stage) {
            CloudBackupStage.PREPARING -> "Preparing backup data..."
            CloudBackupStage.ENCRYPTING -> "Encrypting backup data..."
            CloudBackupStage.UPLOADING -> "Uploading to cloud..."
            CloudBackupStage.DOWNLOADING -> "Downloading from cloud..."
            CloudBackupStage.DECRYPTING -> "Decrypting backup data..."
            CloudBackupStage.VALIDATING -> "Validating backup data..."
            CloudBackupStage.IMPORTING -> "Importing data..."
            CloudBackupStage.COMPLETED -> "Operation completed"
            CloudBackupStage.ERROR -> "Operation failed"
        }
    }
}

// UI State data class
data class CloudBackupUiState(
    val isLoading: Boolean = false,
    val isLoadingBackups: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val backupProgress: CloudBackupProgress? = null,
    val restoreProgress: CloudBackupProgress? = null,
    val restoreResult: CloudRestoreResult? = null,
    val lastBackupId: String? = null,
    val error: String? = null
)

// Factory for creating CloudBackupViewModel
class CloudBackupViewModelFactory(
    private val cloudBackupRepository: CloudBackupRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CloudBackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CloudBackupViewModel(cloudBackupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}