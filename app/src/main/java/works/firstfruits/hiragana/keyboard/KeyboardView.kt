package works.firstfruits.hiragana.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/** Draws a [KeyboardLayout] and reports taps and flicks to [listener]. */
class KeyboardView(context: Context) : View(context) {

    fun interface Listener {
        /** [direction] is CENTER except for a flick on a Kana key. */
        fun onKey(key: Key, direction: FlickDirection)
    }

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

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val specialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 20 * density
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    init {
        applyTheme()
    }

    /** Copies the theme colors into the paints, so onDraw never allocates. */
    private fun applyTheme() {
        keyPaint.color = theme.key
        pressedPaint.color = theme.pressedKey
        specialPaint.color = theme.specialKey
        labelPaint.color = theme.keyText
        hintPaint.color = theme.hintText
        guideCellPaint.color = theme.guideBackground
        guideSelectedPaint.color = theme.guideSelected
        guideTextPaint.color = theme.guideText
        setBackgroundColor(theme.keyboardBackground)
    }

    /** True for keys that are not typed text (convert, enter, backspace and so on). */
    private fun isSpecial(key: Key): Boolean =
        key.action !is KeyAction.Letter && key.action !is KeyAction.Kana && key.action !is KeyAction.Text

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
            val label = if (shifted && key.action is KeyAction.Letter) key.label.uppercase() else key.label
            canvas.drawText(label, rect.centerX(), baseline(rect, labelPaint), labelPaint)
            if (key.hint.isNotEmpty()) canvas.drawText(key.hint, rect.centerX(), rect.bottom - 4 * density, hintPaint)
        }
        val pressedKey = pressed ?: return
        val (key, rect) = pressedKey
        when (val action = key.action) {
            is KeyAction.Kana -> drawFlickGuide(canvas, rect, action, radius)
            is KeyAction.Letter -> drawLetterPreview(canvas, rect, key, radius)
            else -> {}
        }
    }

    /** Draws the five flick cells around [rect] and highlights [flickDirection]. */
    private fun drawFlickGuide(canvas: Canvas, rect: RectF, action: KeyAction.Kana, radius: Float) {
        val width = rect.width()
        val height = rect.height()
        for (cell in FlickGuide.cells(action, rect.centerX(), rect.centerY(), width, height)) {
            cellRect.set(
                cell.centerX - width / 2,
                cell.centerY - height / 2,
                cell.centerX + width / 2,
                cell.centerY + height / 2,
            )
            val paint = if (cell.direction == flickDirection) guideSelectedPaint else guideCellPaint
            canvas.drawRoundRect(cellRect, radius, radius, paint)
            canvas.drawText(cell.kana, cellRect.centerX(), baseline(cellRect, guideTextPaint), guideTextPaint)
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = bounds.firstOrNull { it.second.contains(event.x, event.y) }
                downX = event.x
                downY = event.y
                flickDirection = FlickDirection.CENTER
                repeated = false
                if (pressed?.first?.repeatable == true) repeatHandler.postDelayed(repeater, REPEAT_DELAY_MS)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val key = pressed?.first
                if (key != null) {
                    flickDirection = if (key.action is KeyAction.Kana) {
                        FlickResolver.resolve(event.x - downX, event.y - downY, flickThreshold)
                    } else {
                        FlickDirection.CENTER
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                repeatHandler.removeCallbacks(repeater)
                val key = pressed?.first
                pressed = null
                invalidate()
                if (key != null && !repeated) {
                    val direction = if (key.action is KeyAction.Kana) {
                        FlickResolver.resolve(event.x - downX, event.y - downY, flickThreshold)
                    } else {
                        FlickDirection.CENTER
                    }
                    listener?.onKey(key, direction)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                repeatHandler.removeCallbacks(repeater)
                pressed = null
                invalidate()
            }
        }
        return true
    }

    companion object {
        private const val REPEAT_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 50L
    }
}
