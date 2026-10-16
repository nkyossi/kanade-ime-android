package works.firstfruits.hiragana.keyboard

/**
 * The cells of the flick guide drawn around a pressed kana key: the centre plus
 * one cell for each direction. Coordinates are plain floats so the layout can be
 * checked in JVM unit tests (RectF is not available there).
 */
object FlickGuide {
    /** A guide cell: the text to show, its direction and where its centre is. */
    data class Cell(
        val direction: FlickDirection,
        val label: String,
        val centerX: Float,
        val centerY: Float,
    )

    /**
     * The cells to draw for [labels] (CENTER, LEFT, UP, RIGHT, DOWN), with the
     * pressed key centred at ([centerX], [centerY]) and sized [width] x [height].
     * A null label is skipped.
     */
    fun cells(
        labels: List<String?>,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
    ): List<Cell> {
        fun cell(direction: FlickDirection, x: Float, y: Float): Cell? {
            val label = labels.getOrNull(direction.ordinal) ?: return null
            return Cell(direction, label, x, y)
        }
        return listOfNotNull(
            cell(FlickDirection.CENTER, centerX, centerY),
            cell(FlickDirection.LEFT, centerX - width, centerY),
            cell(FlickDirection.UP, centerX, centerY - height),
            cell(FlickDirection.RIGHT, centerX + width, centerY),
            cell(FlickDirection.DOWN, centerX, centerY + height),
        )
    }
}
