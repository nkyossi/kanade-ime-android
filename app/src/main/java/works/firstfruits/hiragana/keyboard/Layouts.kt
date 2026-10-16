package works.firstfruits.hiragana.keyboard

/** All the layouts by their id; KeyAction.SwitchLayout(id) switches to one of them. */
object Layouts {
    private val fixed: Map<String, KeyboardLayout> = listOf(
        SymbolLayout.numbers,
        SymbolLayout.symbols,
    ).associateBy { it.id }

    /**
     * The layout for [id], built with [options] for the QWERTY and 12-key layouts;
     * the QWERTY layout for an unknown id.
     */
    fun byId(id: String, options: KeyboardOptions = KeyboardOptions()): KeyboardLayout = when (id) {
        KeyboardLayout.FLICK -> FlickLayout.build(options)
        KeyboardLayout.QWERTY -> QwertyLayout.build(options)
        else -> fixed[id] ?: QwertyLayout.build(options)
    }
}
