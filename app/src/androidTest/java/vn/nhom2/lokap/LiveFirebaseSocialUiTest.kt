package vn.nhom2.lokap

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.firestore.*
import dagger.hilt.EntryPoints
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/** All message/comment/notification mutations use visible UI. Server reads prove persistence.
 * Account sign-in in the outer rule is fixture setup, before MainActivity starts. */
@RunWith(AndroidJUnit4::class)
class LiveFirebaseSocialUiTest {
    private val compose = createAndroidComposeRule<MainActivity>(effectContext = StandardTestDispatcher())
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private val args get() = InstrumentationRegistry.getArguments()
    private val graph get() = EntryPoints.get(ins.targetContext.applicationContext, LiveFirebaseTestEntryPoint::class.java)
    private val db get() = graph.firestore()
    private val uid get() = requireNotNull(graph.firebaseAuth().currentUser).uid
    private val marker get() = requireNotNull(args.getString("socialMarker")).also { require(it.matches(Regex("SOCIAL-[a-f0-9]{8}"))) }
    private val primary = "qu0MxHsUAfhZYFO0WZzSxuqJcHh1"
    private val peer = "r2fmm6RoghggbcsCKYTQpnREyP83"
    private val conversation get() = listOf(primary, peer).sorted().joinToString("_")
    private val sampleId = "0b963612-54c8-43ec-8f53-c36d82b8719c"
    private val sampleCaption = "Bai mau Firebase - giu lai UI-6e7e5dae"
    private val setup = TestRule { base, _ -> object : Statement() {
        override fun evaluate() {
            Assume.assumeTrue(args.getString("liveSocialUi") == "true")
            assertTrue(BuildConfig.FIREBASE_EMULATOR_HOST.isBlank())
            assertEquals("lokap-1933c", BuildConfig.FIREBASE_PROJECT_ID)
            runBlocking { withTimeout(30_000) {
                graph.authRepository().logout()
                graph.authRepository().login(requireNotNull(args.getString("testEmail")), requireNotNull(args.getString("testPassword"))).getOrThrow()
            } }
            assertTrue(graph.firebaseAuth().currentUser!!.isEmailVerified)
            try { base.evaluate() } catch(e: Throwable) { screenshot("failure"); throw e }
        }
    } }
    @get:Rule val rules = RuleChain.outerRule(setup).around(compose)

    @Test fun messageCreateEditDelete() {
        assertEquals(primary, uid)
        clickDescription("Hộp thư")
        clickText("Soạn tin")
        clickText("Nhắn tin")
        waitDescription("Chụp ảnh gửi riêng")
        compose.onNode(hasSetTextAction()).performTextReplacement(marker)
        clickDescription("Gửi")
        val ref = awaitOne(db.collection("conversations").document(conversation).collection("messages")) { it.getString("body") == marker }.reference
        waitText(marker); screenshot("message-created")
        longPressText(marker); clickText("Sửa")
        compose.onNodeWithContentDescription("Nội dung chỉnh sửa").performTextReplacement("$marker edited")
        clickText("Lưu")
        awaitDoc(ref) { it.getString("body") == "$marker edited" }; waitText("$marker edited"); screenshot("message-edited")
        compose.activityRule.scenario.recreate()
        // Reopen after recreation and prove the server-backed inbox still has the edited message.
        waitText("$marker edited")
        log("MESSAGE_CREATE_EDIT_OK ${ref.path}; kept for peer receipt verification")
    }

    @Test fun messageDeleteAfterPeerRead() {
        assertEquals(primary, uid)
        val ref = awaitOne(db.collection("conversations").document(conversation).collection("messages")) { it.getString("body") == "$marker edited" }.reference
        val receipt = read(db.collection("conversations").document(conversation)).get("readAt") as? Map<*, *>
        assertNotNull("Peer must have actually opened and read the message", receipt?.get(peer))
        clickDescription("Hộp thư"); clickText("Lokap Test Peer"); waitDescription("Chụp ảnh gửi riêng"); waitText("$marker edited")
        longPressText("$marker edited"); clickText("Xoá")
        // Menu đóng xong mới còn đúng 1 nút "Xoá" (nút xác nhận trong hộp thoại)
        waitText("Xoá tin nhắn?"); compose.waitUntil(15_000) { compose.onAllNodesWithText("Xoá").fetchSemanticsNodes().size == 1 }
        clickText("Xoá")
        awaitDoc(ref) { !it.exists() }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("$marker edited").fetchSemanticsNodes().isEmpty() }
        screenshot("message-deleted"); log("MESSAGE_DELETE_OK ${ref.path}; peer receipt verified")
    }

    @Test fun notificationAndCommentCrud() {
        assertEquals(peer, uid)
        val incoming = awaitOne(db.collection("users").document(peer).collection("notifications")) { it.getString("type") == "NEW_MESSAGE" && it.getString("actorId") == primary }
        notificationCrud(incoming.reference)
        clickDescription("Quay lại")
        clickDescription("Hộp thư"); clickText("Firebase Test"); waitDescription("Chụp ảnh gửi riêng"); waitText("$marker edited")
        screenshot("peer-received-message")
        awaitDoc(db.collection("conversations").document(conversation)) { ((it.get("readAt") as? Map<*, *>)?.get(peer)) != null }
        clickDescription("Quay lại")
        clickDescription("Khám phá")
        commentCrud()
    }

    @Test fun commentCrudAfterNotifications() {
        assertEquals(peer, uid)
        commentCrud()
    }

    private fun commentCrud() {
        clickText("Bạn bè")
        waitDescription(sampleCaption)
        compose.onNodeWithContentDescription(sampleCaption).performScrollTo().performClick()
        waitText("Chi tiết khoảnh khắc")
        compose.waitUntil(30_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 1 }
        val collection = db.collection("posts").document(sampleId).collection("comments")
        val existing = runBlocking { collection.get(Source.SERVER).await().documents.filter { it.getString("content") == marker && it.getString("authorId") == peer } }
        assertTrue("Do not duplicate a test comment", existing.size <= 1)
        if (existing.isEmpty()) {
            compose.onNode(hasSetTextAction()).performTextReplacement(marker)
            ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            clickDescription("Gửi")
        }
        val ref=awaitOne(collection) { it.getString("content")==marker && it.getString("authorId")==peer }.reference
        waitText(marker); revealComment(marker); screenshot("comment-created")
        compose.onNodeWithText("Sửa bình luận").performClick()
        compose.onNodeWithContentDescription("Nội dung chỉnh sửa").performTextReplacement("$marker edited")
        clickText("Lưu")
        awaitDoc(ref) { it.getString("content")=="$marker edited" };waitText("$marker edited");revealComment("$marker edited");screenshot("comment-edited")
        compose.onNodeWithText("Xoá bình luận").performClick();clickText("Xoá")
        awaitDoc(ref) { !it.exists() };screenshot("comment-deleted")
        assertTrue(read(db.collection("posts").document(sampleId)).exists())
        log("COMMENT_CRUD_OK ${ref.path}; sample preserved")
    }

    @Test fun commentNotificationCrud() {
        assertEquals(primary,uid)
        val incoming = awaitOne(db.collection("users").document(primary).collection("notifications")) { it.getString("type")=="COMMENT" && it.getString("actorId")==peer }
        notificationCrud(incoming.reference)
    }

    // Đọc / chưa đọc / xoá nằm trong menu ⋮ của từng thông báo: mở menu rồi mới bấm mục cần thử
    private fun notificationCrud(ref: DocumentReference) {
        clickDescription("Thông báo")
        val menu="notification-menu-${ref.id}"
        val tag="notification-read-${ref.id}"
        compose.waitUntil(30_000) { compose.onAllNodesWithTag(menu).fetchSemanticsNodes().isNotEmpty() }
        screenshot("notification-created")
        fun openMenu() = compose.onNodeWithTag(menu).performScrollTo().performClick()
        if (read(ref).getBoolean("isRead") == true) {
            openMenu(); waitNotificationLabel(tag, "Đánh dấu chưa đọc")
            compose.onNodeWithTag(tag).performClick()
            awaitDoc(ref) { it.getBoolean("isRead") == false }
        }
        openMenu(); waitNotificationLabel(tag, "Đánh dấu đã đọc")
        compose.onNodeWithTag(tag).performClick()
        awaitDoc(ref) { it.getBoolean("isRead")==true }; openMenu(); waitNotificationLabel(tag, "Đánh dấu chưa đọc"); screenshot("notification-read")
        compose.onNodeWithTag(tag).performClick()
        awaitDoc(ref) { it.getBoolean("isRead")==false }; openMenu(); waitNotificationLabel(tag, "Đánh dấu đã đọc"); screenshot("notification-unread")
        compose.onNodeWithTag("notification-delete-${ref.id}").performClick()
        awaitDoc(ref) { !it.exists() };screenshot("notification-deleted")
        log("NOTIFICATION_CRUD_OK ${ref.path}")
    }
    private fun revealComment(text: String) {
        repeat(6) {
            if (runCatching { compose.onNodeWithText(text).assertIsDisplayed() }.isSuccess) return
            // Start inside the scrollable content, above the fixed comment composer.
            compose.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = height * 0.7f, endY = height * 0.2f)
            }
        }
        screenshot("comment-scroll-debug")
        log(compose.onRoot().printToString())
        compose.onNodeWithText(text).assertIsDisplayed()
    }
    private fun waitNotificationLabel(tag: String, label: String) { compose.waitUntil(30_000) {
        compose.onAllNodes(hasTestTag(tag) and hasText(label)).fetchSemanticsNodes().isNotEmpty()
    } }
    private fun clickDescription(text: String) {waitDescription(text);compose.onNodeWithContentDescription(text).performClick()}
    private fun clickText(text: String) {waitText(text);compose.onNodeWithText(text).performClick()}
    private fun longPressText(text: String) {waitText(text);compose.onNodeWithText(text).performTouchInput { longClick() }}
    private fun waitText(text: String) {compose.waitUntil(30_000) {compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
    private fun waitDescription(text: String) {compose.waitUntil(45_000) {compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty()}}
    private fun read(ref: DocumentReference) = runBlocking {withTimeout(30_000) {ref.get(Source.SERVER).await().also {assertFalse(it.metadata.isFromCache);assertFalse(it.metadata.hasPendingWrites())}}}
    private fun awaitDoc(ref: DocumentReference, condition: (DocumentSnapshot)->Boolean) {runBlocking {withTimeout(45_000) {while(true) {val d=ref.get(Source.SERVER).await();if(!d.metadata.isFromCache && !d.metadata.hasPendingWrites() && condition(d)) break;delay(500)}}}}
    private fun awaitOne(query: Query, condition: (DocumentSnapshot)->Boolean): DocumentSnapshot = runBlocking {
        withTimeout(45_000) {var result: DocumentSnapshot?=null;while(result==null){val s=query.get(Source.SERVER).await();assertFalse(s.metadata.isFromCache);if(!s.metadata.hasPendingWrites()) result=s.documents.filter(condition).singleOrNull();if(result==null)delay(500)};requireNotNull(result)}
    }
    private fun screenshot(name:String) {runCatching {val dir=File(ins.targetContext.getExternalFilesDir(null),"live-social/$marker/$uid").apply{mkdirs()};val bitmap=ins.uiAutomation.takeScreenshot();File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()};if(name!="failure")Thread.sleep(2500)}
    private fun log(text:String) { ins.sendStatus(0,Bundle().apply{putString("stream","\nLIVE_SOCIAL: $text\n")}) }
}
