package dev.bigicons.app.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun versionName(ctx: Context): String = runCatching {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
}.getOrNull() ?: "?"

@Composable
private fun Muted(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun LinkRow(title: String, subtitle: String, url: String) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { openUrl(ctx, url) }.padding(vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Muted(if (url.isBlank()) "$subtitle (ссылка скоро)" else subtitle)
    }
}

@Composable
private fun PeopleList(list: List<Person>, emptyText: String) {
    val ctx = LocalContext.current
    if (list.isEmpty()) { Muted(emptyText); return }
    list.forEach { p ->
        val mod = if (p.url.isBlank()) Modifier else Modifier.clip(RoundedCornerShape(12.dp)).clickable { openUrl(ctx, p.url) }
        Column(mod.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall,
                color = if (p.url.isBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
            Muted(p.role)
        }
    }
}

@Composable
fun InfoScreen(bottomReserve: Dp) {
    val ctx = LocalContext.current
    val version = remember { versionName(ctx) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomReserve + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppCard {
            Text("BigIcons", style = MaterialTheme.typography.titleLarge)
            Muted("Версия $version")
            Text("Виджеты-иконки: крупные иконки приложений любого размера с формой, цветом и растяжением на выбор, включая цвета Material You.")
            Text("Как пользоваться", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text("1. Нажмите «Добавить виджет» на вкладке «Виджеты» или добавьте виджет BigIcons с рабочего стола.\n" +
                "2. Выберите приложение и стиль.\n" +
                "3. Размер виджета можно менять: иконка перерисуется.\n" +
                "4. Нажатие по виджету запускает приложение.", style = MaterialTheme.typography.bodyMedium)
            Muted("Лаунчер не сообщает форму своих иконок, поэтому «Как в системе» берёт системную маску. Итоговый размер виджета определяет лаунчер.")
            HorizontalDivider()
            LinkRow("Репозиторий проекта", AppLinks.REPO + if (AppLinks.REPO_PUBLISHED) "" else " (запланирован, пока не опубликован)", AppLinks.REPO)
        }

        AppCard {
            Text("Поддержать автора", style = MaterialTheme.typography.titleMedium)
            Muted("Скоро: донат-алерт / Boosty. Приложение бесплатное, поддержка добровольная.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button({ openUrl(ctx, AppLinks.DONATE_ALERTS) }, enabled = AppLinks.DONATE_ALERTS.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Text(if (AppLinks.DONATE_ALERTS.isBlank()) "Донат (скоро)" else "Донат", maxLines = 1)
                }
                OutlinedButton({ openUrl(ctx, AppLinks.BOOSTY) }, enabled = AppLinks.BOOSTY.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Text(if (AppLinks.BOOSTY.isBlank()) "Boosty (скоро)" else "Boosty", maxLines = 1)
                }
            }
        }

        AppCard {
            Text("Участники", style = MaterialTheme.typography.titleMedium)
            Text("Разработчики", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            PeopleList(People.developers, "")
            HorizontalDivider()
            Text("Поддержавшие", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            PeopleList(People.supporters, "Здесь будут те, кто поддержал проект")
        }

        AppCard {
            Text("Исходники и благодарности", style = MaterialTheme.typography.titleMedium)
            LinkRow("TgWsProxyAndroid (amurcanov)", "Источник стиля интерфейса, лицензия GPL-3.0", AppLinks.TG_WS_PROXY_ANDROID)
            Muted("Оформление (лавандовая палитра, карточки, нижняя навигация-пилюля) повторяет TgWsProxyAndroid и TgAuto.")
            Text("Библиотеки", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Muted("Jetpack Compose, Material 3, AndroidX Core / Activity / Lifecycle (Apache License 2.0).")
            LinkRow("Лицензия BigIcons", "GNU GPL версии 3 (файл LICENSE в репозитории)", AppLinks.LICENSE_URL)
        }

        AppCard {
            Text("Другие проекты", style = MaterialTheme.typography.titleMedium)
            LinkRow("TgAuto", "Автоматизация Telegram-скриптами на Android", AppLinks.TGAUTO)
            LinkRow("LMLAPK", "Проект автора", AppLinks.LMLAPK)
        }
    }
}
