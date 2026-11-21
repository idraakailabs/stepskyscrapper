package ai.idraak.stepskyscraper.core.designsystem.tower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** BUG-010: the dashed outline reaches at most 10 floors above the built ones. */
class GhostOutlineTest {
    private fun height(spec: TowerSpec): Float = buildTowerScene(spec, TowerAnimation()).bounds.let { it.maxY - it.minY }

    @Test
    fun designFramesKeepTheFullOutline() {
        // 2a (14 of 24), 1c (4 of 12), 1a (7 of 12), first tower (4 of 10): unchanged from tower.js.
        assertEquals(24, ghostEnd(14, 24))
        assertEquals(12, ghostEnd(4, 12))
        assertEquals(12, ghostEnd(7, 12))
        assertEquals(10, ghostEnd(4, 10))
    }

    @Test
    fun tallTargetsAreCappedTenFloorsAhead() {
        assertEquals(29, ghostEnd(19, 100))
        assertEquals(10, ghostEnd(0, 250))
        assertEquals(250, ghostEnd(245, 250))
    }

    @Test
    fun builtFloorsFillAtLeastHalfTheDrawingOnTheLadder() {
        listOf(100, 160, 250).forEach { target ->
            val built = height(TowerSpec(floors = 19, target = 19, crownMode = CrownMode.None))
            val drawn = height(TowerSpec(floors = 19, target = target, progress = 0.66f, crane = true))
            assertTrue("19 of $target: built ${built / drawn}", built / drawn >= 0.5f)
        }
    }
}
