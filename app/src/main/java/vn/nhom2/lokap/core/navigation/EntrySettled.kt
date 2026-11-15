package vn.nhom2.lokap.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.first

/**
 * `true` khi màn hiện tại (NavBackStackEntry) đã RESUMED, tức animation chuyển màn (vào hoặc Back) đã chạy xong.
 *
 * Khi Back, Navigation Compose dựng lại màn trước TỪ ĐẦU trong frame đầu của animation, và đồng hồ animation
 * tính cả thời gian dựng đó: việc nặng trong frame này (tạo MapView, bind CameraX, mở dialog, bật bàn phím…)
 * làm animation đứng hình rồi nhảy cóc. Bọc phần nặng bằng cờ này để nó chạy ngay SAU animation.
 *
 * Cờ chỉ bật một lần (latch): Activity bị pause (hộp thoại xin quyền, chia đôi màn hình) không làm phần nặng bị huỷ.
 * Lúc mở app, màn đầu tiên lên RESUMED sau 1–2 frame nên gần như không trễ.
 */
@Composable
fun rememberEntrySettled(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var settled by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    if (!settled) {
        LaunchedEffect(lifecycle) {
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            settled = true
        }
    }
    return settled
}
