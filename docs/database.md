# Lokap — Cơ sở dữ liệu (Firebase Firestore + Cloudinary)

Chốt 12/11/2026: làm đúng công nghệ trong slide tuần 1 (slide 10 "Công nghệ"): **Firebase Authentication**,
**Firestore**, ảnh trên **Cloudinary**, cache máy **Room · SQLite**. Gói Firebase **Spark (miễn phí)**: không có
Cloud Functions, nên mọi luật phải nằm trong **Security Rules** (`firebase/firestore.rules`) hoặc trong app.
Bản Supabase cũ đã chuyển ra `Mobile/LuuTru/supabase/`, app không còn dùng.

## 1. Tổng quan

| Thành phần | Dùng gì | Ghi chú |
| --- | --- | --- |
| Đăng nhập | Firebase Authentication, email + mật khẩu | Đăng ký xong Firebase gửi **link** xác nhận (không có mã 6 số). Email chưa xác nhận → coi như chưa đăng nhập, Rules chặn mọi lệnh ghi |
| Dữ liệu | Firestore (NoSQL, document) | Không join được → lưu sẵn tên/ảnh tác giả và danh sách người xem trong mỗi bài |
| Ảnh | Cloudinary, upload preset **Unsigned** | App chỉ có cloud name + tên preset, không có API secret |
| Cache máy | Room (`lokap.db`) | Feed mở là có ngay; bài chờ gửi không mất khi tắt app |
| Vị trí | Fused Location Provider + VietMap API | Reverse v4 (toạ độ → tỉnh/phường), Autocomplete v4 + Place v4 (tìm địa điểm) |

## 2. Collection

```text
users/{uid}                        hồ sơ công khai — mọi người đã đăng nhập đọc được
  username, displayName, searchName (bỏ dấu, để tìm), avatarUrl, bio, friendCount, createdAt
users/{uid}/private/profile        chỉ chủ tài khoản
  email, birthYear, homeProvinceCode, defaultLocationLevel, notify* (4 công tắc)
usernames/{username}               { uid } — giữ username là duy nhất (tạo cùng transaction với users)
friendships/{uidNhỏ_uidLớn}        users [2 uid đã sắp xếp], requesterId, addresseeId, status pending|accepted, createdAt, acceptedAt
blocks/{người chặn_người bị chặn}  blockerId, blockedId, createdAt — chỉ người chặn đọc được
posts/{postId}                     postId = UUID do app tạo
  authorId, author {username, displayName, avatarUrl}
  imageUrl, imagePublicId                 (Cloudinary)
  caption (≤ 60), audience friends|direct|public, recipientId
  viewerIds [tác giả + bạn bè]  hoặc  [tác giả + người nhận] với bài gửi riêng
  locationLevel place|ward|province|country, provinceCode/Name, wardCode/Name, placeId/Name, landmarkId
  displayLat, displayLng                  toạ độ ĐÃ LÀM MỜ
  delayDays 0|1|3|7, publicVisibleAt, takenAt, createdAt (giờ server)
  status active|hidden, reactions {uid: emoji}, reportCount
posts/{postId}/private/location    lat, lng, accuracyM — toạ độ GPS gốc, chỉ tác giả đọc được
posts/{postId}/reports/{uid}       reason, createdAt — mỗi người 1 phiếu
posts/{postId}/comments/{id}       authorId, content (1–1000), parentId, createdAt, updatedAt, votes {uid: -1|1}
posts/{postId}/notificationReceipts/{uid}  createdAt — chống gửi lặp thông báo bài mới
posts/{postId}/reposts/{uid}       uid, createdAt — đăng lại: chỉ ảnh public đã hiện của người khác; ai đăng nhập cũng đọc
                                   (đếm lượt; truy vấn nhóm "reposts" theo uid cho feed bạn bè + tab Đăng lại ở Hồ sơ)
landmarks/{landmarkId}             id = "vm_" + 20 ký tự đầu SHA-256 của mã địa điểm VietMap (cùng địa điểm → cùng document)
  name (2–60), searchName, address, description (≤ 150, có thể null)
  provinceCode/Name, wardCode/Name, lat, lng (điểm CÔNG KHAI của địa điểm, trong khung 8–24°B, 102–118°Đ)
  vietmapRef, createdBy, createdAt (giờ server), status active|hidden, reportCount
landmarks/{landmarkId}/reports/{uid}  reason, createdAt — mỗi người 1 phiếu, đủ 3 phiếu thì ẩn địa danh
conversations/{uidNhỏ_uidLớn}      members [2 uid], createdAt, readAt {uid: thời điểm đọc từ server}
conversations/{id}/messages/{id}   senderId, body (1–1000), createdAt, updatedAt
  replyTo (tuỳ chọn, 15/11) {postId, authorId, imageUrl, caption, takenAt}: tin gửi từ thanh "Gửi tin nhắn…" dưới ảnh;
  rules ép bài đọc được, authorId + imageUrl khớp bài gốc, tác giả là 1 trong 2 thành viên; không sửa được sau khi gửi
users/{uid}/notifications/{id}    type, actorId, postId, conversationId, sourceId, createdAt, isRead
users/{uid}/chatPreferences/{id}  presetId, customWallpaperUri, wallpaperAlignment — chỉ chủ tài khoản
```

Toạ độ hiển thị (`ComposerViewModel`): tỉnh = tâm tỉnh trong `ProvinceCatalog`; phường = tâm ô lưới 0,02° (~2 km)
chứa vị trí (chưa có dữ liệu tâm phường/xã mới); địa điểm = toạ độ của địa điểm công khai; "Việt Nam" = không có
toạ độ và cũng không lưu toạ độ gốc. Điểm cố định, không cộng nhiễu ngẫu nhiên.

## 3. Luật hiển thị trong Security Rules

Viewer V đọc được bài P khi: V là tác giả, **hoặc** `P.status == 'active'` và (V nằm trong `P.viewerIds`,
**hoặc** `P.audience == 'public'` và `P.publicVisibleAt <= request.time`).

Rules **không lọc** dữ liệu: một truy vấn chỉ được chạy khi Firestore chứng minh được mọi kết quả đều đọc được.
Vì vậy app luôn truy vấn đúng dạng sau (đã có test):

| Màn | Truy vấn | Index |
| --- | --- | --- |
| Feed Bạn bè | `viewerIds array-contains me` · `status == active` · `orderBy createdAt desc` | viewerIds + status + createdAt |
| Khám phá | `audience == public` · `status == active` · `publicVisibleAt <= giờ máy − 2 phút` · `orderBy publicVisibleAt desc` | audience + status + publicVisibleAt |
| Hồ sơ của mình | `authorId == me` · `orderBy createdAt desc` | authorId + createdAt |
| Hồ sơ người khác | 2 truy vấn: thêm `viewerIds array-contains me` / thêm `audience == public` + mốc giờ | xem `firestore.indexes.json` |
| Ảnh địa danh | `landmarkId == X` · `audience == public` · `status == active` · `publicVisibleAt <= giờ máy − 2 phút` · `orderBy publicVisibleAt desc` · tối đa 100 | landmarkId + audience + status + publicVisibleAt |
| Danh mục địa danh | `landmarks` · `status == active` · tối đa 500 | (một trường, không cần index) |

Mốc giờ Khám phá lùi 2 phút vì Rules so với giờ **server**: đồng hồ máy chạy nhanh hơn thì truy vấn bị từ chối
(bị từ chối thì app thử lại với 30 phút).

Ghi (tóm tắt `firestore.rules`):

- Mọi lệnh ghi cần `email_verified`. Tạo bài: đúng tác giả, đủ trường, ảnh phải là URL `res.cloudinary.com`,
  `createdAt` = giờ server, `publicVisibleAt` = giờ server + `delayDays` (lệch tối đa 15 phút), mức "Việt Nam"
  không có toạ độ, bài gửi riêng chỉ gửi được cho bạn bè.
- Người xem chỉ được: đổi đúng ô `reactions.<uid mình>`; tự rút tên mình khỏi `viewerIds`; báo cáo (tăng
  `reportCount` đúng 1 kèm phiếu `reports/{uid}` mới, tài khoản tạo trên 24 giờ, phiếu thứ 3 bắt buộc `status = hidden`).
- Tác giả sửa được `caption`, `viewerIds` (luôn còn chính mình) và `author`; xoá bài.
- Gắn địa danh (lúc tạo bài hoặc tác giả gắn bài đã đăng): chỉ bài **public** ở mức `place`, địa danh phải tồn tại
  và đang `active`, `displayLat/Lng` **bằng đúng** toạ độ địa danh, `provinceCode` trùng tỉnh của địa danh.
  Gỡ ra: tác giả chỉ đổi `landmarkId` về null (vị trí giữ ở điểm địa danh, vốn đã công khai).
- `landmarks`: ai đã xác nhận email và có hồ sơ đều thêm được (đúng `createdBy`, `createdAt` = giờ server,
  `status = active`, `reportCount = 0`); không ghi đè, không sửa, không xoá (ảnh của người khác đang gắn vào).
  Báo cáo giống báo cáo bài (đúng +1, kèm phiếu, tài khoản > 24 giờ, người tạo không tự báo cáo, phiếu thứ 3 ẩn).
- Kết bạn: chỉ người được mời mới chấp nhận; không mời được khi một bên đã chặn bên kia; một trong hai người xoá được.
- `users`: username dạng `[a-z0-9._]{3,20}` và phải khớp `usernames/{username}` trong cùng transaction;
  `birthYear` phải đủ 13 tuổi.

## 4. `viewerIds` được cập nhật thế nào

Firestore không join được nên danh sách người xem nằm sẵn trong bài:

- Gửi bài (`UploadPostWorker`): `viewerIds` = mình + bạn bè lúc gửi (bài gửi riêng: mình + người nhận).
- Huỷ kết bạn / chặn (`FirestoreGraph.detachViewers`): bỏ người kia khỏi bài của mình và tự rút tên mình khỏi
  bài của người kia, ngay lập tức.
- Vừa kết bạn / đổi tên, ảnh (`FirestoreGraph.reconcileMyPosts`): mỗi lần làm mới feed, nếu danh sách bạn hoặc hồ sơ
  đổi so với lần trước thì tác giả cập nhật lại toàn bộ bài của mình. Bạn mới sẽ thấy bài cũ của người kia
  **sau khi người đó mở app** (không có server làm hộ).

## 5. Đánh đổi so với bản Supabase (gói Spark không có server)

| Luật cũ (CLAUDE.md) | Bây giờ |
| --- | --- |
| Lọc hiển thị trong Postgres | Trong Security Rules (luật ở trên). Riêng chặn: người bị chặn vẫn thấy bài **public** của người chặn ở Khám phá (Rules không chứng minh được "không bị chặn" cho cả danh sách); app của người chặn tự ẩn bài của người bị chặn |
| `public_visible_at` tính bằng `now()` server | App tính, Rules bắt sai lệch ≤ 15 phút so với giờ server |
| Giới hạn 20 bạn kiểm ở server | Kiểm ở app (Rules không đếm được document); Rules chỉ chặn `friendCount > 20` |
| Bucket ảnh private + signed URL | Cloudinary unsigned: ảnh là URL công khai, chỉ khó đoán nhờ postId ngẫu nhiên. App không xoá được ảnh (cần API secret) |
| Số trên bong bóng bản đồ đếm ở server | Bản đồ vẫn dùng dữ liệu mẫu; đếm thật cần Cloud Functions (gói Blaze) |
| Push FCM từ Edge Function | Chưa có (cần Cloud Functions) |
| Key VietMap chỉ ở server | Key nằm trong app (bản đồ cần key ở máy anyway). Giới hạn key theo package + SHA-1 trong VietMap Console |

Nâng lên gói Blaze sau này thì thêm Cloud Functions cho: ký upload/URL Cloudinary, push, đếm bong bóng; Rules giữ nguyên.

## 5b. Địa danh và ảnh bìa (14/11/2026)

- **Thêm địa danh:** Bản đồ → nút ⭐+ → tìm địa điểm VietMap → sửa tên, mô tả ngắn → Thêm. Id cố định theo mã
  VietMap nên hai người thêm cùng chỗ ra cùng một địa danh (transaction: người sau nhận địa danh của người trước);
  app cũng dùng lại địa danh trùng tên trong bán kính 150 m (VietMap đôi khi có 2 mã cho một chỗ).
- **Đưa ảnh lên:** màn Địa danh → nút 📷+ → *Chụp* (camera của app) · *Thư viện* (Photo Picker, không xin quyền
  bộ nhớ; ảnh vẫn được cắt vuông, bỏ EXIF khi gửi) · *Đã đăng* (gắn bài public cũ của mình). Ảnh địa danh luôn
  là bài public, vị trí hiển thị = điểm của địa danh. Nhấn giữ ảnh của mình để gỡ khỏi địa danh.
- **Ảnh bìa = bài public đã tới giờ có nhiều cảm xúc nhất** (bằng nhau thì ảnh mới hơn). Không có Cloud Functions
  nên app tự tính trên ≤ 100 bài public mới nhất của địa danh; vì ai cũng đọc được đúng tập bài này nên mọi người
  thấy cùng một ảnh bìa. Bài đang hẹn giờ (bạn bè thấy sớm) không được chọn làm bìa.
- **Lưới ảnh:** Nổi bật (cảm xúc giảm dần) · Mới nhất · Bạn bè · Của tôi. Bài bị ẩn (3 báo cáo) không còn trong lưới.
- **Đánh đổi:** địa danh > 100 ảnh thì bìa chỉ xét 100 ảnh mới nhất; bìa trên bong bóng bản đồ tính từ bài đã tải
  về máy nên có thể khác bìa ở màn Địa danh cho tới khi mở màn đó. Ảnh từ thư viện không chứng minh được là chụp
  tại địa danh (chống spam bằng báo cáo bài / báo cáo địa danh). Lên Blaze thì thêm Cloud Function đếm
  `reactionCount` và ghi `coverPostId` vào địa danh.

## 6. Chạy thử và kiểm thử

Bộ request Postman cho Auth, hồ sơ, bài viết, bạn bè, chat và kiểm tra quyền:
[`firebase/requests/README.md`](../firebase/requests/README.md) (kèm environment Emulator/Firebase thật).

```bash
cd firebase
npm install
npm test            # chạy firestore.rules trên Firestore Emulator: 3 tài khoản alice (tác giả) / bob (bạn) / carol (người lạ)
npm run emulators   # Auth :9099, Firestore :8080, giao diện :4000
```

App dùng emulator: trong `.env` đặt `FIREBASE_EMULATOR_HOST`. Điện thoại thật qua USB: chạy
`adb reverse tcp:8080 tcp:8080` và `adb reverse tcp:9099 tcp:9099` rồi đặt `FIREBASE_EMULATOR_HOST=127.0.0.1`.
Link xác nhận email của Auth Emulator hiện trong log và giao diện emulator (localhost:4000).

Đưa Rules + index lên project thật: `npx firebase login`, `npx firebase use --add` (chọn project), `npm run deploy`.

## 7. App dùng dữ liệu thế nào (`app/.../data/`)

| Lớp | Việc |
| --- | --- |
| `repository/FirebaseAuthRepository` | Đăng ký/đăng nhập, xác nhận email bằng link, hồ sơ + username (transaction) |
| `repository/FirestorePostRepository` | Feed, bài, cảm xúc, báo cáo, upload nền (`PostUploader`) |
| `repository/FirestoreSocialRepository` | Bạn bè, chặn, tìm người (tiền tố username / tên bỏ dấu) |
| `repository/FirestoreContentRepository` | Chat, bình luận, thông báo: listener Firestore theo phiên đăng nhập; gỡ listener khi đổi tài khoản |
| `repository/FirestoreSettingsRepository` | `users/{uid}/private/profile` |
| `repository/FirestoreGraph` | Danh sách bạn / chặn, cập nhật `viewerIds` |
| `remote/CloudinaryUploader` | Upload unsigned, thư mục `lokap/posts/<uid>/<postId>` |
| `location/FusedLocationRepository` + `VietMapApi` | Vị trí hiện tại, tỉnh/phường, tìm địa điểm |
| `worker/UploadPostWorker` | Gửi bài nền: Cloudinary → Firestore, thử lại tối đa 5 lần |

Cấu hình Firebase lấy từ `app/google-services.json` qua plugin `com.google.gms.google-services` (4.5.0, khai báo
trong `gradle/libs.versions.toml`); thiếu file thì build báo lỗi. Dữ liệu giả (`data/fake/`) giờ chỉ còn trong unit test.

## 8. Chat, bình luận, thông báo thật (12/11/2026)

Các màn Chat, Hộp thư, Bình luận, Thông báo và các bộ đếm không còn gọi FakeChatStore,
FakeCommentStore hoặc FakeNotificationStore. Dữ liệu hiển thị từ Firestore listeners; SDK có thể giữ cache,
nhưng thành công ghi chỉ được báo khi máy chủ xác nhận. Không tự trả lời tin, giả “đang nhập” hay giả “đã xem”.

- Chat: hai thành viên xem lịch sử; chỉ bạn bè không chặn nhau gửi/sửa được. Người gửi xoá tin của mình.
  Trạng thái đã xem dựa trên `readAt` do người nhận cập nhật bằng thời gian server.
- Bình luận: người xem được bài mới đọc/viết được; chủ bình luận sửa nội dung của mình. Chủ bài có quyền
  xoá bình luận trên bài của mình; mỗi người chỉ sửa được phiếu bầu của chính mình. Khi xoá bình luận cha,
  các câu trả lời còn lại hiển thị ở cấp gốc.
- Thông báo trong app phát sinh từ tin nhắn, bình luận, lời mời/chấp nhận kết bạn, cảm xúc và bài mới.
  Ghi sự kiện và thông báo cùng batch/transaction; rules kiểm tra sự kiện nguồn, không nhận nội dung thông báo tuỳ ý.
  Thông báo bài mới dùng giao dịch nhỏ theo từng người nhận, giữ nguyên kiểm tra chặn và có phiếu giao một lần.
  Người nhận đánh dấu đã đọc/chưa đọc hoặc xoá; người khác không đọc hay sửa được.
- Xoá bài dọn bình luận và phiếu giao thông báo; ảnh Cloudinary vẫn cần dịch vụ máy chủ để xoá.
- Đây là thông báo **trong app**, chưa phải push FCM khi app đã đóng. Đổi ảnh nền chat lưu URI riêng của
  thiết bị vào thiết lập cá nhân; ảnh nền từ thư viện máy chưa đồng bộ tệp ảnh sang thiết bị khác.

Kiểm thử luật: `firebase/tests/rules.test.js`. Kiểm thử Android opt-in:
`LiveFirebaseSocialUiTest` dùng hai tài khoản test đã xác nhận email, thao tác UI thật và đối chiếu
`Source.SERVER` (không chấp nhận cache hay lệnh ghi còn chờ). Mật khẩu chỉ truyền lúc chạy, không lưu trong mã nguồn.
