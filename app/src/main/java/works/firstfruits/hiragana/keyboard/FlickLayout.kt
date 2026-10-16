package works.firstfruits.hiragana.keyboard

/** The 12-key layout for kana input by tap and flick. */
object FlickLayout {
    private fun kana(s: String): Key {
        // s holds center, left, up, right, down; '_' means nothing.
        val flicks = s.map { if (it == '_') null else it.toString() }
        return Key(flicks[0]!!, KeyAction.Kana(flicks), hint = s.filter { it != '_' }.drop(1))
    }

    /** The cursor key: tap moves left, the right flick moves right. */
    private val cursor = Key("カーソル", flickAction(
        FlickDirection.CENTER to ("←" to KeyAction.CursorLeft),
        FlickDirection.RIGHT to ("→" to KeyAction.CursorRight),
    ))

    /** The 123 key also reaches the QWERTY layout by its right flick. */
    private val numbers = Key("123", flickAction(
        FlickDirection.CENTER to ("123" to KeyAction.SwitchLayout(SymbolLayout.NUMBERS)),
        FlickDirection.RIGHT to ("ABC" to KeyAction.SwitchLayout(KeyboardLayout.QWERTY)),
    ))

    val layout = KeyboardLayout(
        KeyboardLayout.FLICK,
        listOf(
            KeyRow(listOf(Key("ペースト", KeyAction.Paste), kana("あいうえお"), kana("かきくけこ"),
                kana("さしすせそ"), Key("⌫", KeyAction.Backspace, repeatable = true))),
            KeyRow(listOf(cursor, kana("たちつてと"), kana("なにぬねの"), kana("はひふへほ"),
                Key("送り", KeyAction.Okurigana))),
            KeyRow(listOf(Key("カナ", KeyAction.Katakana), kana("まみむめも"), kana("や「ゆ」よ"),
                kana("らりるれろ"), Key("変換", KeyAction.Convert))),
            KeyRow(listOf(numbers, Key("゛゜小", KeyAction.KanaCycle), kana("わをんー_"),
                kana("、。？！_"), Key("確定", KeyAction.Enter))),
        ),
    )
}
