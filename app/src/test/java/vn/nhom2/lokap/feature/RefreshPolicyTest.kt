package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeLandmarkRepository
import vn.nhom2.lokap.data.fake.FakeLocationRepository
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.feature.feed.FeedAction
import vn.nhom2.lokap.feature.feed.FeedViewModel
import vn.nhom2.lokap.feature.map.MapAction
import vn.nhom2.lokap.feature.map.MapViewModel
import vn.nhom2.lokap.feature.region.RegionAlbumAction
import vn.nhom2.lokap.feature.region.RegionAlbumViewModel

/** Ghi lại các lần làm mới feed: force = false là lúc mở màn (được bỏ qua nếu vừa tải), true là người dùng yêu cầu. */
private class RecordingPostRepository(inner: PostRepository = FakePostRepository()) : PostRepository by inner {
    val forces = mutableListOf<Boolean>()
    override suspend fun refreshFeeds(force: Boolean): Result<Unit> {
        forces += force
        return Result.success(Unit)
    }
}

private class RecordingLandmarkRepository(inner: LandmarkRepository = FakeLandmarkRepository()) : LandmarkRepository by inner {
    val forces = mutableListOf<Boolean>()
    override suspend fun refresh(force: Boolean): Result<Unit> {
        forces += force
        return Result.success(Unit)
    }
}

/** Mở màn thì dùng cache có hạn; kéo để làm mới / Thử lại thì luôn tải từ server. */
@OptIn(ExperimentalCoroutinesApi::class)
class RefreshPolicyTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        FakePostStore.resetForTest()
    }

    @After
    fun tearDown() {
        FakePostStore.resetForTest()
        Dispatchers.resetMain()
    }

    @Test
    fun `Feed mo man dung cache, keo de lam moi va Thu lai thi tai that`() {
        val posts = RecordingPostRepository()
        val vm = FeedViewModel(posts, FakeSocialRepository(), FakeAuthRepository(), ContentTestRepository(), ContentTestRepository(), ContentTestRepository())
        assertEquals(listOf(false), posts.forces)

        vm.onAction(FeedAction.OnRefresh)
        vm.onAction(FeedAction.OnRetry)
        assertEquals(listOf(false, true, true), posts.forces)
    }

    @Test
    fun `Ban do mo man dung cache cho feed va dia danh, Thu lai thi tai that`() {
        val posts = RecordingPostRepository()
        val landmarks = RecordingLandmarkRepository()
        val vm = MapViewModel(FakeLocationRepository(), posts, FakeSocialRepository(), landmarks)
        assertEquals(listOf(false), posts.forces)
        assertEquals(listOf(false), landmarks.forces)

        vm.onAction(MapAction.OnRetry)
        assertEquals(listOf(false, true), posts.forces)
        assertEquals(listOf(false, true), landmarks.forces)
    }

    @Test
    fun `Album vung mo man dung cache, Thu lai thi tai that`() {
        val posts = RecordingPostRepository()
        val vm = RegionAlbumViewModel(
            SavedStateHandle(mapOf("level" to "PROVINCE", "code" to "48")), posts, FakeSocialRepository(), FakeAuthRepository()
        )
        assertEquals(listOf(false), posts.forces)

        vm.onAction(RegionAlbumAction.OnRetry)
        assertEquals(listOf(false, true), posts.forces)
    }
}
