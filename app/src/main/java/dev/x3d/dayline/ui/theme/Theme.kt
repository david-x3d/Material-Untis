package dev.x3d.dayline.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.x3d.dayline.domain.model.SubjectColors

// Material-Untis palette: mint primary, lilac secondary, coral tertiary, teal-tinted surfaces
// (no neutral grey anywhere, so the app keeps its colour even with a grey wallpaper).
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CDBD0),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF1E5751),
    onPrimaryContainer = Color(0xFFA3F5EA),
    inversePrimary = Color(0xFF006A62),
    secondary = Color(0xFFC9B8FF),
    onSecondary = Color(0xFF32156E),
    secondaryContainer = Color(0xFF4A3A86),
    onSecondaryContainer = Color(0xFFE7DEFF),
    tertiary = Color(0xFFFFB59E),
    onTertiary = Color(0xFF5C1A06),
    tertiaryContainer = Color(0xFF7B2E18),
    onTertiaryContainer = Color(0xFFFFDBD0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1413),
    onBackground = Color(0xFFDDE7E4),
    surface = Color(0xFF0B1413),
    onSurface = Color(0xFFDDE7E4),
    surfaceVariant = Color(0xFF2D3D3A),
    onSurfaceVariant = Color(0xFFB4C9C4),
    surfaceTint = Color(0xFF7CDBD0),
    inverseSurface = Color(0xFFDDE7E4),
    inverseOnSurface = Color(0xFF283230),
    outline = Color(0xFF7F9692),
    outlineVariant = Color(0xFF334643),
    surfaceBright = Color(0xFF2C3836),
    surfaceDim = Color(0xFF0B1413),
    surfaceContainerLowest = Color(0xFF060F0E),
    surfaceContainerLow = Color(0xFF131E1C),
    surfaceContainer = Color(0xFF172422),
    surfaceContainerHigh = Color(0xFF1F2E2C),
    surfaceContainerHighest = Color(0xFF283937),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A62),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2E6),
    onPrimaryContainer = Color(0xFF00201D),
    inversePrimary = Color(0xFF7CDBD0),
    secondary = Color(0xFF6650A4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DDFF),
    onSecondaryContainer = Color(0xFF22005D),
    tertiary = Color(0xFF9C4235),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD3),
    onTertiaryContainer = Color(0xFF3E0500),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2FBF8),
    onBackground = Color(0xFF151D1C),
    surface = Color(0xFFF2FBF8),
    onSurface = Color(0xFF151D1C),
    surfaceVariant = Color(0xFFD5E8E3),
    onSurfaceVariant = Color(0xFF3B4C48),
    surfaceTint = Color(0xFF006A62),
    inverseSurface = Color(0xFF2A3231),
    inverseOnSurface = Color(0xFFE8F2EF),
    outline = Color(0xFF6A7D79),
    outlineVariant = Color(0xFFB9CCC7),
    surfaceBright = Color(0xFFF2FBF8),
    surfaceDim = Color(0xFFD2DDDA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEAF6F2),
    surfaceContainer = Color(0xFFE2F1EC),
    surfaceContainerHigh = Color(0xFFD8EBE6),
    surfaceContainerHighest = Color(0xFFCDE4DE),
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

/** Subject colours chosen in the settings; read with [subjectColor]. */
val LocalSubjectColors = staticCompositionLocalOf { SubjectColors() }

@Composable
fun subjectColor(subject: String): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return Color(LocalSubjectColors.current.argb(subject, dark))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialUntisTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    subjectColors: SubjectColors = SubjectColors(),
    content: @Composable () -> Unit,
) {
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colors = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalSubjectColors provides subjectColors) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
            typography = DaylineTypography,
            shapes = DaylineShapes,
            content = content,
        )
    }
}
