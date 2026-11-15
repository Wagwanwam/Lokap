package vn.nhom2.lokap.data.local

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cache có hạn (TTL) cho các lần tải lại từ server khi mở màn. */
@OptIn(ExperimentalCoroutinesApi::class)
class RefreshThrottleTest {

    private var now = 1_000L
    private val throttle = RefreshThrottle(ttlMillis = 30_000L, clock = { now })
    private var calls = 0

    private fun load(): Result<Unit> {
        calls++
        return Result.success(Unit)
    }

    @Test
    fun `lan dau mo man thi tai`() = runTest {
        throttle.run("me") { load() }
        assertEquals(1, calls)
    }

    @Test
    fun `mo lai trong 30 giay thi bo qua va coi nhu thanh cong`() = runTest {
        throttle.run("me") { load() }
        now += 29_999
        val result = throttle.run("me") { load() }
        assertEquals(1, calls)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `het 30 giay thi tai lai`() = runTest {
        throttle.run("me") { load() }
        now += 30_000
        throttle.run("me") { load() }
        assertEquals(2, calls)
    }

    @Test
    fun `keo de lam moi hoac Thu lai (force) luon tai`() = runTest {
        throttle.run("me") { load() }
        throttle.run("me", force = true) { load() }
        assertEquals(2, calls)
    }

    @Test
    fun `tai khoan khac thi tai`() = runTest {
        throttle.run("me") { load() }
        throttle.run("other") { load() }
        assertEquals(2, calls)
    }

    @Test
    fun `tai loi thi lan sau van tai`() = runTest {
        throttle.run("me") {
            calls++
            Result.failure(IllegalStateException("Mất kết nối mạng"))
        }
        throttle.run("me") { load() }
        assertEquals(2, calls)
    }

    @Test
    fun `dang xuat xoa du lieu may thi lan sau tai lai`() = runTest {
        throttle.run("me") { load() }
        throttle.reset()
        throttle.run("me") { load() }
        assertEquals(2, calls)
    }

    @Test
    fun `dang xuat dung luc dang tai thi lan sau van tai lai`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val first = async { throttle.run("me") { gate.await(); load() } }
        testScheduler.advanceUntilIdle()
        throttle.reset() // xoá dữ liệu máy khi lần tải cũ còn chạy
        gate.complete(Unit)
        first.await()
        throttle.run("me") { load() }
        assertEquals(2, calls)
    }

    @Test
    fun `hai man mo cung luc chi tai mot lan`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val first = async { throttle.run("me") { gate.await(); load() } }
        val second = async { throttle.run("me") { load() } }
        testScheduler.advanceUntilIdle() // lần 2 đang chờ lần 1
        gate.complete(Unit)
        first.await()
        second.await()
        assertEquals(1, calls)
    }
}
