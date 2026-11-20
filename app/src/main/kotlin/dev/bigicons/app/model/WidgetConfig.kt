package dev.bigicons.app.model

import android.content.Context
import org.json.JSONObject
import java.io.File

enum class Shape(val title: String) {
    AUTO("Как в системе"), CIRCLE("Круг"), SQUIRCLE("Сквиркл"),
    ROUNDED("Скруглённый"), TEARDROP("Слеза"), SQUARE("Квадрат")
}

enum class ColorMode(val title: String) {
    AUTO("Авто"), ORIGINAL("Как есть"), THEMED("Тема"), CUSTOM("Свой цвет")
}

enum class StretchMode(val title: String) {
    CENTER("По центру"), STRETCH("Растянуть"), TILES("Плитки")
}

data class WidgetConfig(
    val pkg: String? = null,
    val customIcon: String? = null,
    val shape: Shape = Shape.AUTO,
    val colorMode: ColorMode = ColorMode.AUTO,
    val customColor: Int = 0xFF6750A4.toInt(),
    val stretch: StretchMode = StretchMode.STRETCH,
    val showLabel: Boolean = false,
    /** масштаб глифа, % */
    val scale: Int = 100,
    /** отступ, % от меньшей стороны */
    val padding: Int = 4,
) {
    fun toJson(): String = JSONObject().apply {
        put("pkg", pkg ?: JSONObject.NULL)
        put("customIcon", customIcon ?: JSONObject.NULL)
        put("shape", shape.name)
        put("colorMode", colorMode.name)
        put("customColor", customColor)
        put("stretch", stretch.name)
        put("showLabel", showLabel)
        put("scale", scale)
        put("padding", padding)
    }.toString()

    companion object {
        private inline fun <reified E : Enum<E>> JSONObject.enumOr(key: String, def: E): E =
            runCatching { enumValueOf<E>(getString(key)) }.getOrDefault(def)

        fun fromJson(s: String?): WidgetConfig? {
            if (s == null) return null
            return runCatching {
                val j = JSONObject(s)
                val d = WidgetConfig()
                WidgetConfig(
                    pkg = if (j.isNull("pkg")) null else j.optString("pkg").ifEmpty { null },
                    customIcon = if (j.isNull("customIcon")) null else j.optString("customIcon").ifEmpty { null },
                    shape = j.enumOr("shape", d.shape),
                    colorMode = j.enumOr("colorMode", d.colorMode),
                    customColor = j.optInt("customColor", d.customColor),
                    stretch = j.enumOr("stretch", d.stretch),
                    showLabel = j.optBoolean("showLabel", d.showLabel),
                    scale = j.optInt("scale", d.scale).coerceIn(40, 160),
                    padding = j.optInt("padding", d.padding).coerceIn(0, 30),
                )
            }.getOrNull()
        }
    }
}

/** Хранилище: JSON на каждый appWidgetId + глобальные значения по умолчанию. */
class ConfigStore(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("bigicons", Context.MODE_PRIVATE)
    private val iconsDir get() = File(ctx.filesDir, "icons").apply { mkdirs() }

    fun get(id: Int): WidgetConfig? = WidgetConfig.fromJson(prefs.getString("w$id", null))

    fun put(id: Int, cfg: WidgetConfig) {
        prefs.edit().putString("w$id", cfg.toJson()).apply()
    }

    fun delete(id: Int) {
        prefs.edit().remove("w$id").apply()
        File(iconsDir, "$id.png").delete()
    }

    fun defaults(): WidgetConfig =
        WidgetConfig.fromJson(prefs.getString("defaults", null)) ?: WidgetConfig()

    fun putDefaults(cfg: WidgetConfig) {
        prefs.edit().putString("defaults", cfg.copy(pkg = null, customIcon = null).toJson()).apply()
    }

    fun iconFile(id: Int) = File(iconsDir, "$id.png")
}
