package dev.bigicons.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.RemoteViews
import dev.bigicons.app.ConfigActivity
import dev.bigicons.app.MainActivity
import dev.bigicons.app.R
import dev.bigicons.app.model.ConfigStore
import dev.bigicons.app.render.IconRenderer
import dev.bigicons.app.render.IconSource
import dev.bigicons.app.render.Layout

object WidgetUpdater {
    val SIZES = listOf(
        1 to 1, 1 to 2, 2 to 1, 2 to 2, 2 to 3, 3 to 2, 3 to 3, 4 to 1, 1 to 4, 4 to 2, 4 to 3, 4 to 4,
    )

    fun providerClass(cols: Int, rows: Int) = "dev.bigicons.app.widget.Widget${cols}x$rows"

    fun allProviders(ctx: Context) =
        SIZES.map { (c, r) -> ComponentName(ctx.packageName, providerClass(c, r)) }

    fun allIds(ctx: Context): List<Int> {
        val mgr = AppWidgetManager.getInstance(ctx)
        return allProviders(ctx).flatMap { mgr.getAppWidgetIds(it).toList() }
    }

    /** Размер виджета в dp: из опций, иначе из имени провайдера. */
    fun sizeDp(ctx: Context, mgr: AppWidgetManager, id: Int): Pair<Int, Int> {
        val o = mgr.getAppWidgetOptions(id)
        val land = ctx.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        var w = if (land) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        var h = if (land) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (w <= 0 || h <= 0) {
            val (c, r) = Layout.parseProvider(mgr.getAppWidgetInfo(id)?.provider?.className) ?: (1 to 1)
            w = Layout.sizeDp(c); h = Layout.sizeDp(r)
        }
        return w to h
    }

    fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val store = ConfigStore(ctx)
        val cfg = store.get(id) ?: store.defaults()
        val density = ctx.resources.displayMetrics.density
        val (wDp, hDp) = sizeDp(ctx, mgr, id)
        var factor = 1f
        var views: RemoteViews? = null
        repeat(3) {
            if (views != null) return@repeat
            val bmp = IconRenderer.render(ctx, cfg, (wDp * density * factor).toInt(), (hDp * density * factor).toInt(), density * factor)
            val rv = RemoteViews(ctx.packageName, R.layout.widget_icon)
            rv.setImageViewBitmap(R.id.img, bmp)
            rv.setContentDescription(R.id.img, IconSource.appLabel(ctx, cfg.pkg) ?: "BigIcons")
            rv.setOnClickPendingIntent(R.id.root, clickIntent(ctx, id, cfg.pkg))
            try {
                mgr.updateAppWidget(id, rv)
                views = rv
            } catch (e: RuntimeException) { // слишком большой bitmap для RemoteViews — уменьшаем
                factor *= 0.6f
            }
        }
    }

    private fun clickIntent(ctx: Context, id: Int, pkg: String?): PendingIntent {
        val launch = pkg?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
        val intent = if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } else if (pkg == null) {
            Intent(ctx, ConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .setData(Uri.parse("bigicons://widget/$id"))
        } else {
            Intent(ctx, MainActivity::class.java) // приложение удалено — открываем BigIcons
        }
        return PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        allIds(ctx).forEach { runCatching { update(ctx, mgr, it) } }
    }

    fun updateForPackage(ctx: Context, pkg: String) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val store = ConfigStore(ctx)
        allIds(ctx).filter { store.get(it)?.pkg == pkg }.forEach { runCatching { update(ctx, mgr, it) } }
    }
}
