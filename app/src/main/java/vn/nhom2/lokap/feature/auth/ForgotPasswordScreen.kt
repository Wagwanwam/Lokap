package vn.nhom2.lokap.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.popBackStackSafely
fun NavGraphBuilder.forgotPasswordScreen(navController: NavController) {
    composable<ForgotPasswordRoute> {
        ForgotPasswordRoute(
            onNavigateBack = {
                navController.popBackStackSafely()
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
            text = "Quên mật khẩu",
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
                    Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = LokapTheme.colors.primary,
                    modifier = Modifier.size(36.dp)
                )
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
