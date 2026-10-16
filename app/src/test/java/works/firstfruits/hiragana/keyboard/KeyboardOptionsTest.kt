package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardOptionsTest {
    private fun letter(layout: KeyboardLayout, label: String): KeyAction.Letter =
        layout.rows.flatMap { it.keys }.first { it.label == label }.action as KeyAction.Letter

    @Test
    fun theTopRowFlicksDownToTheDigits() {
        val layout = QwertyLayout.build(KeyboardOptions())
        assertEquals('1', letter(layout, "q").down)
        assertEquals('0', letter(layout, "p").down)
        assertEquals('7', letter(layout, "u").down)
        assertNull(letter(layout, "a").down)
    }

    @Test
    fun theDigitsCanBeTurnedOff() {
        val layout = QwertyLayout.build(KeyboardOptions(letterDownDigits = false))
        assertNull(letter(layout, "q").down)
        assertNull(letter(layout, "p").down)
    }

    @Test
    fun aVowelLongPressIsCircumflexByDefault() {
        val layout = QwertyLayout.build(KeyboardOptions())
        assertEquals("â", letter(layout, "a").long)
        assertEquals("ô", letter(layout, "o").long)
        assertNull(letter(layout, "k").long)
    }

    @Test
    fun theMacronSettingAppliesToEveryVowel() {
        val layout = QwertyLayout.build(KeyboardOptions(vowelLongPress = VowelLongPress.MACRON))
        assertEquals("ā", letter(layout, "a").long)
        assertEquals("ī", letter(layout, "i").long)
        assertEquals("ū", letter(layout, "u").long)
        assertEquals("ē", letter(layout, "e").long)
        assertEquals("ō", letter(layout, "o").long)
        assertNull(letter(layout, "k").long)
    }

    @Test
    fun theNoneSettingTurnsTheLongPressOff() {
        val layout = QwertyLayout.build(KeyboardOptions(vowelLongPress = VowelLongPress.NONE))
        assertNull(letter(layout, "a").long)
        assertNull(letter(layout, "o").long)
    }

    @Test
    fun theRareKeyUsesTheSettingsAndAnEmptyOneGivesNothing() {
        val layout = FlickLayout.build(KeyboardOptions(rareCenter = "ます", rareUp = "", rareDown = "です"))
        val key = layout.rows[1].keys[4]
        assertEquals("ます", key.label)
        val action = key.action as KeyAction.Kana
        assertEquals("ます", action.kanaFor(FlickDirection.CENTER))
        assertNull(action.kanaFor(FlickDirection.UP))
        assertEquals("です", action.kanaFor(FlickDirection.DOWN))
    }

    @Test
    fun theRareKeyIsKaraAndDesuByDefault() {
        val key = FlickLayout.layout.rows[1].keys[4]
        assertEquals("られ", key.label)
        assertEquals("から", (key.action as KeyAction.Kana).kanaFor(FlickDirection.UP))
    }

    @Test
    fun theShiftKeyIsKeptInTheQwertyLayout() {
        val shift = QwertyLayout.build(KeyboardOptions()).rows[2].keys[0]
        assertEquals(KeyAction.Shift, shift.action)
    }

    @Test
    fun aVowelMarkIsOnlyForVowels() {
        assertEquals("â", VowelLongPress.CIRCUMFLEX.of('a'))
        assertNull(VowelLongPress.CIRCUMFLEX.of('k'))
        assertNull(VowelLongPress.NONE.of('a'))
    }
}
