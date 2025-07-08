package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val widgetRepository: WidgetRepositorySimplified,
    private val activeCycleDao: ActiveCycleDao,
    private val analyticsRepository: AnalyticsRepository,
    private val preferencesManager: DashboardPreferencesManager
) : ViewModel() {
    
    private val _refreshTrigger = MutableStateFlow(0)
    
    // Dashboard preferences state - must be declared before dashboardState
    private val _dashboardPreferences = MutableStateFlow(
        DashboardPreferences(
            widgetConfigs = emptyList(),
            showMotivationalMessages = true,
            showAchievements = true,
            showInsights = true,
            autoRefresh = true,
            defaultTimeframe = "30days",
            dismissedInsights = emptySet()
        )
    )
    val dashboardPreferences: StateFlow<DashboardPreferences> = _dashboardPreferences.asStateFlow()
    
    // Customization mode state
    private val _isCustomizationMode = MutableStateFlow(false)
    val isCustomizationMode: StateFlow<Boolean> = _isCustomizationMode.asStateFlow()
    
    // Widget reordering state (simplified for library integration)
    private val _isReordering = MutableStateFlow(false)
    val isReordering: StateFlow<Boolean> = _isReordering.asStateFlow()
    
    // Get active cycle as a flow
    private val activeCycle = activeCycleDao.getActiveCycle()
    
    // Dashboard state with preferences applied
    val dashboardState: StateFlow<DashboardState> = combine(
        activeCycle,
        _refreshTrigger,
        _dashboardPreferences
    ) { cycle, _, preferences ->
        Triple(cycle, preferences, Unit)
    }.flatMapLatest { (cycle, preferences, _) ->
        widgetRepository.getDashboardState(cycle, preferences.dismissedInsights).map { state ->
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
                // Use the same logic as NextSessionWidget for consistency
                val currentState = dashboardState.value
                val nextSessionWidget = currentState.widgets.find { it is DashboardWidget.NextSessionWidget } as? DashboardWidget.NextSessionWidget
                
                if (nextSessionWidget != null) {
                    // Use the exact same route parameters as the NextSessionWidget
                    val templateId = nextSessionWidget.templateId ?: nextSessionWidget.session.templateId
                    val cycleId = nextSessionWidget.cycleId
                    val weekId = nextSessionWidget.weekId
                    val sessionId = nextSessionWidget.sessionId
                    
                    if (templateId != null && cycleId != null && weekId != null && sessionId != null) {
                        val route = Screen.WorkoutLogger.createRoute(
                            templateId = templateId,
                            cycleId = cycleId,
                            weekId = weekId,
                            sessionId = sessionId
                        )
                        onNavigate(route)
                    } else {
                        // Fallback to simple template route if any parameter is null
                        templateId?.let { id ->
                            onNavigate(Screen.WorkoutLogger.createRoute(id))
                        } ?: onNavigate(Screen.WorkoutLogger.route)
                    }
                } else {
                    // Fallback to original logic if NextSessionWidget not found
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
            }
            
            QuickActionType.START_NEW_CYCLE -> {
                onNavigate(Screen.Programs.route)
            }
            
            QuickActionType.VIEW_ANALYTICS -> {
                onNavigate(Screen.Analytics.defaultRoute)
            }
            
            QuickActionType.ADD_BODYWEIGHT -> {
                // Navigate to workout logger for bodyweight entry
                // This will open the workout logger where users can enter bodyweight at the top
                onNavigate(Screen.WorkoutLogger.route)
            }
            
            QuickActionType.VIEW_HISTORY -> {
                onNavigate(Screen.History.route)
            }
            
            QuickActionType.COMPLETE_CYCLE -> {
                // End the current cycle and navigate to analytics with cycle comparison
                viewModelScope.launch {
                    try {
                        // Get the current cycle ID before clearing
                        val currentCycle = getCurrentCycle()
                        val currentCycleId = currentCycle?.cycleUuid
                        
                        // Clear the cycle on IO dispatcher
                        withContext(Dispatchers.IO) {
                            activeCycleDao.clear()
                        }
                        
                        // Navigate on main thread with proper error handling
                        try {
                            currentCycleId?.let { cycleId ->
                                onNavigate(Screen.Analytics.createRouteWithCycle(cycleId))
                            } ?: run {
                                onNavigate(Screen.Analytics.createRouteWithTab("Comparison"))
                            }
                        } catch (navError: Exception) {
                            // Fallback navigation if specific route fails
                            onNavigate(Screen.Analytics.defaultRoute)
                        }
                    } catch (e: Exception) {
                        // Log error but don't crash - just navigate to analytics
                        println("Error completing cycle: ${e.message}")
                        onNavigate(Screen.Analytics.defaultRoute)
                    }
                }
            }
            
            QuickActionType.VIEW_CYCLE_ANALYTICS -> {
                // Navigate to analytics focused on current cycle comparison
                onNavigate(Screen.Analytics.createRouteWithTab("Comparison"))
            }
            
            QuickActionType.LOG_QUICK_WORKOUT -> {
                // Navigate to workout logger for quick workout entry
                onNavigate(Screen.WorkoutLogger.route)
            }
        }
    }
    
    fun dismissInsight(insightId: String) {
        viewModelScope.launch {
            try {
                // Store dismissed insights in preferences
                val currentPreferences = _dashboardPreferences.value
                val dismissedInsights = currentPreferences.dismissedInsights.toMutableSet()
                dismissedInsights.add(insightId)
                
                _dashboardPreferences.value = currentPreferences.copy(
                    dismissedInsights = dismissedInsights
                )
                
                // Save updated preferences
                saveDashboardPreferences()
                
                // Trigger dashboard refresh to update insights
                refreshDashboard()
            } catch (e: Exception) {
                println("Error dismissing insight: ${e.message}")
            }
        }
    }
    
    // Debug method to reset dismissed insights for testing
    fun resetDismissedInsights() {
        viewModelScope.launch {
            try {
                _dashboardPreferences.value = _dashboardPreferences.value.copy(
                    dismissedInsights = emptySet()
                )
                saveDashboardPreferences()
                refreshDashboard()
            } catch (e: Exception) {
                println("Error resetting dismissed insights: ${e.message}")
            }
        }
    }
    
    fun executeInsightAction(insight: SmartInsight, onNavigate: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Use action text for specific routing instead of generic type-based routing
                when (insight.actionText) {
                    "View Progress" -> {
                        // Navigate to analytics for progress tracking
                        onNavigate(Screen.Analytics.defaultRoute)
                    }
                    "View Schedule" -> {
                        // Navigate to programs for schedule/planning view
                        onNavigate(Screen.Programs.route)
                    }
                    "Browse Programs" -> {
                        // Navigate to programs for program selection
                        onNavigate(Screen.Programs.route)
                    }
                    "Start Workout", "Start First Workout" -> {
                        // For workout start, check if we have active cycle context
                        val currentState = dashboardState.value
                        val nextSessionWidget = currentState.widgets.find { it is DashboardWidget.NextSessionWidget } as? DashboardWidget.NextSessionWidget
                        
                        if (nextSessionWidget != null) {
                            // Use NextSessionWidget route for consistency
                            val templateId = nextSessionWidget.templateId ?: nextSessionWidget.session.templateId
                            val cycleId = nextSessionWidget.cycleId
                            val weekId = nextSessionWidget.weekId
                            val sessionId = nextSessionWidget.sessionId
                            
                            if (templateId != null && cycleId != null && weekId != null && sessionId != null) {
                                val route = Screen.WorkoutLogger.createRoute(
                                    templateId = templateId,
                                    cycleId = cycleId,
                                    weekId = weekId,
                                    sessionId = sessionId
                                )
                                onNavigate(route)
                            } else {
                                // Fallback to simple template route if any parameter is null
                                templateId?.let { id ->
                                    onNavigate(Screen.WorkoutLogger.createRoute(id))
                                } ?: onNavigate(Screen.WorkoutLogger.route)
                            }
                        } else {
                            // Navigate to general workout logger
                            onNavigate(Screen.WorkoutLogger.route)
                        }
                    }
                    "View Analytics" -> {
                        // Navigate to analytics dashboard
                        onNavigate(Screen.Analytics.defaultRoute)
                    }
                    "View History" -> {
                        // Navigate to workout history
                        onNavigate(Screen.History.route)
                    }
                    else -> {
                        // Fallback: route based on insight type for unknown actions
                        when (insight.type) {
                            InsightType.PERFORMANCE, InsightType.CELEBRATION -> {
                                onNavigate(Screen.Analytics.defaultRoute)
                            }
                            InsightType.RECOMMENDATION -> {
                                onNavigate(Screen.Programs.route)
                            }
                            else -> {
                                onNavigate(Screen.Analytics.defaultRoute)
                            }
                        }
                    }
                }
                
                // Optionally dismiss the insight after action
                dismissInsight(insight.id)
            } catch (e: Exception) {
                println("Error executing insight action: ${e.message}")
            }
        }
    }
    
    fun toggleWidget(widgetId: String, isVisible: Boolean) {
        viewModelScope.launch {
            val currentState = dashboardState.value
            val currentConfigs = _dashboardPreferences.value.widgetConfigs.toMutableList()
            val configIndex = currentConfigs.indexOfFirst { it.widgetType == widgetId }
            
            if (configIndex >= 0) {
                // Update existing config with explicit visibility
                currentConfigs[configIndex] = currentConfigs[configIndex].copy(
                    isEnabled = isVisible
                )
            } else {
                // Create configs for all current widgets if they don't exist
                val allWidgetConfigs = currentState.widgets.mapIndexed { index, widget ->
                    WidgetConfig(
                        widgetType = widget.id,
                        isEnabled = if (widget.id == widgetId) isVisible else true,
                        position = index
                    )
                }
                currentConfigs.clear()
                currentConfigs.addAll(allWidgetConfigs)
            }
            
            // Update preferences immediately
            _dashboardPreferences.value = _dashboardPreferences.value.copy(
                widgetConfigs = currentConfigs
            )
            
            // Save preferences
            saveDashboardPreferences()
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
    
    // Get all available widgets including hidden ones
    fun getAllAvailableWidgets(): Flow<List<DashboardWidget>> {
        return combine(
            activeCycle,
            _refreshTrigger
        ) { cycle, _ ->
            cycle
        }.flatMapLatest { cycle ->
            widgetRepository.getDashboardState(cycle).map { state ->
                state.widgets // Return all widgets without filtering
            }
        }
    }
    
    // Hidden widgets state for UI
    val hiddenWidgets: StateFlow<List<DashboardWidget>> = combine(
        activeCycle,
        _refreshTrigger,
        _dashboardPreferences
    ) { cycle, _, preferences ->
        Triple(cycle, preferences, Unit)
    }.flatMapLatest { (cycle, preferences, _) ->
        widgetRepository.getDashboardState(cycle).map { state ->
            val configs = preferences.widgetConfigs
            val configMap = configs.associateBy { it.widgetType }
            
            state.widgets.filter { widget ->
                val config = configMap[widget.id]
                config?.isEnabled == false
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Preference persistence using SharedPreferences
    private fun saveDashboardPreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                preferencesManager.saveDashboardPreferences(_dashboardPreferences.value)
            } catch (e: Exception) {
                // Log error but don't crash - preferences are not critical
                println("Error saving dashboard preferences: ${e.message}")
            }
        }
    }
    
    private fun loadDashboardPreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loadedPreferences = preferencesManager.loadDashboardPreferences()
                _dashboardPreferences.value = loadedPreferences
            } catch (e: Exception) {
                // Fall back to default preferences if loading fails
                println("Error loading dashboard preferences: ${e.message}")
                val defaultPreferences = DashboardPreferences(
                    widgetConfigs = emptyList(),
                    showMotivationalMessages = true,
                    showAchievements = true,
                    showInsights = true,
                    autoRefresh = true,
                    defaultTimeframe = "30days",
                    dismissedInsights = emptySet()
                )
                _dashboardPreferences.value = defaultPreferences
            }
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
        
        // Create a map of widget configurations for quick lookup
        val configMap = configs.associateBy { it.widgetType }
        
        // Filter widgets based on visibility preferences
        val filteredWidgets = state.widgets.filter { widget ->
            val config = configMap[widget.id]
            config?.isEnabled ?: true // Default to visible if no config exists
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
            // No preferences set, use original order
            filteredWidgets.sortedBy { it.priority }
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
        return try {
            analyticsRepository.getCurrentStreak()
        } catch (e: Exception) {
            // Fallback calculation or return 0 if analytics repository doesn't have this method
            0
        }
    }
    
    // Simplified reordering method for library integration
    fun reorderWidgets(fromIndex: Int, toIndex: Int) {
        _isReordering.value = true
        
        val currentWidgets = dashboardState.value.widgets.toMutableList()
        if (fromIndex in currentWidgets.indices && toIndex in currentWidgets.indices && fromIndex != toIndex) {
            // Perform the reordering
            val draggedWidget = currentWidgets.removeAt(fromIndex)
            currentWidgets.add(toIndex, draggedWidget)
            
            // Persist the new order
            persistWidgetOrder(currentWidgets)
        }
        
        _isReordering.value = false
    }
    
    // Arrow button reordering methods
    fun moveWidgetUp(widgetIndex: Int) {
        if (widgetIndex <= 0) return // Can't move first item up
        
        _isReordering.value = true
        val currentWidgets = dashboardState.value.widgets.toMutableList()
        
        if (widgetIndex < currentWidgets.size) {
            // Swap with previous item
            val temp = currentWidgets[widgetIndex]
            currentWidgets[widgetIndex] = currentWidgets[widgetIndex - 1]
            currentWidgets[widgetIndex - 1] = temp
            
            persistWidgetOrder(currentWidgets)
        }
        
        _isReordering.value = false
    }
    
    fun moveWidgetDown(widgetIndex: Int) {
        val currentWidgets = dashboardState.value.widgets.toMutableList()
        if (widgetIndex >= currentWidgets.size - 1) return // Can't move last item down
        
        _isReordering.value = true
        
        if (widgetIndex >= 0) {
            // Swap with next item
            val temp = currentWidgets[widgetIndex]
            currentWidgets[widgetIndex] = currentWidgets[widgetIndex + 1]
            currentWidgets[widgetIndex + 1] = temp
            
            persistWidgetOrder(currentWidgets)
        }
        
        _isReordering.value = false
    }
    
    private fun persistWidgetOrder(widgets: List<DashboardWidget>) {
        viewModelScope.launch {
            // Update widget configs with new order
            val newConfigs = widgets.mapIndexed { index, widget ->
                WidgetConfig(
                    widgetType = widget.id,
                    isEnabled = true, // Keep all visible widgets enabled after reordering
                    position = index
                )
            }
            
            // Update preferences
            _dashboardPreferences.value = _dashboardPreferences.value.copy(
                widgetConfigs = newConfigs
            )
            
            // Save preferences
            saveDashboardPreferences()
        }
    }
}

// Helper data class for quick analytics
data class QuickAnalytics(
    val totalWorkouts: Int,
    val thisWeekWorkouts: Int,
    val currentStreak: Int,
    val avgWeeklyVolume: Float
)

// Helper for combining 4 flows  
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// ViewModelFactory for DashboardViewModel
class DashboardViewModelFactory(
    private val widgetRepository: WidgetRepositorySimplified,
    private val activeCycleDao: ActiveCycleDao,
    private val analyticsRepository: AnalyticsRepository,
    private val preferencesManager: DashboardPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(widgetRepository, activeCycleDao, analyticsRepository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}