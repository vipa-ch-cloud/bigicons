package dev.bigicons.app.render

import dev.bigicons.app.model.ThemeVariant

/** Имена цветовых ресурсов android.R.color (system_*), выбранные для фона и глифа. */
data class ToneNames(val bg: String, val glyph: String)

/**
 * Чистая функция выбора тонов Material You (без обращения к Context, проверяется юнит-тестом).
 * Android 14+ (API 34): готовые роли system_surface_container_* / system_primary_container_* / system_on_*.
 * Android 12-13 (API 31-33): тона палитр system_neutral1_* и system_accent1_*.
 * Ниже 31 системных цветов нет — возвращает null (запасная палитра).
 */
object ThemeTones {
    fun pick(sdk: Int, night: Boolean, variant: ThemeVariant): ToneNames? {
        if (sdk < 31) return null
        val s = if (night) "dark" else "light"
        return when (variant) {
            ThemeVariant.NEUTRAL ->
                if (sdk >= 34) ToneNames("system_surface_container_$s", if (night) "system_neutral1_50" else "system_neutral1_900")
                else if (night) ToneNames("system_neutral1_900", "system_neutral1_50")
                else ToneNames("system_neutral1_100", "system_neutral1_900")
            ThemeVariant.ACCENT ->
                if (sdk >= 34) ToneNames("system_primary_container_$s", "system_on_primary_container_$s")
                else if (night) ToneNames("system_accent1_800", "system_accent1_100")
                else ToneNames("system_accent1_100", "system_accent1_700")
        }
    }
}
