package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
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
    fontSize = 28.sp,
    lineHeight = 34.sp
)

val LokapHeadlineStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp
)

val LokapTitleLargeStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    lineHeight = 23.sp
)

val LokapTitleMediumStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 21.sp
)

val LokapTitleSmallStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.5.sp,
    lineHeight = 18.sp
)

val LokapTitleStyle = LokapTitleMediumStyle

val LokapBodyLargeStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.5.sp,
    lineHeight = 20.sp
)

val LokapBodyMediumStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    lineHeight = 19.sp
)

val LokapBodySmallStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

val LokapBodyStyle = LokapBodyMediumStyle

val LokapLabelLargeStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 17.sp
)

val LokapLabelMediumStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 15.sp
)

val LokapLabelSmallStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.5.sp,
    lineHeight = 14.sp
)

val LokapLabelStyle = LokapLabelMediumStyle

val LokapTypography = Typography(
    displayLarge = LokapDisplayStyle,
    displayMedium = LokapDisplayStyle.copy(fontSize = 24.sp, lineHeight = 30.sp),
    displaySmall = LokapDisplayStyle.copy(fontSize = 22.sp, lineHeight = 28.sp),
    headlineLarge = LokapHeadlineStyle,
    headlineMedium = LokapHeadlineStyle.copy(fontSize = 19.sp, lineHeight = 25.sp),
    headlineSmall = LokapHeadlineStyle.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge = LokapTitleLargeStyle,
    titleMedium = LokapTitleMediumStyle,
    titleSmall = LokapTitleSmallStyle,
    bodyLarge = LokapBodyLargeStyle,
    bodyMedium = LokapBodyMediumStyle,
    bodySmall = LokapBodySmallStyle,
    labelLarge = LokapLabelLargeStyle,
    labelMedium = LokapLabelMediumStyle,
    labelSmall = LokapLabelSmallStyle
)
