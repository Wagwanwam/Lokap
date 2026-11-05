package vn.nhom2.lokap.feature.auth

import vn.nhom2.lokap.core.navigation.popBackStackSafely
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
                navController.popBackStackSafely()
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
