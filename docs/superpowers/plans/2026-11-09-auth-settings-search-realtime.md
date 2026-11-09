# Auth 2-Step, Grouped Settings, Dedicated Search, Realtime Presence & Profile Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the registration flow into a 2-step process, separate search into a dedicated screen with vector icons, relocate theme switching into a grouped-card Settings screen, implement an Android-native Realtime Presence system (Option C), build branded loading animations, and modernize the Profile and Find Friends UI.

**Architecture:** MVVM + UDF pattern with Jetpack Compose, Android `ConnectivityManager` NetworkCallback for connectivity and heartbeat-driven presence, dedicated navigation routes, and component-driven Material 3 design system.

**Tech Stack:** Kotlin 2.1.10, Jetpack Compose, Material 3, Coroutines / StateFlow, AndroidX Navigation Compose, Coil 3.

**Spec:** `C:\Users\admin\.gemini\antigravity\brain\18a79ba9-86b0-46cd-9292-a995035b84ae\spec_redesign_auth_settings_realtime.md`

## Global Constraints
- Target SDK 37, Min SDK 26, JVM 21 (`D:\AndroidStudio\jbr`).
- All icons MUST be vector `ImageVector` from Material Icons (no emoji as icons).
- Do not commit or push to git automatically (`CLAUDE.md` rule: "Không tự commit hay push khi chưa được yêu cầu").
- Maintain backward compatibility of existing fake stores and routes.
- Respond in Vietnamese, concise and direct.

## Review Focus
- Offline disconnection handling while navigating between screens (graceful fallback).
- Age validation in registration (must be at least 13 years old).
- Username availability debouncing during profile setup (no duplicate requests).
- Theme change persistence in `ThemeManager` when toggled from Settings.
- Dark and Light mode color contrast in grouped settings cards.

---

### Task 1: Realtime Connection & Presence System (Option C)

**Files:**
- Create: `app/src/main/java/vn/nhom2/lokap/core/network/ConnectivityObserver.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/core/network/NetworkConnectivityObserver.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/domain/model/UserPresence.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/data/presence/PresenceStore.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/OnlineIndicatorDot.kt`
- Test: `app/src/test/java/vn/nhom2/lokap/core/network/PresenceLogicTest.kt`

- [ ] Write failing test in `PresenceLogicTest.kt` verifying `UserPresence` states (`ONLINE`, `RECENTLY_ACTIVE`, `OFFLINE`) and `PresenceStore` status transitions.
- [ ] Create `ConnectivityObserver.kt` interface with `observe(): Flow<Status>` (`AVAILABLE`, `LOST`).
- [ ] Implement `NetworkConnectivityObserver.kt` using Android `ConnectivityManager`.
- [ ] Implement `PresenceStore.kt` tracking online states and heartbeats.
- [ ] Implement `OnlineIndicatorDot.kt` composable (green dot with border).
- [ ] Run `PresenceLogicTest.kt` and confirm test passes.

---

### Task 2: Global Loading Animations (Pulse Radar, Skeleton Shimmer, Spinner)

**Files:**
- Create: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/LokapPulseLoader.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/LokapShimmer.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/Buttons.kt`

- [ ] Create `LokapPulseLoader.kt` with infinite transition radar ripple and camera shutter breathing animation using Lokap neon pink/coral gradient.
- [ ] Create `LokapShimmer.kt` providing `.lokapShimmer()` modifier with animated gradient shimmer brush.
- [ ] Update `PinkButton` in `Buttons.kt` to use smooth vector circular progress spinner when `loading = true`.
- [ ] Verify Gradle compile for component updates.

---

### Task 3: Rebuild Auth & Registration as 2-Step Flow

**Files:**
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/auth/AuthValidators.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/auth/RegisterContract.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/auth/RegisterViewModel.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/auth/RegisterScreen.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/auth/setup/SetupProfileContract.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/auth/setup/SetupProfileViewModel.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/auth/setup/SetupProfileScreen.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/data/fake/FakeAuthStore.kt`
- Test: `app/src/test/java/vn/nhom2/lokap/feature/AuthLogicTest.kt`

- [ ] Add birthday validation in `AuthValidators.kt` (`validateBirthYear` / `>= 13` years old).
- [ ] Update `RegisterContract.kt` and `RegisterViewModel.kt` to handle Step 1 (Email, Password Strength, Confirm Password, Terms) and navigate to `SetupProfileRoute` on success.
- [ ] Update `RegisterScreen.kt` to show sleek Step 1 UI with step indicator (Bước 1/2: Tài khoản & Bảo mật).
- [ ] Overhaul `SetupProfileContract.kt`, `SetupProfileViewModel.kt`, and `SetupProfileScreen.kt` as Step 2 (Bước 2/2: Thiết lập hồ sơ cá nhân) with Avatar color picker, Full Name, `@username` real-time availability, Birthday picker, Province dropdown, and Bio input.
- [ ] Update unit tests in `AuthLogicTest.kt` and run to confirm pass.

---

### Task 4: Dedicated Search Screen & TopBar Navigation

**Files:**
- Modify: `app/src/main/java/vn/nhom2/lokap/core/navigation/Route.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/feature/search/SearchContract.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/feature/search/SearchViewModel.kt`
- Create: `app/src/main/java/vn/nhom2/lokap/feature/search/SearchScreen.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/feed/FeedScreen.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/navigation/AppNavHost.kt`
- Test: `app/src/test/java/vn/nhom2/lokap/feature/SearchLogicTest.kt`

- [ ] Add `data object Search : Route` in `Route.kt`.
- [ ] Create `SearchContract.kt` and `SearchViewModel.kt` supporting Tab People (Mọi người) and Tab Places (Địa danh).
- [ ] Create `SearchScreen.kt` with auto-focus search bar, recent search tags, and refined result cards.
- [ ] Update `FeedTopBar` in `FeedScreen.kt`: remove theme toggle button, replace with vector `Icons.Rounded.Search` opening `Route.Search`, and streamline tabs to `[ Khám phá | Bạn bè ]`.
- [ ] Register `searchScreen` in `AppNavHost.kt`.
- [ ] Add test in `SearchLogicTest.kt` verifying search filtering.

---

### Task 5: Rebuild Settings Screen with Grouped Cards Layout

**Files:**
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsContract.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsScreen.kt`

- [ ] Update `SettingsContract.kt` with actions for theme switching (`LIGHT`, `DARK`, `SYSTEM`), managing recovery phone, and linked emails.
- [ ] Rebuild `SettingsScreen.kt` with `SettingsGroupCard` component:
  - Nhóm 1: Tài khoản & Bảo mật (Đổi mật khẩu, Thêm SĐT khôi phục, Email liên kết).
  - Nhóm 2: Giao diện & Hiển thị (Chế độ màu Sáng/Tối/Hệ thống).
  - Nhóm 3: Quyền riêng tư & Bản đồ (Cấp độ vị trí, Ẩn/Hiện trạng thái Online).
  - Nhóm 4: Quyền ứng dụng (Camera, Vị trí, Thông báo với tick/x).
  - Nhóm 5: Dữ liệu & Lưu trữ (Xoá cache).
  - Nhóm 6: Lokap & Đăng xuất (Điều khoản, Về Lokap, Đăng xuất).
- [ ] Verify dark and light theme rendering of grouped cards.

---

### Task 6: Rebuild Profile Screen & Find Friends UI

**Files:**
- Modify: `app/src/main/java/vn/nhom2/lokap/feature/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/vn/nhom2/lokap/core/designsystem/component/AvatarRing.kt`

- [ ] Add optional `showOnlineIndicator: Boolean = false` to `AvatarRing.kt`.
- [ ] Rebuild `ProfileHeaderSection` in `ProfileScreen.kt`: balanced glass stats card, sleek gradient avatar ring with online dot, refined check-in badge, compact edit profile button.
- [ ] Modernize `ProfileTabSelector` and photo grid with smooth corner radius.
- [ ] Modernize friend recommendation item card with `14.5.sp` display name, `12.5.sp` username, online dot, and `32.dp` pill button.

---

### Task 7: Comprehensive Verification & End-to-End Build

- [ ] Run full test suite: `$env:JAVA_HOME="D:\AndroidStudio\jbr"; .\gradlew compileDebugKotlin testDebugUnitTest`
- [ ] Build debug APK: `$env:JAVA_HOME="D:\AndroidStudio\jbr"; .\gradlew :app:assembleDebug`
- [ ] Verify 0 compilation errors and 0 test regressions.
