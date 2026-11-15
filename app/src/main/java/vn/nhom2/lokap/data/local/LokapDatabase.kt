package vn.nhom2.lokap.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Cache bài đăng trên máy (Room · SQLite): feed mở ra là có ngay, bài chờ gửi không mất khi tắt app.
 * Firestore vẫn là nguồn chính: mỗi lần làm mới sẽ ghi đè các bài đã gửi (SENT).
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String?,
    /** public_id của ảnh trên Cloudinary (rỗng khi chưa upload xong). */
    val imagePath: String,
    /** Ảnh trên máy (bài của mình): hiện ngay, không cần tải lại. */
    val localImagePath: String?,
    /** URL https của ảnh trên Cloudinary. */
    val remoteImageUrl: String?,
    val caption: String?,
    val audience: String,
    val locationLevel: String,
    val provinceCode: String?,
    val provinceName: String?,
    val wardCode: String?,
    val wardName: String?,
    val placeId: String?,
    val placeName: String?,
    val landmarkId: String?,
    val displayLat: Double?,
    val displayLng: Double?,
    val takenAt: Long,
    val publicVisibleAt: Long?,
    val createdAt: Long,
    val reactionsJson: String,
    val myReaction: String?,
    /** PENDING / SENT / FAILED (PostSendState) */
    val sendState: String,
    val inFriendFeed: Boolean,
    val inPublicFeed: Boolean,
    /** Tham số chỉ dùng lúc upload bài chờ gửi. */
    val pendingDelayDays: Int = 0,
    val pendingRecipientId: String? = null,
    /** Toạ độ gốc chờ ghi vào posts/{id}/private/location; xoá khỏi máy sau khi gửi xong. */
    val pendingExactLat: Double? = null,
    val pendingExactLng: Double? = null,
    val pendingAccuracyM: Float? = null
)

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE inFriendFeed = 1 ORDER BY createdAt DESC")
    fun observeFriendFeed(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE inPublicFeed = 1 ORDER BY publicVisibleAt DESC, createdAt DESC")
    fun observePublicFeed(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    fun observeById(id: String): Flow<PostEntity?>

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun observeByAuthor(authorId: String): Flow<List<PostEntity>>

    /** Ảnh của một địa danh (chỉ bài public mới gắn được địa danh). */
    @Query("SELECT * FROM posts WHERE landmarkId = :landmarkId AND audience = 'PUBLIC' ORDER BY createdAt DESC")
    fun observeByLandmark(landmarkId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: String): PostEntity?

    @Query("SELECT * FROM posts WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<PostEntity>

    @Query("SELECT * FROM posts WHERE sendState = 'PENDING'")
    suspend fun getPending(): List<PostEntity>

    @Upsert
    suspend fun upsert(item: PostEntity)

    @Upsert
    suspend fun upsertAll(items: List<PostEntity>)

    @Query("UPDATE posts SET sendState = :state WHERE id = :id")
    suspend fun updateSendState(id: String, state: String)

    /** Bài đã gửi không còn trong feed Bạn bè của server thì bỏ cờ (giữ bài đang gửi của mình). */
    @Query("UPDATE posts SET inFriendFeed = 0 WHERE sendState = 'SENT' AND id NOT IN (:keepIds)")
    suspend fun clearFriendFlagExcept(keepIds: List<String>)

    @Query("UPDATE posts SET inPublicFeed = 0 WHERE id NOT IN (:keepIds)")
    suspend fun clearPublicFlagExcept(keepIds: List<String>)

    /** Bài đã bị xoá/ẩn trên server thì bỏ khỏi máy (bài riêng gửi mình không hiện ở hồ sơ nên giữ lại). */
    @Query(
        "DELETE FROM posts WHERE sendState = 'SENT' AND authorId = :authorId " +
            "AND audience != 'DIRECT' AND id NOT IN (:keepIds)"
    )
    suspend fun deleteStaleByAuthor(authorId: String, keepIds: List<String>)

    /**
     * Bài của người khác không còn gắn địa danh trên server (bị gỡ, xoá, ẩn) thì bỏ khỏi máy.
     * Giữ bài của mình và bài đang nằm trong feed (feed tự làm mới các bài đó).
     */
    @Query(
        "DELETE FROM posts WHERE landmarkId = :landmarkId AND sendState = 'SENT' AND authorId != :me " +
            "AND inFriendFeed = 0 AND inPublicFeed = 0 AND id NOT IN (:keepIds)"
    )
    suspend fun deleteStaleByLandmark(landmarkId: String, me: String, keepIds: List<String>)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM posts WHERE authorId = :authorId")
    suspend fun deleteByAuthor(authorId: String)

    @Query("DELETE FROM posts")
    suspend fun clearAll()
}

// v2: chuyển sang Firestore + Cloudinary (đổi cột ảnh, thêm toạ độ chờ gửi). Cache nên xoá bảng cũ là đủ.
@Database(entities = [PostEntity::class], version = 2, exportSchema = false)
abstract class LokapDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
