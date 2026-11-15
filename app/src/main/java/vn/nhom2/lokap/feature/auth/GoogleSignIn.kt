package vn.nhom2.lokap.feature.auth

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import vn.nhom2.lokap.BuildConfig

/**
 * Mở hộp chọn tài khoản Google của hệ thống (Credential Manager) và lấy ID token cho Firebase.
 * Hộp thoại cần Context của Activity nên lớp này nằm ở tầng giao diện, không nằm trong ViewModel/Repository.
 *
 * [webClientId] là Web client ID (client_type 3 trong google-services.json), không phải Android client ID:
 * Google cấp ID token cho client này, Firebase kiểm tra token khớp project.
 */
class GoogleSignInLauncher(
    private val activityContext: Context,
    private val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID
) {
    private val credentialManager = CredentialManager.create(activityContext)

    suspend fun launch(): GoogleSignInResult {
        if (webClientId.isBlank()) return GoogleSignInResult.Failure("Đăng nhập Google chưa được cấu hình")

        // Nút "Tiếp tục với Google": luôn hiện danh sách tài khoản, cho thêm tài khoản mới ngay trong hộp thoại
        val option = GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = credentialManager.getCredential(activityContext, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Log.w(TAG, "Loại credential không hỗ trợ: ${credential.type}")
                GoogleSignInResult.Failure(GENERIC_ERROR)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialInterruptedException) {
            GoogleSignInResult.Failure("Bị gián đoạn, thử lại nhé")
        } catch (e: NoCredentialException) {
            // Hay gặp khi máy chưa có tài khoản Google, hoặc SHA-1 của máy build chưa thêm vào Firebase
            Log.w(TAG, "Không có tài khoản Google phù hợp", e)
            GoogleSignInResult.Failure("Không tìm thấy tài khoản Google trên máy")
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.w(TAG, "Thiếu credentials-play-services-auth hoặc Google Play services", e)
            GoogleSignInResult.Failure(UNSUPPORTED_ERROR)
        } catch (e: GetCredentialUnsupportedException) {
            Log.w(TAG, "Máy không hỗ trợ Credential Manager", e)
            GoogleSignInResult.Failure(UNSUPPORTED_ERROR)
        } catch (e: GetCredentialException) {
            // vd "[28444] Developer console is not set up correctly": SHA-1 / Web client ID chưa đúng
            Log.w(TAG, "Credential Manager lỗi: ${e.type}", e)
            GoogleSignInResult.Failure(GENERIC_ERROR)
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "ID token Google không đọc được", e)
            GoogleSignInResult.Failure(GENERIC_ERROR)
        }
    }

    private companion object {
        const val TAG = "GoogleSignIn"
        const val GENERIC_ERROR = "Không đăng nhập được bằng Google. Thử lại sau."
        const val UNSUPPORTED_ERROR = "Máy này chưa hỗ trợ đăng nhập Google"
    }
}

@Composable
fun rememberGoogleSignInLauncher(): GoogleSignInLauncher {
    val context = LocalActivity.current ?: LocalContext.current
    return remember(context) { GoogleSignInLauncher(context) }
}
