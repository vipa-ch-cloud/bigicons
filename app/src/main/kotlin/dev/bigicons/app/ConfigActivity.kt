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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.bigicons.app.model.ConfigStore
import dev.bigicons.app.model.WidgetConfig
import dev.bigicons.app.render.IconSource
import dev.bigicons.app.ui.AppCard
import dev.bigicons.app.ui.AppEntry
import dev.bigicons.app.ui.BigIconsTheme
import dev.bigicons.app.ui.SectionTitle
import dev.bigicons.app.ui.StyleOptions
import dev.bigicons.app.ui.WidgetPreview
import dev.bigicons.app.ui.loadApps
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(
    initial: WidgetConfig, isNew: Boolean, wDp: Int, hDp: Int, widgetId: Int,
    onCancel: () -> Unit, onDone: (WidgetConfig) -> Unit,
) {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf(isNew && initial.pkg == null) }
    var iconTick by remember { mutableStateOf(0) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val tmp = File(File(ctx.filesDir, "icons"), "$widgetId.tmp.png")
            if (importIcon(ctx, uri, tmp)) { cfg = cfg.copy(customIcon = tmp.name); iconTick++ }
        }
    }
    val appLabel = remember(cfg.pkg) { IconSource.appLabel(ctx, cfg.pkg) }

    if (picking) {
        AppPicker(onPick = { cfg = cfg.copy(pkg = it); picking = false }, onBack = {
            if (cfg.pkg != null || cfg.customIcon != null) picking = false else onCancel()
        })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройка виджета") },
                navigationIcon = { IconButton(onCancel) { Icon(Icons.Filled.Close, "Отмена") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onDone(cfg) },
                shape = MaterialTheme.shapes.large,
                icon = { Icon(Icons.Filled.Check, null) },
                text = { Text("Готово") },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WidgetPreview(cfg, wDp, hDp, key = iconTick)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Приложение")
                    Text(appLabel ?: cfg.pkg ?: "Не выбрано", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { picking = true }, Modifier.fillMaxWidth()) { Text("Выбрать приложение") }
                    SectionTitle("Своя иконка")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }, Modifier.weight(1f)) { Text("Выбрать картинку") }
                        if (cfg.customIcon != null) {
                            OutlinedButton(onClick = { cfg = cfg.copy(customIcon = null) }) { Text("Убрать") }
                        }
                    }
                    Text(
                        "Своя картинка проходит ту же маску и цветовую обработку.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AppCard { StyleOptions(cfg) { cfg = it } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPicker(onPick: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = loadApps(ctx) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Выбор приложения") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Поиск") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            )
            val list = apps?.filter { it.label.contains(query, true) || it.pkg.contains(query, true) }
            if (list == null) Text("Загрузка…", Modifier.padding(16.dp))
            else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(list, key = { it.pkg }) { a ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(a.pkg) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(a.icon, null, Modifier.size(44.dp))
                        Column {
                            Text(a.label, style = MaterialTheme.typography.bodyLarge)
                            Text(a.pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
