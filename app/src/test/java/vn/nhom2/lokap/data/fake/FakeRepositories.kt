package vn.nhom2.lokap.data.fake

import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import vn.nhom2.lokap.domain.model.AppSettingsData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.domain.repository.PostDraft
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.ProfileDraft
import vn.nhom2.lokap.domain.repository.RegisterResult
import vn.nhom2.lokap.domain.repository.SettingsRepository
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository

/*
 * Bản dữ liệu giả của các Repository: chỉ bọc các store sẵn có (FakeAuthStore, FakePostStore,
 * SampleSocialData) nên hành vi giống hệt tuần 2 và unit test cũ vẫn đúng.
 * Dùng khi .env / app/google-services.json chưa có cấu hình Firebase (BuildConfig.USE_FAKE_DATA = true).
 */

/** Unconfined: StateFlow suy ra cập nhật ngay trong lúc store đổi giá trị (test đọc .value ngay sau đó). */
private val fakeScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

private fun sessionOf(user: User?): AuthSession = when {
    user == null -> AuthSession.SignedOut
    user.username.isBlank() -> AuthSession.NeedsProfile(user.id, email = null)
    else -> AuthSession.SignedIn(user)
}

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    override val session: StateFlow<AuthSession> = FakeAuthStore.currentUser
        .map(::sessionOf)
        .stateIn(fakeScope, SharingStarted.Eagerly, sessionOf(FakeAuthStore.currentUser.value))

    /** Dữ liệu giả luôn xem "tôi" là người dùng mẫu (bài, bạn bè mẫu đều gắn với người này). */
    override val currentUser: StateFlow<User?> = SampleSocialData.currentUserOverride
        .map<User?, User?> { it ?: SampleData.currentUser }
        .stateIn(fakeScope, SharingStarted.Eagerly, SampleSocialData.getCurrentUser())

    override val onboardingDone: StateFlow<Boolean> = FakeAuthStore.onboardingDone

    override fun markOnboardingDone() = FakeAuthStore.markOnboardingDone()

    override suspend fun login(email: String, password: String): Result<User> =
        FakeAuthStore.login(email, password)

    override suspend fun loginWithGoogle(idToken: String): Result<User> = FakeAuthStore.loginWithGoogle()

    override suspend fun register(email: String, password: String): Result<RegisterResult> =
        FakeAuthStore.register(email = email, password = password).map { RegisterResult.SignedIn }

    override suspend fun confirmEmailVerified(): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.success(Unit)

    override suspend fun isEmailAvailable(email: String): Result<Boolean> = FakeAuthStore.isEmailAvailable(email)

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> =
        FakeAuthStore.isUsernameAvailable(username)

    override suspend fun completeProfile(draft: ProfileDraft): Result<User> =
        FakeAuthStore.completeProfile(
            username = draft.username,
            displayName = draft.displayName,
            avatarUrl = draft.avatarUrl
        )

    override suspend fun updateProfile(displayName: String, username: String, bio: String): Result<User> {
        SampleSocialData.updateMyProfile(displayName = displayName, username = username, bio = bio)
        return Result.success(SampleSocialData.getCurrentUser())
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> {
        delay(400)
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> = FakeAuthStore.resetPassword(email)

    override suspend fun logout() = FakeAuthStore.logout()

    override suspend fun deleteAccount(): Result<Unit> {
        SampleSocialData.resetAccountForDemo()
        FakeAuthStore.logout()
        return Result.success(Unit)
    }
}

@Singleton
class FakePostRepository @Inject constructor() : PostRepository {

    override val friendFeed: Flow<List<Post>> = FakePostStore.posts.map { all ->
        // Dữ liệu giả: mọi người trong SampleData.users đều là bạn của "tôi"
        val meId = SampleData.currentUser.id
        val friendIds = SampleData.users.map { it.id }.toSet()
        all.filter { it.author.id == meId || it.author.id in friendIds }.sortedByDescending { it.takenAt }
    }

    override val publicPosts: Flow<List<Post>> = FakePostStore.posts.map { all ->
        all.filter { post ->
            post.audience == Audience.PUBLIC &&
                post.sendState == PostSendState.SENT &&
                (post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(SampleData.NOW))
        }.sortedByDescending { it.takenAt }
    }

    override val savedPosts: Flow<List<Post>> = FakePostStore.posts.map { all ->
        all.filter { it.imageUrl.isNotBlank() && it.id.hashCode() % 3 == 0 }.take(6)
    }

    override val repostedPosts: Flow<List<Post>> = FakePostStore.posts.map { all ->
        all.filter { it.imageUrl.isNotBlank() && it.id.hashCode() % 2 != 0 }.take(4)
    }

    override fun observePost(postId: String): Flow<Post?> =
        FakePostStore.posts.map { list -> list.firstOrNull { it.id == postId } }

    override fun observeUserPosts(userId: String): Flow<List<Post>> = combine(
        FakePostStore.posts,
        SampleSocialData.friendStatuses,
        SampleSocialData.currentUserOverride
    ) { all, statuses, _ ->
        val me = SampleSocialData.getCurrentUser()
        val target = SampleSocialData.findUser(userId) ?: return@combine emptyList()
        val isMe = target.id == me.id
        val status = if (isMe) FriendStatus.NONE else statuses[target.id] ?: FriendStatus.NONE
        val visible = all
            .filter { post ->
                post.author.id == target.id &&
                    SampleSocialData.isPostVisibleToViewer(post, isMe = isMe, friendStatus = status, now = SampleData.NOW)
            }
            .sortedByDescending { it.takenAt }
        // Hồ sơ mẫu của "tôi" ít bài: mượn ảnh mẫu cho lưới đầy (chỉ ở dữ liệu giả)
        if (isMe && visible.size < 6) {
            visible + SampleData.posts.filter { it.author.id != target.id }
                .take(9 - visible.size)
                .mapIndexed { idx, p -> p.copy(id = "my_profile_photo_$idx", author = target) }
        } else {
            visible
        }
    }

    override fun now(): Instant = SampleData.NOW

    override fun observeLandmarkPosts(landmarkId: String): Flow<List<Post>> = FakePostStore.posts.map { all ->
        all.filter { it.location.landmarkId == landmarkId && it.audience == Audience.PUBLIC }
    }

    override suspend fun refreshLandmarkPosts(landmarkId: String): Result<Unit> = Result.success(Unit)

    override suspend fun setPostLandmark(postId: String, landmark: Landmark?): Result<Unit> {
        val post = FakePostStore.posts.value.firstOrNull { it.id == postId }
            ?: return Result.failure(AppException("Bài đã bị xoá hoặc ẩn", AppException.Reason.NOT_FOUND))
        if (post.author.id != SampleSocialData.getCurrentUser().id) return Result.failure(AppException("Chỉ gắn được ảnh của bạn"))
        if (post.audience != Audience.PUBLIC) return Result.failure(AppException("Chỉ ảnh công khai mới gắn được vào địa danh"))
        FakePostStore.setLandmark(postId, landmark)
        return Result.success(Unit)
    }

    override suspend fun refreshFeeds(force: Boolean): Result<Unit> {
        delay(600L)
        return Result.success(Unit)
    }

    override suspend fun refreshPost(postId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshUserPosts(userId: String): Result<Unit> = Result.success(Unit)

    override suspend fun createPost(draft: PostDraft): Result<String> {
        val now = SampleData.NOW
        val post = Post(
            id = "post_${UUID.randomUUID()}",
            author = SampleSocialData.getCurrentUser(),
            imageUrl = draft.imagePath.ifBlank { "https://picsum.photos/seed/lokap_new/800/800" },
            caption = draft.caption?.trim()?.ifEmpty { null },
            audience = draft.audience,
            location = draft.location,
            takenAt = now,
            publicVisibleAt = if (draft.audience == Audience.PUBLIC) {
                now.plus(Duration.ofDays(draft.publishDelay.days.toLong()))
            } else {
                null
            },
            reactions = emptyList(),
            myReaction = null,
            sendState = PostSendState.PENDING
        )
        FakePostStore.addPendingPostAndSimulateSend(post)
        return Result.success(post.id)
    }

    override suspend fun retrySend(postId: String) = FakePostStore.retrySend(postId)

    override suspend fun updateCaption(postId: String, caption: String?): Result<Unit> {
        FakePostStore.updateCaption(postId, caption?.trim()?.take(60)?.ifEmpty { null })
        return Result.success(Unit)
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        FakePostStore.removePost(postId)
        return Result.success(Unit)
    }

    override suspend fun toggleReaction(postId: String, emoji: String): Result<Unit> {
        FakePostStore.toggleReaction(postId, emoji)
        return Result.success(Unit)
    }

    override suspend fun reportPost(postId: String, reason: String): Result<Unit> {
        // Giống tuần 2: ẩn bài khỏi máy người báo cáo
        FakePostStore.removePost(postId)
        return Result.success(Unit)
    }

    override suspend fun hidePostsByAuthor(authorId: String) = FakePostStore.removePostsByAuthor(authorId)
}

@Singleton
class FakeSocialRepository @Inject constructor() : SocialRepository {

    override val maxFriends: Int = SampleSocialData.MAX_FRIENDS

    override val graph: StateFlow<SocialGraph> = SampleSocialData.friendStatuses
        .map(::graphOf)
        .stateIn(fakeScope, SharingStarted.Eagerly, graphOf(SampleSocialData.friendStatuses.value))

    private fun graphOf(statuses: Map<String, FriendStatus>): SocialGraph {
        val myId = SampleSocialData.getCurrentUser().id
        return SocialGraph(
            statuses = statuses.filterKeys { it != myId },
            users = SampleSocialData.directoryUsers.filter { it.id != myId }.associateBy { it.id }
        )
    }

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)

    override suspend fun search(query: String): Result<List<User>> {
        val q = query.trim().removePrefix("@").lowercase()
        if (q.isBlank()) return Result.success(emptyList())
        val myId = SampleSocialData.getCurrentUser().id
        val statuses = SampleSocialData.friendStatuses.value
        return Result.success(
            SampleSocialData.directoryUsers.filter { user ->
                user.id != myId &&
                    statuses[user.id] != FriendStatus.BLOCKED &&
                    (user.username.lowercase().contains(q) || user.displayName.lowercase().contains(q))
            }
        )
    }

    override suspend fun suggestions(): Result<List<User>> =
        Result.success(SampleData.users.filter { it.id != SampleData.currentUser.id })

    override suspend fun getUser(userId: String): Result<User?> = Result.success(SampleSocialData.findUser(userId))

    override suspend fun getFriendCount(userId: String): Result<Int> =
        Result.success(SampleSocialData.getFriendsCount(userId))

    override suspend fun sendFriendRequest(userId: String): Result<FriendStatus> =
        if (SampleSocialData.sendFriendRequest(userId)) {
            Result.success(FriendStatus.REQUEST_SENT)
        } else {
            Result.failure(friendLimit())
        }

    override suspend fun cancelFriendRequest(userId: String): Result<Unit> {
        SampleSocialData.cancelFriendRequest(userId)
        return Result.success(Unit)
    }

    override suspend fun acceptFriendRequest(userId: String): Result<Unit> =
        if (SampleSocialData.acceptFriendRequest(userId)) Result.success(Unit) else Result.failure(friendLimit())

    override suspend fun declineFriendRequest(userId: String): Result<Unit> {
        SampleSocialData.declineFriendRequest(userId)
        return Result.success(Unit)
    }

    override suspend fun unfriend(userId: String): Result<Unit> {
        SampleSocialData.unfriend(userId)
        return Result.success(Unit)
    }

    override suspend fun block(userId: String): Result<Unit> {
        SampleSocialData.blockUser(userId)
        return Result.success(Unit)
    }

    override suspend fun unblock(userId: String): Result<Unit> {
        SampleSocialData.unblockUser(userId)
        return Result.success(Unit)
    }

    private fun friendLimit() =
        AppException("Bạn đã đạt giới hạn ${SampleSocialData.MAX_FRIENDS} người bạn", AppException.Reason.FRIEND_LIMIT)
}

@Singleton
class FakeSettingsRepository @Inject constructor() : SettingsRepository {

    override val settings: StateFlow<AppSettingsData> = SampleSocialData.settings

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)

    override suspend fun update(transform: (AppSettingsData) -> AppSettingsData): Result<Unit> {
        SampleSocialData.updateSettings(transform)
        return Result.success(Unit)
    }
}
