package dev.bigicons.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.bigicons.app.model.ConfigStore
import dev.bigicons.app.model.WidgetConfig
import dev.bigicons.app.render.IconSource
import dev.bigicons.app.ui.BigIconsTheme
import dev.bigicons.app.ui.SectionTitle
import dev.bigicons.app.ui.StyleOptions
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
        tick.intValue++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(tick: Int, refresh: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Виджеты", "Стиль", "О приложении")
    Scaffold(
        topBar = { TopAppBar(title = { Text(titles[tab]) }) },
        bottomBar = {
            NavigationBar {
                listOf(Icons.Filled.Home, Icons.Filled.Settings, Icons.Filled.Info).forEachIndexed { i, ic ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = { Icon(ic, null) }, label = { Text(titles[i]) })
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> WidgetsScreen(tick, refresh)
                1 -> StyleScreen()
                else -> AboutScreen()
            }
        }
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

    Column(Modifier.fillMaxSize()) {
        if (ids.isEmpty()) {
            Column(Modifier.weight(1f).padding(24.dp)) {
                Text("Виджетов пока нет", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Нажмите «Закрепить виджет» или добавьте виджет BigIcons с главного экрана лаунчера.",
                    Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ids, key = { it }) { id ->
                    val cfg = store.get(id) ?: store.defaults()
                    val (w, h) = WidgetUpdater.sizeDp(ctx, mgr, id)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }
            }
        }
        if (mgr.isRequestPinAppWidgetSupported) {
            ExtendedFloatingActionButton(
                onClick = { dialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Закрепить виджет") },
                modifier = Modifier.padding(16.dp),
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Стиль по умолчанию для новых виджетов.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WidgetPreview(cfg, 140, 140, maxHeight = 140.dp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp)) {
                StyleOptions(cfg) { cfg = it; store.putDefaults(it); applied = false }
            }
        }
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

@Composable
private fun AboutScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("Как пользоваться")
        Text("1. Долгое нажатие на главном экране, Виджеты, BigIcons, выберите размер и перетащите.\n" +
            "2. В открывшемся окне выберите приложение и стиль.\n" +
            "3. Размер виджета можно менять: иконка перерисуется под новый размер.\n" +
            "4. Нажатие по виджету запускает приложение. Если оно удалено, откроется BigIcons.\n" +
            "5. Настройки можно изменить во вкладке «Виджеты» или через долгое нажатие на виджет (Android 12+).")
        SectionTitle("Ограничения")
        Text("• Лаунчер не сообщает форму своих иконок, поэтому «Авто» берёт системную маску.\n" +
            "• Виджеты не привязываются к сетке лаунчера: итоговый размер определяет сам лаунчер.\n" +
            "• Android 12+ может скруглять углы виджета.\n" +
            "• Смена обоев или темы перерисует виджеты, когда процесс приложения запущен или по таймеру (раз в 30 минут).\n" +
            "• Тема Material You приближена цветами системной палитры.")
    }
}
