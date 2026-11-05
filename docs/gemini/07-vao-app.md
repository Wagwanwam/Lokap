```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/FakeAuthStore.kt
package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import vn.nhom2.lokap.core.model.SampleData
import vn.nhom2.lokap.core.model.User
import java.io.IOException
import java.util.UUID

sealed class AuthStoreException(override val message: String) : Exception(message) {
    class InvalidCredentials : AuthStoreException("Email hoặc mật khẩu không đúng")
    class EmailAlreadyTaken : AuthStoreException("Email này đã được đăng ký tài khoản khác")
    class UsernameAlreadyTaken : AuthStoreException("Username này đã có người dùng")
    class NetworkError : AuthStoreException("Mất kết nối mạng. Vui lòng kiểm tra và thử lại.")
}

object FakeAuthStore {
    val currentUser = MutableStateFlow<User?>(null)
    val onboardingDone = MutableStateFlow(false)

    var simulateOffline: Boolean = false

    private val registeredCredentials = mutableMapOf(
        "demo@lokap.vn" to "lokap123"
    )

    private val takenEmails = mutableSetOf(
        "demo@lokap.vn",
        "taken@lokap.vn"
    )

    private val takenUsernames = mutableSetOf(
        "lokap",
        SampleData.currentUser.username.lowercase()
    ).apply {
        SampleData.users.forEach { add(it.username.lowercase()) }
    }

    fun markOnboardingDone() {
        onboardingDone.value = true
    }

    suspend fun login(email: String, password: String): Result<User> {
        delay(800)
        if (simulateOffline || email.trim().equals("offline@lokap.vn", ignoreCase = true)) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val normalizedEmail = email.trim().lowercase()
        val expectedPassword = registeredCredentials[normalizedEmail]
        if (expectedPassword == null || expectedPassword != password) {
            return Result.failure(AuthStoreException.InvalidCredentials())
        }
        val user = if (normalizedEmail == "demo@lokap.vn") {
            SampleData.currentUser
        } else {
            User(
                id = UUID.randomUUID().toString(),
                username = normalizedEmail.substringBefore("@"),
                displayName = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                avatarUrl = "https://picsum.photos/seed/lokap_1/200/200"
            )
        }
        currentUser.value = user
        return Result.success(user)
    }

    suspend fun register(email: String, password: String): Result<User> {
        delay(800)
        if (simulateOffline || email.trim().equals("offline@lokap.vn", ignoreCase = true)) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail in takenEmails) {
            return Result.failure(AuthStoreException.EmailAlreadyTaken())
        }
        registeredCredentials[normalizedEmail] = password
        takenEmails.add(normalizedEmail)
        val incompleteUser = User(
            id = UUID.randomUUID().toString(),
            username = "",
            displayName = "",
            avatarUrl = null
        )
        currentUser.value = incompleteUser
        return Result.success(incompleteUser)
    }

    suspend fun loginWithGoogle(): Result<User> {
        delay(800)
        if (simulateOffline) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val user = SampleData.currentUser
        currentUser.value = user
        return Result.success(user)
    }

    suspend fun logout() {
        delay(800)
        currentUser.value = null
    }

    suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        delay(250)
        if (simulateOffline) {
            return Result.failure(IOException("Offline"))
        }
        val normalized = username.trim().lowercase()
        return Result.success(normalized !in takenUsernames)
    }

    suspend fun completeProfile(username: String, displayName: String): Result<User> {
        return completeProfile(username = username, displayName = displayName, avatarUrl = null)
    }

    suspend fun completeProfile(
        username: String,
        displayName: String,
        avatarUrl: String?
    ): Result<User> {
        delay(800)
        if (simulateOffline) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val normalizedUsername = username.trim().lowercase()
        if (normalizedUsername in takenUsernames) {
            return Result.failure(AuthStoreException.UsernameAlreadyTaken())
        }
        takenUsernames.add(normalizedUsername)
        val existing = currentUser.value
        val updated = User(
            id = existing?.id ?: UUID.randomUUID().toString(),
            username = normalizedUsername,
            displayName = displayName.trim(),
            avatarUrl = avatarUrl ?: existing?.avatarUrl
        )
        currentUser.value = updated
        return Result.success(updated)
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        delay(800)
        if (simulateOffline || email.trim().equals("offline@lokap.vn", ignoreCase = true)) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        return Result.success(Unit)
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/onboarding/OnboardingContract.kt
package vn.nhom2.lokap.feature.onboarding

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

enum class PermissionStatus {
    NOT_GRANTED,
    GRANTED,
    DENIED
}

enum class LocationAccuracyState {
    NONE,
    COARSE_ONLY,
    FINE
}

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val cameraStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val locationStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val locationAccuracy: LocationAccuracyState = LocationAccuracyState.NONE,
    val notificationStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val isSubmitting: Boolean = false
) {
    val canStart: Boolean
        get() = cameraStatus == PermissionStatus.GRANTED && !isSubmitting
}

sealed interface OnboardingAction {
    data class PageChanged(val page: Int) : OnboardingAction
    data object NextPageClicked : OnboardingAction
    data object SkipToPermissionsClicked : OnboardingAction
    data class SyncPermissionState(
        val cameraGranted: Boolean,
        val fineLocationGranted: Boolean,
        val coarseLocationGranted: Boolean,
        val notificationGranted: Boolean
    ) : OnboardingAction
    data class CameraPermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data class LocationPermissionResult(
        val fineGranted: Boolean,
        val coarseGranted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data class NotificationPermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data object StartClicked : OnboardingAction
}

sealed interface OnboardingEffect {
    data object NavigateToLogin : OnboardingEffect
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/onboarding/OnboardingViewModel.kt
package vn.nhom2.lokap.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakeAuthStore

class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.PageChanged -> {
                _uiState.update { it.copy(currentPage = action.page.coerceIn(0, it.totalPages - 1)) }
            }

            OnboardingAction.NextPageClicked -> {
                _uiState.update { state ->
                    val next = (state.currentPage + 1).coerceAtMost(state.totalPages - 1)
                    state.copy(currentPage = next)
                }
            }

            OnboardingAction.SkipToPermissionsClicked -> {
                _uiState.update { state ->
                    state.copy(currentPage = state.totalPages - 1)
                }
            }

            is OnboardingAction.SyncPermissionState -> {
                _uiState.update { state ->
                    val hasAnyLocation = action.fineLocationGranted || action.coarseLocationGranted
                    val accuracy = when {
                        action.fineLocationGranted -> LocationAccuracyState.FINE
                        action.coarseLocationGranted -> LocationAccuracyState.COARSE_ONLY
                        else -> LocationAccuracyState.NONE
                    }
                    state.copy(
                        cameraStatus = if (action.cameraGranted) PermissionStatus.GRANTED else state.cameraStatus,
                        locationStatus = if (hasAnyLocation) PermissionStatus.GRANTED else state.locationStatus,
                        locationAccuracy = accuracy,
                        notificationStatus = if (action.notificationGranted) PermissionStatus.GRANTED else state.notificationStatus
                    )
                }
            }

            is OnboardingAction.CameraPermissionResult -> {
                _uiState.update { state ->
                    state.copy(
                        cameraStatus = if (action.granted) {
                            PermissionStatus.GRANTED
                        } else {
                            PermissionStatus.DENIED
                        }
                    )
                }
            }

            is OnboardingAction.LocationPermissionResult -> {
                _uiState.update { state ->
                    val anyGranted = action.fineGranted || action.coarseGranted
                    val accuracy = when {
                        action.fineGranted -> LocationAccuracyState.FINE
                        action.coarseGranted -> LocationAccuracyState.COARSE_ONLY
                        else -> LocationAccuracyState.NONE
                    }
                    state.copy(
                        locationStatus = if (anyGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                        locationAccuracy = accuracy
                    )
                }
            }

            is OnboardingAction.NotificationPermissionResult -> {
                _uiState.update { state ->
                    state.copy(
                        notificationStatus = if (action.granted) {
                            PermissionStatus.GRANTED
                        } else {
                            PermissionStatus.DENIED
                        }
                    )
                }
            }

            OnboardingAction.StartClicked -> {
                val current = _uiState.value
                if (!current.canStart) return
                _uiState.update { it.copy(isSubmitting = true) }
                FakeAuthStore.markOnboardingDone()
                viewModelScope.launch {
                    _effect.send(OnboardingEffect.NavigateToLogin)
                }
            }
        }
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/onboarding/OnboardingScreen.kt
package vn.nhom2.lokap.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.feature.auth.LoginRoute

fun NavGraphBuilder.onboardingScreen(navController: NavController) {
    composable<OnboardingRoute> {
        OnboardingRoute(
            onNavigateToLogin = {
                navController.navigate(LoginRoute) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun OnboardingRoute(
    onNavigateToLogin: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun syncPermissions() {
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.onAction(
            OnboardingAction.SyncPermissionState(
                cameraGranted = cameraGranted,
                fineLocationGranted = fineGranted,
                coarseLocationGranted = coarseGranted,
                notificationGranted = notificationGranted
            )
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val activity = context.findActivity()

        if (results.containsKey(Manifest.permission.CAMERA)) {
            val granted = results[Manifest.permission.CAMERA] == true
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            viewModel.onAction(OnboardingAction.CameraPermissionResult(granted, rationale))
        }

        if (results.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) ||
            results.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } ?: false
            viewModel.onAction(OnboardingAction.LocationPermissionResult(fine, coarse, rationale))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            results.containsKey(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            val granted = results[Manifest.permission.POST_NOTIFICATIONS] == true
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } ?: false
            viewModel.onAction(OnboardingAction.NotificationPermissionResult(granted, rationale))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onRequestCamera = {
            permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        },
        onRequestLocation = {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onRequestNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            } else {
                viewModel.onAction(
                    OnboardingAction.NotificationPermissionResult(
                        granted = true,
                        shouldShowRationale = false
                    )
                )
            }
        },
        onOpenAppSettings = {
            context.openAppSettings()
        }
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { uiState.totalPages }
    )

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != uiState.currentPage) {
            onAction(OnboardingAction.PageChanged(pagerState.settledPage))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md)
    ) {
        // Top bar with "Bỏ qua" on first 2 pages (indices 0 and 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassChip(
                label = "LOKAP ✨",
                selected = true
            )
            if (uiState.currentPage < 2) {
                TextButton(
                    onClick = { onAction(OnboardingAction.SkipToPermissionsClicked) }
                ) {
                    Text(
                        text = "Bỏ qua",
                        style = MaterialTheme.typography.labelLarge,
                        color = LokapTheme.colors.textSecondary
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> IntroSlidePage(
                    badge = "WIDGET TỨC THÌ",
                    title = "Chụp & gửi tức thì cho bạn thân",
                    subtitle = "Mở app là thấy camera. Chụp 1 chạm, ảnh bay thẳng lên màn hình chính của hội bạn thân.",
                    illustration = { InstantSnapIllustration() }
                )

                1 -> IntroSlidePage(
                    badge = "BẢN ĐỒ SỐNG",
                    title = "Ghim khoảnh khắc lên bản đồ Việt Nam",
                    subtitle = "Khám phá ảnh public gom theo từng Tỉnh/Thành → Phường/Xã khắp 3 miền.",
                    illustration = { VietnamMapIllustration() }
                )

                2 -> IntroSlidePage(
                    badge = "QUYỀN RIÊNG TƯ",
                    title = "Bạn chọn ai thấy, thấy tới đâu",
                    subtitle = "Tuỳ chỉnh mức hiển thị vị trí (Phường/Xã hoặc chỉ Tỉnh/Thành) và hẹn giờ tự xoá ảnh 24h.",
                    illustration = { PrivacyControlIllustration() }
                )

                else -> PermissionsPage(
                    uiState = uiState,
                    onRequestCamera = onRequestCamera,
                    onRequestLocation = onRequestLocation,
                    onRequestNotification = onRequestNotification,
                    onOpenAppSettings = onOpenAppSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        // Page indicator dots + bottom action button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageDotsIndicator(
                totalPages = uiState.totalPages,
                currentPage = uiState.currentPage
            )

            Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

            if (uiState.currentPage < uiState.totalPages - 1) {
                PinkButton(
                    text = if (uiState.currentPage == 2) "Thiết lập quyền" else "Tiếp theo",
                    onClick = { onAction(OnboardingAction.NextPageClicked) },
                    leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PinkButton(
                    text = "Bắt đầu",
                    onClick = { onAction(OnboardingAction.StartClicked) },
                    enabled = uiState.canStart,
                    loading = uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.cameraStatus != PermissionStatus.GRANTED) {
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Cần cấp quyền Camera để tiếp tục vào Lokap",
                        style = MaterialTheme.typography.bodySmall,
                        color = LokapTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDotsIndicator(
    totalPages: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                label = "dot_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) LokapTheme.colors.primary else LokapTheme.colors.surfaceHigh,
                label = "dot_color"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(LokapTheme.shapes.pill)
                    .background(color)
            )
        }
    }
}

@Composable
private fun IntroSlidePage(
    badge: String,
    title: String,
    subtitle: String,
    illustration: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            contentAlignment = Alignment.Center
        ) {
            illustration()
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        GlassChip(label = badge, selected = false)

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = LokapTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LokapTheme.spacing.sm)
        )
    }
}

@Composable
private fun InstantSnapIllustration() {
    val primaryColor = LokapTheme.colors.primary
    val lilacColor = LokapTheme.colors.accentLilac
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.14f),
                radius = size.minDimension * 0.48f
            )
            drawCircle(
                color = lilacColor.copy(alpha = 0.22f),
                radius = size.minDimension * 0.36f,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Back card tilted
        Box(
            modifier = Modifier
                .size(168.dp)
                .rotate(-8f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(regionGradient("79"))
                .border(2.dp, LokapTheme.colors.outline, LokapTheme.shapes.cameraFrame)
        )

        // Front Locket-style square camera frame
        Box(
            modifier = Modifier
                .size(176.dp)
                .rotate(4f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(regionGradient("01"))
                .border(3.dp, LokapTheme.colors.primary, LokapTheme.shapes.cameraFrame),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📸", fontSize = 54.sp)
                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                Box(
                    modifier = Modifier
                        .clip(LokapTheme.shapes.pill)
                        .background(LokapTheme.colors.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Gửi hội bestie ⚡",
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.textPrimary
                    )
                }
            }
        }

        // Floating emoji stickers
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.surfaceHigh)
                .border(1.dp, LokapTheme.colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💖", fontSize = 22.sp)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 12.dp, y = (-16).dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.surfaceHigh)
                .border(1.dp, LokapTheme.colors.accentLime, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔥", fontSize = 22.sp)
        }
    }
}

@Composable
private fun VietnamMapIllustration() {
    val cyan = LokapTheme.colors.accentCyan
    val primary = LokapTheme.colors.primary
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            drawCircle(
                color = cyan.copy(alpha = 0.2f),
                radius = size.minDimension * 0.45f,
                style = Stroke(width = 2.dp.toPx(), pathEffect = dashEffect)
            )
            drawLine(
                color = primary.copy(alpha = 0.5f),
                start = Offset(size.width * 0.28f, size.height * 0.25f),
                end = Offset(size.width * 0.55f, size.height * 0.5f),
                strokeWidth = 3.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = primary.copy(alpha = 0.5f),
                start = Offset(size.width * 0.55f, size.height * 0.5f),
                end = Offset(size.width * 0.38f, size.height * 0.78f),
                strokeWidth = 3.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // North card
        RegionClusterBubble(
            emoji = "☕",
            title = "Hà Nội · Hoàn Kiếm",
            provinceCode = "01",
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 18.dp)
        )

        // Central card
        RegionClusterBubble(
            emoji = "🌊",
            title = "Đà Nẵng · Hải Châu",
            provinceCode = "48",
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // South card
        RegionClusterBubble(
            emoji = "🛵",
            title = "TP.HCM · Bến Nghé",
            provinceCode = "79",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = (-12).dp)
        )
    }
}

@Composable
private fun RegionClusterBubble(
    emoji: String,
    title: String,
    provinceCode: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(LokapTheme.shapes.card)
            .background(regionGradient(provinceCode))
            .border(1.5.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.background.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "📍 +28 ảnh mới",
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun PrivacyControlIllustration() {
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(regionGradient("92"))
                .border(2.dp, LokapTheme.colors.primary, LokapTheme.shapes.card)
                .padding(LokapTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            Text(text = "🛡️⏱️", fontSize = 44.sp)

            Text(
                text = "Chế độ hiển thị thông minh",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LokapTheme.colors.textPrimary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
            ) {
                GlassChip(label = "👑 Bạn thân", selected = true)
                GlassChip(label = "🌏 Public Tỉnh/Xã", selected = false)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
            ) {
                GlassChip(label = "⏳ Tự huỷ 24h", selected = true)
                GlassChip(label = "📍 Ẩn toạ độ nhà", selected = false)
            }
        }
    }
}

@Composable
private fun PermissionsPage(
    uiState: OnboardingUiState,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = LokapTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
    ) {
        Text(
            text = "Cấp quyền để quẩy Lokap 🚀",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary
        )
        Text(
            text = "Lokap tôn trọng quyền riêng tư của bạn. Bạn luôn kiểm soát được từng quyền bên dưới.",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.textSecondary
        )

        // 1. Camera card (Required)
        PermissionItemCard(
            emoji = "📸",
            title = "Camera",
            badgeText = "Bắt buộc",
            badgeColor = LokapTheme.colors.primary,
            description = "Mở app là chụp ngay khoảnh khắc gửi hội bạn thân.",
            extraWarning = null,
            status = uiState.cameraStatus,
            onRequestPermission = onRequestCamera,
            onOpenSettings = onOpenAppSettings
        )

        // 2. Location card (Optional + COARSE note)
        val coarseNote = if (uiState.locationAccuracy == LocationAccuracyState.COARSE_ONLY) {
            "Chỉ đăng được ở mức Tỉnh trở lên"
        } else {
            null
        }
        PermissionItemCard(
            emoji = "📍",
            title = "Vị trí",
            badgeText = "Có thể bỏ qua",
            badgeColor = LokapTheme.colors.accentCyan,
            description = "Để gắn khu vực Tỉnh/Thành → Phường/Xã. Chỉ lấy vị trí lúc chụp, không theo dõi.",
            extraWarning = coarseNote,
            status = uiState.locationStatus,
            onRequestPermission = onRequestLocation,
            onOpenSettings = onOpenAppSettings
        )

        // 3. Notification card (Android 13+, Optional)
        PermissionItemCard(
            emoji = "🔔",
            title = "Thông báo",
            badgeText = "Có thể bỏ qua",
            badgeColor = LokapTheme.colors.accentLilac,
            description = "Báo ngay khi bạn thân vừa thả ảnh mới lên widget của bạn.",
            extraWarning = null,
            status = uiState.notificationStatus,
            onRequestPermission = onRequestNotification,
            onOpenSettings = onOpenAppSettings
        )
    }
}

@Composable
private fun PermissionItemCard(
    emoji: String,
    title: String,
    badgeText: String,
    badgeColor: Color,
    description: String,
    extraWarning: String?,
    status: PermissionStatus,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val borderColor = when (status) {
        PermissionStatus.GRANTED -> LokapTheme.colors.success
        PermissionStatus.DENIED -> LokapTheme.colors.error
        PermissionStatus.NOT_GRANTED -> LokapTheme.colors.outline
    }

    Surface(
        shape = LokapTheme.shapes.card,
        color = LokapTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, LokapTheme.shapes.card)
    ) {
        Column(
            modifier = Modifier.padding(LokapTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LokapTheme.colors.surfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LokapTheme.colors.textPrimary
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor
                        )
                    }
                }

                PermissionStatusBadge(status = status)
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LokapTheme.colors.textSecondary
            )

            if (extraWarning != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LokapTheme.shapes.pill)
                        .background(LokapTheme.colors.surfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LokapTheme.colors.accentYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = extraWarning,
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.accentYellow
                    )
                }
            }

            when (status) {
                PermissionStatus.NOT_GRANTED -> {
                    GhostButton(
                        text = "Cấp quyền $title",
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PermissionStatus.DENIED -> {
                    GhostButton(
                        text = "Mở Cài đặt",
                        onClick = onOpenSettings,
                        leadingIcon = Icons.Default.Settings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PermissionStatus.GRANTED -> Unit
            }
        }
    }
}

@Composable
private fun PermissionStatusBadge(status: PermissionStatus) {
    val (label, color) = when (status) {
        PermissionStatus.NOT_GRANTED -> "Chưa cấp" to LokapTheme.colors.textTertiary
        PermissionStatus.GRANTED -> "Đã cấp" to LokapTheme.colors.success
        PermissionStatus.DENIED -> "Bị từ chối" to LokapTheme.colors.error
    }

    Row(
        modifier = Modifier
            .clip(LokapTheme.shapes.pill)
            .background(LokapTheme.colors.surfaceHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (status == PermissionStatus.GRANTED) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

@Preview(showBackground = true, name = "Onboarding - Slide 1")
@Composable
private fun OnboardingSlide1Preview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(currentPage = 0),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - Slide 2 Map")
@Composable
private fun OnboardingSlide2Preview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(currentPage = 1),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - Permissions Mixed & Coarse")
@Composable
private fun OnboardingPermissionsPreview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentPage = 3,
                cameraStatus = PermissionStatus.GRANTED,
                locationStatus = PermissionStatus.GRANTED,
                locationAccuracy = LocationAccuracyState.COARSE_ONLY,
                notificationStatus = PermissionStatus.DENIED
            ),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/AuthValidators.kt
package vn.nhom2.lokap.feature.auth

object AuthValidators {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[a-z0-9._]{3,20}$")

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Email không được để trống"
            !EMAIL_REGEX.matches(trimmed) -> "Email sai định dạng (ví dụ: ban@lokap.vn)"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return when {
            password.isEmpty() -> "Mật khẩu không được để trống"
            password.length < 8 -> "Mật khẩu phải có ít nhất 8 ký tự"
            !hasLetter || !hasDigit -> "Mật khẩu phải bao gồm cả chữ và số"
            else -> null
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Vui lòng nhập lại mật khẩu"
            password != confirmPassword -> "Mật khẩu nhập lại không khớp"
            else -> null
        }
    }

    fun validateDisplayName(displayName: String): String? {
        val trimmed = displayName.trim()
        return when {
            trimmed.isEmpty() -> "Tên hiển thị không được để trống"
            trimmed.length < 2 -> "Tên hiển thị cần ít nhất 2 ký tự"
            trimmed.length > 30 -> "Tên hiển thị tối đa 30 ký tự"
            else -> null
        }
    }

    fun validateUsernameFormat(username: String): String? {
        return when {
            username.isEmpty() -> "Username không được để trống"
            username.length < 3 -> "Username cần ít nhất 3 ký tự"
            username.length > 20 -> "Username tối đa 20 ký tự"
            !USERNAME_REGEX.matches(username) -> "Chỉ dùng chữ thường a–z, số 0–9, dấu chấm (.) và gạch dưới (_)"
            else -> null
        }
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/LoginContract.kt
package vn.nhom2.lokap.feature.auth

import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val serverError: String? = null,
    val isOffline: Boolean = false
) {
    val isBusy: Boolean
        get() = isLoading || isGoogleLoading
}

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object EmailFocusLost : LoginAction
    data object PasswordFocusLost : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data object SubmitClicked : LoginAction
    data object GoogleSignInClicked : LoginAction
    data object RetryClicked : LoginAction
    data object ForgotPasswordClicked : LoginAction
    data object RegisterClicked : LoginAction
}

sealed interface LoginEffect {
    data object NavigateToCamera : LoginEffect
    data object NavigateToSetupProfile : LoginEffect
    data object NavigateToRegister : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/LoginViewModel.kt
package vn.nhom2.lokap.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var lastAttemptWasGoogle = false

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> {
                _uiState.update { state ->
                    state.copy(
                        email = action.value,
                        emailError = if (state.emailTouched) AuthValidators.validateEmail(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is LoginAction.PasswordChanged -> {
                _uiState.update { state ->
                    state.copy(
                        password = action.value,
                        passwordError = if (state.passwordTouched) AuthValidators.validatePassword(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            LoginAction.EmailFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        emailTouched = true,
                        emailError = AuthValidators.validateEmail(state.email)
                    )
                }
            }

            LoginAction.PasswordFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        passwordTouched = true,
                        passwordError = AuthValidators.validatePassword(state.password)
                    )
                }
            }

            LoginAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            LoginAction.SubmitClicked -> submitEmailLogin()

            LoginAction.GoogleSignInClicked -> submitGoogleLogin()

            LoginAction.RetryClicked -> {
                if (lastAttemptWasGoogle) {
                    submitGoogleLogin()
                } else {
                    submitEmailLogin()
                }
            }

            LoginAction.ForgotPasswordClicked -> {
                viewModelScope.launch {
                    _effect.send(LoginEffect.NavigateToForgotPassword)
                }
            }

            LoginAction.RegisterClicked -> {
                viewModelScope.launch {
                    _effect.send(LoginEffect.NavigateToRegister)
                }
            }
        }
    }

    private fun submitEmailLogin() {
        val current = _uiState.value
        if (current.isBusy) return

        val emailError = AuthValidators.validateEmail(current.email)
        val passwordError = AuthValidators.validatePassword(current.password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailTouched = true,
                    passwordTouched = true,
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        lastAttemptWasGoogle = false
        _uiState.update {
            it.copy(
                isLoading = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.login(current.email, current.password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false) }
                    if (user.username.isBlank()) {
                        _effect.send(LoginEffect.NavigateToSetupProfile)
                    } else {
                        _effect.send(LoginEffect.NavigateToCamera)
                    }
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = isOffline,
                            serverError = error.message ?: "Đã xảy ra lỗi"
                        )
                    }
                }
            )
        }
    }

    private fun submitGoogleLogin() {
        val current = _uiState.value
        if (current.isBusy) return

        lastAttemptWasGoogle = true
        _uiState.update {
            it.copy(
                isGoogleLoading = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.loginWithGoogle()
            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isGoogleLoading = false) }
                    if (user.username.isBlank()) {
                        _effect.send(LoginEffect.NavigateToSetupProfile)
                    } else {
                        _effect.send(LoginEffect.NavigateToCamera)
                    }
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            isOffline = isOffline,
                            serverError = error.message ?: "Không thể kết nối với Google"
                        )
                    }
                }
            )
        }
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/LoginScreen.kt
package vn.nhom2.lokap.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute

fun NavGraphBuilder.loginScreen(navController: NavController) {
    composable<LoginRoute> {
        LoginRoute(
            onNavigateToCamera = {
                navController.navigate(Route.Camera) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToSetupProfile = {
                navController.navigate(SetupProfileRoute) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToRegister = {
                navController.navigate(RegisterRoute) {
                    launchSingleTop = true
                }
            },
            onNavigateToForgotPassword = {
                navController.navigate(ForgotPasswordRoute) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun LoginRoute(
    onNavigateToCamera: () -> Unit,
    onNavigateToSetupProfile: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToCamera -> onNavigateToCamera()
                LoginEffect.NavigateToSetupProfile -> onNavigateToSetupProfile()
                LoginEffect.NavigateToRegister -> onNavigateToRegister()
                LoginEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var emailHadFocus by remember { mutableStateOf(false) }
    var passwordHadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        // Large gradient logo "Lokap" (pink -> lilac)
        Text(
            text = "Lokap",
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        LokapTheme.colors.primary,
                        LokapTheme.colors.accentLilac
                    )
                ),
                fontSize = 52.sp,
                fontWeight = FontWeight.Black
            )
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Chụp tức thì · Ghim khắp Việt Nam 🇻🇳",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        // Email field
        OutlinedTextField(
            value = uiState.email,
            onValueChange = { onAction(LoginAction.EmailChanged(it)) },
            enabled = !uiState.isBusy,
            label = { Text("Email") },
            placeholder = { Text("demo@lokap.vn") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = null)
            },
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { err ->
                { Text(text = err, color = LokapTheme.colors.error) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        emailHadFocus = true
                    } else if (emailHadFocus) {
                        onAction(LoginAction.EmailFocusLost)
                    }
                }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        // Password field
        OutlinedTextField(
            value = uiState.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            enabled = !uiState.isBusy,
            label = { Text("Mật khẩu") },
            placeholder = { Text("Ít nhất 8 ký tự chữ và số") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                TextButton(
                    onClick = { onAction(LoginAction.TogglePasswordVisibility) },
                    enabled = !uiState.isBusy
                ) {
                    Text(
                        text = if (uiState.isPasswordVisible) "Ẩn" else "Hiện",
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.primary
                    )
                }
            },
            visualTransformation = if (uiState.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { err ->
                { Text(text = err, color = LokapTheme.colors.error) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onAction(LoginAction.SubmitClicked)
                }
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        passwordHadFocus = true
                    } else if (passwordHadFocus) {
                        onAction(LoginAction.PasswordFocusLost)
                    }
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onAction(LoginAction.ForgotPasswordClicked) },
                enabled = !uiState.isBusy
            ) {
                Text(
                    text = "Quên mật khẩu?",
                    style = MaterialTheme.typography.labelLarge,
                    color = LokapTheme.colors.primarySoft
                )
            }
        }

        if (uiState.serverError != null) {
            Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
            if (uiState.isOffline) {
                ErrorState(
                    message = uiState.serverError,
                    onRetry = { onAction(LoginAction.RetryClicked) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = uiState.serverError,
                    style = MaterialTheme.typography.bodySmall,
                    color = LokapTheme.colors.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = LokapTheme.spacing.xs)
                )
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        PinkButton(
            text = "Đăng nhập",
            onClick = {
                focusManager.clearFocus()
                onAction(LoginAction.SubmitClicked)
            },
            enabled = !uiState.isBusy,
            loading = uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        GhostButton(
            text = "Tiếp tục với Google",
            onClick = {
                focusManager.clearFocus()
                onAction(LoginAction.GoogleSignInClicked)
            },
            enabled = !uiState.isBusy,
            loading = uiState.isGoogleLoading,
            leadingIcon = Icons.Default.AccountCircle,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chưa có tài khoản? ",
                style = MaterialTheme.typography.bodyMedium,
                color = LokapTheme.colors.textSecondary
            )
            Text(
                text = "Đăng ký",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = LokapTheme.colors.primary,
                modifier = Modifier.clickable(enabled = !uiState.isBusy) {
                    onAction(LoginAction.RegisterClicked)
                }
            )
        }
    }
}

@Composable
internal fun lokapTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LokapTheme.colors.textPrimary,
    unfocusedTextColor = LokapTheme.colors.textPrimary,
    disabledTextColor = LokapTheme.colors.textTertiary,
    focusedContainerColor = LokapTheme.colors.surface,
    unfocusedContainerColor = LokapTheme.colors.surface,
    disabledContainerColor = LokapTheme.colors.surface,
    errorContainerColor = LokapTheme.colors.surface,
    focusedBorderColor = LokapTheme.colors.primary,
    unfocusedBorderColor = LokapTheme.colors.outline,
    errorBorderColor = LokapTheme.colors.error,
    focusedLabelColor = LokapTheme.colors.primary,
    unfocusedLabelColor = LokapTheme.colors.textSecondary,
    errorLabelColor = LokapTheme.colors.error,
    cursorColor = LokapTheme.colors.primary
)

@Preview(showBackground = true, name = "Login - Default")
@Composable
private fun LoginScreenDefaultPreview() {
    LokapTheme {
        LoginScreen(
            uiState = LoginUiState(email = "demo@lokap.vn"),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - Validation & Server Error")
@Composable
private fun LoginScreenErrorPreview() {
    LokapTheme {
        LoginScreen(
            uiState = LoginUiState(
                email = "sai-email",
                emailTouched = true,
                emailError = "Email sai định dạng (ví dụ: ban@lokap.vn)",
                password = "123",
                passwordTouched = true,
                passwordError = "Mật khẩu phải có ít nhất 8 ký tự",
                serverError = "Email hoặc mật khẩu không đúng"
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - Offline State")
@Composable
private fun LoginScreenOfflinePreview() {
    LokapTheme {
        LoginScreen(
            uiState = LoginUiState(
                email = "demo@lokap.vn",
                password = "lokap123",
                isOffline = true,
                serverError = "Mất kết nối mạng. Vui lòng kiểm tra và thử lại."
            ),
            onAction = {}
        )
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/RegisterContract.kt
package vn.nhom2.lokap.feature.auth

import kotlinx.serialization.Serializable

@Serializable
data object RegisterRoute

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val confirmPasswordTouched: Boolean = false,
    val termsTouched: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val serverError: String? = null,
    val isOffline: Boolean = false
)

sealed interface RegisterAction {
    data class EmailChanged(val value: String) : RegisterAction
    data class PasswordChanged(val value: String) : RegisterAction
    data class ConfirmPasswordChanged(val value: String) : RegisterAction
    data class AcceptedTermsChanged(val accepted: Boolean) : RegisterAction
    data object EmailFocusLost : RegisterAction
    data object PasswordFocusLost : RegisterAction
    data object ConfirmPasswordFocusLost : RegisterAction
    data object TogglePasswordVisibility : RegisterAction
    data object ToggleConfirmPasswordVisibility : RegisterAction
    data object SubmitClicked : RegisterAction
    data object RetryClicked : RegisterAction
    data object BackToLoginClicked : RegisterAction
}

sealed interface RegisterEffect {
    data object NavigateToSetupProfile : RegisterEffect
    data object NavigateBackToLogin : RegisterEffect
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/RegisterViewModel.kt
package vn.nhom2.lokap.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.EmailChanged -> {
                _uiState.update { state ->
                    state.copy(
                        email = action.value,
                        emailError = if (state.emailTouched) AuthValidators.validateEmail(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is RegisterAction.PasswordChanged -> {
                _uiState.update { state ->
                    state.copy(
                        password = action.value,
                        passwordError = if (state.passwordTouched) AuthValidators.validatePassword(action.value) else null,
                        confirmPasswordError = if (state.confirmPasswordTouched) {
                            AuthValidators.validateConfirmPassword(action.value, state.confirmPassword)
                        } else {
                            null
                        },
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is RegisterAction.ConfirmPasswordChanged -> {
                _uiState.update { state ->
                    state.copy(
                        confirmPassword = action.value,
                        confirmPasswordError = if (state.confirmPasswordTouched) {
                            AuthValidators.validateConfirmPassword(state.password, action.value)
                        } else {
                            null
                        },
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is RegisterAction.AcceptedTermsChanged -> {
                _uiState.update { state ->
                    state.copy(
                        acceptedTerms = action.accepted,
                        termsTouched = true,
                        termsError = if (action.accepted) null else "Bạn cần đồng ý Điều khoản & Chính sách riêng tư"
                    )
                }
            }

            RegisterAction.EmailFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        emailTouched = true,
                        emailError = AuthValidators.validateEmail(state.email)
                    )
                }
            }

            RegisterAction.PasswordFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        passwordTouched = true,
                        passwordError = AuthValidators.validatePassword(state.password)
                    )
                }
            }

            RegisterAction.ConfirmPasswordFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        confirmPasswordTouched = true,
                        confirmPasswordError = AuthValidators.validateConfirmPassword(
                            state.password,
                            state.confirmPassword
                        )
                    )
                }
            }

            RegisterAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            RegisterAction.ToggleConfirmPasswordVisibility -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }

            RegisterAction.SubmitClicked, RegisterAction.RetryClicked -> submitRegister()

            RegisterAction.BackToLoginClicked -> {
                viewModelScope.launch {
                    _effect.send(RegisterEffect.NavigateBackToLogin)
                }
            }
        }
    }

    private fun submitRegister() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailError = AuthValidators.validateEmail(current.email)
        val passwordError = AuthValidators.validatePassword(current.password)
        val confirmError = AuthValidators.validateConfirmPassword(current.password, current.confirmPassword)
        val termsError = if (current.acceptedTerms) {
            null
        } else {
            "Bạn cần đồng ý Điều khoản & Chính sách riêng tư"
        }

        if (emailError != null || passwordError != null || confirmError != null || termsError != null) {
            _uiState.update {
                it.copy(
                    emailTouched = true,
                    passwordTouched = true,
                    confirmPasswordTouched = true,
                    termsTouched = true,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                    termsError = termsError
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.register(current.email, current.password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(RegisterEffect.NavigateToSetupProfile)
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    val emailTakenError = if (error is AuthStoreException.EmailAlreadyTaken) {
                        error.message
                    } else {
                        null
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = isOffline,
                            emailError = emailTakenError ?: it.emailError,
                            serverError = if (emailTakenError == null) error.message else null
                        )
                    }
                }
            )
        }
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/RegisterScreen.kt
package vn.nhom2.lokap.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute

fun NavGraphBuilder.registerScreen(navController: NavController) {
    composable<RegisterRoute> {
        RegisterRoute(
            onNavigateToSetupProfile = {
                navController.navigate(SetupProfileRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun RegisterRoute(
    onNavigateToSetupProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RegisterEffect.NavigateToSetupProfile -> onNavigateToSetupProfile()
                RegisterEffect.NavigateBackToLogin -> onNavigateBack()
            }
        }
    }

    RegisterScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var emailHadFocus by remember { mutableStateOf(false) }
    var passwordHadFocus by remember { mutableStateOf(false) }
    var confirmHadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md)
    ) {
        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
        }

        IconButton(
            onClick = { onAction(RegisterAction.BackToLoginClicked) },
            enabled = !uiState.isLoading
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = LokapTheme.colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Text(
            text = "Tạo tài khoản mới ✨",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Gia nhập Lokap để chia sẻ khoảnh khắc cùng hội bạn thân.",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

        // Email
        OutlinedTextField(
            value = uiState.email,
            onValueChange = { onAction(RegisterAction.EmailChanged(it)) },
            enabled = !uiState.isLoading,
            label = { Text("Email") },
            placeholder = { Text("ban@lokap.vn") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = null)
            },
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { err ->
                { Text(text = err, color = LokapTheme.colors.error) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        emailHadFocus = true
                    } else if (emailHadFocus) {
                        onAction(RegisterAction.EmailFocusLost)
                    }
                }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        // Password
        OutlinedTextField(
            value = uiState.password,
            onValueChange = { onAction(RegisterAction.PasswordChanged(it)) },
            enabled = !uiState.isLoading,
            label = { Text("Mật khẩu") },
            placeholder = { Text("Ít nhất 8 ký tự, gồm chữ và số") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                TextButton(
                    onClick = { onAction(RegisterAction.TogglePasswordVisibility) },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (uiState.isPasswordVisible) "Ẩn" else "Hiện",
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.primary
                    )
                }
            },
            visualTransformation = if (uiState.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { err ->
                { Text(text = err, color = LokapTheme.colors.error) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        passwordHadFocus = true
                    } else if (passwordHadFocus) {
                        onAction(RegisterAction.PasswordFocusLost)
                    }
                }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        // Confirm password
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = { onAction(RegisterAction.ConfirmPasswordChanged(it)) },
            enabled = !uiState.isLoading,
            label = { Text("Nhập lại mật khẩu") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                TextButton(
                    onClick = { onAction(RegisterAction.ToggleConfirmPasswordVisibility) },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (uiState.isConfirmPasswordVisible) "Ẩn" else "Hiện",
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.primary
                    )
                }
            },
            visualTransformation = if (uiState.isConfirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = uiState.confirmPasswordError != null,
            supportingText = uiState.confirmPasswordError?.let { err ->
                { Text(text = err, color = LokapTheme.colors.error) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        confirmHadFocus = true
                    } else if (confirmHadFocus) {
                        onAction(RegisterAction.ConfirmPasswordFocusLost)
                    }
                }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        // Terms checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !uiState.isLoading) {
                    onAction(RegisterAction.AcceptedTermsChanged(!uiState.acceptedTerms))
                }
        ) {
            Checkbox(
                checked = uiState.acceptedTerms,
                onCheckedChange = { onAction(RegisterAction.AcceptedTermsChanged(it)) },
                enabled = !uiState.isLoading,
                colors = CheckboxDefaults.colors(
                    checkedColor = LokapTheme.colors.primary,
                    checkmarkColor = LokapTheme.colors.onPrimary,
                    uncheckedColor = LokapTheme.colors.outline
                )
            )
            Text(
                text = "Tôi đồng ý Điều khoản & Chính sách riêng tư",
                style = MaterialTheme.typography.bodyMedium,
                color = LokapTheme.colors.textPrimary
            )
        }

        if (uiState.termsError != null) {
            Text(
                text = uiState.termsError,
                style = MaterialTheme.typography.bodySmall,
                color = LokapTheme.colors.error,
                modifier = Modifier.padding(start = LokapTheme.spacing.md)
            )
        }

        if (uiState.serverError != null) {
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
            ErrorState(
                message = uiState.serverError,
                onRetry = { onAction(RegisterAction.RetryClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

        PinkButton(
            text = "Tạo tài khoản",
            onClick = {
                focusManager.clearFocus()
                onAction(RegisterAction.SubmitClicked)
            },
            enabled = !uiState.isLoading,
            loading = uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "Register - Default")
@Composable
private fun RegisterScreenDefaultPreview() {
    LokapTheme {
        RegisterScreen(
            uiState = RegisterUiState(),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "Register - Email Taken & Unchecked Terms")
@Composable
private fun RegisterScreenErrorsPreview() {
    LokapTheme {
        RegisterScreen(
            uiState = RegisterUiState(
                email = "taken@lokap.vn",
                emailTouched = true,
                emailError = "Email này đã được đăng ký tài khoản khác",
                password = "lokap123",
                confirmPassword = "lokap999",
                confirmPasswordTouched = true,
                confirmPasswordError = "Mật khẩu nhập lại không khớp",
                termsTouched = true,
                termsError = "Bạn cần đồng ý Điều khoản & Chính sách riêng tư"
            ),
            onAction = {}
        )
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/ForgotPasswordContract.kt
package vn.nhom2.lokap.feature.auth

import kotlinx.serialization.Serializable

@Serializable
data object ForgotPasswordRoute

data class ForgotPasswordUiState(
    val email: String = "",
    val emailTouched: Boolean = false,
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val submittedEmail: String? = null,
    val serverError: String? = null,
    val isOffline: Boolean = false
)

sealed interface ForgotPasswordAction {
    data class EmailChanged(val value: String) : ForgotPasswordAction
    data object EmailFocusLost : ForgotPasswordAction
    data object SubmitClicked : ForgotPasswordAction
    data object RetryClicked : ForgotPasswordAction
    data object BackToLoginClicked : ForgotPasswordAction
}

sealed interface ForgotPasswordEffect {
    data object NavigateBackToLogin : ForgotPasswordEffect
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/ForgotPasswordViewModel.kt
package vn.nhom2.lokap.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore

class ForgotPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.EmailChanged -> {
                _uiState.update { state ->
                    state.copy(
                        email = action.value,
                        emailError = if (state.emailTouched) AuthValidators.validateEmail(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            ForgotPasswordAction.EmailFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        emailTouched = true,
                        emailError = AuthValidators.validateEmail(state.email)
                    )
                }
            }

            ForgotPasswordAction.SubmitClicked, ForgotPasswordAction.RetryClicked -> submitReset()

            ForgotPasswordAction.BackToLoginClicked -> {
                viewModelScope.launch {
                    _effect.send(ForgotPasswordEffect.NavigateBackToLogin)
                }
            }
        }
    }

    private fun submitReset() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailError = AuthValidators.validateEmail(current.email)
        if (emailError != null) {
            _uiState.update {
                it.copy(
                    emailTouched = true,
                    emailError = emailError
                )
            }
            return
        }

        val targetEmail = current.email.trim()
        _uiState.update {
            it.copy(
                isLoading = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.resetPassword(targetEmail)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            submittedEmail = targetEmail
                        )
                    }
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = isOffline,
                            serverError = error.message ?: "Đã xảy ra lỗi"
                        )
                    }
                }
            )
        }
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/ForgotPasswordScreen.kt
package vn.nhom2.lokap.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

fun NavGraphBuilder.forgotPasswordScreen(navController: NavController) {
    composable<ForgotPasswordRoute> {
        ForgotPasswordRoute(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun ForgotPasswordRoute(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ForgotPasswordEffect.NavigateBackToLogin -> onNavigateBack()
            }
        }
    }

    ForgotPasswordScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onAction: (ForgotPasswordAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var emailHadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md)
    ) {
        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
        }

        IconButton(
            onClick = { onAction(ForgotPasswordAction.BackToLoginClicked) },
            enabled = !uiState.isLoading
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = LokapTheme.colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Text(
            text = "Quên mật khẩu? 🔑",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Nhập email bạn dùng trên Lokap, tụi mình sẽ gửi hướng dẫn đặt lại mật khẩu.",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

        if (uiState.submittedEmail != null) {
            Surface(
                shape = LokapTheme.shapes.card,
                color = LokapTheme.colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LokapTheme.colors.success, LokapTheme.shapes.card)
            ) {
                Column(
                    modifier = Modifier.padding(LokapTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                ) {
                    Text(text = "📬", fontSize = 40.sp)
                    Text(
                        text = "Đã gửi link đặt lại mật khẩu tới ${uiState.submittedEmail}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LokapTheme.colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Hãy kiểm tra hộp thư đến hoặc thư rác (Spam) trong vài phút tới nhé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LokapTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

            GhostButton(
                text = "Quay lại đăng nhập",
                onClick = { onAction(ForgotPasswordAction.BackToLoginClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onAction(ForgotPasswordAction.EmailChanged(it)) },
                enabled = !uiState.isLoading,
                label = { Text("Email") },
                placeholder = { Text("ban@lokap.vn") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                },
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { err ->
                    { Text(text = err, color = LokapTheme.colors.error) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onAction(ForgotPasswordAction.SubmitClicked)
                    }
                ),
                shape = LokapTheme.shapes.card,
                colors = lokapTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            emailHadFocus = true
                        } else if (emailHadFocus) {
                            onAction(ForgotPasswordAction.EmailFocusLost)
                        }
                    }
            )

            if (uiState.serverError != null) {
                Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
                ErrorState(
                    message = uiState.serverError,
                    onRetry = { onAction(ForgotPasswordAction.RetryClicked) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

            PinkButton(
                text = "Gửi link đặt lại",
                onClick = {
                    focusManager.clearFocus()
                    onAction(ForgotPasswordAction.SubmitClicked)
                },
                enabled = !uiState.isLoading,
                loading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, name = "ForgotPassword - Form")
@Composable
private fun ForgotPasswordFormPreview() {
    LokapTheme {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(email = "ban@lokap.vn"),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "ForgotPassword - Sent Confirmation")
@Composable
private fun ForgotPasswordSentPreview() {
    LokapTheme {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(
                email = "khongtontai@lokap.vn",
                submittedEmail = "khongtontai@lokap.vn"
            ),
            onAction = {}
        )
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/setup/SetupProfileContract.kt
package vn.nhom2.lokap.feature.auth.setup

import kotlinx.serialization.Serializable

@Serializable
data object SetupProfileRoute

enum class UsernameAvailability {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN,
    INVALID
}

val PresetAvatars = listOf(
    "https://picsum.photos/seed/lokap_genz_1/200/200",
    "https://picsum.photos/seed/lokap_genz_2/200/200",
    "https://picsum.photos/seed/lokap_genz_3/200/200",
    "https://picsum.photos/seed/lokap_genz_4/200/200",
    "https://picsum.photos/seed/lokap_genz_5/200/200",
    "https://picsum.photos/seed/lokap_genz_6/200/200"
)

data class SetupProfileUiState(
    val selectedAvatarUrl: String? = null,
    val presetAvatars: List<String> = PresetAvatars,
    val displayName: String = "",
    val username: String = "",
    val displayNameTouched: Boolean = false,
    val usernameTouched: Boolean = false,
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val usernameAvailability: UsernameAvailability = UsernameAvailability.IDLE,
    val isSubmitting: Boolean = false,
    val serverError: String? = null,
    val isOffline: Boolean = false
) {
    val isFormValid: Boolean
        get() = displayName.trim().length in 2..30 &&
            displayNameError == null &&
            usernameError == null &&
            usernameAvailability == UsernameAvailability.AVAILABLE &&
            !isSubmitting
}

sealed interface SetupProfileAction {
    data class AvatarSelected(val avatarUrl: String?) : SetupProfileAction
    data class DisplayNameChanged(val value: String) : SetupProfileAction
    data class UsernameChanged(val value: String) : SetupProfileAction
    data object DisplayNameFocusLost : SetupProfileAction
    data object UsernameFocusLost : SetupProfileAction
    data object SubmitClicked : SetupProfileAction
    data object RetryClicked : SetupProfileAction
}

sealed interface SetupProfileEffect {
    data object NavigateToCamera : SetupProfileEffect
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/setup/SetupProfileViewModel.kt
package vn.nhom2.lokap.feature.auth.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.feature.auth.AuthValidators

class SetupProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SetupProfileUiState())
    val uiState: StateFlow<SetupProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SetupProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var usernameCheckJob: Job? = null

    fun onAction(action: SetupProfileAction) {
        when (action) {
            is SetupProfileAction.AvatarSelected -> {
                _uiState.update { it.copy(selectedAvatarUrl = action.avatarUrl) }
            }

            is SetupProfileAction.DisplayNameChanged -> {
                _uiState.update { state ->
                    state.copy(
                        displayName = action.value,
                        displayNameError = if (state.displayNameTouched) {
                            AuthValidators.validateDisplayName(action.value)
                        } else {
                            null
                        },
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is SetupProfileAction.UsernameChanged -> {
                val lowered = action.value.lowercase()
                onUsernameInputChanged(lowered)
            }

            SetupProfileAction.DisplayNameFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        displayNameTouched = true,
                        displayNameError = AuthValidators.validateDisplayName(state.displayName)
                    )
                }
            }

            SetupProfileAction.UsernameFocusLost -> {
                _uiState.update { state ->
                    val formatError = AuthValidators.validateUsernameFormat(state.username)
                    state.copy(
                        usernameTouched = true,
                        usernameError = formatError ?: if (state.usernameAvailability == UsernameAvailability.TAKEN) {
                            "Username này đã có người dùng"
                        } else {
                            null
                        }
                    )
                }
            }

            SetupProfileAction.SubmitClicked, SetupProfileAction.RetryClicked -> submitProfile()
        }
    }

    private fun onUsernameInputChanged(lowered: String) {
        usernameCheckJob?.cancel()

        val formatError = AuthValidators.validateUsernameFormat(lowered)
        if (formatError != null) {
            _uiState.update { state ->
                state.copy(
                    username = lowered,
                    usernameAvailability = if (lowered.isEmpty()) {
                        UsernameAvailability.IDLE
                    } else {
                        UsernameAvailability.INVALID
                    },
                    usernameError = if (state.usernameTouched || lowered.isNotEmpty()) formatError else null,
                    serverError = null,
                    isOffline = false
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                username = lowered,
                usernameAvailability = UsernameAvailability.CHECKING,
                usernameError = null,
                serverError = null,
                isOffline = false
            )
        }

        usernameCheckJob = viewModelScope.launch {
            delay(400) // Debounce 400ms
            val result = FakeAuthStore.isUsernameAvailable(lowered)
            result.fold(
                onSuccess = { available ->
                    _uiState.update { state ->
                        if (state.username != lowered) return@update state
                        state.copy(
                            usernameAvailability = if (available) {
                                UsernameAvailability.AVAILABLE
                            } else {
                                UsernameAvailability.TAKEN
                            },
                            usernameError = if (available) {
                                null
                            } else {
                                "Username này đã có người dùng"
                            }
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            usernameAvailability = UsernameAvailability.IDLE,
                            isOffline = true,
                            serverError = "Mất kết nối mạng khi kiểm tra username"
                        )
                    }
                }
            )
        }
    }

    private fun submitProfile() {
        val current = _uiState.value
        if (!current.isFormValid || current.isSubmitting) return

        val displayNameError = AuthValidators.validateDisplayName(current.displayName)
        val usernameFormatError = AuthValidators.validateUsernameFormat(current.username)
        if (displayNameError != null || usernameFormatError != null) {
            _uiState.update {
                it.copy(
                    displayNameTouched = true,
                    usernameTouched = true,
                    displayNameError = displayNameError,
                    usernameError = usernameFormatError
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSubmitting = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.completeProfile(
                username = current.username,
                displayName = current.displayName,
                avatarUrl = current.selectedAvatarUrl
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.send(SetupProfileEffect.NavigateToCamera)
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    val isTaken = error is AuthStoreException.UsernameAlreadyTaken
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isOffline = isOffline,
                            usernameAvailability = if (isTaken) UsernameAvailability.TAKEN else it.usernameAvailability,
                            usernameError = if (isTaken) error.message else it.usernameError,
                            serverError = if (!isTaken) error.message else null
                        )
                    }
                }
            )
        }
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/auth/setup/SetupProfileScreen.kt
package vn.nhom2.lokap.feature.auth.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.feature.auth.lokapTextFieldColors

fun NavGraphBuilder.setupProfileScreen(navController: NavController) {
    composable<SetupProfileRoute> {
        SetupProfileRoute(
            onNavigateToCamera = {
                navController.navigate(Route.Camera) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun SetupProfileRoute(
    onNavigateToCamera: () -> Unit,
    viewModel: SetupProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SetupProfileEffect.NavigateToCamera -> onNavigateToCamera()
            }
        }
    }

    SetupProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupProfileScreen(
    uiState: SetupProfileUiState,
    onAction: (SetupProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var displayNameHadFocus by remember { mutableStateOf(false) }
    var usernameHadFocus by remember { mutableStateOf(false) }

    val previewName = uiState.displayName.trim().ifEmpty { "Lokap" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Text(
            text = "Tạo chiếc hồ sơ chất riêng 💅",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Chọn avatar và đặt chiếc username để hội bạn dễ tìm thấy bạn.",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

        // Main preview AvatarRing (shows initial if selectedAvatarUrl == null)
        AvatarRing(
            avatarUrl = uiState.selectedAvatarUrl,
            displayName = previewName,
            size = 96.dp,
            hasNewPost = true
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        GlassChip(
            label = if (uiState.selectedAvatarUrl == null) {
                "Đang dùng chữ cái đầu"
            } else {
                "Dùng chữ cái đầu"
            },
            selected = uiState.selectedAvatarUrl == null,
            onClick = { onAction(SetupProfileAction.AvatarSelected(null)) }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Text(
            text = "Hoặc chọn nhanh 1 trong 6 avatar mẫu:",
            style = MaterialTheme.typography.labelLarge,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            uiState.presetAvatars.forEach { avatarUrl ->
                val isSelected = uiState.selectedAvatarUrl == avatarUrl
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) LokapTheme.colors.primary else LokapTheme.colors.outline,
                            shape = CircleShape
                        )
                        .clickable(enabled = !uiState.isSubmitting) {
                            onAction(SetupProfileAction.AvatarSelected(avatarUrl))
                        }
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarRing(
                        avatarUrl = avatarUrl,
                        displayName = previewName,
                        size = 56.dp,
                        hasNewPost = isSelected,
                        onClick = { onAction(SetupProfileAction.AvatarSelected(avatarUrl)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        // Display name (2 - 30 chars)
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = { onAction(SetupProfileAction.DisplayNameChanged(it)) },
            enabled = !uiState.isSubmitting,
            label = { Text("Tên hiển thị (2–30 ký tự)") },
            placeholder = { Text("Ví dụ: Minh Anh 🍓") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Person, contentDescription = null)
            },
            isError = uiState.displayNameError != null,
            supportingText = {
                if (uiState.displayNameError != null) {
                    Text(text = uiState.displayNameError, color = LokapTheme.colors.error)
                } else {
                    Text(
                        text = "${uiState.displayName.trim().length}/30 ký tự",
                        color = LokapTheme.colors.textTertiary
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        displayNameHadFocus = true
                    } else if (displayNameHadFocus) {
                        onAction(SetupProfileAction.DisplayNameFocusLost)
                    }
                }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        // Username (3-20 chars, lowercase, debounce 400ms with ✓ / ✗ inside field)
        OutlinedTextField(
            value = uiState.username,
            onValueChange = { onAction(SetupProfileAction.UsernameChanged(it)) },
            enabled = !uiState.isSubmitting,
            label = { Text("Username (3–20 ký tự)") },
            placeholder = { Text("minhanh.2k5") },
            prefix = {
                Text(
                    text = "@",
                    color = LokapTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            trailingIcon = {
                UsernameAvailabilityTrailingIcon(availability = uiState.usernameAvailability)
            },
            isError = uiState.usernameError != null || uiState.usernameAvailability == UsernameAvailability.TAKEN,
            supportingText = {
                when {
                    uiState.usernameError != null -> {
                        Text(text = uiState.usernameError, color = LokapTheme.colors.error)
                    }

                    uiState.usernameAvailability == UsernameAvailability.AVAILABLE -> {
                        Text(
                            text = "✓ Username hợp lệ và chưa ai dùng",
                            color = LokapTheme.colors.success
                        )
                    }

                    uiState.usernameAvailability == UsernameAvailability.CHECKING -> {
                        Text(
                            text = "Đang kiểm tra độ trùng...",
                            color = LokapTheme.colors.textSecondary
                        )
                    }

                    else -> {
                        Text(
                            text = "Chỉ dùng a–z, 0–9, dấu chấm (.) và gạch dưới (_)",
                            color = LokapTheme.colors.textTertiary
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (uiState.isFormValid) {
                        onAction(SetupProfileAction.SubmitClicked)
                    }
                }
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        usernameHadFocus = true
                    } else if (usernameHadFocus) {
                        onAction(SetupProfileAction.UsernameFocusLost)
                    }
                }
        )

        if (uiState.serverError != null) {
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
            ErrorState(
                message = uiState.serverError,
                onRetry = { onAction(SetupProfileAction.RetryClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        PinkButton(
            text = "Vào Lokap",
            onClick = {
                focusManager.clearFocus()
                onAction(SetupProfileAction.SubmitClicked)
            },
            enabled = uiState.isFormValid,
            loading = uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UsernameAvailabilityTrailingIcon(availability: UsernameAvailability) {
    when (availability) {
        UsernameAvailability.IDLE -> Unit
        UsernameAvailability.CHECKING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LokapTheme.colors.primary
            )
        }

        UsernameAvailability.AVAILABLE -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Hợp lệ",
                tint = LokapTheme.colors.success
            )
        }

        UsernameAvailability.TAKEN, UsernameAvailability.INVALID -> {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Không hợp lệ",
                tint = LokapTheme.colors.error
            )
        }
    }
}

@Preview(showBackground = true, name = "SetupProfile - Available Ready")
@Composable
private fun SetupProfileAvailablePreview() {
    LokapTheme {
        SetupProfileScreen(
            uiState = SetupProfileUiState(
                displayName = "Minh Anh",
                username = "minhanh.2k5",
                usernameAvailability = UsernameAvailability.AVAILABLE
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "SetupProfile - Username Taken")
@Composable
private fun SetupProfileTakenPreview() {
    LokapTheme {
        SetupProfileScreen(
            uiState = SetupProfileUiState(
                displayName = "Lokap Official",
                username = "lokap",
                usernameTouched = true,
                usernameAvailability = UsernameAvailability.TAKEN,
                usernameError = "Username này đã có người dùng"
            ),
            onAction = {}
        )
    }
}
```
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/launch/RememberStartDestination.kt
package vn.nhom2.lokap.feature.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.feature.auth.LoginRoute
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute
import vn.nhom2.lokap.feature.onboarding.OnboardingRoute

/**
 * Xác định route khởi đầu của Lokap từ [FakeAuthStore].
 * Trả về `null` khi đang đọc trạng thái ban đầu để giữ màn hình Splash tiếp tục hiển thị.
 */
@Composable
fun rememberStartDestination(): Any? {
    val onboardingDone by FakeAuthStore.onboardingDone.collectAsStateWithLifecycle()
    val currentUser by FakeAuthStore.currentUser.collectAsStateWithLifecycle()
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isInitialized = true
    }

    if (!isInitialized) return null

    return remember(onboardingDone, currentUser) {
        when {
            !onboardingDone -> OnboardingRoute
            currentUser == null -> LoginRoute
            currentUser?.username.isNullOrBlank() -> SetupProfileRoute
            else -> Route.Camera
        }
    }
}
```
1. Đoạn thay đổi trong AppNavHost để dùng rememberStartDestination()

Chỉ cần cập nhật phần khởi tạo startDestination và đăng ký 5 màn hình mới trong AppNavHost.kt:

```kotlin
// Thêm các import mới vào AppNavHost.kt:
import vn.nhom2.lokap.feature.auth.forgotPasswordScreen
import vn.nhom2.lokap.feature.auth.loginScreen
import vn.nhom2.lokap.feature.auth.registerScreen
import vn.nhom2.lokap.feature.auth.setup.setupProfileScreen
import vn.nhom2.lokap.feature.launch.rememberStartDestination
import vn.nhom2.lokap.feature.onboarding.onboardingScreen

// Bên trong @Composable fun AppNavHost(...):
val startDestination = rememberStartDestination() ?: return // Giữ màn splash hiện khi đang đọc

NavHost(
    navController = navController,
    startDestination = startDestination,
    modifier = modifier
) {
    onboardingScreen(navController)
    loginScreen(navController)
    registerScreen(navController)
    forgotPasswordScreen(navController)
    setupProfileScreen(navController)

    // Các màn hình hiện có của project (cameraScreen, feedScreen, ...) giữ nguyên bên dưới
```
2. Quyền cần thêm vào AndroidManifest.xml
```kotlin
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<uses-feature
    android:name="android.hardware.camera.any"
    android:required="false" />
```
3. Dependency mới

androidx.activity:activity-compose (nếu chưa khai báo trực tiếp trong app/build.gradle.kts, dùng cho rememberLauncherForActivityResult)

androidx.lifecycle:lifecycle-runtime-compose (nếu chưa có, dùng cho collectAsStateWithLifecycle)