package dev.bigicons.app.render

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import dev.bigicons.app.model.Shape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

object Shapes {
    private var sysPath: Path? = null
    private var sysTried = false
    private var sysClass: Shape? = null

    /** Системная маска адаптивных иконок (config_icon_mask, viewport 100x100), если доступна. */
    @Synchronized
    fun systemMask(): Path? {
        if (!sysTried) {
            sysTried = true
            sysPath = runCatching {
                val id = Resources.getSystem().getIdentifier("config_icon_mask", "string", "android")
                if (id == 0) null
                else PathParser.createPathFromPathData(Resources.getSystem().getString(id))
            }.getOrNull()
        }
        return sysPath
    }

    /** Ближайший пресет к системной маске (по доле заполнения квадрата). */
    @Synchronized
    fun classifySystem(): Shape {
        sysClass?.let { return it }
        val p = systemMask()
        val cls = if (p == null) Shape.SQUIRCLE else runCatching {
            val n = 64
            val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ALPHA_8)
            val m = Matrix().apply { setScale(n / 100f, n / 100f) }
            val q = Path(p).apply { transform(m) }
            Canvas(bmp).drawPath(q, Paint(Paint.ANTI_ALIAS_FLAG))
            val px = IntArray(n * n)
            val argb = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
            Canvas(argb).drawBitmap(bmp, 0f, 0f, Paint())
            argb.getPixels(px, 0, n, 0, 0, n, n)
            val ratio = px.count { (it ushr 24) > 127 }.toFloat() / (n * n)
            classifyByFill(ratio)
        }.getOrDefault(Shape.SQUIRCLE)
        sysClass = cls
        return cls
    }

    fun classifyByFill(ratio: Float): Shape = when {
        ratio < 0.86f -> Shape.CIRCLE
        ratio < 0.95f -> Shape.SQUIRCLE
        else -> Shape.ROUNDED
    }

    private fun squircle(): Path {
        val path = Path()
        val steps = 120
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2 * Math.PI)
            val c = cos(t); val s = sin(t)
            val x = 50 + 50 * Math.signum(c) * abs(c).pow(2 / 4.2)
            val y = 50 + 50 * Math.signum(s) * abs(s).pow(2 / 4.2)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
        return path
    }

    /** Контур в координатах 0..100. */
    fun unitPath(shape: Shape): Path = when (shape) {
        Shape.AUTO -> systemMask()?.let { Path(it) } ?: squircle()
        Shape.CIRCLE -> Path().apply { addCircle(50f, 50f, 50f, Path.Direction.CW) }
        Shape.SQUIRCLE -> squircle()
        Shape.ROUNDED -> Path().apply { addRoundRect(0f, 0f, 100f, 100f, 22f, 22f, Path.Direction.CW) }
        Shape.TEARDROP -> Path().apply {
            addRoundRect(
                RectF(0f, 0f, 100f, 100f),
                floatArrayOf(50f, 50f, 50f, 50f, 10f, 10f, 50f, 50f), Path.Direction.CW,
            )
        }
        Shape.SQUARE -> Path().apply { addRoundRect(0f, 0f, 100f, 100f, 5f, 5f, Path.Direction.CW) }
    }

    /** Маска в прямоугольнике `rect` (форма масштабируется под rect). */
    fun pathIn(shape: Shape, rect: RectF): Path {
        val p = unitPath(shape)
        val m = Matrix()
        m.setRectToRect(RectF(0f, 0f, 100f, 100f), rect, Matrix.ScaleToFit.FILL)
        p.transform(m)
        return p
    }

    /** Маска для растянутой иконки: скруглённый прямоугольник с радиусом «как у иконки». */
    fun stretchPath(shape: Shape, rect: RectF): Path {
        if (abs(rect.width() - rect.height()) < 1f) return pathIn(shape, rect)
        val eff = if (shape == Shape.AUTO) classifySystem() else shape
        val half = min(rect.width(), rect.height()) / 2f
        val radii: FloatArray = when (eff) {
            Shape.CIRCLE, Shape.AUTO -> FloatArray(8) { half }
            Shape.SQUIRCLE -> FloatArray(8) { half * 0.72f }
            Shape.ROUNDED -> FloatArray(8) { half * 0.44f }
            Shape.TEARDROP -> floatArrayOf(half, half, half, half, half * 0.2f, half * 0.2f, half, half)
            Shape.SQUARE -> FloatArray(8) { half * 0.1f }
        }
        return Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
    }
}
