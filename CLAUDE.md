# Lokap (Lọc kẹt) — hướng dẫn cho Claude Code

Tài liệu gốc: **"Lọc kẹt — Cấu trúc hệ thống v1"** (doc trên claude.ai). Bản export Markdown để ở `docs/system-design.md`.
Khi file này và tài liệu gốc khác nhau, hỏi nhóm trước khi làm.

## Cách làm việc với nhóm

- Trả lời bằng tiếng Việt, ngắn gọn, đi thẳng vào vấn đề. Code kèm giải thích ngắn bằng bullet.
- Với câu hỏi công nghệ: nêu vấn đề trước, đưa 2–3 lựa chọn kèm đánh đổi, **hỏi trước khi chọn hướng**.
- Kiểm tra dữ liệu, phiên bản thư viện trước khi khẳng định.
- Sau mỗi phần việc lớn, đặt 1 câu hỏi gợi mở để người học tự trả lời (nhóm đang thử phương pháp học qua câu hỏi).
- Không tự commit hay push khi chưa được yêu cầu.

## Dự án

App Android kiểu Locket: mở app là camera, chụp và gửi ảnh cho bạn bè. Thêm bản đồ public gom ảnh theo tỉnh/thành → phường/xã (34 tỉnh, không còn cấp quận/huyện từ 01/07/2025), bong bóng địa danh nổi tiếng, feed bạn bè + khám phá, nhắn tin 1-1.

Công nghệ theo slide "Công nghệ" (chốt 12/11/2026, chi tiết và đánh đổi ở `docs/database.md`):

- Android: Kotlin, MVVM + UDF, Hilt, **Jetpack Compose + Material 3** (giữ Compose, slide sửa ô Giao diện thay vì XML), Room · SQLite (cache), CameraX (camera của máy), Coil 3, WorkManager, Glance.
- Bản đồ và thiết bị: **VietMap SDK** (bản đồ) + VietMap API (toạ độ → tỉnh/phường, tìm địa điểm), **Fused Location Provider** (vị trí).
- Dữ liệu và đám mây: **Firebase Authentication**, **Firestore**, **Cloudinary** (ảnh). Gói Firebase **Spark miễn phí → không có Cloud Functions**: luật nằm trong Security Rules + app.
- Push: chưa có (cần Cloud Functions, gói Blaze).

## Cấu trúc repo

```text
Mobile/                       # thư mục làm việc, không phải git
├── Lokap/                    # repo git của app (bên dưới)
├── BaoCao/Tuan1 · Tuan2 · Tuan3/   # slide .pptx, PDF, kịch bản từng tuần
└── LuuTru/supabase/          # backend Supabase cũ, app không còn dùng

Lokap/
├── app/                      # Android, package vn.nhom2.lokap, minSdk 26, targetSdk 37, AGP 9.3, Kotlin 2.2.10
│   └── src/
│       ├── main/java/vn/nhom2/lokap/
│       │   ├── core/         # theme, component dùng chung, điều hướng, mạng, xử lý ảnh
│       │   ├── data/         # local (Room, prefs) · location (Fused, VietMap) · remote (Cloudinary, schema Firestore) · repository (Firebase*) · worker
│       │   ├── di/           # Hilt
│       │   ├── domain/       # model + interface Repository (không phụ thuộc Android)
│       │   └── feature/      # mỗi màn 1 thư mục: XxxScreen (View) · XxxViewModel · XxxContract (UiState)
│       ├── test/             # unit test + dữ liệu giả (data/fake)
│       └── androidTest/      # test trên Firebase thật (opt-in)
├── firebase/
│   ├── firebase.json         # emulator: Auth 9099, Firestore 8080, UI 4000
│   ├── firestore.rules       # luật hiển thị + kiểm tra dữ liệu ghi
│   ├── firestore.indexes.json
│   └── tests/rules.test.js   # `npm test`: 3 tài khoản (chủ, bạn, người lạ) trên Firestore Emulator
├── tools/                    # script Python: tạo logo, tách file Gemini
└── docs/                     # database.md = mô hình Firestore + đánh đổi
```

## Quy tắc bắt buộc (riêng tư và bảo mật)

1. Toạ độ GPS gốc chỉ nằm ở `posts/{id}/private/location`, chỉ tác giả đọc được. Document `posts` chỉ có `displayLat/Lng` đã làm mờ (tâm tỉnh / ô lưới ~2 km / điểm địa danh công khai), cố định, không cộng nhiễu ngẫu nhiên. Mức "Việt Nam" không lưu toạ độ nào.
2. Lọc hiển thị (bạn bè, bài riêng, hẹn giờ, ẩn) nằm trong **Security Rules**; truy vấn của app phải khớp rules (rules không lọc hộ). Mỗi bài có `viewerIds`. Sửa rules thì thêm/sửa test trong `firebase/tests/` và chạy `npm test`.
3. `publicVisibleAt` do app tính lúc gửi, rules bắt lệch ≤ 15 phút so với giờ server.
4. App chỉ có khoá phía client: Firebase (apiKey/appId/projectId), Cloudinary cloud name + **upload preset Unsigned**, key VietMap (giới hạn theo package + SHA-1). **Không bao giờ** đưa Cloudinary API secret, service account Firebase vào app hay vào git.
5. Số trên bong bóng bản đồ đếm từ các bài đã tải mà người dùng có quyền xem; không phải tổng toàn hệ thống.
6. Ảnh Cloudinary là URL công khai khó đoán (postId UUID); app không xoá được ảnh (đã ghi trong docs/database.md).
7. Nhắn tin chỉ giữa bạn bè; nội dung lấy từ Firestore.

## Mô hình dữ liệu (Firestore)

| Collection | Trường chính |
| --- | --- |
| users/{uid} | username, displayName, searchName, avatarUrl, bio, friendCount, createdAt |
| users/{uid}/private/profile | email, birthYear, homeProvinceCode, defaultLocationLevel, notify* |
| usernames/{username} | uid |
| friendships/{uidNhỏ_uidLớn} | users[2], requesterId, addresseeId, status (pending, accepted), createdAt, acceptedAt |
| blocks/{blocker_blocked} | blockerId, blockedId, createdAt |
| posts/{postId} | authorId, author{…}, imageUrl, imagePublicId, caption (≤ 60), audience (friends, direct, public), viewerIds, recipientId, locationLevel (place, ward, province, country), provinceCode/Name, wardCode/Name, placeId/Name, landmarkId, displayLat/Lng, delayDays (0/1/3/7), publicVisibleAt, takenAt, createdAt, status (active, hidden), reactions {uid: emoji}, reportCount |
| posts/{postId}/private/location | lat, lng, accuracyM |
| posts/{postId}/reports/{uid} | reason, createdAt |
| landmarks/{landmarkId} | name, searchName, address, description, provinceCode/Name, wardCode/Name, lat, lng (điểm công khai), vietmapRef, createdBy, createdAt, status (active, hidden), reportCount |
| landmarks/{landmarkId}/reports/{uid} | reason, createdAt |

Ảnh của địa danh là bài **public** có `landmarkId` (toạ độ hiển thị = toạ độ địa danh, rules ép). Ảnh bìa = bài public đã hiện có nhiều cảm xúc nhất, app tự tính (xem `docs/database.md` mục 5b).

## Luật hiển thị (hàm `canRead()` trong `firebase/firestore.rules`)

Viewer V thấy bài P khi V là tác giả, hoặc P.status = active và một trong hai:
- V nằm trong P.viewerIds (bạn bè của tác giả lúc gửi / người nhận bài riêng; bạn bè thấy bài public ngay, kể cả đang hẹn giờ);
- P.audience = public và P.publicVisibleAt ≤ giờ server.

Báo cáo: mỗi người 1 phiếu, tài khoản ≥ 24 giờ, đủ 3 phiếu thì `status = hidden` (rules ép). Chặn: xoá quan hệ bạn + rút tên khỏi viewerIds hai phía; người bị chặn vẫn thấy bài public của người chặn ở Khám phá (giới hạn của Rules).

## Thiết lập lần đầu

1. Firebase Console → tạo project (Spark) → Authentication: bật Email/Password và **Google** → Firestore: tạo database (region `asia-southeast1`).
2. Thêm app Android package `vn.nhom2.lokap` → tải `google-services.json` vào `app/` (bắt buộc: plugin google-services đọc file này; không commit).
   Đăng nhập Google (Credential Manager): **mỗi máy build** thêm SHA-1 debug của mình (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`) bằng `cd firebase && npx firebase apps:android:sha:create <app id> <SHA-1>` (app id = `mobilesdk_app_id` trong google-services.json). File google-services.json phải tải **sau** khi bật Google (`npx firebase apps:sdkconfig ANDROID <app id> -o ../app/google-services.json`) mới có Web client ID (`client_type` 3); thiếu thì nút Google báo "chưa được cấu hình".
3. Đẩy rules + index: `cd firebase && npm install && npx firebase login && npx firebase use --add && npm run deploy`.
4. Cloudinary Console → Upload presets → tạo preset **Unsigned**, folder `lokap` → điền `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_UPLOAD_PRESET` vào `.env`.
5. VietMap Console → API Keys: key của consumer **API** → `VIETMAP_API_KEY` (tìm địa điểm, thêm địa danh, toạ độ → tỉnh/phường); key của consumer **Tilemap** → `VIETMAP_TILE_KEY` (bản đồ nền). Dùng chung 1 key thì bản đồ nền ăn hết hạn mức tìm kiếm (HTTP 423 "Your request is limited"). Không có key: bản đồ vẽ tay, đoán tỉnh theo tâm gần nhất.
6. Chạy thử không cần project thật: `cd firebase && npm run emulators`, đặt `FIREBASE_EMULATOR_HOST` (xem `.env.example`).
7. Build lỗi `jlink ... does not exist`: Gradle đang lấy JRE của extension VS Code → chạy với `JAVA_HOME` trỏ JDK đầy đủ.
8. Firebase BoM giữ **34.12.0**: bản 35.x (auth 25) build bằng Kotlin 2.4, auth 24.1+ bằng 2.3 → Kotlin 2.2.10 không đọc được. Tương tự `googleid` giữ **1.2.0** (1.2.1 là Kotlin 2.4 dù POM ghi stdlib 1.8): kiểm tra `mv=` bằng `javap -v`, đừng tin POM.
9. Bản release (chốt 15/11): ký bằng khoá riêng `~/keystores/lokap-release.jks` (alias `lokap`, để ngoài repo, mất file/mật khẩu là không cập nhật được app). Điền 4 dòng `RELEASE_*` trong `.env`; thiếu thì APK release chưa ký, không cài được. SHA-1 khoá release đã thêm vào Firebase (`./gradlew :app:signingReport` để xem). Cài: `./gradlew :app:installRelease`. Chữ ký khác bản debug nên chuyển qua lại phải gỡ app (mất dữ liệu máy, đăng nhập lại).

## Thiết bị chạy thử (chốt 05/11)
- Dùng **điện thoại Android thật** qua USB (`adb devices` phải thấy máy). **Không bật emulator**: laptop 16 GB RAM không gánh nổi emulator + Gradle (build từng mất 27 phút).
- Trước khi chạy thử: `adb devices`. Không thấy máy thì dừng lại, nhắc người dùng kiểm tra cáp, USB debugging và hộp thoại "Cho phép gỡ lỗi USB" trên điện thoại; không tự bật emulator thay thế.
- Chụp màn hình bằng `adb exec-out screencap -p`, lưu vào `docs/screenshots/`.
- Trước khi gửi lệnh `adb shell input`: kiểm tra `dumpsys window | grep mCurrentFocus` là `vn.nhom2.lokap`. Điện thoại
  thử là máy riêng của thành viên, đang dùng thì không chạm nhầm sang app khác.


## Quy chuẩn giao diện tối giản (chốt 06/11)
- Bớt chữ: bỏ dòng phụ/giải thích; tiêu đề chỉ giữ danh từ chính (ví dụ chỉ icon vị trí + "Đà Nẵng").
- Ưu tiên icon hơn chữ; icon ≥ 24dp, nút chính 28–32dp, vùng chạm ≥ 48dp; luôn có contentDescription tiếng Việt.
- Chữ trên ảnh căn giữa, có nền mờ tối phía sau để đọc được.
- Áp dụng cho mọi màn mới và khi sửa màn cũ.

## Dữ liệu ứng dụng (12/11/2026)

- App luôn dùng Firebase; thiếu cấu hình Firebase thì build báo lỗi, không tự bật dữ liệu mẫu.
- Fixtures và fake repositories chỉ nằm ở `app/src/test`, không đóng gói vào APK.
- Bản đồ, album và hồ sơ dùng bài thật từ repository; nguồn rỗng thì hiển thị rỗng. Danh mục tỉnh ở `data/location/ProvinceCatalog.kt` là dữ liệu tham chiếu.
- Tìm địa điểm dùng VietMap; chưa cấu hình thì báo chưa khả dụng. Không tự chọn avatar, năm sinh hoặc quê quán.
