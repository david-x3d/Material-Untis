package dev.x3d.dayline.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Mint = Color(0xFF7CDBD0)
private val Lilac = Color(0xFFC4B5FD)
private val Ink = Color(0xFF0E1116)
private val Raised = Color(0xFF171C24)
private val RaisedHigh = Color(0xFF1F2630)
private val Cream = Color(0xFFE8EEF5)
private val Danger = Color(0xFFFFB4A8)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF1A4A44),
    onPrimaryContainer = Mint,
    secondary = Lilac,
    onSecondary = Color(0xFF2E1064),
    secondaryContainer = Color(0xFF3B3358),
    onSecondaryContainer = Lilac,
    tertiary = Danger,
    onTertiary = Color(0xFF3B0A05),
    background = Ink,
    onBackground = Cream,
    surface = Raised,
    onSurface = Cream,
    surfaceVariant = RaisedHigh,
    onSurfaceVariant = Color(0xFFB5BEC9),
    error = Danger,
    onError = Color(0xFF3B0A05),
    outline = Color(0xFF3D4654),
)

private val DaylineShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val DaylineTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun MaterialUntisTheme(content: @Composable () -> Unit) {
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        DarkColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = DaylineTypography,
        shapes = DaylineShapes,
        content = content,
    )
}
