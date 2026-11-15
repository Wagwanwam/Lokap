package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.feature.postdetail.RedditCommentUi
object FakeCommentStore {

    private val initialSampleComments: Map<String, List<RedditCommentUi>> = mapOf(
        "p_01" to listOf(
            RedditCommentUi(
                id = "c1_p_01",
                authorId = "user_khôi",
                authorName = "Nguyễn Minh Khôi",
                authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                timeAgo = "15 phút trước",
                content = "Góc này chụp lúc hoàng hôn lên ảnh chắc mê lắm luôn! 🔥",
                score = 14,
                userVote = 0,
                replies = listOf(
                    RedditCommentUi(
                        id = "c1_1_p_01",
                        authorId = "user_linh",
                        authorName = "Trần Khánh Linh",
                        authorAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9",
                        timeAgo = "10 phút trước",
                        content = "Chuẩn bạn ơi, tầm 5h30 chiều nắng xiên đẹp mê ly luôn á",
                        score = 6,
                        userVote = 0,
                        replyToAuthorName = "Nguyễn Minh Khôi",
                        replies = listOf(
                            RedditCommentUi(
                                id = "c1_1_1_p_01",
                                authorId = "user_khôi",
                                authorName = "Nguyễn Minh Khôi",
                                authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                                timeAgo = "5 phút trước",
                                content = "Cuối tuần này phải set kèo đi lại mới được!",
                                score = 3,
                                userVote = 0,
                                replyToAuthorName = "Trần Khánh Linh"
                            )
                        )
                    ),
                    RedditCommentUi(
                        id = "c1_2_p_01",
                        authorId = "user_nam",
                        authorName = "Lê Hoàng Nam",
                        authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d",
                        timeAgo = "8 phút trước",
                        content = "Chỗ này gửi xe có tiện không b?",
                        score = 2,
                        userVote = 0,
                        replyToAuthorName = "Nguyễn Minh Khôi"
                    )
                )
            ),
            RedditCommentUi(
                id = "c2_p_01",
                authorId = "user_thảo",
                authorName = "Phương Thảo",
                authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
                timeAgo = "30 phút trước",
                content = "Filter màu này trông nghệ thật sự, dùng filter nào trong app vậy bạn?",
                score = 9,
                userVote = 0,
                replies = listOf(
                    RedditCommentUi(
                        id = "c2_1_p_01",
                        authorId = "current_user",
                        authorName = "Bạn",
                        authorAvatarUrl = null,
                        timeAgo = "20 phút trước",
                        content = "Mình dùng template Film 35mm của Lokap lúc đăng á!",
                        score = 5,
                        userVote = 0,
                        replyToAuthorName = "Phương Thảo"
                    )
                )
            )
        ),
        "p_02" to listOf(
            RedditCommentUi(
                id = "c1_p_02",
                authorId = "user_nam",
                authorName = "Lê Hoàng Nam",
                authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d",
                timeAgo = "25 phút trước",
                content = "Ghé quán nước Mót chưa b ơi?",
                score = 4,
                userVote = 0
            ),
            RedditCommentUi(
                id = "c2_p_02",
                authorId = "user_linh",
                authorName = "Trần Khánh Linh",
                authorAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9",
                timeAgo = "18 phút trước",
                content = "Đi tầm này mát mẻ, đỡ đông hơn hẳn nè ✨",
                score = 7,
                userVote = 0
            )
        ),
        "p_03" to listOf(
            RedditCommentUi(
                id = "c1_p_03",
                authorId = "user_thảo",
                authorName = "Phương Thảo",
                authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
                timeAgo = "1 giờ trước",
                content = "Góc này thơ quá 🏮",
                score = 8,
                userVote = 0
            ),
            RedditCommentUi(
                id = "c2_p_03",
                authorId = "user_khôi",
                authorName = "Nguyễn Minh Khôi",
                authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                timeAgo = "40 phút trước",
                content = "Hội An lúc nào cũng yên bình như vậy",
                score = 5,
                userVote = 0
            )
        ),
        "p_04" to listOf(
            RedditCommentUi(
                id = "c1_p_04",
                authorId = "user_ngọc",
                authorName = "Đỗ Bảo Ngọc",
                authorAvatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1",
                timeAgo = "2 giờ trước",
                content = "Chỉ chỗ bí mật cho tui với nha 🌿",
                score = 3,
                userVote = 0
            )
        )
    )

    private val _comments = MutableStateFlow(initialSampleComments)
    val comments: StateFlow<Map<String, List<RedditCommentUi>>> = _comments.asStateFlow()

    fun getComments(postId: String): List<RedditCommentUi> {
        return _comments.value[postId] ?: emptyList()
    }

    fun getTotalCount(postId: String): Int {
        val rootList = getComments(postId)
        return countRecursive(rootList)
    }

    fun observeTotalCount(postId: String): Flow<Int> {
        return _comments.map { map ->
            val list = map[postId] ?: emptyList()
            countRecursive(list)
        }
    }

    fun observeAllCounts(): Flow<Map<String, Int>> {
        return _comments.map { map ->
            map.mapValues { countRecursive(it.value) }
        }
    }

    fun addComment(
        postId: String,
        content: String,
        replyTargetId: String? = null,
        replyToAuthorName: String? = null
    ) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        val newComment = RedditCommentUi(
            id = "c_${System.currentTimeMillis()}_$postId",
            authorId = SampleData.currentUser.id,
            authorName = "Bạn",
            authorAvatarUrl = null,
            timeAgo = "Vừa xong",
            content = trimmed,
            score = 1,
            userVote = 1,
            replyToAuthorName = replyToAuthorName
        )

        _comments.update { currentMap ->
            val postComments = currentMap[postId] ?: emptyList()
            val updatedComments = if (replyTargetId != null) {
                addReplyRecursive(postComments, replyTargetId, newComment)
            } else {
                postComments + newComment
            }
            currentMap + (postId to updatedComments)
        }
    }

    fun voteComment(postId: String, commentId: String, isUpvote: Boolean) {
        _comments.update { currentMap ->
            val postComments = currentMap[postId] ?: emptyList()
            val updatedComments = updateVoteRecursive(postComments, commentId, isUpvote)
            currentMap + (postId to updatedComments)
        }
    }

    private fun countRecursive(list: List<RedditCommentUi>): Int {
        var count = 0
        for (item in list) {
            count += 1 + countRecursive(item.replies)
        }
        return count
    }

    private fun addReplyRecursive(
        list: List<RedditCommentUi>,
        targetId: String,
        newReply: RedditCommentUi
    ): List<RedditCommentUi> {
        return list.map { comment ->
            if (comment.id == targetId) {
                comment.copy(replies = comment.replies + newReply)
            } else {
                comment.copy(replies = addReplyRecursive(comment.replies, targetId, newReply))
            }
        }
    }

    private fun updateVoteRecursive(
        list: List<RedditCommentUi>,
        commentId: String,
        isUpvote: Boolean
    ): List<RedditCommentUi> {
        return list.map { comment ->
            if (comment.id == commentId) {
                val currentVote = comment.userVote
                val targetVote = if (isUpvote) 1 else -1
                val newVote = if (currentVote == targetVote) 0 else targetVote
                val delta = newVote - currentVote
                comment.copy(
                    userVote = newVote,
                    score = comment.score + delta
                )
            } else {
                comment.copy(replies = updateVoteRecursive(comment.replies, commentId, isUpvote))
            }
        }
    }
}
