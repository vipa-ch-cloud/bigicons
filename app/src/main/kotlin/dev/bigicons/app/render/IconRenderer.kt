package dev.bigicons.app.render

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import dev.bigicons.app.model.ColorMode
import dev.bigicons.app.model.Shape
import dev.bigicons.app.model.StretchMode
import dev.bigicons.app.model.ThemeVariant
import dev.bigicons.app.model.WidgetConfig
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object IconRenderer {
    /** Размер холста слоя: 108dp-слой адаптивной иконки, 4px на dp. Видимая часть — центральные 288px. */
    private const val G = 432
    private const val VISIBLE = 288f
    private const val MAX_PX = 1_000_000L

    private class Palette(val bg: Int, val glyph: Int)

    private class Prepared(
        val bgColor: Int?,
        val bgDrawable: Drawable?,
        val glyph: Bitmap?,
        val tint: Int?,
        val cover: Bitmap?,
    )

    fun isNight(ctx: Context) =
        (ctx.applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** AUTO: тематическая, если Android 13+ и у приложения есть monochrome-слой. */
    fun resolveMode(cfg: WidgetConfig, src: IconSource): ColorMode = when (cfg.colorMode) {
        ColorMode.AUTO ->
            if (Build.VERSION.SDK_INT >= 33 && src.mono != null) ColorMode.THEMED else ColorMode.ORIGINAL
        else -> cfg.colorMode
    }

    private fun palette(ctx: Context, mode: ColorMode, custom: Int, night: Boolean, variant: ThemeVariant): Palette? = when (mode) {
        ColorMode.ORIGINAL, ColorMode.AUTO -> null
        ColorMode.CUSTOM -> Palette(
            custom,
            if (ColorUtils.calculateLuminance(custom) > 0.5) 0xFF1B1B1F.toInt() else Color.WHITE,
        )
        ColorMode.THEMED -> themed(ctx, night, variant)
    }

    /** Тематическая палитра Material You: тона выбирает [ThemeTones.pick], значения берутся у системы в момент рендера. */
    private fun themed(ctx: Context, night: Boolean, variant: ThemeVariant): Palette {
        val names = ThemeTones.pick(Build.VERSION.SDK_INT, night, variant) ?: return fallbackTheme(night)
        return runCatching {
            val res = ctx.applicationContext.resources
            fun c(n: String): Int {
                val id = res.getIdentifier(n, "color", "android")
                require(id != 0)
                return res.getColor(id, ctx.applicationContext.theme)
            }
            Palette(c(names.bg), c(names.glyph))
        }.getOrElse { fallbackTheme(night) }
    }

    private fun fallbackTheme(night: Boolean) =
        if (night) Palette(0xFF4A4458.toInt(), 0xFFE8DEF8.toInt()) else Palette(0xFFE8DEF8.toInt(), 0xFF4A4458.toInt())

    private fun layerBitmap(d: Drawable, size: Float = G.toFloat()): Bitmap {
        val bmp = Bitmap.createBitmap(G, G, Bitmap.Config.ARGB_8888)
        val off = ((G - size) / 2).toInt()
        d.setBounds(off, off, off + size.toInt(), off + size.toInt())
        d.draw(Canvas(bmp))
        return bmp
    }

    private fun imageBitmap(src: Bitmap): Bitmap {
        val bmp = Bitmap.createBitmap(G, G, Bitmap.Config.ARGB_8888)
        val side = VISIBLE * 0.9f
        val k = min(side / src.width, side / src.height)
        val w = src.width * k; val h = src.height * k
        Canvas(bmp).drawBitmap(
            src, null, RectF((G - w) / 2, (G - h) / 2, (G + w) / 2, (G + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG),
        )
        return bmp
    }

    private fun toSilhouette(bmp: Bitmap): Bitmap {
        val px = IntArray(G * G)
        bmp.getPixels(px, 0, G, 0, 0, G, G)
        val a = Monochrome.silhouette(px, G, G)
        for (i in a.indices) a[i] = a[i] shl 24
        return Bitmap.createBitmap(a, G, G, Bitmap.Config.ARGB_8888)
    }

    private fun prepare(src: IconSource, mode: ColorMode, pal: Palette?, night: Boolean): Prepared {
        val tinted = pal != null
        if (src.custom != null) {
            return if (!tinted) Prepared(null, null, null, null, src.custom)
            else Prepared(pal!!.bg, null, toSilhouette(imageBitmap(src.custom)), pal.glyph, null)
        }
        if (src.isAdaptive) {
            if (!tinted) return Prepared(null, src.adaptiveBg, layerBitmap(src.adaptiveFg!!), null, null)
            val glyph = if (src.mono != null) layerBitmap(src.mono) else toSilhouette(layerBitmap(src.adaptiveFg!!))
            return Prepared(pal!!.bg, null, glyph, pal.glyph, null)
        }
        // старая (не адаптивная) иконка
        val bmp = layerBitmap(src.legacy!!, VISIBLE * 0.8f)
        return if (!tinted) {
            Prepared(if (night) 0xFF2B2930.toInt() else Color.WHITE, null, bmp, null, null)
        } else Prepared(pal!!.bg, null, toSilhouette(bmp), pal.glyph, null)
    }

    fun render(ctx: Context, cfg: WidgetConfig, wIn: Int, hIn: Int, density: Float): Bitmap {
        var w = max(wIn, 24); var h = max(hIn, 24); var d = density
        val area = w.toLong() * h
        if (area > MAX_PX) {
            val f = sqrt(MAX_PX.toDouble() / area).toFloat()
            w = max(24, (w * f).toInt()); h = max(24, (h * f).toInt()); d *= f
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val night = isNight(ctx)
        val src = IconSource.load(ctx, cfg)
        if (src == null) {
            drawPlaceholder(ctx, c, cfg, w.toFloat(), h.toFloat(), night)
            return bmp
        }
        val mode = resolveMode(cfg, src)
        val pal = palette(ctx, mode, cfg.customColor, night, cfg.themeVariant)
        val prep = prepare(src, mode, pal, night)

        val (cols, rows) = if (cfg.stretch == StretchMode.TILES) {
            Layout.cells(w / d) to Layout.cells(h / d)
        } else 1 to 1
        val cw = w.toFloat() / cols; val ch = h.toFloat() / rows
        val labelH = if (cfg.showLabel && ch > 44 * d) 16 * d else 0f
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11 * d
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            setShadowLayer(3 * d, 0f, 1 * d, 0x99000000.toInt())
        }
        for (r in 0 until rows) for (col in 0 until cols) {
            val cell = RectF(col * cw, r * ch, (col + 1) * cw, (r + 1) * ch)
            val area2 = RectF(cell.left, cell.top, cell.right, cell.bottom - labelH)
            val pad = cfg.padding / 100f * min(area2.width(), area2.height()) + 1.5f * d
            area2.inset(pad, pad)
            if (area2.width() < 4 || area2.height() < 4) continue
            val tile = if (cfg.stretch == StretchMode.STRETCH) area2 else {
                val s = min(area2.width(), area2.height())
                RectF(area2.centerX() - s / 2, area2.centerY() - s / 2, area2.centerX() + s / 2, area2.centerY() + s / 2)
            }
            drawTile(c, prep, cfg, tile)
            if (labelH > 0) {
                val t = TextUtils.ellipsize(src.label, text, cw - 4 * d, TextUtils.TruncateAt.END).toString()
                c.drawText(t, cell.centerX(), cell.bottom - 3.5f * d, text)
            }
        }
        return bmp
    }

    private fun drawTile(c: Canvas, p: Prepared, cfg: WidgetConfig, rect: RectF) {
        val path = if (cfg.stretch == StretchMode.STRETCH) Shapes.stretchPath(cfg.shape, rect)
        else Shapes.pathIn(cfg.shape, rect)
        val layer = c.saveLayer(rect, null)
        c.clipRect(rect)
        val scale = cfg.scale / 100f
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (p.cover != null) {
            val cover = max(rect.width() / p.cover.width, rect.height() / p.cover.height) * scale
            val w = p.cover.width * cover; val h = p.cover.height * cover
            c.drawBitmap(
                p.cover, null,
                RectF(rect.centerX() - w / 2, rect.centerY() - h / 2, rect.centerX() + w / 2, rect.centerY() + h / 2),
                paint,
            )
        } else {
            p.bgColor?.let { c.drawColor(it) }
            p.bgDrawable?.let {
                val side = max(rect.width(), rect.height()) * G / VISIBLE
                it.setBounds(
                    (rect.centerX() - side / 2).toInt(), (rect.centerY() - side / 2).toInt(),
                    (rect.centerX() + side / 2).toInt(), (rect.centerY() + side / 2).toInt(),
                )
                it.draw(c)
            }
            p.glyph?.let { g ->
                val side = min(rect.width(), rect.height()) * G / VISIBLE * scale
                if (p.tint != null) paint.colorFilter = PorterDuffColorFilter(p.tint, PorterDuff.Mode.SRC_IN)
                c.drawBitmap(
                    g, null,
                    RectF(rect.centerX() - side / 2, rect.centerY() - side / 2, rect.centerX() + side / 2, rect.centerY() + side / 2),
                    paint,
                )
            }
        }
        // маска формы: стираем всё вне контура (с антиалиасингом)
        val outside = android.graphics.Path(path).apply { fillType = android.graphics.Path.FillType.INVERSE_WINDING }
        val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        c.drawPath(outside, clear)
        c.restoreToCount(layer)
    }

    private fun drawPlaceholder(ctx: Context, c: Canvas, cfg: WidgetConfig, w: Float, h: Float, night: Boolean) {
        val pal = themed(ctx, night, cfg.themeVariant)
        val s = min(w, h)
        val rect = if (cfg.stretch == StretchMode.STRETCH) RectF(w * 0.03f, h * 0.03f, w * 0.97f, h * 0.97f)
        else RectF((w - s) / 2 + s * 0.03f, (h - s) / 2 + s * 0.03f, (w + s) / 2 - s * 0.03f, (h + s) / 2 - s * 0.03f)
        val path = if (cfg.stretch == StretchMode.STRETCH) Shapes.stretchPath(cfg.shape, rect) else Shapes.pathIn(cfg.shape, rect)
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.bg })
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pal.glyph; style = Paint.Style.STROKE; strokeWidth = s * 0.07f; strokeCap = Paint.Cap.ROUND
        }
        val k = s * 0.14f
        c.drawLine(rect.centerX() - k, rect.centerY(), rect.centerX() + k, rect.centerY(), p)
        c.drawLine(rect.centerX(), rect.centerY() - k, rect.centerX(), rect.centerY() + k, p)
    }
}
