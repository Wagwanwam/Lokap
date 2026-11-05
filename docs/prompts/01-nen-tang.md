# Prompt 01 — Nền tảng: design system, domain model, dữ liệu mẫu

Bạn là lập trình viên Android senior. Hãy viết code Kotlin cho app **Lokap**: app kiểu Locket (mở app là camera, chụp và gửi ảnh cho bạn bè), thêm bản đồ public gom ảnh theo tỉnh/thành → phường/xã của Việt Nam (34 tỉnh sau sáp nhập 2025, không còn cấp quận/huyện), feed bạn bè + khám phá, nhắn tin 1-1. Giao diện chỉ có dark mode, nền đen, màu nhấn hồng, phong cách GenZ.

## Ràng buộc kỹ thuật (bắt buộc)
- Package gốc: `vn.nhom2.lokap`. minSdk 26, Kotlin 2.2, Jetpack Compose (Compose BOM) + Material 3.
- Chỉ dùng: Compose, Material 3, `androidx.compose.ui:ui-text-google-fonts`, `java.time`. Không thêm thư viện nào khác.
- Không viết màn hình, ViewModel, Hilt, Room, network trong prompt này.
- Mọi data class trong domain là bất biến (`val`), đánh dấu `@Immutable` khi dùng trong UI.

## Phần A — Design system (`core/designsystem/theme/`)
1. `Color.kt`: các token sau, dùng đúng mã màu:
   background #0B0B0D · surface #141417 · surfaceHigh #1E1E23 · outline #2C2C33 · primary #FF4D8D · primarySoft #FF8FB8 · onPrimary #0B0B0D (chữ trên nền hồng là màu ĐEN, không dùng trắng) · textPrimary #F5F5F7 · textSecondary #A1A1AA · textTertiary #71717A · accentLilac #B98CFF · accentLime #C6FF4D · accentCyan #4DE1FF · accentYellow #FFD84D · error #FF6B6B · success #4ADE80.
   Gom vào `data class LokapColors` + `LocalLokapColors`.
2. `Type.kt`: font **Be Vietnam Pro** qua Google Fonts downloadable (`GoogleFont.Provider` với `R.array.com_google_android_gms_fonts_certs`; file certs nhóm sẽ tự thêm). Thang chữ: Display 32/40 ExtraBold, Title 20/28 SemiBold, Body 15/22 Regular, Label 12/16 Medium. Map sang `Typography` của Material 3.
3. `Shape.kt`: cameraFrame 40dp, card 28dp, sheet 28dp (chỉ góc trên), chip/nút bo tròn hoàn toàn.
4. `Spacing.kt`: `data class Spacing(xs=4, sm=8, md=12, lg=16, xl=24, xxl=32 dp)` + `LocalSpacing`.
5. `Gradient.kt`: `fun regionGradient(provinceCode: String): Brush` trả về gradient 2 màu **cố định theo mã tỉnh** (cùng mã luôn cùng màu; chọn từ 8 cặp ghép từ các màu accent và primary; dùng hash ổn định, không dùng Random).
6. `Theme.kt`: `LokapTheme { }` chỉ dark: `darkColorScheme` map từ token, cung cấp `LocalLokapColors`, `LocalSpacing`, Typography, Shapes. Thêm `object LokapTheme { val colors; val spacing }` để gọi `LokapTheme.colors.primary`.
7. Một `@Preview` tên `DesignSystemPreview` hiển thị bảng màu, thang chữ (có chữ tiếng Việt có dấu: "Chụp khoảnh khắc ở Hội An") và 3 ô gradient.

## Phần B — Domain model (`domain/model/`)
- `enum class Audience { FRIENDS, DIRECT, PUBLIC }`
- `enum class LocationLevel { PLACE, WARD, PROVINCE, COUNTRY }` kèm nhãn tiếng Việt: Địa điểm cụ thể / Phường-xã / Tỉnh-thành / Chỉ "Việt Nam".
- `enum class PublishDelay(val days: Int) { NOW(0), ONE_DAY(1), THREE_DAYS(3), SEVEN_DAYS(7) }`
- `enum class RegionLevel { PROVINCE, WARD }`
- `data class Province(code, name, isCity: Boolean, lat, lng)` · `data class Ward(code, provinceCode, name, lat, lng)`
- `data class User(id, username, displayName, avatarUrl: String?)`
- `enum class FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIENDS, BLOCKED }`
- `data class PostLocation(level: LocationLevel, provinceCode: String?, provinceName: String?, wardCode: String?, wardName: String?, placeName: String?, landmarkId: String?, lat: Double?, lng: Double?)` — lat/lng là toạ độ ĐÃ LÀM MỜ để hiển thị.
- `enum class PostSendState { PENDING, SENT, FAILED }`
- `data class Post(id, author: User, imageUrl, caption: String?, audience, location: PostLocation, takenAt: Instant, publicVisibleAt: Instant?, reactions: List<Reaction>, myReaction: String?, sendState: PostSendState)`
- `data class Reaction(emoji: String, count: Int)`
- `enum class MapBubbleType { PROVINCE, WARD, PROVINCE_WIDE, PLACE, LANDMARK }`
- `data class MapBubble(id, type, code: String?, label, lat, lng, postCount: Int, coverImageUrl: String?, hasFriendPosts: Boolean)`
- `data class Landmark(id, name, provinceCode, wardCode: String?, lat, lng, category, coverUrl, coverAttribution, description, postCount: Int)`
- `data class Place(id, name, address, lat, lng)`
- `enum class NotificationType { NEW_POST, REACTION, FRIEND_REQUEST, FRIEND_ACCEPTED, NEW_MESSAGE, POST_HIDDEN }`
- `data class AppNotification(id, type, actor: User?, postId: String?, text, createdAt: Instant, isRead: Boolean)`
- `data class Conversation(id, friend: User, lastMessagePreview: String, lastMessageAt: Instant, unreadCount: Int)`
- `data class Message(id, conversationId, senderId, body: String?, replyToPost: Post?, attachedPost: Post?, createdAt: Instant, sendState: PostSendState, seen: Boolean)`
- `TimeAgo.kt` trong `core/common/`: hàm `formatTimeAgo(instant, now)` trả về tiếng Việt: "vừa xong", "5 phút trước", "3 giờ trước", "chụp 3 ngày trước" (dùng cho bài hẹn giờ), quá 7 ngày thì "dd/MM".

## Phần C — Dữ liệu mẫu (`data/fake/`)
1. `ProvinceCatalog.kt`: danh sách đủ **34** đơn vị (dùng đúng tên dưới đây, code tạm là slug, lat/lng là toạ độ trung tâm gần đúng):
   Thành phố trực thuộc trung ương (6): Hà Nội, Huế, Hải Phòng, Đà Nẵng, Thành phố Hồ Chí Minh, Cần Thơ.
   Tỉnh (28): Lai Châu, Điện Biên, Sơn La, Lạng Sơn, Quảng Ninh, Thanh Hóa, Nghệ An, Hà Tĩnh, Cao Bằng, Tuyên Quang, Lào Cai, Thái Nguyên, Phú Thọ, Bắc Ninh, Hưng Yên, Ninh Bình, Quảng Trị, Quảng Ngãi, Gia Lai, Khánh Hòa, Lâm Đồng, Đắk Lắk, Đồng Nai, Tây Ninh, Vĩnh Long, Đồng Tháp, Cà Mau, An Giang.
   Ghi chú đầu file: "Dữ liệu tạm cho UI, sẽ thay bằng dữ liệu từ backend".
2. `SampleData.kt`:
   - 6 user mẫu (tên Việt, username không dấu), user đầu tiên là "tôi".
   - Vài phường mẫu: Đà Nẵng (Hải Châu, Sơn Trà, Ngũ Hành Sơn, Hội An), Huế (Thuận Hóa, Phú Xuân).
   - 6 địa danh mẫu: Cầu Rồng, Bán đảo Sơn Trà, Ngũ Hành Sơn, Phố cổ Hội An, Đại Nội Huế, Chùa Thiên Mụ (coverUrl dùng `https://picsum.photos/seed/<slug>/800/800`, coverAttribution "Ảnh minh hoạ").
   - 20 bài mẫu trải đủ 3 audience, đủ 4 mức vị trí, có 2 bài PENDING/FAILED, có 2 bài public đang hẹn giờ; ảnh dùng picsum với seed khác nhau.
   - Map bubbles mẫu cho 3 mức zoom, 5 conversation, 12 message (có tin trả lời từ ảnh và tin kèm ảnh gửi riêng), 8 notification.
   - Mọi thời gian tính tương đối từ một `val NOW = Instant.parse("2026-11-05T08:00:00Z")` để preview luôn ổn định.

## Định dạng trả lời
- Mỗi file một khối code riêng. Dòng ĐẦU TIÊN của mỗi khối là comment đường dẫn đầy đủ, ví dụ:
  `// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Color.kt`
- Không giải thích dài, chỉ code. Cuối cùng liệt kê dependency cần thêm (nếu có).
- Nếu câu trả lời quá dài, dừng ở cuối một file hoàn chỉnh và ghi `CÒN TIẾP`; mình sẽ nhắn "tiếp tục".
