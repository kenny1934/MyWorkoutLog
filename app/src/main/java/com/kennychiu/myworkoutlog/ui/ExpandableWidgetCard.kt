package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SimpleExpandableWidgetCard(
    title: String,
    isExpandable: Boolean = true,
    modifier: Modifier = Modifier,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isCompactMode = isCompactWidgetMode()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(adaptiveContentPadding())
        ) {
            // Header with optional expand/collapse functionality
            if (isExpandable && expandedContent != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = adaptiveTextSize(
                            baseSize = MaterialTheme.typography.titleMedium.fontSize,
                            compactMultiplier = 0.8f,
                            mediumMultiplier = 0.9f,
                            expandedMultiplier = 1f
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (isCompactMode) 2 else 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    // Animated expand/collapse icon
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300),
                        label = "expand_icon_rotation"
                    )
                    
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(if (isCompactMode) 20.dp else 24.dp)
                            .rotate(rotationAngle)
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isCompactMode) 8.dp else 12.dp))
            } else {
                // Static title for non-expandable widgets
                Text(
                    text = title,
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.titleMedium.fontSize,
                        compactMultiplier = 0.8f,
                        mediumMultiplier = 0.9f,
                        expandedMultiplier = 1f
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (isCompactMode) 2 else 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(if (isCompactMode) 8.dp else 12.dp))
            }
            
            // Collapsed content (always shown)
            collapsedContent()
            
            // Expanded content (shown when expanded)
            if (isExpandable && expandedContent != null) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(300, easing = EaseInOut)
                    ) + fadeIn(
                        animationSpec = tween(300, delayMillis = 150)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(300, easing = EaseInOut)
                    ) + fadeOut(
                        animationSpec = tween(150)
                    )
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        expandedContent()
                    }
                }
            }
        }
    }
}