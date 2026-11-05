package vn.nhom2.lokap.feature.auth

import vn.nhom2.lokap.core.navigation.navigateClearingBackStack
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
                navController.navigateClearingBackStack(Route.Camera)
            },
            onNavigateToSetupProfile = {
                navController.navigateClearingBackStack(SetupProfileRoute)
            },
            onNavigateToRegister = {
                navController.navigate(RegisterRoute) { launchSingleTop = true }
            },
            onNavigateToForgotPassword = {
                navController.navigate(ForgotPasswordRoute) { launchSingleTop = true }
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
