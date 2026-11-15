package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.feature.feed.FeedAction
import vn.nhom2.lokap.feature.feed.FeedViewModel
import vn.nhom2.lokap.feature.post.canRepost
import vn.nhom2.lokap.feature.post.mergeFriendTimeline
import vn.nhom2.lokap.feature.postdetail.PostDetailAction
import vn.nhom2.lokap.feature.postdetail.PostDetailViewModel

/** Đăng lại: ảnh nào đăng lại được, tab Bạn bè trộn ảnh đăng lại, nút ↻ ở Khám phá và trang ảnh. */
@OptIn(ExperimentalCoroutinesApi::class)
class RepostLogicTest {

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

    private fun post(id: String) = SampleData.posts.first { it.id == id }
    private val me get() = SampleData.currentUser.id

    /** Feed bạn bè không có p_10 (coi như ảnh công khai của người lạ). */
    private fun repoWithoutStranger(): PostRepository {
        val inner = FakePostRepository()
        return object : PostRepository by inner {
            override val friendFeed: Flow<List<Post>> = inner.friendFeed.map { list -> list.filterNot { it.id == "p_10" } }
        }
    }

    private fun feed(repo: PostRepository = FakePostRepository()) =
        FeedViewModel(repo, FakeSocialRepository(), FakeAuthRepository(), content, content, content)

    // p_01, p_10: ảnh công khai đã hiện của bạn bè · p_02: ảnh bạn bè · p_04: ảnh công khai đang hẹn giờ của mình

    @Test
    fun `chi dang lai anh cong khai da hien cua nguoi khac`() {
        val now = SampleData.NOW
        assertTrue(canRepost(post("p_01"), me, now))
        assertFalse("Ảnh bạn bè: bạn bè của mình không xem được", canRepost(post("p_02"), me, now))
        assertFalse("Ảnh của mình", canRepost(post("p_04"), me, now))
        val scheduled = post("p_10").copy(publicVisibleAt = now.plusSeconds(3600))
        assertFalse("Ảnh đang hẹn giờ", canRepost(scheduled, me, now))
        assertFalse("Chưa đăng nhập", canRepost(post("p_01"), "", now))
    }

    @Test
    fun `tron feed theo luc dang lai, bo anh trung`() {
        fun at(minutes: Long) = Instant.ofEpochSecond(1_000_000 + minutes * 60)
        val a = post("p_01").copy(id = "a", takenAt = at(10))
        val b = post("p_01").copy(id = "b", takenAt = at(5))
        val c = post("p_10").copy(id = "c", takenAt = at(0))
        val d = post("p_10").copy(id = "d", takenAt = at(0))
        val lan = SampleData.users[1]
        val merged = mergeFriendTimeline(
            originals = listOf(a, b),
            reposts = listOf(
                c.copy(repostedBy = lan, repostedAt = at(7)),
                a.copy(repostedBy = lan, repostedAt = at(12)), // đã có trong feed gốc
                d.copy(repostedBy = lan, repostedAt = at(1)),
                c.copy(repostedBy = SampleData.currentUser, repostedAt = at(3)) // lượt cũ hơn của cùng ảnh
            )
        )
        assertEquals(listOf("a", "c", "b", "d"), merged.map { it.id })
        assertNull("Bài gốc giữ nguyên, không gắn người đăng lại", merged[0].repostedBy)
        assertEquals(at(7), merged[1].repostedAt)
    }

    @Test
    fun `dang lai o Kham pha thi tang so luot va hien o tab Ban be`() {
        val vm = feed(repoWithoutStranger())
        assertFalse(vm.uiState.value.friendPosts.any { it.id == "p_10" })

        vm.onAction(FeedAction.OnToggleRepost("p_10"))
        val state = vm.uiState.value
        assertTrue(state.repostStates.getValue("p_10").isMine)
        assertEquals(1, state.repostStates.getValue("p_10").count)
        val reposted = state.friendPosts.single { it.id == "p_10" }
        assertEquals(me, reposted.repostedBy?.id)

        vm.onAction(FeedAction.OnSelectFriendFilter(me))
        assertTrue("Chọn \"Tôi\": có cả ảnh mình đã đăng lại", vm.uiState.value.visibleFriendPosts.any { it.id == "p_10" })

        vm.onAction(FeedAction.OnToggleRepost("p_10"))
        assertFalse(vm.uiState.value.repostStates.getValue("p_10").isMine)
        assertFalse(vm.uiState.value.friendPosts.any { it.id == "p_10" })
    }

    @Test
    fun `anh ban be da co trong feed thi dang lai khong hien lan nua`() {
        val vm = feed()
        vm.onAction(FeedAction.OnToggleRepost("p_01"))
        assertEquals(1, vm.uiState.value.friendPosts.count { it.id == "p_01" })
        assertNull(vm.uiState.value.friendPosts.first { it.id == "p_01" }.repostedBy)
    }

    @Test
    fun `trang anh co nut dang lai cho anh cong khai cua nguoi khac`() {
        val repo = FakePostRepository()
        val vm = PostDetailViewModel(
            SavedStateHandle(mapOf("id" to "p_01")), repo, FakeSocialRepository(), FakeAuthRepository(), content, content
        )
        assertTrue(vm.uiState.value.showRepost)
        assertTrue(vm.uiState.value.canRepost)
        vm.onAction(PostDetailAction.OnToggleRepost)
        assertTrue(vm.uiState.value.repostState.isMine)

        val own = PostDetailViewModel(
            SavedStateHandle(mapOf("id" to "p_04")), repo, FakeSocialRepository(), FakeAuthRepository(), content, content
        )
        assertTrue("Ảnh công khai của mình: hiện số lượt", own.uiState.value.showRepost)
        assertFalse("…nhưng không tự đăng lại", own.uiState.value.canRepost)
        own.onAction(PostDetailAction.OnToggleRepost)
        assertFalse(own.uiState.value.repostState.isMine)

        val friendsOnly = PostDetailViewModel(
            SavedStateHandle(mapOf("id" to "p_02")), repo, FakeSocialRepository(), FakeAuthRepository(), content, content
        )
        assertFalse(friendsOnly.uiState.value.showRepost)
    }
}
