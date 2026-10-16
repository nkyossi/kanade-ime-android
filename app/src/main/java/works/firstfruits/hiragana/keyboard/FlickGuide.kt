package works.firstfruits.hiragana.keyboard

/**
 * The cells of the flick guide drawn around a pressed key: for the simple keys
 * the centre and the four sides, for a kana key with three-stage flick a 3x3
 * square of the stage that is open. Coordinates are plain floats so the layout
 * can be checked in JVM unit tests (RectF is not available there).
 */
object FlickGuide {
    /** A guide cell: the text to show, where its centre is and how big it is. */
    data class Cell(
        val label: String,
        val centerX: Float,
        val centerY: Float,
        val width: Float,
        val height: Float,
        val selected: Boolean,
    )

    /**
     * The cells to draw for [labels] (CENTER, LEFT, UP, RIGHT, DOWN), with the
     * pressed key centred at ([centerX], [centerY]) and sized [width] x [height].
     * A null label is skipped.
     */
    fun plus(
        labels: List<String?>,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        selected: FlickDirection,
    ): List<Cell> {
        fun cell(direction: FlickDirection, x: Float, y: Float): Cell? {
            val label = labels.getOrNull(direction.ordinal) ?: return null
            return Cell(label, x, y, width, height, direction == selected)
        }
        return listOfNotNull(
            cell(FlickDirection.CENTER, centerX, centerY),
            cell(FlickDirection.LEFT, centerX - width, centerY),
            cell(FlickDirection.UP, centerX, centerY - height),
            cell(FlickDirection.RIGHT, centerX + width, centerY),
            cell(FlickDirection.DOWN, centerX, centerY + height),
        )
    }

    /** How much smaller a cell of a stage guide is than the key it belongs to. */
    const val STAGE_SCALE = 0.5f

    /** How much of a stage cell is filled, so that neighbouring cells show a gap. */
    private const val STAGE_FILL = 0.9f

    /**
     * How far a whole stage guide reaches from the middle of its key, as a
     * fraction of the key size: the step to the outer cells plus half a cell.
     */
    const val STAGE_REACH = STAGE_SCALE * (1 + STAGE_FILL / 2)

    /**
     * The cells of the open stage of a three-stage flick: [center] in the middle
     * and the kana of [directions] around it, at the side or corner each one is
     * reached from. [selected] is the direction chosen so far.
     */
    fun stage(
        center: String,
        directions: Map<FlickDirection, String>,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        selected: FlickDirection?,
    ): List<Cell> {
        val stepX = width * STAGE_SCALE
        val stepY = height * STAGE_SCALE
        val cellWidth = stepX * STAGE_FILL
        val cellHeight = stepY * STAGE_FILL
        val cells = mutableListOf(
            Cell(center, centerX, centerY, cellWidth, cellHeight, selected == null),
        )
        for ((direction, label) in directions) {
            val offset = offsetOf(direction) ?: continue
            cells += Cell(
                label,
                centerX + offset.first * stepX,
                centerY + offset.second * stepY,
                cellWidth,
                cellHeight,
                direction == selected,
            )
        }
        return cells
    }

    /**
     * The cells moved so that the whole guide of a key whose middle is
     * ([centerX], [centerY]) and which reaches ([reachX], [reachY]) to each side
     * lies inside ([left], [top])-([right], [bottom]), so a guide on an edge key
     * stays readable. The whole reach is measured and not only the cells that
     * are showing, so the guide keeps its place when another stage opens.
     */
    fun fit(
        cells: List<Cell>,
        centerX: Float,
        centerY: Float,
        reachX: Float,
        reachY: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): List<Cell> {
        if (cells.isEmpty()) return cells
        var dx = (left - (centerX - reachX)).coerceAtLeast(0f)
        dx -= ((centerX + reachX + dx) - right).coerceAtLeast(0f)
        var dy = (top - (centerY - reachY)).coerceAtLeast(0f)
        dy -= ((centerY + reachY + dy) - bottom).coerceAtLeast(0f)
        if (dx == 0f && dy == 0f) return cells
        return cells.map { it.copy(centerX = it.centerX + dx, centerY = it.centerY + dy) }
    }

    /** Which of the 3x3 cells a direction uses; null for the centre. */
    private fun offsetOf(direction: FlickDirection): Pair<Int, Int>? = when (direction) {
        FlickDirection.CENTER -> null
        FlickDirection.LEFT -> -1 to 0
        FlickDirection.RIGHT -> 1 to 0
        FlickDirection.UP -> 0 to -1
        FlickDirection.DOWN -> 0 to 1
        FlickDirection.UP_LEFT -> -1 to -1
        FlickDirection.UP_RIGHT -> 1 to -1
        FlickDirection.DOWN_LEFT -> -1 to 1
        FlickDirection.DOWN_RIGHT -> 1 to 1
    }
}
