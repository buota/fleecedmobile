package com.calpoly.fleecedlogin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Web app palette ────────────────────────────────────────────────────────
val RetroDark    = Color(0xFF1A1B26)   // retro-dark  – main background
val Dark         = Color(0xFF2B2B2B)   // dark        – surface
val RetroPurple  = Color(0xFF7AA2F7)   // retro-purple – primary accent
val RetroYellow  = Color(0xFFE0AF68)   // retro-yellow – gold / ranks
val RetroOrange  = Color(0xFFFF9E64)   // retro-orange – tertiary
val Sage         = Color(0xFF8FA89E)   // sage         – muted text
val LightGreen   = Color(0xFFB8E8B8)   // light-green
val LightPurple  = Color(0xFFC5B8E8)   // light-purple
val LightBlueWeb = Color(0xFFB8D8E8)   // light-blue
val LightCyan    = Color(0xFFB8E8D8)   // light-cyan
val VoteGreen    = Color(0xFF90EE90)   // vote-green
val VoteRed      = Color(0xFFFF6B6B)   // vote-red
val VoteBlue     = Color(0xFF4A9EFF)   // vote-blue

// ── Convenience aliases kept for backward compat ───────────────────────────
val Gold         = RetroYellow
val RetroRed     = VoteRed
val Silver       = Sage
val Bronze       = Color(0xFFCD7F32)
val Teal         = LightCyan          // previously mapped to LightBlue

val SkyBlue      = RetroPurple        // alias for screens that import SkyBlue

// Kept so screens that import the old navy names still compile
val Navy         = Color(0xFF1B2A4A)
val NavyLight    = Color(0xFF243B6A)
val NavyDeep     = Color(0xFF0F1B33)
val LightBlue    = RetroPurple        // primary is now retro-purple
val LightBlueAccent = LightPurple

// Dark surface layers
val DarkBg             = RetroDark
val DarkSurface        = Color(0xFF1E2030)
val DarkSurfaceVariant = Color(0xFF24283B)

// ── Color scheme ───────────────────────────────────────────────────────────
private val FleecedColorScheme = darkColorScheme(
    primary             = RetroPurple,
    onPrimary           = RetroDark,
    primaryContainer    = Color(0xFF2A3050),
    onPrimaryContainer  = LightPurple,
    secondary           = LightCyan,
    onSecondary         = RetroDark,
    secondaryContainer  = Color(0xFF1E2A30),
    onSecondaryContainer = LightCyan,
    tertiary            = RetroOrange,
    onTertiary          = Color(0xFF2A1A00),
    tertiaryContainer   = Color(0xFF3A2800),
    onTertiaryContainer = RetroYellow,
    error               = VoteRed,
    onError             = Color.White,
    errorContainer      = Color(0xFF93000A),
    onErrorContainer    = Color(0xFFFFDAD6),
    background          = RetroDark,
    onBackground        = Color(0xFFCDD6F4),
    surface             = DarkSurface,
    onSurface           = Color(0xFFCDD6F4),
    surfaceVariant      = DarkSurfaceVariant,
    onSurfaceVariant    = Sage,
    outline             = RetroPurple.copy(alpha = 0.4f),
    inverseSurface      = Color(0xFFCDD6F4),
    inverseOnSurface    = RetroDark
)

private val FleecedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FleecedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FleecedColorScheme,
        shapes      = FleecedShapes,
        typography  = FleecedTypography,
        content     = content
    )
}
