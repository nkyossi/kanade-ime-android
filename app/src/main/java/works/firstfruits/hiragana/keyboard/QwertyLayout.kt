package works.firstfruits.hiragana.keyboard

/** The QWERTY layout for roman letters, with the conversion keys of Hiragana IME. */
object QwertyLayout {
    private fun letters(s: String) = s.map { Key(it.toString(), KeyAction.Letter(it)) }

    val layout = KeyboardLayout(
        KeyboardLayout.QWERTY,
        listOf(
            KeyRow(letters("qwertyuiop")),
            KeyRow(letters("asdfghjkl") + Key("ー", KeyAction.Letter('-'))),
            KeyRow(
                listOf(Key("⇧", KeyAction.Shift, 1.5f)) + letters("zxcvbnm") +
                    Key("⌫", KeyAction.Backspace, 1.5f, repeatable = true),
            ),
            KeyRow(
                listOf(
                    Key("12キー", KeyAction.SwitchLayout(KeyboardLayout.FLICK), 1.5f),
                    Key("あ/A", KeyAction.ToggleMode, 1.2f),
                    Key("、", KeyAction.Letter(',')),
                    Key("送り", KeyAction.Okurigana, 1.2f),
                    Key("変換", KeyAction.Convert, 2.4f),
                    Key("カナ", KeyAction.Katakana, 1.2f),
                    Key("。", KeyAction.Letter('.')),
                    Key("確定", KeyAction.Enter, 1.5f),
                ),
            ),
        ),
    )
}
