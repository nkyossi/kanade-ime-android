package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class KeyboardThemeTest {
    @Test
    fun lightKeyTextHasEnoughContrast() {
        assertTrue(contrast(KeyboardTheme.light.key, KeyboardTheme.light.keyText) >= 4.5)
        assertTrue(contrast(KeyboardTheme.light.specialKey, KeyboardTheme.light.keyText) >= 4.5)
    }

    @Test
    fun darkKeyTextHasEnoughContrast() {
        assertTrue(contrast(KeyboardTheme.dark.key, KeyboardTheme.dark.keyText) >= 4.5)
        assertTrue(contrast(KeyboardTheme.dark.specialKey, KeyboardTheme.dark.keyText) >= 4.5)
    }

    @Test
    fun rowHeightIsClampedToRange() {
        assertEquals(ROW_HEIGHT_DP_MIN.toDouble(), clampRowHeightDp(0f).toDouble(), 0.0)
        assertEquals(ROW_HEIGHT_DP_MIN.toDouble(), clampRowHeightDp(ROW_HEIGHT_DP_MIN).toDouble(), 0.0)
        assertEquals(52.0, clampRowHeightDp(52f).toDouble(), 0.0)
        assertEquals(ROW_HEIGHT_DP_MAX.toDouble(), clampRowHeightDp(ROW_HEIGHT_DP_MAX).toDouble(), 0.0)
        assertEquals(ROW_HEIGHT_DP_MAX.toDouble(), clampRowHeightDp(200f).toDouble(), 0.0)
    }

    /** The WCAG relative luminance of a 0xAARRGGBB color. */
    private fun luminance(color: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel((color shr 16) and 0xFF) +
            0.7152 * channel((color shr 8) and 0xFF) +
            0.0722 * channel(color and 0xFF)
    }

    /** The WCAG contrast ratio between two opaque colors. */
    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
