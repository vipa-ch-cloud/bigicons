package dev.bigicons.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.22f

/** Карточка секции: скруглённая, с тонкой рамкой и лёгкой тенью (стиль TgAuto). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (dark) lerp(c.surface, c.primaryContainer, 0.20f) else lerp(c.surface, c.surfaceVariant, 0.28f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = if (dark) 0.52f else 0.24f)),
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = verticalArrangement, content = content)
    }
}

class NavItem(val label: String, val icon: ImageVector)

/** Нижняя навигация-«пилюля»: плавающая плашка с анимированным индикатором активного пункта. */
@Composable
fun PillNavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    val shell = if (dark) c.surface.copy(alpha = 0.92f) else lerp(c.surface, c.surfaceVariant, 0.48f).copy(alpha = 0.96f)
    val indicator = if (dark) c.primaryContainer.copy(alpha = 0.85f) else lerp(c.primaryContainer, c.surface, 0.18f)
    val index by animateFloatAsState(selected.toFloat(), tween(520, easing = CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f)), label = "pill")
    val unselected = c.onSurfaceVariant.copy(alpha = 0.6f)

    BoxWithConstraints(
        modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        val pad = 8.dp
        val itemW = (maxWidth - pad * 2) / items.size
        Surface(
            shape = RoundedCornerShape(28.dp), color = shell,
            border = BorderStroke(1.dp, if (dark) c.outlineVariant.copy(alpha = 0.42f) else c.outline.copy(alpha = 0.16f)),
            shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxWidth().height(72.dp)) {
                Surface(
                    shape = RoundedCornerShape(22.dp), color = indicator,
                    modifier = Modifier.offset(x = pad + itemW * index).padding(vertical = 6.dp).width(itemW).fillMaxHeight(),
                ) {}
                Row(Modifier.fillMaxSize().padding(horizontal = pad, vertical = 6.dp)) {
                    items.forEachIndexed { i, item ->
                        val emphasis = (1f - abs(i - index)).coerceIn(0f, 1f)
                        val tint = lerp(unselected, c.primary, emphasis)
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).clickable { onSelect(i) },
                            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(item.icon, item.label, Modifier.size(22.dp), tint = tint)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1,
                                fontWeight = if (emphasis > 0.55f) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Цвет-подложка под иконку в карточках. */
@Composable
fun accentContainer(): Color = MaterialTheme.colorScheme.primaryContainer

/** Фоновые «шары» цвета палитры (как в TgAuto): мягкое свечение за контентом. */
@Composable
fun AppBackdrop() {
    val c = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    val a = if (dark) 0.22f else 0.30f
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            androidx.compose.ui.graphics.Brush.radialGradient(listOf(c.primaryContainer.copy(alpha = a), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.05f), size.width * 0.7f),
            radius = size.width * 0.7f, center = Offset(size.width * 0.9f, size.height * 0.05f),
        )
        drawCircle(
            androidx.compose.ui.graphics.Brush.radialGradient(listOf(c.secondaryContainer.copy(alpha = a), Color.Transparent), Offset(size.width * 0.05f, size.height * 0.85f), size.width * 0.8f),
            radius = size.width * 0.8f, center = Offset(size.width * 0.05f, size.height * 0.85f),
        )
    }
}
