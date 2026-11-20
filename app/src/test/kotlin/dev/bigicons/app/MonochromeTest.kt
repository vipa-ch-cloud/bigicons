package dev.bigicons.app

import dev.bigicons.app.render.Monochrome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonochromeTest {
    @Test fun transparentIsEmpty() {
        val out = Monochrome.silhouette(IntArray(16), 4, 4)
        assertTrue(out.all { it == 0 })
    }

    @Test fun alphaShapeKeepsAlpha() {
        val px = IntArray(100)
        px[55] = 0xFF112233.toInt()
        val out = Monochrome.silhouette(px, 10, 10)
        assertEquals(255, out[55])
        assertEquals(0, out[0])
    }

    @Test fun solidBackgroundIsRemoved() {
        val w = 20
        val px = IntArray(w * w) { 0xFFFFFFFF.toInt() }
        for (y in 6..13) for (x in 6..13) px[y * w + x] = 0xFF000000.toInt()
        val out = Monochrome.silhouette(px, w, w)
        assertEquals(0, out[0])
        assertTrue(out[10 * w + 10] > 200)
    }
}
