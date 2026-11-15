# Request Firebase cho Lokap

Bộ **51 request Postman** dùng Firebase Authentication REST và Firestore REST, theo schema và
Security Rules của dự án. Đây là request thử API, không thay thế Firebase SDK/realtime listener trong app.

## Import và chọn môi trường

### Postman v12 Local Mode trên máy hiện tại

Các file đã được Upgrade sang YAML v3. Bản đang dùng trong workspace **Mobile** nằm ở:

- `Mobile/postman/collections/Lokap · Firebase Auth & Firestore/` — 51 request.
- `Mobile/postman/environments/Lokap · Emulator · A.environment.yaml`.
- `Mobile/postman/environments/Lokap · Firebase · A.environment.yaml`.

Mở **Collections → Lokap · Firebase Auth & Firestore** ở thanh bên trái, rồi chọn environment
ở góc trên phải. Không kéo các YAML này vào hộp Import: bản Postman hiện tại báo Incorrect format
với cách đó. Workspace đã tự nhận chúng qua thư mục `postman/`.

Trên máy này, URL, `projectId`, `databaseId` và API key đã được **thay bằng giá trị thật trực tiếp**
trong cả 51 request của collection Firebase: thanh URL, query parameters và đường dẫn document
trong body không còn biến cấu hình. Chỉ giữ biến tài khoản/dữ liệu như `uid`, `postId`, `peerUid`.
Collection **Lokap · Firebase Emulator** dùng URL local và project `demo-lokap` riêng; chọn environment
Emulator khi dùng collection đó. Request `08.1` chỉ chạy trong collection Emulator.
Không đổi environment để chuyển cùng một collection giữa Firebase thật và Emulator; chọn đúng cả
collection lẫn environment. Script sẽ chặn khi project của environment không khớp.
Chỉ cần điền email/mật khẩu để đăng nhập; token/UID lấy từ response. Những thao tác khác vẫn cần
nội dung hoặc đối tượng tương ứng như `username`, `peerUid`, `postId`, `message`, `caption`.

Để cập nhật lại URL từ cấu hình app, chạy `node Lokap/firebase/requests/bind-firebase.mjs`
từ thư mục `Mobile`. Script chỉ sửa file local, không gửi request tới Firebase.

Bản YAML trong thư mục `Lokap/firebase/requests/` giữ làm bản tham chiếu; khi sửa request đang dùng,
sửa bản trong `Mobile/postman/`. Các bước JSON dưới đây dành cho bản xuất v2.1 tạo lại bằng script.

### Bản JSON v2.1

Trong Postman, chọn **Import** rồi nhập:

okap.postman_collection.json`
- `Emulator.postman_environment.json` để chạy local, hoặc `Firebase.postman_environment.json` để dùng project thật.

Chọn environment ở góc trên phải. Gửi từng request theo luồng bên dưới; **không Run cả collection**:
có bước đổi tài khoản, gửi email, các request cố ý trả 403 và nhóm xóa dữ liệu.

### Emulator

Từ thư mục `Lokap/firebase`:

```bash
npm install
npm run emulators
```

Environment đã đặt project `demo-lokap`, API key giả, Auth `127.0.0.1:9099`, Firestore
`127.0.0.1:8080`. Email `lokap.a@example.test` và mật khẩu đi kèm chỉ là dữ liệu local.
Emulator xác minh email qua mã trong request `08.1`; không gửi email thật.

Nếu cổng 8080 đang được dùng, tạo bản sao `firebase.json`, đổi `emulators.firestore.port`,
khởi động CLI với `--config <file-bản-sao>` và đổi `firestoreBaseUrl` tương ứng trong Postman.
Nếu đổi cổng Auth, sửa cả `authBaseUrl`, `tokenBaseUrl` và `authEmulatorOrigin`.

### Firebase thật

Điền các biến sau trong environment `Lokap · Firebase · A`:

| Biến | Giá trị |
| --- | --- |
| `projectId` | `project_info.project_id` trong `app/google-services.json` |
| `apiKey` | API key của project, từ Firebase Console hoặc `client[].api_key[].current_key` |
| `email`, `password` | Tài khoản dùng để thử |
| `username` | Tên duy nhất, 3–20 ký tự `[a-z0-9._]` |
| `displayName`, `bio`, `birthYear` | Tên 2–30 ký tự, bio tối đa 60 ký tự, năm sinh đủ 13 tuổi |

Bật provider Email/Password, tạo Firestore và triển khai rules/index của repo trước khi thử.
Giữ `databaseId=(default)` trừ khi project dùng database khác. Nếu API key giới hạn chỉ cho ứng dụng
Android, REST từ Postman có thể bị từ chối; dùng cấu hình key phù hợp cho công cụ thử API,
không gỡ giới hạn key đang dùng của app chỉ để chạy thử.

Các file mẫu không chứa key, mật khẩu hay token thật. Chỉ điền chúng trong environment local của
Postman; không export environment đã có token vào git. Nếu cần lưu file trong thư mục này,
dùng tên `*.local.postman_environment.json` (đã được ignore).

## Luồng một tài khoản

1. `01.1` đăng ký (hoặc `01.2` đăng nhập nếu đã có tài khoản).
2. `01.3` gửi link xác minh. Emulator: chạy `08.1` để tự lưu `oobCode`, rồi `01.4`.
   Firebase thật: mở link trong email; hoặc lấy tham số `oobCode` từ link, điền vào environment rồi chạy `01.4`.
3. Chạy `01.2` hoặc `01.5` **sau khi xác minh** để token mới chứa `email_verified=true`.
   `01.6` kiểm tra tài khoản Auth.
4. Điền username, chạy `02.1`. **404 là kết quả đúng khi tên chưa được dùng**.
5. `02.2` tạo hồ sơ công khai, hồ sơ riêng và giữ username trong một commit. Chỉ chạy cho tài khoản
   chưa có hồ sơ; lần sau đọc bằng `02.3`/`02.4`, sửa bằng `02.5`/`02.6`.
6. `04.1` tạo bài public. Mỗi lần gửi tạo `postId` mới và lưu `postAuthorId`.
   `04.4` đọc bài, `04.7` đọc bài của mình. `04.6` truy vấn khám phá lùi 2 phút như app,
   nên bài mới cần đợi hơn 2 phút để có thể xuất hiện.

Đăng nhập/đăng ký/refresh tự lưu `idToken`, `refreshToken`, `uid`. Các request Firestore kế thừa
Bearer token này. Khi token hết hạn, chạy `01.5`.

## Luồng hai tài khoản A/B

Nhân đôi environment thành **A** và **B**. Với B, đổi email/username, xóa các biến token/UID của A
nếu đã có, rồi đăng ký, xác minh và tạo hồ sơ như trên. Đặt `peerUid` của A bằng UID của B và ngược lại.
`pairId` được tính bằng hai UID đã sắp xếp.

| Bước | Tài khoản | Request |
| --- | --- | --- |
| Mời kết bạn | A | `03.2` |
| Chấp nhận | B | `03.3` |
| Xem quan hệ | A hoặc B | `03.1`, `03.4` |
| Đăng bài bạn bè kèm GPS riêng | A | `04.2` |
| Chép `postId` từ A sang B, đọc bài | B | `04.4` (tự lưu `postAuthorId`) |
| Feed bạn bè, cảm xúc | B | `04.5`, `04.9` |
| Bình luận và thông báo cho A | B | `05.2` |
| Tác giả tự bình luận, không tự gửi thông báo | A | `05.3` |
| Tạo cuộc trò chuyện (một lần) | A | `06.1` |
| Gửi tin, đọc, sửa, đánh dấu đã xem | A/B | `06.3`–`06.6` (chỉ người gửi sửa tin mình) |
| Đọc thông báo, đánh dấu đã đọc | Người nhận | `07.1`, `07.2` |
| Kiểm tra quyền riêng tư | B | `09.1`–`09.4`, kết quả đúng là **403** |

`05.1` đọc bình luận. `commentId` và `messageId` tự tạo khi thêm mới, giữ lại để sửa/xóa;
muốn thao tác một mục của người kia thì chép ID tương ứng sang environment đang dùng.
`07.1` tự chọn ID thông báo mới nhất cho request `07.2`.

## Các chi tiết cần biết

- Timestamp `createdAt`, `updatedAt`, `acceptedAt`, `readAt` dùng `REQUEST_TIME` của Firestore.
  Đồng hồ máy chỉ dùng cho `takenAt`, hẹn giờ public và mốc truy vấn khám phá.
- `delayDays` nhận `0`, `1`, `3`, `7`; sửa trước khi tạo bài public. Request friends/direct luôn dùng 0.
- Bài friends mẫu có người xem là mình và `peerUid`; bài public mẫu có mình trong `viewerIds`.
  App thật tính danh sách toàn bộ bạn bè qua repository.
- Mẫu public/direct dùng mức `country`: không có tọa độ. Mẫu friends dùng `province`: tọa độ hiển thị
  là tâm Đà Nẵng, GPS mẫu nằm trong `posts/{postId}/private/location`, chỉ tác giả đọc được.
- Ảnh mẫu là ảnh Cloudinary demo công khai. Điền `imageUrl`/`imagePublicId` sau khi upload ảnh của bạn;
  bộ này không upload/xóa ảnh Cloudinary.
- `04.11` báo cáo cần hồ sơ người báo cáo đã tạo **hơn 24 giờ**, khác tác giả và chưa báo cáo bài đó.
  Chạy `04.4` ngay trước để lấy `reportCount` và `postUpdateTime`; commit có precondition chống mất
  cập nhật khi có người báo cáo đồng thời. Nếu precondition lỗi, đọc lại rồi gửi lại. Phiếu thứ 3 ẩn bài.
- Các thao tác kết bạn, gửi tin và bình luận cho người khác kèm thông báo trong cùng commit.
  `04.9` chỉ minh họa cập nhật cảm xúc; không kèm thông báo REACTION. Tạo bài không gửi thông báo NEW_POST.
- Chưa bao gồm quy trình hủy bạn/chặn/xóa tài khoản: app phải rút `viewerIds` và dọn dữ liệu liên quan.
  Xóa riêng document quan hệ không tương đương đầy đủ thao tác hủy bạn.
- Nhóm `10` xóa từng mục đã chọn. Xóa comment và receipt trước khi xóa bài (receipt phải xóa cùng
  commit với bài). Firestore không tự xóa subcollection; phiếu reports và ảnh Cloudinary còn lại.

## Lỗi thường gặp

| Kết quả | Kiểm tra |
| --- | --- |
| `PERMISSION_DENIED` / 403 | Email đã xác minh và đã refresh token chưa; UID đúng chủ chưa; query đủ điều kiện Rules chưa |
| `ALREADY_EXISTS` / 409 | Hồ sơ, username hoặc cuộc trò chuyện đã có; dùng request đọc/sửa thay cho tạo |
| `FAILED_PRECONDITION` | Index chưa triển khai/xây xong, hoặc `postUpdateTime` báo cáo đã cũ |
| `INVALID_ID_TOKEN`, token hết hạn | Chạy `01.2`/`01.5`; không dùng token Emulator cho Firebase thật |
| 400 khi tạo hồ sơ/bài | Kiểm tra kiểu dữ liệu JSON của Firestore; giới hạn nội dung của Rules thường trả 403 |
| Kết nối localhost thất bại | Khởi động Emulator, kiểm tra cổng và dùng Postman Desktop/Desktop Agent |

## Cập nhật collection

Nguồn nằm trong `build-collection.mjs`; sau khi sửa, tạo lại ba file JSON mẫu:

```bash
cd Lokap/firebase
node requests/build-collection.mjs
```

Script chỉ tạo file local; không gọi Firebase. Biến có hậu tố `Json` do pre-request script tạo bằng
`JSON.stringify`, nên mật khẩu, tên và nội dung có dấu nháy/xuống dòng vẫn gửi đúng. Sửa biến gốc
trong environment, không cần tự điền các biến `Json`.

Kiểm tra ngày 13/11/2026 bằng Newman trên Auth/Firestore Emulator: **51/51 mẫu request**, 97 lượt
gọi, 110 assertion thành công. Có kiểm tra email chưa xác minh, quyền riêng tư, giữ cảm xúc của
người khác khi cập nhật và ẩn bài sau 3 báo cáo. Kiểm thử báo cáo dùng fixture local có hồ sơ
được đặt lùi 2 ngày; request bình thường vẫn chịu điều kiện >24 giờ. Chưa chạy trên project thật;
Emulator không kiểm tra yêu cầu composite index như Firestore thật.

Tham khảo: [Firebase Auth REST](https://firebase.google.com/docs/reference/rest/auth),
[Firestore REST và Bearer token](https://firebase.google.com/docs/firestore/use-rest-api),
[Firestore Write và server timestamp](https://firebase.google.com/docs/firestore/reference/rest/v1/Write),
[Auth Emulator](https://firebase.google.com/docs/emulator-suite/connect_auth).
