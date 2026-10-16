package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlickLayoutTest {
    private fun key(label: String, layout: KeyboardLayout = FlickLayout.layout) =
        layout.rows.flatMap { it.keys }.first { it.label == label }

    private fun flick(label: String, layout: KeyboardLayout = FlickLayout.layout) =
        key(label, layout).action as KeyAction.Flick

    @Test
    fun everyRowHasFiveKeys() {
        for (row in FlickLayout.layout.rows) assertEquals(5, row.keys.size)
    }

    @Test
    fun topLeftKeyPastes() {
        assertEquals(KeyAction.Paste, FlickLayout.layout.rows[0].keys[0].action)
    }

    @Test
    fun cursorKeyMovesLeftAndRight() {
        val cursor = flick("カーソル")
        assertEquals(KeyAction.CursorLeft, cursor.actionFor(FlickDirection.CENTER))
        assertEquals(KeyAction.CursorRight, cursor.actionFor(FlickDirection.RIGHT))
    }

    @Test
    fun numberKeyFlicksToQwerty() {
        val numbers = flick("123")
        assertEquals(KeyAction.SwitchLayout(SymbolLayout.NUMBERS), numbers.actionFor(FlickDirection.CENTER))
        assertEquals(KeyAction.SwitchLayout(KeyboardLayout.QWERTY), numbers.actionFor(FlickDirection.RIGHT))
    }

    @Test
    fun qwertyKeysFlickToTheOtherLayouts() {
        assertEquals(
            KeyAction.SwitchLayout(KeyboardLayout.FLICK),
            flick("あ/A", QwertyLayout.layout).actionFor(FlickDirection.UP),
        )
        assertEquals(KeyAction.Okurigana, flick("変換", QwertyLayout.layout).actionFor(FlickDirection.UP))
    }

    @Test
    fun boundaryKeysAreGone() {
        val actions = FlickLayout.layout.rows.flatMap { it.keys }.map { it.action }
        assertTrue(actions.none { it == KeyAction.Shrink })
        assertTrue(actions.none { it == KeyAction.Expand })
    }
}
