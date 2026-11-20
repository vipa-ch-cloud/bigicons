package dev.bigicons.app

import dev.bigicons.app.render.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutTest {
    @Test fun cellsRoundTrip() {
        for (n in 1..6) assertEquals(n, Layout.cells(Layout.sizeDp(n).toFloat()))
    }
    @Test fun cellsAtLeastOne() = assertEquals(1, Layout.cells(0f))
    @Test fun parseProvider() {
        assertEquals(2 to 3, Layout.parseProvider("dev.bigicons.app.widget.Widget2x3"))
        assertNull(Layout.parseProvider("dev.bigicons.app.Other"))
        assertNull(Layout.parseProvider(null))
    }
}
