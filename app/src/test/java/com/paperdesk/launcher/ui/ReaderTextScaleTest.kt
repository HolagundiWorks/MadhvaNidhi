package com.paperdesk.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTextScaleTest {

    @Test
    fun clampEnforcesLowerAndUpperBounds() {
        assertEquals(ReaderTextScale.MIN_SCALE, ReaderTextScale.clamp(0.40f), 0.001f)
        assertEquals(ReaderTextScale.MAX_SCALE, ReaderTextScale.clamp(3.00f), 0.001f)
        assertEquals(1.30f, ReaderTextScale.clamp(1.304f), 0.001f)
    }

    @Test
    fun stepUpAndStepDownMoveByStepAndClampAtBounds() {
        val up = ReaderTextScale.stepUp(ReaderTextScale.DEFAULT_SCALE)
        assertEquals(1.15f, up, 0.001f)

        val down = ReaderTextScale.stepDown(ReaderTextScale.DEFAULT_SCALE)
        assertEquals(ReaderTextScale.MIN_SCALE, down, 0.001f)

        assertEquals(ReaderTextScale.MIN_SCALE, ReaderTextScale.stepDown(ReaderTextScale.MIN_SCALE), 0.001f)
        assertEquals(ReaderTextScale.MAX_SCALE, ReaderTextScale.stepUp(ReaderTextScale.MAX_SCALE), 0.001f)
    }

    @Test
    fun scaledTypographyAndPercentLabelsMatchClampedScale() {
        assertEquals("100%", ReaderTextScale.percentLabel(1.0f))
        assertEquals("85%", ReaderTextScale.percentLabel(0.5f))
        assertEquals("220%", ReaderTextScale.percentLabel(2.5f))

        assertEquals(ReaderTextScale.BASE_BODY_SP * 1.30f, ReaderTextScale.bodySp(1.30f), 0.01f)
        assertEquals(ReaderTextScale.BASE_HEAD_SP * 1.30f, ReaderTextScale.headSp(1.30f), 0.01f)
        assertEquals(ReaderTextScale.BASE_LINE_HEIGHT_SP * 1.30f, ReaderTextScale.lineHeightSp(1.30f), 0.01f)
    }
}
