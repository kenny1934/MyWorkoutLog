package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController
) {
    EnhancedDashboardScreen(
        dashboardViewModel = dashboardViewModel,
        navController = navController
    )
}


@Composable
fun ArrowReorderWidgetCard(
    widget: DashboardWidget,
    navController: NavHostController,
    isCustomizationMode: Boolean,
    isWidgetVisible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleVisibility: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEndCycle: (() -> Unit)? = null
) {
    // Use transparent Card to preserve enhanced styling while maintaining structure
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Render the actual widget content
            when (widget) {
            is DashboardWidget.WelcomeWidget -> SimpleWelcomeWidgetCard(widget)
            is DashboardWidget.QuickStatsWidget -> SimpleQuickStatsWidgetCard(widget)
            is DashboardWidget.CycleProgressWidget -> SimpleCycleProgressWidgetCard(
                widget = widget,
                navController = navController,
                onEndCycle = onEndCycle
            )
            is DashboardWidget.ActivityHeatmapWidget -> SimpleActivityHeatmapWidgetCard(widget)
            is DashboardWidget.BodyweightTrendWidget -> SimpleBodyweightTrendWidgetCard(widget)
            is DashboardWidget.PerformanceTrendWidget -> SimplePerformanceTrendWidgetCard(
                widget = widget,
                navController = navController
            )
            is DashboardWidget.NextSessionWidget -> SimpleNextSessionWidgetCard(widget)
            is DashboardWidget.VolumeProgressWidget -> SimpleVolumeProgressWidgetCard(
                widget = widget,
                navController = navController
            )
            is DashboardWidget.AchievementWidget -> SimpleAchievementWidgetCard(widget)
            }

            // Customization controls — compact chip pinned to bottom-end so the
            // widget's own title and content stay readable while the user is
            // reordering / toggling visibility.
            if (isCustomizationMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move up",
                                tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move down",
                                tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = { onToggleVisibility(widget.id) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = if (isWidgetVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isWidgetVisible) "Hide widget" else "Show widget",
                                tint = if (isWidgetVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveWidgetGrid(
    widgets: List<DashboardWidget>,
    quickActions: List<QuickAction>,
    insights: List<SmartInsight>,
    isCustomizationMode: Boolean,
    dashboardPreferences: DashboardPreferences,
    hiddenWidgets: List<DashboardWidget>,
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController,
    layoutInfo: AdaptiveLayoutInfo,
    onShowCompleteCycleConfirmation: (Boolean) -> Unit,
    onPendingCompleteCycleAction: (QuickAction?) -> Unit
) {
    // Calculate layout values before LazyColumn
    val columnCount = if (layoutInfo.useTwoColumns) {
        smartColumnCount(minWidgetWidth = 280.dp)
    } else {
        1
    }
    val spacing = adaptiveSpacing()
    val widgetHeight = adaptiveWidgetHeight()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // High priority insights (same as compact mode)
        val urgentInsights = insights.filter {
            it.priority == InsightPriority.URGENT || it.priority == InsightPriority.HIGH
        }
        items(urgentInsights, key = { it.id }) { insight ->
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                EnhancedInsightCard(
                    insight = insight,
                    onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                    onAction = { insight ->
                        dashboardViewModel.executeInsightAction(insight) { route ->
                            navController.navigate(route)
                        }
                    }
                )
            }
        }

        // Low priority insights (same as compact mode)
        val lowPriorityInsights = insights.filter {
            it.priority == InsightPriority.LOW || it.priority == InsightPriority.MEDIUM
        }
        items(lowPriorityInsights, key = { it.id }) { insight ->
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                EnhancedInsightCard(
                    insight = insight,
                    onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                    onAction = { insight ->
                        dashboardViewModel.executeInsightAction(insight) { route ->
                            navController.navigate(route)
                        }
                    }
                )
            }
        }

        // Quick actions (if needed for large screens)
        if (quickActions.isNotEmpty()) {
            item {
                EnhancedDashboardWidgetCard(
                    title = "Quick Actions",
                    icon = Icons.Default.FlashOn
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(quickActions) { action ->
                            EnhancedQuickActionButton(
                                action = action,
                                onClick = { clickedAction ->
                                    when (clickedAction.action) {
                                        QuickActionType.COMPLETE_CYCLE -> {
                                            onPendingCompleteCycleAction(clickedAction)
                                            onShowCompleteCycleConfirmation(true)
                                        }
                                        else -> {
                                            dashboardViewModel.executeQuickAction(clickedAction) { route ->
                                                navController.navigate(route)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Widgets Section with Adaptive Grid
        if (layoutInfo.useTwoColumns) {
            // Smart grid layout that prevents widget squishing
            val chunkedWidgets = widgets.chunked(columnCount)

            items(chunkedWidgets) { widgetRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    widgetRow.forEach { widget ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = widgetHeight)
                        ) {
                            val index = widgets.indexOf(widget)
                            ArrowReorderWidgetCard(
                                widget = widget,
                                navController = navController,
                                isCustomizationMode = isCustomizationMode,
                                isWidgetVisible = dashboardPreferences.widgetConfigs.find { it.widgetType == widget.id }?.isEnabled != false,
                                canMoveUp = index > 0,
                                canMoveDown = index < widgets.size - 1,
                                onToggleVisibility = { widgetId -> dashboardViewModel.toggleWidgetVisibility(widgetId) },
                                onMoveUp = { dashboardViewModel.moveWidgetUp(index) },
                                onMoveDown = { dashboardViewModel.moveWidgetDown(index) },
                                onEndCycle = null
                            )
                        }
                    }
                    // Fill remaining space if row is not complete
                    repeat(columnCount - widgetRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // Single column layout for compact screens
            itemsIndexed(widgets) { index, widget ->
                ArrowReorderWidgetCard(
                    widget = widget,
                    navController = navController,
                    isCustomizationMode = isCustomizationMode,
                    isWidgetVisible = dashboardPreferences.widgetConfigs.find { it.widgetType == widget.id }?.isEnabled != false,
                    canMoveUp = index > 0,
                    canMoveDown = index < widgets.size - 1,
                    onToggleVisibility = { widgetId -> dashboardViewModel.toggleWidgetVisibility(widgetId) },
                    onMoveUp = { dashboardViewModel.moveWidgetUp(index) },
                    onMoveDown = { dashboardViewModel.moveWidgetDown(index) },
                    onEndCycle = null
                )
            }
        }

        // Hidden widgets section (only show in customization mode)
        if (isCustomizationMode && hiddenWidgets.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hidden widgets",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hidden Widgets",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        hiddenWidgets.forEach { widget ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = widget.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            dashboardViewModel.toggleWidgetVisibility(widget.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Show widget",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveDashboardContent(
    dashboardState: DashboardState,
    isLoading: Boolean,
    error: String?,
    isCustomizationMode: Boolean,
    dashboardPreferences: DashboardPreferences,
    hiddenWidgets: List<DashboardWidget>,
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController,
    layoutInfo: AdaptiveLayoutInfo,
    showCompleteCycleConfirmation: Boolean,
    pendingCompleteCycleAction: QuickAction?,
    onShowCompleteCycleConfirmation: (Boolean) -> Unit,
    onPendingCompleteCycleAction: (QuickAction?) -> Unit
) {
    val spacing = adaptiveSpacing()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(layoutInfo.contentPadding)
    ) {
        // Header with customization toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showDebugDialog by remember { mutableStateOf(false) }

            Text(
                text = "Dashboard",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.combinedClickable(
                    onClick = { /* Normal click does nothing */ },
                    onLongClick = { showDebugDialog = true }
                )
            )

            OutlinedButton(
                onClick = { dashboardViewModel.toggleCustomizationMode() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isCustomizationMode)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isCustomizationMode) Icons.Default.Done else Icons.Default.Edit,
                    contentDescription = if (isCustomizationMode) "Done" else "Customize",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCustomizationMode) "Done" else "Edit")
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // Persistent next-session CTA — same hoist as the compact path. Sits above the
        // widget grid so "Start next session" is visible without scrolling on tablets too.
        val nextCtaCycle = dashboardState.widgets
            .filterIsInstance<DashboardWidget.CycleProgressWidget>()
            .firstOrNull()?.cycle
            ?.takeIf { cycleProgress(it).nextSession != null }
        if (nextCtaCycle != null) {
            NextSessionCtaCard(cycle = nextCtaCycle, navController = navController)
            Spacer(modifier = Modifier.height(spacing))
        }

        when {
            isLoading && dashboardState.widgets.isEmpty() -> {
                DashboardSkeletonGrid()
            }
            error != null -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { dashboardViewModel.onPullToRefresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            !isLoading && dashboardState.widgets.isEmpty() -> {
                DashboardEmptyState(
                    onGoToLibrary = { navController.navigate(Screen.Library.route) }
                )
            }
            else -> {
                // Content with adaptive layout
                AdaptiveWidgetGrid(
                    widgets = dashboardState.widgets,
                    quickActions = dashboardState.quickActions,
                    insights = dashboardState.insights,
                    isCustomizationMode = isCustomizationMode,
                    dashboardPreferences = dashboardPreferences,
                    hiddenWidgets = hiddenWidgets,
                    dashboardViewModel = dashboardViewModel,
                    navController = navController,
                    layoutInfo = layoutInfo,
                    onShowCompleteCycleConfirmation = onShowCompleteCycleConfirmation,
                    onPendingCompleteCycleAction = onPendingCompleteCycleAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()
    val isLoading by dashboardViewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by dashboardViewModel.error.collectAsStateWithLifecycle()
    val isCustomizationMode by dashboardViewModel.isCustomizationMode.collectAsStateWithLifecycle()
    val dashboardPreferences by dashboardViewModel.dashboardPreferences.collectAsStateWithLifecycle()
    val hiddenWidgets by dashboardViewModel.hiddenWidgets.collectAsStateWithLifecycle()
    val showBodyweightDialog by dashboardViewModel.showBodyweightDialog.collectAsStateWithLifecycle()
    val weightUnit by dashboardViewModel.weightUnit.collectAsStateWithLifecycle()

    // Adaptive layout information
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val spacing = adaptiveSpacing()

    // Setup simple LazyColumn state (non-widget items)
    val lazyListState = rememberLazyListState()

    // Confirmation dialog state
    var showCompleteCycleConfirmation by remember { mutableStateOf(false) }
    var pendingCompleteCycleAction by remember { mutableStateOf<QuickAction?>(null) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { dashboardViewModel.onPullToRefresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (layoutInfo.useTwoColumns) {
            // Use adaptive grid layout for large screens
            AdaptiveDashboardContent(
                dashboardState = dashboardState,
                isLoading = isLoading,
                error = error,
                isCustomizationMode = isCustomizationMode,
                dashboardPreferences = dashboardPreferences,
                hiddenWidgets = hiddenWidgets,
                dashboardViewModel = dashboardViewModel,
                navController = navController,
                layoutInfo = layoutInfo,
                showCompleteCycleConfirmation = showCompleteCycleConfirmation,
                pendingCompleteCycleAction = pendingCompleteCycleAction,
                onShowCompleteCycleConfirmation = { showCompleteCycleConfirmation = it },
                onPendingCompleteCycleAction = { pendingCompleteCycleAction = it }
            )
        } else {
            // Use single column layout for compact screens
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(layoutInfo.contentPadding),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
            // Header with customization toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showDebugDialog by remember { mutableStateOf(false) }

                    Text(
                        text = "Dashboard",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.combinedClickable(
                            onClick = { /* Normal click does nothing */ },
                            onLongClick = { showDebugDialog = true }
                        )
                    )

                    // Debug Dialog
                    if (showDebugDialog) {
                        AlertDialog(
                            onDismissRequest = { showDebugDialog = false },
                            title = { Text("Debug Options") },
                            text = { Text("Reset dismissed insights to show them again for testing?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        dashboardViewModel.resetDismissedInsights()
                                        showDebugDialog = false
                                    }
                                ) {
                                    Text("Reset Insights")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDebugDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    IconButton(
                        onClick = { dashboardViewModel.toggleCustomizationMode() }
                    ) {
                        Icon(
                            imageVector = if (isCustomizationMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = if (isCustomizationMode) "Exit customization" else "Customize dashboard",
                            tint = if (isCustomizationMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Persistent next-session CTA — hoists the "Start next session" action out
            // of SimpleCycleProgressWidgetCard's expanded widget (previously buried 3 taps
            // deep) onto the first scroll line. Rendered only when an active cycle has
            // an unfinished session; source the cycle from the CycleProgressWidget so no
            // new VM plumbing is needed.
            val nextCtaCycle = dashboardState.widgets
                .filterIsInstance<DashboardWidget.CycleProgressWidget>()
                .firstOrNull()?.cycle
                ?.takeIf { cycleProgress(it).nextSession != null }
            if (nextCtaCycle != null) {
                item {
                    NextSessionCtaCard(cycle = nextCtaCycle, navController = navController)
                }
            }

            // Error state
            error?.let { errorMessage ->
                item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { dashboardViewModel.refreshDashboard() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                }
            }

            // Loading state — skeleton cards instead of a bare spinner so the user
            // sees the shape of the dashboard arriving.
            if (isLoading && dashboardState.widgets.isEmpty()) {
                item {
                    DashboardSkeletonGrid()
                }
            }

            // Empty state — fresh install / no widgets configured yet.
            if (!isLoading && dashboardState.widgets.isEmpty() &&
                dashboardState.insights.isEmpty() && dashboardState.quickActions.isEmpty()) {
                item {
                    DashboardEmptyState(
                        onGoToLibrary = { navController.navigate(Screen.Library.route) }
                    )
                }
            }

            // Content (when not loading)
            if (!isLoading) {
                // High priority insights
                val urgentInsights = dashboardState.insights.filter {
                    it.priority == InsightPriority.URGENT || it.priority == InsightPriority.HIGH
                }
                items(urgentInsights, key = { it.id }) { insight ->
                    EnhancedInsightCard(
                        insight = insight,
                        onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                        onAction = { insight ->
                            dashboardViewModel.executeInsightAction(insight) { route ->
                                navController.navigate(route)
                            }
                        }
                    )
                }

                // Quick actions
                if (dashboardState.quickActions.isNotEmpty()) {
                    item {
                        EnhancedDashboardWidgetCard(
                            title = "Quick Actions",
                            icon = Icons.Default.FlashOn
                        ) {
                            Column {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    items(dashboardState.quickActions, key = { it.id }) { action ->
                                        EnhancedQuickActionButton(
                                            action = action,
                                            onClick = { selectedAction ->
                                                if (selectedAction.action == QuickActionType.COMPLETE_CYCLE) {
                                                    // Show confirmation dialog for cycle completion
                                                    pendingCompleteCycleAction = selectedAction
                                                    showCompleteCycleConfirmation = true
                                                } else {
                                                    // Execute other actions directly
                                                    dashboardViewModel.executeQuickAction(selectedAction) { route ->
                                                        navController.navigate(route)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                // Scroll indicator when there are more than 3 actions
                                if (dashboardState.quickActions.size > 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(3) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        CircleShape
                                                    )
                                            )
                                            if (index < 2) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Swipe for more actions",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dashboard widgets with arrow button reordering
                itemsIndexed(
                    items = dashboardState.widgets,
                    key = { _, widget -> "widget_${widget.id}" }
                ) { index, widget ->
                    // Get current visibility state from preferences
                    val widgetConfig = dashboardPreferences.widgetConfigs.find { it.widgetType == widget.id }
                    val isWidgetVisible = widgetConfig?.isEnabled ?: widget.isVisible

                    ArrowReorderWidgetCard(
                        widget = widget,
                        navController = navController,
                        isCustomizationMode = isCustomizationMode,
                        isWidgetVisible = isWidgetVisible,
                        canMoveUp = index > 0,
                        canMoveDown = index < dashboardState.widgets.size - 1,
                        onToggleVisibility = { widgetId: String ->
                            dashboardViewModel.toggleWidgetVisibility(widgetId)
                        },
                        onMoveUp = { dashboardViewModel.moveWidgetUp(index) },
                        onMoveDown = { dashboardViewModel.moveWidgetDown(index) },
                        onEndCycle = {
                            dashboardViewModel.executeQuickAction(
                                QuickAction(
                                    id = "end_cycle",
                                    title = "End Cycle",
                                    description = "End current cycle",
                                    icon = Icons.Default.Close,
                                    action = QuickActionType.COMPLETE_CYCLE
                                )
                            ) { route -> navController.navigate(route) }
                        }
                    )
                }

                // Hidden widgets section (only show in customization mode)
                if (isCustomizationMode && hiddenWidgets.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hidden widgets",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Hidden Widgets",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            hiddenWidgets.forEach { widget ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = widget.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                dashboardViewModel.toggleWidgetVisibility(widget.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Show widget",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }

                // Low priority insights
                val lowPriorityInsights = dashboardState.insights.filter {
                    it.priority == InsightPriority.LOW || it.priority == InsightPriority.MEDIUM
                }
                if (lowPriorityInsights.isNotEmpty()) {
                    item {
                        EnhancedDashboardWidgetCard(
                        title = "Insights",
                        icon = Icons.Default.Lightbulb
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lowPriorityInsights.forEach { insight ->
                                EnhancedInsightCard(
                                    insight = insight,
                                    onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                                    onAction = { insight ->
                                        dashboardViewModel.executeInsightAction(insight) { route ->
                                            navController.navigate(route)
                                        }
                                    }
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
        }
    }

    // Complete Cycle Confirmation Dialog
    if (showCompleteCycleConfirmation && pendingCompleteCycleAction != null) {
        AlertDialog(
            onDismissRequest = {
                showCompleteCycleConfirmation = false
                pendingCompleteCycleAction = null
            },
            title = {
                Text(
                    text = "Complete Cycle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to complete the current cycle? This will end your current program and you can start a new one.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteCycleConfirmation = false
                        pendingCompleteCycleAction?.let { action ->
                            dashboardViewModel.executeQuickAction(action) { route ->
                                navController.navigate(route)
                            }
                        }
                        pendingCompleteCycleAction = null
                    }
                ) {
                    Text("Complete Cycle")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCompleteCycleConfirmation = false
                        pendingCompleteCycleAction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bodyweight Entry Dialog
    if (showBodyweightDialog) {
        BodyweightEntryDialog(
            onDismiss = { dashboardViewModel.hideBodyweightDialog() },
            onSave = { weight, date, notes ->
                dashboardViewModel.saveBodyweightEntry(weight, date, notes)
            },
            weightUnit = weightUnit
        )
    }
}
