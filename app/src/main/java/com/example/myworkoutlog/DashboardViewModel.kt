package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val widgetRepository: WidgetRepositorySimplified,
    private val activeCycleDao: ActiveCycleDao,
    private val analyticsRepository: AnalyticsRepository
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
            defaultTimeframe = "30days"
        )
    )
    val dashboardPreferences: StateFlow<DashboardPreferences> = _dashboardPreferences.asStateFlow()
    
    // Customization mode state
    private val _isCustomizationMode = MutableStateFlow(false)
    val isCustomizationMode: StateFlow<Boolean> = _isCustomizationMode.asStateFlow()
    
    // Drag and drop state management
    private val _dragState = MutableStateFlow<DragState?>(null)
    val dragState: StateFlow<DragState?> = _dragState.asStateFlow()
    
    // Optimistic widget order state (for immediate drag feedback)
    private val _optimisticWidgetOrder = MutableStateFlow<List<DashboardWidget>?>(null)
    val optimisticWidgetOrder: StateFlow<List<DashboardWidget>?> = _optimisticWidgetOrder.asStateFlow()
    
    // Get active cycle as a flow
    private val activeCycle = activeCycleDao.getActiveCycle()
    
    // Base dashboard state (without optimistic updates)
    private val baseDashboardState: StateFlow<DashboardState> = combine(
        activeCycle,
        _refreshTrigger,
        _dashboardPreferences
    ) { cycle, _, preferences ->
        Triple(cycle, preferences, Unit)
    }.flatMapLatest { (cycle, preferences, _) ->
        widgetRepository.getDashboardState(cycle).map { state ->
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
    
    // Dashboard state with optimistic updates for drag operations
    val dashboardState: StateFlow<DashboardState> = combine(
        baseDashboardState,
        _optimisticWidgetOrder
    ) { baseState, optimisticOrder ->
        if (optimisticOrder != null) {
            baseState.copy(widgets = optimisticOrder)
        } else {
            baseState
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
    
    
    // Legacy method - now delegates to new drag system
    fun reorderWidgets(fromIndex: Int, toIndex: Int) {
        val currentState = dashboardState.value
        val widgets = currentState.widgets.toMutableList()
        
        if (fromIndex in widgets.indices && toIndex in widgets.indices && fromIndex != toIndex) {
            // Perform the reordering
            val draggedWidget = widgets.removeAt(fromIndex)
            widgets.add(toIndex, draggedWidget)
            
            // Use the new persist method
            persistWidgetOrder(widgets)
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
        // Cancel any ongoing drag when exiting customization mode
        if (_isCustomizationMode.value) {
            cancelDrag()
        }
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
        // TODO: Implement streak calculation
        return 0
    }
    
    // Drag and drop methods
    fun startDrag(widgetId: String) {
        val baseWidgets = baseDashboardState.value.widgets
        val draggedIndex = baseWidgets.indexOfFirst { it.id == widgetId }
        
        if (draggedIndex >= 0) {
            _dragState.value = DragState(
                draggedWidgetId = widgetId,
                draggedIndex = draggedIndex,
                currentOffset = Offset.Zero, // Always start at zero
                targetIndex = draggedIndex
            )
            
            // Set optimistic order to base order
            _optimisticWidgetOrder.value = baseWidgets
        }
    }
    
    fun updateDrag(offset: Offset, cardHeight: Float) {
        val currentDragState = _dragState.value ?: return
        val baseWidgets = baseDashboardState.value.widgets // Use base widgets, not current state
        
        // Calculate target index based on cumulative drag offset
        val pixelsPerCard = cardHeight * 1.2f // Account for spacing
        val offsetChange = (offset.y / pixelsPerCard).toInt()
        val newTargetIndex = (currentDragState.draggedIndex + offsetChange)
            .coerceIn(0, baseWidgets.size - 1)
        
        // Update drag state with the received offset (already calculated correctly in UI)
        _dragState.value = currentDragState.copy(
            currentOffset = offset,
            targetIndex = newTargetIndex
        )
        
        // Update optimistic order to show real-time reordering
        if (newTargetIndex != currentDragState.draggedIndex) {
            val reorderedWidgets = baseWidgets.toMutableList()
            val draggedWidget = reorderedWidgets.removeAt(currentDragState.draggedIndex)
            reorderedWidgets.add(newTargetIndex, draggedWidget)
            _optimisticWidgetOrder.value = reorderedWidgets
        } else {
            // Reset to original order if dragged back to start position
            _optimisticWidgetOrder.value = baseWidgets
        }
    }
    
    fun endDrag() {
        val currentDragState = _dragState.value ?: return
        val finalWidgets = _optimisticWidgetOrder.value ?: return
        
        // Clear drag state
        _dragState.value = null
        
        // Persist the final order
        if (currentDragState.draggedIndex != currentDragState.targetIndex) {
            persistWidgetOrder(finalWidgets)
        }
        
        // Clear optimistic order (will fall back to persisted order)
        _optimisticWidgetOrder.value = null
    }
    
    fun cancelDrag() {
        // Clear both drag state and optimistic order
        _dragState.value = null
        _optimisticWidgetOrder.value = null
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

// Drag and drop state
data class DragState(
    val draggedWidgetId: String,
    val draggedIndex: Int,
    val currentOffset: Offset,
    val targetIndex: Int
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