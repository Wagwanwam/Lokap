# Prompt 02 — Component dùng chung, điều hướng, màn hình Camera

Tiếp tục app Lokap trong cuộc trò chuyện này. Code bạn viết ở lượt trước đã được đưa vào project và build thành công, với 3 chỉnh sửa: `formatTimeAgo(..., isDelayedShot: Boolean = false)`; `MaterialLokapShapes.extraSmall = 8dp`, `small = 12dp` (nút/chip dùng `LokapShapes.pill`); `MainActivity` dùng `enableEdgeToEdge()` + `LokapTheme`. Giữ nguyên tên class/hàm bạn đã viết (LokapTheme.colors, LokapTheme.spacing, LocalLokapShapes, regionGradient, SampleData, ProvinceCatalog, domain model).

## Ràng buộc
- Package `vn.nhom2.lokap`, Kotlin 2.2, Compose + Material 3, minSdk 26. Chỉ dark mode, dùng token màu của LokapTheme, KHÔNG hard-code màu.
- Thư viện được dùng thêm: Navigation Compose (route type-safe với `@Serializable`), kotlinx-serialization, CameraX (core, camera2, lifecycle, view), Coil 3 (coil-compose + coil-network-okhttp), lifecycle-viewmodel-compose, lifecycle-runtime-compose. KHÔNG dùng Hilt, Room, network khác ở bước này.
- Chữ hiển thị bằng tiếng Việt có dấu. Mọi component có `@Preview` dùng `SampleData`.
- Chỗ dùng camera phải kiểm tra `LocalInspectionMode.current`: trong Preview thì vẽ khung giả, không gọi CameraX.
- Mẫu màn hình bắt buộc: `XxxUiState` (data class bất biến), `XxxAction` (sealed interface), `XxxEffect` (sự kiện một lần qua `Channel`), `XxxViewModel` (StateFlow, nhận Action), `XxxRoute` (có state, lấy ViewModel bằng `viewModel()`), `XxxScreen` (không state, chỉ nhận `state` + `onAction`, có Preview cho từng trạng thái).

## Phần A — Component (`core/designsystem/component/`)
1. `ShutterButton`: tròn 80dp, viền trắng 4dp, lõi hồng primary; nhấn thì co lại (spring, dampingRatio 0.6) + rung nhẹ (HapticFeedback); có trạng thái disabled.
2. `PinkButton` (nền primary, chữ onPrimary màu đen) và `GhostButton` (viền outline). Có `loading`.
3. `GlassChip`: chip bo tròn nền surfaceHigh, có icon trái, trạng thái selected (viền hồng).
4. `AvatarRing`: ảnh tròn (Coil `AsyncImage`), vòng hồng khi có bài mới; fallback chữ cái đầu tên.
5. `BadgeDot`: chấm số chưa đọc (99+).
6. `EmojiBar`: hàng emoji 😍😂🔥😮😢 + số đếm; emoji đang chọn nổi bật.
7. `PhotoCard`: thẻ bài đăng vuông bo 28dp: ảnh, avatar + tên, chip vị trí theo `LocationLevel` (Địa điểm/Phường/Tỉnh/"Việt Nam"), nhãn thời gian `formatTimeAgo` (truyền `isDelayedShot = true` chỉ khi bài PUBLIC có `publicVisibleAt` > now), `EmojiBar`, menu ⋮ (Báo cáo, Chặn); hiển thị trạng thái PENDING ("Đang gửi…") và FAILED (nút Thử lại).
8. `RegionCard` (thẻ khu vực GenZ): nền `regionGradient(code)`, tên khu vực chữ Display, 3 ảnh xếp chồng nghiêng ±6° kiểu polaroid, badge số ảnh, 1–2 sticker emoji, con dấu "ĐÃ ĐẾN" màu accentLime khi `visited = true`.
9. `MapBubble` (bong bóng: ảnh bìa tròn + số bài, viền cyan khi có bài bạn bè) và `LandmarkBubble` (viền vàng 3dp, lớn hơn 1.3 lần, nhãn tên).
10. `EmptyState`, `ErrorState` (nút Thử lại), `OfflineBanner`, `ShimmerBox` (hiệu ứng tải).

## Phần B — Điều hướng (`core/navigation/`)
- `Route.kt`: các route `@Serializable`: Camera (start), Composer(imagePath: String), Feed, Map, RegionAlbum(level: String, code: String), Landmark(id: String), PostDetail(id: String), Profile(userId: String?), Friends, Notifications, Inbox, Chat(conversationId: String), Settings.
- `AppNavHost.kt`: Camera là màn hình đầu. Các màn chưa làm dùng chung `PlaceholderScreen(title)` hiển thị `EmptyState("Đang xây dựng")` + nút quay lại. Chuyển cảnh: Feed trượt từ dưới lên, các màn khác trượt ngang.
- Cập nhật `MainActivity` để hiển thị `AppNavHost` thay vì `DesignSystemPreview`.

## Phần C — Màn hình Camera (`feature/camera/`)
- Bố cục từ trên xuống:
  - Thanh trên: `AvatarRing` (→ Profile), chip giữa "Bạn bè · N" (→ Friends), nút bản đồ (→ Map), nút tin nhắn có `BadgeDot` số chưa đọc (→ Inbox).
  - Khung preview vuông, bo `LokapShapes.cameraFrame` (40dp), chiếm hết chiều ngang trừ lề 16dp.
  - Thanh dưới: nút flash (tắt/bật/tự động), `ShutterButton`, nút lật camera trước/sau. KHÔNG có nút thư viện ảnh.
  - Dưới cùng: gợi ý "Vuốt lên xem bài" + mũi tên; vuốt lên trên toàn màn hình → Feed.
- CameraX: `PreviewView` bọc bằng `AndroidView`, bind theo lifecycle; `ImageCapture` dùng `CAPTURE_MODE_ZERO_SHUTTER_LAG` nếu máy hỗ trợ, không thì `CAPTURE_MODE_MINIMIZE_LATENCY`; preview độ phân giải 1080p; chụp ra file trong `cacheDir`; chụp xong → Effect điều hướng Composer(imagePath). Ảnh camera trước phải lật ngang.
- Quyền camera: trạng thái chưa hỏi, đã từ chối (giải thích + nút Cho phép), từ chối vĩnh viễn (nút "Mở Cài đặt" mở trang cài đặt app); camera lỗi/đang bận → `ErrorState`.
- Chạm đúp vào preview → lật camera; chụm 2 ngón → zoom.
- `CameraUiState` gồm: permission, lensFacing, flashMode, isCapturing, friendCount, unreadMessages, me (User); lấy dữ liệu từ `SampleData`.
- Preview cho 4 trạng thái: bình thường, chưa cấp quyền, từ chối vĩnh viễn, đang chụp.
- Thêm vào `AndroidManifest.xml`: quyền CAMERA, INTERNET; `<uses-feature android:name="android.hardware.camera" android:required="false"/>`.

## Định dạng trả lời
- Mỗi file một khối code, dòng ĐẦU TIÊN là `// FILE: app/src/main/...` (đường dẫn đầy đủ). File sửa toàn bộ thì gửi lại nguyên file.
- Không giải thích dài. Cuối cùng liệt kê dependency và plugin cần thêm theo **tên artifact, không ghi phiên bản**.
- Quá dài thì dừng ở cuối một file hoàn chỉnh, ghi `CÒN TIẾP`.
