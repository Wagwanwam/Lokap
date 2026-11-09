package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(LokapTheme.shapes.pill)
                .background(colors.surfaceHigh)
                .border(1.dp, colors.outline, LokapTheme.shapes.pill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colors.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onActionClick != null) {
            PinkButton(
                text = actionLabel,
                onClick = onActionClick
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Thử lại"
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.outline, LokapTheme.shapes.card)
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = colors.error,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        PinkButton(
            text = retryLabel,
            leadingIcon = Icons.Rounded.Refresh,
            onClick = onRetry
        )
    }
}

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    message: String = "Không có kết nối mạng · Ảnh sẽ tự động gửi khi có mạng lại"
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.pill)
            .background(colors.surfaceHigh)
            .border(1.dp, colors.accentYellow, LokapTheme.shapes.pill)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = colors.accentYellow,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = LokapTheme.shapes.card
) {
    val colors = LokapTheme.colors
    val transition = rememberInfiniteTransition(label = "lokap_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            colors.surface,
            colors.surfaceHigh,
            colors.surface
        ),
        start = Offset(progress - 300f, progress - 300f),
        end = Offset(progress, progress)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun StateFeedbackPreview() {
    LokapTheme {
        Column(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            OfflineBanner()
            EmptyState(
                title = "Chưa có khoảnh khắc nào",
                subtitle = "Hãy chụp tấm ảnh đầu tiên cùng hội bạn thân ở Đà Nẵng"
            )
            ErrorState(
                message = "Không thể khởi động máy ảnh. Vui lòng đóng các ứng dụng khác đang dùng camera.",
                onRetry = {}
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
        }
    }
}
