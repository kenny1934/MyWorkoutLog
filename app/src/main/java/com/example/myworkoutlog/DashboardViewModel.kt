package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val widgetRepository: WidgetRepositorySimplified,
    private val activeCycleDao: ActiveCycleDao,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _refreshTrigger = MutableStateFlow(0)
    
    // Get active cycle as a flow
    private val activeCycle = activeCycleDao.getActiveCycle()
    
    // Dashboard state that recomposes when active cycle, preferences, or refresh is triggered
    val dashboardState: StateFlow<DashboardState> = combine(
        activeCycle,
        _refreshTrigger,
        _dashboardPreferences
    ) { cycle, _, preferences ->
        // This will trigger recomposition when cycle, preferences change or refresh is called
        Triple(cycle, preferences, Unit)
    }.flatMapLatest { (cycle, preferences, _) ->
        widgetRepository.getDashboardState(cycle).map { state ->
            // Apply preferences to filter and reorder widgets
            applyPreferencesToDashboardState(state, preferences)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState(
            mode = DashboardMode.NoActiveCycle,
            widgets = emptyList(),
            quickActions = emptyList(),
            insights = emptyList(),
            isLoading = true
        )
    )
    
    // Loading state for dashboard
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Pull-to-refresh specific loading state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // Error state for dashboard
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        refreshDashboard()
    }
    
    fun refreshDashboard() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _refreshTrigger.value = _refreshTrigger.value + 1
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun onPullToRefresh() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _error.value = null
                
                // Trigger data refresh
                _refreshTrigger.value = _refreshTrigger.value + 1
                
                // Add small delay to ensure smooth animation
                kotlinx.coroutines.delay(500)
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh dashboard"
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun executeQuickAction(action: QuickAction, onNavigate: (String) -> Unit) {
        when (action.action) {
            QuickActionType.START_NEXT_SESSION -> {
                val currentState = dashboardState.value
                if (currentState.mode is DashboardMode.ActiveCycle) {
                    val cycle = currentState.mode.cycle
                    
                    // Find the next session to log based on completedSessions
                    val completedSessionIds = cycle.completedSessions.keys.toSet()
                    
                    // Find the first incomplete session across all weeks
                    var nextSession: ProgramSessionDefinition? = null
                    var nextWeek: ProgramWeekDefinition? = null
                    
                    for (week in cycle.cycleProgram.weeks) {
                        for (session in week.sessions) {
                            if (session.id !in completedSessionIds) {
                                nextSession = session
                                nextWeek = week
                                break
                            }
                        }
                        if (nextSession != null) break
                    }
                    
                    if (nextWeek != null && nextSession != null) {
                        val route = Screen.WorkoutLogger.createRoute(
                            templateId = nextSession.workoutTemplateId,
                            cycleId = cycle.cycleUuid,
                            weekId = nextWeek.id,
                            sessionId = nextSession.id
                        )
                        onNavigate(route)
                    }
                }
            }
            
            QuickActionType.START_NEW_CYCLE -> {
                onNavigate(Screen.Programs.route)
            }
            
            QuickActionType.VIEW_ANALYTICS -> {
                onNavigate(Screen.Analytics.route)
            }
            
            QuickActionType.ADD_BODYWEIGHT -> {
                // TODO: Implement bodyweight logging dialog or screen
                onNavigate(Screen.WorkoutLogger.route) // Placeholder
            }
            
            QuickActionType.VIEW_HISTORY -> {
                onNavigate(Screen.History.route)
            }
            
            QuickActionType.COMPLETE_CYCLE -> {
                // End the current cycle and navigate to analytics with cycle summary
                viewModelScope.launch(Dispatchers.IO) {
                    activeCycleDao.clear()
                }
                onNavigate(Screen.Analytics.route)
            }
            
            QuickActionType.VIEW_CYCLE_ANALYTICS -> {
                // Navigate to analytics focused on current cycle
                onNavigate(Screen.Analytics.route)
            }
            
            QuickActionType.LOG_QUICK_WORKOUT -> {
                // TODO: Implement quick workout logging
                onNavigate(Screen.WorkoutLogger.route)
            }
        }
    }
    
    fun dismissInsight(insightId: String) {
        // TODO: Implement insight dismissal logic
        viewModelScope.launch {
            // For now, just refresh to remove the insight
            refreshDashboard()
        }
    }
    
    fun toggleWidget(widgetId: String, isVisible: Boolean) {
        // TODO: Implement widget visibility toggle
        viewModelScope.launch {
            // Save preference and refresh
            refreshDashboard()
        }
    }
    
    // Dashboard preferences state
    private val _dashboardPreferences = MutableStateFlow(
        DashboardPreferences(
            widgetConfigs = emptyList(),
            showMotivationalMessages = true,
            showAchievements = true,
            showInsights = true,
            autoRefresh = true,
            defaultTimeframe = "30days"
        )
    )
    val dashboardPreferences: StateFlow<DashboardPreferences> = _dashboardPreferences.asStateFlow()
    
    // Customization mode state
    private val _isCustomizationMode = MutableStateFlow(false)
    val isCustomizationMode: StateFlow<Boolean> = _isCustomizationMode.asStateFlow()
    
    fun reorderWidgets(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentState = dashboardState.value
            val widgets = currentState.widgets.toMutableList()
            
            if (fromIndex in widgets.indices && toIndex in widgets.indices && fromIndex != toIndex) {
                // Perform the reordering
                val draggedWidget = widgets.removeAt(fromIndex)
                widgets.add(toIndex, draggedWidget)
                
                // Update widget configs with new order
                val newConfigs = widgets.mapIndexed { index, widget ->
                    WidgetConfig(
                        widgetType = widget.id,
                        isEnabled = true, // Keep all visible widgets enabled after reordering
                        position = index
                    )
                }
                
                // Update preferences immediately - this will trigger the state recomposition
                _dashboardPreferences.value = _dashboardPreferences.value.copy(
                    widgetConfigs = newConfigs
                )
                
                // Save preferences
                saveDashboardPreferences()
                
                // Note: No need to call refreshDashboard() as the preference change will trigger state update
            }
        }
    }
    
    fun toggleWidgetVisibility(widgetId: String) {
        viewModelScope.launch {
            val currentState = dashboardState.value
            val currentConfigs = _dashboardPreferences.value.widgetConfigs.toMutableList()
            val configIndex = currentConfigs.indexOfFirst { it.widgetType == widgetId }
            
            if (configIndex >= 0) {
                // Toggle existing config
                currentConfigs[configIndex] = currentConfigs[configIndex].copy(
                    isEnabled = !currentConfigs[configIndex].isEnabled
                )
            } else {
                // Create configs for all current widgets if they don't exist
                val allWidgetConfigs = currentState.widgets.mapIndexed { index, widget ->
                    WidgetConfig(
                        widgetType = widget.id,
                        isEnabled = if (widget.id == widgetId) false else true, // Toggle the specific widget
                        position = index
                    )
                }
                currentConfigs.clear()
                currentConfigs.addAll(allWidgetConfigs)
            }
            
            // Update preferences immediately - this will trigger state recomposition
            _dashboardPreferences.value = _dashboardPreferences.value.copy(
                widgetConfigs = currentConfigs
            )
            
            // Save preferences
            saveDashboardPreferences()
            
            // Note: No need to call refreshDashboard() as the preference change will trigger state update
        }
    }
    
    fun toggleCustomizationMode() {
        _isCustomizationMode.value = !_isCustomizationMode.value
    }
    
    // Preference persistence (basic in-memory implementation)
    private fun saveDashboardPreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Implement SharedPreferences or Room persistence
            // For now, preferences persist only during app session
            println("Dashboard preferences saved: ${_dashboardPreferences.value}")
        }
    }
    
    private fun loadDashboardPreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Load from SharedPreferences or Room database
            // For now, using default preferences
            val defaultPreferences = DashboardPreferences(
                widgetConfigs = emptyList(),
                showMotivationalMessages = true,
                showAchievements = true,
                showInsights = true,
                autoRefresh = true,
                defaultTimeframe = "30days"
            )
            _dashboardPreferences.value = defaultPreferences
        }
    }
    
    init {
        // Load preferences on initialization
        loadDashboardPreferences()
    }
    
    private fun applyPreferencesToDashboardState(
        state: DashboardState,
        preferences: DashboardPreferences
    ): DashboardState {
        val configs = preferences.widgetConfigs
        if (configs.isEmpty()) {
            // No preferences set, return original state
            return state
        }
        
        // Create a map of widget configurations for quick lookup
        val configMap = configs.associateBy { it.widgetType }
        
        // Filter widgets based on visibility preferences
        val filteredWidgets = state.widgets.filter { widget ->
            val config = configMap[widget.id]
            config?.isEnabled ?: widget.isVisible // Default to widget's original visibility
        }
        
        // Reorder widgets based on preferences
        val reorderedWidgets = if (configs.isNotEmpty()) {
            // Sort by position from preferences, then by original order for missing widgets
            filteredWidgets.sortedWith { widget1, widget2 ->
                val config1 = configMap[widget1.id]
                val config2 = configMap[widget2.id]
                
                when {
                    config1 != null && config2 != null -> config1.position.compareTo(config2.position)
                    config1 != null && config2 == null -> -1
                    config1 == null && config2 != null -> 1
                    else -> widget1.priority.compareTo(widget2.priority) // Fallback to original priority
                }
            }
        } else {
            filteredWidgets
        }
        
        return state.copy(widgets = reorderedWidgets)
    }
    
    // Helper method to get widget by ID
    fun getWidget(widgetId: String): DashboardWidget? {
        return dashboardState.value.widgets.find { it.id == widgetId }
    }
    
    // Helper method to check if specific widget type is visible
    fun isWidgetVisible(widgetType: String): Boolean {
        return dashboardState.value.widgets.any { it::class.simpleName?.contains(widgetType) == true }
    }
    
    // Get insights by priority
    fun getInsightsByPriority(priority: InsightPriority): List<SmartInsight> {
        return dashboardState.value.insights.filter { it.priority == priority }
    }
    
    // Get high priority insights count
    fun getHighPriorityInsightsCount(): Int {
        return dashboardState.value.insights.count { 
            it.priority == InsightPriority.HIGH || it.priority == InsightPriority.URGENT 
        }
    }
    
    // Check if dashboard is in loading state
    fun isDashboardLoading(): Boolean {
        return dashboardState.value.isLoading || _isLoading.value
    }
    
    // Get current dashboard mode
    fun getCurrentMode(): DashboardMode {
        return dashboardState.value.mode
    }
    
    // Check if user has active cycle
    fun hasActiveCycle(): Boolean {
        return dashboardState.value.mode is DashboardMode.ActiveCycle
    }
    
    // Get current cycle if available
    fun getCurrentCycle(): ActiveProgramCycle? {
        return when (val mode = dashboardState.value.mode) {
            is DashboardMode.ActiveCycle -> mode.cycle
            else -> null
        }
    }
    
    // Get current cycle progress if available
    fun getCurrentCycleProgress(): CycleProgress? {
        return when (val mode = dashboardState.value.mode) {
            is DashboardMode.ActiveCycle -> mode.progress
            else -> null
        }
    }
    
    // Analytics integration methods
    fun getQuickAnalytics(): Flow<QuickAnalytics> = flow {
        val currentCycle = getCurrentCycle()
        val analytics = QuickAnalytics(
            totalWorkouts = analyticsRepository.getTotalWorkoutCount(),
            thisWeekWorkouts = analyticsRepository.getThisWeekWorkoutCount(),
            currentStreak = calculateStreak(),
            avgWeeklyVolume = analyticsRepository.getAverageWeeklyVolume()
        )
        emit(analytics)
    }
    
    private suspend fun calculateStreak(): Int {
        // TODO: Implement streak calculation
        return 0
    }
}

// Helper data class for quick analytics
data class QuickAnalytics(
    val totalWorkouts: Int,
    val thisWeekWorkouts: Int,
    val currentStreak: Int,
    val avgWeeklyVolume: Float
)

// ViewModelFactory for DashboardViewModel
class DashboardViewModelFactory(
    private val widgetRepository: WidgetRepositorySimplified,
    private val activeCycleDao: ActiveCycleDao,
    private val analyticsRepository: AnalyticsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(widgetRepository, activeCycleDao, analyticsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}