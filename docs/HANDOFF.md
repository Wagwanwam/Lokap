# Bàn giao từ Claude (app) sang Claude Code — 05/11/2026

Đọc file này cùng `CLAUDE.md`. Đây là trạng thái dự án tại thời điểm chuyển phiên.

## Người dùng và cách làm việc
- Sinh viên CNTT, học thêm tiếng Nhật. Muốn câu trả lời tiếng Việt, ngắn, logic, không chào hỏi rườm rà.
- Câu hỏi công nghệ: **nêu vấn đề trước**, đưa lựa chọn có đánh đổi, **hỏi trước khi chọn hướng** (dùng câu hỏi trắc nghiệm khi có thể).
- Luôn kiểm tra dữ liệu/phiên bản trước khi khẳng định.
- Đang thử phương pháp học qua câu hỏi: sau mỗi phần việc, đặt 1 câu hỏi gợi mở cho người học tự trả lời, rồi nhận xét câu trả lời. Ghi lại nhận xét về cách học hiệu quả qua từng lần.
- Câu hỏi đang chờ người dùng trả lời:
  1. Vì sao lọc bài theo `visible_at` bắt buộc phải làm ở server?
  2. Vì sao prompt kèm đặc tả chi tiết giúp model yếu hơn (Gemini Flash) cho code tốt gần bằng model mạnh?
  3. Vì sao `applicationId` gần như không đổi được sau khi app lên Google Play?

## Bối cảnh môn học (6 tuần)
1. Mô tả đề tài (đã nộp slide theo hướng cũ "riêng tư"; cần cập nhật slide theo hướng mới có bản đồ public và báo thầy).
2. Thiết kế giao diện: luồng màn hình, wireframe/mockup Figma, bảng màu, layout tĩnh.
3. CSDL + kiến trúc: ERD, MVVM, cấu trúc thư mục, CRUD cơ bản.
4. Chức năng: đăng nhập/đăng ký, tìm kiếm, thông báo, gọi API; kiểm tra đầu vào, xử lý lỗi.
5. Kiểm thử: kế hoạch, test case (đúng/sai/trống/biên), máy ảo + máy thật, mất mạng, bảng lỗi, test lại.
6. Demo + trình bày; mỗi thành viên nói phần việc của mình.

## Quyết định đã chốt (chi tiết trong docs/system-design.md)
- Project **Lokap**, package `vn.nhom2.lokap`, minSdk 26, targetSdk 37, AGP 9.3, Kotlin + Compose, chỉ dark mode đen–hồng, font Be Vietnam Pro.
- Backend **Supabase** (Free). Bản đồ VietMap. Push FCM qua Edge Function.
- Ba kiểu đăng: Bạn bè / Riêng 1 người (gửi ảnh từ chat) / Public.
- Mức vị trí do người đăng chọn: Địa điểm cụ thể / Phường-xã / Tỉnh-thành (mặc định) / Chỉ "Việt Nam". Bài chỉ vào bong bóng đúng mức đã chọn.
- Hẹn giờ public: ngay / 1 / 3 / 7 ngày; bạn bè thấy ngay; nhãn "chụp 3 ngày trước".
- Đơn vị hành chính: 34 tỉnh → phường/xã (không còn quận/huyện).
- Địa danh: lọc từ Wikidata + nhóm tự chọn, đủ 34 tỉnh; VietMap Place cho gắn tag quán/trường.
- Feed: bạn bè theo thời gian; Khám phá xếp hạng bằng công thức (chưa dùng AI).
- Kiểm duyệt: báo cáo + chặn + tự ẩn khi ≥ 3 báo cáo.
- Nhắn tin 1-1: chỉ giữa bạn bè; chữ, trả lời từ ảnh, đã xem, số chưa đọc, đang nhập; gửi ảnh trong chat = bài Locket riêng cho người đó.
- **Không** cho đăng ảnh từ thư viện.

## Chia việc công cụ
- **Gemini web (model Pro)** dựng code frontend theo prompt trong `docs/prompts/`. Gemini trong Android Studio chỉ dùng được Flash (lỗi chung của Google từ 07/2026, chưa có bản sửa), nên người dùng chọn Gemini web.
- Quy trình: Claude viết prompt → người dùng dán vào Gemini web (hoặc Claude app điều khiển qua Claude in Chrome) → dán kết quả cho Claude Code → Claude Code đặt file theo dòng `// FILE:` ở đầu mỗi khối code, build, sửa lỗi, rà theo CLAUDE.md → nếu Gemini sai thì Claude Code viết prompt sửa ngắn cho người dùng gửi lại.
- **Claude Code** làm backend Supabase và review/sửa toàn bộ code.

## Đã làm
- Tạo project từ template Empty Activity (Compose). Đổi package `com.example.lokap` → `vn.nhom2.lokap`, minSdk 24 → 26.
- `CLAUDE.md` ở thư mục gốc.
- Prompt 01 đã gửi Gemini (Argon). Câu trả lời gốc lưu ở `docs/gemini/01-nen-tang.md`; 18 file đã tách vào `app/src/main/java/vn/nhom2/lokap/` (core/designsystem/theme, core/common, domain/model, data/fake). Đã thêm dependency `ui-text-google-fonts` vào version catalog và app/build.gradle.kts.
- Cuộc trò chuyện Gemini của prompt 01: https://gemini.google.com/u/4/app/984ddeccded8e73e
- Tài liệu thiết kế đầy đủ nằm trên claude.ai ("Lọc kẹt — Cấu trúc hệ thống v1"); người dùng cần export Markdown về `docs/system-design.md`.

## Việc tiếp theo (theo thứ tự)
0. Xem mục "Việc cho Claude Code sau prompt 01" ngay dưới.
1. Kiểm tra `docs/system-design.md` đã có chưa; chưa có thì nhắc người dùng export.
2. `git init`, `.gitignore` có `local.properties`; commit đầu tiên khi người dùng đồng ý.
3. Gradle sync; thêm dependency cho prompt 01 (`androidx.compose.ui:ui-text-google-fonts`) và file `res/values/font_certs.xml` cho Google Fonts provider.
4. Gửi prompt 01 cho Gemini, nhận code, đặt file, build, sửa.
5. Thay theme mặc định của template (`ui/theme/`) bằng `core/designsystem/theme/`.
6. Song song: backend bước 1–3 trong CLAUDE.md (hỏi người dùng trước mỗi quyết định; người dùng chưa tạo project Supabase).

## Câu hỏi còn mở
- Bạn bè có thấy vị trí chi tiết hơn người lạ không, hay một mức áp dụng cho tất cả?
- Soạn bài ở Camera chính có cho chọn vài người nhận không, hay chỉ "tất cả bạn bè"?
- Nguồn dữ liệu phường/xã sau sáp nhập kèm toạ độ tâm: lấy từ đâu, ai kiểm tra?

## Việc cho Claude Code sau prompt 01
1. Tạo `app/src/main/res/values-v23/font_certs.xml`: tải nguyên văn từ https://raw.githubusercontent.com/android/compose-samples/main/Jetchat/app/src/main/res/values-v23/font_certs.xml (chứng chỉ Google Fonts provider; KHÔNG gõ tay chuỗi base64).
2. Xoá `ui/theme/` của template; sửa `MainActivity` dùng `vn.nhom2.lokap.core.designsystem.theme.LokapTheme`, bật `enableEdgeToEdge()`, nội dung tạm là `DesignSystemPreview`.
3. Build (`gradlew assembleDebug`) và sửa lỗi biên dịch.
4. Lỗi đã thấy khi rà code Gemini, sửa luôn:
   - `TimeAgo.formatTimeAgo`: tham số `isDelayedShot` đang mặc định `true`, khiến mọi bài 1–7 ngày đều hiện "chụp X ngày trước". Đổi mặc định thành `false`; chỉ truyền `true` cho bài public hẹn giờ.
   - `Shape.kt`: `MaterialLokapShapes.extraSmall/small = CircleShape` làm DropdownMenu, TextField, Snackbar bị bo tròn kiểu viên thuốc. Đổi extraSmall = 8dp, small = 12dp; nút/chip dùng `LokapShapes.pill` riêng.
   - `SampleData.kt`: 12 bài có `publicVisibleAt` khác null trong khi đặc tả chỉ cần 2 bài public đang hẹn giờ. Kiểm tra: bài FRIENDS/DIRECT phải `publicVisibleAt = null`; chỉ đúng 2 bài PUBLIC có `publicVisibleAt > NOW`.
   - Bỏ khối dependencies Gemini đề xuất (BOM 2025.02.00), giữ version catalog hiện tại.
5. Chạy `DesignSystemPreview`, chụp màn hình cho người dùng xem.

## Việc cho Claude Code sau prompt 02 (component + điều hướng + Camera)
Claude (app) đã tách 17 file Gemini vào project (`core/designsystem/component/`, `core/navigation/`, `feature/camera/`), câu trả lời gốc ở `docs/gemini/02-component-camera.md`. Đã sửa tay 2 chỗ Gemini làm hỏng:
- `AndroidManifest.xml`: Gemini xoá mất theme `@style/Theme.Lokap` (splash nền tối), backup rules và `adjustResize`. Đã khôi phục bản gốc và chỉ thêm quyền CAMERA, INTERNET, `uses-feature camera required=false`.
- `MainActivity.kt`: giữ `SystemBarStyle.dark` + nền đen; hiển thị `AppNavHost()`; bỏ `safeDrawingPadding` vì các màn hình tự `systemBarsPadding()`.

Việc cần làm:
1. Thêm vào version catalog + `app/build.gradle.kts` (kiểm tra phiên bản ổn định mới nhất tương thích Kotlin 2.2.10 / AGP 9.3, không đoán): navigation-compose, plugin `org.jetbrains.kotlin.plugin.serialization` + kotlinx-serialization-json, camera-core/camera2/lifecycle/view (cùng một phiên bản), coil3 coil-compose + coil-network-okhttp, lifecycle-viewmodel-compose, lifecycle-runtime-compose.
2. Build, sửa lỗi biên dịch. Không đổi giao diện khi sửa lỗi.
3. Rà nhanh: không hard-code màu (đã grep: sạch), Preview không gọi CameraX, quyền camera đủ 3 trạng thái.
4. Chạy trên emulator Pixel 10: cấp quyền camera, chụp 1 ảnh (phải sang màn Composer placeholder), vuốt lên → Feed placeholder, bấm các nút trên thanh trên. Chụp màn hình Camera + 2 Preview (RegionCard, PhotoCard) vào `docs/screenshots/` để làm slide tuần 2.
5. Commit: `feat: component dùng chung, điều hướng, màn hình Camera`.

## Việc cho Claude Code sau prompt 03 (Feed, Soạn bài, Chi tiết bài)
Claude (app) đã tách 11 file mới (không ghi đè file cũ): `data/fake/SampleData2.kt`, `data/fake/FakePostStore.kt`, `feature/feed/*`, `feature/composer/*`, `feature/postdetail/*`. Câu trả lời gốc: `docs/gemini/03-feed-composer.md`. Gemini KHÔNG sửa AppNavHost; phần cần nối:
- Import: `vn.nhom2.lokap.feature.feed.feedScreen`, `vn.nhom2.lokap.feature.composer.composerScreen`, `vn.nhom2.lokap.feature.postdetail.postDetailScreen`.
- Trong `AppNavHost.kt`, thay 3 `composable<Route.Feed|Composer|PostDetail> { PlaceholderScreen(...) }` bằng `feedScreen(navController)`, `composerScreen(navController)`, `postDetailScreen(navController)`. Giữ nguyên launchSingleTop và các chỉnh sửa trước.

Việc cần làm:
1. Nối 3 màn hình vào AppNavHost như trên.
2. Build, sửa lỗi biên dịch (Gemini có thể gọi sai tên trong SampleData/component — sửa theo tên thật, không đổi giao diện).
3. Kiểm tra logic: Khám phá KHÔNG hiện bài public đang hẹn giờ; Composer chỉ hiện Hẹn giờ khi Public; chọn "Địa điểm cụ thể" mà chưa chọn địa điểm thì không cho Gửi; Gửi xong quay về Camera và bài mới hiện ở Feed với "Đang gửi…" rồi thành đã gửi.
4. Chạy emulator: Camera → chụp → Composer → Gửi → vuốt lên Feed thấy bài; mở Chi tiết bài; thử Báo cáo/Chặn. Chụp màn Feed (2 tab), Composer, Chi tiết bài vào `docs/screenshots/`.
5. Commit `feat: Feed, Soạn bài, Chi tiết bài (dữ liệu giả)`.

## Việc cho Claude Code sau prompt 04 (Bản đồ, Album khu vực, Địa danh)
Gemini viết trong cuộc trò chuyện MỚI (cuộc cũ quá nặng làm treo trình duyệt), có kèm chữ ký component/model thật trong prompt. Claude (app) đã tách 5 file mới: `data/fake/SampleMapData.kt`, `feature/map/LokapMap.kt` (bản đồ giả bằng Compose, tách lớp để thay VietMap ở tuần 4), `feature/map/MapScreen.kt`, `feature/region/RegionAlbumScreen.kt`, `feature/landmark/LandmarkScreen.kt`. Gốc: `docs/gemini/04-map.md`.
1. Nối vào AppNavHost: import `vn.nhom2.lokap.feature.map.mapScreen`, `vn.nhom2.lokap.feature.region.regionAlbumScreen`, `vn.nhom2.lokap.feature.landmark.landmarkScreen`; thay 3 PlaceholderScreen của Route.Map / RegionAlbum / Landmark bằng `mapScreen(navController)`, `regionAlbumScreen(navController)`, `landmarkScreen(navController)`.
2. Build, sửa lỗi biên dịch theo tên thật (không đổi giao diện).
3. Kiểm tra đúng đặc tả: zoom < 8 tỉnh; 8–12 phường + "Khắp <tỉnh>"; ≥ 13 ghim địa điểm; địa danh từ zoom 8; bài mức "Việt Nam" không lên bản đồ.
4. Chạy: Camera → nút bản đồ → zoom vào Đà Nẵng → chạm bong bóng phường → Xem album → chạm ảnh; chạm bong bóng địa danh. Chụp màn Bản đồ (3 mức zoom), Album, Địa danh vào `docs/screenshots/`.
5. Commit `feat: Bản đồ (giả), Album khu vực, Địa danh`.

## Quy trình Gemini (chốt 05/11, từ prompt 05)
1. Claude (app) viết prompt vào `docs/prompts/0X-*.md`, tự chứa đủ chữ ký component/model thật.
2. Người dùng dán vào **cuộc trò chuyện Gemini MỚI** (model Argon), gửi; xong bấm nút Sao chép dưới câu trả lời, dán vào `docs/gemini/0X-*.md`.
3. Tách file: `python tools/split_gemini.py docs/gemini/0X-*.md` (xem trước) rồi thêm `--write`. Script không ghi đè file đã có.
4. Claude (app) rà code, ghi mục "Việc cho Claude Code sau prompt 0X" vào file này; Claude Code nối AppNavHost, build, sửa lỗi, chạy thử, commit.

## Thiết bị chạy thử (từ prompt 05)
Điện thoại Android thật qua USB, không dùng emulator nữa. Xem mục "Thiết bị chạy thử" trong CLAUDE.md.

## Việc cho Claude Code: sửa lỗi bản đồ (04b)
Được phép sửa giao diện nhỏ, không qua Gemini. Commit riêng `fix: bản đồ`.
1. Phụ đề "Phường / Xã & Địa danh • Zoom 11,2" quá dài làm nút "Vị trí của tôi" bị bẻ dòng: rút gọn phụ đề (bỏ số zoom) và cho nút một dòng.
2. Thanh lọc / nút quay lại che bong bóng: thêm khoảng đệm phía trên cho vùng vẽ bản đồ (hoặc dời tâm camera) để bong bóng không nằm dưới thanh lọc.
3. Zoom 9–11 bong bóng phường và địa danh chồng nhau: bong bóng nào đè lên bong bóng có nhiều bài hơn thì ẩn; địa danh ưu tiên hiện.
4. Tên phường cũ (Hàng Bạc, Bến Nghé, Thảo Điền…): tra nguồn chính thức (Nghị quyết sắp xếp đơn vị hành chính cấp xã năm 2025 của từng tỉnh) rồi đổi sang tên phường MỚI, ghi nguồn trong comment đầu SampleMapData.
5. Số trên bong bóng và trang Địa danh phải đếm từ FakePostStore, không gõ cứng.
6. Chạy trên điện thoại, chụp lại bản đồ (zoom tỉnh, phường, gần) thay ảnh 08–09.

## Việc cho Claude Code sau prompt 05 (Hồ sơ, Bạn bè, Cài đặt)
Claude (app) đã ghi 10 file mới bằng `tools/split_gemini.py`: `data/fake/SampleSocialData.kt`, `feature/profile/*` (3 file), `feature/friends/*` (3 file), `feature/settings/*` (3 file). Gốc: `docs/gemini/05-ca-nhan.md`. Đã rà: không hard-code màu, icon đều có trong material-icons-core, gọi đúng chữ ký component.
1. Nối AppNavHost: import `vn.nhom2.lokap.feature.profile.profileScreen`, `vn.nhom2.lokap.feature.friends.friendsScreen`, `vn.nhom2.lokap.feature.settings.settingsScreen`; thay 3 PlaceholderScreen của Route.Profile / Friends / Settings.
2. Build, sửa lỗi biên dịch theo tên thật.
3. Kiểm tra logic: "Đã đến X/34" đếm tỉnh khác nhau; hồ sơ người khác chỉ hiện bài được phép xem (FRIENDS khi là bạn, PUBLIC đã hiện; không hiện bài public đang hẹn giờ với người lạ); giới hạn 20 bạn khoá nút Kết bạn/Chấp nhận; Xoá tài khoản chỉ bật khi gõ đúng "XOÁ"; Sửa tên 2–30 ký tự.
4. Thêm unit test cho các luật ở bước 3.
5. Chạy trên điện thoại: Camera → avatar → Hồ sơ (2 tab) → Cài đặt; Camera → chip Bạn bè → tìm, kết bạn. Chụp ảnh vào `docs/screenshots/`.
6. Commit `feat: Hồ sơ, Bạn bè, Cài đặt (dữ liệu giả)`; commit riêng `docs/`, `tools/`, `CLAUDE.md` thành `docs: prompt 05–06, script tách file Gemini, quy định thiết bị test`.

## Việc cho Claude Code sau prompt 06 (Tin nhắn, Chat 1-1, Thông báo)
Claude (app) đã ghi 10 file mới: `data/fake/FakeChatStore.kt`, `feature/inbox/*`, `feature/chat/*`, `feature/notifications/*` (mỗi thư mục 3 file). Gốc: `docs/gemini/06-lien-lac.md` (Gemini gộp cả 10 file vào một khối code; `tools/split_gemini.py` đã được nâng cấp để tách theo từng dòng FILE). Đã rà: không hard-code màu, không dùng Random, có imePadding cho ô nhập, icon đều trong material-icons-core.
1. Nối AppNavHost: import `vn.nhom2.lokap.feature.inbox.inboxScreen`, `vn.nhom2.lokap.feature.chat.chatScreen`, `vn.nhom2.lokap.feature.notifications.notificationsScreen`; thay 3 PlaceholderScreen của Route.Inbox / Chat / Notifications.
2. Build, sửa lỗi biên dịch theo tên thật.
3. Kiểm tra logic: chỉ chat với bạn bè (không còn là bạn → khoá ô nhập); mở chat thì unreadCount về 0; gửi tin PENDING → SENT; "Đang nhập…"/"Đã xem" không ghi vào kho; bạn trả lời theo vòng xoay cố định; thông báo chạm đúng đích và đánh dấu đã đọc; nút camera trong chat tạm về Route.Camera (TODO tuần 4: chế độ gửi riêng). Badge tin nhắn trên Camera và Feed phải lấy từ FakeChatStore.
4. Unit test cho các luật ở bước 3.
5. Chạy trên điện thoại: Camera → nút tin nhắn → mở chat → gửi tin; Feed → chuông → thông báo. Chụp ảnh vào `docs/screenshots/`.
6. Commit `feat: Tin nhắn, Chat 1-1, Thông báo (dữ liệu giả)`.


## Việc cho Claude Code sau prompt 07 (Onboarding, Đăng nhập/Đăng ký, Thiết lập hồ sơ)

18 file đã ghi vào project (Claude app tách từ `docs/gemini/07-vao-app.md`, lấy thẳng từ khối code nên lần này không mất generic). Claude app đã sửa sẵn: `FakeAuthStore.kt` import sai `core.model.User/SampleData` → `domain.model.User` (SampleData cùng package). `tools/split_gemini.py` đã chặn đường dẫn có `..` hoặc tuyệt đối.

1. **Nối AppNavHost**: đăng ký `onboardingScreen`, `loginScreen`, `registerScreen`, `forgotPasswordScreen`, `setupProfileScreen`; dùng `rememberStartDestination()` (null → chưa vẽ NavHost, nền đen như cũ).
2. **Sửa `rememberStartDestination` (lỗi logic quan trọng)**: bản Gemini dùng `remember(onboardingDone, currentUser)` nên startDestination **đổi mỗi khi đăng nhập/đăng xuất/xong onboarding** → NavHost dựng lại graph, back stack bị reset, có thể điều hướng 2 lần. Phải chốt **một lần** lúc mở app (đọc `.value` trong `remember {}`), sau đó chỉ điều hướng bằng `navigate(...){ popUpTo(0) }`. Bỏ mẹo `LaunchedEffect` + `isInitialized` nếu không cần.
3. **Manifest**: chỉ thêm `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`. Giữ nguyên `uses-feature android.hardware.camera required=false` đang có, **không** thay bằng `camera.any` như Gemini gợi ý. Không thêm dependency nếu build đã có activity-compose và lifecycle-runtime-compose.
4. **Đăng xuất**: nối nút Đăng xuất ở Cài đặt với `FakeAuthStore.logout()` rồi `navigate(LoginRoute){ popUpTo(0) }`. Xoá tài khoản ("XOÁ") cũng về Login.
5. **Kiểm tra logic**:
   - Đăng nhập sai: chỉ báo "Email hoặc mật khẩu không đúng", không nói sai ô nào. `offline@lokap.vn` giả mất mạng → OfflineBanner + thử lại.
   - Quên mật khẩu: luôn báo "Đã gửi link…" kể cả email không tồn tại.
   - Lỗi ô chỉ hiện sau khi rời ô hoặc bấm nút; nút khoá + loading khi gửi, bấm 2 lần không gọi 2 lần.
   - Mật khẩu ≥ 8 ký tự và có cả chữ và số; nhập lại phải khớp; chưa tích điều khoản thì không gửi.
   - Username 3–20 ký tự `[a-z0-9._]`, tự chuyển chữ thường, debounce 400ms, "lokap" và username trong SampleData báo trùng; huỷ job cũ khi gõ tiếp (không để kết quả cũ ghi đè kết quả mới).
   - Tên hiển thị 2–30 ký tự sau trim.
   - Quyền: chỉ cho COARSE → hiện "Chỉ đăng được ở mức Tỉnh trở lên"; từ chối hẳn → nút "Mở Cài đặt"; Android < 13 ẩn/tự coi như đã cấp thông báo. Không vòng lặp xin lại quyền (lỗi từng gặp ở Camera).
   - Sau đăng nhập/đăng ký xong, bấm Back ở Camera phải thoát app, không quay lại Login.
   - Màu, icon: chỉ `LokapTheme.colors`, chữ trên nền hồng là `onPrimary`; icon chỉ trong material-icons-core.
6. **Unit test**: `AuthValidators` (email, mật khẩu, username, tên) và `FakeAuthStore` (sai mật khẩu, email trùng, username trùng). Gọi `simulateOffline` thì nhớ reset sau test vì là object dùng chung.
7. Chạy trên điện thoại thật, chụp màn hình vào `docs/screenshots/` (onboarding 3 trang, xin quyền, login, register có lỗi, quên mật khẩu, setup profile).
8. Commit: `feat: Onboarding, Đăng nhập/Đăng ký, Thiết lập hồ sơ (dữ liệu giả)`; commit riêng `docs: prompt 07, chặn đường dẫn lạ trong split_gemini`.

Ghi chú: trạng thái đăng nhập giả chỉ nằm trong bộ nhớ, nên mỗi lần mở lại app sẽ thấy onboarding. Giữ vậy cho buổi demo; tuần 3 phiên Supabase sẽ lưu thật.


## Quyết định 06/11 (trả lời câu hỏi sau prompt 06)
1. **Bản đồ**: địa danh **0 bài nhường chỗ** cho bong bóng phường; địa danh có bài vẫn ưu tiên. Sửa + thêm unit test (Đà Nẵng zoom 9–11 phải thấy bong bóng phường).
2. **Dữ liệu giả**: bỏ/sửa thông báo "Đỗ Bảo Ngọc gửi lời mời kết bạn" vì người này đã là bạn (đổi sang loại khác, ví dụ thả cảm xúc).
3. **Lặp chú thích ở chat**: giao cho Gemini (prompt 08). Claude Code **không sửa** `FeedScreen.kt`, `PhotoCard.kt`, `ChatScreen.kt` cho tới khi Claude app tách xong 08, để tránh ghi đè.
4. Ảnh chụp chat có avatar kèm số "13" đè lên tin: đó là bong bóng chat head của Messenger trên điện thoại, không phải lỗi app. Tắt bong bóng chat Messenger trước khi chụp màn hình.

Commit riêng: `fix: địa danh 0 bài nhường bong bóng phường, sửa dữ liệu thông báo`.


## Việc cho Claude Code sau prompt 08 (giao diện tối giản: Feed, PhotoCard, Chat)

Làm **sau khi đã commit 07 và mục "Quyết định 06/11"**. Câu trả lời Gemini ở `docs/gemini/08-toi-gian-ui.md` (Claude app đã kiểm: chỉ icon gói core, không màu cứng, chữ ký hàm public và số Preview giữ nguyên).

1. Ghi đè đúng 3 file: `python tools/split_gemini.py docs/gemini/08-toi-gian-ui.md --write --force` (chỉ có 3 file: FeedScreen.kt, PhotoCard.kt, ChatScreen.kt). Xem `git diff --stat` để chắc không file nào khác bị đổi.
2. Rà `git diff` từng file: chỉ đổi giao diện, không đổi logic/Action/điều hướng. Chỗ nào Gemini lỡ xoá hành vi (ví dụ onClick, chặn bấm 2 lần, cuộn tới tin mới nhất, khoá ô nhập khi hết là bạn) thì khôi phục.
3. **Khoá chat chỉ còn icon Lock** là quá tối giản: người dùng không hiểu vì sao bị khoá. Giữ icon + chữ ngắn "Chưa là bạn" (quy chuẩn: giữ chữ khi icon dễ hiểu nhầm).
4. Kiểm tra trên điện thoại:
   - Khám phá: tiêu đề chỉ còn icon vị trí + "Đà Nẵng", không còn dòng phụ.
   - Thẻ ảnh: tên + thời gian đọc được trên ảnh sáng (có nền mờ), caption căn giữa, hàng tác giả và chip vị trí thẳng hàng.
   - Chat: tin có ảnh Locket chỉ hiện caption một lần.
   - Icon ≥ 24dp, vùng chạm ≥ 48dp.
5. **Tắt bong bóng chat Messenger trên điện thoại trước khi chụp.** Chụp lại `05-feed-ban-be`, `06-feed-kham-pha`, `21-chat` (ghi đè ảnh cũ).
6. Commit: `style: giao diện tối giản cho Feed, thẻ ảnh, Chat`.


## Việc cho Claude Code sau prompt 09 (màn Giới thiệu + Xin quyền tối giản)

Prompt `docs/prompts/09-xin-quyen.md` gửi kèm `OnboardingScreen.kt` **tại thời điểm 00:55 06/11** (lúc Claude Code đang nối 07). Khi Claude app báo đã có `docs/gemini/09-xin-quyen.md`:
1. Trước khi ghi đè, lưu bản hiện tại: nếu sau 00:55 bạn đã sửa `OnboardingScreen.kt` thì ghi lại các chỗ sửa để áp lại lên bản mới.
2. `python tools/split_gemini.py docs/gemini/09-xin-quyen.md --write --force` (chỉ 1 file). Rà `git diff`: logic xin quyền (FINE + COARSE, mở Cài đặt, Android 13+, không vòng lặp xin lại) phải còn nguyên.
3. Người dùng chốt: **bỏ hết chữ giải thích**, kể cả "Chỉ lấy vị trí lúc chụp". Chỉ còn icon + "Camera / Vị trí / Thông báo"; đã cấp = viền hồng + ✓ hồng; bấm cả ô để cấp.
4. Kiểm tra TalkBack đọc được trạng thái qua contentDescription ("Camera, đã cấp").
5. Chụp lại `23-onboarding-1..3`, `24-xin-quyen`. Commit: `style: màn giới thiệu và xin quyền tối giản`.


## Phiên Claude Code trên Mac (06/11, 01:25–02:05) — đọc mục này trước khi làm tiếp

Một thành viên khác clone repo về MacBook Pro M4 16 GB (macOS 26.2) và làm tiếp từ commit `c75e11d` (commit WIP do bạn trên Windows đẩy lên lúc 01:20).

### Môi trường trên Mac (khác Windows)
- Repo: `/Users/nguyenduythuan/vscode/Mobile/Lokap`, nhánh `wip/onboarding-ui` (cũng là nhánh mặc định trên GitHub, không có `main`).
- **Build luôn dùng** `ANDROID_HOME=$HOME/Library/Android/sdk bash ./gradlew --no-daemon assembleDebug testDebugUnitTest` (người dùng chốt). Lý do:
  - VS Code (extension Java, đang mở dự án PBL4) bật một Gradle 9.5.0 daemon trên JRE riêng thiếu `jlink`. Lokap cũng đòi Java 25 nên Gradle dùng lại daemon đó → lỗi `jlink ... does not exist`. `--no-daemon` né được.
  - `gradlew` thiếu quyền chạy (mode 644 từ Windows) → gọi `bash ./gradlew`.
  - Chưa có `local.properties` → truyền `ANDROID_HOME`. Android Studio mở dự án sẽ tự tạo file này.
- `adb` chưa có trong PATH: `~/Library/Android/sdk/platform-tools/adb`.
- `python` trên máy là 2.7 → dùng `python3 tools/split_gemini.py ...`.
- **Chưa cấu hình `git user.name/email`** → muốn commit phải hỏi người dùng trước. CLAUDE.md: chỉ commit khi được bảo.
- **Thiết bị: người dùng chốt dùng emulator `Pixel_10_Pro_XL` (`emulator-5554`, Android 17, 1344×2992) trên máy này.** Quy định "không bật emulator" trong CLAUDE.md là cho laptop Windows yếu.
  - Emulator do một phiên Claude khác (dự án PBL4) bật, trên đó có app `com.pbl4.food`: không tắt emulator, không gỡ app đó.
  - Điện thoại HONOR (`AXSCVB5304005477`) cũng đang cắm nhưng không dùng: máy có user phụ 100 nên một số lệnh `pm` bị từ chối.
- Chạy thử nhanh trên emulator:
  - Cài app: `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`.
  - Cấp sẵn quyền: `adb -s emulator-5554 shell pm grant vn.nhom2.lokap android.permission.CAMERA`, làm tương tự cho `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`.
  - Mỗi lần mở app đều thấy onboarding. Bấm "Bỏ qua" → "Bắt đầu" → đăng nhập `demo@lokap.vn` / `lokap123`.
  - Đọc màn hình: `adb shell uiautomator dump` (Compose có text/content-desc). Chạm: `adb shell input tap x y`. Chụp: `adb exec-out screencap -p > docs/screenshots/<tên>.png`.

### Đã xong trong phiên Mac
1. Ở HEAD `c75e11d`: build OK, unit test 59/59 pass.
2. **Quyết định 06/11 (bản đồ):** người dùng chốt **giữ luật trong commit WIP**:
   - zoom < 11: bong bóng phường ưu tiên hơn cả địa danh có bài;
   - zoom ≥ 11: địa danh có bài ưu tiên;
   - địa danh 0 bài luôn nhường chỗ.
   Kiểm trên emulator:
   - zoom 9 (chạm bong bóng tỉnh Đà Nẵng) → 2 phường (P. Hải Châu 3, P. Hội An 2), 0 địa danh;
   - zoom 11 (nút "Vị trí của tôi") → 4 địa danh, 0 phường.
   Mục này không cần làm thêm.
3. **Prompt 08 ĐÃ ÁP, CHƯA COMMIT** (chỉ 3 file `FeedScreen.kt`, `PhotoCard.kt`, `ChatScreen.kt`):
   - Bản code chụp trong prompt 08 khớp HEAD, nên ghi đè bằng `split_gemini.py --write --force` không mất gì. Rà diff: không mất `onAction`/Effect/LaunchedEffect/cuộn tới tin mới/`enabled`/Preview/chữ ký hàm.
   - Đã sửa thêm trên bản Gemini:
     - khoá chat = icon Lock 24dp + chữ **"Chưa là bạn"**;
     - contentDescription "Đang gửi" cho vòng xoay (Feed, PhotoCard, Chat) và "Đang nhập…" cho "•••";
     - icon đã gửi/đã xem 16→24dp; icon nhãn DIRECT 20→24dp (pill 40dp);
     - khôi phục 3 comment Gemini xoá: cuộn tin mới nhất, TODO tuần 4 nút camera, `launch` riêng cho snackbar.
   - Code main biên dịch được. Có 1 cảnh báo `Condition is always 'true'` ở `ChatScreen.kt` (~dòng 390, vế `&& message.body != null` thừa), có thể bỏ.
   - **Chưa làm:**
     - chạy unit test (đang đỏ vì việc của phiên khác, xem dưới);
     - kiểm trên emulator theo mục "sau prompt 08" bước 4;
     - chụp lại `05-feed-ban-be`, `06-feed-kham-pha`, `21-chat`;
     - commit `style: giao diện tối giản cho Feed, thẻ ảnh, Chat`.

### ⚠️ Có một phiên Claude Code khác sửa cùng thư mục
- Từ 01:40, một phiên khác (app Claude desktop) làm tính năng **"kiểm tra email trùng khi đăng ký"**, chưa commit. Phiên đó sửa 4 file: `AuthLogicTest.kt`, `FakeAuthStore.kt`, `RegisterContract.kt`, `RegisterViewModel.kt`.
- Lúc 01:58 test của phiên đó còn đỏ (chưa có `EmailAvailability`), nên `testDebugUnitTest` lỗi biên dịch cho tới khi phiên đó xong.
- **Không sửa 4 file trên.** Khi commit chỉ `git add` đúng file của việc mình làm; đừng dùng `git add -A` / `git commit -a`.

### Việc tiếp theo (theo thứ tự)
1. Hỏi người dùng phiên "kiểm tra email" đã xong và đã dừng chưa. Sau đó chạy lại build + toàn bộ unit test (phải xanh).
2. Kiểm prompt 08 trên emulator theo bước 4 mục "sau prompt 08", rồi chụp lại 05, 06, 21 (ghi đè).
3. Làm prompt 09 theo mục "sau prompt 09" ở trên:
   - Đã kiểm: chỗ sửa sau 00:55 trong `OnboardingScreen.kt` chỉ là "Bến Nghé" → "Sài Gòn", và bản Gemini 09 không còn dòng này, nên ghi đè không mất gì.
   - Chạy thử (không ghi) cho thấy script chỉ ghi 1 file.
4. Commit khi người dùng bảo. Trước đó cần `git user.name/email`. Đề xuất thêm commit `git update-index --chmod=+x gradlew`.
5. Yêu cầu cũ bị ngắt: viết kịch bản slide 5–7 phút từ `docs/screenshots/` + code. Hỏi lại người dùng sau khi có ảnh mới.

### Cập nhật phiên Mac (06/11, 02:05–02:25): đã xong việc 1–3 ở trên, CHƯA commit
- Build OK, unit test 69/69 (tính cả test của phiên kiểm tra email). Phiên email đã `idle`, nhưng nó sửa **5 file** (thêm `RegisterScreen.kt`) chứ không phải 4. Chưa rõ phiên đó muốn commit lúc nào.
- **Prompt 08:** đã kiểm trên emulator, chụp lại 05, 06, 21. Ảnh 05 chụp bài thứ 2 vì bài đầu đang ở trạng thái "Đang gửi". Sửa thêm:
  - `FeedScreen.kt`: chip vị trí bị xuống 2–3 dòng → pill tác giả có `weight(1f, fill = false)`; tên đổi `titleSmall` → `labelLarge` (giống PhotoCard) để hiện đủ.
  - `PhotoCard.kt`: nút ⋮ lệch vị trí giữa các thẻ → luôn sát mép phải.
  - `ChatScreen.kt`: bỏ vế `&& message.body != null` thừa.
- **Prompt 09:** đã ghi đè `OnboardingScreen.kt`; `OnboardingRoute` giống hệt HEAD. Đã chụp lại 23-1..3, 24. Sửa thêm: bỏ mô tả sai "tự hủy sau 24 giờ"; dùng `clearAndSetSemantics` để TalkBack không đọc trạng thái hai lần. Đã thử: từ chối 2 lần → mở Cài đặt, cấp trong Cài đặt rồi quay lại tự cập nhật, vị trí tương đối, thông báo.
- **Commit khi được bảo** (chỉ `git add` đúng file, không đụng 5 file của phiên email):
  - `style: giao diện tối giản cho Feed, thẻ ảnh, Chat`: `FeedScreen.kt`, `PhotoCard.kt`, `ChatScreen.kt`, ảnh 05, 06, 21.
  - `style: màn giới thiệu và xin quyền tối giản`: `OnboardingScreen.kt`, ảnh 23-1..3, 24.
- **Còn mở, chưa sửa:**
  - Nút emoji chỉ rộng ~39dp (`EmojiBar`), dưới chuẩn 48dp; icon trong `GlassChip` 16dp, dấu ✓ ô quyền 14dp, dưới chuẩn 24dp. Đây là component dùng chung hoặc huy hiệu nhỏ, cần nhóm quyết.
  - Minh hoạ trang 3 có nhãn "24h ⏳", có thể bị hiểu là tự huỷ ảnh; thiết kế chỉ có hẹn giờ công khai (1/3/7 ngày).
  - Một lần `adb swipe` trên thẻ đầu Feed mở nhầm màn Chi tiết; thử lại 4 lần không lặp lại, chưa rõ nguyên nhân.
  - Chưa bật TalkBack thật, mới kiểm qua `uiautomator dump`. Chưa thử trên điện thoại thật.

### Phiên Mac 06/11 (02:28–03:55): rà căn chỉnh trên emulator + slide tuần 2 — CHƯA commit
- Emulator `Pixel_10_Pro_XL` (QEMU, `emulator-5554`) chạy sẵn, không tắt. Đã cài APK mới. Bật demo mode thanh trạng thái (9:41) để chụp ảnh rồi đã tắt, `sysui_demo_allowed` trả về 0.
- Build OK, unit test 69/69. Không đụng 5 file của phiên "kiểm tra email".
- Đo toạ độ bằng `uiautomator dump` trên mọi màn, sửa 7 file (chỉ giao diện, không đổi logic/Action/điều hướng):
  - `CameraScreen.kt`: thanh trên dùng `Box` + `align` thay `Row SpaceBetween` → chip "Bạn bè" về đúng tâm (trước lệch trái 26dp so với nút chụp).
  - `ProfileScreen.kt`: 3 cột thống kê cùng `weight(1f)` (cột đầu 1.3f làm "Bài đăng" lệch phải 19dp so với avatar).
  - `GlassChip.kt`: `spacedBy(xs, CenterHorizontally)` → chip bị kéo giãn (tab Ảnh/Dấu chân) có chữ ở giữa.
  - `RegionCard.kt`: chỉ vẽ đúng số ảnh, ảnh đầu nằm trên (trước luôn vẽ 3 khung nên khu vực 1–2 ảnh lộ khung trống); thẻ hẹp < 280dp (Hộ chiếu vùng miền) ẩn sticker, thu nhỏ chồng ảnh, tên 24sp → hết đè badge.
  - `MapScreen.kt` + `LokapMap.kt`: header chỉ đệm cạnh trên + hai bên (`systemBarsPadding()` cũ cộng cả thanh điều hướng vào chiều cao header); thêm `bottomInset` cho phép chiếu → bong bóng TP.HCM không còn nằm dưới nút ◀ ● ■; lề header 12 → 16dp nên hàng lọc cân hai bên.
  - `SetupProfileScreen.kt`: dòng phụ căn giữa như tiêu đề; 6 avatar mẫu xếp 3 + 3 ở giữa (trước 5 + 1, ô lẻ bên trái).
- Commit đề xuất khi được bảo: `style: sửa căn chỉnh Camera, Hồ sơ, Bản đồ, thẻ khu vực, Thiết lập hồ sơ` (chỉ `git add` 7 file trên).
- Slide tuần 2 nằm ngoài repo, cạnh `a.pptx`: `Lokap-Tuan2-GiaoDien.pptx` (+ `.pdf`, kịch bản trong Notes), `Lokap-Tuan2-KichBan.md`, `Lokap-Tuan2-ThietKe/` (sơ đồ luồng, 6 wireframe, 25 mockup).
- Còn mở, cần nhóm quyết:
  - Lề ngang chưa thống nhất: Album, Địa danh, Bạn bè, Tin nhắn, Thông báo, Hồ sơ dùng 12dp; các màn còn lại 16dp.
  - Ở zoom 9, chạm bong bóng phường → nhảy zoom 11.2, nơi địa danh có bài được ưu tiên nên bong bóng phường bị ẩn: khó mở sheet "Xem album" của phường từ bản đồ.
  - Ảnh bìa địa danh Cầu Rồng là ảnh dâu tây (dữ liệu giả picsum); màn chờ khởi động vẫn là icon Android mặc định; nút "Tiếp theo" ở màn giới thiệu đặt mũi tên → bên trái chữ.
