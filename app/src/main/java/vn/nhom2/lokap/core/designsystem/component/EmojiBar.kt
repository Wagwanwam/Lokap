package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Reaction
private val DefaultEmojiSet = listOf("😍", "😂", "🔥", "😮", "😢")

@Composable
fun EmojiBar(
    reactions: List<Reaction>,
    selectedEmoji: String?,
    onSelectEmoji: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shape = LokapTheme.shapes.pill

    val countsByEmoji = remember(reactions) {
        reactions.associate { it.emoji to it.count }
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DefaultEmojiSet.forEach { emoji ->
            val isSelected = selectedEmoji == emoji
            val count = countsByEmoji[emoji] ?: 0

            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) colors.surfaceHigh else colors.surface)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) colors.primary else colors.surface,
                        shape = shape
                    )
                    .clickable { onSelectEmoji(emoji) }
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) colors.primary else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun EmojiBarPreview() {
    LokapTheme {
        val samplePost = SampleData.posts.first()
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg)
        ) {
            EmojiBar(
                reactions = samplePost.reactions,
                selectedEmoji = samplePost.myReaction,
                onSelectEmoji = {}
            )
        }
    }
}
