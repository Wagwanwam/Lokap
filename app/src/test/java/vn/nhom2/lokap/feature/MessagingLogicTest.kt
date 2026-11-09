package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeChatStore
import vn.nhom2.lokap.data.fake.FakeNotificationStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.data.fake.lastMessageIn
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.NotificationType
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.feature.camera.CameraViewModel
import vn.nhom2.lokap.feature.chat.ChatAction
import vn.nhom2.lokap.feature.chat.ChatTimelineItem
import vn.nhom2.lokap.feature.chat.ChatViewModel
import vn.nhom2.lokap.feature.feed.FeedViewModel
import vn.nhom2.lokap.feature.notifications.NotificationsAction
import vn.nhom2.lokap.feature.notifications.NotificationsEffect
import vn.nhom2.lokap.feature.notifications.NotificationsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingLogicTest {

    private lateinit var dispatcher: TestDispatcher
    private val conv = SampleData.conversations.first()
    private val myId = SampleData.currentUser.id

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        SampleSocialData.resetAccountForDemo()
        FakeNotificationStore.resetForTest()
    }

    @After
    fun tearDown() {
        SampleSocialData.resetAccountForDemo()
        FakeNotificationStore.resetForTest()
        Dispatchers.resetMain()
    }

    private fun chat(id: String = conv.id) = ChatViewModel(SavedStateHandle(mapOf("conversationId" to id)))

    private fun ChatViewModel.mine() = uiState.value.timelineItems
        .filterIsInstance<ChatTimelineItem.MessageNode>()
        .filter { it.isMine }

    // ---------- Chỉ chat với bạn bè ----------

    @Test
    fun `khong con la ban thi kho tu choi va khoa o nhap`() = runTest(dispatcher) {
        SampleSocialData.unfriend(conv.friend.id)
        assertNull(FakeChatStore.addOutgoingMessage(conv.id, myId, "alo"))

        val vm = chat()
        assertFalse(vm.uiState.value.isFriendStatusValid)
        val before = FakeChatStore.messages.value.size
        vm.onAction(ChatAction.InputChanged("alo"))
        vm.onAction(ChatAction.SendMessage)
        assertEquals(before, FakeChatStore.messages.value.size)
        assertEquals("alo", vm.uiState.value.inputText) // không xoá chữ đã gõ
    }

    @Test
    fun `bi chan thi cung khong gui duoc`() {
        SampleSocialData.blockUser(conv.friend.id)
        assertNull(FakeChatStore.addOutgoingMessage(conv.id, myId, "alo"))
    }

    // ---------- Mở chat ----------

    @Test
    fun `mo chat thi so chua doc ve 0 va badge Camera giam`() = runTest(dispatcher) {
        val target = FakeChatStore.conversations.value.first { it.unreadCount > 0 }
        val camera = CameraViewModel()
        val before = camera.uiState.value.unreadMessages
        assertEquals(FakeChatStore.conversations.value.sumOf { it.unreadCount }, before)

        chat(target.id)
        assertEquals(0, FakeChatStore.getConversation(target.id)!!.unreadCount)
        assertEquals(before - target.unreadCount, camera.uiState.value.unreadMessages)
    }

    // ---------- Gửi tin, "Đã xem", "Đang nhập", trả lời ----------

    @Test
    fun `gui tin PENDING roi SENT, Da xem va Dang nhap khong ghi vao kho, tra loi theo vong xoay`() = runTest(dispatcher) {
        val vm = chat()
        val countBefore = FakeChatStore.messages.value.size

        vm.onAction(ChatAction.InputChanged("  chào  "))
        vm.onAction(ChatAction.SendMessage)
        val sent = FakeChatStore.lastMessageIn(conv.id)!!
        assertEquals("chào", sent.body)
        assertEquals(PostSendState.PENDING, sent.sendState)
        assertEquals("", vm.uiState.value.inputText)

        advanceTimeBy(801)
        assertEquals(PostSendState.SENT, FakeChatStore.messages.value.first { it.id == sent.id }.sendState)

        // Sau 2 giây: bạn "đã xem" + "đang nhập" — chỉ ở màn hình
        advanceTimeBy(1300)
        assertTrue(vm.uiState.value.isFriendTyping)
        assertEquals(countBefore + 1, FakeChatStore.messages.value.size)
        assertFalse("Kho không được ghi cờ đã xem", FakeChatStore.messages.value.first { it.id == sent.id }.seen)
        assertTrue("Màn hình hiện Đã xem", vm.mine().first { it.message.id == sent.id }.message.seen)

        advanceTimeBy(1600)
        assertFalse(vm.uiState.value.isFriendTyping)
        val reply1 = FakeChatStore.lastMessageIn(conv.id)!!
        assertEquals(conv.friend.id, reply1.senderId)

        vm.onAction(ChatAction.InputChanged("tiếp"))
        vm.onAction(ChatAction.SendMessage)
        advanceTimeBy(4000)
        val reply2 = FakeChatStore.lastMessageIn(conv.id)!!
        assertEquals(listOf("Ảnh cháy quá trời ơi 🔥", "Đang ở đâu đấy? Tí qua đón nha!"), listOf(reply1.body, reply2.body))
    }

    @Test
    fun `tin moi nam trong ngay Hom nay theo dong ho cua app`() {
        val vm = chat()
        vm.onAction(ChatAction.InputChanged("hôm nay"))
        vm.onAction(ChatAction.SendMessage)
        val headers = vm.uiState.value.timelineItems.filterIsInstance<ChatTimelineItem.DateHeader>()
        assertEquals("Hôm nay", headers.first().label) // danh sách đảo: mục đầu là ngày mới nhất
    }

    // ---------- Thông báo ----------

    private suspend fun NotificationsViewModel.nextEffect() = withTimeoutOrNull(1000) { effects.first() }

    @Test
    fun `cham thong bao bai viet mo dung bai va danh dau da doc trong kho`() = runTest(dispatcher) {
        val n = FakeNotificationStore.notifications.value.first { it.type == NotificationType.NEW_POST && it.postId != null && !it.isRead }
        val feed = FeedViewModel()
        val bellBefore = feed.uiState.value.unreadNotificationsCount
        val vm = NotificationsViewModel()
        vm.onAction(NotificationsAction.ClickNotification(n))
        assertEquals(NotificationsEffect.NavigateToPostDetail(n.postId!!), vm.nextEffect())
        assertTrue(FakeNotificationStore.notifications.value.first { it.id == n.id }.isRead)
        assertEquals(bellBefore - 1, feed.uiState.value.unreadNotificationsCount)
    }

    @Test
    fun `thong bao bai viet khong co postId thi khong mo bai nao`() = runTest(dispatcher) {
        val n = FakeNotificationStore.notifications.value.first { it.type == NotificationType.REACTION }.copy(postId = null)
        val vm = NotificationsViewModel()
        vm.onAction(NotificationsAction.ClickNotification(n))
        assertNull(vm.nextEffect())
    }

    @Test
    fun `cham thong bao tin nhan mo dung cuoc tro chuyen`() = runTest(dispatcher) {
        val n = FakeNotificationStore.notifications.value.first { it.type == NotificationType.NEW_MESSAGE }
        val vm = NotificationsViewModel()
        vm.onAction(NotificationsAction.ClickNotification(n))
        val effect = vm.nextEffect() as NotificationsEffect.NavigateToChat
        assertEquals(n.actor!!.id, FakeChatStore.getConversation(effect.conversationId)!!.friend.id)
    }

    @Test
    fun `chap nhan loi moi tu thong bao cap nhat danh sach ban that`() = runTest(dispatcher) {
        // Dữ liệu mẫu không còn lời mời từ người đã là bạn: dựng một lời mời từ người đang chờ kết bạn
        val requester = SampleSocialData.directoryUsers.first {
            SampleSocialData.getFriendStatus(it.id) == FriendStatus.REQUEST_RECEIVED
        }
        val n = FakeNotificationStore.notifications.value.first().copy(
            id = "notif_test_request", type = NotificationType.FRIEND_REQUEST, actor = requester, postId = null, isRead = false
        )
        FakeNotificationStore.addForTest(n)
        val vm = NotificationsViewModel()
        vm.onAction(NotificationsAction.AcceptFriendRequest(n.id))
        assertEquals(FriendStatus.FRIENDS, SampleSocialData.getFriendStatus(n.actor!!.id))
        assertEquals(true, vm.uiState.value.handledFriendRequestIds[n.id])
    }

    // ---------- Chat Theme ----------

    @Test
    fun `chat theme presets co dung 15 mau gom 7 don sac va 8 gradient`() {
        assertEquals(7, vn.nhom2.lokap.feature.chat.ChatThemePresets.SOLID_PRESETS.size)
        assertEquals(8, vn.nhom2.lokap.feature.chat.ChatThemePresets.GRADIENT_PRESETS.size)
        assertEquals(15, vn.nhom2.lokap.feature.chat.ChatThemePresets.ALL_PRESETS.size)
    }

    @Test
    fun `thay doi theme preset va hinh nen gallery cap nhat thanh cong`() = runTest(dispatcher) {
        val vm = chat()
        assertEquals("LOKAP_PINK", vm.uiState.value.themeConfig.presetId)
        assertNull(vm.uiState.value.themeConfig.customWallpaperUri)

        // Chọn preset OCEAN_BLUE
        vm.onAction(ChatAction.SelectThemePreset("OCEAN_BLUE"))
        assertEquals("OCEAN_BLUE", vm.uiState.value.themeConfig.presetId)

        // Chọn ảnh từ Gallery
        val sampleUri = "content://media/external/images/media/42"
        vm.onAction(ChatAction.SetCustomWallpaper(sampleUri))
        assertEquals(sampleUri, vm.uiState.value.themeConfig.customWallpaperUri)

        // Gỡ ảnh từ Gallery
        vm.onAction(ChatAction.RemoveCustomWallpaper)
        assertNull(vm.uiState.value.themeConfig.customWallpaperUri)
        assertEquals("OCEAN_BLUE", vm.uiState.value.themeConfig.presetId)
    }
}
