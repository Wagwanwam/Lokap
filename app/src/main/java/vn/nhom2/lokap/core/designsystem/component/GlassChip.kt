package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData
@Composable
fun GlassChip(
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shape = LokapTheme.shapes.pill
    val borderColor = if (selected) colors.primary else colors.outline

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceHigh)
            .border(width = if (selected) 1.5.dp else 1.dp, color = borderColor, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.textPrimary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun GlassChipPreview() {
    LokapTheme {
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            GlassChip(
                label = "Bạn bè · ${SampleData.users.size - 1}",
                leadingIcon = Icons.Rounded.Person,
                selected = false,
                onClick = {}
            )
            GlassChip(
                label = SampleData.landmarks.first().name,
                leadingIcon = Icons.Rounded.LocationOn,
                selected = true,
                onClick = {}
            )
        }
    }
}
