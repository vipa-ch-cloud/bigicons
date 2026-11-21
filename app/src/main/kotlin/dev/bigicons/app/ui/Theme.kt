package dev.bigicons.app.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Настройки внешнего вида самого приложения (не иконок-виджетов).
 * Хранятся в SharedPreferences, состояние наблюдаемое: переключатель в «Стиле» сразу перекрашивает UI.
 */
object UiPrefs {
    private const val FILE = "ui_prefs"
    private const val KEY_DYNAMIC = "app_dynamic_colors"

    /** true = Material You, false = «Классика» (лавандовая палитра). */
    var dynamicColors by mutableStateOf(false)
        private set
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        dynamicColors = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_DYNAMIC, false)
    }

    fun setDynamic(ctx: Context, value: Boolean) {
        dynamicColors = value
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_DYNAMIC, value).apply()
    }

    val dynamicSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

private val Sans = FontFamily.Default

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Классика: лавандовая палитра в духе TgWsProxyAndroid / TgAuto.
private val ClassicLight = lightColorScheme(
    primary = Color(0xFF5B588D), onPrimary = Color.White,
    primaryContainer = Color(0xFFE2DFFF), onPrimaryContainer = Color(0xFF1A1744),
    secondary = Color(0xFF5B588D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2DFFF), onSecondaryContainer = Color(0xFF1A1744),
    background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFF6F3FA), onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE4E1EC), onSurfaceVariant = Color(0xFF47464F),
    surfaceContainerLow = Color(0xFFF3F0F8), surfaceContainer = Color(0xFFEFECF4),
    surfaceContainerHigh = Color(0xFFE9E6EF), surfaceContainerHighest = Color(0xFFE4E1E9),
    outline = Color(0xFF787680), outlineVariant = Color(0xFFC8C5D0),
)

private val ClassicDark = darkColorScheme(
    primary = Color(0xFFC4C0FF), onPrimary = Color(0xFF2D2A5B),
    primaryContainer = Color(0xFF434073), onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFFC4C0FF), onSecondary = Color(0xFF2D2A5B),
    secondaryContainer = Color(0xFF434073), onSecondaryContainer = Color(0xFFE2DFFF),
    background = Color(0xFF131316), onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF1B1B1F), onSurface = Color(0xFFC8C5D0),
    surfaceVariant = Color(0xFF47464F), onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainerLow = Color(0xFF1B1B1F), surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF2A2A2E), surfaceContainerHighest = Color(0xFF353539),
    outline = Color(0xFF918F9A), outlineVariant = Color(0xFF47464F),
)

@Composable
fun BigIconsTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    UiPrefs.load(ctx)
    val dark = isSystemInDarkTheme()
    val scheme: ColorScheme = when {
        UiPrefs.dynamicColors && UiPrefs.dynamicSupported ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> if (dark) ClassicDark else ClassicLight
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
