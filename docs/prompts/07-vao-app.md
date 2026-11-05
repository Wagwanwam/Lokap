Bạn là lập trình viên Android senior. Viết code Kotlin (Jetpack Compose + Material 3) cho phần "vào app" của Lokap: app kiểu Locket (mở ra là camera, chụp gửi bạn bè) kèm bản đồ public gom ảnh theo tỉnh/thành → phường/xã của Việt Nam. Chỉ dark mode, màu nhấn hồng, phong cách GenZ. Chữ hiển thị tiếng Việt có dấu.

## Project hiện có (KHÔNG viết lại các file này, chỉ dùng)
- Package gốc `vn.nhom2.lokap`, minSdk 26, Kotlin 2.2.10, Navigation Compose 2.10 (route @Serializable), Coil 3.3, lifecycle-viewmodel-compose 2.11, icon chỉ dùng gói material-icons-core (Icons.Default.* và Icons.AutoMirrored.Filled.*).
- Theme: `LokapTheme.colors` (background, surface, surfaceHigh, outline, primary, primarySoft, onPrimary, textPrimary, textSecondary, textTertiary, accentLilac, accentLime, accentCyan, accentYellow, error, success), `LokapTheme.spacing` (xs, sm, md, lg, xl, xxl), `LokapTheme.shapes` (cameraFrame, card, sheet, pill), `regionGradient(provinceCode: String): Brush`. Không hard-code màu. Chữ trên nền hồng dùng `onPrimary` (màu đen). Font Be Vietnam Pro đã có trong Typography.
- Model: `User(id, username, displayName, avatarUrl: String?)`. Dữ liệu giả: `object SampleData` có `currentUser`, `users`.
- Route có sẵn (`vn.nhom2.lokap.core.navigation.Route`): `Camera`. KHÔNG sửa Route.kt: route mới khai báo ngay trong package của màn hình, dạng `@Serializable data object OnboardingRoute`, `@Serializable data object LoginRoute`, `@Serializable data object RegisterRoute`, `@Serializable data object SetupProfileRoute`, `@Serializable data object ForgotPasswordRoute`.
- Component có sẵn (`vn.nhom2.lokap.core.designsystem.component`): `PinkButton(text, onClick, modifier, enabled = true, loading = false, leadingIcon = null)`; `GhostButton(...)` cùng tham số; `GlassChip(label, modifier, leadingIcon = null, selected = false, onClick = null)`; `AvatarRing(avatarUrl, displayName, modifier, size = 44.dp, hasNewPost = false, onClick = null)`; `ErrorState(message, onRetry, modifier)`; `OfflineBanner(modifier)`.
- Mẫu đăng ký màn hình: `fun NavGraphBuilder.feedScreen(navController: NavController) { composable<Route.Feed> { ... } }`; màn hình tự `systemBarsPadding()` và `imePadding()` ở form; điều hướng dùng `launchSingleTop = true`.
- Mẫu bắt buộc mỗi màn: `XxxUiState` (data class bất biến), `XxxAction` (sealed interface), `XxxEffect` (Channel), `XxxViewModel` (StateFlow), `XxxRoute` (viewModel()), `XxxScreen` (không state, có Preview cho từng trạng thái, bọc trong LokapTheme).
- Tạo mới `data/fake/FakeAuthStore.kt`: object giữ `MutableStateFlow<User?>` (null = chưa đăng nhập) và `onboardingDone: MutableStateFlow<Boolean>`; hàm `login(email, password)`, `register(email, password)`, `loginWithGoogle()`, `logout()`, `completeProfile(username, displayName)`, `resetPassword(email)` đều là `suspend`, có `delay(800)` giả lập mạng. Tài khoản mẫu: `demo@lokap.vn` / `lokap123`. Email "taken@lokap.vn" coi như đã tồn tại; username "lokap" coi như đã có người dùng.

## Phần A — Giới thiệu + xin quyền (`feature/onboarding/`)
- HorizontalPager 3 trang + chấm trang: (1) "Chụp & gửi tức thì cho bạn thân" (2) "Ghim khoảnh khắc lên bản đồ Việt Nam" (3) "Bạn chọn ai thấy, thấy tới đâu" (nói về mức vị trí và hẹn giờ). Mỗi trang có minh hoạ vẽ bằng Compose (hình khối, gradient từ regionGradient, emoji lớn), không dùng ảnh ngoài.
- Trang cuối là màn xin quyền: 3 thẻ Camera (bắt buộc), Vị trí (để gắn khu vực, có thể bỏ qua, ghi rõ "Chỉ lấy vị trí lúc chụp, không theo dõi"), Thông báo (Android 13+, có thể bỏ qua). Mỗi thẻ có trạng thái Chưa cấp / Đã cấp / Bị từ chối (nút "Mở Cài đặt"). Dùng `rememberLauncherForActivityResult(RequestMultiplePermissions)`; quyền vị trí xin cả FINE và COARSE, nếu người dùng chỉ cho COARSE thì hiện ghi chú "Chỉ đăng được ở mức Tỉnh trở lên".
- Nút "Bắt đầu" → đặt onboardingDone = true → LoginRoute. Nút "Bỏ qua" ở 2 trang đầu.

## Phần B — Đăng nhập / Đăng ký / Quên mật khẩu (`feature/auth/`)
- Đăng nhập: logo chữ "Lokap" lớn (gradient hồng → lilac), email, mật khẩu (nút ẩn/hiện), "Quên mật khẩu?", PinkButton Đăng nhập, GhostButton "Tiếp tục với Google", link "Chưa có tài khoản? Đăng ký".
- Đăng ký: email, mật khẩu, nhập lại mật khẩu, ô tích "Tôi đồng ý Điều khoản & Chính sách riêng tư" (bắt buộc), PinkButton Tạo tài khoản → SetupProfileRoute.
- Quên mật khẩu: email → "Đã gửi link đặt lại mật khẩu tới …" (luôn báo vậy dù email không tồn tại, để không lộ ai đã đăng ký).
- Kiểm tra đầu vào (hiện lỗi dưới ô, chỉ sau khi rời ô hoặc bấm nút): email trống / sai định dạng; mật khẩu < 8 ký tự hoặc không có cả chữ và số; nhập lại không khớp; chưa tích điều khoản. Lỗi máy chủ giả: sai mật khẩu ("Email hoặc mật khẩu không đúng" — không nói rõ sai cái nào), email đã tồn tại, mất mạng (OfflineBanner + nút thử lại). Nút bị khoá và hiện loading khi đang gửi; chống bấm 2 lần.
- Đăng nhập thành công → `popUpTo(0)` rồi tới Route.Camera (không quay lại được màn đăng nhập).

## Phần C — Thiết lập hồ sơ (`feature/auth/setup/`)
- Chọn ảnh đại diện: hiện AvatarRing chữ cái đầu + 6 avatar mẫu (picsum) để chọn (chưa mở thư viện ảnh).
- Tên hiển thị (2–30 ký tự), username (3–20 ký tự, chỉ a–z, 0–9, dấu chấm và gạch dưới, tự chuyển chữ thường, kiểm tra trùng có debounce 400ms với dấu ✓/✗ ngay trong ô).
- PinkButton "Vào Lokap" chỉ bật khi hợp lệ → `popUpTo(0)` tới Route.Camera.

## Phần D — Điểm vào app (`feature/launch/`)
- `@Composable fun rememberStartDestination(): Any?` đọc FakeAuthStore: chưa onboarding → OnboardingRoute; chưa đăng nhập → LoginRoute; đăng nhập nhưng chưa có username → SetupProfileRoute; còn lại → Route.Camera. Trả null khi đang đọc (để màn splash tiếp tục hiện).
- Ghi rõ ở cuối câu trả lời cách Claude Code sửa AppNavHost để dùng `rememberStartDestination()` (chỉ đưa đoạn thay đổi, không gửi cả file).

## Định dạng trả lời
- Mỗi file một khối ```kotlin … ``` RIÊNG (không gộp nhiều file vào một khối), dòng ĐẦU TIÊN là `// FILE: app/src/main/java/vn/nhom2/lokap/...`. Chỉ tạo file MỚI.
- Mỗi màn có `fun NavGraphBuilder.onboardingScreen / loginScreen / registerScreen / forgotPasswordScreen / setupProfileScreen(navController: NavController)`.
- Cuối cùng: (1) đoạn thay đổi AppNavHost; (2) quyền cần thêm vào AndroidManifest (chỉ liệt kê); (3) dependency mới nếu có (chỉ tên artifact).
- Quá dài thì dừng ở cuối một file hoàn chỉnh, ghi `CÒN TIẾP`.
