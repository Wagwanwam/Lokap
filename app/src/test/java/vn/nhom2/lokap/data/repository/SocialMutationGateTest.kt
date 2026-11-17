package vn.nhom2.lokap.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.nhom2.lokap.domain.repository.AppException

@OptIn(ExperimentalCoroutinesApi::class)
class SocialMutationGateTest {
    @Test
    fun `same pair cannot queue a stale mutation but another pair can proceed`() = runTest {
        val gate = SocialMutationGate()
        val finish = CompletableDeferred<Unit>()
        val first = launch { gate.run("alice_bob") { finish.await() } }
        runCurrent()

        var staleOperationRan = false
        val repeated = runCatching { gate.run("alice_bob") { staleOperationRan = true } }
        assertTrue(repeated.exceptionOrNull() is AppException)
        assertFalse(staleOperationRan)
        assertEquals("ok", gate.run("alice_carol") { "ok" })

        finish.complete(Unit)
        first.join()
        assertFalse(staleOperationRan)
        assertEquals("resend", gate.run("alice_bob") { "resend" })
    }

    @Test
    fun `failure releases pair for explicit retry`() = runTest {
        val gate = SocialMutationGate()
        val failed = runCatching { gate.run("alice_bob") { error("offline") } }
        assertEquals("offline", failed.exceptionOrNull()?.message)
        assertEquals("retry", gate.run("alice_bob") { "retry" })
    }

    @Test
    fun `cancellation releases pair`() = runTest {
        val gate = SocialMutationGate()
        val job = launch { gate.run("alice_bob") { CompletableDeferred<Unit>().await() } }
        runCurrent()
        job.cancelAndJoin()
        assertEquals("retry", gate.run("alice_bob") { "retry" })
    }
}
