package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.feature.feed.FeedAction
import vn.nhom2.lokap.feature.feed.FeedViewModel
import vn.nhom2.lokap.feature.post.CommentUi
import vn.nhom2.lokap.feature.post.CommentsAction
import vn.nhom2.lokap.feature.post.CommentsViewModel
import vn.nhom2.lokap.feature.postdetail.PostDetailAction
import vn.nhom2.lokap.feature.postdetail.PostDetailViewModel

/** Thanh "Gửi tin nhắn…" dưới ảnh, popup bình luận và trang ảnh (thay màn chi tiết). */
@OptIn(ExperimentalCoroutinesApi::class)
class PostInteractionLogicTest {

    private val content = ContentTestRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        FakePostStore.resetForTest()
        SampleSocialData.resetAccountForDemo()
    }

    @After
    fun tearDown() {
        FakePostStore.resetForTest()
        SampleSocialData.resetAccountForDemo()
        Dispatchers.resetMain()
    }

    private fun feed() = FeedViewModel(FakePostRepository(), FakeSocialRepository(), FakeAuthRepository(), content, content, content)

    private fun viewer(postId: String) = PostDetailViewModel(
        SavedStateHandle(mapOf("id" to postId)), FakePostRepository(), FakeSocialRepository(), FakeAuthRepository(), content, content
    )

    private fun post(id: String) = SampleData.posts.first { it.id == id }

    // p_01: bạn bè đăng công khai · p_02: bạn bè đăng cho bạn bè · p_04: bài của mình
    @Test
    fun `moi anh cua ban be deu co thanh nhan tin, ke ca anh cong khai`() {
        val state = feed().uiState.value
        assertEquals(Audience.PUBLIC, post("p_01").audience)
        assertTrue("Ảnh công khai của bạn bè: nhắn tin được", state.canMessage(post("p_01")))
        val stranger = post("p_01").let { it.copy(author = it.author.copy(id = "user_nguoi_la")) }
        assertFalse("Ảnh công khai của người lạ: không nhắn tin", state.canMessage(stranger))
        assertTrue(state.canMessage(post("p_02")))
        assertFalse("Ảnh của mình: không nhắn cho chính mình", state.canMessage(post("p_04")))
    }

    @Test
    fun `gui tin duoi anh thi kem anh, o lai Feed va xoa noi dung da go`() {
        val vm = feed()
        val photo = post("p_02")
        vm.onAction(FeedAction.OnMessageDraftChange(photo.id, "Đẹp quá"))
        vm.onAction(FeedAction.OnSendPhotoMessage(photo))
        assertEquals(1, content.sendCount)
        assertEquals(photo.id, content.lastReplyTo?.id)
        assertNull(vm.uiState.value.messageDrafts[photo.id])
        assertTrue(vm.uiState.value.sendingMessagePostIds.isEmpty())
    }

    @Test
    fun `gui loi thi giu noi dung de gui lai`() {
        val vm = feed()
        val photo = post("p_02")
        content.failure = AppException("denied")
        vm.onAction(FeedAction.OnMessageDraftChange(photo.id, "Đẹp quá"))
        vm.onAction(FeedAction.OnSendPhotoMessage(photo))
        assertEquals("Đẹp quá", vm.uiState.value.messageDrafts[photo.id])
    }

    @Test
    fun `anh cong khai cua nguoi la khong gui duoc tin, cua ban be thi gui kem anh`() {
        val vm = feed()
        val strangerPhoto = post("p_01").let { it.copy(author = it.author.copy(id = "user_nguoi_la")) }
        vm.onAction(FeedAction.OnMessageDraftChange(strangerPhoto.id, "Chào"))
        vm.onAction(FeedAction.OnSendPhotoMessage(strangerPhoto))
        assertEquals(0, content.sendCount)

        val friendPublic = post("p_01")
        vm.onAction(FeedAction.OnMessageDraftChange(friendPublic.id, "Chào"))
        vm.onAction(FeedAction.OnSendPhotoMessage(friendPublic))
        assertEquals(1, content.sendCount)
        assertEquals("p_01", content.lastReplyTo?.id)
    }

    @Test
    fun `bam binh luan trong Feed mo popup, khong chuyen man`() {
        val vm = feed()
        vm.onAction(FeedAction.OnOpenComments("p_01"))
        assertEquals("p_01", vm.uiState.value.activeCommentPostId)
        vm.onAction(FeedAction.OnDismissComments)
        assertNull(vm.uiState.value.activeCommentPostId)
    }

    @Test
    fun `popup binh luan gui xong thi xoa o nhap va bo tra loi`() {
        val vm = CommentsViewModel(content, FakeAuthRepository())
        vm.open("p_01")
        assertFalse(vm.uiState.value.isLoading)
        val target = CommentUi(id = "c1", authorId = "u", authorName = "An", timeAgo = "Vừa xong", content = "hi")
        vm.onAction(CommentsAction.OnReplyTo(target))
        vm.onAction(CommentsAction.OnInputChange("Hay quá"))
        vm.onAction(CommentsAction.OnSend)
        assertEquals("", vm.uiState.value.input)
        assertNull(vm.uiState.value.replyingTo)
    }

    @Test
    fun `binh luan loi thi giu noi dung va bao loi`() {
        val vm = CommentsViewModel(content, FakeAuthRepository())
        vm.open("p_01")
        content.failure = AppException("denied")
        vm.onAction(CommentsAction.OnInputChange("Hay quá"))
        vm.onAction(CommentsAction.OnSend)
        assertEquals("Hay quá", vm.uiState.value.input)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `dong popup thi quen bai dang mo`() {
        val vm = CommentsViewModel(content, FakeAuthRepository())
        vm.open("p_01")
        vm.close()
        vm.onAction(CommentsAction.OnInputChange("x"))
        assertNull(vm.uiState.value.postId)
        assertEquals("", vm.uiState.value.input)
    }

    @Test
    fun `trang anh - anh cong khai cua ban be co ca binh luan lan nhan tin`() {
        val vm = viewer("p_01")
        assertTrue(vm.uiState.value.canMessage)
        assertTrue("Bài công khai có bình luận", vm.uiState.value.showComments)
    }

    @Test
    fun `trang anh - anh ban be gui tin kem anh`() {
        val vm = viewer("p_02")
        assertTrue(vm.uiState.value.canMessage)
        vm.onAction(PostDetailAction.OnMessageDraftChange("Nhớ quá"))
        vm.onAction(PostDetailAction.OnSendMessage)
        assertEquals("p_02", content.lastReplyTo?.id)
        assertEquals("", vm.uiState.value.messageDraft)
    }
}
