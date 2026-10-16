package works.firstfruits.hiragana.keyboard

/** The QWERTY layout for roman letters, with the conversion keys of Hiragana IME. */
object QwertyLayout {
    /** The letters of the top row, and the digit each gives on a down flick. */
    private const val TOP_ROW = "qwertyuiop"
    private const val DIGITS = "1234567890"

    private fun letter(c: Char, options: KeyboardOptions, digit: Char? = null) = KeyAction.Letter(
        c,
        down = digit.takeIf { options.letterDownDigits },
        long = options.vowelLongPress.of(c),
    )

    private fun letters(s: String, options: KeyboardOptions) = s.map { Key(it.toString(), letter(it, options)) }

    /** The QWERTY layout with the digits and the long presses of [options]. */
    fun build(options: KeyboardOptions) = KeyboardLayout(
        KeyboardLayout.QWERTY,
        listOf(
            KeyRow(TOP_ROW.map { Key(it.toString(), letter(it, options, DIGITS[TOP_ROW.indexOf(it)])) }),
            KeyRow(letters("asdfghjkl", options) + Key("ー", KeyAction.Letter('-'))),
            KeyRow(
                listOf(Key("⇧", KeyAction.Shift, 1.5f)) + letters("zxcvbnm", options) +
                    Key("⌫", KeyAction.Backspace, 1.5f, repeatable = true),
            ),
            KeyRow(
                listOf(
                    Key("あ/A", flickAction(
                        FlickDirection.CENTER to ("あ/A" to KeyAction.ToggleMode),
                        FlickDirection.UP to ("12キー" to KeyAction.SwitchLayout(KeyboardLayout.FLICK)),
                    ), 1.2f),
                    Key("123", KeyAction.SwitchLayout(SymbolLayout.NUMBERS)),
                    spaceKey(2.4f),
                    Key("カナ", flickAction(
                        FlickDirection.CENTER to ("カナ" to KeyAction.Katakana),
                        FlickDirection.UP to ("変換" to KeyAction.Convert),
                    ), 1.2f),
                    Key("、", flickAction(
                        FlickDirection.CENTER to ("、" to KeyAction.Text("、")),
                        FlickDirection.LEFT to ("。" to KeyAction.Text("。")),
                    )),
                    Key("確定", KeyAction.Enter, 1.5f),
                ),
            ),
        ),
    )

    val layout = build(KeyboardOptions())
}
