package works.firstfruits.hiragana.keyboard

/** The number and symbol layouts. Symbols are committed as text, not through the engine. */
object SymbolLayout {
    const val NUMBERS = "numbers"
    const val SYMBOLS = "symbols"

    /** Digits and punctuation typed as US keyboard letters, like the QWERTY keys. */
    private fun letters(s: String) = s.map { Key(it.toString(), KeyAction.Letter(it)) }

    /** Keys committed as they are, without the engine. */
    private fun text(s: String) = s.map { Key(it.toString(), KeyAction.Text(it.toString())) }

    /** The shared bottom row: a layout switch, the flick layout, space and enter. */
    private fun bottom(switchLabel: String, switchId: String) = listOf(
        Key(switchLabel, KeyAction.SwitchLayout(switchId)),
        Key("あいう", KeyAction.SwitchLayout(KeyboardLayout.FLICK)),
        Key("空白", KeyAction.Space),
        Key("確定", KeyAction.Enter),
    )

    val numbers = KeyboardLayout(
        NUMBERS,
        listOf(
            KeyRow(letters("1234567890")),
            KeyRow(letters("-/:;()") + text("¥") + letters("&@\"")),
            KeyRow(
                listOf(Key("記号", KeyAction.SwitchLayout(SYMBOLS))) + letters(".,?!'") +
                    Key("⌫", KeyAction.Backspace, 1.5f, repeatable = true),
            ),
            KeyRow(bottom("ABC", KeyboardLayout.QWERTY)),
        ),
    )

    val symbols = KeyboardLayout(
        SYMBOLS,
        listOf(
            KeyRow(text("[]{}#%^*+=")),
            KeyRow(text("_\\|~<>€£•…")),
            KeyRow(text("「」『』【】・～※〒") + Key("⌫", KeyAction.Backspace, 1.5f, repeatable = true)),
            KeyRow(bottom("123", NUMBERS)),
        ),
    )
}
