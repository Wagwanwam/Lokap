package vn.nhom2.lokap.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vn.nhom2.lokap.data.remote.Fs
import vn.nhom2.lokap.data.remote.toAppException
import vn.nhom2.lokap.di.ApplicationScope
import vn.nhom2.lokap.domain.model.AppSettingsData
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.domain.repository.SettingsRepository

/** Mức vị trí mặc định + công tắc thông báo ở `users/{uid}/private/profile` (chỉ chủ đọc/ghi). */
@Singleton
class FirestoreSettingsRepository @Inject constructor(
    private val dbLazy: dagger.Lazy<FirebaseFirestore>,
    private val auth: AuthRepository,
    @ApplicationScope appScope: CoroutineScope
) : SettingsRepository {

    private val db: FirebaseFirestore get() = dbLazy.get()

    private val _settings = MutableStateFlow(AppSettingsData())
    override val settings: StateFlow<AppSettingsData> = _settings.asStateFlow()

    init {
        appScope.launch {
            auth.session
                .map { (it as? AuthSession.SignedIn)?.user?.id }
                .distinctUntilChanged()
                .collect { uid -> if (uid == null) _settings.value = AppSettingsData() else refresh() }
        }
    }

    override suspend fun refresh(): Result<Unit> = appCall {
        val doc = privateProfile().get().await()
        _settings.value = AppSettingsData(
            defaultLocationLevel = doc.getString(Fs.DEFAULT_LEVEL)
                ?.let { runCatching { LocationLevel.valueOf(it.uppercase()) }.getOrNull() }
                ?: LocationLevel.PROVINCE,
            notifyNewPhotos = doc.getBoolean(Fs.NOTIFY_NEW_PHOTOS) ?: true,
            notifyReactions = doc.getBoolean(Fs.NOTIFY_REACTIONS) ?: true,
            notifyMessages = doc.getBoolean(Fs.NOTIFY_MESSAGES) ?: true,
            notifyFriendRequests = doc.getBoolean(Fs.NOTIFY_FRIEND_REQUESTS) ?: true
        )
    }

    override suspend fun update(transform: (AppSettingsData) -> AppSettingsData): Result<Unit> {
        val before = _settings.value
        val after = transform(before)
        if (after == before) return Result.success(Unit)
        _settings.value = after
        return appCall {
            privateProfile().set(
                mapOf(
                    Fs.DEFAULT_LEVEL to after.defaultLocationLevel.name,
                    Fs.NOTIFY_NEW_PHOTOS to after.notifyNewPhotos,
                    Fs.NOTIFY_REACTIONS to after.notifyReactions,
                    Fs.NOTIFY_MESSAGES to after.notifyMessages,
                    Fs.NOTIFY_FRIEND_REQUESTS to after.notifyFriendRequests
                ),
                SetOptions.merge()
            ).await()
            Unit
        }.onFailure { _settings.value = before }
    }

    private fun privateProfile() = db.collection(Fs.USERS).document(requireMe())
        .collection(Fs.PRIVATE).document(Fs.PROFILE)

    private fun requireMe(): String = auth.currentUser.value?.id
        ?: throw AppException("Bạn cần đăng nhập lại", AppException.Reason.NOT_SIGNED_IN)

    private inline fun <T> appCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAppException())
    }
}
