package vn.nhom2.lokap.core.designsystem.component
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
@Composable
fun ContentEditDialog(title: String, text: String, busy: Boolean, onChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = onChange, modifier = Modifier.semantics { contentDescription = "Nội dung chỉnh sửa" }, enabled = !busy) },
        confirmButton = { TextButton(onClick = onSave, enabled = !busy && text.trim().isNotEmpty()) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Huỷ") } })
}
@Composable
fun ContentDeleteDialog(title: String, busy: Boolean, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(title) }, text = { Text("Nội dung sẽ bị xoá khỏi cuộc trò chuyện hoặc bài viết.") },
        confirmButton = { TextButton(onClick = onDelete, enabled = !busy) { Text("Xoá") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Huỷ") } })
}
