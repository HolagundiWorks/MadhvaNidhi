package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardBadgesTest {
    @Test fun weeklyAwardsUseBrownSilverGoldInOrder() {
        assertEquals(listOf(DashboardBadges.Finish.BROWN, DashboardBadges.Finish.SILVER, DashboardBadges.Finish.GOLD),
            DashboardBadges.WEEKLY_TIERS.map { DashboardBadges.finishFor(it) })
    }

    @Test fun streakMilestoneFinishesReflectProgress() {
        fun finish(id: String) = DashboardBadges.finishFor(DashboardBadges.STREAK_TIERS.first { it.id == id })
        assertEquals(DashboardBadges.Finish.BROWN, finish("streak_7"))
        assertEquals(DashboardBadges.Finish.SILVER, finish("streak_21"))
        assertEquals(DashboardBadges.Finish.GOLD, finish("streak_108"))
        assertEquals(DashboardBadges.Finish.GOLD, finish("streak_365"))
    }

    @Test fun japaMilestoneFinishesReflectProgress() {
        fun finish(id: String) = DashboardBadges.finishFor(DashboardBadges.JAPA_TIERS.first { it.id == id })
        assertEquals(DashboardBadges.Finish.BROWN, finish("japa_108"))
        assertEquals(DashboardBadges.Finish.SILVER, finish("japa_10001"))
        assertEquals(DashboardBadges.Finish.GOLD, finish("japa_1000001"))
    }
}
