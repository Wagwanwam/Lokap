Bạn là lập trình viên Android senior. Viết code Kotlin (Jetpack Compose + Material 3) cho 3 màn hình của app Lokap: app kiểu Locket (mở ra là camera, chụp gửi bạn bè) kèm bản đồ public gom ảnh theo tỉnh/thành → phường/xã của Việt Nam (34 tỉnh). Chỉ dark mode, màu nhấn hồng, phong cách GenZ. Chữ hiển thị tiếng Việt có dấu.

## Project hiện có (KHÔNG viết lại các file này, chỉ dùng)
- Package gốc `vn.nhom2.lokap`, minSdk 26, Kotlin 2.2.10, Navigation Compose 2.10 (route @Serializable), Coil 3.3, lifecycle-viewmodel-compose 2.11, icon chỉ dùng gói material-icons-core.
- Theme: `LokapTheme.colors` (background, surface, surfaceHigh, outline, primary, primarySoft, onPrimary, textPrimary, textSecondary, textTertiary, accentLilac, accentLime, accentCyan, accentYellow, error, success), `LokapTheme.spacing` (xs, sm, md, lg, xl, xxl), `LokapTheme.shapes` (cameraFrame, card, sheet, pill), `regionGradient(provinceCode: String): Brush`. Không hard-code màu.
- Model (`vn.nhom2.lokap.domain.model`): `User(id, username, displayName, avatarUrl: String?)`; `enum class FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIENDS, BLOCKED }`; `enum class LocationLevel(val labelVi: String) { PLACE, WARD, PROVINCE, COUNTRY }`; `Province(code, name, isCity, lat, lng)`; `Post(id, author: User, imageUrl, caption, audience, location: PostLocation(level, provinceCode, provinceName, wardCode, wardName, placeName, landmarkId, lat, lng), takenAt: Instant, publicVisibleAt: Instant?, reactions, myReaction, sendState)`; `MapBubble`, `Landmark`.
- Dữ liệu giả (`vn.nhom2.lokap.data.fake`): `object SampleData` có `NOW`, `currentUser: User`, `users: List<User>`, `posts`, `landmarks`; `object ProvinceCatalog { val allProvinces: List<Province> }` (đủ 34); `object FakePostStore { val posts: StateFlow<List<Post>> }`.
- Route (`vn.nhom2.lokap.core.navigation.Route`): `Profile(userId: String? = null)` (null = hồ sơ của tôi), `Friends`, `Settings`, `PostDetail(id)`, `Chat(conversationId)`, `RegionAlbum(level, code)`.
- Bản đồ: `feature/map/LokapMap(bubbles: List<MapBubble>, landmarks: List<Landmark>, camera: MapCameraState, onCameraChange, onBubbleClick, onLandmarkClick, modifier)`; `MapCameraState(centerLat, centerLng, zoom)`.
- Component có sẵn (`vn.nhom2.lokap.core.designsystem.component`), dùng đúng chữ ký:
  - `AvatarRing(avatarUrl: String?, displayName: String, modifier, size: Dp = 44.dp, hasNewPost: Boolean = false, onClick: (() -> Unit)? = null)`
  - `GlassChip(label: String, modifier, leadingIcon: ImageVector? = null, selected: Boolean = false, onClick: (() -> Unit)? = null)`
  - `PinkButton(text, onClick, modifier, enabled = true, loading = false, leadingIcon: ImageVector? = null)`; `GhostButton(...)` cùng tham số
  - `RegionCard(code, regionName, photoUrls: List<String>, postCount: Int, visited: Boolean, modifier, stickers = ..., onClick: (() -> Unit)? = null)`
  - `BadgeDot(count: Int, modifier)`; `EmptyState(title, modifier, subtitle: String? = null, actionLabel: String? = null, onActionClick: (() -> Unit)? = null)`; `ErrorState(message, onRetry, modifier)`; `OfflineBanner(modifier)`; `ShimmerBox(modifier, shape)`
- Mẫu đăng ký màn hình: `fun NavGraphBuilder.feedScreen(navController: NavController) { composable<Route.Feed> { ... } }`; màn hình tự `systemBarsPadding()`; điều hướng dùng `launchSingleTop = true`.
- Mẫu bắt buộc mỗi màn: `XxxUiState` (data class bất biến), `XxxAction` (sealed interface), `XxxEffect` (Channel), `XxxViewModel` (StateFlow), `XxxRoute` (viewModel()), `XxxScreen` (không state, có Preview cho từng trạng thái, bọc trong LokapTheme).
- Dữ liệu mẫu mới (lời mời kết bạn, danh sách chặn, cài đặt) để trong file mới `data/fake/SampleSocialData.kt`.

## Phần A — Hồ sơ (`feature/profile/`)
- Header: AvatarRing 96dp, tên, @username; 3 số liệu: "Đã đến X/34 tỉnh" (đếm tỉnh khác nhau trong bài của người đó, dùng ProvinceCatalog), số bài, số bạn.
- Hồ sơ của tôi: nút "Sửa hồ sơ" (bottom sheet đổi tên hiển thị, kiểm tra 2–30 ký tự) và icon Cài đặt (→ Route.Settings). Hồ sơ người khác: nút theo FriendStatus: Kết bạn / Đã gửi lời mời (huỷ được) / Chấp nhận + Từ chối / Bạn bè (menu: Nhắn tin → Chat, Huỷ kết bạn) / Đã chặn (Bỏ chặn).
- 2 tab: "Ảnh" (LazyVerticalGrid 3 cột → PostDetail; xem hồ sơ người khác chỉ thấy bài họ cho phép: bài FRIENDS khi là bạn bè, bài PUBLIC đã hiện) và "Dấu chân" (LokapMap chỉ với bong bóng tỉnh có bài của người đó, bên dưới là hàng RegionCard ngang của các tỉnh đã đến).
- Trạng thái: loading, chưa có bài ("Chưa có khoảnh khắc nào"), lỗi, người dùng không tồn tại.

## Phần B — Bạn bè (`feature/friends/`)
- Ô tìm theo username (lọc trên dữ liệu mẫu, có debounce 300ms). Kết quả hiện nút theo FriendStatus.
- Mục "Lời mời kết bạn" (Chấp nhận / Từ chối), mục "Đã gửi" (Huỷ), mục "Bạn bè (N)" (chạm → Profile, giữ lâu → menu Huỷ kết bạn / Chặn với AlertDialog xác nhận).
- Giới hạn bạn bè tối đa 20 người như Locket: đủ 20 thì nút Kết bạn/Chấp nhận bị khoá và hiện ghi chú.
- Trạng thái: loading, không tìm thấy ("Không có ai tên như vậy"), chưa có bạn ("Rủ bạn bè cài Lokap nhé"), lỗi.

## Phần C — Cài đặt (`feature/settings/`)
- Nhóm "Riêng tư": Mức vị trí mặc định (4 lựa chọn LocationLevel, dạng radio), "Người đã chặn" (danh sách, Bỏ chặn).
- Nhóm "Thông báo": công tắc Ảnh mới từ bạn bè / Cảm xúc / Tin nhắn / Lời mời kết bạn; nếu chưa có quyền POST_NOTIFICATIONS (Android 13+) thì hiện thẻ cảnh báo + nút "Cấp quyền" (mở cài đặt thông báo của app).
- Nhóm "Tài khoản": Đăng xuất (AlertDialog xác nhận), Xoá tài khoản (AlertDialog yêu cầu gõ chữ "XOÁ" mới bật nút xác nhận; giải thích ảnh và tin nhắn sẽ bị xoá vĩnh viễn).
- Cuối trang: phiên bản app (BuildConfig hoặc PackageManager).
- Lưu cài đặt tạm trong ViewModel (chưa có DataStore).

## Định dạng trả lời
- Mỗi file một khối ```kotlin … ```, dòng ĐẦU TIÊN là `// FILE: app/src/main/java/vn/nhom2/lokap/...`. Chỉ tạo file MỚI.
- Mỗi màn có `fun NavGraphBuilder.profileScreen / friendsScreen / settingsScreen(navController: NavController)`.
- Cuối cùng: (1) import + các dòng gọi cần thêm vào AppNavHost; (2) dependency mới nếu có (chỉ tên artifact).
- Quá dài thì dừng ở cuối một file hoàn chỉnh, ghi `CÒN TIẾP`.
