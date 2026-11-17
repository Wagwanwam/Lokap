package vn.nhom2.lokap

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies only the explicitly named UI test message. No writes or account switching. */
@RunWith(AndroidJUnit4::class)
class AuthorizedPeerReadOnlyTest {
    @Test fun verifyAuthorizedMessageOnServer() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("livePeerReadOnly") == "true")
        val peerName = requireNotNull(args.getString("peerUsername"))
        val body = requireNotNull(args.getString("expectedBody"))
        require(body.startsWith("QA_LOKAP_"))
        val auth = FirebaseAuth.getInstance()
        val uid = requireNotNull(auth.currentUser).uid
        val db = FirebaseFirestore.getInstance()
        assertTrue(BuildConfig.FIREBASE_EMULATOR_HOST.isBlank())
        withTimeout(30_000) {
            val me = db.collection("users").document(uid).get(Source.SERVER).await()
            assertEquals("thuan", me.getString("username"))
            val peer = db.collection("usernames").document(peerName).get(Source.SERVER).await().getString("uid")!!
            assertEquals(peerName, db.collection("users").document(peer).get(Source.SERVER).await().getString("username"))
            val pair = listOf(uid, peer).sorted().joinToString("_")
            val friendship = db.collection("friendships").document(pair).get(Source.SERVER).await()
            assertEquals("accepted", friendship.getString("status"))
            val conversation = db.collection("conversations").document(pair)
            val messages = conversation.collection("messages").get(Source.SERVER).await()
            assertFalse(messages.metadata.isFromCache)
            assertFalse(messages.metadata.hasPendingWrites())
            val matching = messages.documents.filter { it.getString("body") == body && it.getString("senderId") == uid }
            assertEquals("Only one copy of the UI test message may exist", 1, matching.size)
            val sent = matching.single()
            val conversationSnapshot = conversation.get(Source.SERVER).await()
            assertEquals(listOf(uid, peer).sorted(), (conversationSnapshot.get("members") as List<*>).filterIsInstance<String>().sorted())
            val readAt = conversationSnapshot.get("readAt") as? Map<*, *>
            val peerRead = readAt?.get(peer) as? com.google.firebase.Timestamp
            val sentAt = requireNotNull(sent.getTimestamp("createdAt"))
            val incomingAfterTest = messages.documents.filter {
                it.getString("senderId") == peer && (it.getTimestamp("createdAt")?.compareTo(sentAt) ?: -1) > 0
            }
            val incomingIds = incomingAfterTest.map { it.id }.toSet()
            val incomingNotifications = db.collection("users").document(uid).collection("notifications")
                .get(Source.SERVER).await().documents.filter {
                    it.getString("type") == "NEW_MESSAGE" && it.getString("actorId") == peer &&
                        it.getString("sourceId") in incomingIds
                }
            val report = JSONObject().apply {
                put("peer_username", peerName)
                put("message_id", sent.id)
                put("matching_message_count", matching.size)
                put("server_confirmed", true)
                put("friendship_preserved", true)
                put("peer_read_receipt", peerRead != null && peerRead >= sentAt)
                put("members_match_authorized_pair", true)
                put("incoming_after_test_count", incomingAfterTest.size)
                put("incoming_notification_count", incomingNotifications.size)
                put("incoming_notifications_read_count", incomingNotifications.count { it.getBoolean("isRead") == true })
                put("expected_reply_received", messages.documents.any {
                    it.getString("senderId") == peer && it.getString("body") == "TEST_OK_01" &&
                        (it.getTimestamp("createdAt")?.compareTo(sentAt) ?: -1) > 0
                })
                put("default_account_preserved", auth.currentUser?.uid == uid)
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
                putString("stream", "\nLIVE_PEER_READ_ONLY=$report\n")
            })
        }
    }
}
