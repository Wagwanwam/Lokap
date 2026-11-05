package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

@Composable
fun PinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled && !loading,
        shape = LokapTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = colors.textTertiary
        ),
        contentPadding = PaddingValues(horizontal = spacing.xl, vertical = spacing.md),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    OutlinedButton(
        onClick = { if (!loading) onClick() },
        enabled = enabled && !loading,
        shape = LokapTheme.shapes.pill,
        border = BorderStroke(1.dp, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.background,
            disabledContentColor = colors.textTertiary
        ),
        contentPadding = PaddingValues(horizontal = spacing.xl, vertical = spacing.md),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.primary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun ButtonsPreview() {
    LokapTheme {
        Column(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            PinkButton(text = "Cho phép máy ảnh", onClick = {})
            PinkButton(text = "Đang gửi ảnh...", onClick = {}, loading = true)
            GhostButton(text = "Thử lại", onClick = {})
            GhostButton(text = "Đang tải...", onClick = {}, loading = true)
        }
    }
}
