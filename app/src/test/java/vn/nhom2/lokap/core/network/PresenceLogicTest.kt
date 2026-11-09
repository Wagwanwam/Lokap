package vn.nhom2.lokap.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.presence.PresenceStore
import vn.nhom2.lokap.domain.model.PresenceStatus
import vn.nhom2.lokap.domain.model.UserPresence
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class PresenceLogicTest {

    @Before
    fun setUp() {
        PresenceStore.reset()
    }

    @Test
    fun `user presence online shows active status`() {
        val now = Instant.now()
        val presence = UserPresence(
            userId = "user_1",
            status = PresenceStatus.ONLINE,
            lastSeenAt = now
        )
        assertTrue(presence.isOnline)
        assertEquals("Đang hoạt động", presence.formatStatus(now))
    }

    @Test
    fun `user presence recently active shows minutes ago`() {
        val now = Instant.now()
        val fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES)
        val presence = UserPresence(
            userId = "user_2",
            status = PresenceStatus.RECENTLY_ACTIVE,
            lastSeenAt = fiveMinutesAgo
        )
        assertFalse(presence.isOnline)
        assertEquals("Hoạt động 5 phút trước", presence.formatStatus(now))
    }

    @Test
    fun `user presence offline shows offline text`() {
        val now = Instant.now()
        val twoDaysAgo = now.minus(2, ChronoUnit.DAYS)
        val presence = UserPresence(
            userId = "user_3",
            status = PresenceStatus.OFFLINE,
            lastSeenAt = twoDaysAgo
        )
        assertFalse(presence.isOnline)
        assertEquals("Ngoại tuyến", presence.formatStatus(now))
    }

    @Test
    fun `presence store tracks user online and offline updates`() = runTest {
        PresenceStore.setUserOnline("u123", true)
        assertEquals(PresenceStatus.ONLINE, PresenceStore.getPresence("u123").value.status)

        PresenceStore.setUserOnline("u123", false)
        assertEquals(PresenceStatus.RECENTLY_ACTIVE, PresenceStore.getPresence("u123").value.status)
    }

    @Test
    fun `presence store tracks network status`() {
        PresenceStore.setNetworkOnline(true)
        assertTrue(PresenceStore.isNetworkAvailable.value)

        PresenceStore.setNetworkOnline(false)
        assertFalse(PresenceStore.isNetworkAvailable.value)
    }
}
