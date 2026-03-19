package com.calpoly.fleecedlogin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.calpoly.fleecedlogin.R.array.com_google_android_gms_fonts_certs
)

private val tomorrowFont = GoogleFont("Tomorrow")

val TomorrowFontFamily = FontFamily(
    Font(googleFont = tomorrowFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = tomorrowFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = tomorrowFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = tomorrowFont, fontProvider = provider, weight = FontWeight.Bold),
)

// Keep alias so any screens still referencing PixelFontFamily compile
val PixelFontFamily = TomorrowFontFamily

// Display font for the Polls screen — Barlow Condensed ExtraBold Italic
private val barlowCondensedFont = GoogleFont("Barlow Condensed")

val BarlowCondensedFamily = FontFamily(
    Font(googleFont = barlowCondensedFont, fontProvider = provider, weight = FontWeight.ExtraBold, style = FontStyle.Italic),
    Font(googleFont = barlowCondensedFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = barlowCondensedFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = barlowCondensedFont, fontProvider = provider, weight = FontWeight.Normal),
)

val FleecedTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
