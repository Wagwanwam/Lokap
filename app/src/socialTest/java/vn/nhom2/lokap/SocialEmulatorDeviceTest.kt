package vn.nhom2.lokap

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.NotificationType
import vn.nhom2.lokap.feature.notifications.NotificationsUiState

/**
 * Opt-in integration on the physical device, using real repositories and disposable backend fixtures.
 * Run with -e socialEmulator true, Auth 19099/Firestore 18080 forwarded through USB.
 * Default FirebaseApp (the person's signed-in account) is never signed out or used for writes.
 */
@RunWith(AndroidJUnit4::class)
class SocialEmulatorDeviceTest {
    @Test
    fun duplicateInvitesNotificationsAndMessagesAcrossTwoVerifiedSessions() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("socialEmulator") == "true")
        check(BuildConfig.BUILD_TYPE == "qa") { "Run only in the loopback-enabled QA build" }
        val defaultAuth = FirebaseAuth.getInstance()
        val defaultUidBefore = defaultAuth.currentUser?.uid
        val context = instrumentation.targetContext
        val checks = JSONArray()
        fun passed(name: String, details: String) {
            checks.put(JSONObject().put("check", name).put("details", details).put("status", "PASS"))
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "\nSOCIAL_QA_PASS: $name — $details\n") })
        }
        val a = withTimeout(30_000) { SocialEmulatorFixture.open(context, first = true) }
        var b: SocialEmulatorFixture? = null
        try {
            val receiver = withTimeout(30_000) { SocialEmulatorFixture.open(context, first = false) }
            b = receiver
            val uidA = a.user.id
            val uidB = receiver.user.id
            val pairId = listOf(uidA, uidB).sorted().joinToString("_")
            val friendship = a.db.collection("friendships").document(pairId)
            val noticesA = a.db.collection("users").document(uidA).collection("notifications")
            val noticesB = receiver.db.collection("users").document(uidB).collection("notifications")
            assertFalse("Reset the isolated emulator fixtures before this scenario", server(friendship).exists())
            assertEquals(0, server(noticesA).size)
            assertEquals(0, server(noticesB).size)

            val aSecondCaller = a.anotherSocialRepository()
            val repeated = coroutineScope {
                List(8) { index -> async {
                    (if (index % 2 == 0) a.social else aSecondCaller).sendFriendRequest(uidB)
                } }.awaitAll()
            }
            assertTrue("At least one request must reach Firestore", repeated.any { it.isSuccess })
            repeated.filter { it.isSuccess }.forEach { assertEquals(FriendStatus.REQUEST_SENT, it.getOrThrow()) }
            repeat(3) { assertEquals(FriendStatus.REQUEST_SENT, a.social.sendFriendRequest(uidB).getOrThrow()) }
            val pending = server(friendship)
            assertEquals("pending", pending.getString("status"))
            assertEquals(uidA, pending.getString("requesterId"))
            assertEquals(1, server(a.db.collection("friendships").whereArrayContains("users", uidA)).size)
            assertEquals(1, server(noticesB).count { it.getString("type") == "FRIEND_REQUEST" })
            val firstNotice = receiver.content.observeNotifications().awaitValue { list ->
                list.count { it.type == NotificationType.FRIEND_REQUEST && it.actor?.id == uidA } == 1
            }.single()
            assertEquals(1, withTimeout(15_000) { receiver.content.observeUnreadCount().first { it == 1 } })
            passed("repeated_friend_request", "8 parallel calls through 2 repository instances plus 3 retries produced 1 friendship and 1 incoming notification")

            a.social.cancelFriendRequest(uidB).getOrThrow()
            assertFalse(server(friendship).exists())
            assertTrue("A withdrawn invitation cannot be accepted", receiver.social.acceptFriendRequest(uidA).isFailure)
            assertEquals(0, server(noticesA).count { it.getString("type") == "FRIEND_ACCEPTED" })
            assertEquals(FriendStatus.REQUEST_SENT, a.social.sendFriendRequest(uidB).getOrThrow())
            val requestHistory = receiver.content.observeNotifications().awaitValue { list ->
                list.count { it.type == NotificationType.FRIEND_REQUEST && it.actor?.id == uidA } == 2
            }.filter { it.type == NotificationType.FRIEND_REQUEST }
            assertEquals(2, server(noticesB).count { it.getString("type") == "FRIEND_REQUEST" })
            val newNotice = requestHistory.single { it.id != firstNotice.id }
            val pendingUi = NotificationsUiState(unreadNotifications = requestHistory,
                friendStatuses = mapOf(uidA to FriendStatus.REQUEST_RECEIVED), isGraphChecked = true)
            assertEquals(false, pendingUi.friendRequestDecision(requestHistory.single { it.id == firstNotice.id }))
            assertNull(pendingUi.friendRequestDecision(newNotice))
            passed("cancel_and_resend", "Withdrawal closes the old request; a deliberate new invitation creates one new event and only its latest notification is actionable")

            val bSecondCaller = receiver.anotherSocialRepository()
            val accepts = coroutineScope { List(6) { index -> async {
                (if (index % 2 == 0) receiver.social else bSecondCaller).acceptFriendRequest(uidA)
            } }.awaitAll() }
            assertTrue(accepts.any { it.isSuccess })
            repeat(2) { receiver.social.acceptFriendRequest(uidA).getOrThrow() }
            assertEquals("accepted", server(friendship).getString("status"))
            assertEquals(1, server(noticesA).count { it.getString("type") == "FRIEND_ACCEPTED" })
            assertTrue(a.social.cancelFriendRequest(uidB).isFailure)
            assertTrue(receiver.social.declineFriendRequest(uidA).isFailure)
            assertEquals("accepted", server(friendship).getString("status"))
            assertEquals(FriendStatus.FRIENDS, a.social.sendFriendRequest(uidB).getOrThrow())
            assertEquals(2, server(noticesB).count { it.getString("type") == "FRIEND_REQUEST" })
            passed("accept_and_stale_actions", "Repeated accept creates 1 acceptance notice; stale cancel/decline cannot delete accepted friendship; sending again while friends creates no invitation")

            val conversationId = a.content.openConversation(uidB).getOrThrow()
            assertEquals(pairId, conversationId)
            assertEquals(conversationId, receiver.content.openConversation(uidA).getOrThrow())
            val marker = "SOCIAL-QA-${System.currentTimeMillis()}"
            a.content.send(conversationId, marker).getOrThrow()
            val receiverChat = receiver.content.observeChat(conversationId).awaitValue { chat -> chat.messages.any { it.body == marker } }
            assertEquals(1, receiverChat.messages.count { it.body == marker })
            assertEquals(1, receiverChat.conversation?.unreadCount)
            val inbox = receiver.content.observeInbox().awaitValue { list -> list.any { it.id == conversationId && it.unreadCount == 1 } }
            assertEquals(marker, inbox.single { it.id == conversationId }.lastMessagePreview)
            val message = receiverChat.messages.single { it.body == marker }
            val messageRef = a.db.collection("conversations").document(conversationId).collection("messages").document(message.id)
            val messageNoticeId = "message_${message.id}"
            val messageNotice = noticesB.document(messageNoticeId)
            assertEquals(marker, server(messageRef).getString("body"))
            val incoming = receiver.content.observeNotifications().awaitValue { list -> list.any { it.id == messageNoticeId } }
            assertEquals(NotificationType.NEW_MESSAGE, incoming.single { it.id == messageNoticeId }.type)
            assertFalse(server(messageNotice).getBoolean("isRead")!!)
            passed("message_delivery", "Receiver chat, inbox unread count and NEW_MESSAGE notification all observe exactly one server-backed message")

            receiver.content.markRead(conversationId).getOrThrow()
            val senderChat = a.content.observeChat(conversationId).awaitValue { chat -> chat.messages.any { it.id == message.id && it.seen } }
            assertTrue(senderChat.messages.single { it.id == message.id }.seen)
            receiver.content.observeInbox().awaitValue { list -> list.singleOrNull { it.id == conversationId }?.unreadCount == 0 }
            receiver.content.markNotificationRead(messageNoticeId, true).getOrThrow()
            assertEquals(true, server(messageNotice).getBoolean("isRead"))
            receiver.content.observeNotifications().awaitValue { list -> list.any { it.id == messageNoticeId && it.isRead } }
            receiver.content.markNotificationRead(messageNoticeId, false).getOrThrow()
            assertEquals(false, server(messageNotice).getBoolean("isRead"))
            receiver.content.observeNotifications().awaitValue { list -> list.any { it.id == messageNoticeId && !it.isRead } }
            passed("read_receipts_and_notification_state", "Receiver readAt clears inbox unread and sender sees receipt; notification read/unread changes persist on the server")

            val edited = "$marker edited"
            a.content.edit(conversationId, message.id, edited).getOrThrow()
            assertEquals(edited, server(messageRef).getString("body"))
            receiver.content.observeChat(conversationId).awaitValue { chat -> chat.messages.any { it.id == message.id && it.body == edited } }
            a.content.delete(conversationId, message.id).getOrThrow()
            assertFalse(server(messageRef).exists())
            receiver.content.observeChat(conversationId).awaitValue { chat -> chat.messages.none { it.id == message.id } }
            receiver.content.deleteNotification(messageNoticeId).getOrThrow()
            assertFalse(server(messageNotice).exists())
            receiver.content.observeNotifications().awaitValue { list -> list.none { it.id == messageNoticeId } }
            passed("message_edit_delete", "Edited body reaches receiver; sender delete removes message; receiver notification delete removes only that event")

            assertEquals(defaultUidBefore, defaultAuth.currentUser?.uid)
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "\nSOCIAL_QA_RESULT=" + JSONObject()
                .put("backend", SocialEmulatorFixture.PROJECT).put("checks", checks)
                .put("default_session_preserved", true).toString() + "\n") })
        } finally {
            b?.close()
            a.close()
            assertEquals("Default account must remain signed in unchanged", defaultUidBefore, defaultAuth.currentUser?.uid)
        }
    }

    private suspend fun server(ref: DocumentReference): DocumentSnapshot = withTimeout(20_000) {
        ref.get(Source.SERVER).await().also {
            assertFalse(it.metadata.isFromCache)
            assertFalse(it.metadata.hasPendingWrites())
        }
    }

    private suspend fun server(query: Query): List<DocumentSnapshot> = withTimeout(20_000) {
        query.get(Source.SERVER).await().also {
            assertFalse(it.metadata.isFromCache)
            assertFalse(it.metadata.hasPendingWrites())
        }.documents
    }

    private suspend fun <T> Flow<Result<T>>.awaitValue(predicate: (T) -> Boolean): T =
        withTimeout(20_000) { map { it.getOrThrow() }.first(predicate) }
}
