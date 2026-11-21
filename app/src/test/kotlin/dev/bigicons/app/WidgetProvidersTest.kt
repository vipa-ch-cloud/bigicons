package dev.bigicons.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет все 12 provider-XML: размеры по формуле AOSP 70n-30, имя размера, resize и превью. */
class WidgetProvidersTest {
    private val res = listOf("src/main/res", "app/src/main/res").map(::File).first { it.isDirectory }
    private val sizes = listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2, 2 to 3, 3 to 2, 3 to 3, 4 to 1, 1 to 4, 4 to 2, 4 to 3, 4 to 4)

    private fun attrs(f: File): Map<String, String> =
        Regex("""android:(\w+)="([^"]*)"""").findAll(f.readText()).associate { it.groupValues[1] to it.groupValues[2] }

    private fun dp(v: String?) = v!!.removeSuffix("dp").toInt()

    @Test fun twelveProviders() {
        assertEquals(12, sizes.toSet().size)
        assertEquals(12, File(res, "xml").listFiles { f -> f.name.startsWith("widget_") }!!.size)
    }

    @Test fun everyProviderMatchesFormulaAndName() {
        for ((c, r) in sizes) {
            val a = attrs(File(res, "xml/widget_${c}x$r.xml"))
            val tag = "${c}x$r"
            assertEquals(tag, 70 * c - 30, dp(a["minWidth"]))
            assertEquals(tag, 70 * r - 30, dp(a["minHeight"]))
            assertEquals(tag, c, a["targetCellWidth"]!!.toInt())
            assertEquals(tag, r, a["targetCellHeight"]!!.toInt())
            // minResize не меньше одной ячейки и не больше самого размера
            assertTrue(tag, dp(a["minResizeWidth"]) in 40..dp(a["minWidth"]))
            assertTrue(tag, dp(a["minResizeHeight"]) in 40..dp(a["minHeight"]))
            // maxResize не режет заявленный размер
            assertTrue(tag, dp(a["maxResizeWidth"]) >= dp(a["minWidth"]))
            assertTrue(tag, dp(a["maxResizeHeight"]) >= dp(a["minHeight"]))
            assertEquals("@drawable/widget_preview_$tag", a["previewImage"])
            assertEquals("@layout/widget_preview_$tag", a["previewLayout"])
            assertTrue(tag, File(res, "drawable/widget_preview_$tag.xml").isFile)
            assertTrue(tag, File(res, "layout/widget_preview_$tag.xml").isFile)
        }
    }

    @Test fun previewAspectMatchesSize() {
        for ((c, r) in sizes) {
            val a = attrs(File(res, "drawable/widget_preview_${c}x$r.xml"))
            assertEquals(70 * c - 30, dp(a["width"]))
            assertEquals(70 * r - 30, dp(a["height"]))
        }
    }
}
