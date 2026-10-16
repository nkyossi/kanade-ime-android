package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test
import works.firstfruits.hiragana.ime.Keysym

class FlickResolverTest {
    private val t = 20f

    @Test
    fun shortMoveIsTap() {
        assertEquals(FlickDirection.CENTER, FlickResolver.resolve(0f, 0f, t))
        assertEquals(FlickDirection.CENTER, FlickResolver.resolve(19f, -19f, t))
    }

    @Test
    fun directions() {
        assertEquals(FlickDirection.LEFT, FlickResolver.resolve(-30f, 5f, t))
        assertEquals(FlickDirection.RIGHT, FlickResolver.resolve(30f, -5f, t))
        assertEquals(FlickDirection.UP, FlickResolver.resolve(5f, -30f, t))
        assertEquals(FlickDirection.DOWN, FlickResolver.resolve(-5f, 30f, t))
    }

    @Test
    fun flickKeysHaveFiveDirections() {
        for (row in FlickLayout.layout.rows) for (key in row.keys) {
            val action = key.action
            if (action is KeyAction.Kana) {
                assertEquals(key.label, 5, action.flicks.size)
                assertEquals(key.label, action.kanaFor(FlickDirection.CENTER))
            }
        }
        val ka = FlickLayout.layout.rows[0].keys[2].action as KeyAction.Kana
        assertEquals("き", ka.kanaFor(FlickDirection.LEFT))
        assertEquals("こ", ka.kanaFor(FlickDirection.DOWN))
    }

    @Test
    fun keycodesOfUsKeyboard() {
        assertEquals(16, Keysym.keycodeOf('q'))
        assertEquals(30, Keysym.keycodeOf('A'))
        assertEquals(50, Keysym.keycodeOf('m'))
        assertEquals(12, Keysym.keycodeOf('-'))
        assertEquals(51, Keysym.keycodeOf(','))
    }
}
