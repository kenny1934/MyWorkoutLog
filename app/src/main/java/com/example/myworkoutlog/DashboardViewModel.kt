package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val widgetRepository: WidgetRepositorySimplified,
    private val activeCycleDao: ActiveCycleDao,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _refreshTrigger = MutableStateFlow(0)
    
    // Get active cycle as a flow
    private val activeCycle = activeCycleDao.getActiveCycle()
    
    // Dashboard state that recomposes when active cycle changes or refresh is triggered
    val dashboardState: StateFlow<DashboardState> = combine(
        activeCycle,
        _refreshTrigger
    ) { cycle, _ ->
        // This will trigger recomposition when either activeCycle changes or refresh is called
        cycle
    }.flatMapLatest { cycle ->
        widgetRepository.getDashboardState(cycle)
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
    
    fun executeQuickAction(action: QuickAction, onNavigate: (String) -> Unit) {
        when (action.action) {
            QuickActionType.START_NEXT_SESSION -> {
                val currentState = dashboardState.value
                if (currentState.mode is DashboardMode.ActiveCycle) {
                    val nextSession = currentState.widgets
                        .filterIsInstance<DashboardWidget.NextSessionWidget>()
                        .firstOrNull()
                    
                    if (nextSession != null) {
                        // Navigate to workout logger with next session
                        val cycle = currentState.mode.cycle
                        val sessionInfo = nextSession.session
                        
                        // Find the week and session IDs for navigation
                        val week = cycle.cycleProgram.weeks.find { week ->
                            week.sessions.any { it.sessionName == sessionInfo.name }
                        }
                        val session = week?.sessions?.find { it.sessionName == sessionInfo.name }
                        
                        if (week != null && session != null) {
                            val route = Screen.WorkoutLogger.createRoute(
                                templateId = session.workoutTemplateId,
                                cycleId = cycle.cycleUuid,
                                weekId = week.id,
                                sessionId = session.id
                            )
                            onNavigate(route)
                        }
                    }
                }
            }
            
            QuickActionType.START_NEW_CYCLE -> {
                onNavigate(Screen.Library.route)
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
    
    fun reorderWidgets(newOrder: List<String>) {
        // TODO: Implement widget reordering
        viewModelScope.launch {
            // Save new order preference and refresh
            refreshDashboard()
        }
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