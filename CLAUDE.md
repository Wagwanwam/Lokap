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

- Android: Kotlin + Jetpack Compose, MVVM + UDF, Hilt, Room, CameraX, Coil 3, WorkManager, Glance, VietMap SDK. Frontend do Gemini dựng theo tài liệu gốc.
- Backend: **Supabase** (Auth, Postgres + PostGIS, Storage, Realtime, Edge Functions), gói Free. Claude Code dựng phần này.
- Push: FCM, gửi từ Edge Function.

## Cấu trúc repo

```text
Lokap/
├── app/                      # Android, package vn.nhom2.lokap, minSdk 26, targetSdk 37, AGP 9.3
├── supabase/
│   ├── config.toml
│   ├── migrations/           # 0001_extensions, 0002_schema, 0003_rls, 0004_functions, 0005_triggers, 0006_storage
│   ├── seed.sql              # provinces, wards, landmarks, dữ liệu demo
│   ├── tests/                # pgTAP: test RLS và luật hiển thị
│   └── functions/
│       ├── _shared/
│       ├── reverse-geocode/
│       ├── place-search/
│       └── send-push/
├── data/                     # dữ liệu thô: tỉnh, phường/xã, địa danh (CSV/JSON + ghi nguồn)
├── .github/workflows/keepalive.yml
└── docs/
```

## Quy tắc bắt buộc (riêng tư và bảo mật)

1. Toạ độ gốc chỉ nằm trong `post_private_locations`, chỉ chủ bài đọc được. Bảng `posts` chỉ có `display_point` đã làm mờ (tâm phường/tỉnh, cố định, không cộng nhiễu ngẫu nhiên).
2. Mọi việc lọc hiển thị (bạn bè, bài riêng, hẹn giờ, chặn, ẩn) làm trong Postgres (RLS + RPC), không bao giờ ở app.
3. `public_visible_at` tính bằng `now()` của server, không nhận giờ từ app.
4. Bật RLS cho **mọi** bảng. App chỉ có anon key. `service_role`, key VietMap, service account FCM chỉ nằm trong secrets của Edge Functions.
5. Số trên bong bóng bản đồ đếm theo đúng luật hiển thị (không lộ bài hẹn giờ, bài của người bị chặn).
6. Bucket ảnh `posts` là private; app dùng signed URL.
7. Nhắn tin chỉ giữa bạn bè; `send_message` kiểm tra còn là bạn và không chặn nhau.

## Mô hình dữ liệu

| Bảng | Trường chính |
| --- | --- |
| profiles | id (= auth.users.id), username unique, display_name, avatar_path, default_location_level, created_at |
| friendships | requester_id, addressee_id, status (pending, accepted), created_at |
| posts | id (UUID do app tạo), author_id, image_path, caption (≤ 60), audience (friends, direct, public), location_level (place, ward, province, country), province_code, ward_code, place_id, landmark_id, display_point geography, public_visible_at, taken_at, status (active, hidden, deleted), reaction_count, report_count |
| post_private_locations | post_id, exact_point geography, accuracy_m |
| post_recipients | post_id, user_id (cho audience = direct) |
| reactions | post_id, user_id, emoji, created_at |
| provinces | code, name, centroid (34 dòng) |
| wards | code, province_code, name, centroid (~3.300 dòng) |
| landmarks | id, name, province_code, ward_code, point, category, cover_url, cover_attribution, wikidata_id, popularity |
| places | id, vietmap_ref, name, address, point |
| reports | post_id, reporter_id (unique theo cặp), reason, created_at |
| blocks | blocker_id, blocked_id |
| notifications | id, user_id, type, payload jsonb, read_at, created_at |
| device_tokens | user_id, fcm_token, updated_at |
| conversations | id, user_a, user_b (user_a < user_b, unique theo cặp), last_message_at |
| messages | id (UUID do app tạo), conversation_id, sender_id, body, post_id, created_at |
| conversation_reads | conversation_id, user_id, last_read_at |

## Luật hiển thị (một hàm SQL dùng chung, ví dụ `can_view_post(viewer, post)`)

Viewer V thấy bài P khi: P.status = active, hai bên không chặn nhau, và một trong bốn:
- V là tác giả;
- P.audience ∈ (friends, public) và V là bạn của tác giả (bạn bè thấy bài public ngay, kể cả đang hẹn giờ);
- P.audience = direct và V nằm trong post_recipients;
- P.audience = public và public_visible_at ≤ now().

## RPC và Edge Functions

| Tên | Đầu vào | Đầu ra / ghi chú |
| --- | --- | --- |
| create_post | postId, imagePath, caption, audience, level, lat, lng, placeId?, delayDays (0/1/3/7), recipientId? | Upsert theo postId; ghi toạ độ gốc vào bảng riêng; tính display_point theo level; direct thì ghi post_recipients + 1 message |
| get_friend_feed | cursor | Bài bạn bè + bài riêng gửi mình, mới nhất trước |
| get_discover_feed | wardCode, provinceCode (không lưu), cursor, snapshot_at | Xếp hạng: score = W_gần × W_địa_danh × (c+1)/(h+2)^1.5; W_gần 3/2/1 (cùng phường/cùng tỉnh/khác); W_địa_danh 1.5; tối đa 2 bài/người/trang |
| get_map_bubbles | bbox, zoom, filter, timeRange | zoom < 8: theo tỉnh; 8–12: theo phường/xã (+ bong bóng "Khắp tỉnh"); ≥ 13: ghim place; landmark từ zoom 8; level country không lên bản đồ |
| get_region_album, get_landmark | level/code hoặc landmarkId, cursor | |
| react, report_post, block_user | | report ≥ 3 → status = hidden; tài khoản < 24 giờ không được báo cáo |
| get_conversations, get_messages, send_message, mark_read | | Realtime: postgres_changes trên messages; "đang nhập" dùng broadcast, không ghi DB |
| Edge reverse-geocode | lat, lng | provinceCode, wardCode, tên (gọi VietMap Reverse v4, giấu key) |
| Edge place-search | text, lat, lng | Danh sách place |
| Edge send-push | gọi từ trigger (pg_net) | FCM HTTP v1, data message |

Phân trang bằng cursor (created_at, id), không dùng offset.

## Thứ tự việc backend

1. `supabase init`; migration bật `postgis`, `pg_net`, `pg_cron`.
2. Schema + index (GIST cho geography, index cho cursor).
3. RLS cho mọi bảng + hàm `can_view_post`, `are_friends`, `is_blocked`.
4. RPC theo bảng trên.
5. Storage bucket `posts` (private) + policy: chỉ ghi vào `posts/<uid>/...`.
6. Trigger: report → ẩn bài; insert posts/messages → gọi send-push.
7. Seed: 34 tỉnh, phường/xã, địa danh. **Chưa có nguồn dữ liệu phường/xã mới kèm toạ độ tâm: tìm nguồn, đề xuất cho nhóm và hỏi trước khi dùng.**
8. Edge Functions.
9. Test pgTAP: đăng nhập bằng 3 tài khoản (chủ, bạn, người lạ) để kiểm tra từng luật ở mục trên.
10. GitHub Actions gọi API mỗi ngày để project Free không bị tạm dừng.

## Thiết lập lần đầu

1. Tạo project trên supabase.com, chọn region Singapore (gần Việt Nam nhất).
2. Cài Supabase CLI, rồi: `supabase login` → `supabase link --project-ref <ref>`.
3. Đẩy schema: `supabase db push`. Chạy local (cần Docker): `supabase start`.
4. Secrets: `supabase secrets set VIETMAP_API_KEY=... FCM_SERVICE_ACCOUNT_JSON=...`
5. Deploy function: `supabase functions deploy <tên>`.
6. App Android chỉ nhận `SUPABASE_URL` và `SUPABASE_ANON_KEY` qua `local.properties` → BuildConfig; không commit file này.

## Thiết bị chạy thử (chốt 05/11)
- Dùng **điện thoại Android thật** qua USB (`adb devices` phải thấy máy). **Không bật emulator**: laptop 16 GB RAM không gánh nổi emulator + Gradle (build từng mất 27 phút).
- Trước khi chạy thử: `adb devices`. Không thấy máy thì dừng lại, nhắc người dùng kiểm tra cáp, USB debugging và hộp thoại "Cho phép gỡ lỗi USB" trên điện thoại; không tự bật emulator thay thế.
- Chụp màn hình bằng `adb exec-out screencap -p`, lưu vào `docs/screenshots/`.


## Quy chuẩn giao diện tối giản (chốt 06/11)
- Bớt chữ: bỏ dòng phụ/giải thích; tiêu đề chỉ giữ danh từ chính (ví dụ chỉ icon vị trí + "Đà Nẵng").
- Ưu tiên icon hơn chữ; icon ≥ 24dp, nút chính 28–32dp, vùng chạm ≥ 48dp; luôn có contentDescription tiếng Việt.
- Chữ trên ảnh căn giữa, có nền mờ tối phía sau để đọc được.
- Áp dụng cho mọi màn mới và khi sửa màn cũ.
