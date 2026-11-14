package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class JapaCountMathTest {
    @Test fun tapChangesBothTotalsByOne() {
        assertEquals(JapaCountMath.Change(65, 12541, 1), JapaCountMath.add(64, 12540, 1))
    }
    @Test fun manualBatchAndUndoRestoreBothTotals() {
        val recorded = JapaCountMath.add(64, 12540, 108)
        assertEquals(JapaCountMath.Change(172, 12648, 108), recorded)
        assertEquals(JapaCountMath.Change(64, 12540, 108), JapaCountMath.undo(recorded.daily, recorded.cumulative, recorded.delta))
    }
    @Test fun countCanPassDailyTargetWithoutChangingIt() {
        assertEquals(112, JapaCountMath.add(100, 1000, 12).daily)
        assertEquals(101, JapaGoals.DEFAULT_DAILY)
    }
    @Test fun overflowAcceptsSameDeltaForBothTotalsAndCanUndo() {
        val added = JapaCountMath.add(4, Long.MAX_VALUE - 2, 108)
        assertEquals(JapaCountMath.Change(6, Long.MAX_VALUE, 2), added)
        assertEquals(JapaCountMath.Change(4, Long.MAX_VALUE - 2, 2), JapaCountMath.undo(added.daily, added.cumulative, added.delta))
    }
    @Test fun invalidCountsAndExcessiveUndoCannotGoNegative() {
        assertEquals(JapaCountMath.Change(0, 0, 0), JapaCountMath.add(-1, -10, -1))
        assertEquals(JapaCountMath.Change(0, 90, 10), JapaCountMath.undo(10, 100, 108))
    }
}
