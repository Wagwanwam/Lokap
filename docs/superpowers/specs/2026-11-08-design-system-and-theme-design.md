# Thiết Kế Chi Tiết: Giai Đoạn 1 - Design System, Theme Sáng/Tối & Logo Mới

## 1. Mục tiêu
Nâng cấp và làm mới nhận diện giao diện của ứng dụng Lokap:
- **Logo mới:** Sử dụng biểu tượng chữ "L" isometric 3D tối giản (từ ảnh người dùng cung cấp `media_1794144952896_8f913622.png`), gồm hai thẻ bo góc 3D phẳng tạo hình chữ "L" đại diện cho Lokap (Lọc kẹt). Logo được đưa vào launcher icon trên nền hồng nhẹ/đơn sắc và hiển thị trong app.
- **Bảng màu đơn sắc (Monochrome) & Hồng nhẹ (Soft Pink):** Tinh gọn màu sắc, loại bỏ hoàn toàn các gradient đa sắc (`regionGradient`) và các màu phụ chói mắt (`AccentLime`, `AccentYellow`, `AccentCyan`, `AccentLilac`).
- **Chế độ Sáng / Tối (Light & Dark Mode):** Hỗ trợ chuyển đổi giao diện sáng/tối linh hoạt thông qua State toàn cục (`ThemeConfig`), đồng bộ với thanh trạng thái hệ thống (`enableEdgeToEdge`).
- **Vector Icons:** Thay thế toàn bộ emoji trong UI bằng Material Symbols / Compose Vector Icons chuẩn.
- **Tối giản nội dung văn bản (Clean text):** Rút gọn câu chữ, bỏ các văn bản phụ thừa thãi.

---

## 2. Kiến trúc & Thiết kế kỹ thuật

### 2.1. Quản lý Theme & ThemeMode
- Định nghĩa enum `ThemeMode`:
  ```kotlin
  enum class ThemeMode {
      SYSTEM, LIGHT, DARK
  }
  ```
- Định nghĩa `ThemeRepository` / `ThemeManager`:
  - Lưu trữ trạng thái `themeMode` thông qua `StateFlow<ThemeMode>` trong app scope.
  - Cung cấp hàm `setThemeMode(mode: ThemeMode)` và `toggleTheme()`.
- Cập nhật `LokapTheme`:
  - Bổ sung `LokapLightColorScheme` song song với `LokapDarkColorScheme`.
  - Hỗ trợ `isDark: Boolean = when (themeMode) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme() }`.
  - Cập nhật `LocalLokapColors provides if (isDark) darkColors else lightColors`.
  - Cung cấp `LocalThemeMode` để các màn hình con (TopBar, Settings) dễ dàng đọc và đổi theme.
- Cập nhật `MainActivity`:
  - Đồng bộ `enableEdgeToEdge` với icon thanh trạng thái (status bar) và navigation bar:
    - Khi ở Dark Mode: System bar nền trong suốt, icon màu sáng.
    - Khi ở Light Mode: System bar nền trong suốt, icon màu tối.

### 2.2. Bảng mã màu chuẩn (Monochrome + Soft Pink)
| Thành phần | Dark Mode | Light Mode | Ý nghĩa |
| --- | --- | --- | --- |
| `background` | `#121214` | `#F8F9FA` | Nền ứng dụng chính |
| `surface` | `#1C1C21` | `#FFFFFF` | Nền thẻ, card, dialog |
| `surfaceHigh` | `#26262D` | `#F1F3F5` | Nền chip, ô nhập liệu |
| `outline` | `#2D2D36` | `#E4E4E7` | Viền ngăn cách |
| `primary` (Hồng nhẹ) | `#F472B6` | `#F472B6` | Nút chính, điểm nhấn |
| `primarySoft` | `#FBCFE8` | `#FDF2F8` | Nền phụ của nút hồng |
| `onPrimary` | `#18181B` | `#FFFFFF` | Chữ / icon trên nút hồng |
| `textPrimary` | `#F4F4F5` | `#18181B` | Tiêu đề, chữ chính |
| `textSecondary` | `#9CA3AF` | `#6B7280` | Phụ đề, mô tả |
| `textTertiary` | `#6B7280` | `#9CA3AF` | Placeholder, nhãn mờ |
| `success` | `#34D399` | `#10B981` | Dấu tick hoàn thành |
| `error` | `#F87171` | `#EF4444` | Trạng thái lỗi |

*Lưu ý:* Bỏ hoàn toàn các token gradient và thay bằng màu đơn sắc tinh tế `SurfaceHigh` hoặc `Outline`.

### 2.3. Logo & App Launcher Icon
- **Nguồn biểu tượng:** Ảnh `media_1794144952896_8f913622.png` (500x500 RGBA, chữ "L" isometric 3D tối giản gồm 2 thẻ vuông bo góc, nền trong suốt).
- **Icon Launcher thích ứng (Adaptive Icon):**
  - **Foreground (`ic_launcher_foreground`):** Chứa biểu tượng chữ "L" isometric căn giữa tỷ lệ chuẩn adaptive icon (tối ưu vùng an toàn 66dp/108dp).
  - **Background (`ic_launcher_background.xml`):** Nền hồng nhẹ `#F472B6` (hoặc Dark neutral `#121214`), đơn sắc, không gradient.
- **Mipmap Assets:** Tự động tạo và xuất ảnh PNG sắc nét theo đầy đủ mật độ:
  - `mipmap-mdpi` (48x48)
  - `mipmap-hdpi` (72x72)
  - `mipmap-xhdpi` (96x96)
  - `mipmap-xxhdpi` (144x144)
  - `mipmap-xxxhdpi` (192x192)
- **In-App Branding:** Đưa logo chữ "L" vào component `LokapLogo` dùng ở màn hình Splash / Onboarding / Login và TopBar.

### 2.4. Vector Assets thay thế Emoji
- **Camera:** Thay 📸 bằng `Icons.Rounded.CameraAlt`
- **Vị trí:** Thay 📍 bằng `Icons.Rounded.LocationOn`
- **Thông báo:** Thay 🔔 bằng `Icons.Rounded.Notifications`
- **Kiểm tra/Hoàn thành:** Thay ✅ bằng `Icons.Rounded.CheckCircle` hoặc `Icons.Rounded.Check`
- **Cài đặt:** `Icons.Rounded.Settings`
- **Chế độ sáng/tối:** `Icons.Rounded.DarkMode` & `Icons.Rounded.LightMode`

---

## 3. Các tệp tin cần tạo mới & chỉnh sửa

1. **`app/src/main/res/drawable/`:**
   - `ic_launcher_background.xml`: Cập nhật màu nền hồng nhẹ `#F472B6`.
   - `ic_launcher_foreground.xml`: Vẽ vector 3 khung ảnh polaroid xếp lớp.
2. **`core/designsystem/theme/Color.kt`:**
   - Cập nhật `LokapColors` hỗ trợ 2 bộ màu `DarkLokapColors` và `LightLokapColors`.
   - Loại bỏ các token gradient và token màu phụ neon.
3. **`core/designsystem/theme/Theme.kt`:**
   - Bổ sung `LokapLightColorScheme`.
   - Bổ sung `ThemeMode` và `LocalThemeMode`.
   - Cập nhật `LokapTheme(themeMode: ThemeMode = ...)` chuyển đổi mượt mà giữa Sáng và Tối.
4. **`core/designsystem/theme/Gradient.kt`:**
   - Tối giản hóa hoặc gỡ bỏ `regionGradient` chuyển về màu đơn sắc tinh tế theo theme.
5. **`core/designsystem/component/Buttons.kt`, `GlassChip.kt`, `AvatarRing.kt`:**
   - Cập nhật theo màu đơn sắc mới và màu nút hồng nhẹ.
6. **`MainActivity.kt`:**
   - Kết nối với `ThemeManager` để cập nhật động `enableEdgeToEdge` status bar / navigation bar.

---

## 4. Kế hoạch kiểm thử & nghiệm thu
- Chạy `./gradlew :app:assembleDebug` đảm bảo build thành công 100%.
- Kiểm tra tính nhất quán hiển thị trên cả Dark Mode và Light Mode.
- Kiểm tra Icon hiển thị chuẩn trên màn hình chính Android.
