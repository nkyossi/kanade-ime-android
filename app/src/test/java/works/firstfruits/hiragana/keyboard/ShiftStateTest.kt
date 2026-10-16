package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftStateTest {
    @Test
    fun oneTapShiftsTheNextLetterOnly() {
        val shift = ShiftState()
        shift.tap(0)
        assertTrue(shift.shifted)
        assertFalse(shift.locked)
        shift.letterTyped()
        assertFalse(shift.shifted)
    }

    @Test
    fun aSlowSecondTapCancelsTheShift() {
        val shift = ShiftState()
        shift.tap(0)
        shift.tap(1000)
        assertFalse(shift.shifted)
        assertFalse(shift.locked)
    }

    @Test
    fun twoQuickTapsLockTheShift() {
        val shift = ShiftState()
        shift.tap(0)
        shift.tap(200)
        assertTrue(shift.shifted)
        assertTrue(shift.locked)
    }

    @Test
    fun aLockSurvivesTypingAndEndsWithTheNextTap() {
        val shift = ShiftState()
        shift.tap(0)
        shift.tap(200)
        shift.letterTyped()
        shift.letterTyped()
        assertTrue(shift.locked)
        assertTrue(shift.shifted)
        shift.tap(5000)
        assertFalse(shift.locked)
        assertFalse(shift.shifted)
    }

    @Test
    fun aTapAfterTheLockIsEndedIsOneShotAgain() {
        val shift = ShiftState()
        shift.tap(0)
        shift.tap(200)
        shift.tap(400)
        // Quick after the tap that ended the lock, but the shift is off, so it only shifts once.
        shift.tap(600)
        assertTrue(shift.shifted)
        assertFalse(shift.locked)
    }
}
