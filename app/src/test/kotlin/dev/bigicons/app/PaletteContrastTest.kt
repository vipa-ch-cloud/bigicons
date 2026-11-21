package dev.bigicons.app

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import dev.bigicons.app.ui.Palettes
import org.junit.Assert.assertTrue
import org.junit.Test

/** Текст должен читаться на любой палитре: контраст не ниже 4.5:1 (WCAG AA) для основных пар цветов. */
class PaletteContrastTest {
    // Формула из androidx.core.graphics.ColorUtils.calculateContrast (WCAG 2.x). Сам ColorUtils на JVM-тесте
    // без Robolectric вызывает android.graphics.Color.alpha() и падает ("not mocked"), поэтому формула повторена здесь.
    private fun lum(argb: Int): Double {
        fun ch(v: Int): Double { val c = v / 255.0; return if (c < 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4) }
        return 0.2126 * ch((argb shr 16) and 0xFF) + 0.7152 * ch((argb shr 8) and 0xFF) + 0.0722 * ch(argb and 0xFF)
    }
    private fun contrast(fg: Int, bg: Int): Double {
        val a = lum(fg) + 0.05; val b = lum(bg) + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    private fun pairs(c: ColorScheme) = listOf(
        "onSurface/surface" to (c.onSurface to c.surface),
        "onBackground/background" to (c.onBackground to c.background),
        "onPrimary/primary" to (c.onPrimary to c.primary),
        "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
        "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface),
        "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
        "onSecondaryContainer/secondaryContainer" to (c.onSecondaryContainer to c.secondaryContainer),
        "primary/surface (акцентный текст)" to (c.primary to c.surface),
    )

    @Test fun everyPaletteIsReadable() {
        val failures = mutableListOf<String>()
        for ((id, _) in Palettes.all) for (dark in listOf(false, true)) {
            for ((name, p) in pairs(Palettes.scheme(id, dark))) {
                val ratio = contrast(p.first.toArgb(), p.second.toArgb())
                if (ratio < 4.5) failures += "$id ${if (dark) "dark" else "light"} $name = %.2f".format(ratio)
            }
        }
        assertTrue("Низкий контраст:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
