package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.domain.model.AppNotification
object FakeNotificationStore {

    private val _notifications = MutableStateFlow(SampleData.notifications.sortedByDescending { it.createdAt })
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    val unreadCount: Flow<Int> = notifications.map { list -> list.count { !it.isRead } }

    fun markRead(notificationId: String) {
        _notifications.update { list ->
            list.map { if (it.id == notificationId && !it.isRead) it.copy(isRead = true) else it }
        }
    }

    fun markAllRead() {
        _notifications.update { list -> list.map { if (it.isRead) it else it.copy(isRead = true) } }
    }

    internal fun addForTest(notification: AppNotification) {
        _notifications.update { listOf(notification) + it }
    }

    internal fun resetForTest() {
        _notifications.value = SampleData.notifications.sortedByDescending { it.createdAt }
    }
}
