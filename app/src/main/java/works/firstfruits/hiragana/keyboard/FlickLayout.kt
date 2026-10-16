package works.firstfruits.hiragana.keyboard

import works.firstfruits.hiragana.R

/** The 12-key layout for kana input by tap and flick. */
object FlickLayout {
    private fun kana(s: String): Key {
        // s holds center, left, up, right, down; '_' means nothing.
        val flicks = s.map { if (it == '_') null else it.toString() }
        return Key(flicks[0]!!, KeyAction.Kana(flicks, KanaTable.tree(flicks[0]!!)))
    }

    /** The cursor key: tap moves left, the right flick moves right. */
    private val cursor = Key("←", flickAction(
        FlickDirection.CENTER to ("←" to KeyAction.CursorLeft),
        FlickDirection.RIGHT to ("→" to KeyAction.CursorRight),
    ))

    /** The 123 key also reaches the QWERTY layout by its right flick. */
    private val numbers = Key("123", flickAction(
        FlickDirection.CENTER to ("123" to KeyAction.SwitchLayout(SymbolLayout.NUMBERS)),
        FlickDirection.RIGHT to ("ABC" to KeyAction.SwitchLayout(KeyboardLayout.QWERTY)),
    ))

    /**
     * The rare key: its tap, up flick and down flick give the kana of [options] (by
     * default られ, から and です); an empty one gives nothing. It has no three-stage flick.
     */
    private fun rare(options: KeyboardOptions): Key {
        val flicks = listOf(options.rareCenter, null, options.rareUp, null, options.rareDown)
            .map { it?.takeIf { kana -> kana.isNotEmpty() } }
        return Key(options.rareCenter, KeyAction.Kana(flicks))
    }

    /** The 12-key layout with the rare key of [options]. */
    fun build(options: KeyboardOptions) = KeyboardLayout(
        KeyboardLayout.FLICK,
        listOf(
            KeyRow(listOf(Key("", KeyAction.Paste, iconRes = R.drawable.ic_content_paste), kana("あいうえお"), kana("かきくけこ"),
                kana("さしすせそ"), Key("⌫", KeyAction.Backspace, repeatable = true))),
            KeyRow(listOf(cursor, kana("たちつてと"), kana("なにぬねの"), kana("はひふへほ"), rare(options))),
            KeyRow(listOf(Key("カナ", KeyAction.Katakana), kana("まみむめも"), kana("や「ゆ」よ"),
                kana("らりるれろ"), spaceKey())),
            KeyRow(listOf(numbers, Key("゛゜小", KeyAction.KanaCycle), kana("わをんー_"),
                kana("、。？！_"), Key("確定", KeyAction.Enter))),
        ),
    )

    val layout = build(KeyboardOptions())
}
