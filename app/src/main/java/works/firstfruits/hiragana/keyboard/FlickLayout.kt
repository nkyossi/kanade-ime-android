package works.firstfruits.hiragana.keyboard

/** The 12-key layout for kana input by tap and flick. */
object FlickLayout {
    private fun kana(s: String): Key {
        // s holds center, left, up, right, down; '_' means nothing.
        val flicks = s.map { if (it == '_') null else it.toString() }
        return Key(flicks[0]!!, KeyAction.Kana(flicks), hint = s.filter { it != '_' }.drop(1))
    }

    val layout = KeyboardLayout(
        KeyboardLayout.FLICK,
        listOf(
            KeyRow(listOf(Key("←境界", KeyAction.Shrink), kana("あいうえお"), kana("かきくけこ"), kana("さしすせそ"),
                Key("⌫", KeyAction.Backspace, repeatable = true))),
            KeyRow(listOf(Key("境界→", KeyAction.Expand), kana("たちつてと"), kana("なにぬねの"), kana("はひふへほ"),
                Key("送り", KeyAction.Okurigana))),
            KeyRow(listOf(Key("カナ", KeyAction.Katakana), kana("まみむめも"), kana("や「ゆ」よ"), kana("らりるれろ"),
                Key("変換", KeyAction.Convert))),
            KeyRow(listOf(Key("ABC", KeyAction.SwitchLayout(KeyboardLayout.QWERTY)), Key("゛゜小", KeyAction.KanaCycle),
                kana("わをんー_"), kana("、。？！_"), Key("確定", KeyAction.Enter))),
        ),
    )
}
