package dev.bigicons.app

import dev.bigicons.app.model.ThemeVariant
import dev.bigicons.app.render.ThemeTones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeTonesTest {
    @Test fun belowApi31HasNoSystemColors() {
        assertNull(ThemeTones.pick(30, true, ThemeVariant.NEUTRAL))
        assertNull(ThemeTones.pick(26, false, ThemeVariant.ACCENT))
    }

    @Test fun api31NeutralUsesPaletteTones() {
        val d = ThemeTones.pick(32, true, ThemeVariant.NEUTRAL)!!
        assertEquals("system_neutral1_900", d.bg); assertEquals("system_neutral1_50", d.glyph)
        val l = ThemeTones.pick(31, false, ThemeVariant.NEUTRAL)!!
        assertEquals("system_neutral1_100", l.bg); assertEquals("system_neutral1_900", l.glyph)
    }

    @Test fun api31AccentIs800_100DarkAnd100_700Light() {
        val d = ThemeTones.pick(33, true, ThemeVariant.ACCENT)!!
        assertEquals("system_accent1_800", d.bg); assertEquals("system_accent1_100", d.glyph)
        val l = ThemeTones.pick(33, false, ThemeVariant.ACCENT)!!
        assertEquals("system_accent1_100", l.bg); assertEquals("system_accent1_700", l.glyph)
    }

    @Test fun api34UsesRoles() {
        val n = ThemeTones.pick(34, true, ThemeVariant.NEUTRAL)!!
        assertEquals("system_surface_container_dark", n.bg)
        val a = ThemeTones.pick(36, false, ThemeVariant.ACCENT)!!
        assertEquals("system_primary_container_light", a.bg); assertEquals("system_on_primary_container_light", a.glyph)
    }
}
