package vn.nhom2.lokap.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/** Chờ lâu nhất giữa 2 lần nghe lại. */
private const val MAX_RETRY_DELAY_MS = 60_000L

/**
 * Listener Firestore bị lỗi (index vừa deploy còn đang build, rules chưa cập nhật…) thì phát [fallback] để màn
 * không đứng chờ, rồi nghe lại sau 2, 4, 8… giây (tối đa 60 giây). Không có bước này thì listener dừng hẳn và
 * màn giữ dữ liệu rỗng tới khi mở lại app, dù server đã sẵn sàng.
 */
internal fun <T> Flow<T>.retryListen(fallback: T, firstDelayMs: Long = 2_000L): Flow<T> =
    retryWhen { cause, attempt ->
        if (cause is CancellationException) return@retryWhen false
        emit(fallback)
        delay((firstDelayMs shl attempt.coerceAtMost(5L).toInt()).coerceAtMost(MAX_RETRY_DELAY_MS))
        true
    }
