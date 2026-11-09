package vn.nhom2.lokap.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute
fun NavGraphBuilder.registerScreen(navController: NavController) {
    composable<RegisterRoute> {
        RegisterRoute(
            onNavigateToSetupProfile = {
                navController.navigate(SetupProfileRoute) { launchSingleTop = true }
            },
            onNavigateBackToLogin = {
                navController.popBackStackSafely()
            }
        )
    }
}

@Composable
fun RegisterRoute(
    onNavigateToSetupProfile: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RegisterEffect.NavigateToSetupProfile -> onNavigateToSetupProfile()
                RegisterEffect.NavigateBackToLogin -> onNavigateBackToLogin()
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
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    var emailHadFocus by remember { mutableStateOf(false) }
    var passwordHadFocus by remember { mutableStateOf(false) }
    var confirmPasswordHadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.md)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceHigh,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onAction(RegisterAction.BackToLoginClicked) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LokapLogo(size = 28.dp, showText = true)

            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(spacing.md))

        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(spacing.sm))
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.primary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
        ) {
            Text(
                text = "Bước 1/2: Tài khoản & Bảo mật",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        Text(
            text = "Tạo tài khoản Lokap",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        Text(
            text = "Nhập email và mật khẩu an toàn để khởi tạo tài khoản.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {

                Column {
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { onAction(RegisterAction.EmailChanged(it)) },
                        label = { Text("Email đăng ký") },
                        placeholder = { Text("ví dụ: ban@lokap.vn") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = if (uiState.emailError != null) colors.error else colors.primary
                            )
                        },
                        isError = uiState.emailError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = lokapTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
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
                    if (uiState.emailError != null) {
                        Text(
                            text = uiState.emailError,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                Column {
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { onAction(RegisterAction.PasswordChanged(it)) },
                        label = { Text("Mật khẩu") },
                        placeholder = { Text("Tối thiểu 8 ký tự, có chữ và số") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (uiState.passwordError != null) colors.error else colors.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { onAction(RegisterAction.TogglePasswordVisibility) }) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (uiState.isPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = uiState.passwordError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = lokapTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
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
                    if (uiState.passwordError != null) {
                        Text(
                            text = uiState.passwordError,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    if (uiState.password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        PasswordStrengthBar(strength = uiState.passwordStrength)
                    }
                }

                Column {
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { onAction(RegisterAction.ConfirmPasswordChanged(it)) },
                        label = { Text("Xác nhận mật khẩu") },
                        placeholder = { Text("Nhập lại mật khẩu ở trên") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (uiState.confirmPasswordError != null) colors.error else colors.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { onAction(RegisterAction.ToggleConfirmPasswordVisibility) }) {
                                Icon(
                                    imageVector = if (uiState.isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (uiState.isConfirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = uiState.confirmPasswordError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onAction(RegisterAction.SubmitClicked) }),
                        colors = lokapTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    confirmPasswordHadFocus = true
                                } else if (confirmPasswordHadFocus) {
                                    onAction(RegisterAction.ConfirmPasswordFocusLost)
                                }
                            }
                    )
                    if (uiState.confirmPasswordError != null) {
                        Text(
                            text = uiState.confirmPasswordError,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAction(RegisterAction.AcceptedTermsChanged(!uiState.acceptedTerms)) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.acceptedTerms,
                            onCheckedChange = { onAction(RegisterAction.AcceptedTermsChanged(it)) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.primary,
                                checkmarkColor = colors.buttonText
                            )
                        )
                        Text(
                            text = "Tôi đồng ý với Điều khoản dịch vụ và Chính sách bảo mật của Lokap",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    if (uiState.termsError != null) {
                        Text(
                            text = uiState.termsError,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                }
            }
        }

        if (uiState.serverError != null) {
            Spacer(modifier = Modifier.height(spacing.sm))
            ErrorState(
                message = uiState.serverError,
                onRetry = { onAction(RegisterAction.RetryClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        PinkButton(
            text = "Tiếp tục thiết lập hồ sơ",
            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = { onAction(RegisterAction.SubmitClicked) },
            loading = uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đã có tài khoản? ",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = "Đăng nhập ngay",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.primary,
                modifier = Modifier.clickable { onAction(RegisterAction.BackToLoginClicked) }
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Composable
private fun PasswordStrengthBar(strength: PasswordStrength) {
    val (label, targetColor, barsFilled) = when (strength) {
        PasswordStrength.NONE -> Triple("", Color.Transparent, 0)
        PasswordStrength.WEAK -> Triple("Độ mạnh: Yếu", Color(0xFFEF4444), 1)
        PasswordStrength.MEDIUM -> Triple("Độ mạnh: Trung bình", Color(0xFFF59E0B), 2)
        PasswordStrength.STRONG -> Triple("Độ mạnh: Rất mạnh", Color(0xFF10B981), 3)
    }

    val animatedColor by animateColorAsState(targetValue = targetColor, label = "StrengthColor")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < barsFilled) animatedColor else LokapTheme.colors.outline.copy(alpha = 0.25f)
                        )
                )
            }
        }
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = animatedColor
            )
        }
    }
}
