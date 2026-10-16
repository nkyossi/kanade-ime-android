package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutsTest {
    private val layouts = listOf(
        QwertyLayout.layout,
        FlickLayout.layout,
        SymbolLayout.numbers,
        SymbolLayout.symbols,
    )

    @Test
    fun switchLayoutTargetsAreRegistered() {
        val ids = layouts.map { it.id }.toSet()
        for (layout in layouts) for (row in layout.rows) for (key in row.keys) {
            val action = key.action
            if (action is KeyAction.SwitchLayout) {
                assertTrue("${layout.id} -> ${action.id}", action.id in ids)
            }
        }
    }

    @Test
    fun everyLayoutHasBackspaceAndEnter() {
        for (layout in layouts) {
            val actions = layout.rows.flatMap { it.keys }.map { it.action }
            assertTrue("${layout.id} has no ⌫", actions.any { it is KeyAction.Backspace })
            assertTrue("${layout.id} has no 確定", actions.any { it == KeyAction.Enter })
        }
    }

    @Test
    fun rowsAreNotEmpty() {
        for (layout in layouts) for (row in layout.rows) {
            assertTrue(layout.id, row.keys.isNotEmpty())
        }
    }
}
