package dev.bigicons.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bigicons.app.model.ColorMode
import dev.bigicons.app.model.Shape
import dev.bigicons.app.model.StretchMode
import dev.bigicons.app.model.ThemeVariant
import dev.bigicons.app.model.WidgetConfig

private val Swatches = listOf(
    0xFF6750A4, 0xFF1E88E5, 0xFF00897B, 0xFF43A047, 0xFFF9A825, 0xFFEF6C00,
    0xFFE53935, 0xFFD81B60, 0xFF5D4037, 0xFF37474F, 0xFFFFFFFF, 0xFF000000,
).map { it.toInt() }

/**
 * Настройки стиля виджета тремя карточками-секциями (как SettingsTab в TgAuto):
 * форма, цвет, размещение. Вызывается внутри колонки экрана.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.StyleSections(cfg: WidgetConfig, onChange: (WidgetConfig) -> Unit) {
    AppSectionCard {
        SectionHeader(Icons.Default.Settings, "Форма маски")
        ChoiceRow(Shape.entries, cfg.shape, { it.title }) { onChange(cfg.copy(shape = it)) }
    }
    AppSectionCard {
        SectionHeader(Icons.Default.Settings, "Цвет иконки")
        ChoiceRow(ColorMode.entries, cfg.colorMode, { it.title }, perRow = 2) { onChange(cfg.copy(colorMode = it)) }
        Hint(
            when (cfg.colorMode) {
                ColorMode.AUTO -> "Тематическая (Material You), если у приложения есть monochrome-слой на Android 13+, иначе как есть."
                ColorMode.ORIGINAL -> "Оригинальные цвета иконки."
                ColorMode.THEMED -> "Цвета берутся из системной палитры Material You (акцент из настроек обоев и стиля). Без monochrome-слоя глиф строится из силуэта."
                ColorMode.CUSTOM -> "Одноцветный глиф на выбранном цвете."
            }
        )
        if (cfg.colorMode == ColorMode.THEMED || cfg.colorMode == ColorMode.AUTO) {
            SectionDivider()
            ChoiceRow(ThemeVariant.entries, cfg.themeVariant, { it.title }, perRow = 2) { onChange(cfg.copy(themeVariant = it)) }
            Hint(
                when (cfg.themeVariant) {
                    ThemeVariant.NEUTRAL -> "Как обычные плитки в системе: нейтральный фон из системной палитры."
                    ThemeVariant.ACCENT -> "Как выделенная плитка: фон и глиф в цвете акцента."
                }
            )
        }
        if (cfg.colorMode == ColorMode.CUSTOM) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Swatches.forEach { col ->
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(col))
                            .border(
                                if (cfg.customColor == col) 3.dp else 1.dp,
                                if (cfg.customColor == col) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            )
                            .clickable { onChange(cfg.copy(customColor = col)) },
                    )
                }
            }
        }
    }
    AppSectionCard {
        SectionHeader(Icons.Default.Settings, "Размещение")
        FieldLabel("Растяжение")
        ChoiceRow(StretchMode.entries, cfg.stretch, { it.title }) { onChange(cfg.copy(stretch = it)) }
        Hint(
            when (cfg.stretch) {
                StretchMode.CENTER -> "Одна иконка обычного размера по центру виджета."
                StretchMode.STRETCH -> "Одна плитка на весь виджет с закруглением как у иконок."
                StretchMode.TILES -> "Несколько одинаковых иконок по сетке виджета."
            }
        )
        SectionDivider()
        SwitchRow("Показывать подпись", null, cfg.showLabel) { onChange(cfg.copy(showLabel = it)) }
        SectionDivider()
        FieldLabel("Масштаб глифа: ${cfg.scale}%")
        Slider(cfg.scale.toFloat(), { onChange(cfg.copy(scale = it.toInt())) }, valueRange = 60f..140f)
        FieldLabel("Отступ: ${cfg.padding}%")
        Slider(cfg.padding.toFloat(), { onChange(cfg.copy(padding = it.toInt())) }, valueRange = 0f..25f)
    }
}
