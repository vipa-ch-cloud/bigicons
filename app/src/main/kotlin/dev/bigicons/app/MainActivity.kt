package dev.bigicons.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bigicons.app.model.ConfigStore
import dev.bigicons.app.render.IconSource
import dev.bigicons.app.ui.*
import dev.bigicons.app.widget.WidgetUpdater
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BigIconsTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AppBackdrop(Modifier.matchParentSize())
                    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
                        MainContent(tick.intValue) { tick.intValue++ }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // пересчёт цветов при каждом открытии: акцент/обои/тема могли смениться без уведомления
        WidgetUpdater.updateAll(this)
        tick.intValue++
    }
}

@Composable
private fun MainContent(tick: Int, refresh: () -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val navItems = remember {
        listOf(NavItem("Виджеты", Icons.Default.Home), NavItem("Стиль", Icons.Default.Settings), NavItem("Инфо", Icons.Default.Info))
    }
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 96.dp

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .pointerInput(selectedTab) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f; dragTargetIndex = -1; dragProgress = 0f },
                        onDragCancel = { dragTargetIndex = -1; dragProgress = 0f },
                        onDragEnd = {
                            if (dragTargetIndex in navItems.indices && dragProgress >= 0.5f) selectedTab = dragTargetIndex
                            dragTargetIndex = -1
                            dragProgress = 0f
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        if (abs(totalDrag) < 12f) {
                            dragTargetIndex = -1; dragProgress = 0f
                            return@detectHorizontalDragGestures
                        }
                        val candidate = if (totalDrag < 0f) selectedTab + 1 else selectedTab - 1
                        if (candidate !in navItems.indices) {
                            dragTargetIndex = -1; dragProgress = 0f
                            return@detectHorizontalDragGestures
                        }
                        dragTargetIndex = candidate
                        dragProgress = (abs(totalDrag) / 180f).coerceIn(0f, 1f)
                    }
                },
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(225)) },
                modifier = Modifier.fillMaxSize().padding(bottom = navOverlayReserve),
                label = "tab_content",
            ) { page ->
                when (page) {
                    0 -> WidgetsTab(tick, refresh)
                    1 -> StyleTab()
                    else -> InfoScreen(16.dp)
                }
            }
            PillNavigationBar(
                navItems = navItems,
                selectedTab = selectedTab,
                dragTargetIndex = dragTargetIndex,
                dragProgress = dragProgress,
                onTabSelected = { selectedTab = it; dragTargetIndex = -1; dragProgress = 0f },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Вкладка «Виджеты» по структуре ScriptsTab из TgAuto: заголовок, карточки со статусом и действиями, добавление. */
@Composable
private fun WidgetsTab(tick: Int, refresh: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ConfigStore(ctx) }
    val mgr = remember { AppWidgetManager.getInstance(ctx) }
    val ids = remember(tick) { WidgetUpdater.allIds(ctx) }
    var sizeDialog by remember { mutableStateOf(false) }
    val canPin = mgr.isRequestPinAppWidgetSupported

    if (sizeDialog) {
        SizePickerDialog(
            sizes = WidgetUpdater.SIZES.map { it.first to it.second },
            onPick = { c, r ->
                sizeDialog = false
                mgr.requestPinAppWidget(ComponentName(ctx.packageName, WidgetUpdater.providerClass(c, r)), null, null)
            },
            onDismiss = { sizeDialog = false },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            TabHeader("Виджеты") {
                RefreshAction(ids.isNotEmpty()) { WidgetUpdater.updateAll(ctx); refresh() }
            }
        }
        if (canPin) item {
            AppSectionCard {
                SectionHeader(Icons.Default.Add, "Добавить виджет")
                Hint("Выберите размер, и лаунчер предложит поставить виджет BigIcons на рабочий стол.")
                BigButton("Добавить виджет", Icons.Default.Add, onClick = { sizeDialog = true })
            }
        }
        if (ids.isEmpty()) item {
            AppSectionCard {
                SectionHeader(Icons.Default.Home, "Виджетов пока нет")
                Hint(
                    if (canPin) "Нажмите «Добавить виджет» или добавьте виджет BigIcons с главного экрана лаунчера."
                    else "Добавьте виджет BigIcons с главного экрана лаунчера."
                )
            }
        }
        items(ids, key = { it }) { id ->
            val cfg = store.get(id) ?: store.defaults()
            val (w, h) = WidgetUpdater.sizeDp(ctx, mgr, id)
            val configured = cfg.pkg != null || cfg.customIcon != null
            AppSectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        IconSource.appLabel(ctx, cfg.pkg) ?: if (cfg.customIcon != null) "Своя иконка" else "Не настроен",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1,
                    )
                    if (configured) StatusPill("Настроен", AppColors.ok()) else StatusPill("Не настроен", AppColors.warn())
                }
                WidgetPreview(cfg, w, h, maxHeight = 120.dp, key = tick)
                Hint("Размер на экране: $w × $h dp")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Править", Icons.Default.Settings, modifier = Modifier.weight(1f), onClick = {
                        ctx.startActivity(Intent(ctx, ConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                    })
                    BigButton("Сбросить", Icons.Default.Refresh, modifier = Modifier.weight(1f), danger = true, onClick = {
                        store.put(id, store.defaults().copy(pkg = cfg.pkg))
                        WidgetUpdater.update(ctx, mgr, id)
                        refresh()
                    })
                }
            }
        }
    }
}

@Composable
private fun RefreshAction(enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick, enabled = enabled) {
        androidx.compose.material3.Icon(Icons.Default.Refresh, contentDescription = "Обновить все виджеты", tint = MaterialTheme.colorScheme.primary)
    }
}

/** Вкладка «Стиль» по структуре SettingsTab из TgAuto: секции-карточки с теми же контролами. */
@Composable
private fun StyleTab() {
    val ctx = LocalContext.current
    val store = remember { ConfigStore(ctx) }
    var cfg by remember { mutableStateOf(store.defaults()) }
    var applied by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TabHeader("Стиль")
        AppSectionCard {
            SectionHeader(Icons.Default.Settings, "Оформление приложения")
            FieldLabel("Тема")
            ChoiceRow(UiPrefs.modes, UiPrefs.themeMode, {
                when (it) { UiPrefs.MODE_LIGHT -> "Светлая"; UiPrefs.MODE_DARK -> "Тёмная"; else -> "Система" }
            }) { UiPrefs.setThemeMode(ctx, it) }
            val useDynamic = UiPrefs.dynamicColors && UiPrefs.dynamicSupported
            if (UiPrefs.dynamicSupported) {
                SectionDivider()
                SwitchRow("Material You", "Цвета берутся из обоев (Android 12+)", UiPrefs.dynamicColors) { UiPrefs.setDynamic(ctx, it) }
            }
            if (!useDynamic) {
                SectionDivider()
                FieldLabel("Палитра")
                ChoiceRow(Palettes.all.map { it.first }, UiPrefs.palette, { id -> Palettes.all.first { it.first == id }.second }) {
                    UiPrefs.setPalette(ctx, it)
                }
                Hint("Палитры «Цвет 1», «Эспрессо», «Цвет 2» есть в светлом и тёмном варианте. На виджеты-иконки это не влияет.")
            } else {
                Hint("Чтобы выбрать палитру, выключите Material You.")
            }
        }
        Hint("Стиль по умолчанию для новых виджетов.")
        WidgetPreview(cfg, 140, 140, maxHeight = 140.dp)
        StyleSections(cfg) { cfg = it; store.putDefaults(it); applied = false }
        AppSectionCard {
            SectionHeader(Icons.Default.Check, "Применить к существующим")
            Hint("Форма, цвет и размещение будут применены ко всем виджетам. Выбранные приложения и свои иконки сохранятся.")
            BigButton(if (applied) "Применено" else "Применить ко всем виджетам", Icons.Default.Check, onClick = {
                val mgr = AppWidgetManager.getInstance(ctx)
                WidgetUpdater.allIds(ctx).forEach { id ->
                    val old = store.get(id) ?: return@forEach
                    store.put(id, cfg.copy(pkg = old.pkg, customIcon = old.customIcon))
                    WidgetUpdater.update(ctx, mgr, id)
                }
                applied = true
            })
        }
        Spacer(Modifier.height(8.dp))
    }
}
