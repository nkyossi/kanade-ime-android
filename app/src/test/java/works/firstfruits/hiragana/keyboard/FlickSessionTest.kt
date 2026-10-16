package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the 3-dan flick paths against the ones スミレ入力 documents. The guide
 * draws every cell around the middle of the key, so a test moves the finger to
 * where that cell lies and expects the kana the cell shows.
 */
class FlickSessionTest {
    private val threshold = 20f

    private fun session(label: String): FlickSession {
        val session = FlickSession(threshold)
        session.press(KanaTable.tree(label)!!, 0f, 0f)
        return session
    }

    @Test
    fun aTapTakesTheFirstKana() {
        val session = session("か")
        assertEquals("か", session.current)
        assertEquals("か", session.release())
    }

    @Test
    fun aFlickTakesTheKanaOfThatSide() {
        val session = session("か")
        assertTrue(session.move(-30f, 0f))
        assertEquals(FlickDirection.LEFT, session.highlight)
        assertEquals("き", session.current)
        assertEquals("き", session.release())
    }

    @Test
    fun aTapOnTheCornerTakesTheDakuten() {
        val session = session("か")
        session.move(30f, -30f)
        assertEquals("が", session.current)
    }

    @Test
    fun aSecondFlickOpensTheYoon() {
        val session = session("か")
        session.move(-30f, 0f)
        // The cell of きゅ is drawn above the key, so the finger moves up.
        session.move(-30f, -40f)
        assertEquals("きゅ", session.current)
    }

    @Test
    fun aMoveToTheOtherSideOfTheKeyIsTheYa() {
        val session = session("か")
        session.move(-30f, 0f)
        session.move(30f, 0f)
        assertEquals("きゃ", session.current)
        assertEquals(FlickDirection.RIGHT, session.highlight)
    }

    @Test
    fun aFlickDownFromTheIColumnIsTheYo() {
        val session = session("か")
        session.move(-30f, 0f)
        session.move(0f, 30f)
        assertEquals("きょ", session.current)
    }

    @Test
    fun aTurnFromTheYuOfTheIColumnReachesTheYa() {
        val session = session("か")
        session.move(-30f, 0f)
        session.move(0f, -30f)
        assertEquals("きゅ", session.current)
        // The stage above keeps きゃ and きょ reachable from here.
        session.move(30f, 0f)
        assertEquals("きゃ", session.current)
    }

    @Test
    fun theLongYoonExtendsTheYu() {
        val session = session("か")
        session.move(-30f, 0f)
        session.move(0f, -30f)
        assertEquals("きゅ", session.current)
        // The long form sits up and to the left of the key.
        session.move(-30f, -40f)
        assertEquals("きゅう", session.current)
    }

    @Test
    fun theYoonOfTheDakutenSwitchesTheRow() {
        val session = session("か")
        session.move(-30f, 0f)
        // ぎ is the cell below the き cell, on the corner of the key.
        session.move(-30f, 40f)
        assertEquals("ぎ", session.current)
    }

    @Test
    fun aTurnFromTheGiReachesTheGya() {
        val session = session("か")
        session.move(-30f, 0f)
        session.move(-30f, 40f)
        session.move(30f, 0f)
        assertEquals("ぎゃ", session.current)
    }

    @Test
    fun aFingerThatComesBackToTheKeyKeepsTheStage() {
        val session = session("か")
        session.move(-30f, 0f)
        assertFalse(session.move(-8f, -8f))
        assertEquals("き", session.current)
        assertFalse(session.move(-30f, 0f))
        assertEquals("き", session.current)
        session.move(0f, -30f)
        assertEquals("きゅ", session.current)
        assertFalse(session.move(2f, -4f))
        assertEquals("きゅ", session.current)
    }

    @Test
    fun aFingerThatStaysOnOneCellChangesNothing() {
        val session = session("か")
        assertTrue(session.move(-30f, 0f))
        assertFalse(session.move(-30f, 0f))
        assertFalse(session.move(-32f, 2f))
        assertEquals(FlickDirection.LEFT, session.highlight)
        assertEquals("き", session.current)
    }

    @Test
    fun theHaRowReachesTheFa() {
        val session = session("は")
        session.move(0f, -30f)
        assertEquals("ふ", session.current)
        session.move(-30f, 0f)
        assertEquals("ふぁ", session.current)
    }

    @Test
    fun theTaRowReachesTheTi() {
        val session = session("た")
        session.move(30f, 0f)
        assertEquals("て", session.current)
        session.move(30f, -30f)
        assertEquals("てぃ", session.current)
        session.move(30f, -60f)
        assertEquals("てぃー", session.current)
    }

    @Test
    fun theSmallAComesFromTheCornerOfTheARow() {
        val session = session("あ")
        session.move(30f, -30f)
        assertEquals("ぁ", session.current)
        session.move(30f, -60f)
        assertEquals("ぁ", session.current)
    }

    @Test
    fun theNumbersAreOnTheLeftOfTheORow() {
        val session = session("か")
        session.move(0f, 30f)
        assertEquals("こ", session.current)
        session.move(-30f, 0f)
        assertEquals("2", session.current)
    }

    @Test
    fun aKeyWithoutStagesHasNoCurrent() {
        val session = FlickSession(threshold)
        assertFalse(session.active)
        assertNull(session.release())
    }

    @Test
    fun optionsShowTheDirectionsOfTheStage() {
        val session = session("か")
        assertEquals(
            setOf(
                FlickDirection.CENTER,
                FlickDirection.UP_RIGHT,
                FlickDirection.LEFT,
                FlickDirection.UP,
                FlickDirection.RIGHT,
                FlickDirection.DOWN,
            ),
            session.options().keys,
        )
        session.move(-30f, 0f)
        assertEquals(
            setOf(
                FlickDirection.LEFT,
                FlickDirection.DOWN_LEFT,
                FlickDirection.UP,
                FlickDirection.RIGHT,
                FlickDirection.DOWN,
            ),
            session.options().keys,
        )
    }
}
