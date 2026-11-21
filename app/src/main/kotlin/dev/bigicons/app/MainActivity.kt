package dev.bigicons.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.bigicons.app.model.ConfigStore
import dev.bigicons.app.render.IconSource
import dev.bigicons.app.ui.AppCard
import dev.bigicons.app.ui.BigIconsTheme
import dev.bigicons.app.ui.InfoScreen
import dev.bigicons.app.ui.NavItem
import dev.bigicons.app.ui.PillNavigationBar
import dev.bigicons.app.ui.Segments
import dev.bigicons.app.ui.SectionTitle
import dev.bigicons.app.ui.StyleOptions
import dev.bigicons.app.ui.UiPrefs
import dev.bigicons.app.ui.WidgetPreview
import dev.bigicons.app.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BigIconsTheme { MainScreen(tick.intValue) { tick.intValue++ } } }
    }

    override fun onResume() {
        super.onResume()
        // пересчёт цветов при каждом открытии: акцент/обои/тема могли смениться без уведомления
        WidgetUpdater.updateAll(this)
        tick.intValue++
    }
}

private val NAV_RESERVE = 104.dp

@Composable
private fun MainScreen(tick: Int, refresh: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val items = remember {
        listOf(NavItem("Виджеты", Icons.Filled.Home), NavItem("Стиль", Icons.Filled.Settings), NavItem("Инфо", Icons.Filled.Info))
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Text(
                items[tab].label, style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
            )
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> WidgetsScreen(tick, refresh)
                    1 -> StyleScreen()
                    else -> InfoScreen(NAV_RESERVE)
                }
            }
        }
        PillNavigationBar(items, tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun WidgetsScreen(tick: Int, refresh: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ConfigStore(ctx) }
    val mgr = remember { AppWidgetManager.getInstance(ctx) }
    val ids = remember(tick) { WidgetUpdater.allIds(ctx) }
    var dialog by remember { mutableStateOf(false) }

    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Размер виджета") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    WidgetUpdater.SIZES.forEach { (c, r) ->
                        TextButton(onClick = {
                            dialog = false
                            if (mgr.isRequestPinAppWidgetSupported) {
                                mgr.requestPinAppWidget(ComponentName(ctx.packageName, WidgetUpdater.providerClass(c, r)), null, null)
                            }
                        }, Modifier.fillMaxWidth()) { Text("$c × $r") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ dialog = false }) { Text("Отмена") } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (ids.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                Text("Виджетов пока нет", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Нажмите «Добавить виджет» или добавьте виджет BigIcons с главного экрана лаунчера.",
                    Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = NAV_RESERVE + 72.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(ids, key = { it }) { id ->
                    val cfg = store.get(id) ?: store.defaults()
                    val (w, h) = WidgetUpdater.sizeDp(ctx, mgr, id)
                    AppCard {
                        WidgetPreview(cfg, w, h, maxHeight = 120.dp, key = tick)
                        Text(
                            IconSource.appLabel(ctx, cfg.pkg) ?: if (cfg.customIcon != null) "Своя иконка" else "Не настроен",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({
                                ctx.startActivity(Intent(ctx, ConfigActivity::class.java)
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                            }) { Text("Править") }
                            OutlinedButton({
                                store.put(id, store.defaults().copy(pkg = cfg.pkg))
                                WidgetUpdater.update(ctx, mgr, id)
                                refresh()
                            }) { Text("Сбросить") }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { WidgetUpdater.updateAll(ctx); refresh() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Обновить все виджеты") }
                }
            }
        }
        if (mgr.isRequestPinAppWidgetSupported) {
            ExtendedFloatingActionButton(
                onClick = { dialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Добавить виджет") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = NAV_RESERVE),
            )
        }
    }
}

@Composable
private fun StyleScreen() {
    val ctx = LocalContext.current
    val store = remember { ConfigStore(ctx) }
    var cfg by remember { mutableStateOf(store.defaults()) }
    var applied by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = NAV_RESERVE + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppCard {
            SectionTitle("Цвета приложения")
            val modes = if (UiPrefs.dynamicSupported) listOf(false, true) else listOf(false)
            Segments(modes, UiPrefs.dynamicColors, { if (it) "Material You" else "Классика" }) { UiPrefs.setDynamic(ctx, it) }
            Text(
                if (UiPrefs.dynamicSupported) "Классика: лавандовая палитра. Material You: цвета берутся из обоев (Android 12+). На виджеты-иконки это не влияет."
                else "Material You доступен с Android 12. Используется лавандовая палитра.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Стиль по умолчанию для новых виджетов.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        WidgetPreview(cfg, 140, 140, maxHeight = 140.dp)
        AppCard { StyleOptions(cfg) { cfg = it; store.putDefaults(it); applied = false } }
        OutlinedButton(onClick = {
            val mgr = AppWidgetManager.getInstance(ctx)
            WidgetUpdater.allIds(ctx).forEach { id ->
                val old = store.get(id) ?: return@forEach
                store.put(id, cfg.copy(pkg = old.pkg, customIcon = old.customIcon))
                WidgetUpdater.update(ctx, mgr, id)
            }
            applied = true
        }, Modifier.fillMaxWidth()) { Text(if (applied) "Применено" else "Применить ко всем виджетам") }
    }
}
