package works.firstfruits.hiragana.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

/** Draws a [KeyboardLayout] and reports taps and flicks to [listener]. */
class KeyboardView(context: Context) : View(context) {

    interface Listener {
        /** [direction] is CENTER except for a flick on a Kana key. */
        fun onKey(key: Key, direction: FlickDirection)

        /** A three-stage flick chose [kana]; it is inserted like a plain kana key. */
        fun onKana(kana: String) {}

        /** The finger has stayed on a key that has a long press; its tap is not sent then. */
        fun onLongPress(key: Key) {}
    }

    /** A flick guide: the cells to draw over the keyboard, and how to draw them. */
    data class Guide(
        val cells: List<FlickGuide.Cell>,
        val textSizeDp: Float,
        /** True for the 3x3 guides, which are kept inside the available area. */
        val contained: Boolean,
    )

    var listener: Listener? = null

    var layout: KeyboardLayout = QwertyLayout.layout
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    /** Shows the letter keys in upper case while the one-shot shift is on. */
    var shifted = false
        set(value) {
            field = value
            invalidate()
        }

    /** The keyboard colors; dark when the screen is in night mode. */
    var theme: KeyboardTheme = KeyboardTheme.forContext(context)
        set(value) {
            field = value
            applyTheme()
            invalidate()
        }

    /** Shows the shift key as caps lock (⇪) while the shift is locked. */
    var capsLock = false
        set(value) {
            field = value
            invalidate()
        }

    /** The height of a key row in dp, clamped to [ROW_HEIGHT_DP_MIN]..[ROW_HEIGHT_DP_MAX]. */
    var rowHeightDp: Float = 52f
        set(value) {
            field = clampRowHeightDp(value)
            requestLayout()
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val rowHeight get() = rowHeightDp * density
    private val gap = 3 * density
    private val flickThreshold = 24 * density

    /**
     * Whether the kana keys follow the eight directions and the stages beyond the
     * first, like スミレ入力. When it is off a kana key only knows the five
     * directions of [KeyAction.Kana.flicks].
     */
    var threeStageFlick = true
        set(value) {
            if (field == value) return
            field = value
            session.cancel()
            invalidateGuide()
        }

    /** Follows the three-stage flick of the key that is being pressed. */
    private val session = FlickSession(flickThreshold)

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val specialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = LABEL_SIZE_DP * density
    }
    private val flickLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 9 * density
    }
    private val guideCellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guideSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guideTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 20 * density
    }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x80, 0x80, 0x80)
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    private val previewTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x20, 0x20, 0x20)
        textAlign = Paint.Align.CENTER
        textSize = 40 * density
    }
    private val cellRect = RectF()
    private val bubbleRect = RectF()

    /** Key icons, loaded once per resource id. */
    private val icons = mutableMapOf<Int, Drawable>()

    private var bounds: List<Pair<Key, RectF>> = emptyList()
    private var pressed: Pair<Key, RectF>? = null
    private var downX = 0f
    private var downY = 0f
    private var topRowTop = 0f
    private var flickDirection = FlickDirection.CENTER

    private val repeatHandler = Handler(Looper.getMainLooper())
    private var repeated = false
    private val repeater = object : Runnable {
        override fun run() {
            val key = pressed?.first ?: return
            repeated = true
            listener?.onKey(key, FlickDirection.CENTER)
            repeatHandler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    /** Fires the long press of the pressed key when the finger has stayed on it. */
    private val longPresser = Runnable {
        val key = pressed?.first ?: return@Runnable
        // The long press has acted, so the release must not tap the key again.
        repeated = true
        listener?.onLongPress(key)
    }

    init {
        applyTheme()
    }

    /** Copies the theme colors into the paints, so onDraw never allocates. */
    private fun applyTheme() {
        keyPaint.color = theme.key
        pressedPaint.color = theme.pressedKey
        specialPaint.color = theme.specialKey
        labelPaint.color = theme.keyText
        flickLabelPaint.color = theme.hintText
        guideCellPaint.color = theme.guideBackground
        guideSelectedPaint.color = theme.guideSelected
        guideTextPaint.color = theme.guideText
        setBackgroundColor(theme.keyboardBackground)
    }

    /** True for keys that are not typed text (convert, enter, backspace and so on). */
    private fun isSpecial(key: Key): Boolean =
        key.action !is KeyAction.Letter && key.action !is KeyAction.Kana && key.action !is KeyAction.Text

    /** True for keys that choose what they do by the flick direction. */
    private fun isFlickable(action: KeyAction): Boolean =
        action is KeyAction.Kana || action is KeyAction.Flick ||
            (action is KeyAction.Letter && action.down != null)

    /** True for the keys that have a long press. */
    private fun hasLongPress(key: Key): Boolean = (key.action as? KeyAction.Letter)?.long != null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (layout.rows.size * rowHeight + gap).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val result = mutableListOf<Pair<Key, RectF>>()
        val w = (right - left).toFloat()
        layout.rows.forEachIndexed { r, row ->
            val total = row.keys.sumOf { it.width.toDouble() }.toFloat()
            var x = 0f
            val y = r * rowHeight
            for (key in row.keys) {
                val kw = w * key.width / total
                result += key to RectF(x + gap, y + gap, x + kw, y + rowHeight)
                x += kw
            }
        }
        bounds = result
        topRowTop = result.minOfOrNull { it.second.top } ?: 0f
    }

    override fun onDraw(canvas: Canvas) {
        val radius = 6 * density
        for ((key, rect) in bounds) {
            val paint = when {
                pressed?.first === key -> pressedPaint
                isSpecial(key) -> specialPaint
                else -> keyPaint
            }
            canvas.drawRoundRect(rect, radius, radius, paint)
            if (key.iconRes != 0) {
                drawIcon(canvas, rect, key.iconRes)
            } else {
                val label = when {
                    shifted && key.action is KeyAction.Letter -> key.label.uppercase()
                    capsLock && key.action == KeyAction.Shift -> CAPS_LOCK_LABEL
                    else -> key.label
                }
                // Shrink a label wider than its key, e.g. 「12キー」 on a narrow screen.
                labelPaint.textSize = LABEL_SIZE_DP * density
                val room = rect.width() - 6 * density
                val width = labelPaint.measureText(label)
                if (room < width) labelPaint.textSize *= room / width
                canvas.drawText(label, rect.centerX(), baseline(rect, labelPaint), labelPaint)
                drawFlickLabels(canvas, key, rect)
            }
        }
        val pressedKey = pressed ?: return
        val (key, rect) = pressedKey
        // The flick guide is drawn by the parent (KeyboardInputView) so that the
        // guide of a top row key overlaps the candidate bar instead of being cut
        // off by it; see guide and drawGuide().
        if (key.action is KeyAction.Letter) drawLetterPreview(canvas, rect, key, radius)
    }

    /** The flick guide of the pressed key, or null when the key has none to show. */
    val guide: Guide?
        get() {
            val (key, rect) = pressed ?: return null
            val tree = treeOf(key)
            if (tree != null && session.active) {
                val directions = LinkedHashMap<FlickDirection, String>()
                for ((direction, node) in session.options()) {
                    if (direction == FlickDirection.CENTER) continue
                    directions[direction] = node.text
                }
                return Guide(
                    FlickGuide.stage(
                        session.current ?: tree.label,
                        directions,
                        rect.centerX(),
                        rect.centerY(),
                        rect.width(),
                        rect.height(),
                        session.highlight,
                    ),
                    STAGE_TEXT_SIZE_DP,
                    contained = true,
                )
            }
            val labels = when (val action = key.action) {
                is KeyAction.Kana -> action.flicks
                is KeyAction.Flick -> action.labels
                // A letter with a down flick shows its digit in the guide only.
                is KeyAction.Letter -> listOf(null, null, null, null, (action.down ?: return null).toString())
                else -> return null
            }
            return Guide(
                FlickGuide.plus(
                    labels,
                    rect.centerX(),
                    rect.centerY(),
                    rect.width(),
                    rect.height(),
                    flickDirection,
                ),
                LABEL_SIZE_DP,
                contained = false,
            )
        }

    /** The stages of [key], or null when it has none or they are turned off. */
    private fun treeOf(key: Key): FlickKey? =
        if (threeStageFlick) (key.action as? KeyAction.Kana)?.tree else null

    /**
     * Draws [guide]: the parent calls this after its children, over the candidate
     * bar. ([left], [top])-([right], [bottom]) is the area the guide has, in the
     * coordinates of this view, for the guides that have to stay inside it.
     */
    fun drawGuide(canvas: Canvas, guide: Guide, left: Float, top: Float, right: Float, bottom: Float) {
        val rect = pressed?.second
        val cells = if (guide.contained && rect != null) {
            val reachX = rect.width() * FlickGuide.STAGE_REACH
            val reachY = rect.height() * FlickGuide.STAGE_REACH
            FlickGuide.fit(
                guide.cells,
                rect.centerX(),
                rect.centerY(),
                reachX,
                reachY,
                left,
                top,
                right,
                bottom,
            )
        } else {
            guide.cells
        }
        drawGuideCells(canvas, cells, guide.textSizeDp * density)
    }

    /** The view that draws the flick guide on top of the keyboard. */
    var guideOverlay: View? = null

    private fun invalidateGuide() {
        invalidate()
        guideOverlay?.invalidate()
    }

    /** Draws the cells of a guide, each label shrunk to fit its cell. */
    private fun drawGuideCells(canvas: Canvas, cells: List<FlickGuide.Cell>, textSize: Float) {
        val radius = 6 * density
        for (cell in cells) {
            cellRect.set(
                cell.centerX - cell.width / 2,
                cell.centerY - cell.height / 2,
                cell.centerX + cell.width / 2,
                cell.centerY + cell.height / 2,
            )
            val paint = if (cell.selected) guideSelectedPaint else guideCellPaint
            canvas.drawRoundRect(cellRect, radius, radius, paint)
            guideTextPaint.textSize = textSize
            val room = cellRect.width() - 4 * density
            val width = guideTextPaint.measureText(cell.label)
            if (room < width) guideTextPaint.textSize *= room / width
            canvas.drawText(cell.label, cellRect.centerX(), baseline(cellRect, guideTextPaint), guideTextPaint)
        }
    }

    /** Draws the enlarged label in a bubble above the key, or inside a top row key. */
    private fun drawLetterPreview(canvas: Canvas, rect: RectF, key: Key, radius: Float) {
        val label = if (shifted) key.label.uppercase() else key.label
        val box = LetterPreview.above(rect.left, rect.top, rect.right, rect.bottom, topRowTop)
        if (box == null) {
            canvas.drawText(label, rect.centerX(), baseline(rect, previewTextPaint), previewTextPaint)
            return
        }
        bubbleRect.set(box.left, box.top, box.right, box.bottom)
        canvas.drawRoundRect(bubbleRect, radius, radius, bubblePaint)
        canvas.drawRoundRect(bubbleRect, radius, radius, bubbleBorderPaint)
        canvas.drawText(label, bubbleRect.centerX(), baseline(bubbleRect, previewTextPaint), previewTextPaint)
    }

    /** The baseline that centres [textPaint] vertically in [rect]. */
    private fun baseline(rect: RectF, textPaint: Paint): Float =
        rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2

    /**
     * Draws the faint kana of a flickable key small, at the side each one is
     * reached from, so the flicks of a key can be read before it is pressed.
     */
    private fun drawFlickLabels(canvas: Canvas, key: Key, rect: RectF) {
        val labels = when (val action = key.action) {
            is KeyAction.Kana -> action.flicks
            is KeyAction.Flick -> action.labels
            else -> return
        }
        fun side(direction: FlickDirection, x: Float, y: Float, align: Paint.Align) {
            val label = labels.getOrNull(direction.ordinal) ?: return
            flickLabelPaint.textAlign = align
            canvas.drawText(label, x, y, flickLabelPaint)
        }
        val margin = 2 * density
        val middle = baseline(rect, flickLabelPaint)
        side(FlickDirection.LEFT, rect.left + margin, middle, Paint.Align.LEFT)
        side(FlickDirection.RIGHT, rect.right - margin, middle, Paint.Align.RIGHT)
        side(FlickDirection.UP, rect.centerX(), rect.top + margin - flickLabelPaint.ascent(), Paint.Align.CENTER)
        side(FlickDirection.DOWN, rect.centerX(), rect.bottom - margin, Paint.Align.CENTER)
        flickLabelPaint.textAlign = Paint.Align.CENTER
    }

    /** Draws the icon of a key, e.g. the clipboard of ペースト, centred in [rect]. */
    private fun drawIcon(canvas: Canvas, rect: RectF, iconRes: Int) {
        val icon = icons.getOrPut(iconRes) { context.getDrawable(iconRes)!!.mutate() }
        val size = min(rect.width(), rect.height()) * ICON_SCALE
        icon.setBounds(
            (rect.centerX() - size / 2).toInt(),
            (rect.centerY() - size / 2).toInt(),
            (rect.centerX() + size / 2).toInt(),
            (rect.centerY() + size / 2).toInt(),
        )
        icon.setTint(labelPaint.color)
        icon.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // A new finger finishes the key of the previous one, so that
                // fast typing with two thumbs does not lose keys.
                release()
                val i = event.actionIndex
                press(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_MOVE -> {
                val i = event.findPointerIndex(pointerId)
                val key = pressed?.first
                if (key != null && 0 <= i) {
                    lastX = event.getX(i)
                    lastY = event.getY(i)
                    // A finger that moves on is a flick, not a long press.
                    if (abs(lastX - downX) > flickThreshold || abs(lastY - downY) > flickThreshold) {
                        repeatHandler.removeCallbacks(longPresser)
                    }
                    if (session.active) {
                        if (session.move(lastX, lastY)) invalidateGuide()
                    } else {
                        flickDirection = if (isFlickable(key.action)) {
                            FlickResolver.resolve(lastX - downX, lastY - downY, flickThreshold)
                        } else {
                            FlickDirection.CENTER
                        }
                        invalidateGuide()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = event.actionIndex
                if (event.getPointerId(i) == pointerId) {
                    lastX = event.getX(i)
                    lastY = event.getY(i)
                    release()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                repeatHandler.removeCallbacks(repeater)
                repeatHandler.removeCallbacks(longPresser)
                pressed = null
                pointerId = NO_POINTER
                session.cancel()
                invalidateGuide()
            }
        }
        return true
    }

    private var pointerId = NO_POINTER
    private var lastX = 0f
    private var lastY = 0f

    private fun press(id: Int, x: Float, y: Float) {
        pointerId = id
        pressed = bounds.firstOrNull { it.second.contains(x, y) }
        downX = x
        downY = y
        lastX = x
        lastY = y
        flickDirection = FlickDirection.CENTER
        repeated = false
        val tree = pressed?.first?.let { treeOf(it) }
        if (tree == null) session.cancel() else session.press(tree, x, y)
        if (pressed?.first?.repeatable == true) repeatHandler.postDelayed(repeater, REPEAT_DELAY_MS)
        repeatHandler.removeCallbacks(longPresser)
        pressed?.first?.takeIf { hasLongPress(it) }?.let { repeatHandler.postDelayed(longPresser, LONG_PRESS_MS) }
        invalidateGuide()
    }

    /** Sends the pressed key, if any, as of the last position of its finger. */
    private fun release() {
        repeatHandler.removeCallbacks(repeater)
        repeatHandler.removeCallbacks(longPresser)
        val key = pressed?.first
        pressed = null
        pointerId = NO_POINTER
        invalidateGuide()
        if (session.active) {
            // A quick flick may end before any MOVE event arrives, so let the
            // session see where the finger came up.
            session.move(lastX, lastY)
            val kana = session.release()
            if (kana != null && !repeated) listener?.onKana(kana)
            return
        }
        if (key != null && !repeated) {
            val direction = if (isFlickable(key.action)) {
                FlickResolver.resolve(lastX - downX, lastY - downY, flickThreshold)
            } else {
                FlickDirection.CENTER
            }
            listener?.onKey(key, direction)
        }
    }

    companion object {
        private const val NO_POINTER = -1
        private const val LABEL_SIZE_DP = 20f
        private const val STAGE_TEXT_SIZE_DP = 16f
        private const val ICON_SCALE = 0.5f
        private const val REPEAT_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 50L
        private const val LONG_PRESS_MS = 400L
        private const val CAPS_LOCK_LABEL = "⇪"
    }
}
