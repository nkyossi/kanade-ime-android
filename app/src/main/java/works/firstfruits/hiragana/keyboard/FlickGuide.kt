package works.firstfruits.hiragana.keyboard

/**
 * The cells of the flick guide drawn around a pressed kana key: the centre plus
 * one cell for each direction. Coordinates are plain floats so the layout can be
 * checked in JVM unit tests (RectF is not available there).
 */
object FlickGuide {
    /** A guide cell: the kana to show, its direction and where its centre is. */
    data class Cell(
        val direction: FlickDirection,
        val kana: String,
        val centerX: Float,
        val centerY: Float,
    )

    /**
     * The cells to draw for [action], with the pressed key centred at
     * ([centerX], [centerY]) and sized [width] x [height]. A null kana is skipped.
     */
    fun cells(
        action: KeyAction.Kana,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
    ): List<Cell> {
        fun cell(direction: FlickDirection, x: Float, y: Float): Cell? {
            val kana = action.kanaFor(direction) ?: return null
            return Cell(direction, kana, x, y)
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
