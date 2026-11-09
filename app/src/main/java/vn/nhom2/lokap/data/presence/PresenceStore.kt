package vn.nhom2.lokap.data.presence

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.nhom2.lokap.domain.model.*
object PresenceStore {

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val userPresenceMap = mutableMapOf<String, MutableStateFlow<UserPresence>>()

    init {
        seedDefaultPresences()
    }

    fun setNetworkOnline(online: Boolean) {
        _isNetworkAvailable.value = online
    }

    fun getPresence(userId: String): StateFlow<UserPresence> {
        return userPresenceMap.getOrPut(userId) {
            MutableStateFlow(
                UserPresence(
                    userId = userId,
                    status = PresenceStatus.OFFLINE,
                    lastSeenAt = Instant.now().minus(1, ChronoUnit.HOURS)
                )
            )
        }.asStateFlow()
    }

    fun setUserOnline(userId: String, isOnline: Boolean) {
        val flow = userPresenceMap.getOrPut(userId) {
            MutableStateFlow(UserPresence(userId = userId))
        }
        flow.value = UserPresence(
            userId = userId,
            status = if (isOnline) PresenceStatus.ONLINE else PresenceStatus.RECENTLY_ACTIVE,
            lastSeenAt = Instant.now()
        )
    }

    fun reset() {
        _isNetworkAvailable.value = true
        userPresenceMap.clear()
        seedDefaultPresences()
    }

    private fun seedDefaultPresences() {
        val now = Instant.now()

        val sampleOnline = listOf("user_1", "user_khanhlinh", "user_hoangnam", "user_thaomy")
        val sampleRecent = listOf("user_2", "user_ducanh", "user_baongoc")

        sampleOnline.forEach { id ->
            userPresenceMap[id] = MutableStateFlow(
                UserPresence(userId = id, status = PresenceStatus.ONLINE, lastSeenAt = now)
            )
        }

        sampleRecent.forEach { id ->
            userPresenceMap[id] = MutableStateFlow(
                UserPresence(
                    userId = id,
                    status = PresenceStatus.RECENTLY_ACTIVE,
                    lastSeenAt = now.minus(12, ChronoUnit.MINUTES)
                )
            )
        }
    }
}
