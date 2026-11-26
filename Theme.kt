package com.pushlock.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette: near-black canvas, one electric-lime accent.
val Bg = Color(0xFF0B0E13)
val Surface1 = Color(0xFF12161D)
val Surface2 = Color(0xFF1A1F28)
val Line = Color(0xFF262D38)
val Accent = Color(0xFFC6FF3D)
val TextPrimary = Color(0xFFF2F5F9)
val TextMuted = Color(0xFF8A94A6)
val Danger = Color(0xFFFF6B6B)
val Warn = Color(0xFFFFC24D)

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    secondary = Accent,
    onSecondary = Bg,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    outline = Line,
    error = Danger,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 72.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun PushLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, shapes = AppShapes) {
        // Surface supplies the default text colour (otherwise Text would be black).
        Surface(color = Color.Transparent, contentColor = TextPrimary, content = content)
    }
}

/** Dark canvas with a soft lime glow in the top-right corner. */
fun Modifier.appBackground(): Modifier = this
    .background(Bg)
    .drawBehind {
        drawRect(
            Brush.radialGradient(
                colors = listOf(Accent.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.85f, 0f),
                radius = size.width * 0.95f,
            ),
        )
    }
