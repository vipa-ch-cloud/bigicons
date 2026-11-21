package dev.bigicons.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.bigicons.app.model.ConfigStore
import dev.bigicons.app.model.WidgetConfig
import dev.bigicons.app.render.IconSource
import dev.bigicons.app.ui.*
import dev.bigicons.app.widget.WidgetUpdater
import java.io.File

class ConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val store = ConfigStore(this)
        val mgr = AppWidgetManager.getInstance(this)
        val existing = store.get(widgetId)
        val initial = existing ?: store.defaults()
        val (wDp, hDp) = WidgetUpdater.sizeDp(this, mgr, widgetId)

        setContent {
            BigIconsTheme {
                ConfigScreen(
                    initial = initial, isNew = existing == null, wDp = wDp, hDp = hDp, widgetId = widgetId,
                    onCancel = { finish() },
                    onDone = { cfg ->
                        store.put(widgetId, finalizeIcon(store, cfg))
                        WidgetUpdater.update(this, mgr, widgetId)
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    },
                )
            }
        }
    }

    /** Временный файл своей иконки превращаем в постоянный. */
    private fun finalizeIcon(store: ConfigStore, cfg: WidgetConfig): WidgetConfig {
        val final = store.iconFile(widgetId)
        val name = cfg.customIcon
        return when {
            name == null -> { final.delete(); cfg }
            name.endsWith(".tmp.png") -> {
                val tmp = File(final.parentFile, name)
                if (tmp.exists()) { final.delete(); tmp.renameTo(final) }
                cfg.copy(customIcon = final.name)
            }
            else -> cfg
        }
    }
}

private fun importIcon(ctx: android.content.Context, uri: Uri, out: File): Boolean = runCatching {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    var sample = 1
    while (opts.outWidth / sample > 1024 || opts.outHeight / sample > 1024) sample *= 2
    val bmp = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@runCatching false
    out.parentFile?.mkdirs()
    out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    true
}.getOrDefault(false)

@Composable
private fun ConfigScreen(
    initial: WidgetConfig, isNew: Boolean, wDp: Int, hDp: Int, widgetId: Int,
    onCancel: () -> Unit, onDone: (WidgetConfig) -> Unit,
) {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf(isNew && initial.pkg == null) }
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var iconTick by remember { mutableStateOf(0) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val tmp = File(File(ctx.filesDir, "icons"), "$widgetId.tmp.png")
            if (importIcon(ctx, uri, tmp)) { cfg = cfg.copy(customIcon = tmp.name); iconTick++ }
        }
    }
    val appLabel = remember(cfg.pkg) { IconSource.appLabel(ctx, cfg.pkg) }
    LaunchedEffect(picking) { if (picking && apps == null) apps = loadApps(ctx) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ConfigScaffold(
            title = "Настройка виджета",
            subtitle = appLabel ?: cfg.pkg,
            onClose = onCancel,
            actions = {
                BigButton("Отмена", Icons.Default.Close, onCancel, Modifier.weight(1f))
                BigButton("Готово", Icons.Default.Check, { onDone(cfg) }, Modifier.weight(1f))
            },
        ) {
            WidgetPreview(cfg, wDp, hDp, key = iconTick)
            AppSectionCard {
                SectionHeader(Icons.Default.Settings, "Приложение")
                Text(appLabel ?: cfg.pkg ?: "Не выбрано", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                BigButton("Выбрать приложение", Icons.Default.Search, { picking = true })
            }
            AppSectionCard {
                SectionHeader(Icons.Default.Add, "Своя иконка")
                Hint("Своя картинка проходит ту же маску и цветовую обработку.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Выбрать картинку", Icons.Default.Add, modifier = Modifier.weight(1f), onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    if (cfg.customIcon != null) {
                        BigButton("Убрать", Icons.Default.Delete, modifier = Modifier.weight(1f), danger = true,
                            onClick = { cfg = cfg.copy(customIcon = null) })
                    }
                }
            }
            StyleSections(cfg) { cfg = it }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (picking) {
        AppPickerDialog(
            apps = apps,
            onPick = { cfg = cfg.copy(pkg = it); picking = false },
            onDismiss = { if (cfg.pkg != null || cfg.customIcon != null) picking = false else onCancel() },
        )
    }
}
