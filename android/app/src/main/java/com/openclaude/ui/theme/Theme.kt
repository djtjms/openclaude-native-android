package com.openclaude.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BrandPurple = Color(0xFF7C5CFF)
val BrandBlue = Color(0xFF5B8CFF)
val SurfaceColor = Color(0xFF0E0E11)
val SurfaceVariantColor = Color(0xFF1A1A24)
val OnSurfaceColor = Color(0xFFE8E8ED)
val OnSurfaceVariantColor = Color(0xFFA0A0B0)
val OutlineColor = Color(0xFF2A2A35)

val BrandGradient = Brush.horizontalGradient(listOf(BrandPurple, BrandBlue))

private val DarkColorScheme = darkColorScheme(
    surface = SurfaceColor,
    background = SurfaceColor,
    primary = BrandBlue,
    secondary = BrandPurple,
    onSurface = OnSurfaceColor,
    onBackground = OnSurfaceColor,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = OnSurfaceVariantColor,
    outline = OutlineColor,
)

@Composable
fun OpenClaudeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
        ),
        content = content
    )
}