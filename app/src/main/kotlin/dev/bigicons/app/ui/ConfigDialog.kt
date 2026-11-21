package dev.bigicons.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * Каркас экрана настройки, по образцу ConfigDialog из TgAuto: фон-«шары», заголовок с кнопкой закрытия,
 * прокручиваемое содержимое и ряд кнопок действий внизу.
 */
@Composable
fun ConfigScaffold(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AppBackdrop()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)
        ) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Закрыть") }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
        }
    }
}

/** Полноэкранный диалог на фоне темы (как ConfigDialog в TgAuto). */
@Composable
fun FullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}

/** Выбор размера виджета: те же кнопки выбора, что и в остальном интерфейсе. */
@Composable
fun SizePickerDialog(sizes: List<Pair<Int, Int>>, onPick: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    FullScreenDialog(onDismiss) {
        ConfigScaffold(
            title = "Размер виджета", subtitle = "Ширина × высота в ячейках лаунчера",
            onClose = onDismiss,
            actions = { BigButton("Отмена", Icons.Default.Close, onDismiss, Modifier.weight(1f)) },
        ) {
            AppSectionCard {
                ChoiceRow(sizes, null, { "${it.first} × ${it.second}" }, perRow = 3) { onPick(it.first, it.second) }
            }
        }
    }
}

/** Выбор приложения: поиск и список, оформленные карточкой. */
@Composable
fun AppPickerDialog(apps: List<AppEntry>?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    FullScreenDialog(onDismiss) {
        ConfigScaffold(
            title = "Выбор приложения", subtitle = null, onClose = onDismiss,
            actions = { BigButton("Назад", Icons.Default.Close, onDismiss, Modifier.weight(1f)) },
        ) {
            AppTextField(query, { query = it }, "Поиск", leadingIcon = Icons.Default.Search)
            val list = apps?.filter { it.label.contains(query, true) || it.pkg.contains(query, true) }
            if (list == null) Hint("Загрузка…")
            else AppSectionCard(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (list.isEmpty()) Hint("Ничего не найдено")
                list.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(a.pkg) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(a.icon, null, Modifier.size(44.dp))
                        Column {
                            Text(a.label, style = MaterialTheme.typography.bodyLarge)
                            Hint(a.pkg)
                        }
                    }
                }
            }
        }
    }
}
