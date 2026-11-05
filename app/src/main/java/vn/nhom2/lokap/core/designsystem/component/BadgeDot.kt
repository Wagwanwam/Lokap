package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData

@Composable
fun BadgeDot(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    val colors = LokapTheme.colors
    val shape = LokapTheme.shapes.pill
    val displayText = if (count > 99) "99+" else count.toString()

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(shape)
            .background(colors.primary)
            .border(1.5.dp, colors.background, shape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onPrimary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun BadgeDotPreview() {
    LokapTheme {
        val unreadTotal = SampleData.conversations.sumOf { it.unreadCount }
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            BadgeDot(count = unreadTotal)
            BadgeDot(count = 12)
            BadgeDot(count = 128)
        }
    }
}
