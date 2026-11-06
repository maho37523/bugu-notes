package com.dspro.android.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val DshGreen = Color(0xFF315B4A)
val DshGreenPressed = Color(0xFF244638)
val DshGreenSoft = Color(0xFFDCEFE5)

data class DshPalette(
    val sidebar: Color,
    val main: Color,
    val surface: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val muted: Color,
    val subtle: Color,
    val navActive: Color,
    val navActiveText: Color,
    val userBubble: Color,
    val codeBg: Color,
    val chipBg: Color,
    val accent: Color,
    val accentSoft: Color,
)

private val LightPalette = DshPalette(
    sidebar = Color(0xFFF2F7F3),
    main = Color(0xFFFFFEFC),
    surface = Color(0xFFFFFEFC),
    hairline = Color(0xFFDCE6DF),
    hairlineStrong = Color(0xFFC4D2C8),
    muted = Color(0xFF5B665D),
    subtle = Color(0xFF68776D),
    navActive = Color(0xFFDCEFE5),
    navActiveText = Color(0xFF254B3A),
    userBubble = Color(0xFFEAF2EB),
    codeBg = Color(0xFFF0F4EF),
    chipBg = Color(0xFFEAF2EB),
    accent = DshGreen,
    accentSoft = DshGreenSoft,
)

private val DarkPalette = DshPalette(
    sidebar = Color(0xFF191E1A),
    main = Color(0xFF20251F),
    surface = Color(0xFF282D27),
    hairline = Color(0xFF39443A),
    hairlineStrong = Color(0xFF4A584C),
    muted = Color(0xFFC1CDC1),
    subtle = Color(0xFFAAB9AB),
    navActive = Color(0xFF344A3C),
    navActiveText = Color(0xFFDCEFE1),
    userBubble = Color(0xFF303931),
    codeBg = Color(0xFF303831),
    chipBg = Color(0xFF303931),
    accent = Color(0xFF9BD5B2),
    accentSoft = Color(0xFF344A3C),
)

val LocalDshPalette = staticCompositionLocalOf { LightPalette }
val LocalDshDark = staticCompositionLocalOf { false }

object Dsh {
    val colors: DshPalette
        @Composable get() = LocalDshPalette.current
    val dark: Boolean
        @Composable get() = LocalDshDark.current
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF315B4A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = DshGreenSoft,
    onPrimaryContainer = Color(0xFF254B3A),
    background = Color(0xFFFFFEFC),
    onBackground = Color(0xFF272D28),
    surface = Color(0xFFFFFEFC),
    onSurface = Color(0xFF272D28),
    surfaceVariant = Color(0xFFEAF2EB),
    onSurfaceVariant = Color(0xFF526257),
    outline = Color(0xFFC4D2C8),
    outlineVariant = Color(0xFFDCE6DF),
    error = Color(0xFFD92D20),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD5B2),
    onPrimary = Color(0xFF173626),
    primaryContainer = Color(0xFF344A3C),
    onPrimaryContainer = Color(0xFFDCEFE1),
    background = Color(0xFF20251F),
    onBackground = Color(0xFFF1F4EF),
    surface = Color(0xFF282D27),
    onSurface = Color(0xFFF1F4EF),
    surfaceVariant = Color(0xFF303931),
    onSurfaceVariant = Color(0xFFC1CDC1),
    outline = Color(0xFF4A584C),
    outlineVariant = Color(0xFF39443A),
    error = Color(0xFFF97066),
)

private val DshTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 15.sp,
    ),
)

private val DshShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun DshTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    activity.window.isStatusBarContrastEnforced = false
                    activity.window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalDshPalette provides palette,
        LocalDshDark provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = DshTypography,
            shapes = DshShapes,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
