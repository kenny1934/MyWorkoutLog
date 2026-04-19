package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.ui.theme.Dimens
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Dashboard loading state: three shimmer-style placeholder cards so the user sees the shape
 * of the dashboard arriving instead of a single spinner on a blank screen.
 */
@Composable
fun DashboardSkeletonGrid(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "dashboard-skeleton")
    val alpha by shimmer.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacing16)
    ) {
        repeat(3) {
            SkeletonWidgetCard(alpha = alpha)
        }
    }
}

@Composable
private fun SkeletonWidgetCard(alpha: Float) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spacing16),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
        ) {
            // Title bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.5f)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(baseColor.copy(alpha = alpha))
            )
            // Two content lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(baseColor.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.7f)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(baseColor.copy(alpha = alpha))
            )
        }
    }
}

/**
 * Empty state shown when the dashboard has no widgets and isn't loading — i.e. a fresh install.
 * Points the user at the Library so they can set up exercises / templates / programs.
 */
@Composable
fun DashboardEmptyState(
    onGoToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.spacing32, vertical = Dimens.spacing48),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconXLarge),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Dimens.spacing16))
        Text(
            text = "Nothing to show yet",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.spacing8))
        Text(
            text = "Head to the Library to add exercises, build a template, and start your first workout.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.spacing24))
        Button(onClick = onGoToLibrary) {
            Text("Go to Library")
        }
    }
}
