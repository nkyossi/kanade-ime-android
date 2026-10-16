package works.firstfruits.hiragana.keyboard

/** What a key does. HiraganaImeService maps each action to the engine. */
sealed interface KeyAction {
    /** An ASCII character sent to the engine as a key of a US keyboard (QWERTY). */
    data class Letter(val char: Char) : KeyAction

    /**
     * Kana chosen by a tap or a flick (12-key). [flicks] holds the kana for
     * [FlickDirection.CENTER], LEFT, UP, RIGHT and DOWN in that order; null
     * means nothing in that direction.
     */
    data class Kana(val flicks: List<String?>) : KeyAction {
        fun kanaFor(direction: FlickDirection): String? = flicks.getOrNull(direction.ordinal)
    }

    /** Text committed as it is, without the engine (e.g. emoji). Not used yet. */
    data class Text(val text: String) : KeyAction

    /** Switches to another layout, e.g. from QWERTY to symbols. */
    data class SwitchLayout(val id: String) : KeyAction

    data object Convert : KeyAction       // 変換: converts the kana before the cursor (Space)
    data object Okurigana : KeyAction     // 送り: converts with okurigana (Shift+Henkan)
    data object Katakana : KeyAction      // カナ: makes the kana before the cursor katakana (Right Shift)
    data object Shrink : KeyAction        // ←境界: shortens the reading (Tab)
    data object Expand : KeyAction        // 境界→: extends the reading (Shift+Tab)
    data object Enter : KeyAction         // 確定 / 改行 (Return)
    data object Cancel : KeyAction        // 取消 (Escape)
    data object Backspace : KeyAction
    data object Space : KeyAction         // a plain space; converts in the hiragana mode like the Space bar
    data object Shift : KeyAction         // one-shot shift for Letter keys
    data object KanaCycle : KeyAction     // ゛゜小: か→が, は→ば→ぱ, つ→っ→づ
    data object ToggleMode : KeyAction    // あ ⇔ A
    data object CursorLeft : KeyAction
    data object CursorRight : KeyAction
}

/** The five directions of a flick. The order matches KeyAction.Kana.flicks. */
enum class FlickDirection { CENTER, LEFT, UP, RIGHT, DOWN }

/**
 * A key. [width] is relative to the other keys of the row (1 = normal).
 * [hint] is drawn small, e.g. the flick candidates.
 */
data class Key(
    val label: String,
    val action: KeyAction,
    val width: Float = 1f,
    val hint: String = "",
    val repeatable: Boolean = false,
)

data class KeyRow(val keys: List<Key>)

/** A keyboard: rows from top to bottom. */
data class KeyboardLayout(val id: String, val rows: List<KeyRow>) {
    companion object {
        const val QWERTY = "qwerty"
        const val FLICK = "flick"
    }
}
