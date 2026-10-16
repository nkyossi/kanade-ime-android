package works.firstfruits.hiragana.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/** A row of conversion candidates above the keyboard; also shows the input mode. */
class CandidateBar(context: Context) : LinearLayout(context) {

    fun interface Listener {
        fun onCandidate(index: Int)
    }

    var listener: Listener? = null

    /** The bar colors; dark when the screen is in night mode. */
    var theme: KeyboardTheme = KeyboardTheme.forContext(context)
        set(value) {
            field = value
            applyTheme()
        }

    private val density = resources.displayMetrics.density
    private val modeView = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
    }
    private val list = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val scroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(list)
    }
    private var cursor = -1

    init {
        orientation = HORIZONTAL
        val height = (44 * density).toInt()
        addView(modeView, LayoutParams((44 * density).toInt(), height))
        addView(scroll, LayoutParams(0, height, 1f))
        applyTheme()
    }

    /** Applies the theme to the bar and to the candidates already shown. */
    private fun applyTheme() {
        setBackgroundColor(theme.candidateBackground)
        modeView.setTextColor(theme.modeText)
        for (i in 0 until list.childCount) {
            val view = list.getChildAt(i) as? TextView ?: continue
            view.setTextColor(theme.candidateText)
            view.setBackgroundColor(if (i == cursor) theme.selectedCandidate else Color.TRANSPARENT)
        }
    }

    /** Shows the input mode, or hides it when [label] is null. */
    fun setMode(label: String?) {
        modeView.text = label.orEmpty()
        modeView.visibility = if (label == null) GONE else VISIBLE
    }

    fun setCandidates(candidates: List<String>, cursor: Int) {
        this.cursor = cursor
        list.removeAllViews()
        candidates.forEachIndexed { i, text ->
            val view = TextView(context).apply {
                this.text = text
                textSize = 18f
                gravity = Gravity.CENTER
                val pad = (12 * density).toInt()
                setPadding(pad, 0, pad, 0)
                setTextColor(theme.candidateText)
                setBackgroundColor(if (i == cursor) theme.selectedCandidate else Color.TRANSPARENT)
                setOnClickListener { listener?.onCandidate(i) }
            }
            list.addView(view, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        if (cursor in 0 until list.childCount) {
            list.post { list.getChildAt(cursor)?.let { scroll.smoothScrollTo(maxOf(0, it.left - it.width), 0) } }
        } else {
            scroll.scrollTo(0, 0)
        }
    }
}
