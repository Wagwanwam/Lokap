```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Color.kt
package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val BackgroundToken = Color(0xFF0B0B0D)
val SurfaceToken = Color(0xFF141417)
val SurfaceHighToken = Color(0xFF1E1E23)
val OutlineToken = Color(0xFF2C2C33)
val PrimaryToken = Color(0xFFFF4D8D)
val PrimarySoftToken = Color(0xFFFF8FB8)
val OnPrimaryToken = Color(0xFF0B0B0D)
val TextPrimaryToken = Color(0xFFF5F5F7)
val TextSecondaryToken = Color(0xFFA1A1AA)
val TextTertiaryToken = Color(0xFF71717A)
val AccentLilacToken = Color(0xFFB98CFF)
val AccentLimeToken = Color(0xFFC6FF4D)
val AccentCyanToken = Color(0xFF4DE1FF)
val AccentYellowToken = Color(0xFFFFD84D)
val ErrorToken = Color(0xFFFF6B6B)
val SuccessToken = Color(0xFF4ADE80)

@Immutable
data class LokapColors(
    val background: Color = BackgroundToken,
    val surface: Color = SurfaceToken,
    val surfaceHigh: Color = SurfaceHighToken,
    val outline: Color = OutlineToken,
    val primary: Color = PrimaryToken,
    val primarySoft: Color = PrimarySoftToken,
    val onPrimary: Color = OnPrimaryToken,
    val textPrimary: Color = TextPrimaryToken,
    val textSecondary: Color = TextSecondaryToken,
    val textTertiary: Color = TextTertiaryToken,
    val accentLilac: Color = AccentLilacToken,
    val accentLime: Color = AccentLimeToken,
    val accentCyan: Color = AccentCyanToken,
    val accentYellow: Color = AccentYellowToken,
    val error: Color = ErrorToken,
    val success: Color = SuccessToken
)

val LocalLokapColors = staticCompositionLocalOf { LokapColors() }
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Type.kt
package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import vn.nhom2.lokap.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val beVietnamProFont = GoogleFont("Be Vietnam Pro")

val BeVietnamProFamily = FontFamily(
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

val LokapDisplayStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 32.sp,
    lineHeight = 40.sp
)

val LokapTitleStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp
)

val LokapBodyStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
)

val LokapLabelStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

val LokapTypography = Typography(
    displayLarge = LokapDisplayStyle,
    displayMedium = LokapDisplayStyle,
    displaySmall = LokapDisplayStyle,
    headlineLarge = LokapDisplayStyle,
    headlineMedium = LokapTitleStyle,
    headlineSmall = LokapTitleStyle,
    titleLarge = LokapTitleStyle,
    titleMedium = LokapTitleStyle,
    titleSmall = LokapTitleStyle,
    bodyLarge = LokapBodyStyle,
    bodyMedium = LokapBodyStyle,
    bodySmall = LokapBodyStyle,
    labelLarge = LokapLabelStyle,
    labelMedium = LokapLabelStyle,
    labelSmall = LokapLabelStyle
)
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Shape.kt
package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class LokapShapes(
    val cameraFrame: Shape = RoundedCornerShape(40.dp),
    val card: Shape = RoundedCornerShape(28.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val pill: Shape = CircleShape
)

val LocalLokapShapes = staticCompositionLocalOf { LokapShapes() }

val MaterialLokapShapes = Shapes(
    extraSmall = CircleShape,
    small = CircleShape,
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    extraLarge = RoundedCornerShape(40.dp)
)
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Spacing.kt
package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Gradient.kt
package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val RegionGradientPairs: List<Pair<Color, Color>> = listOf(
    PrimaryToken to AccentLilacToken,
    PrimaryToken to AccentYellowToken,
    PrimarySoftToken to AccentCyanToken,
    AccentLilacToken to AccentCyanToken,
    AccentCyanToken to AccentLimeToken,
    AccentLimeToken to AccentYellowToken,
    PrimaryToken to PrimarySoftToken,
    AccentLilacToken to PrimarySoftToken
)

/**
 * Trả về gradient 2 màu cố định theo mã tỉnh (cùng mã luôn cùng màu).
 * Dùng thuật toán băm FNV-1a 32-bit ổn định, không dùng Random.
 */
fun regionGradient(provinceCode: String): Brush {
    val normalized = provinceCode.trim().lowercase()
    var hash = 0x811c9dc5.toInt()
    for (ch in normalized) {
        hash = hash xor ch.code
        hash *= 0x01000193
    }
    val index = (hash and Int.MAX_VALUE) % RegionGradientPairs.size
    val (startColor, endColor) = RegionGradientPairs[index]
    return Brush.linearGradient(
        colors = listOf(startColor, endColor),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/theme/Theme.kt
package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val LokapDarkColorScheme = darkColorScheme(
    primary = PrimaryToken,
    onPrimary = OnPrimaryToken,
    primaryContainer = PrimarySoftToken,
    onPrimaryContainer = OnPrimaryToken,
    secondary = AccentLilacToken,
    onSecondary = OnPrimaryToken,
    tertiary = AccentCyanToken,
    onTertiary = OnPrimaryToken,
    background = BackgroundToken,
    onBackground = TextPrimaryToken,
    surface = SurfaceToken,
    onSurface = TextPrimaryToken,
    surfaceVariant = SurfaceHighToken,
    onSurfaceVariant = TextSecondaryToken,
    outline = OutlineToken,
    error = ErrorToken,
    onError = OnPrimaryToken
)

object LokapTheme {
    val colors: LokapColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLokapColors.current

    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val shapes: LokapShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalLokapShapes.current
}

@Composable
fun LokapTheme(
    content: @Composable () -> Unit
) {
    val lokapColors = LokapColors()
    val spacing = Spacing()
    val lokapShapes = LokapShapes()

    CompositionLocalProvider(
        LocalLokapColors provides lokapColors,
        LocalSpacing provides spacing,
        LocalLokapShapes provides lokapShapes
    ) {
        MaterialTheme(
            colorScheme = LokapDarkColorScheme,
            typography = LokapTypography,
            shapes = MaterialLokapShapes,
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "DesignSystemPreview", showBackground = true, backgroundColor = 0xFF0B0B0D, widthDp = 420, heightDp = 920)
@Composable
fun DesignSystemPreview() {
    LokapTheme {
        val c = LokapTheme.colors
        val s = LokapTheme.spacing

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.background)
                .padding(s.lg),
            verticalArrangement = Arrangement.spacedBy(s.lg)
        ) {
            Text(
                text = "LOKAP DESIGN SYSTEM",
                style = MaterialTheme.typography.labelLarge,
                color = c.primary
            )

            // 1. Bảng màu (Color Tokens)
            val swatches = listOf(
                "background" to c.background,
                "surface" to c.surface,
                "surfaceHigh" to c.surfaceHigh,
                "outline" to c.outline,
                "primary" to c.primary,
                "primarySoft" to c.primarySoft,
                "onPrimary" to c.onPrimary,
                "textPrimary" to c.textPrimary,
                "textSecondary" to c.textSecondary,
                "textTertiary" to c.textTertiary,
                "accentLilac" to c.accentLilac,
                "accentLime" to c.accentLime,
                "accentCyan" to c.accentCyan,
                "accentYellow" to c.accentYellow,
                "error" to c.error,
                "success" to c.success
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(s.sm),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                swatches.forEach { (name, color) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(LokapTheme.shapes.pill)
                                .background(color)
                                .border(1.dp, c.outline, LokapTheme.shapes.pill)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textSecondary
                        )
                    }
                }
            }

            // Nút mẫu kiểm tra onPrimary chữ ĐEN trên nền hồng
            Box(
                modifier = Modifier
                    .clip(LokapTheme.shapes.pill)
                    .background(c.primary)
                    .padding(horizontal = s.lg, vertical = s.sm)
            ) {
                Text(
                    text = "Nút Primary (chữ đen #0B0B0D)",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onPrimary
                )
            }

            // 2. Thang chữ (Typography Scale)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LokapTheme.shapes.card)
                    .background(c.surface)
                    .padding(s.lg),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                Text(
                    text = "Chụp khoảnh khắc ở Hội An",
                    style = MaterialTheme.typography.displayLarge,
                    color = c.textPrimary
                )
                Text(
                    text = "Title 20/28 — Bản đồ khám phá 34 tỉnh thành",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary
                )
                Text(
                    text = "Body 15/22 — Chia sẻ ảnh tức thì cùng hội bạn thân hoặc ghim lên bản đồ công khai theo từng phường, xã.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary
                )
                Text(
                    text = "Label 12/16 — chụp 3 ngày trước · Phường Hội An, Đà Nẵng",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textTertiary
                )
            }

            // 3. Ba ô Gradient cố định theo mã tỉnh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.md)
            ) {
                listOf("da-nang" to "Đà Nẵng", "ha-noi" to "Hà Nội", "hue" to "Huế").forEach { (code, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(LokapTheme.shapes.card)
                            .background(regionGradient(code))
                            .padding(s.md),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.onPrimary
                        )
                    }
                }
            }
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/Audience.kt
package vn.nhom2.lokap.domain.model

enum class Audience {
    FRIENDS,
    DIRECT,
    PUBLIC
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/LocationLevel.kt
package vn.nhom2.lokap.domain.model

enum class LocationLevel(val labelVi: String) {
    PLACE("Địa điểm cụ thể"),
    WARD("Phường-xã"),
    PROVINCE("Tỉnh-thành"),
    COUNTRY("Chỉ \"Việt Nam\"")
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/PublishDelay.kt
package vn.nhom2.lokap.domain.model

enum class PublishDelay(val days: Int) {
    NOW(0),
    ONE_DAY(1),
    THREE_DAYS(3),
    SEVEN_DAYS(7)
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/Region.kt
package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable

enum class RegionLevel {
    PROVINCE,
    WARD
}

@Immutable
data class Province(
    val code: String,
    val name: String,
    val isCity: Boolean,
    val lat: Double,
    val lng: Double
)

@Immutable
data class Ward(
    val code: String,
    val provinceCode: String,
    val name: String,
    val lat: Double,
    val lng: Double
)
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/User.kt
package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?
)

enum class FriendStatus {
    NONE,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    FRIENDS,
    BLOCKED
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/Post.kt
package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

/**
 * [lat] và [lng] là toạ độ ĐÃ LÀM MỜ để hiển thị an toàn trên UI.
 */
@Immutable
data class PostLocation(
    val level: LocationLevel,
    val provinceCode: String?,
    val provinceName: String?,
    val wardCode: String?,
    val wardName: String?,
    val placeName: String?,
    val landmarkId: String?,
    val lat: Double?,
    val lng: Double?
)

enum class PostSendState {
    PENDING,
    SENT,
    FAILED
}

@Immutable
data class Reaction(
    val emoji: String,
    val count: Int
)

@Immutable
data class Post(
    val id: String,
    val author: User,
    val imageUrl: String,
    val caption: String?,
    val audience: Audience,
    val location: PostLocation,
    val takenAt: Instant,
    val publicVisibleAt: Instant?,
    val reactions: List<Reaction>,
    val myReaction: String?,
    val sendState: PostSendState
)
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/MapModels.kt
package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable

enum class MapBubbleType {
    PROVINCE,
    WARD,
    PROVINCE_WIDE,
    PLACE,
    LANDMARK
}

@Immutable
data class MapBubble(
    val id: String,
    val type: MapBubbleType,
    val code: String?,
    val label: String,
    val lat: Double,
    val lng: Double,
    val postCount: Int,
    val coverImageUrl: String?,
    val hasFriendPosts: Boolean
)

@Immutable
data class Landmark(
    val id: String,
    val name: String,
    val provinceCode: String,
    val wardCode: String?,
    val lat: Double,
    val lng: Double,
    val category: String,
    val coverUrl: String,
    val coverAttribution: String,
    val description: String,
    val postCount: Int
)

@Immutable
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/AppNotification.kt
package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

enum class NotificationType {
    NEW_POST,
    REACTION,
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    NEW_MESSAGE,
    POST_HIDDEN
}

@Immutable
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actor: User?,
    val postId: String?,
    val text: String,
    val createdAt: Instant,
    val isRead: Boolean
)
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/domain/model/Messaging.kt
package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Conversation(
    val id: String,
    val friend: User,
    val lastMessagePreview: String,
    val lastMessageAt: Instant,
    val unreadCount: Int
)

@Immutable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String?,
    val replyToPost: Post?,
    val attachedPost: Post?,
    val createdAt: Instant,
    val sendState: PostSendState,
    val seen: Boolean
)
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/common/TimeAgo.kt
package vn.nhom2.lokap.core.common

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val VIET_NAM_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
private val SHORT_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM").withZone(VIET_NAM_ZONE)

/**
 * Định dạng khoảng thời gian sang tiếng Việt:
 * - Dưới 1 phút: "vừa xong"
 * - Dưới 60 phút: "X phút trước"
 * - Dưới 24 giờ: "X giờ trước"
 * - Từ 1 đến 7 ngày: "chụp X ngày trước" (hoặc "X ngày trước" nếu [isDelayedShot] = false)
 * - Quá 7 ngày: "dd/MM"
 */
fun formatTimeAgo(
    instant: Instant,
    now: Instant,
    isDelayedShot: Boolean = true
): String {
    val duration = Duration.between(instant, now)
    if (duration.isNegative || duration.seconds < 60) {
        return "vừa xong"
    }
    val minutes = duration.toMinutes()
    if (minutes < 60) {
        return "$minutes phút trước"
    }
    val hours = duration.toHours()
    if (hours < 24) {
        return "$hours giờ trước"
    }
    val days = duration.toDays()
    if (days <= 7) {
        return if (isDelayedShot) "chụp $days ngày trước" else "$days ngày trước"
    }
    return SHORT_DATE_FORMATTER.format(instant)
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/ProvinceCatalog.kt
// Dữ liệu tạm cho UI, sẽ thay bằng dữ liệu từ backend
package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.Province

object ProvinceCatalog {
    val allProvinces: List<Province> = listOf(
        // 6 Thành phố trực thuộc trung ương
        Province(code = "ha-noi", name = "Hà Nội", isCity = true, lat = 21.0285, lng = 105.8542),
        Province(code = "hue", name = "Huế", isCity = true, lat = 16.4637, lng = 107.5909),
        Province(code = "hai-phong", name = "Hải Phòng", isCity = true, lat = 20.8449, lng = 106.6881),
        Province(code = "da-nang", name = "Đà Nẵng", isCity = true, lat = 16.0544, lng = 108.2022),
        Province(code = "tp-ho-chi-minh", name = "Thành phố Hồ Chí Minh", isCity = true, lat = 10.7769, lng = 106.7009),
        Province(code = "can-tho", name = "Cần Thơ", isCity = true, lat = 10.0452, lng = 105.7469),

        // 28 Tỉnh
        Province(code = "lai-chau", name = "Lai Châu", isCity = false, lat = 22.3864, lng = 103.4703),
        Province(code = "dien-bien", name = "Điện Biên", isCity = false, lat = 21.3860, lng = 103.0230),
        Province(code = "son-la", name = "Sơn La", isCity = false, lat = 21.3256, lng = 103.9188),
        Province(code = "lang-son", name = "Lạng Sơn", isCity = false, lat = 21.8537, lng = 106.7615),
        Province(code = "quang-ninh", name = "Quảng Ninh", isCity = false, lat = 20.9599, lng = 107.0425),
        Province(code = "thanh-hoa", name = "Thanh Hóa", isCity = false, lat = 19.8067, lng = 105.7852),
        Province(code = "nghe-an", name = "Nghệ An", isCity = false, lat = 18.6796, lng = 105.6813),
        Province(code = "ha-tinh", name = "Hà Tĩnh", isCity = false, lat = 18.3559, lng = 105.8877),
        Province(code = "cao-bang", name = "Cao Bằng", isCity = false, lat = 22.6663, lng = 106.2522),
        Province(code = "tuyen-quang", name = "Tuyên Quang", isCity = false, lat = 21.8236, lng = 105.2142),
        Province(code = "lao-cai", name = "Lào Cai", isCity = false, lat = 22.4856, lng = 103.9707),
        Province(code = "thai-nguyen", name = "Thái Nguyên", isCity = false, lat = 21.5942, lng = 105.8482),
        Province(code = "phu-tho", name = "Phú Thọ", isCity = false, lat = 21.3227, lng = 105.4019),
        Province(code = "bac-ninh", name = "Bắc Ninh", isCity = false, lat = 21.1861, lng = 106.0763),
        Province(code = "hung-yen", name = "Hưng Yên", isCity = false, lat = 20.6464, lng = 106.0511),
        Province(code = "ninh-binh", name = "Ninh Bình", isCity = false, lat = 20.2506, lng = 105.9745),
        Province(code = "quang-tri", name = "Quảng Trị", isCity = false, lat = 16.8163, lng = 107.1003),
        Province(code = "quang-ngai", name = "Quảng Ngãi", isCity = false, lat = 15.1214, lng = 108.8044),
        Province(code = "gia-lai", name = "Gia Lai", isCity = false, lat = 13.9833, lng = 108.0000),
        Province(code = "khanh-hoa", name = "Khánh Hòa", isCity = false, lat = 12.2388, lng = 109.1967),
        Province(code = "lam-dong", name = "Lâm Đồng", isCity = false, lat = 11.9404, lng = 108.4583),
        Province(code = "dak-lak", name = "Đắk Lắk", isCity = false, lat = 12.6667, lng = 108.0500),
        Province(code = "dong-nai", name = "Đồng Nai", isCity = false, lat = 10.9574, lng = 106.8427),
        Province(code = "tay-ninh", name = "Tây Ninh", isCity = false, lat = 11.3352, lng = 106.1099),
        Province(code = "vinh-long", name = "Vĩnh Long", isCity = false, lat = 10.2537, lng = 105.9722),
        Province(code = "dong-thap", name = "Đồng Tháp", isCity = false, lat = 10.4598, lng = 105.6326),
        Province(code = "ca-mau", name = "Cà Mau", isCity = false, lat = 9.1768, lng = 105.1524),
        Province(code = "an-giang", name = "An Giang", isCity = false, lat = 10.3864, lng = 105.4352)
    )

    private val byCode: Map<String, Province> = allProvinces.associateBy { it.code }

    fun findByCode(code: String): Province? = byCode[code]
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/SampleData.kt
package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.AppNotification
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Conversation
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.MapBubble
import vn.nhom2.lokap.domain.model.MapBubbleType
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.NotificationType
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.Reaction
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.model.Ward
import java.time.Duration
import java.time.Instant

object SampleData {

    val NOW: Instant = Instant.parse("2026-11-05T08:00:00Z")

    private fun minutesAgo(minutes: Long): Instant = NOW.minus(Duration.ofMinutes(minutes))
    private fun hoursAgo(hours: Long): Instant = NOW.minus(Duration.ofHours(hours))
    private fun daysAgo(days: Long): Instant = NOW.minus(Duration.ofDays(days))
    private fun daysLater(days: Long): Instant = NOW.plus(Duration.ofDays(days))

    // 1. 6 User mẫu (user đầu tiên là "tôi")
    val currentUser = User(
        id = "u_me",
        username = "minhkhoi",
        displayName = "Nguyễn Minh Khôi",
        avatarUrl = "https://picsum.photos/seed/avatar_minhkhoi/300/300"
    )

    val users: List<User> = listOf(
        currentUser,
        User(
            id = "u_2",
            username = "khanhlinh",
            displayName = "Trần Khánh Linh",
            avatarUrl = "https://picsum.photos/seed/avatar_khanhlinh/300/300"
        ),
        User(
            id = "u_3",
            username = "hoangnam",
            displayName = "Lê Hoàng Nam",
            avatarUrl = "https://picsum.photos/seed/avatar_hoangnam/300/300"
        ),
        User(
            id = "u_4",
            username = "thaomy",
            displayName = "Phạm Thảo My",
            avatarUrl = "https://picsum.photos/seed/avatar_thaomy/300/300"
        ),
        User(
            id = "u_5",
            username = "ducanh",
            displayName = "Vũ Đức Anh",
            avatarUrl = "https://picsum.photos/seed/avatar_ducanh/300/300"
        ),
        User(
            id = "u_6",
            username = "baongoc",
            displayName = "Đỗ Bảo Ngọc",
            avatarUrl = "https://picsum.photos/seed/avatar_baongoc/300/300"
        )
    )

    // 2. Phường mẫu (Đà Nẵng sau sáp nhập có Hội An; Huế có Thuận Hóa, Phú Xuân)
    val wards: List<Ward> = listOf(
        Ward(code = "dn-hai-chau", provinceCode = "da-nang", name = "Phường Hải Châu", lat = 16.0678, lng = 108.2208),
        Ward(code = "dn-son-tra", provinceCode = "da-nang", name = "Phường Sơn Trà", lat = 16.0911, lng = 108.2483),
        Ward(code = "dn-ngu-hanh-son", provinceCode = "da-nang", name = "Phường Ngũ Hành Sơn", lat = 16.0025, lng = 108.2611),
        Ward(code = "dn-hoi-an", provinceCode = "da-nang", name = "Phường Hội An", lat = 15.8801, lng = 108.3380),
        Ward(code = "hue-thuan-hoa", provinceCode = "hue", name = "Phường Thuận Hóa", lat = 16.4619, lng = 107.5955),
        Ward(code = "hue-phu-xuan", provinceCode = "hue", name = "Phường Phú Xuân", lat = 16.4728, lng = 107.5776)
    )

    // 3. Địa điểm cụ thể (Places) mẫu
    val places: List<Place> = listOf(
        Place(id = "pl_1", name = "Trình Cà Phê", address = "25 Phạm Hồng Thái, Hải Châu, Đà Nẵng", lat = 16.0682, lng = 108.2215),
        Place(id = "pl_2", name = "Faifo Coffee", address = "130 Trần Phú, Hội An, Đà Nẵng", lat = 15.8772, lng = 108.3280),
        Place(id = "pl_3", name = "Cà Phê Muối 142", address = "142 Đặng Thái Thân, Phú Xuân, Huế", lat = 16.4712, lng = 107.5750)
    )

    // 4. 6 Địa danh (Landmarks) mẫu
    val landmarks: List<Landmark> = listOf(
        Landmark(
            id = "lm_cau_rong",
            name = "Cầu Rồng",
            provinceCode = "da-nang",
            wardCode = "dn-hai-chau",
            lat = 16.0611,
            lng = 108.2275,
            category = "Biểu tượng đô thị",
            coverUrl = "https://picsum.photos/seed/cau-rong/800/800",
            coverAttribution = "Ảnh minh hoạ",
            description = "Cây cầu phun lửa và phun nước cuối tuần bắc qua sông Hàn.",
            postCount = 128
        ),
        Landmark(
            id = "lm_son_tra",
            name = "Bán đảo Sơn Trà",
            provinceCode = "da-nang",
            wardCode = "dn-son-tra",
            lat = 16.1189,
            lng = 108.2736,
            category = "Thiên nhiên",
            coverUrl = "https://picsum.photos/seed/ban-dao-son-tra/800/800",
            coverAttribution = "Ảnh minh hoạ",
            description = "Cung đường biển ôm trọn rừng nguyên sinh và chùa Linh Ứng.",
            postCount = 94
        ),
        Landmark(
            id = "lm_ngu_hanh_son",
            name = "Ngũ Hành Sơn",
            provinceCode = "da-nang",
            wardCode = "dn-ngu-hanh-son",
            lat = 16.0039,
            lng = 108.2631,
            category = "Danh thắng",
            coverUrl = "https://picsum.photos/seed/ngu-hanh-son/800/800",
            coverAttribution = "Ảnh minh hoạ",
            description = "Quần thể 5 ngọn núi đá vôi cùng hệ thống hang động huyền ảo.",
            postCount = 67
        ),
        Landmark(
            id = "lm_hoi_an",
            name = "Phố cổ Hội An",
            provinceCode = "da-nang",
            wardCode = "dn-hoi-an",
            lat = 15.8794,
            lng = 108.3350,
            category = "Di sản thế giới",
            coverUrl = "https://picsum.photos/seed/pho-co-hoi-an/800/800",
            coverAttribution = "Ảnh minh hoạ",
            description = "Những dãy nhà tường vàng rêu phong và đèn lồng rực rỡ bên sông Hoài.",
            postCount = 215
        ),
        Landmark(
            id = "lm_dai_noi_hue",
            name = "Đại Nội Huế",
            provinceCode = "hue",
            wardCode = "hue-phu-xuan",
            lat = 16.4698,
            lng = 107.5784,
            category = "Di sản thế giới",
            coverUrl = "https://picsum.photos/seed/dai-noi-hue/800/800",
            coverAttribution = "Ảnh minh hoạ",
            description = "Hoàng thành cổ kính gắn liền với triều đại nhà Nguyễn.",
            postCount = 142
        ),
        Landmark(
            id = "lm_chua_thien_mu",
            name = "Chùa Thiên Mụ",
            provinceCode = "hue",
            wardCode = "hue-phu-xuan",
            lat = 16.4534,
            lng = 107.5449,
            category = "Tâm linh",
            coverUrl = "https://picsum.photos/seed/chua-thien-mu/800/800",
            coverAttribution = "Ảnh minh hoạ",
            description = "Ngôi chùa cổ bên dòng sông Hương với tháp Phước Duyên 7 tầng.",
            postCount = 88
        )
    )

    // 5. 20 Bài đăng mẫu (trải đủ 3 audience, 4 mức vị trí, 2 bài PENDING/FAILED, 2 bài public hẹn giờ)
    val posts: List<Post> = listOf(
        // Bài 1: PUBLIC - PLACE (Landmark Cầu Rồng)
        Post(
            id = "p_01",
            author = users[1],
            imageUrl = "https://picsum.photos/seed/lokap_post_01/800/800",
            caption = "Đợi Cầu Rồng phun lửa tối cuối tuần 🔥",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-hai-chau",
                wardName = "Phường Hải Châu",
                placeName = "Cầu Rồng",
                landmarkId = "lm_cau_rong",
                lat = 16.0615,
                lng = 108.2270
            ),
            takenAt = minutesAgo(5),
            publicVisibleAt = minutesAgo(5),
            reactions = listOf(Reaction("🔥", 14), Reaction("🩷", 8)),
            myReaction = "🔥",
            sendState = PostSendState.SENT
        ),
        // Bài 2: FRIENDS - PLACE (Quán cà phê)
        Post(
            id = "p_02",
            author = users[2],
            imageUrl = "https://picsum.photos/seed/lokap_post_02/800/800",
            caption = "Cà phê bơ sáng sớm tỉnh cả người ☕",
            audience = Audience.FRIENDS,
            location = PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-hai-chau",
                wardName = "Phường Hải Châu",
                placeName = "Trình Cà Phê",
                landmarkId = null,
                lat = 16.0680,
                lng = 108.2210
            ),
            takenAt = minutesAgo(18),
            publicVisibleAt = null,
            reactions = listOf(Reaction("☕", 5), Reaction("😍", 3)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 3: PUBLIC - PLACE (Phố cổ Hội An) - ĐÃ QUA HẸN GIỜ 3 NGÀY (chụp 3 ngày trước, vừa hiển thị public)
        Post(
            id = "p_03",
            author = users[3],
            imageUrl = "https://picsum.photos/seed/lokap_post_03/800/800",
            caption = "Chiều hoàng hôn trên mái ngói Hội An 🏮",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-hoi-an",
                wardName = "Phường Hội An",
                placeName = "Phố cổ Hội An",
                landmarkId = "lm_hoi_an",
                lat = 15.8790,
                lng = 108.3355
            ),
            takenAt = daysAgo(3),
            publicVisibleAt = minutesAgo(30),
            reactions = listOf(Reaction("🏮", 29), Reaction("✨", 11)),
            myReaction = "🏮",
            sendState = PostSendState.SENT
        ),
        // Bài 4: PUBLIC - ĐANG HẸN GIỜ #1 (chụp 2 giờ trước, hẹn 1 ngày sau mới lên bản đồ public)
        Post(
            id = "p_04",
            author = currentUser,
            imageUrl = "https://picsum.photos/seed/lokap_post_04/800/800",
            caption = "Góc bí mật trên bán đảo Sơn Trà, mai mới lên map nhé 🌿",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-son-tra",
                wardName = "Phường Sơn Trà",
                placeName = "Bán đảo Sơn Trà",
                landmarkId = "lm_son_tra",
                lat = 16.1180,
                lng = 108.2740
            ),
            takenAt = hoursAgo(2),
            publicVisibleAt = daysLater(1),
            reactions = listOf(Reaction("🌿", 4)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 5: PUBLIC - ĐANG HẸN GIỜ #2 (chụp 4 giờ trước, hẹn 3 ngày sau mới lên bản đồ public)
        Post(
            id = "p_05",
            author = currentUser,
            imageUrl = "https://picsum.photos/seed/lokap_post_05/800/800",
            caption = "Đại Nội Huế ngày nắng nhẹ, để dành 3 hôm nữa mở khoá 🌤️",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = "hue",
                provinceName = "Huế",
                wardCode = "hue-phu-xuan",
                wardName = "Phường Phú Xuân",
                placeName = "Đại Nội Huế",
                landmarkId = "lm_dai_noi_hue",
                lat = 16.4700,
                lng = 107.5780
            ),
            takenAt = hoursAgo(4),
            publicVisibleAt = daysLater(3),
            reactions = emptyList(),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 6: TRẠNG THÁI PENDING (của currentUser đang gửi dở)
        Post(
            id = "p_06",
            author = currentUser,
            imageUrl = "https://picsum.photos/seed/lokap_post_06/800/800",
            caption = "Đang trên đỉnh Ngũ Hành Sơn sóng yếu quá...",
            audience = Audience.FRIENDS,
            location = PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-ngu-hanh-son",
                wardName = "Phường Ngũ Hành Sơn",
                placeName = "Ngũ Hành Sơn",
                landmarkId = "lm_ngu_hanh_son",
                lat = 16.0042,
                lng = 108.2630
            ),
            takenAt = minutesAgo(1),
            publicVisibleAt = null,
            reactions = emptyList(),
            myReaction = null,
            sendState = PostSendState.PENDING
        ),
        // Bài 7: TRẠNG THÁI FAILED (của currentUser gửi lỗi cần bấm thử lại)
        Post(
            id = "p_07",
            author = currentUser,
            imageUrl = "https://picsum.photos/seed/lokap_post_07/800/800",
            caption = "Mất mạng lúc chụp chiều sông Hương 😭",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.WARD,
                provinceCode = "hue",
                provinceName = "Huế",
                wardCode = "hue-thuan-hoa",
                wardName = "Phường Thuận Hóa",
                placeName = null,
                landmarkId = null,
                lat = 16.4620,
                lng = 107.5950
            ),
            takenAt = minutesAgo(12),
            publicVisibleAt = null,
            reactions = emptyList(),
            myReaction = null,
            sendState = PostSendState.FAILED
        ),
        // Bài 8: DIRECT - Gửi riêng 1-1 cho bạn
        Post(
            id = "p_08",
            author = users[1],
            imageUrl = "https://picsum.photos/seed/lokap_post_08/800/800",
            caption = "Qua đón tui đi ăn ốc liền nha!",
            audience = Audience.DIRECT,
            location = PostLocation(
                level = LocationLevel.WARD,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-hai-chau",
                wardName = "Phường Hải Châu",
                placeName = null,
                landmarkId = null,
                lat = 16.0675,
                lng = 108.2205
            ),
            takenAt = minutesAgo(25),
            publicVisibleAt = null,
            reactions = listOf(Reaction("🫡", 1)),
            myReaction = "🫡",
            sendState = PostSendState.SENT
        ),
        // Bài 9: DIRECT - Gửi riêng 1-1 từ currentUser
        Post(
            id = "p_09",
            author = currentUser,
            imageUrl = "https://picsum.photos/seed/lokap_post_09/800/800",
            caption = "Đợi 5 phút đang dắt xe ra nè 🛵",
            audience = Audience.DIRECT,
            location = PostLocation(
                level = LocationLevel.WARD,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-son-tra",
                wardName = "Phường Sơn Trà",
                placeName = null,
                landmarkId = null,
                lat = 16.0910,
                lng = 108.2480
            ),
            takenAt = minutesAgo(20),
            publicVisibleAt = null,
            reactions = listOf(Reaction("🩷", 1)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 10: PUBLIC - Mức WARD (Phường Hội An)
        Post(
            id = "p_10",
            author = users[4],
            imageUrl = "https://picsum.photos/seed/lokap_post_10/800/800",
            caption = "Nước Mót mát lạnh giữa trưa nắng 🌿",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.WARD,
                provinceCode = "da-nang",
                provinceName = "Đà Nẵng",
                wardCode = "dn-hoi-an",
                wardName = "Phường Hội An",
                placeName = null,
                landmarkId = null,
                lat = 15.8805,
                lng = 108.3375
            ),
            takenAt = hoursAgo(3),
            publicVisibleAt = hoursAgo(3),
            reactions = listOf(Reaction("🥤", 9)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 11: PUBLIC - Mức PLACE (Chùa Thiên Mụ)
        Post(
            id = "p_11",
            author = users[5],
            imageUrl = "https://picsum.photos/seed/lokap_post_11/800/800",
            caption = "Tiếng chuông chùa Thiên Mụ bình yên cực kỳ",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = "hue",
                provinceName = "Huế",
                wardCode = "hue-phu-xuan",
                wardName = "Phường Phú Xuân",
                placeName = "Chùa Thiên Mụ",
                landmarkId = "lm_chua_thien_mu",
                lat = 16.4536,
                lng = 107.5451
            ),
            takenAt = hoursAgo(6),
            publicVisibleAt = hoursAgo(6),
            reactions = listOf(Reaction("🙏", 17), Reaction("✨", 6)),
            myReaction = "✨",
            sendState = PostSendState.SENT
        ),
        // Bài 12: FRIENDS - Mức PROVINCE (Hà Nội)
        Post(
            id = "p_12",
            author = users[2],
            imageUrl = "https://picsum.photos/seed/lokap_post_12/800/800",
            caption = "Mùa thu Hà Nội cốm non thơm lừng 🍂",
            audience = Audience.FRIENDS,
            location = PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = "ha-noi",
                provinceName = "Hà Nội",
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 21.0285,
                lng = 105.8542
            ),
            takenAt = hoursAgo(10),
            publicVisibleAt = null,
            reactions = listOf(Reaction("🍂", 12)),
            myReaction = "🍂",
            sendState = PostSendState.SENT
        ),
        // Bài 13: PUBLIC - Mức PROVINCE (TP. Hồ Chí Minh)
        Post(
            id = "p_13",
            author = users[3],
            imageUrl = "https://picsum.photos/seed/lokap_post_13/800/800",
            caption = "Sài Gòn đêm không ngủ 🌃",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = "tp-ho-chi-minh",
                provinceName = "Thành phố Hồ Chí Minh",
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 10.7769,
                lng = 106.7009
            ),
            takenAt = hoursAgo(15),
            publicVisibleAt = hoursAgo(15),
            reactions = listOf(Reaction("🔥", 21), Reaction("🌃", 9)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 14: PUBLIC - Mức PROVINCE (Lâm Đồng)
        Post(
            id = "p_14",
            author = users[4],
            imageUrl = "https://picsum.photos/seed/lokap_post_14/800/800",
            caption = "Săn mây 5 giờ sáng lạnh tê tái ☁️",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = "lam-dong",
                provinceName = "Lâm Đồng",
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 11.9404,
                lng = 108.4583
            ),
            takenAt = daysAgo(1),
            publicVisibleAt = daysAgo(1),
            reactions = listOf(Reaction("☁️", 33)),
            myReaction = "☁️",
            sendState = PostSendState.SENT
        ),
        // Bài 15: FRIENDS - Mức COUNTRY (Chỉ "Việt Nam")
        Post(
            id = "p_15",
            author = users[5],
            imageUrl = "https://picsum.photos/seed/lokap_post_15/800/800",
            caption = "Đố mọi người đoán được mình đang ở tỉnh nào 🤭",
            audience = Audience.FRIENDS,
            location = PostLocation(
                level = LocationLevel.COUNTRY,
                provinceCode = null,
                provinceName = null,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = null,
                lng = null
            ),
            takenAt = daysAgo(2),
            publicVisibleAt = null,
            reactions = listOf(Reaction("🤔", 7)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 16: PUBLIC - Mức COUNTRY (Chỉ "Việt Nam")
        Post(
            id = "p_16",
            author = users[1],
            imageUrl = "https://picsum.photos/seed/lokap_post_16/800/800",
            caption = "Bầu trời chiều nay cực phẩm 🌅",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.COUNTRY,
                provinceCode = null,
                provinceName = null,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = null,
                lng = null
            ),
            takenAt = daysAgo(4),
            publicVisibleAt = daysAgo(4),
            reactions = listOf(Reaction("🌅", 19)),
            myReaction = "🌅",
            sendState = PostSendState.SENT
        ),
        // Bài 17: PUBLIC - Mức PROVINCE (Khánh Hòa)
        Post(
            id = "p_17",
            author = users[2],
            imageUrl = "https://picsum.photos/seed/lokap_post_17/800/800",
            caption = "Biển trong vắt nhìn thấu đáy 🌊",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = "khanh-hoa",
                provinceName = "Khánh Hòa",
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 12.2388,
                lng = 109.1967
            ),
            takenAt = daysAgo(5),
            publicVisibleAt = daysAgo(5),
            reactions = listOf(Reaction("🌊", 25)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 18: PUBLIC - Mức PROVINCE (Lào Cai)
        Post(
            id = "p_18",
            author = users[3],
            imageUrl = "https://picsum.photos/seed/lokap_post_18/800/800",
            caption = "Ruộng bậc thang mùa lúa chín vàng rực 🌾",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = "lao-cai",
                provinceName = "Lào Cai",
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 22.4856,
                lng = 103.9707
            ),
            takenAt = daysAgo(6),
            publicVisibleAt = daysAgo(6),
            reactions = listOf(Reaction("🌾", 41)),
            myReaction = "🌾",
            sendState = PostSendState.SENT
        ),
        // Bài 19: FRIENDS - Mức WARD (Phường Thuận Hóa, Huế) - Quá 7 ngày để test dd/MM
        Post(
            id = "p_19",
            author = users[4],
            imageUrl = "https://picsum.photos/seed/lokap_post_19/800/800",
            caption = "Bún bò Huế cay xè lưỡi 🍜",
            audience = Audience.FRIENDS,
            location = PostLocation(
                level = LocationLevel.WARD,
                provinceCode = "hue",
                provinceName = "Huế",
                wardCode = "hue-thuan-hoa",
                wardName = "Phường Thuận Hóa",
                placeName = null,
                landmarkId = null,
                lat = 16.4619,
                lng = 107.5955
            ),
            takenAt = daysAgo(10),
            publicVisibleAt = null,
            reactions = listOf(Reaction("🍜", 15)),
            myReaction = null,
            sendState = PostSendState.SENT
        ),
        // Bài 20: PUBLIC - Mức PROVINCE (Cần Thơ) - Quá 7 ngày
        Post(
            id = "p_20",
            author = users[5],
            imageUrl = "https://picsum.photos/seed/lokap_post_20/800/800",
            caption = "Chợ nổi Cái Răng tấp nập từ tinh mơ 🛶",
            audience = Audience.PUBLIC,
            location = PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = "can-tho",
                provinceName = "Cần Thơ",
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 10.0452,
                lng = 105.7469
            ),
            takenAt = daysAgo(14),
            publicVisibleAt = daysAgo(14),
            reactions = listOf(Reaction("🛶", 31)),
            myReaction = "🛶",
            sendState = PostSendState.SENT
        )
    )

    // 6. Map Bubbles mẫu cho 3 mức zoom
    // Zoom Mức 1 (Toàn quốc — gom theo Tỉnh/Thành phố)
    val provinceMapBubbles: List<MapBubble> = listOf(
        MapBubble(
            id = "mb_prov_danang",
            type = MapBubbleType.PROVINCE,
            code = "da-nang",
            label = "Đà Nẵng",
            lat = 16.0544,
            lng = 108.2022,
            postCount = 512,
            coverImageUrl = "https://picsum.photos/seed/cau-rong/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_prov_hue",
            type = MapBubbleType.PROVINCE,
            code = "hue",
            label = "Huế",
            lat = 16.4637,
            lng = 107.5909,
            postCount = 248,
            coverImageUrl = "https://picsum.photos/seed/dai-noi-hue/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_prov_hanoi",
            type = MapBubbleType.PROVINCE,
            code = "ha-noi",
            label = "Hà Nội",
            lat = 21.0285,
            lng = 105.8542,
            postCount = 890,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_12/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_prov_hcm",
            type = MapBubbleType.PROVINCE,
            code = "tp-ho-chi-minh",
            label = "TP. Hồ Chí Minh",
            lat = 10.7769,
            lng = 106.7009,
            postCount = 1024,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_13/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_prov_lamdong",
            type = MapBubbleType.PROVINCE,
            code = "lam-dong",
            label = "Lâm Đồng",
            lat = 11.9404,
            lng = 108.4583,
            postCount = 310,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_14/800/800",
            hasFriendPosts = false
        ),
        MapBubble(
            id = "mb_prov_cantho",
            type = MapBubbleType.PROVINCE,
            code = "can-tho",
            label = "Cần Thơ",
            lat = 10.0452,
            lng = 105.7469,
            postCount = 175,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_20/800/800",
            hasFriendPosts = false
        )
    )

    // Zoom Mức 2 (Trong một Tỉnh/Thành — gom theo Phường/Xã + 1 cụm Toàn tỉnh)
    val wardMapBubblesDaNang: List<MapBubble> = listOf(
        MapBubble(
            id = "mb_ward_haichau",
            type = MapBubbleType.WARD,
            code = "dn-hai-chau",
            label = "P. Hải Châu",
            lat = 16.0678,
            lng = 108.2208,
            postCount = 180,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_01/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_ward_sontra",
            type = MapBubbleType.WARD,
            code = "dn-son-tra",
            label = "P. Sơn Trà",
            lat = 16.0911,
            lng = 108.2483,
            postCount = 96,
            coverImageUrl = "https://picsum.photos/seed/ban-dao-son-tra/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_ward_nguhanhson",
            type = MapBubbleType.WARD,
            code = "dn-ngu-hanh-son",
            label = "P. Ngũ Hành Sơn",
            lat = 16.0025,
            lng = 108.2611,
            postCount = 67,
            coverImageUrl = "https://picsum.photos/seed/ngu-hanh-son/800/800",
            hasFriendPosts = false
        ),
        MapBubble(
            id = "mb_ward_hoian",
            type = MapBubbleType.WARD,
            code = "dn-hoi-an",
            label = "P. Hội An",
            lat = 15.8801,
            lng = 108.3380,
            postCount = 145,
            coverImageUrl = "https://picsum.photos/seed/pho-co-hoi-an/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_wide_danang",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "da-nang",
            label = "Chung toàn Đà Nẵng",
            lat = 16.0400,
            lng = 108.1800,
            postCount = 24,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_02/800/800",
            hasFriendPosts = false
        )
    )

    // Zoom Mức 3 (Chi tiết trong Phường — Địa danh Landmark & Địa điểm Place)
    val detailMapBubblesDaNang: List<MapBubble> = listOf(
        MapBubble(
            id = "mb_lm_caurong",
            type = MapBubbleType.LANDMARK,
            code = "lm_cau_rong",
            label = "Cầu Rồng",
            lat = 16.0611,
            lng = 108.2275,
            postCount = 128,
            coverImageUrl = "https://picsum.photos/seed/cau-rong/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_lm_hoian",
            type = MapBubbleType.LANDMARK,
            code = "lm_hoi_an",
            label = "Phố cổ Hội An",
            lat = 15.8794,
            lng = 108.3350,
            postCount = 215,
            coverImageUrl = "https://picsum.photos/seed/pho-co-hoi-an/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_pl_trinh",
            type = MapBubbleType.PLACE,
            code = "pl_1",
            label = "Trình Cà Phê",
            lat = 16.0682,
            lng = 108.2215,
            postCount = 19,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_02/800/800",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "mb_pl_faifo",
            type = MapBubbleType.PLACE,
            code = "pl_2",
            label = "Faifo Coffee",
            lat = 15.8772,
            lng = 108.3280,
            postCount = 34,
            coverImageUrl = "https://picsum.photos/seed/lokap_post_03/800/800",
            hasFriendPosts = false
        )
    )

    // 7. 5 Cuộc trò chuyện (Conversations) mẫu
    val conversations: List<Conversation> = listOf(
        Conversation(
            id = "conv_1",
            friend = users[1],
            lastMessagePreview = "Đợi 5 phút đang dắt xe ra nè 🛵",
            lastMessageAt = minutesAgo(20),
            unreadCount = 2
        ),
        Conversation(
            id = "conv_2",
            friend = users[2],
            lastMessagePreview = "Quán Trình Cà Phê đông không ông?",
            lastMessageAt = minutesAgo(14),
            unreadCount = 0
        ),
        Conversation(
            id = "conv_3",
            friend = users[3],
            lastMessagePreview = "Ảnh Hội An chỉnh màu đỉnh vậy!",
            lastMessageAt = minutesAgo(45),
            unreadCount = 1
        ),
        Conversation(
            id = "conv_4",
            friend = users[4],
            lastMessagePreview = "Cuối tuần làm chuyến đèo Hải Vân đi",
            lastMessageAt = hoursAgo(5),
            unreadCount = 0
        ),
        Conversation(
            id = "conv_5",
            friend = users[5],
            lastMessagePreview = "Đã gửi một ảnh cho bạn",
            lastMessageAt = daysAgo(1),
            unreadCount = 0
        )
    )

    // 8. 12 Tin nhắn mẫu (có tin trả lời từ ảnh replyToPost & tin kèm ảnh gửi riêng attachedPost)
    val messages: List<Message> = listOf(
        // Cuộc trò chuyện 1 (với Trần Khánh Linh - users[1])
        Message(
            id = "msg_01",
            conversationId = "conv_1",
            senderId = users[1].id,
            body = "Tối nay Cầu Rồng đông lắm luôn á!",
            replyToPost = posts[0], // Trả lời từ bài chụp Cầu Rồng
            attachedPost = null,
            createdAt = minutesAgo(35),
            sendState = PostSendState.SENT,
            seen = true
        ),
        Message(
            id = "msg_02",
            conversationId = "conv_1",
            senderId = currentUser.id,
            body = "Góc chụp đẹp ghê, đứng bên đường Trần Hưng Đạo hả?",
            replyToPost = null,
            attachedPost = null,
            createdAt = minutesAgo(32),
            sendState = PostSendState.SENT,
            seen = true
        ),
        Message(
            id = "msg_03",
            conversationId = "conv_1",
            senderId = users[1].id,
            body = "Đúng rồi, đứng ngay đầu cầu luôn.",
            replyToPost = null,
            attachedPost = null,
            createdAt = minutesAgo(28),
            sendState = PostSendState.SENT,
            seen = true
        ),
        Message(
            id = "msg_04",
            conversationId = "conv_1",
            senderId = users[1].id,
            body = "Qua đón tui đi ăn ốc liền nha!",
            replyToPost = null,
            attachedPost = posts[7], // Tin nhắn kèm ảnh gửi riêng DIRECT (p_08)
            createdAt = minutesAgo(25),
            sendState = PostSendState.SENT,
            seen = false
        ),
        Message(
            id = "msg_05",
            conversationId = "conv_1",
            senderId = currentUser.id,
            body = "Đợi 5 phút đang dắt xe ra nè 🛵",
            replyToPost = null,
            attachedPost = posts[8], // Tin nhắn kèm ảnh gửi riêng DIRECT (p_09)
            createdAt = minutesAgo(20),
            sendState = PostSendState.SENT,
            seen = false
        ),

        // Cuộc trò chuyện 2 (với Lê Hoàng Nam - users[2])
        Message(
            id = "msg_06",
            conversationId = "conv_2",
            senderId = currentUser.id,
            body = "Quán Trình Cà Phê đông không ông?",
            replyToPost = posts[1], // Trả lời từ bài cà phê bơ
            attachedPost = null,
            createdAt = minutesAgo(16),
            sendState = PostSendState.SENT,
            seen = true
        ),
        Message(
            id = "msg_07",
            conversationId = "conv_2",
            senderId = users[2].id,
            body = "Còn bàn ngoài sân thoáng lắm, chạy qua đi!",
            replyToPost = null,
            attachedPost = null,
            createdAt = minutesAgo(14),
            sendState = PostSendState.SENT,
            seen = true
        ),

        // Cuộc trò chuyện 3 (với Phạm Thảo My - users[3])
        Message(
            id = "msg_08",
            conversationId = "conv_3",
            senderId = currentUser.id,
            body = "Ảnh Hội An chỉnh màu đỉnh vậy!",
            replyToPost = posts[2], // Trả lời từ bài Hội An
            attachedPost = null,
            createdAt = minutesAgo(50),
            sendState = PostSendState.SENT,
            seen = true
        ),
        Message(
            id = "msg_09",
            conversationId = "conv_3",
            senderId = users[3].id,
            body = "Cam gốc của Lokap luôn đó không chỉnh gì đâu 🏮",
            replyToPost = null,
            attachedPost = null,
            createdAt = minutesAgo(45),
            sendState = PostSendState.SENT,
            seen = false
        ),

        // Cuộc trò chuyện 4 (với Vũ Đức Anh - users[4])
        Message(
            id = "msg_10",
            conversationId = "conv_4",
            senderId = users[4].id,
            body = "Cuối tuần làm chuyến đèo Hải Vân đi",
            replyToPost = null,
            attachedPost = null,
            createdAt = hoursAgo(5),
            sendState = PostSendState.SENT,
            seen = true
        ),
        Message(
            id = "msg_11",
            conversationId = "conv_4",
            senderId = currentUser.id,
            body = "Chốt luôn, 6h sáng thứ Bảy tập trung nhé!",
            replyToPost = null,
            attachedPost = null,
            createdAt = hoursAgo(4),
            sendState = PostSendState.SENT,
            seen = true
        ),

        // Cuộc trò chuyện 5 (với Đỗ Bảo Ngọc - users[5])
        Message(
            id = "msg_12",
            conversationId = "conv_5",
            senderId = users[5].id,
            body = "Nhìn chùa Thiên Mụ mê chưa nè",
            replyToPost = posts[10],
            attachedPost = null,
            createdAt = daysAgo(1),
            sendState = PostSendState.SENT,
            seen = true
        )
    )

    // 9. 8 Thông báo (Notifications) mẫu
    val notifications: List<AppNotification> = listOf(
        AppNotification(
            id = "notif_1",
            type = NotificationType.NEW_POST,
            actor = users[1],
            postId = "p_01",
            text = "Trần Khánh Linh vừa đăng khoảnh khắc mới tại Cầu Rồng.",
            createdAt = minutesAgo(5),
            isRead = false
        ),
        AppNotification(
            id = "notif_2",
            type = NotificationType.REACTION,
            actor = users[2],
            postId = "p_04",
            text = "Lê Hoàng Nam đã thả 🌿 vào ảnh của bạn ở Bán đảo Sơn Trà.",
            createdAt = minutesAgo(15),
            isRead = false
        ),
        AppNotification(
            id = "notif_3",
            type = NotificationType.NEW_MESSAGE,
            actor = users[1],
            postId = "p_08",
            text = "Trần Khánh Linh đã gửi riêng một ảnh cho bạn.",
            createdAt = minutesAgo(25),
            isRead = false
        ),
        AppNotification(
            id = "notif_4",
            type = NotificationType.FRIEND_REQUEST,
            actor = users[5],
            postId = null,
            text = "Đỗ Bảo Ngọc đã gửi lời mời kết bạn.",
            createdAt = hoursAgo(2),
            isRead = false
        ),
        AppNotification(
            id = "notif_5",
            type = NotificationType.FRIEND_ACCEPTED,
            actor = users[4],
            postId = null,
            text = "Vũ Đức Anh đã chấp nhận lời mời kết bạn của bạn.",
            createdAt = hoursAgo(6),
            isRead = true
        ),
        AppNotification(
            id = "notif_6",
            type = NotificationType.NEW_POST,
            actor = users[3],
            postId = "p_03",
            text = "Phạm Thảo My vừa mở khoảnh khắc hẹn giờ tại Phố cổ Hội An.",
            createdAt = hoursAgo(8),
            isRead = true
        ),
        AppNotification(
            id = "notif_7",
            type = NotificationType.REACTION,
            actor = users[3],
            postId = "p_09",
            text = "Phạm Thảo My đã thả 🩷 vào ảnh của bạn.",
            createdAt = daysAgo(1),
            isRead = true
        ),
        AppNotification(
            id = "notif_8",
            type = NotificationType.POST_HIDDEN,
            actor = null,
            postId = null,
            text = "Một ảnh công khai của bạn đã được ẩn do vi phạm nguyên tắc cộng đồng.",
            createdAt = daysAgo(2),
            isRead = true
        )
    )
}
```

```kotlin
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    debugImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-text-google-fonts")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```