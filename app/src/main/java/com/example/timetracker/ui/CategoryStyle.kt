package com.example.timetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.timetracker.data.ActiveState

/**
 * Icon/color pairing per category, shared by the dashboard's timer cards and the
 * history cards so both screens read as one visual language.
 */
fun categoryIcon(state: ActiveState): ImageVector = when (state) {
    ActiveState.WORK -> Icons.Filled.Work
    ActiveState.SELF -> Icons.Filled.SelfImprovement
    ActiveState.SLEEP -> Icons.Filled.Bedtime
    ActiveState.IDLE -> Icons.Filled.Schedule
}

@Composable
fun categoryColor(state: ActiveState): Color = when (state) {
    ActiveState.WORK -> MaterialTheme.colorScheme.primary
    ActiveState.SELF -> MaterialTheme.colorScheme.secondary
    ActiveState.SLEEP -> MaterialTheme.colorScheme.tertiary
    ActiveState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** A category's icon on a soft tonal circle - the "badge" reused on both screens. */
@Composable
fun CategoryIconBadge(state: ActiveState, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val color = categoryColor(state)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIcon(state),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
