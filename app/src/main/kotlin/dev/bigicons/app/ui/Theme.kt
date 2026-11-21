package dev.bigicons.app.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Настройки внешнего вида самого приложения (не иконок-виджетов), как в TgAuto:
 * режим (система/светлая/тёмная), палитра и переключатель Material You.
 * Состояние наблюдаемое: изменение сразу перекрашивает UI.
 */
object UiPrefs {
    private const val FILE = "ui_prefs"
    private const val KEY_DYNAMIC = "app_dynamic_colors"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_PALETTE = "palette"

    const val MODE_SYSTEM = "system"
    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"
    val modes = listOf(MODE_SYSTEM, MODE_LIGHT, MODE_DARK)

    var themeMode by mutableStateOf(MODE_SYSTEM)
        private set
    var palette by mutableStateOf(Palettes.INDIGO)
        private set
    /** true = цвета из обоев (Android 12+). По умолчанию включено там, где поддерживается. */
    var dynamicColors by mutableStateOf(false)
        private set
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        themeMode = p.getString(KEY_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
        palette = p.getString(KEY_PALETTE, Palettes.INDIGO) ?: Palettes.INDIGO
        dynamicColors = p.getBoolean(KEY_DYNAMIC, dynamicSupported)
    }

    private fun edit(ctx: Context, f: android.content.SharedPreferences.Editor.() -> Unit) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().apply(f).apply()

    fun setThemeMode(ctx: Context, v: String) { themeMode = v; edit(ctx) { putString(KEY_MODE, v) } }
    fun setPalette(ctx: Context, v: String) { palette = v; edit(ctx) { putString(KEY_PALETTE, v) } }
    fun setDynamic(ctx: Context, v: Boolean) { dynamicColors = v; edit(ctx) { putBoolean(KEY_DYNAMIC, v) } }

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

@Composable
fun BigIconsTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    UiPrefs.load(ctx)
    val dark = when (UiPrefs.themeMode) {
        UiPrefs.MODE_LIGHT -> false
        UiPrefs.MODE_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val scheme: ColorScheme = when {
        UiPrefs.dynamicColors && UiPrefs.dynamicSupported ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> Palettes.scheme(UiPrefs.palette, dark)
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
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes) {
        // Корень темы: Surface задаёт LocalContentColor = onBackground. Без него Text вне Surface/Scaffold
        // рисуется чёрным (LocalContentColor по умолчанию) и пропадает на тёмных палитрах.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, content = content)
    }
}
