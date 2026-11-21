package dev.bigicons.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bigicons.app.model.ColorMode
import dev.bigicons.app.model.Shape
import dev.bigicons.app.model.StretchMode
import dev.bigicons.app.model.ThemeVariant
import dev.bigicons.app.model.WidgetConfig

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Segments(items: List<T>, selected: T?, title: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        items.forEachIndexed { i, it ->
            SegmentedButton(
                selected = it == selected,
                onClick = { onSelect(it) },
                shape = SegmentedButtonDefaults.itemShape(i, items.size),
                label = { Text(title(it), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

private val Swatches = listOf(
    0xFF6750A4, 0xFF1E88E5, 0xFF00897B, 0xFF43A047, 0xFFF9A825, 0xFFEF6C00,
    0xFFE53935, 0xFFD81B60, 0xFF5D4037, 0xFF37474F, 0xFFFFFFFF, 0xFF000000,
).map { it.toInt() }

/** Общий набор настроек стиля: форма, цвет, режим растяжения, подпись, масштаб, отступ. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleOptions(cfg: WidgetConfig, onChange: (WidgetConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Форма маски")
        Shape.entries.chunked(3).forEach { row ->
            Segments(row, cfg.shape.takeIf { it in row }, { it.title }) { onChange(cfg.copy(shape = it)) }
        }
        SectionTitle("Цвет иконки")
        Segments(ColorMode.entries, cfg.colorMode, { it.title }) { onChange(cfg.copy(colorMode = it)) }
        Text(
            when (cfg.colorMode) {
                ColorMode.AUTO -> "Тематическая (Material You), если у приложения есть monochrome-слой на Android 13+, иначе как есть."
                ColorMode.ORIGINAL -> "Оригинальные цвета иконки."
                ColorMode.THEMED -> "Цвета берутся из системной палитры Material You (акцент из настроек обоев и стиля). Без monochrome-слоя глиф строится из силуэта."
                ColorMode.CUSTOM -> "Одноцветный глиф на выбранном цвете."
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (cfg.colorMode == ColorMode.THEMED || cfg.colorMode == ColorMode.AUTO) {
            Segments(ThemeVariant.entries, cfg.themeVariant, { it.title }) { onChange(cfg.copy(themeVariant = it)) }
            Text(
                when (cfg.themeVariant) {
                    ThemeVariant.NEUTRAL -> "Как обычные плитки в системе: нейтральный фон из системной палитры."
                    ThemeVariant.ACCENT -> "Как выделенная плитка: фон и глиф в цвете акцента."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (cfg.colorMode == ColorMode.CUSTOM) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Swatches.forEach { col ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(col))
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
        SectionTitle("Растяжение")
        Segments(StretchMode.entries, cfg.stretch, { it.title }) { onChange(cfg.copy(stretch = it)) }
        Text(
            when (cfg.stretch) {
                StretchMode.CENTER -> "Одна иконка обычного размера по центру виджета."
                StretchMode.STRETCH -> "Одна плитка на весь виджет с закруглением как у иконок."
                StretchMode.TILES -> "Несколько одинаковых иконок по сетке виджета."
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Показывать подпись", Modifier.weight(1f))
            Switch(cfg.showLabel, { onChange(cfg.copy(showLabel = it)) })
        }
        Text("Масштаб глифа: ${cfg.scale}%")
        Slider(cfg.scale.toFloat(), { onChange(cfg.copy(scale = it.toInt())) }, valueRange = 60f..140f)
        Text("Отступ: ${cfg.padding}%")
        Slider(cfg.padding.toFloat(), { onChange(cfg.copy(padding = it.toInt())) }, valueRange = 0f..25f)
    }
}
