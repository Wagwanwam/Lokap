package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
@Composable
fun BlockUserDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceHigh,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = "Chặn $displayName?",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "Họ sẽ không xem hay nhắn được cho bạn.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Chặn",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Huỷ",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
            }
        }
    )
}
