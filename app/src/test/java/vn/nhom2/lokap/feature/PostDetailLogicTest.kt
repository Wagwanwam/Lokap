package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
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
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.feature.postdetail.PostDetailAction
import vn.nhom2.lokap.feature.postdetail.PostDetailEffect
import vn.nhom2.lokap.feature.postdetail.PostDetailViewModel

/** Chi tiết bài đọc qua PostRepository; bài của mình có thêm Sửa lời nhắn / Xoá bài. */
@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailLogicTest {

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

    private fun detail(postId: String) = PostDetailViewModel(
        SavedStateHandle(mapOf("id" to postId)),
        FakePostRepository(),
        FakeSocialRepository(),
        FakeAuthRepository(), ContentTestRepository(), ContentTestRepository()
    )

    private fun offlineDetail(postId: String) = PostDetailViewModel(
        SavedStateHandle(mapOf("id" to postId)),
        OfflinePostRepository(),
        FakeSocialRepository(),
        FakeAuthRepository(), ContentTestRepository(), ContentTestRepository()
    )

    private suspend fun PostDetailViewModel.nextEffect() = withTimeoutOrNull(1000) { effects.first() }

    @Test
    fun `mat mang nhung bai da co trong may thi van xem duoc`() {
        val vm = offlineDetail("p_01")
        assertEquals("p_01", vm.uiState.value.post?.id)
        assertNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.isOffline)
        assertFalse(vm.uiState.value.isNotFound)
    }

    @Test
    fun `mat mang va chua co bai trong may thi bao loi de thu lai, khong bao da xoa`() {
        val vm = offlineDetail("p_khong_co")
        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isNotFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `xoa bai xong khong nhay man Da xoa truoc khi quay lai`() = runBlocking {
        val vm = detail("p_04")
        vm.onAction(PostDetailAction.OnOpenDeleteDialog)
        vm.onAction(PostDetailAction.OnConfirmDelete)
        assertNull(vm.uiState.value.post)
        assertFalse(vm.uiState.value.isNotFound)
        assertEquals(PostDetailEffect.NavigateBack, vm.nextEffect())
    }

    @Test
    fun `mo bai thi co bai va biet bai nao la cua minh`() {
        val mine = detail("p_04")
        assertEquals("p_04", mine.uiState.value.post?.id)
        assertTrue(mine.uiState.value.isOwnPost)
        assertFalse(mine.uiState.value.isLoading)
        assertFalse(detail("p_01").uiState.value.isOwnPost)
    }

    @Test
    fun `bai khong ton tai thi bao da xoa`() {
        val vm = detail("p_khong_co")
        assertNull(vm.uiState.value.post)
        assertTrue(vm.uiState.value.isNotFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `sua loi nhan toi da 45 ky tu, luu thi cap nhat bai`() {
        val vm = detail("p_04")
        vm.onAction(PostDetailAction.OnOpenEditCaption)
        assertTrue(vm.uiState.value.isEditCaptionOpen)
        assertEquals(SampleData.posts.first { it.id == "p_04" }.caption, vm.uiState.value.editCaptionInput)

        vm.onAction(PostDetailAction.OnChangeCaptionInput("a".repeat(70)))
        assertEquals(45, vm.uiState.value.editCaptionInput.length)

        vm.onAction(PostDetailAction.OnChangeCaptionInput("  Lời nhắn mới  "))
        vm.onAction(PostDetailAction.OnSaveCaption)
        assertEquals("Lời nhắn mới", FakePostStore.posts.value.first { it.id == "p_04" }.caption)
        assertEquals("Lời nhắn mới", vm.uiState.value.post?.caption)
        assertFalse(vm.uiState.value.isEditCaptionOpen)
    }

    @Test
    fun `luu loi nhan rong thi bai khong con caption`() {
        val vm = detail("p_04")
        vm.onAction(PostDetailAction.OnOpenEditCaption)
        vm.onAction(PostDetailAction.OnChangeCaptionInput("   "))
        vm.onAction(PostDetailAction.OnSaveCaption)
        assertNull(FakePostStore.posts.value.first { it.id == "p_04" }.caption)
    }

    @Test
    fun `xoa bai xac nhan thi bo khoi kho va quay lai`() = runBlocking {
        val vm = detail("p_04")
        vm.onAction(PostDetailAction.OnOpenDeleteDialog)
        assertTrue(vm.uiState.value.showDeleteConfirmDialog)
        vm.onAction(PostDetailAction.OnConfirmDelete)
        assertTrue(FakePostStore.posts.value.none { it.id == "p_04" })
        assertEquals(PostDetailEffect.NavigateBack, vm.nextEffect())
    }

    @Test
    fun `bai nguoi khac khong mo duoc sua hay xoa`() {
        val vm = detail("p_01")
        vm.onAction(PostDetailAction.OnOpenEditCaption)
        vm.onAction(PostDetailAction.OnOpenDeleteDialog)
        assertFalse(vm.uiState.value.isEditCaptionOpen)
        assertFalse(vm.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun `chan tac gia thi ghi trang thai chan, bo moi bai cua ho va quay lai`() = runBlocking {
        val vm = detail("p_01")
        val authorId = SampleData.posts.first { it.id == "p_01" }.author.id
        vm.onAction(PostDetailAction.OnOpenBlockDialog)
        vm.onAction(PostDetailAction.OnConfirmBlock)
        assertEquals(FriendStatus.BLOCKED, SampleSocialData.getFriendStatus(authorId))
        assertTrue(FakePostStore.posts.value.none { it.author.id == authorId })
        assertEquals(PostDetailEffect.NavigateBack, vm.nextEffect())
    }

    @Test
    fun `bao cao thi an bai va quay lai`() = runBlocking {
        val vm = detail("p_01")
        vm.onAction(PostDetailAction.OnSubmitReport("Spam"))
        assertTrue(FakePostStore.posts.value.none { it.id == "p_01" })
        assertEquals(PostDetailEffect.NavigateBack, vm.nextEffect())
    }

    @Test
    fun `tha cam xuc thi doi myReaction cua bai, bam lai thi bo`() {
        val vm = detail("p_01")
        vm.onAction(PostDetailAction.OnReact("😂"))
        assertEquals("😂", vm.uiState.value.post?.myReaction)
        vm.onAction(PostDetailAction.OnReact("😂"))
        assertNull(vm.uiState.value.post?.myReaction)
    }
}
