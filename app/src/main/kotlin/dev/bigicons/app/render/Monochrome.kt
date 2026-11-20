package dev.bigicons.app.render

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Fallback «одноцветной» обработки для иконок без monochrome-слоя.
 * Возвращает альфа-маску (0..255) силуэта глифа.
 *
 *  - полупрозрачное изображение (глиф на прозрачном фоне): маска = альфа;
 *  - сплошная картинка (фон + глиф): маска = цветовое расстояние до фона
 *    (медианный цвет рамки области содержимого), сглаженное порогами.
 */
object Monochrome {
    fun silhouette(px: IntArray, w: Int, h: Int): IntArray {
        val out = IntArray(w * h)
        var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
        for (y in 0 until h) for (x in 0 until w) {
            if ((px[y * w + x] ushr 24) > 16) {
                if (x < x0) x0 = x; if (x > x1) x1 = x
                if (y < y0) y0 = y; if (y > y1) y1 = y
            }
        }
        if (x1 < 0) return out
        val bw = x1 - x0 + 1; val bh = y1 - y0 + 1
        var opaque = 0
        for (y in y0..y1) for (x in x0..x1) if ((px[y * w + x] ushr 24) > 200) opaque++
        val coverage = opaque.toFloat() / (bw * bh)

        fun alphaMask() {
            for (i in px.indices) out[i] = px[i] ushr 24
        }
        if (coverage < 0.8f) { alphaMask(); return out }

        // сплошной случай: цвет фона = медиана непрозрачных пикселей рамки bbox
        val rs = ArrayList<Int>(); val gs = ArrayList<Int>(); val bs = ArrayList<Int>()
        fun sample(x: Int, y: Int) {
            val p = px[y * w + x]
            if ((p ushr 24) > 200) {
                rs += (p shr 16) and 0xFF; gs += (p shr 8) and 0xFF; bs += p and 0xFF
            }
        }
        for (x in x0..x1) { sample(x, y0); sample(x, y1) }
        for (y in y0..y1) { sample(x0, y); sample(x1, y) }
        if (rs.isEmpty()) { alphaMask(); return out }
        val br = rs.sorted()[rs.size / 2]; val bg = gs.sorted()[gs.size / 2]; val bb = bs.sorted()[bs.size / 2]

        var strong = 0
        for (y in y0..y1) for (x in x0..x1) {
            val p = px[y * w + x]
            val a = p ushr 24
            val dr = ((p shr 16) and 0xFF) - br
            val dg = ((p shr 8) and 0xFF) - bg
            val db = (p and 0xFF) - bb
            val dist = sqrt((dr * dr + dg * dg + db * db) / 3f)
            val t = min(1f, max(0f, (dist - 28f) / 62f))
            val s = t * t * (3 - 2 * t)
            val v = (s * a).toInt()
            out[y * w + x] = v
            if (v > 128) strong++
        }
        // всё одного цвета (сплошная фигура) — используем альфу
        if (strong < 0.02f * opaque) alphaMask()
        return out
    }
}
