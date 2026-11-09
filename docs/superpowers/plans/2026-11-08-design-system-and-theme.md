# Giai Đoạn 1: Design System, Theme Sáng/Tối & Logo Mới Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hiện đại hóa nhận diện thương hiệu Lokap bằng cách tích hợp Logo chữ "L" isometric 3D tối giản, xây dựng hệ thống màu Đơn sắc (Monochrome) kết hợp nút Hồng nhẹ (Soft Pink), hỗ trợ chế độ Sáng/Tối (Light & Dark Mode) toàn cục, và thay thế toàn bộ emoji thành Vector Assets.

**Architecture:** Mở rộng `LokapColors` để hỗ trợ 2 bộ màu `DarkLokapColors` và `LightLokapColors`. Quản lý trạng thái theme qua `ThemeManager` với `StateFlow<ThemeMode>` và cung cấp qua `LocalThemeMode`. `MainActivity` đồng bộ icon thanh điều hướng và thanh trạng thái (`enableEdgeToEdge`) theo theme hiện hành. Xuất bộ asset Mipmap cho launcher icon và tạo component `LokapLogo`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android Adaptive Icons, AndroidX Lifecycle, StateFlow.

**Spec:** `docs/superpowers/specs/2026-11-08-design-system-and-theme-design.md`

## Global Constraints
- TargetSdk 37, MinSdk 26, AGP 9.3, package `vn.nhom2.lokap`.
- Không tự ý commit hoặc push git nếu chưa có yêu cầu từ người dùng (theo `CLAUDE.md`).
- Bỏ hoàn toàn các dải màu gradient đa sắc (`regionGradient`) và các màu neon sặc sỡ (`AccentLime`, `AccentCyan`, `AccentLilac`, `AccentYellow`).
- Màu nút chính: Hồng nhẹ `#F472B6`, chữ tương phản `#18181B` (Dark) hoặc `#FFFFFF` (Light).
- Giữ vững cấu trúc MVVM + UDF hiện có của dự án.

## Review Focus
- **Theme switching:** Chuyển đổi giữa Light và Dark mode phải cập nhật tức thì toàn bộ UI mà không gây chớp nháy hoặc crash do CompositionLocal thiếu provider.
- **Edge-to-edge system bars:** Status bar và navigation bar icon phải có độ tương phản đúng (icon tối ở nền sáng, icon sáng ở nền tối).
- **Launcher icon quality:** Các icon mipmap từ mdpi đến xxxhdpi phải sắc nét, căn giữa đúng vùng an toàn của Adaptive Icon.
- **Regression in components:** `Buttons.kt`, `GlassChip.kt`, `AvatarRing.kt` không được tham chiếu đến các token màu cũ đã bị xóa.
- **Build integrity:** Dự án phải compile và pass unit tests bằng `./gradlew testDebugUnitTest`.

---

### Task 1: Tích hợp Logo Chữ "L" Isometric & Tạo Mipmap Launcher Icon

**Files:**
- Create: `tools/generate_logo_assets.py`
- Modify: `app/src/main/res/drawable/ic_launcher_background.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/mipmap-mdpi/ic_launcher.png`, `ic_launcher_round.png`
- Modify: `app/src/main/res/mipmap-hdpi/ic_launcher.png`, `ic_launcher_round.png`
- Modify: `app/src/main/res/mipmap-xhdpi/ic_launcher.png`, `ic_launcher_round.png`
- Modify: `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`, `ic_launcher_round.png`
- Modify: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`, `ic_launcher_round.png`
- Create: `app/src/main/res/drawable/ic_lokap_logo.png`
- Create: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/LokapLogo.kt`

**Interfaces:**
- Consumes: `C:/Users/admin/.gemini/antigravity/brain/18a79ba9-86b0-46cd-9292-a995035b84ae/.user_uploaded/media_1794144952896_8f913622.png`
- Produces: `@Composable fun LokapLogo(modifier: Modifier = Modifier, size: Dp = 48.dp, showText: Boolean = false)`

- [ ] **Step 1: Viết script python tạo assets icon từ ảnh gốc**
Tạo file `tools/generate_logo_assets.py` đọc ảnh gốc `media_1794144952896_8f913622.png`, crop sát viền trong suốt, thêm viền đệm theo tỷ lệ an toàn adaptive icon (66/108), vẽ trên nền hồng nhẹ `#F472B6` và xuất ra:
- `ic_lokap_logo.png` (trong suốt, 512x512)
- Các thư mục mipmap: mdpi (48px), hdpi (72px), xhdpi (96px), xxhdpi (144px), xxxhdpi (192px) cho cả vuông và tròn.

- [ ] **Step 2: Chạy script và kiểm tra file assets sinh ra**
Chạy: `python tools/generate_logo_assets.py`
Expected: Các file PNG được tạo đầy đủ trong `app/src/main/res/drawable/` và các thư mục `mipmap-*/`.

- [ ] **Step 3: Cập nhật `ic_launcher_background.xml` và `ic_launcher_foreground.xml`**
Cập nhật màu nền background `#F472B6`, và foreground tham chiếu tới `ic_lokap_logo` hoặc vẽ vector adaptive icon.

- [ ] **Step 4: Tạo Composable `LokapLogo.kt`**
Tạo component hiển thị Logo chữ "L" kèm tuỳ chọn nhãn văn bản "Lokap" chuẩn typography.

---

### Task 2: Hệ Thống Màu Đơn Sắc + Hồng Nhẹ & Quản Lý ThemeMode (Light / Dark)

**Files:**
- Create: `app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/ThemeMode.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Color.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Theme.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Gradient.kt`
- Create: `app/src/test/java/vn/nhom2/lokap/core/designsystem/theme/ThemeColorTest.kt`

**Interfaces:**
- Produces:
  - `enum class ThemeMode { SYSTEM, LIGHT, DARK }`
  - `object ThemeManager { val themeMode: StateFlow<ThemeMode>; fun setThemeMode(mode: ThemeMode); fun toggleTheme() }`
  - `val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }`
  - `fun DarkLokapColors(): LokapColors`
  - `fun LightLokapColors(): LokapColors`

- [ ] **Step 1: Viết Unit Test kiểm tra Color Tokens & ThemeManager**
Tạo `ThemeColorTest.kt` kiểm tra:
- Khởi tạo mặc định của `ThemeManager` là `ThemeMode.SYSTEM` hoặc lưu cấu hình.
- `DarkLokapColors` có background `#121214` và primary `#F472B6`.
- `LightLokapColors` có background `#F8F9FA` và primary `#F472B6`.
- `toggleTheme()` chuyển đổi đúng giữa LIGHT và DARK.

- [ ] **Step 2: Chạy test để xác nhận FAIL**
Chạy test (sử dụng `$env:JAVA_HOME="D:\AndroidStudio\jbr"`).
Expected: FAIL với lỗi "unresolved reference: ThemeMode / ThemeManager".

- [ ] **Step 3: Cài đặt `ThemeMode.kt`, `Color.kt`, `Theme.kt`, `Gradient.kt`**
- `ThemeMode.kt`: Định nghĩa enum và `ThemeManager`.
- `Color.kt`: Định nghĩa bảng màu đơn sắc Dark/Light và `LokapColors`.
- `Theme.kt`: Bổ sung `LokapLightColorScheme`, hỗ trợ `themeMode: ThemeMode` và `LocalThemeMode`.
- `Gradient.kt`: Thay thế gradient sặc sỡ cũ bằng solid / subtle neutral brush dịu mắt.

- [ ] **Step 4: Chạy test để xác nhận PASS**
Chạy test unit.
Expected: PASS.

---

### Task 3: Refactor Các Component Core & Loại Bỏ Emoji / Gradient Cũ

**Files:**
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/Buttons.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/GlassChip.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/AvatarRing.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/BadgeDot.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/MapBubbles.kt`

**Interfaces:**
- Consumes: `LokapTheme.colors`, `LokapColors` mới
- Produces: Components hiển thị chuẩn trên cả 2 nền Light và Dark mà không vỡ màu/mất tương phản.

- [ ] **Step 1: Cập nhật `Buttons.kt`**
- `PinkButton`: Nền hồng nhẹ `#F472B6`, chữ `#18181B` (ở Dark) / `#FFFFFF` (ở Light), viền tinh tế khi disable/pressed.
- `GhostButton`: Viền `outline`, chữ `textPrimary`, nền trong suốt / `surfaceHigh`.

- [ ] **Step 2: Cập nhật `GlassChip.kt` & `AvatarRing.kt` & `BadgeDot.kt`**
- Bỏ các màu neon viền cũ (`accentCyan`, `accentLime`).
- Thay viền bằng `primary` (hồng nhẹ) khi được chọn hoặc `outline` khi chưa chọn.
- AvatarRing: Viền hồng nhẹ khi có bài mới, viền outline khi không có.

- [ ] **Step 3: Kiểm tra compile các component**
Chạy: `$env:JAVA_HOME="D:\AndroidStudio\jbr"; .\gradlew :app:compileDebugKotlin`
Expected: Biên dịch thành công các component core.

---

### Task 4: Tích Hợp Edge-to-Edge Động & Nút Chuyển Giao Diện Sáng / Tối

**Files:**
- Modify: `app/src/main/java/vn/nhom2/lokap/MainActivity.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsContract.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `ThemeManager.themeMode`, `ThemeMode`
- Produces:
  - Tùy chọn chuyển đổi theme trong màn hình Cài đặt (Hệ thống, Sáng, Tối).
  - Tự động thay đổi icon status bar/navigation bar theo theme.

- [ ] **Step 1: Cập nhật `MainActivity.kt`**
Collect `ThemeManager.themeMode` as state. Xác định `isDark` và gọi `enableEdgeToEdge()` với style phù hợp (Light icon cho Dark theme, Dark icon cho Light theme).

- [ ] **Step 2: Cập nhật `SettingsScreen.kt`**
Thêm nhóm cài đặt "Giao diện" (Appearance) với 3 tùy chọn:
- Theo hệ thống
- Giao diện tối
- Giao diện sáng
Icon hiển thị: `Icons.Rounded.DarkMode` / `Icons.Rounded.LightMode`.

- [ ] **Step 3: Kết nối `SettingsViewModel` và `ThemeManager`**
Khi người dùng chọn chế độ theme trong Settings, gọi `ThemeManager.setThemeMode(...)`.

---

### Task 5: Kiểm Thử Toàn Diện & Biên Dịch Ứng Dụng

**Files:**
- Test: Chạy toàn bộ test unit của dự án và kiểm tra build apk debug.

- [ ] **Step 1: Chạy toàn bộ Unit Tests**
Run: `$env:JAVA_HOME="D:\AndroidStudio\jbr"; .\gradlew testDebugUnitTest`
Expected: Tất cả các unit test pass 100%.

- [ ] **Step 2: Build APK Debug**
Run: `$env:JAVA_HOME="D:\AndroidStudio\jbr"; .\gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.
