package works.firstfruits.hiragana.keyboard

/**
 * The shift of the letter keys. One tap shifts the next letter; two quick taps
 * lock it (caps lock) until the next tap.
 */
class ShiftState(private val doubleTapMillis: Long = 400L) {
    /** True while the letters are upper case, one-shot or locked. */
    var shifted = false
        private set

    /** True while locked, as caps lock. */
    var locked = false
        private set

    private var lastTap: Long? = null

    /** A tap of the shift key at [now], in milliseconds. */
    fun tap(now: Long) {
        val last = lastTap
        val quick = last != null && now - last <= doubleTapMillis
        lastTap = now
        when {
            locked -> {
                locked = false
                shifted = false
            }
            quick && shifted -> locked = true
            else -> shifted = !shifted
        }
    }

    /** A letter was typed: a one-shot shift ends, a lock stays. */
    fun letterTyped() {
        if (!locked) shifted = false
    }
}
