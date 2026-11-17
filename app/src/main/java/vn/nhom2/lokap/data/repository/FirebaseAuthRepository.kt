package vn.nhom2.lokap.data.repository

import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import vn.nhom2.lokap.data.local.LocalCache
import vn.nhom2.lokap.data.local.SessionPrefs
import vn.nhom2.lokap.data.remote.CloudinaryUploader
import vn.nhom2.lokap.data.remote.Fs
import vn.nhom2.lokap.data.remote.toAuthException
import vn.nhom2.lokap.data.remote.toUser
import vn.nhom2.lokap.di.ApplicationScope
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.domain.repository.AuthStoreException
import vn.nhom2.lokap.domain.repository.ProfileDraft
import vn.nhom2.lokap.domain.repository.RegisterResult

/**
 * Đăng nhập/đăng ký bằng Firebase Authentication (email + mật khẩu, xác nhận email bằng link; hoặc Google),
 * hồ sơ ở Firestore `users/{uid}` + `usernames/{username}`. Firebase tự lưu phiên nên mở lại app vẫn còn đăng nhập.
 * Email chưa xác nhận thì coi như chưa đăng nhập (Security Rules cũng chặn ghi khi email_verified = false).
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuthLazy: dagger.Lazy<FirebaseAuth>,
    private val dbLazy: dagger.Lazy<FirebaseFirestore>,
    private val prefs: SessionPrefs,
    private val localCache: LocalCache,
    private val cloudinary: CloudinaryUploader,
    private val credentialManager: CredentialManager,
    @ApplicationScope private val appScope: CoroutineScope
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth get() = firebaseAuthLazy.get()
    private val db: FirebaseFirestore get() = dbLazy.get()

    private val _session = MutableStateFlow<AuthSession>(AuthSession.Loading)
    override val session: StateFlow<AuthSession> = _session.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.onboardingDone)
    override val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    /** Listener của Firebase và login() cùng tải hồ sơ: khoá để không chạy chồng nhau. */
    private val sessionLock = Mutex()

    init {
        firebaseAuth.addAuthStateListener { fa ->
            val user = fa.currentUser
            appScope.launch { sessionLock.withLock { onAuthState(user) } }
        }
    }

    private suspend fun onAuthState(user: FirebaseUser?) {
        when {
            user == null || !user.isEmailVerified -> {
                if (_session.value !is AuthSession.SignedOut) setSignedOut()
            }
            else -> {
                val current = _session.value
                val known = (current is AuthSession.SignedIn && current.user.id == user.uid) ||
                    (current is AuthSession.NeedsProfile && current.userId == user.uid)
                if (!known) resolveProfile(user.uid, user.email)
            }
        }
    }

    override fun markOnboardingDone() {
        prefs.onboardingDone = true
        _onboardingDone.value = true
    }

    override suspend fun login(email: String, password: String): Result<User> = authCall {
        val user = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: throw AuthStoreException.Unknown("Không đăng nhập được")
        // Chưa bấm link xác nhận: vẫn giữ đăng nhập để màn Xác nhận email gửi lại link được
        if (!user.isEmailVerified) throw AuthStoreException.EmailNotConfirmed(email.trim())
        loadSignedInUser(user)
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> = authCall {
        // Không gửi nonce nên tham số accessToken để null; Firebase tự kiểm tra chữ ký và hạn của ID token
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = try {
            firebaseAuth.signInWithCredential(credential).await().user
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Không phải sai mật khẩu: token hết hạn, hoặc cấp cho Web client ID của project khác
            Log.w(TAG, "Firebase từ chối ID token Google", e)
            throw AuthStoreException.Unknown("Google từ chối phiên đăng nhập. Thử lại sau.")
        } ?: throw AuthStoreException.Unknown("Không đăng nhập được bằng Google")
        // Email Google đã được Google xác nhận nên isEmailVerified = true, không qua màn Xác nhận email
        loadSignedInUser(user)
    }

    /** Đã đăng nhập Firebase: có hồ sơ → SignedIn, chưa có → NeedsProfile (User trả về có username rỗng). */
    private suspend fun loadSignedInUser(user: FirebaseUser): User = sessionLock.withLock {
        val profile = fetchProfile(user.uid)
        if (profile != null) {
            profile.also(::setSignedIn)
        } else {
            _currentUser.value = null
            _session.value = AuthSession.NeedsProfile(user.uid, user.email)
            User(id = user.uid, username = "", displayName = "", avatarUrl = null)
        }
    }

    override suspend fun register(email: String, password: String): Result<RegisterResult> = authCall {
        val user = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: throw AuthStoreException.Unknown("Không đăng ký được")
        // Gửi link lỗi (vd quá nhiều lần) thì màn Xác nhận email có nút gửi lại
        runCatching { user.sendEmailVerification().await() }
        RegisterResult.NeedsEmailVerification(email.trim())
    }

    override suspend fun confirmEmailVerified(): Result<Unit> = authCall {
        val user = firebaseAuth.currentUser
            ?: throw AppException("Phiên đăng ký đã hết, hãy đăng nhập lại", AppException.Reason.NOT_SIGNED_IN)
        user.reload().await()
        val fresh = firebaseAuth.currentUser ?: user
        if (!fresh.isEmailVerified) throw AuthStoreException.EmailNotConfirmed(fresh.email.orEmpty())
        // Làm mới token để Security Rules thấy email_verified = true
        fresh.getIdToken(true).await()
        sessionLock.withLock { resolveProfile(fresh.uid, fresh.email) }
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> = authCall {
        val user = firebaseAuth.currentUser
            ?: throw AppException("Phiên đăng ký đã hết, hãy đăng nhập lại", AppException.Reason.NOT_SIGNED_IN)
        user.sendEmailVerification().await()
    }

    // Project Firebase mới bật "Email enumeration protection" nên app không hỏi được email đã có ai dùng chưa
    override suspend fun isEmailAvailable(email: String): Result<Boolean> =
        Result.failure(AuthStoreException.Unsupported("Firebase không cho kiểm tra trước email"))

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = authCall {
        val snap = db.collection(Fs.USERNAMES).document(username.trim().lowercase()).get().await()
        !snap.exists() || snap.getString(Fs.UID) == firebaseAuth.currentUser?.uid
    }

    override suspend fun completeProfile(draft: ProfileDraft): Result<User> = authCall {
        val uid = requireUid()
        val username = draft.username.trim().lowercase()
        val displayName = draft.displayName.trim()
        val avatarUrl = uploadAvatarIfLocal(draft.avatarUrl, uid)
        val userRef = db.collection(Fs.USERS).document(uid)
        val usernameRef = db.collection(Fs.USERNAMES).document(username)
        db.runTransaction { tx ->
            val taken = tx.get(usernameRef)
            if (taken.exists() && taken.getString(Fs.UID) != uid) return@runTransaction false
            val existing = tx.get(userRef)
            val publicProfile = mutableMapOf<String, Any?>(
                Fs.USERNAME to username,
                Fs.DISPLAY_NAME to displayName,
                Fs.SEARCH_NAME to Fs.searchKey(displayName),
                Fs.AVATAR_URL to avatarUrl,
                Fs.BIO to draft.bio.trim()
            )
            if (!existing.exists()) {
                publicProfile[Fs.FRIEND_COUNT] = 0
                publicProfile[Fs.CREATED_AT] = FieldValue.serverTimestamp()
            } else {
                existing.getString(Fs.USERNAME)?.takeIf { it != username }?.let { old ->
                    tx.delete(db.collection(Fs.USERNAMES).document(old))
                }
            }
            tx.set(usernameRef, mapOf(Fs.UID to uid))
            tx.set(userRef, publicProfile, SetOptions.merge())
            tx.set(
                userRef.collection(Fs.PRIVATE).document(Fs.PROFILE),
                mapOf(
                    Fs.EMAIL to firebaseAuth.currentUser?.email,
                    Fs.BIRTH_YEAR to draft.birthYear,
                    Fs.HOME_PROVINCE to draft.homeProvinceCode,
                    Fs.DEFAULT_LEVEL to LocationLevel.PROVINCE.name
                ),
                SetOptions.merge()
            )
            true
        }.await().let { ok -> if (!ok) throw AuthStoreException.UsernameAlreadyTaken() }

        User(id = uid, username = username, displayName = displayName, avatarUrl = avatarUrl, bio = draft.bio.trim())
            .also(::setSignedIn)
    }

    override suspend fun updateProfile(displayName: String, username: String, bio: String): Result<User> = authCall {
        val uid = requireUid()
        val newUsername = username.trim().removePrefix("@").lowercase()
        val userRef = db.collection(Fs.USERS).document(uid)
        val newUsernameRef = db.collection(Fs.USERNAMES).document(newUsername)
        val ok = db.runTransaction { tx ->
            val current = tx.get(userRef)
            val oldUsername = current.getString(Fs.USERNAME)
            if (oldUsername != newUsername) {
                val taken = tx.get(newUsernameRef)
                if (taken.exists() && taken.getString(Fs.UID) != uid) return@runTransaction false
                oldUsername?.let { tx.delete(db.collection(Fs.USERNAMES).document(it)) }
                tx.set(newUsernameRef, mapOf(Fs.UID to uid))
            }
            tx.update(
                userRef,
                mapOf(
                    Fs.USERNAME to newUsername,
                    Fs.DISPLAY_NAME to displayName.trim(),
                    Fs.SEARCH_NAME to Fs.searchKey(displayName),
                    Fs.BIO to bio.trim()
                )
            )
            true
        }.await()
        if (!ok) throw AuthStoreException.UsernameAlreadyTaken()
        val updated = (_currentUser.value ?: User(uid, newUsername, displayName.trim(), null))
            .copy(username = newUsername, displayName = displayName.trim(), bio = bio.trim())
        updated.also(::setSignedIn)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = authCall {
        if (currentPassword.isBlank()) {
            throw AuthStoreException.Unknown("Vui lòng nhập mật khẩu hiện tại")
        }
        val user = firebaseAuth.currentUser ?: throw AuthStoreException.RecentLoginRequired()
        if (user.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
            throw AuthStoreException.Unsupported("Tài khoản này không dùng mật khẩu. Hãy quản lý đăng nhập qua nhà cung cấp của bạn.")
        }
        val email = user.email?.takeIf { it.isNotBlank() } ?: throw AuthStoreException.RecentLoginRequired()
        // Luôn kiểm tra mật khẩu đã nhập, kể cả khi phiên Firebase vẫn được xem là vừa đăng nhập.
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        user.updatePassword(newPassword).await()
        Unit
    }

    override suspend fun resetPassword(email: String): Result<Unit> = authCall {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override suspend fun logout() {
        runCatching { firebaseAuth.signOut() }
        clearGoogleCredentialState()
        sessionLock.withLock { setSignedOut() }
    }

    override suspend fun deleteAccount(): Result<Unit> = authCall {
        val user = firebaseAuth.currentUser ?: throw AuthStoreException.RecentLoginRequired()
        // Firebase chỉ cho xoá tài khoản vừa đăng nhập gần đây: kiểm tra TRƯỚC khi xoá dữ liệu
        val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
        if (System.currentTimeMillis() - lastSignIn > RECENT_LOGIN.inWholeMilliseconds) {
            throw AuthStoreException.RecentLoginRequired()
        }
        deleteFirestoreData(user.uid)
        user.delete().await()
        clearGoogleCredentialState()
        sessionLock.withLock { setSignedOut() }
    }

    // ------------------------------------------------------------------

    /**
     * Báo Credential Manager quên phiên Google của app, lần sau bấm "Tiếp tục với Google" sẽ hỏi chọn tài khoản lại.
     * Tài khoản email cũng gọi được (không có gì để xoá). Máy không có Google Play services thì ném lỗi → bỏ qua.
     */
    private suspend fun clearGoogleCredentialState() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Không xoá được phiên Google trong Credential Manager", e)
        }
    }

    /** Xoá bài (kèm toạ độ gốc), quan hệ bạn bè, danh sách chặn, username và hồ sơ. Ảnh Cloudinary còn lại. */
    private suspend fun deleteFirestoreData(uid: String) {
        val ops = mutableListOf<com.google.firebase.firestore.DocumentReference>()
        db.collection(Fs.POSTS).whereEqualTo(Fs.AUTHOR_ID, uid).get().await().documents.forEach { post ->
            ops += post.reference.collection(Fs.PRIVATE).document(Fs.LOCATION)
            ops += post.reference
        }
        ops += db.collection(Fs.FRIENDSHIPS).whereArrayContains(Fs.PAIR_USERS, uid).get().await().documents.map { it.reference }
        ops += db.collection(Fs.BLOCKS).whereEqualTo(Fs.BLOCKER, uid).get().await().documents.map { it.reference }
        val userRef = db.collection(Fs.USERS).document(uid)
        userRef.get().await().getString(Fs.USERNAME)?.let { ops += db.collection(Fs.USERNAMES).document(it) }
        ops += userRef.collection(Fs.PRIVATE).document(Fs.PROFILE)
        ops += userRef
        ops.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach(batch::delete)
            batch.commit().await()
        }
    }

    /** Ảnh đại diện chọn từ máy thì đưa lên Cloudinary; ảnh đã có URL (https) giữ nguyên. */
    private suspend fun uploadAvatarIfLocal(avatar: String?, uid: String): String? {
        if (avatar.isNullOrBlank() || avatar.startsWith("http")) return avatar
        val file = File(avatar.removePrefix("file://"))
        if (!file.exists()) return null
        return cloudinary.uploadAvatar(file, uid).url
    }

    private suspend fun fetchProfile(uid: String): User? =
        db.collection(Fs.USERS).document(uid).get().await().takeIf(DocumentSnapshot::exists)?.toUser()

    /** Có phiên rồi: tải hồ sơ để biết vào Camera hay Thiết lập hồ sơ. Gọi khi đang giữ [sessionLock]. */
    private suspend fun resolveProfile(uid: String, email: String?) {
        runCatching { withTimeout(8_000) { fetchProfile(uid) } }.fold(
            onSuccess = { user ->
                if (user != null) {
                    setSignedIn(user)
                } else {
                    _currentUser.value = null
                    _session.value = AuthSession.NeedsProfile(uid, email)
                }
            },
            onFailure = {
                // Mất mạng lúc mở app: dùng hồ sơ đã lưu để vẫn vào được app
                val cached = prefs.cachedProfile(uid)
                if (cached != null) setSignedIn(cached) else _session.value = AuthSession.NeedsProfile(uid, email)
            }
        )
    }

    private fun setSignedIn(user: User) {
        prefs.saveProfile(user)
        _currentUser.value = user
        _session.value = AuthSession.SignedIn(user)
    }

    private suspend fun setSignedOut() {
        prefs.clearProfile()
        _currentUser.value = null
        _session.value = AuthSession.SignedOut
        localCache.clear()
    }

    private fun requireUid(): String =
        firebaseAuth.currentUser?.uid
            ?: throw AppException("Bạn cần đăng nhập lại", AppException.Reason.NOT_SIGNED_IN)

    private inline fun <T> authCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAuthException())
    }

    private companion object {
        const val TAG = "FirebaseAuthRepository"
        val RECENT_LOGIN = 5.minutes
    }
}
