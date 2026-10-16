package works.firstfruits.hiragana.ime

/** The X keysyms and evdev key codes the engine understands (core/Keysyms.h). */
object Keysym {
    const val SPACE = 0x020
    const val BACKSPACE = 0xff08
    const val TAB = 0xff09
    const val RETURN = 0xff0d
    const val ESCAPE = 0xff1b
    const val HENKAN = 0xff23
    const val SHIFT_R = 0xffe2
    const val LEFT = 0xff51
    const val RIGHT = 0xff53

    const val SHIFT_MASK = 1 shl 0
    const val RELEASE_MASK = 1 shl 30

    // evdev key codes
    const val KEY_ESC = 1
    const val KEY_BACKSPACE = 14
    const val KEY_TAB = 15
    const val KEY_ENTER = 28
    const val KEY_RIGHTSHIFT = 54
    const val KEY_SPACE = 57
    const val KEY_HENKAN = 92

    private const val ROW_DIGITS = "1234567890-="
    private val ROWS = listOf("qwertyuiop[]" to 16, "asdfghjkl;'" to 30, "zxcvbnm,./" to 44)

    /** The evdev key code of a US keyboard for [c] (lowercase), or 0. */
    fun keycodeOf(c: Char): Int {
        val lower = c.lowercaseChar()
        ROW_DIGITS.indexOf(lower).let { if (0 <= it) return 2 + it }
        for ((row, start) in ROWS) {
            val i = row.indexOf(lower)
            if (0 <= i) return start + i
        }
        return 0
    }
}
