package dev.bigicons.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Загрузка системы / обновление BigIcons: перерисовать все виджеты. */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdater.updateAll(context)
    }
}

/** Установка/обновление/удаление пакета: перерисовать связанные виджеты (или показать заглушку). */
class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        WidgetUpdater.updateForPackage(context, pkg)
    }
}
