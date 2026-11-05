Bạn là lập trình viên Android senior. Viết code Kotlin (Jetpack Compose + Material 3) cho 3 màn hình của app Lokap: app kiểu Locket (mở ra là camera, chụp gửi bạn bè) kèm bản đồ public gom ảnh theo tỉnh/thành → phường/xã của Việt Nam. Chỉ dark mode, màu nhấn hồng, phong cách GenZ. Chữ hiển thị tiếng Việt có dấu.

## Project hiện có (KHÔNG viết lại các file này, chỉ dùng)
- Package gốc `vn.nhom2.lokap`, minSdk 26, Kotlin 2.2.10, Navigation Compose 2.10 (route @Serializable), Coil 3.3, lifecycle-viewmodel-compose 2.11, icon chỉ dùng gói material-icons-core (Icons.Default.* và Icons.AutoMirrored.Filled.*).
- Theme: `LokapTheme.colors` (background, surface, surfaceHigh, outline, primary, primarySoft, onPrimary, textPrimary, textSecondary, textTertiary, accentLilac, accentLime, accentCyan, accentYellow, error, success), `LokapTheme.spacing` (xs, sm, md, lg, xl, xxl), `LokapTheme.shapes` (cameraFrame, card, sheet, pill). Không hard-code màu. Chữ trên nền hồng dùng `onPrimary` (màu đen).
- Model (`vn.nhom2.lokap.domain.model`): `User(id, username, displayName, avatarUrl: String?)`; `Post(id, author: User, imageUrl, caption, audience, location: PostLocation, takenAt: Instant, publicVisibleAt: Instant?, reactions, myReaction, sendState: PostSendState)`; `enum class PostSendState { PENDING, SENT, FAILED }`; `Conversation(id, friend: User, lastMessagePreview: String, lastMessageAt: Instant, unreadCount: Int)`; `Message(id, conversationId, senderId, body: String?, replyToPost: Post?, attachedPost: Post?, createdAt: Instant, sendState: PostSendState, seen: Boolean)`; `enum class NotificationType { NEW_POST, REACTION, FRIEND_REQUEST, FRIEND_ACCEPTED, NEW_MESSAGE, POST_HIDDEN }`; `AppNotification(id, type, actor: User?, postId: String?, text, createdAt: Instant, isRead: Boolean)`.
- Dữ liệu giả (`vn.nhom2.lokap.data.fake`): `object SampleData` có `NOW`, `currentUser`, `users`, `posts`, `conversations`, `messages`, `notifications`; `object FakePostStore { val posts: StateFlow<List<Post>> }`. Hàm có sẵn `formatTimeAgo(instant: Instant, now: Instant, isDelayedShot: Boolean = false): String` (package `vn.nhom2.lokap.core.common`).
- Route (`vn.nhom2.lokap.core.navigation.Route`): `Inbox`, `Chat(conversationId: String)`, `Notifications`, `PostDetail(id)`, `Profile(userId: String?)`, `Friends`, `Camera`.
- Component có sẵn (`vn.nhom2.lokap.core.designsystem.component`), dùng đúng chữ ký:
  - `AvatarRing(avatarUrl: String?, displayName: String, modifier, size: Dp = 44.dp, hasNewPost: Boolean = false, onClick: (() -> Unit)? = null)`
  - `BadgeDot(count: Int, modifier)`; `GlassChip(label, modifier, leadingIcon = null, selected = false, onClick = null)`
  - `PinkButton(text, onClick, modifier, enabled = true, loading = false, leadingIcon = null)`; `GhostButton(...)` cùng tham số
  - `EmptyState(title, modifier, subtitle = null, actionLabel = null, onActionClick = null)`; `ErrorState(message, onRetry, modifier)`; `OfflineBanner(modifier)`; `ShimmerBox(modifier, shape)`
- Mẫu đăng ký màn hình: `fun NavGraphBuilder.feedScreen(navController: NavController) { composable<Route.Feed> { ... } }`; màn hình tự `systemBarsPadding()` (riêng ô nhập chat dùng thêm `imePadding()`); điều hướng dùng `launchSingleTop = true`.
- Mẫu bắt buộc mỗi màn: `XxxUiState` (data class bất biến), `XxxAction` (sealed interface), `XxxEffect` (Channel), `XxxViewModel` (StateFlow), `XxxRoute` (viewModel()), `XxxScreen` (không state, có Preview cho từng trạng thái, bọc trong LokapTheme).
- Kho dữ liệu chat dùng chung tạo mới ở `data/fake/FakeChatStore.kt` (object giữ `MutableStateFlow` của conversations và messages, khởi tạo từ SampleData). Không sửa SampleData.

## Quy tắc nghiệp vụ
- Chỉ nhắn tin giữa bạn bè. Ảnh gửi trong chat là bài Locket riêng (audience DIRECT) cho đúng người đó.
- "Đang nhập…" và "Đã xem" không ghi vào kho dữ liệu, chỉ là trạng thái giả lập trong ViewModel.

## Phần A — Tin nhắn (`feature/inbox/`)
- Danh sách cuộc trò chuyện: AvatarRing, tên, tin cuối (tin có ảnh hiện "📷 Đã gửi một ảnh"), thời gian `formatTimeAgo`, BadgeDot số chưa đọc; cuộc có tin chưa đọc thì tên in đậm. Sắp xếp mới nhất trước.
- Nút "Soạn mới" ở thanh trên → bottom sheet chọn bạn (danh sách users trừ tôi) → mở Chat.
- Vuốt sang trái một dòng → nút "Đánh dấu đã đọc".
- Trạng thái: loading, trống ("Chưa có tin nhắn. Trả lời một bức ảnh để bắt đầu"), lỗi, offline.

## Phần B — Chat 1-1 (`feature/chat/`)
- Thanh trên: nút quay lại, AvatarRing + tên bạn (chạm → Profile), trạng thái "Đang nhập…" khi có.
- Danh sách tin (LazyColumn reverseLayout, nhóm theo ngày với nhãn "Hôm nay", "Hôm qua", "dd/MM"): bong bóng của tôi nền primary chữ onPrimary căn phải; của bạn nền surfaceHigh căn trái. Tin `replyToPost` có ảnh thu nhỏ 48dp + chữ "Trả lời ảnh" phía trên bong bóng; tin `attachedPost` hiện như thẻ ảnh vuông 220dp bo 28dp kiểu Locket (chạm → PostDetail). Dưới tin cuối của tôi: "Đã xem" hoặc "Đã gửi". Tin PENDING mờ đi + "Đang gửi…", FAILED có nút Thử lại.
- Ô nhập dưới cùng: nút camera (tạm điều hướng Route.Camera, ghi chú TODO tuần 4: chế độ gửi riêng), ô chữ tối đa 1000 ký tự, nút gửi chỉ bật khi có chữ. Gửi: thêm tin PENDING vào FakeChatStore, sau 800ms thành SENT; sau 2 giây giả lập bạn "Đang nhập…" 1,5 giây rồi trả lời một câu ngẫu nhiên từ danh sách cố định (dùng index xoay vòng, không dùng Random).
- Mở chat thì đặt unreadCount về 0.
- Trạng thái: loading, cuộc trò chuyện mới chưa có tin ("Gửi lời chào đến <tên> 👋"), không còn là bạn bè (khoá ô nhập, ghi "Bạn không thể nhắn tin với người này"), lỗi, offline.

## Phần C — Thông báo (`feature/notifications/`)
- Danh sách nhóm "Mới" (chưa đọc) và "Trước đó"; mỗi dòng: AvatarRing của actor (hoặc icon cho POST_HIDDEN), câu thông báo, thời gian; dòng chưa đọc có chấm hồng bên trái.
- Chạm: NEW_POST/REACTION → PostDetail; FRIEND_REQUEST/FRIEND_ACCEPTED → Friends; NEW_MESSAGE → Inbox; POST_HIDDEN → PostDetail. Chạm thì đánh dấu đã đọc.
- Nút "Đọc tất cả" ở thanh trên. Dòng FRIEND_REQUEST có 2 nút nhỏ Chấp nhận / Từ chối ngay trong dòng.
- Trạng thái: loading, trống ("Chưa có thông báo nào"), lỗi.

## Định dạng trả lời
- Mỗi file một khối ```kotlin … ```, dòng ĐẦU TIÊN là `// FILE: app/src/main/java/vn/nhom2/lokap/...`. Chỉ tạo file MỚI.
- Mỗi màn có `fun NavGraphBuilder.inboxScreen / chatScreen / notificationsScreen(navController: NavController)`.
- Cuối cùng: (1) import + các dòng gọi cần thêm vào AppNavHost; (2) dependency mới nếu có (chỉ tên artifact).
- Quá dài thì dừng ở cuối một file hoàn chỉnh, ghi `CÒN TIẾP`.
