package works.firstfruits.hiragana.keyboard

/**
 * Follows one three-stage flick: which kana key is pressed, which stage is open
 * and which kana would be inserted if the finger came up now.
 *
 * The view feeds it the finger positions; the decisions are plain Kotlin, so
 * they can be checked without a device. Every cell of the guide is drawn around
 * the key, and the finger picks a cell by moving that way from the point it
 * pressed: a flick to a side takes that kana, a flick to a corner takes the
 * だくてん or the small kana, and a move from there opens the next stage
 * (き→きゅ→きゅう).
 *
 * The directions are always measured from the point of the press, never from
 * where a stage happened to open, so a finger that stays on the key keeps the
 * kana that is shown and the guide cannot flicker between two stages.
 */
class FlickSession(private val threshold: Float) {

    private var key: FlickKey? = null
    private var mode = KanaMode.NORMAL
    private val path = mutableListOf<FlickDirection>()

    /** The point the finger went down on; the directions are measured from it. */
    private var startX = 0f
    private var startY = 0f

    private var stageMap: Map<FlickDirection, FlickNode> = emptyMap()
    private var parentMap: Map<FlickDirection, FlickNode>? = null
    private var parentBranch: FlickNode.Branch? = null

    /** True while a flick is being followed. */
    var active = false
        private set

    /** The kana that [release] would insert, or null when the stage has none. */
    var current: String? = null
        private set

    /** The direction chosen in the open stage, or null when none is yet. */
    var highlight: FlickDirection? = null
        private set

    /** Starts following a flick on [key] with the finger down at ([x], [y]). */
    fun press(key: FlickKey, x: Float, y: Float) {
        this.key = key
        mode = KanaMode.NORMAL
        path.clear()
        startX = x
        startY = y
        active = true
        rebuild()
    }

    /**
     * Follows the finger to ([x], [y]); returns true when the shown stage, the
     * highlight or the chosen kana changed, so that the guide can be redrawn.
     * A finger that stays within [threshold] of the press does not change
     * anything, so coming back to the key keeps the kana of the open stage.
     */
    fun move(x: Float, y: Float): Boolean {
        key ?: return false
        val dx = x - startX
        val dy = y - startY
        if (FlickResolver.isTap(dx, dy, threshold)) return false
        val direction = FlickResolver.closest(dx, dy, options().keys) ?: return false
        if (direction == highlight) return false
        return push(direction)
    }

    /** The kana to insert for this flick, and ends the flick. */
    fun release(): String? {
        val kana = current
        cancel()
        return kana
    }

    /** Forgets the flick, e.g. when it was cancelled or already used. */
    fun cancel() {
        active = false
        key = null
        path.clear()
        stageMap = emptyMap()
        parentMap = null
        parentBranch = null
        highlight = null
        current = null
        mode = KanaMode.NORMAL
    }

    /**
     * The directions that do something in the open stage, together with the ones
     * the stage above keeps reachable through
     * [FlickNode.Branch.continuations] (so きゅ leads on to きゃ and きょ without
     * going back to き first).
     */
    fun options(): Map<FlickDirection, FlickNode> {
        val branch = parentBranch ?: return stageMap
        val entry = path.lastOrNull() ?: return stageMap
        val parent = parentMap ?: return stageMap
        val extra = branch.continuations
            .filter { it != entry }
            .mapNotNull { direction -> parent[direction]?.let { direction to it } }
        return if (extra.isEmpty()) stageMap else extra.toMap() + stageMap
    }

    // --- the stages ---

    private fun push(direction: FlickDirection): Boolean {
        if (parentBranch != null && !stageMap.containsKey(direction)) {
            // Only the stage above knows this direction (きゅ→きゃ): go back to it
            // first, so that the move starts from there.
            path.removeAt(path.lastIndex)
            rebuild()
        }
        path += direction
        if (rebuild()) return true
        path.removeAt(path.lastIndex)
        rebuild()
        return false
    }

    private fun rebuild(): Boolean {
        val key = key ?: return false
        if (!applyPath(key.map(mode))) return false
        switchModeIfNeeded(key)
        return true
    }

    /** Walks [path] from [root] and remembers the stage it ends on. */
    private fun applyPath(root: Map<FlickDirection, FlickNode>): Boolean {
        var map = root
        var parent: Map<FlickDirection, FlickNode>? = null
        var branch: FlickNode.Branch? = null
        var chosen: FlickDirection? = null
        var label = ""
        for (direction in path) {
            val node = map[direction] ?: return false
            chosen = direction
            if (node is FlickNode.Branch) {
                parent = map
                branch = node
                map = node.children
                label = node.label
            }
        }
        val node = chosen?.let { map[it] } ?: map[FlickDirection.CENTER]
        stageMap = map
        parentMap = parent
        parentBranch = branch
        highlight = chosen
        // A direction may belong to the stage above (きゅ→きゃ), so look the kana
        // up in the stage that is shown rather than in the one that was walked.
        val shown = options()
        val chosenNode = chosen?.let { shown[it] } ?: shown[FlickDirection.CENTER]
        current = chosenNode?.text ?: node?.text ?: label
        return true
    }

    /**
     * Picks a kana of another row (か→が) and swaps the stages to that row, so
     * that the next moves lead to ぎゃ and the like. Does nothing when the new
     * row has no matching stage.
     */
    private fun switchModeIfNeeded(key: FlickKey) {
        var guard = 0
        while (guard++ < 3) {
            val node = highlight?.let { stageMap[it] } ?: stageMap[FlickDirection.CENTER]
            val next = (node as? FlickNode.Input)?.nextMode ?: return
            if (next == mode) return
            val previous = mode
            mode = next
            if (!applyPath(key.map(mode))) {
                mode = previous
                applyPath(key.map(previous))
                return
            }
        }
    }
}
