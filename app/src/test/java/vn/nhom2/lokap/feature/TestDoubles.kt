package vn.nhom2.lokap.feature

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.PostDraft
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository

private fun offlineError() = AppException("Mất kết nối mạng. Kiểm tra mạng rồi thử lại.", AppException.Reason.OFFLINE)

/** PostRepository "mất mạng": mọi lần ghi / làm mới đều thất bại, phần đọc uỷ quyền cho bản giả. */
class OfflinePostRepository(
    inner: PostRepository = FakePostRepository()
) : PostRepository by inner {

    private val offline = offlineError()

    override suspend fun createPost(draft: PostDraft): Result<String> = Result.failure(offline)

    override suspend fun refreshFeeds(force: Boolean): Result<Unit> = Result.failure(offline)

    override suspend fun refreshPost(postId: String): Result<Unit> = Result.failure(offline)

    override suspend fun refreshUserPosts(userId: String): Result<Unit> = Result.failure(offline)
}

/**
 * PostRepository giống bản Firestore lúc mới mở app: cache trên máy (Room) về SAU khi ViewModel đã dựng,
 * còn làm mới từ server thì treo mãi (mạng chậm). Test điều khiển cache qua [cache].
 */
class SlowCachePostRepository(
    inner: PostRepository = FakePostRepository()
) : PostRepository by inner {

    val cache = MutableStateFlow<List<Post>>(emptyList())

    override val friendFeed: Flow<List<Post>> get() = cache

    override val publicPosts: Flow<List<Post>> get() = cache.map { list -> list.filter { it.audience == Audience.PUBLIC } }

    override suspend fun refreshFeeds(force: Boolean): Result<Unit> = awaitCancellation()
}

/** SocialRepository "mất mạng": graph đọc từ bản giả, mọi lời gọi server đều thất bại. */
class OfflineSocialRepository(
    inner: SocialRepository = FakeSocialRepository()
) : SocialRepository by inner {

    private val offline = offlineError()

    override suspend fun refresh(): Result<Unit> = Result.failure(offline)

    override suspend fun search(query: String): Result<List<User>> = Result.failure(offline)

    override suspend fun sendFriendRequest(userId: String): Result<FriendStatus> = Result.failure(offline)
}

/** SocialRepository với graph do test điều khiển: giả lập get_social_graph về muộn sau khi mở màn. */
class LateGraphSocialRepository(
    inner: SocialRepository = FakeSocialRepository()
) : SocialRepository by inner {

    val graphState = MutableStateFlow(SocialGraph())

    override val graph: StateFlow<SocialGraph> get() = graphState

    /** Như bản Firestore: gợi ý suy ra từ graph (bạn bè + người đang mời). */
    override suspend fun suggestions(): Result<List<User>> =
        Result.success(graphState.value.usersWith(FriendStatus.REQUEST_RECEIVED) + graphState.value.usersWith(FriendStatus.FRIENDS))
}
