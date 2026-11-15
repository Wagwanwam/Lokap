package vn.nhom2.lokap.feature.auth.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateClearingBackStack
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute

fun NavGraphBuilder.verifyEmailScreen(navController: NavController) {
    composable<VerifyEmailRoute> {
        VerifyEmailRoute(
            onNavigateToSetupProfile = { navController.navigateClearingBackStack(SetupProfileRoute) },
            onNavigateToCamera = { navController.navigateClearingBackStack(Route.Camera) },
            onNavigateBack = { navController.popBackStackSafely() }
        )
    }
}

@Composable
fun VerifyEmailRoute(
    onNavigateToSetupProfile: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: VerifyEmailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                VerifyEmailEffect.NavigateToSetupProfile -> onNavigateToSetupProfile()
                VerifyEmailEffect.NavigateToCamera -> onNavigateToCamera()
                VerifyEmailEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Bấm link trong app email xong quay lại: tự kiểm tra, khỏi phải bấm nút
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(VerifyEmailAction.ScreenResumed) }

    VerifyEmailScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
fun VerifyEmailScreen(
    uiState: VerifyEmailUiState,
    onAction: (VerifyEmailAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(spacing.sm))
        }

        IconButton(
            onClick = { onAction(VerifyEmailAction.BackClicked) },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        Icon(
            imageVector = Icons.Filled.MarkEmailRead,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.padding(bottom = spacing.md)
        )

        Text(
            text = "Kiểm tra email",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = uiState.email,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = "Bấm link xác nhận rồi quay lại",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center
        )

        val message = uiState.errorMessage ?: uiState.infoMessage
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = if (uiState.errorMessage != null) colors.error else colors.success,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.sm)
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        PinkButton(
            text = "Đã xác nhận",
            onClick = { onAction(VerifyEmailAction.ConfirmClicked) },
            enabled = uiState.canSubmit,
            loading = uiState.isChecking,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(
            onClick = { onAction(VerifyEmailAction.ResendClicked) },
            enabled = uiState.canResend,
            modifier = Modifier.padding(top = spacing.sm)
        ) {
            Text(
                text = when {
                    uiState.isResending -> "Đang gửi lại…"
                    uiState.resendSecondsLeft > 0 -> "Gửi lại link sau ${uiState.resendSecondsLeft} giây"
                    else -> "Gửi lại link"
                },
                color = if (uiState.canResend) colors.primary else colors.textTertiary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifyEmailScreenPreview() {
    LokapTheme {
        VerifyEmailScreen(
            uiState = VerifyEmailUiState(email = "ban@lokap.vn", resendSecondsLeft = 42),
            onAction = {}
        )
    }
}
