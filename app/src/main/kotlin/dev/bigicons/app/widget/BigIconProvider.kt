package dev.bigicons.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import dev.bigicons.app.model.ConfigStore

/** Базовый провайдер: подклассы Widget<колонки>x<строки> отличаются только XML-размерами. */
abstract class BigIconProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { WidgetUpdater.update(context, mgr, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        WidgetUpdater.update(context, mgr, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val store = ConfigStore(context)
        ids.forEach { store.delete(it) }
    }
}
