package works.firstfruits.hiragana.keyboard

/** The vowels whose keys have a long press, in the order of [VowelLongPress.of]. */
const val VOWELS = "aiueo"

/** What a long press on a vowel key gives. */
enum class VowelLongPress {
    NONE,
    CIRCUMFLEX,
    MACRON;

    /** The letter a long press on [vowel] gives, or null for [NONE]. */
    fun of(vowel: Char): String? {
        val marks = when (this) {
            NONE -> return null
            CIRCUMFLEX -> "âîûêô"
            MACRON -> "āīūēō"
        }
        val i = VOWELS.indexOf(vowel)
        return if (i < 0) null else marks[i].toString()
    }
}

/** The settings that change the keyboard layouts. */
data class KeyboardOptions(
    /** The long press of the vowel keys a, i, u, e and o. */
    val vowelLongPress: VowelLongPress = VowelLongPress.CIRCUMFLEX,
    /** Whether a down flick on the letters of the top row gives the digits. */
    val letterDownDigits: Boolean = true,
    /** The kana of the rare key of the 12-key layout: its tap, up flick and down flick. */
    val rareCenter: String = RARE_CENTER,
    val rareUp: String = RARE_UP,
    val rareDown: String = RARE_DOWN,
) {
    companion object {
        const val RARE_CENTER = "られ"
        const val RARE_UP = "から"
        const val RARE_DOWN = "です"
    }
}
