package dev.bigicons.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.bigicons.app.model.WidgetConfig
import dev.bigicons.app.render.IconRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap)

suspend fun loadApps(ctx: Context): List<AppEntry> = withContext(Dispatchers.IO) {
    val pm = ctx.packageManager
    val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(q, 0)
        .distinctBy { it.activityInfo.packageName }
        .map {
            AppEntry(
                it.activityInfo.packageName,
                it.loadLabel(pm).toString(),
                runCatching { it.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }
                    .getOrElse { android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap() },
            )
        }
        .sortedBy { it.label.lowercase() }
}

/** Превью виджета: рендерится тем же кодом, что и сам виджет. */
@Composable
fun WidgetPreview(cfg: WidgetConfig, wDp: Int, hDp: Int, maxHeight: Dp = 180.dp, key: Any? = null) {
    val ctx = LocalContext.current
    var bmp by remember { mutableStateOf<ImageBitmap?>(null) }
    val density = ctx.resources.displayMetrics.density
    LaunchedEffect(cfg, wDp, hDp, key) {
        bmp = withContext(Dispatchers.Default) {
            runCatching {
                IconRenderer.render(ctx, cfg, (wDp * density).toInt(), (hDp * density).toInt(), density).asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .heightIn(min = 90.dp, max = maxHeight + 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        bmp?.let {
            Image(
                it, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.heightIn(max = maxHeight).aspectRatio(wDp.toFloat() / hDp),
            )
        }
    }
}
