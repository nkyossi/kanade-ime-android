package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import works.firstfruits.hiragana.R

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
        val cursor = flick("←")
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
    }

    @Test
    fun theQwertyOkuriganaKeyIsNowTheSpaceKey() {
        assertTrue(QwertyLayout.layout.rows.flatMap { it.keys }.none { it.label == "送り" })
        val space = flick("空白", QwertyLayout.layout)
        assertEquals(KeyAction.Blank(flipped = false), space.actionFor(FlickDirection.CENTER))
        assertEquals(KeyAction.Blank(flipped = true), space.actionFor(FlickDirection.UP))
        assertEquals(R.drawable.ic_space, key("空白", QwertyLayout.layout).iconRes)
    }

    @Test
    fun theQwertyKanaKeyFlicksUpToTheConvert() {
        val kana = flick("カナ", QwertyLayout.layout)
        assertEquals(KeyAction.Katakana, kana.actionFor(FlickDirection.CENTER))
        assertEquals(KeyAction.Convert, kana.actionFor(FlickDirection.UP))
    }

    @Test
    fun theSpaceKeyReplacesConvertAndShowsTheSpaceIcon() {
        assertTrue(FlickLayout.layout.rows.flatMap { it.keys }.none { it.action == KeyAction.Convert })
        assertEquals(R.drawable.ic_space, key("空白").iconRes)
    }

    @Test
    fun theSpaceKeyTapFollowsTheModeAndUpFlickTheOtherWidth() {
        val space = flick("空白")
        assertEquals(KeyAction.Blank(flipped = false), space.actionFor(FlickDirection.CENTER))
        assertEquals(KeyAction.Blank(flipped = true), space.actionFor(FlickDirection.UP))
    }

    @Test
    fun theRareKeyFlicksToKaraAndDesu() {
        val rare = key("られ").action as KeyAction.Kana
        assertEquals("られ", rare.kanaFor(FlickDirection.CENTER))
        assertEquals("から", rare.kanaFor(FlickDirection.UP))
        assertEquals("です", rare.kanaFor(FlickDirection.DOWN))
        assertEquals(null, rare.kanaFor(FlickDirection.LEFT))
        assertEquals(null, rare.kanaFor(FlickDirection.RIGHT))
    }

    @Test
    fun boundaryKeysAreGone() {
        val actions = FlickLayout.layout.rows.flatMap { it.keys }.map { it.action }
        assertTrue(actions.none { it == KeyAction.Shrink })
        assertTrue(actions.none { it == KeyAction.Expand })
    }

    @Test
    fun qwertyCommaFlicksToThePeriod() {
        val comma = flick("、", QwertyLayout.layout)
        assertEquals(KeyAction.Text("、"), comma.actionFor(FlickDirection.CENTER))
        assertEquals(KeyAction.Text("。"), comma.actionFor(FlickDirection.LEFT))
    }

    @Test
    fun kanaKeysCarryTheirThreeStageStages() {
        val sides = listOf(
            FlickDirection.CENTER,
            FlickDirection.LEFT,
            FlickDirection.UP,
            FlickDirection.RIGHT,
            FlickDirection.DOWN,
        )
        var seen = 0
        for (row in FlickLayout.layout.rows) for (key in row.keys) {
            val action = key.action
            // られ has two kana and no stages, so it is not one of the 11 kana keys.
            if (action !is KeyAction.Kana || key.label == "られ") continue
            val tree = action.tree
            assertTrue(key.label, tree != null)
            assertEquals(key.label, tree!!.label)
            seen++
            // The first stage still holds what the five-way list says, so turning
            // the three-stage flick off changes as little as possible.
            for (direction in sides) {
                val expected = action.kanaFor(direction) ?: continue
                assertEquals(expected, tree.map(KanaMode.NORMAL)[direction]?.text)
            }
        }
        assertEquals(11, seen)
    }
}
