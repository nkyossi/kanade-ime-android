package works.firstfruits.hiragana.keyboard

/**
 * A three-stage flick: while the finger stays on a kana key it may turn from
 * direction to direction, and every turn opens another stage of kana. This is
 * the model of the stages themselves; [FlickSession] follows the finger.
 */
sealed interface FlickNode {
    /** A kana to insert. [nextMode] switches the key to another row, e.g. き→ぎ. */
    data class Input(val text: String, val nextMode: KanaMode? = null) : FlickNode

    /**
     * Opens another stage. [label] is the kana of this stage (shown in the middle
     * of the guide), [cancelOnTap] is unused by the current gesture but kept for
     * the definitions, and [continuations] keeps the kana of the parent stage in
     * those directions reachable without going back first (so きゅ→きょ works).
     */
    data class Branch(
        val label: String,
        val children: Map<FlickDirection, FlickNode>,
        val cancelOnTap: Boolean = true,
        val continuations: Set<FlickDirection> = emptySet(),
    ) : FlickNode
}

/** The row of kana a key is in: plain, with だくてん or with はんだくてん. */
enum class KanaMode { NORMAL, DAKUTEN, HANDAKUTEN }

/** The stages of one kana key, one map for each [KanaMode] the key supports. */
data class FlickKey(
    val label: String,
    val normal: Map<FlickDirection, FlickNode>,
    val dakuten: Map<FlickDirection, FlickNode>? = null,
    val handakuten: Map<FlickDirection, FlickNode>? = null,
) {
    /** The first stage of this key in [mode]; a row without だくてん stays plain. */
    fun map(mode: KanaMode): Map<FlickDirection, FlickNode> = when (mode) {
        KanaMode.NORMAL -> normal
        KanaMode.DAKUTEN -> dakuten ?: normal
        KanaMode.HANDAKUTEN -> handakuten ?: normal
    }
}

/** The kana a node stands for: an input node's kana, or a stage's own kana. */
val FlickNode.text: String
    get() = when (this) {
        is FlickNode.Input -> text
        is FlickNode.Branch -> label
    }
