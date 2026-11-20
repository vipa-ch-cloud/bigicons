package dev.bigicons.app.render

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import dev.bigicons.app.model.ConfigStore
import dev.bigicons.app.model.WidgetConfig
import java.io.File

/** Исходные слои иконки. */
class IconSource(
    val label: String,
    /** пользовательское изображение */
    val custom: Bitmap? = null,
    val adaptiveBg: Drawable? = null,
    val adaptiveFg: Drawable? = null,
    val mono: Drawable? = null,
    val legacy: Drawable? = null,
) {
    val isAdaptive get() = adaptiveFg != null

    companion object {
        fun customFile(ctx: Context, cfg: WidgetConfig): File? =
            cfg.customIcon?.let { File(File(ctx.filesDir, "icons"), it) }?.takeIf { it.exists() }

        fun appLabel(ctx: Context, pkg: String?): String? = pkg?.let {
            runCatching {
                val pm = ctx.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString()
            }.getOrNull()
        }

        /** null — нечего рисовать (приложение не выбрано/удалено и нет своей иконки). */
        fun load(ctx: Context, cfg: WidgetConfig): IconSource? {
            val label = appLabel(ctx, cfg.pkg)
            customFile(ctx, cfg)?.let { f ->
                val bmp = BitmapFactory.decodeFile(f.path)
                if (bmp != null) return IconSource(label ?: "Иконка", custom = bmp)
            }
            val pkg = cfg.pkg ?: return null
            val pm = ctx.packageManager
            val d: Drawable = try {
                val comp = pm.getLaunchIntentForPackage(pkg)?.component
                if (comp != null) pm.getActivityIcon(comp) else pm.getApplicationIcon(pkg)
            } catch (e: PackageManager.NameNotFoundException) {
                return null
            }
            // слои системных drawable разделяются, поэтому работаем с копией
            val nd = d.constantState?.newDrawable()?.mutate() ?: d
            return if (nd is AdaptiveIconDrawable) {
                val mono = if (Build.VERSION.SDK_INT >= 33) nd.monochrome else null
                IconSource(label ?: pkg, adaptiveBg = nd.background, adaptiveFg = nd.foreground, mono = mono)
            } else {
                IconSource(label ?: pkg, legacy = nd)
            }
        }
    }
}
