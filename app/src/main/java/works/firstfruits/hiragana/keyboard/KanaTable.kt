package works.firstfruits.hiragana.keyboard

import works.firstfruits.hiragana.keyboard.FlickDirection.CENTER
import works.firstfruits.hiragana.keyboard.FlickDirection.DOWN
import works.firstfruits.hiragana.keyboard.FlickDirection.DOWN_LEFT
import works.firstfruits.hiragana.keyboard.FlickDirection.DOWN_RIGHT
import works.firstfruits.hiragana.keyboard.FlickDirection.LEFT
import works.firstfruits.hiragana.keyboard.FlickDirection.RIGHT
import works.firstfruits.hiragana.keyboard.FlickDirection.UP
import works.firstfruits.hiragana.keyboard.FlickDirection.UP_LEFT
import works.firstfruits.hiragana.keyboard.FlickDirection.UP_RIGHT

/**
 * The three-stage flick tables of the 12-key layout, the same kana as スミレ入力:
 * だくてん and はんだくてん on the corners, ようおん (きゃ・きゅ・きょ) and the long
 * vowels (きゅう) through the further stages, small kana and the numbers on the
 * corners of the あ row and so on.
 *
 * Each key starts with one map per [KanaMode]; the gesture moves between the
 * modes as it picks a kana such as ぎ, and then ぎゃ is one turn away.
 */
object KanaTable {
    /** The stages of the key whose first kana is [label]. */
    private val keys: Map<String, FlickKey> = buildMap {
        put("あ", staticKey("あ") {
            add(CENTER, input("あ"))
            add(UP_RIGHT, modeBranch("ぁ", "あ", UP_RIGHT to "ぁ"))
            add(LEFT, branch("い") { add(LEFT, input("い")); add(DOWN_LEFT, input("ぃ")) })
            add(UP, branch("う") {
                add(UP, input("う"))
                add(UP_LEFT, input("ぅ"))
                add(UP_RIGHT, input("ゔ"))
            })
            add(RIGHT, branch("え") { add(RIGHT, input("え")); add(DOWN_RIGHT, input("ぇ")) })
            add(DOWN, branch("お") {
                add(DOWN, input("お"))
                add(DOWN_RIGHT, input("ぉ"))
                add(DOWN_LEFT, input("1"))
            })
        })
        put("か", row(Spec("か", "2", Series("か", "き", "く", "け", "こ"), dakuten = Series("が", "ぎ", "ぐ", "げ", "ご"))))
        put("さ", row(Spec("さ", "3", Series("さ", "し", "す", "せ", "そ"), dakuten = Series("ざ", "じ", "ず", "ぜ", "ぞ"))))
        put("た", row(Spec(
            "た", "4", Series("た", "ち", "つ", "て", "と"),
            dakuten = Series("だ", "ぢ", "づ", "で", "ど"),
            normalOverrides = overrides {
                u.upsert(UP_RIGHT, input("っ"))
                e.upsert(UP_RIGHT, branch("てぃ") {
                    add(UP_RIGHT, input("てぃ"))
                    add(UP, input("てぃー"))
                })
            },
            dakutenOverrides = overrides {
                u.upsert(UP_RIGHT, input("っ"))
                u.remove(LEFT)
                e.upsert(DOWN, input("でぃ"))
                e.upsert(DOWN_LEFT, input("でぃー"))
            },
        )))
        put("な", row(Spec("な", "5", Series("な", "に", "ぬ", "ね", "の"))))
        put("は", row(Spec(
            "は", "6", Series("は", "ひ", "ふ", "へ", "ほ"),
            dakuten = Series("ば", "び", "ぶ", "べ", "ぼ"),
            handakuten = Series("ぱ", "ぴ", "ぷ", "ぺ", "ぽ"),
            normalOverrides = overrides {
                u.cancelOnTap = false
                u.upsert(RIGHT, input("ふう"))
                u.upsert(LEFT, branch("ふぁ", cancelOnTap = false) {
                    add(LEFT, input("ふぁ"))
                    add(UP, input("ふぃ"))
                    add(RIGHT, input("ふぇ"))
                    add(DOWN, input("ふぉ"))
                })
                o.cancelOnTap = false
                o.upsert(RIGHT, input("ほう"))
            },
            dakutenOverrides = overrides {
                u.cancelOnTap = false
                u.upsert(UP_LEFT, input("ぶ", KanaMode.DAKUTEN))
                e.upsert(DOWN_RIGHT, input("べ", KanaMode.DAKUTEN))
                o.cancelOnTap = false
                o.upsert(DOWN_RIGHT, input("ぼ", KanaMode.DAKUTEN))
            },
            handakutenOverrides = overrides {
                u.cancelOnTap = false
                o.cancelOnTap = false
            },
        )))
        put("ま", row(Spec("ま", "7", Series("ま", "み", "む", "め", "も"))))
        put("や", staticKey("や") {
            add(CENTER, input("や"))
            add(UP_RIGHT, modeBranch("ゃ", "や", UP_RIGHT to "ゃ"))
            // スミレ入力 puts the plain brackets first here; 「」 are the ones this
            // keyboard has always had on those flicks, so they stay in front.
            add(LEFT, branch("「") { add(LEFT, input("「")); add(DOWN_LEFT, input("(")) })
            add(UP, branch("ゆ") {
                add(UP, input("ゆ"))
                add(UP_LEFT, input("ゅ"))
                add(UP_RIGHT, input("ゆう"))
            })
            add(RIGHT, branch("」") { add(RIGHT, input("」")); add(DOWN_RIGHT, input(")")) })
            add(DOWN, branch("よ") {
                add(DOWN, input("よ"))
                add(DOWN_RIGHT, input("ょ"))
                add(DOWN_LEFT, input("よう"))
                add(LEFT, input("8"))
            })
        })
        put("ら", row(Spec("ら", "9", Series("ら", "り", "る", "れ", "ろ"))))
        put("わ", staticKey("わ") {
            add(CENTER, input("わ"))
            add(UP_RIGHT, modeBranch("ゎ", "わ", UP_RIGHT to "ゎ"))
            add(LEFT, branch("を") { add(LEFT, input("を")) })
            add(UP, branch("ん") { add(UP, input("ん")) })
            add(RIGHT, branch("ー") { add(RIGHT, input("ー")) })
            add(DOWN, branch("〜") { add(DOWN, input("〜")); add(DOWN_LEFT, input("0")) })
        })
        put("、", staticKey("、") {
            add(CENTER, input("、"))
            add(LEFT, branch("。") { add(LEFT, input("。")) })
            add(UP, branch("？") {
                add(UP, input("？"))
                add(UP_LEFT, input("："))
                add(UP_RIGHT, input("・"))
            })
            add(RIGHT, branch("！") { add(RIGHT, input("！")) })
            add(DOWN, branch("…") { add(DOWN, input("…")) })
        })
    }

    /** The stages of the key labelled [label], or null when it has none. */
    fun tree(label: String): FlickKey? = keys[label]

    // --- the generated rows ---

    private data class Series(val a: String, val i: String, val u: String, val e: String, val o: String)

    private class Spec(
        val label: String,
        val number: String?,
        val normal: Series,
        val dakuten: Series? = null,
        val handakuten: Series? = null,
        val normalOverrides: Overrides = Overrides(),
        val dakutenOverrides: Overrides = Overrides(),
        val handakutenOverrides: Overrides = Overrides(),
    )

    /** What a row changes about the generated u, e or o stage. */
    private class Overrides {
        val u = Column()
        val e = Column()
        val o = Column()
    }

    private class Column {
        var cancelOnTap: Boolean? = null
        val upserts = mutableListOf<Pair<FlickDirection, FlickNode>>()
        val removals = mutableSetOf<FlickDirection>()

        fun upsert(direction: FlickDirection, node: FlickNode) {
            upserts += direction to node
        }

        fun remove(direction: FlickDirection) {
            removals += direction
        }
    }

    private fun overrides(block: Overrides.() -> Unit): Overrides = Overrides().apply(block)

    private fun row(spec: Spec): FlickKey = FlickKey(
        label = spec.label,
        normal = normalStage(spec),
        dakuten = spec.dakuten?.let { dakutenStage(spec, it) },
        handakuten = if (spec.dakuten != null && spec.handakuten != null) {
            handakutenStage(spec, spec.dakuten, spec.handakuten)
        } else {
            null
        },
    )

    private fun normalStage(spec: Spec): Map<FlickDirection, FlickNode> = stage {
        val dakuten = spec.dakuten
        val handakuten = spec.handakuten
        add(CENTER, input(spec.normal.a))
        dakuten?.let { add(UP_RIGHT, modeBranch(it.a, spec.normal.a, UP_RIGHT to it.a)) }
        handakuten?.let { add(UP_LEFT, modeBranch(it.a, spec.normal.a, UP_LEFT to it.a)) }
        add(LEFT, branch(spec.normal.i, cancelOnTap = false) {
            add(LEFT, input(spec.normal.i, KanaMode.NORMAL))
            dakuten?.let { add(DOWN_LEFT, input(it.i, KanaMode.DAKUTEN)) }
            handakuten?.let { add(UP_LEFT, input(it.i, KanaMode.HANDAKUTEN)) }
            add(UP, yoonBranch("${spec.normal.i}ゅ"))
            add(RIGHT, input("${spec.normal.i}ゃ"))
            add(DOWN, yoonBranch("${spec.normal.i}ょ"))
        })
        add(UP, branch(spec.normal.u, overrides = spec.normalOverrides.u) {
            add(UP, input(spec.normal.u))
            dakuten?.let { add(UP_LEFT, input(it.u, KanaMode.DAKUTEN)) }
            if (handakuten != null) {
                add(UP_RIGHT, input(handakuten.u, KanaMode.HANDAKUTEN))
            } else {
                add(UP_RIGHT, input("${spec.normal.u}う"))
            }
        })
        add(RIGHT, branch(spec.normal.e, overrides = spec.normalOverrides.e) {
            add(RIGHT, input(spec.normal.e))
            dakuten?.let { add(DOWN_RIGHT, input(it.e, KanaMode.DAKUTEN)) }
            handakuten?.let { add(UP_RIGHT, input(it.e, KanaMode.HANDAKUTEN)) }
        })
        add(DOWN, branch(spec.normal.o, overrides = spec.normalOverrides.o) {
            add(DOWN, input(spec.normal.o))
            dakuten?.let { add(DOWN_RIGHT, input(it.o, KanaMode.DAKUTEN)) }
            if (handakuten != null) {
                add(DOWN_LEFT, input(handakuten.o, KanaMode.HANDAKUTEN))
            } else {
                add(DOWN_LEFT, input("${spec.normal.o}う"))
            }
            spec.number?.let { add(LEFT, input(it)) }
        })
    }

    private fun dakutenStage(spec: Spec, dakuten: Series): Map<FlickDirection, FlickNode> = stage {
        val handakuten = spec.handakuten
        add(CENTER, input(spec.normal.a))
        add(UP_RIGHT, modeBranch(dakuten.a, spec.normal.a, UP_RIGHT to dakuten.a))
        handakuten?.let { add(UP_LEFT, modeBranch(it.a, spec.normal.a, UP_LEFT to it.a)) }
        add(LEFT, branch(dakuten.i, cancelOnTap = false) {
            add(DOWN_LEFT, input(dakuten.i, KanaMode.DAKUTEN))
            add(UP_LEFT, input(spec.normal.i, KanaMode.NORMAL))
            add(UP, yoonBranch("${dakuten.i}ゅ"))
            add(RIGHT, input("${dakuten.i}ゃ"))
            add(DOWN, yoonBranch("${dakuten.i}ょ"))
        })
        add(UP, branch(spec.normal.u, overrides = spec.dakutenOverrides.u) {
            add(UP_LEFT, input(dakuten.u))
            add(UP, input(spec.normal.u, KanaMode.NORMAL))
            handakuten?.let { add(UP_RIGHT, input(it.u, KanaMode.HANDAKUTEN)) }
            add(LEFT, input("${dakuten.u}う"))
        })
        add(RIGHT, branch(spec.normal.e, overrides = spec.dakutenOverrides.e) {
            add(DOWN_RIGHT, input(dakuten.e))
            add(RIGHT, input(spec.normal.e, KanaMode.NORMAL))
            handakuten?.let { add(UP_RIGHT, input(it.e, KanaMode.HANDAKUTEN)) }
        })
        add(DOWN, branch(spec.normal.o, overrides = spec.dakutenOverrides.o) {
            add(DOWN_RIGHT, input(dakuten.o))
            add(DOWN, input(spec.normal.o, KanaMode.NORMAL))
            handakuten?.let { add(DOWN_LEFT, input(it.o, KanaMode.HANDAKUTEN)) }
            add(RIGHT, input("${dakuten.o}う"))
        })
    }

    private fun handakutenStage(spec: Spec, dakuten: Series, handakuten: Series): Map<FlickDirection, FlickNode> = stage {
        add(CENTER, input(spec.normal.a))
        add(UP_RIGHT, modeBranch(dakuten.a, spec.normal.a, UP_RIGHT to dakuten.a))
        add(UP_LEFT, modeBranch(handakuten.a, spec.normal.a, UP_LEFT to handakuten.a))
        add(LEFT, branch(handakuten.i, cancelOnTap = false) {
            add(UP_LEFT, input(handakuten.i, KanaMode.HANDAKUTEN))
            add(DOWN_LEFT, input(spec.normal.i, KanaMode.NORMAL))
            add(UP, yoonBranch("${handakuten.i}ゅ"))
            add(RIGHT, input("${handakuten.i}ゃ"))
            add(DOWN, yoonBranch("${handakuten.i}ょ"))
        })
        add(UP, branch(spec.normal.u, overrides = spec.handakutenOverrides.u) {
            add(UP, input(spec.normal.u, KanaMode.NORMAL))
            add(UP_LEFT, input(dakuten.u, KanaMode.DAKUTEN))
            add(UP_RIGHT, input(handakuten.u, KanaMode.HANDAKUTEN))
            add(RIGHT, input("${handakuten.u}う"))
        })
        add(RIGHT, branch(spec.normal.e, overrides = spec.handakutenOverrides.e) {
            add(RIGHT, input(spec.normal.e, KanaMode.NORMAL))
            add(DOWN_RIGHT, input(dakuten.e, KanaMode.DAKUTEN))
            add(UP_RIGHT, input(handakuten.e, KanaMode.HANDAKUTEN))
        })
        add(DOWN, branch(spec.normal.o, overrides = spec.handakutenOverrides.o) {
            add(DOWN, input(spec.normal.o, KanaMode.NORMAL))
            add(DOWN_RIGHT, input(dakuten.o, KanaMode.DAKUTEN))
            add(DOWN_LEFT, input(handakuten.o, KanaMode.HANDAKUTEN))
            add(LEFT, input("${handakuten.o}う"))
            spec.number?.let { add(UP_LEFT, input(it)) }
        })
    }

    /** A stage that opens a ようおん: its own kana and its long form. */
    private fun yoonBranch(text: String): FlickNode.Branch {
        val long = if (text.endsWith("ょ")) DOWN_LEFT else UP_LEFT
        val primary = if (text.endsWith("ょ")) DOWN else UP
        return branch(text, continuations = setOf(UP, RIGHT, DOWN)) {
            add(primary, input(text))
            add(long, input("${text}う"))
        }
    }

    /** The corner that leads to the だくてん (or はんだくてん) of the あ column. */
    private fun modeBranch(label: String, plain: String, vararg entries: Pair<FlickDirection, String>): FlickNode.Branch =
        branch(label) {
            add(CENTER, input(plain))
            entries.forEach { (direction, text) -> add(direction, input(text)) }
        }

    // --- the small builder ---

    private fun stage(block: Stage.() -> Unit): Map<FlickDirection, FlickNode> =
        Stage().apply(block).entries

    private fun input(text: String, nextMode: KanaMode? = null): FlickNode =
        FlickNode.Input(text, nextMode)

    private fun branch(
        label: String,
        cancelOnTap: Boolean = true,
        continuations: Set<FlickDirection> = emptySet(),
        overrides: Column = Column(),
        block: Stage.() -> Unit,
    ): FlickNode.Branch {
        val stage = Stage().apply(block)
        stage.removeAll(overrides.removals)
        overrides.upserts.forEach { (direction, node) -> stage.add(direction, node) }
        return FlickNode.Branch(label, stage.entries, overrides.cancelOnTap ?: cancelOnTap, continuations)
    }

    /** A definition written out by hand instead of generated from a [Spec]. */
    private fun staticKey(label: String, block: Stage.() -> Unit): FlickKey =
        FlickKey(label, stage(block))

    private class Stage {
        val entries = LinkedHashMap<FlickDirection, FlickNode>()

        fun add(direction: FlickDirection, node: FlickNode) {
            entries[direction] = node
        }

        fun removeAll(directions: Set<FlickDirection>) {
            directions.forEach { entries.remove(it) }
        }
    }
}
