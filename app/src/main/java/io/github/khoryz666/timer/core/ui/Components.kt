package io.github.khoryz666.timer.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.khoryz666.timer.quota.QuotaStatus
import io.github.khoryz666.timer.tracking.Pillar

@Composable
@ReadOnlyComposable
fun pillarColor(pillar: Pillar): Color = when (pillar) {
    Pillar.ENGINE -> MaterialTheme.colorScheme.primary
    Pillar.JOY -> MaterialTheme.colorScheme.secondary
}

/** A rounded card; [accent] draws a coloured outline (used for whatever is running). */
@Composable
fun SectionCard(modifier: Modifier = Modifier, accent: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(if (accent != null) 2.dp else 1.dp, accent ?: MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun StatusChip(status: QuotaStatus, hasFloor: Boolean) {
    val (label, container, content) = when (status) {
        QuotaStatus.TARGET_MET -> Triple(
            "Target met",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        QuotaStatus.FLOOR_MET -> Triple(
            "Floor met",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        QuotaStatus.BELOW_FLOOR -> Triple(
            if (hasFloor) "Below floor" else "Below target",
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** A thin rounded progress bar with an optional tick mark at [marker] (0..1). */
@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, marker: Float? = null) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        if (marker != null && marker in 0f..1f) {
            Row(Modifier.fillMaxWidth(marker).fillMaxHeight(), horizontalArrangement = Arrangement.End) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface))
            }
        }
    }
}

/**
 * One segment per target block: full segments are done, the next one fills partially, and
 * the segments up to the floor sit on a slightly darker track.
 */
@Composable
fun BlockSegments(blocks: Int, partial: Float, target: Int, floor: Int, color: Color, modifier: Modifier = Modifier) {
    val segments = maxOf(target, blocks + if (partial > 0f) 1 else 0)
    Row(modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(segments) { index ->
            val fill = when {
                index < blocks -> 1f
                index == blocks -> partial
                else -> 0f
            }
            val track = if (index < floor) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.surfaceContainerHigh
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(track),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(color))
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = SmallCaps,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
