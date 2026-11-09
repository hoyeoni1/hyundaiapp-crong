package com.hyundaiapp.cruisebreaker.ui.overlay

import com.hyundaiapp.cruisebreaker.ui.RiskRowUi
import org.junit.Assert.*
import org.junit.Test

class OverlayContentPolicyTest {
    private fun row(id: String) = RiskRowUi(id, id, 100.0, .4, 100)

    @Test fun `restricted cumulative budget is not bypassed by changing nearby rows`() {
        val policy = OverlayContentPolicy()
        val limits = OverlayLimits(maxItems = 2)
        assertEquals(listOf("a", "b"), policy.rows(listOf(row("a"), row("b"), row("c")), limits).map { it.id })
        assertEquals(listOf("b"), policy.rows(listOf(row("c"), row("b")), limits).map { it.id })
        assertTrue(policy.rows(listOf(row("d")), limits).isEmpty())
        policy.rows(emptyList(), OverlayLimits())
        assertEquals(listOf("d"), policy.rows(listOf(row("d")), limits).map { it.id })
    }

    @Test fun `a stricter live content limit also removes previously admitted excess items`() {
        val policy = OverlayContentPolicy()
        val rows = listOf(row("a"), row("b"), row("c"))
        assertEquals(3, policy.rows(rows, OverlayLimits(maxItems = 3)).size)
        assertEquals(listOf("a"), policy.rows(rows.reversed(), OverlayLimits(maxItems = 1)).map { it.id })
        assertTrue(policy.rows(rows, OverlayLimits(maxItems = 0)).isEmpty())
    }

    @Test fun `no content or insufficient room for numeric labels prevents display`() {
        assertFalse(OverlayLimits(maxItems = 0).canDisplay)
        assertFalse(OverlayLimits(maxStringLength = 7).canDisplay)
        assertTrue(OverlayLimits(maxItems = 1, maxStringLength = 8).canDisplay)
    }

    @Test fun `text follows character limits without splitting surrogate pairs`() {
        assertEquals("해제 다…", limitedOverlayText("해제 다발 구간", 5))
        assertEquals("a…", limitedOverlayText("a🚗구간", 3))
        assertEquals("365m", limitedOverlayText("365m", 8))
        assertEquals("", limitedOverlayText("해제", 0))
    }
}
