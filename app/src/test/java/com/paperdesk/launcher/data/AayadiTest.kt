package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AayadiTest {
    @Test fun knownThirtyByFortyCase() {
        val r = Aayadi.calculate(30.0, 0.0, 40.0, 0.0)
        assertEquals(1200.0, r.padam, 0.0001)
        assertEquals(8.0, r.aaya, 0.0001)
        assertEquals(12.0, r.dhana, 0.0001)
        assertEquals(8.0, r.runa, 0.0001)
        assertEquals(120.0, r.ayush, 0.0001)
        assertEquals(10.0, r.vyaya, 0.0001)
    }

    @Test fun inchesAreConvertedToFeet() {
        val r = Aayadi.calculate(10.0, 6.0, 20.0, 0.0)
        assertEquals(10.5, r.length, 0.0001)
        assertEquals(210.0, r.padam, 0.0001)
    }

    @Test fun zeroDimensionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Aayadi.calculate(0.0, 0.0, 20.0, 0.0) }
    }
}
