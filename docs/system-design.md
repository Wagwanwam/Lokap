# Lọc kẹt — Cấu trúc hệ thống v1

Nov 5, 2026 · @Wang

Lọc kẹt là app Android mở ra là camera như Locket, thêm bản đồ public gom ảnh theo tỉnh/thành rồi phường/xã. Dựng bằng Kotlin + Jetpack Compose, backend Supabase. Gemini/Antigravity dựng frontend theo tài liệu này; Claude dựng khung backend.

## Quyết định đã chốt

| Hạng mục | Quyết định | Ghi chú |
| --- | --- | --- |
| Nền tảng | Android, Kotlin + Jetpack Compose | Project Lokap, package vn.nhom2.lokap, minSdk 26, targetSdk 37, AGP 9.3, chỉ dọc (portrait) |
| Kiến trúc | MVVM + luồng dữ liệu một chiều (UDF), Hilt cho DI | Repository là ranh giới giữa UI và dữ liệu |
| Giao diện | Chỉ dark mode, đen + hồng | Font Be Vietnam Pro (hỗ trợ tiếng Việt đầy đủ) |
| Camera | CameraX, ZERO\_SHUTTER\_LAG nếu máy hỗ trợ, không thì MINIMIZE\_LATENCY | Màn hình mở app là camera |
| Bản đồ | VietMap SDK (MapLibre) | Chỉ khởi tạo khi mở tab Map |
| Backend | Supabase: Auth, Postgres + PostGIS, Storage, Realtime, Edge Functions | Gói Free; Claude dựng khung |
| Cache máy | Room (feed, widget, bài chờ gửi) + Coil (ảnh) |  |
| Thông báo đẩy | FCM, gửi từ Supabase Edge Function | FCM miễn phí, cần project Firebase chỉ để lấy FCM |
| Kiểu đăng | Bạn bè (tất cả bạn), Riêng 1 người (gửi từ chat), Public | Một bài chọn một kiểu |
| Mức vị trí | Địa điểm cụ thể / Phường-xã / Tỉnh-thành (mặc định) / Chỉ "Việt Nam" | Bài chỉ vào bong bóng đúng mức đã chọn |
| Hẹn giờ public | Ngay / 1 / 3 / 7 ngày | Bạn bè thấy ngay; người lạ thấy sau, nhãn "chụp 3 ngày trước" |
| Đơn vị hành chính | Tỉnh/thành (34) → phường/xã | Cấp quận/huyện đã bỏ từ 01/07/2025 |
| Địa danh | Wikidata lọc + nhóm tự chọn, đủ 34 tỉnh | VietMap Place cho gắn tag quán, trường |
| Newsfeed | Bạn bè trước → gần tôi → địa danh nổi bật | Công thức chấm điểm, chưa dùng AI |
| Kiểm duyệt | Báo cáo + chặn + tự ẩn khi ≥ 3 báo cáo |  |
| Chia việc | Gemini/Antigravity dựng frontend, Claude dựng backend và review |  |
| Nhắn tin 1-1 | Giữ; chỉ giữa bạn bè. Có chữ, trả lời từ ảnh, đã xem, số chưa đọc, đang nhập | Supabase Realtime; "đang nhập" dùng broadcast, không ghi DB |
| Gửi ảnh trong chat | Chụp kiểu Locket, thành bài riêng cho đúng người đó; hiện trong chat, Feed Bạn bè và widget của người nhận | audience = direct |
| Ảnh từ thư viện | Không cho | Chỉ ảnh chụp trực tiếp, nên vị trí luôn là nơi chụp thật |

## Kiến trúc tổng thể

&#91;embedded content: kiến trúc · app, Supabase, dịch vụ ngoài\]

App chỉ giữ anon key và key tile bản đồ. Mọi quyết định ai xem được gì chạy trong Postgres; key VietMap và quyền gửi FCM nằm trong Edge Functions. Trigger trên bảng posts gọi send-push khi có bài mới.

## Màn hình và điều hướng

&#91;embedded content: luồng màn hình · 17 màn hình\]

Đã đăng nhập thì Splash vào thẳng Camera. Soạn bài gửi xong quay về Camera; vuốt xuống từ Feed cũng về Camera. Từ Feed, chạm chip vị trí mở Album khu vực, ô Trả lời mở Chat 1-1. Trong Chat, nút camera chụp và gửi riêng cho người đó.

## Cấu trúc thư mục frontend

Một module `app`, chia theo tính năng. Mỗi thành viên sở hữu thư mục `feature/` của mình; file dùng chung (`core/`, `domain/`, `AppNavHost`) chỉ nhóm trưởng sửa để tránh xung đột merge.

```text
app/src/main/java/vn/nhom2/lokap/
├── LokapApp.kt                # Application, @HiltAndroidApp
├── MainActivity.kt            # edge-to-edge, setContent { LokapTheme { AppNavHost() } }
├── core/
│   ├── designsystem/
│   │   ├── theme/             # Color.kt, Type.kt, Shape.kt, Spacing.kt, Gradient.kt, Theme.kt
│   │   └── component/         # ShutterButton, PinkButton, GlassChip, AvatarRing, RegionCard,
│   │                          # LandmarkBubble, PhotoCard, EmojiBar, EmptyState, ErrorState, Shimmer
│   ├── navigation/            # Route.kt (sealed, @Serializable), AppNavHost.kt
│   ├── common/                # UiText, AppResult, TimeAgo.kt ("chụp 3 ngày trước"), DispatcherProvider
│   ├── camera/                # CameraManager: bind CameraX, chọn capture mode, lật camera, flash
│   ├── image/                 # ImageProcessor: xoay theo EXIF, resize 1080, nén, xoá EXIF
│   ├── location/              # LocationClient: Fused, timeout, kiểm tra mock location
│   └── permission/            # rememberPermissionState, PermissionRationale
├── domain/
│   ├── model/                 # User, Post, Audience, LocationLevel, PublishDelay, Region,
│   │                          # RegionLevel, MapBubble, Landmark, Place, Reaction, Notification, Conversation, Message
│   └── repository/            # interface: AuthRepository, PostRepository, FeedRepository,
│                              # MapRepository, FriendRepository, LandmarkRepository,
│                              # PlaceRepository, NotificationRepository, ReportRepository, ChatRepository
├── data/
│   ├── fake/                  # FakeXxxRepository + SampleData.kt (34 tỉnh, ảnh mẫu, bạn bè mẫu)
│   ├── remote/                # SupabaseModule, dto/, mapper/, RPC wrappers
│   ├── local/                 # Room: LokapDb, dao/, entity/
│   ├── repository/            # SupabaseXxxRepository (bản thật)
│   └── worker/                # UploadPostWorker, RefreshWidgetWorker
├── feature/
│   ├── onboarding/            # giới thiệu + xin quyền
│   ├── auth/                  # Login, Register, ForgotPassword, SetupProfile
│   ├── camera/                # CameraScreen (màn hình chính)
│   ├── composer/              # ComposerScreen + LocationLevelPicker, PlaceSearchSheet, DelayPicker
│   ├── feed/                  # FeedScreen: tab Bạn bè + tab Khám phá, PostDetail
│   ├── map/                   # MapScreen, MapBubbleLayer, MapFilterBar
│   ├── region/                # RegionAlbumScreen (thẻ khu vực)
│   ├── landmark/              # LandmarkScreen
│   ├── profile/               # ProfileScreen, EditProfile, đã đến X/34
│   ├── friends/               # danh sách, lời mời, tìm theo username
│   ├── chat/                  # Tin nhắn, Chat 1-1, gửi ảnh riêng
│   ├── notifications/
│   ├── report/                # ReportSheet, BlockDialog
│   └── settings/              # mức vị trí mặc định, người đã chặn, đăng xuất, xoá tài khoản
├── widget/                    # Glance: LatestFriendPhotoWidget + Receiver
└── di/                        # RepositoryModule: bind Fake hoặc Supabase theo BuildConfig.USE_FAKE_DATA
```

Thư viện chính: Compose BOM, Material 3, Navigation Compose (route type-safe), Hilt, Coroutines/Flow, CameraX (+ camera-compose), Coil 3, Room, WorkManager, Glance, Play Services Location, supabase-kt, Ktor client, kotlinx.serialization, VietMap Android SDK, Firebase Messaging.

## Khuôn mẫu một màn hình

Mọi màn hình theo cùng một khuôn, để code do Gemini sinh ra đồng nhất và dễ review.

1. `XxxUiState`: một data class bất biến chứa mọi thứ màn hình cần vẽ, gồm cả `isLoading`, `error`.
2. `XxxAction`: sealed interface các thao tác người dùng (bấm, kéo, nhập).
3. `XxxEffect`: sự kiện một lần (điều hướng, snackbar, rung), gửi qua `Channel`.
4. `XxxViewModel`: nhận Action, gọi Repository, phát `StateFlow<UiState>`.
5. `XxxRoute`: composable có trạng thái, lấy ViewModel bằng `hiltViewModel()`, thu Effect.
6. `XxxScreen`: composable không trạng thái, chỉ nhận `state` và `onAction`; có `@Preview` cho đủ trạng thái.

```kotlin
data class FeedUiState(
    val tab: FeedTab = FeedTab.FRIENDS,
    val posts: List<PostUi> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val error: UiText? = null,
)

sealed interface FeedAction {
    data class SelectTab(val tab: FeedTab) : FeedAction
    data class React(val postId: String, val emoji: String) : FeedAction
    data object LoadMore : FeedAction
    data object Refresh : FeedAction
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepo: FeedRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()
    fun onAction(action: FeedAction) { /* ... */ }
}

@Composable
fun FeedRoute(vm: FeedViewModel = hiltViewModel(), onOpenPost: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    FeedScreen(state = state, onAction = vm::onAction, onOpenPost = onOpenPost)
}
```

Fake data là điều kiện để làm frontend trước. Mỗi Repository có bản `Fake` trả dữ liệu mẫu (có `delay(400)` để thấy trạng thái loading, và cờ bật lỗi giả để test trạng thái error). Khi backend xong chỉ đổi `USE_FAKE_DATA = false`, UI không sửa dòng nào.

## Design system đen–hồng

Nền đen, hồng là màu nhấn duy nhất cho hành động chính; bốn màu phụ chỉ dùng cho thẻ khu vực, sticker và bong bóng. Tỉ lệ tương phản đo trên nền #0B0B0D; chữ thường cần ≥ 4.5:1.

| Token | Mã màu | Tương phản | Dùng cho |
| --- | --- | --- | --- |
| background | #0B0B0D | — | Nền màn hình |
| surface | #141417 | — | Thẻ, bottom sheet |
| surfaceHigh | #1E1E23 | — | Chip, ô nhập, dialog |
| outline | #2C2C33 | — | Viền mảnh, đường kẻ |
| primary (hồng) | #FF4D8D | 6.27:1 | Nút chụp, nút chính, tab đang chọn |
| primarySoft | #FF8FB8 | 9.25:1 | Link, chữ nhấn trên nền tối |
| onPrimary | #0B0B0D | 6.27:1 | Chữ trên nền hồng. Không dùng trắng: chỉ 3.14:1 |
| textPrimary | #F5F5F7 | 18.06:1 | Chữ chính |
| textSecondary | #A1A1AA | 7.67:1 | Chữ phụ, thời gian |
| textTertiary | #71717A | 4.07:1 | Chỉ chữ ≥ 18sp hoặc trạng thái tắt |
| accentLilac | #B98CFF | 7.73:1 | Thẻ khu vực, gradient |
| accentLime | #C6FF4D | 16.68:1 | Sticker, huy hiệu "đã đến" |
| accentCyan | #4DE1FF | 12.67:1 | Bong bóng bạn bè |
| accentYellow | #FFD84D | 14.22:1 | Bong bóng địa danh nổi tiếng |
| error | #FF6B6B | 7.09:1 | Lỗi |
| success | #4ADE80 | 11.29:1 | Thành công, đã gửi |

- **Font:** Be Vietnam Pro. Display 32/40 ExtraBold cho tên khu vực trên thẻ; Title 20/28 SemiBold; Body 15/22 Regular; Label 12/16 Medium.
- **Icon:** Material Symbols Rounded, nét 2dp, kích thước 24dp.
- **Hình khối:** khung camera bo 40dp (như Locket), thẻ 28dp, chip và nút bo tròn hoàn toàn. Khoảng cách theo bước 4dp: 4, 8, 12, 16, 24, 32.
- **Chuyển động:** spring cho nút chụp và thẻ (dampingRatio 0.6); rung nhẹ khi chụp và khi thả cảm xúc; màn hình flash trắng 80ms khi chụp.
- **Thẻ khu vực kiểu GenZ (RegionCard):** gradient 2 màu chọn cố định theo mã tỉnh (cùng tỉnh luôn cùng màu); tên khu vực cỡ lớn; 3 ảnh xếp chồng nghiêng ±6° như polaroid; huy hiệu số ảnh; 1–2 sticker emoji; con dấu "ĐÃ ĐẾN" màu lime nếu người xem từng đăng ở đó.
- **Bong bóng địa danh (LandmarkBubble):** viền vàng dày 3dp, ảnh bìa tròn, nhãn tên phía dưới, lớn hơn bong bóng thường 1.3 lần.
- **Edge-to-edge:** targetSdk 35 trở lên bắt buộc vẽ tràn viền; mọi màn hình xử lý `WindowInsets` (tai thỏ, thanh điều hướng).

## Đặc tả các màn hình chính

Mỗi màn hình phải có đủ các trạng thái ở cột thứ ba, kể cả khi dùng fake data. Đây cũng là danh sách test case UI cho tuần 5.

| Màn hình | Thành phần chính | Trạng thái bắt buộc | Thao tác |
| --- | --- | --- | --- |
| Camera (màn hình chính) | Thanh trên: avatar (→ Hồ sơ), chip "Bạn bè · N" (→ Bạn bè), nút bản đồ, nút tin nhắn có badge số chưa đọc. Khung preview vuông bo 40dp. Thanh dưới: flash, nút chụp hồng 80dp, lật camera. Không có nút thư viện ảnh. Gợi ý "Vuốt lên xem bài" | Chưa cấp quyền camera; từ chối vĩnh viễn (nút mở Cài đặt); camera đang mở; camera lỗi | Chạm chụp; vuốt lên → Feed; chạm đúp → lật camera; chụm 2 ngón → zoom |
| Soạn bài (Composer) | Ảnh vừa chụp, ô caption phủ trên ảnh (≤ 60 ký tự). Công tắc Đối tượng: Bạn bè / Public. Chip Mức vị trí (4 mức), chip Gắn địa điểm (mở PlaceSearchSheet), chip Hẹn giờ (chỉ hiện khi Public). Nút Gửi hồng, nút Huỷ | Đang lấy vị trí; không có GPS (chỉ cho mức "Việt Nam"); quyền vị trí gần đúng (khoá mức Địa điểm cụ thể và Phường); phát hiện vị trí giả (khoá Public chính xác) | Gửi → quay về Camera ngay, bài hiện "Đang gửi…" trong Feed |
| Feed | Tab Bạn bè: pager dọc toàn màn hình, mỗi trang một ảnh như Locket. Tab Khám phá: danh sách thẻ, nhóm "Gần bạn" rồi "Địa danh nổi bật". Mỗi bài: ảnh, avatar + tên, chip vị trí, nhãn thời gian, thanh emoji, ô Trả lời (mở Chat), menu ⋮ (Báo cáo, Chặn) | Loading (shimmer); trống ("Chưa có bài, rủ bạn bè vào"); offline (banner + dữ liệu cache); lỗi; bài đang gửi; bài gửi lỗi (nút Thử lại) | Vuốt xuống → về Camera; kéo để làm mới; chạm chip vị trí → Album khu vực |
| Bản đồ | Bản đồ VietMap nền tối; lớp bong bóng; thanh lọc trên cùng: Tất cả / Bạn bè / Public, Thời gian (7 ngày / 30 ngày / Tất cả); nút về vị trí của tôi; bottom sheet xem trước thẻ khu vực | Đang tải bong bóng; vùng không có bài; lỗi tải bản đồ; offline | Chạm bong bóng → zoom vào hoặc mở thẻ; chạm bong bóng địa danh → màn hình Địa danh |
| Album khu vực | RegionCard làm header (gradient, tên, số ảnh, con dấu ĐÃ ĐẾN); lưới ảnh 3 cột; lọc Bạn bè / Public | Loading; trống; phân trang | Chạm ảnh → Chi tiết bài; nút chia sẻ thẻ (xuất ảnh thẻ) |
| Địa danh | Ảnh bìa + ghi nguồn Wikimedia, tên, tỉnh, mô tả ngắn, lưới ảnh người dùng chụp tại đó | Loading; chưa có ảnh nào ("Hãy là người đầu tiên") | Chạm ảnh → Chi tiết bài |
| Hồ sơ | Avatar vành hồng, tên, @username; thống kê "Đã đến X/34 tỉnh", số bài, số bạn; tab Lưới ảnh / Bản đồ dấu chân | Loading; chưa có bài; hồ sơ người khác (nút Kết bạn / Đã gửi / Bạn bè) | Sửa hồ sơ; xoá bài của mình; mở Cài đặt |
| Bạn bè | Ô tìm theo username; lời mời đến; lời mời đã gửi; danh sách bạn | Không tìm thấy; đã là bạn; đã chặn | Kết bạn, chấp nhận, từ chối, huỷ kết bạn |
| Thông báo | Danh sách: ảnh mới từ bạn, cảm xúc, lời mời, bài bị ẩn do báo cáo | Trống; chưa đọc / đã đọc | Chạm → mở đúng nơi |
| Đăng nhập / Đăng ký | Email + mật khẩu, Google; đăng ký xong → đặt username + avatar | Email sai định dạng; mật khẩu < 8 ký tự; username trùng; sai mật khẩu; mất mạng |  |
| Cài đặt | Mức vị trí mặc định; người đã chặn; quyền thông báo; đăng xuất; xoá tài khoản | Xác nhận trước khi xoá tài khoản |  |
| Widget | Ảnh mới nhất của bạn bè, tên + thời gian; cỡ 2x2 và 4x4 | Chưa đăng nhập; chưa có ảnh | Chạm → mở bài đó trong app |
| Tin nhắn | Danh sách cuộc trò chuyện: avatar, tên, tin cuối (ảnh thì hiện "Đã gửi một ảnh"), giờ, badge số chưa đọc; chỉ bạn bè | Loading; trống ("Chưa có tin nhắn"); offline | Chạm → Chat 1-1; nút soạn mới → chọn bạn |
| Chat 1-1 | Bong bóng tin (của mình nền hồng, chữ đen); tin trả lời kèm ảnh thu nhỏ của bài; ảnh gửi riêng hiện như thẻ Locket; dòng "Đang nhập…"; "Đã xem" dưới tin cuối; ô nhập + nút camera + nút gửi | Loading; tin đang gửi; tin gửi lỗi (Thử lại); đã huỷ kết bạn hoặc bị chặn (khoá ô nhập); offline | Gửi chữ; nút camera → Camera chế độ gửi riêng → Soạn bài khoá người nhận; chạm ảnh → Chi tiết bài |

## Bản đồ: bong bóng theo mức zoom

Một bài chỉ xuất hiện ở mức chi tiết bằng hoặc thô hơn mức người đăng đã chọn. Server gom sẵn và trả về danh sách bong bóng, app không tự đếm.

| Mức vị trí của bài | Zoom toàn quốc (< 8) | Zoom tỉnh (8–12) | Zoom gần (≥ 13) |
| --- | --- | --- | --- |
| Địa điểm cụ thể | Đếm vào bong bóng tỉnh | Đếm vào bong bóng phường/xã | Ghim riêng tại địa điểm, kèm tên quán/trường |
| Phường/xã | Đếm vào bong bóng tỉnh | Đếm vào bong bóng phường/xã | Vẫn nằm trong bong bóng phường, đặt ở tâm phường |
| Tỉnh/thành | Đếm vào bong bóng tỉnh | Bong bóng "Khắp \<tỉnh>" đặt ở tâm tỉnh | Như cột bên trái |
| Chỉ "Việt Nam" | Không lên bản đồ | Không lên bản đồ | Không lên bản đồ; chỉ ở Feed và Hồ sơ |

- **Bong bóng địa danh** (vàng) hiện từ zoom 8 trở lên, kể cả khi chưa có bài nào, để bản đồ không trống lúc mới ra mắt. Bài được tính vào địa danh khi gắn tag địa danh đó hoặc ở mức Địa điểm cụ thể trong bán kính 300 m.
- **Mỗi bong bóng** gồm: số bài, ảnh bìa (bài nhiều cảm xúc nhất), viền cyan nếu có bài của bạn bè.
- **Số đếm phải theo luật hiển thị:** bài hẹn giờ chưa tới giờ và bài của người bị chặn không được tính với người lạ. Nếu tính, số trên bong bóng tăng lên cũng đủ làm lộ "ai đó vừa ở đây".
- **Ngưỡng zoom** (8, 13) là giá trị khởi điểm, chỉnh lại khi test trên máy thật.

## Luồng đăng bài

Người dùng chỉ chờ đến bước 4; từ bước 5 mọi việc chạy nền, nên app luôn thấy "đăng xong ngay".

1. **Chụp** (CameraX): rung nhẹ + flash trắng; ảnh gốc ghi vào bộ nhớ tạm của app.
2. **Xử lý ảnh** trên `Dispatchers.Default`: xoay theo EXIF, lật nếu camera trước, cắt vuông, resize 1080×1080, nén JPEG 80, ghi file mới không có EXIF. Song song: lấy vị trí (tối đa 5 giây) và gọi `reverse-geocode` để có tên tỉnh, phường.
3. **Soạn bài**: chọn đối tượng, mức vị trí, địa điểm, hẹn giờ, caption.
4. **Bấm Gửi**: app tạo `postId` (UUID) ngay trên máy, lưu bài vào Room với trạng thái `PENDING`, quay về Camera.
5. **UploadPostWorker** (WorkManager, cần mạng, tự thử lại): tải ảnh lên Storage tại `posts/<userId>/<postId>.jpg` (ghi đè được, không tạo bản trùng).
6. **Gọi RPC `create_post`** với cùng `postId`: server tự tính tâm phường/tỉnh để hiển thị, lưu toạ độ gốc vào bảng riêng chỉ chủ bài đọc được, và tự tính `public_visible_at = now() + delay` theo giờ server.
7. **Room** chuyển bài sang `SENT`. Nếu lỗi sau 5 lần thử → `FAILED`, Feed hiện nút Thử lại.
8. **Trigger trên bảng posts** gọi Edge Function `send-push`, gửi FCM data message cho bạn bè của người đăng (kể cả khi bài Public hẹn giờ).
9. **Máy bạn bè** nhận push → lưu bài vào Room → cập nhật widget + hiện thông báo.

Gửi ảnh từ Chat dùng đúng luồng này với audience = direct và người nhận cố định. Ở bước 6, `create_post` ghi thêm post\_recipients và một tin nhắn trỏ tới bài; bước 8 chỉ push cho người nhận.

## Hợp đồng dữ liệu với backend

Phần này là bản nháp để frontend và backend khớp nhau; ERD chi tiết làm ở tuần 3. Domain model trong app phải bám đúng các trường dưới đây.

| Bảng | Trường chính | Ai đọc được |
| --- | --- | --- |
| profiles | id, username (duy nhất), display\_name, avatar\_path, default\_location\_level, created\_at | Mọi người đã đăng nhập |
| friendships | requester\_id, addressee\_id, status (pending, accepted), created\_at | Hai người trong quan hệ |
| posts | id, author\_id, image\_path, caption, audience (friends, direct, public), location\_level (place, ward, province, country), province\_code, ward\_code, place\_id, landmark\_id, display\_point, public\_visible\_at, taken\_at, status (active, hidden, deleted), reaction\_count | Theo luật hiển thị bên dưới |
| post\_private\_locations | post\_id, exact\_point, accuracy\_m | Chỉ chủ bài |
| reactions | post\_id, user\_id, emoji, created\_at | Ai xem được bài |
| provinces | code, name, centroid (34 dòng) | Mọi người |
| wards | code, province\_code, name, centroid (\~3.300 dòng) | Mọi người |
| landmarks | id, name, province\_code, ward\_code, point, category, cover\_url, cover\_attribution, wikidata\_id, popularity | Mọi người |
| places | id, vietmap\_ref, name, address, point | Mọi người |
| reports | post\_id, reporter\_id (duy nhất theo cặp), reason, created\_at | Chỉ người báo cáo |
| blocks | blocker\_id, blocked\_id | Chỉ người chặn |
| notifications | id, user\_id, type, payload, read\_at, created\_at | Chỉ người nhận |
| device\_tokens | user\_id, fcm\_token, updated\_at | Chỉ chủ token |
| post\_recipients | post\_id, user\_id | Chủ bài và người nhận |
| conversations | id, user\_a, user\_b (user\_a < user\_b, duy nhất theo cặp), last\_message\_at | Hai người trong cuộc trò chuyện |
| messages | id (UUID tạo trên máy), conversation\_id, sender\_id, body, post\_id (tin trả lời hoặc ảnh gửi riêng), created\_at | Hai người trong cuộc trò chuyện |
| conversation\_reads | conversation\_id, user\_id, last\_read\_at | Hai người trong cuộc trò chuyện |

**Luật hiển thị (viết một lần trong Postgres, mọi RPC dùng chung):** người xem V thấy bài P khi P đang `active`, hai bên không chặn nhau, và thỏa một trong bốn: V là tác giả; P gửi cho bạn bè hoặc Public và V là bạn của tác giả; P là bài riêng và V là người nhận; P là Public và `public_visible_at ≤ now()`.

| RPC / Edge Function | Đầu vào | Đầu ra |
| --- | --- | --- |
| `create_post` | postId, imagePath, caption, audience, level, lat, lng, placeId?, delayDays | Post |
| `get_friend_feed` | cursor | Danh sách bài bạn bè, mới nhất trước |
| `get_discover_feed` | wardCode, provinceCode của người xem (không lưu), cursor | Danh sách bài đã xếp hạng |
| `get_map_bubbles` | bbox, zoom, filter, timeRange | Danh sách MapBubble (loại, mã vùng, toạ độ, số bài, ảnh bìa, có bạn bè) |
| `get_region_album` | level, code, filter, cursor | Thông tin vùng + danh sách bài |
| `get_landmark` | landmarkId, cursor | Landmark + danh sách bài |
| `report_post`, `block_user`, `react` | … | Kết quả |
| Edge `reverse-geocode` | lat, lng | provinceCode, wardCode, tên (giấu key VietMap) |
| Edge `place-search` | từ khoá, lat, lng | Danh sách Place |
| Edge `send-push` | gọi từ trigger | Gửi FCM |
| get\_conversations | cursor | Cuộc trò chuyện + tin cuối + số chưa đọc |
| get\_messages | conversationId, cursor | Tin nhắn, mới nhất trước |
| send\_message | messageId, friendId, body?, postId? | Message; tự tạo conversation nếu chưa có; từ chối nếu không còn là bạn hoặc bị chặn |
| mark\_read | conversationId | Cập nhật last\_read\_at |
| Realtime kênh chat:\<conversationId> | Đăng ký khi mở Chat, huỷ khi rời | Tin mới (postgres\_changes) + "đang nhập" (broadcast) |

Ảnh lưu ở bucket riêng tư `posts`, app lấy signed URL. Phân trang dùng cursor (created\_at, id), không dùng offset.

## Công thức xếp hạng newsfeed

Tab Bạn bè xếp theo thời gian, mới nhất trước, giống Locket. Tab Khám phá chấm điểm từng bài public rồi xếp giảm dần:

```latex
\text{score} = W_{\text{gần}} \cdot W_{\text{địa danh}} \cdot \frac{c + 1}{(h + 2)^{1.5}}
```

- c = số cảm xúc; h = số giờ từ lúc bài hiện public. Bài càng cũ điểm càng giảm, nhiều cảm xúc thì giảm chậm hơn.
- W\_gần = 3 nếu cùng phường/xã với người xem, 2 nếu cùng tỉnh, 1 nếu khác tỉnh. App gửi wardCode và provinceCode hiện tại kèm request; server dùng để xếp hạng rồi bỏ, không lưu.
- W\_địa danh = 1.5 nếu bài gắn địa danh nổi tiếng, ngược lại 1.
- Tab Khám phá có hai nhóm: "Gần bạn" (bài cùng tỉnh) rồi "Địa danh nổi bật" (bài gắn địa danh trên cả nước).
- Đa dạng: mỗi trang tối đa 2 bài của cùng một người.
- Phân trang ổn định: trang đầu ghi lại mốc thời gian `snapshot_at`, các trang sau tính điểm theo mốc đó, nên bài không bị lặp hoặc nhảy giữa hai trang.
- Cá nhân hoá sau này (chưa làm trong 6 tuần): cộng điểm cho loại địa điểm và khu vực người xem hay thả cảm xúc. AI chỉ có ích khi đã có nhiều dữ liệu tương tác.

## Chống lag

Mục tiêu: mở app đến lúc thấy preview camera dưới 1,5 giây trên máy tầm trung; cuộn feed không giật. Đo trên bản release, không đo trên bản debug.

- [ ] CameraX bind theo lifecycle; chỉ khởi tạo Preview + ImageCapture, không bật ImageAnalysis
- [ ] Preview độ phân giải 1080p, không dùng 4K
- [ ] Xoay, resize, nén ảnh trên `Dispatchers.Default`; đọc ghi file trên `Dispatchers.IO`
- [ ] Upload qua WorkManager, UI không chờ mạng
- [ ] Coil 3 cho mọi ảnh; `size` đúng khung hiển thị; cache key là storage path, không phải signed URL
- [ ] Chỉ khởi tạo VietMap khi mở tab Bản đồ; Supabase client tạo lười qua Hilt
- [ ] `LazyColumn`/`VerticalPager` có `key` và `contentType`; UiState dùng `ImmutableList`
- [ ] Không đọc state trong lúc vẽ nếu không cần: dùng lambda modifier (`Modifier.offset { }`, `graphicsLayer { }`)
- [ ] SplashScreen API; Baseline Profile sinh bằng module Macrobenchmark (tuần 5)
- [ ] R8 bật cho release (`isMinifyEnabled = true`, `isShrinkResources = true`)
- [ ] Kiểm tra bằng Layout Inspector (số lần recompose) và Android Studio Profiler (RAM khi cuộn 100 ảnh)

## Rà soát lỗi tiềm ẩn

Tám lỗi mức Cao làm lộ vị trí hoặc hỏng buổi demo; phải xử lý trước tuần 4. Bảng xếp theo mức độ, cao trước.

| Mức | Khu vực | Lỗi có thể xảy ra | Hậu quả | Cách phòng |
| --- | --- | --- | --- | --- |
| Cao | Riêng tư | File ảnh còn EXIF GPS | Chọn mức tỉnh vẫn lộ toạ độ chính xác | Ghi file mới không EXIF trước khi upload; test bằng công cụ đọc EXIF |
| Cao | Riêng tư | Toạ độ gốc nằm chung bảng posts | RLS chặn theo dòng, không theo cột; ai xem được bài là đọc được toạ độ | Bảng `post_private_locations` riêng, chỉ chủ bài đọc |
| Cao | Riêng tư | Lọc bài hẹn giờ ở phía app | Gọi thẳng API là thấy bài chưa tới giờ | Lọc trong RPC/RLS; `public_visible_at` tính bằng giờ server |
| Cao | Bảo mật | Quên bật RLS một bảng; lộ service\_role key trong APK | Ai cũng đọc/sửa/xoá được dữ liệu | Bật RLS mọi bảng; APK chỉ có anon key; test bằng tài khoản lạ |
| Cao | Demo | Supabase Free tự tạm dừng sau 1 tuần không hoạt động | Ngày demo app không kết nối được | GitHub Actions gọi API mỗi ngày; mở dashboard kiểm tra trước demo |
| Cao | Dữ liệu | Chưa có bộ dữ liệu phường/xã mới kèm toạ độ tâm | Không vẽ được bong bóng phường | Tìm và kiểm tra nguồn ngay tuần 2 (đối chiếu VietMap) |
| Cao | Widget/push | Máy Xiaomi, Oppo, Vivo diệt app chạy nền | Không nhận push, widget không cập nhật | Dùng FCM high-priority; màn hình hướng dẫn bật Tự khởi động; widget tự làm mới khi mở app |
| Cao | Nhắn tin | RLS bảng messages chỉ kiểm tra người gửi | Người thứ ba đọc được tin nhắn riêng | Chỉ 2 thành viên conversation đọc/ghi; test bằng tài khoản thứ ba |
| Trung bình | Camera | Quyền bị từ chối vĩnh viễn | Màn hình chính đen | Trạng thái riêng + nút mở Cài đặt |
| Trung bình | Camera | Ảnh lệch hướng, ảnh camera trước bị ngược | Ảnh hiển thị sai | Xoay theo EXIF trước khi xoá EXIF; lật ngang ảnh camera trước |
| Trung bình | Camera | Decode ảnh gốc 12MP | Crash hết bộ nhớ (\~48 MB mỗi ảnh) | Decode thu nhỏ ngay từ đầu (`ImageDecoder` với kích thước đích) |
| Trung bình | Vị trí | Trong nhà không bắt được GPS; tắt định vị | Kẹt ở màn hình soạn bài | Timeout 5 giây → dùng vị trí gần nhất → không có thì chỉ cho mức "Việt Nam" |
| Trung bình | Vị trí | Chỉ cấp quyền vị trí gần đúng (sai số vài km) | Gán nhầm phường | Khoá mức Địa điểm cụ thể và Phường; cho Tỉnh trở lên |
| Trung bình | Vị trí | App giả lập vị trí (fake GPS) | Spam bài public ở địa danh nổi tiếng | Kiểm tra `isMock` (API 31+) hoặc `isFromMockProvider`; phát hiện thì không cho public ở mức cụ thể |
| Trung bình | Vị trí | Reverse geocode lỗi hoặc trả địa chỉ cũ (có quận/huyện) | Sai bong bóng | Yêu cầu định dạng mới; server tự xác định phường bằng tâm gần nhất nếu lỗi |
| Trung bình | Upload | Mạng rớt giữa chừng; bấm Gửi hai lần | Bài trùng hoặc ảnh mồ côi trong Storage | `postId` tạo trên máy + upsert; vô hiệu nút sau khi bấm; job dọn ảnh không có bài |
| Trung bình | Ảnh | Signed URL đổi mỗi lần lấy | Coil coi là ảnh mới, tải lại, đốt hết 5 GB băng thông | Cache key = storage path |
| Trung bình | Bản đồ | Số trên bong bóng tính cả bài hẹn giờ/bị chặn | Lộ "ai đó vừa ở đây" | Đếm trong RPC theo cùng luật hiển thị |
| Trung bình | Bản đồ | MapView trong Compose không nhận lifecycle | Rò bộ nhớ, crash khi quay lại | Bọc bằng `AndroidView` + chuyển onStart/onStop/onDestroy |
| Trung bình | Kiểm duyệt | 3 người cố ý báo cáo để ẩn bài người khác | Bài hợp lệ bị ẩn | Mỗi người báo cáo 1 lần/bài; tài khoản dưới 24 giờ không được báo cáo; thông báo cho chủ bài |
| Trung bình | Feed | Phân trang bằng offset khi có bài mới | Bài lặp hoặc bị bỏ qua | Cursor (created\_at, id); feed xếp hạng dùng `snapshot_at` |
| Trung bình | Chính sách | Không có nút xoá tài khoản | Google Play yêu cầu với app cho tạo tài khoản | Xoá tài khoản trong Cài đặt, xoá cả ảnh trong Storage |
| Trung bình | Nhắn tin | Huỷ kết bạn hoặc chặn nhưng vẫn gửi tin được | Quấy rối | send\_message kiểm tra còn là bạn và không chặn nhau |
| Trung bình | Nhắn tin | Mất mạng khi gửi rồi gửi lại; tin đến lệch thứ tự | Tin lặp, sai thứ tự | messageId tạo trên máy + upsert; sắp theo created\_at của server |
| Trung bình | Nhắn tin | Không huỷ kênh Realtime khi rời màn hình | Rò kết nối, vượt giới hạn kết nối của gói Free | Huỷ kênh trong onCleared hoặc DisposableEffect |
| Thấp | Nhắn tin | "Đang nhập" và "đã xem" ghi DB mỗi lần gõ | Tốn tài nguyên, chậm | "Đang nhập" qua broadcast, tối đa 1 lần/2 giây; "đã xem" cập nhật khi mở chat và khi có tin mới |
| Thấp | Giao diện | Không xử lý insets (targetSdk 35+ bắt buộc tràn viền) | Nút bị tai thỏ, thanh điều hướng che | `WindowInsets` trên mọi màn hình; test máy có tai thỏ |
| Thấp | Giao diện | Process bị hệ thống kill khi đang soạn bài | Mất ảnh vừa chụp | Lưu đường dẫn ảnh và lựa chọn vào `SavedStateHandle` |
| Thấp | Thông báo | Android 13+ cần quyền POST\_NOTIFICATIONS | Không hiện thông báo | Xin quyền ở onboarding |
| Thấp | Dữ liệu | Username trùng khi hai người đăng ký cùng lúc | Lỗi khó hiểu | Ràng buộc unique; bắt lỗi 23505 → "Username đã có người dùng" |
| Thấp | Dữ liệu | Địa danh dùng ảnh Wikimedia không ghi nguồn | Vi phạm giấy phép CC | Lưu và hiển thị `cover_attribution` |
| Thấp | Quy trình | Gemini sinh code mỗi màn một kiểu | Khó ghép, khó sửa | Chốt theme, domain model, interface Repository trước; mỗi prompt một màn hình, kèm khuôn mẫu ở trên |

## Lộ trình 6 tuần và phân công

Backend chạy song song từ tuần 2, nên tuần 3 đã nối được dữ liệu thật và tuần 4 chỉ còn hoàn thiện.

| Tuần | Yêu cầu của thầy | Frontend (nhóm + Gemini) | Backend (Claude) |
| --- | --- | --- | --- |
| 1 | Mô tả đề tài | Cập nhật slide theo hướng mới (bản đồ public), báo thầy | — |
| 2 | Thiết kế giao diện | Figma: luồng màn hình, wireframe, mockup. Gemini dựng theme + layout tĩnh mọi màn hình với fake data | Schema, RLS, seed 34 tỉnh + phường/xã + địa danh |
| 3 | CSDL và kiến trúc | ERD, sơ đồ MVVM, cây thư mục. Room cache. Thêm/sửa/xoá: tạo bài, sửa caption, xoá bài | RPC cơ bản, Auth, Storage, `reverse-geocode` |
| 4 | Xây dựng chức năng | Nối camera → upload, feed, bản đồ, kết bạn, nhắn tin, thông báo, báo cáo; kiểm tra đầu vào | `get_map_bubbles`, `get_discover_feed`, `send-push` |
| 5 | Kiểm thử | Test case theo bảng màn hình + bảng lỗi tiềm ẩn; test offline, nhiều cỡ màn hình; Baseline Profile | Test RLS bằng tài khoản lạ, sửa lỗi |
| 6 | Demo | Kịch bản demo, slide tổng kết | Giữ project không bị tạm dừng, dữ liệu demo |

| Người | Phụ trách |
| --- | --- |
| Thành viên 1 (nhóm trưởng) | Design system, navigation, `domain/`, Bản đồ, Album khu vực, Địa danh |
| Thành viên 2 | Camera, Soạn bài, xử lý ảnh, vị trí, UploadPostWorker, Cài đặt |
| Thành viên 3 | Đăng nhập/đăng ký, Hồ sơ, Bạn bè, Nhắn tin 1-1 (Realtime) |
| Thành viên 4 | Feed, cảm xúc, Thông báo, Widget, Báo cáo/Chặn, trưởng kiểm thử |
| Claude | Backend Supabase, review code frontend, viết prompt cho Gemini |

## Câu hỏi còn mở

- [x] Nhắn tin 1-1: giữ, chỉ giữa bạn bè; gửi ảnh trong chat là bài Locket riêng cho người đó.
- [ ] Bạn bè có thấy vị trí chi tiết hơn người lạ không, hay một mức vị trí áp dụng cho tất cả?
- [ ] Soạn bài ở Camera chính có cho chọn vài người nhận không, hay chỉ "tất cả bạn bè"? Hạ tầng bài riêng đã có nhờ chat.
- [x] Ảnh từ thư viện: không cho, chỉ ảnh chụp trực tiếp.
- [x] Đã chốt: project Lokap, package vn.nhom2.lokap (thay cho `vn.nhom2.lockket`).
- [ ] Bộ dữ liệu phường/xã sau sáp nhập kèm toạ độ tâm: lấy từ nguồn nào, ai kiểm tra?
