package vn.nhom2.lokap.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Listener tab "Đăng lại": index đang build thì không được dừng hẳn. */
@OptIn(ExperimentalCoroutinesApi::class)
class RetryListenTest {

    @Test
    fun `listener loi thi phat rong roi nghe lai, server san sang thi co du lieu`() = runTest {
        var attempts = 0
        val listener = flow {
            attempts++
            if (attempts < 3) error("FAILED_PRECONDITION: The query requires an index (đang build)")
            emit(listOf("p_10"))
        }

        val shown = listener.retryListen(fallback = emptyList<String>()).toList()

        assertEquals(listOf(emptyList(), emptyList(), listOf("p_10")), shown)
        assertEquals(3, attempts)
        assertEquals("Chờ 2 s rồi 4 s trước khi nghe lại", 6_000L, currentTime)
    }

    @Test
    fun `cho lau nhat 60 giay giua 2 lan nghe lai`() = runTest {
        var attempts = 0
        val listener = flow {
            attempts++
            if (attempts <= 8) error("PERMISSION_DENIED")
            emit(1)
        }

        listener.retryListen(fallback = 0).toList()

        // 2 + 4 + 8 + 16 + 32 + 60 + 60 + 60 giây
        assertEquals(242_000L, currentTime)
    }
}
