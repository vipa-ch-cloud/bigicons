package dev.bigicons.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * ВСЕ внешние ссылки приложения. Чтобы подставить настоящие адреса, правьте только этот объект.
 * Пустая строка = «ссылки пока нет»: кнопка будет неактивной или со значком «скоро».
 */
object AppLinks {
    /** Основной репозиторий BigIcons. */
    const val REPO = "https://github.com/vipa-ch-cloud/bigicons"
    const val REPO_PUBLISHED = true

    /** Источник стиля интерфейса (GPLv3). */
    const val TG_WS_PROXY_ANDROID = "https://github.com/amurcanov/tg-ws-proxy-android"

    /** Другие проекты автора. */
    const val TGAUTO = "https://github.com/vipa-ch-cloud/TgAuto"
    const val ASCIIWALL = "https://github.com/vipa-ch-cloud/AsciiWall"

    /** Поддержка автора. Реальные ссылки на платёжные сервисы сюда подставляет автор. */
    const val DONATE_ALERTS = "" // TODO: ссылка DonationAlerts
    const val BOOSTY = ""        // TODO: ссылка Boosty

    const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
}

/** Открывает ссылку; при пустой ссылке или отсутствии браузера показывает сообщение вместо падения. */
fun openUrl(ctx: Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(ctx, "Ссылка пока не задана", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "Не найден браузер для открытия ссылки", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(ctx, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
    }
}
