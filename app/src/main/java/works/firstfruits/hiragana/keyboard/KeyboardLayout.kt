package works.firstfruits.hiragana.keyboard

import works.firstfruits.hiragana.R

/** What a key does. HiraganaImeService maps each action to the engine. */
sealed interface KeyAction {
    /**
     * An ASCII character sent to the engine as a key of a US keyboard (QWERTY).
     * [down] is the digit a down flick gives, and [long] the text a long press
     * gives; null means none.
     */
    data class Letter(val char: Char, val down: Char? = null, val long: String? = null) : KeyAction

    /**
     * Kana chosen by a tap or a flick (12-key). [flicks] holds the kana for
     * [FlickDirection.CENTER], LEFT, UP, RIGHT and DOWN in that order; null
     * means nothing in that direction. [tree] adds the eight directions and the
     * further stages of a three-stage flick; it is null for keys that have none,
     * and is used instead of [flicks] while the setting is on.
     */
    data class Kana(val flicks: List<String?>, val tree: FlickKey? = null) : KeyAction {
        fun kanaFor(direction: FlickDirection): String? = flicks.getOrNull(direction.ordinal)
    }

    /**
     * Any action chosen by a tap or a flick, like [Kana] but not limited to kana.
     * [actions] holds the action for CENTER, LEFT, UP, RIGHT and DOWN in that
     * order; [labels] holds what the flick guide shows there. null means nothing.
     */
    data class Flick(
        val actions: List<KeyAction?>,
        val labels: List<String?> = emptyList(),
    ) : KeyAction {
        fun actionFor(direction: FlickDirection): KeyAction? = actions.getOrNull(direction.ordinal)
        fun labelFor(direction: FlickDirection): String? = labels.getOrNull(direction.ordinal)
    }

    /** Text committed as it is, without the engine (e.g. punctuation and emoji). */
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
    data object Paste : KeyAction        // ペースト: inserts the clipboard text

    /**
     * A space that is committed at once. Its width follows the mode: full-width in
     * the kana modes, half-width in the A mode. [flipped] gives the other width.
     */
    data class Blank(val flipped: Boolean) : KeyAction
}

/**
 * Builds a [KeyAction.Flick] from direction to (label, action) pairs, e.g.
 * `flickAction(FlickDirection.RIGHT to ("ABC" to KeyAction.SwitchLayout(id)))`.
 */
fun flickAction(vararg options: Pair<FlickDirection, Pair<String, KeyAction>>): KeyAction.Flick {
    val byDirection = options.toMap()
    return KeyAction.Flick(
        actions = FlickDirection.entries.map { byDirection[it]?.second },
        labels = FlickDirection.entries.map { byDirection[it]?.first },
    )
}

/**
 * The space key of both layouts: a tap gives the space of the mode (full-width in
 * the kana modes, half-width in the A mode) and the up flick the other width.
 * The key never converts.
 */
fun spaceKey(width: Float = 1f): Key = Key(
    "空白",
    flickAction(
        FlickDirection.CENTER to ("通常" to KeyAction.Blank(flipped = false)),
        FlickDirection.UP to ("別幅" to KeyAction.Blank(flipped = true)),
    ),
    width = width,
    iconRes = R.drawable.ic_space,
)

/**
 * The directions of a flick: the centre, the four sides and, for the
 * three-stage flick of a kana key, the four corners. The order of the first
 * five matches KeyAction.Kana.flicks and KeyAction.Flick.actions.
 */
enum class FlickDirection { CENTER, LEFT, UP, RIGHT, DOWN, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT }

/**
 * A key. [width] is relative to the other keys of the row (1 = normal).
 * When [iconRes] is not zero the key shows that drawable instead of [label],
 * like a clipboard for ペースト.
 */
data class Key(
    val label: String,
    val action: KeyAction,
    val width: Float = 1f,
    val repeatable: Boolean = false,
    val iconRes: Int = 0,
)

data class KeyRow(val keys: List<Key>)

/** A keyboard: rows from top to bottom. */
data class KeyboardLayout(val id: String, val rows: List<KeyRow>) {
    companion object {
        const val QWERTY = "qwerty"
        const val FLICK = "flick"
    }
}
