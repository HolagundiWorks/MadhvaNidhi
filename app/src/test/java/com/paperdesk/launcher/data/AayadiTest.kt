package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AayadiTest {
    /**
     * Rows of the house-size table in the Kannada book (page 97): building length x breadth in whole feet,
     * then aaya (with its name), dhana, runa and ayush. Area is length x breadth in square feet.
     */
    private data class Row(val l: Int, val b: Int, val aaya: Int, val name: String, val dhana: Int, val runa: Int, val ayush: Int)

    private val table = listOf(
        Row(17, 21, 5, "ವೃಷಭ", 12, 7, 93),
        Row(15, 21, 3, "ಸಿಂಹ", 12, 1, 75),
        Row(13, 21, 1, "ಧ್ವಜ", 12, 3, 57),
        Row(21, 29, 1, "ಧ್ವಜ", 12, 3, 81),
        Row(21, 27, 7, "ಗಜ", 12, 5, 63),
        Row(23, 31, 1, "ಧ್ವಜ", 4, 3, 57),
        Row(23, 33, 7, "ಗಜ", 12, 5, 111),
        Row(21, 33, 5, "ವೃಷಭ", 12, 7, 117),
        Row(25, 43, 3, "ಸಿಂಹ", 8, 1, 75),
        Row(23, 45, 3, "ಸಿಂಹ", 12, 1, 75),
        Row(25, 41, 1, "ಧ್ವಜ", 4, 3, 105),
        // The printed dhana (11) is not a value the formula can give, so only the other columns are compared.
        Row(29, 43, 7, "ಗಜ", 4, 5, 63),
        Row(31, 45, 3, "ಸಿಂಹ", 12, 1, 75),
        Row(31, 43, 5, "ವೃಷಭ", 8, 7, 117),
        Row(31, 39, 1, "ಧ್ವಜ", 12, 3, 81),
        Row(33, 39, 7, "ಗಜ", 12, 5, 63),
        Row(35, 41, 3, "ಸಿಂಹ", 8, 1, 75),
        Row(35, 43, 1, "ಧ್ವಜ", 4, 3, 105),
        Row(29, 45, 1, "ಧ್ವಜ", 12, 3, 105),
    )

    @Test fun everyTableRowMatchesAayaRunaAndAyush() {
        for (row in table) {
            val r = Aayadi.calculate(row.l.toDouble(), row.b.toDouble())
            val tag = "${row.l}x${row.b}"
            assertEquals(tag, row.aaya.toDouble(), r.aaya, 0.0001)
            assertEquals(tag, row.name, r.aayaName)
            assertEquals(tag, row.runa.toDouble(), r.runa, 0.0001)
            assertEquals(tag, row.ayush.toDouble(), r.ayush, 0.0001)
        }
    }

    @Test fun everyTableRowMatchesDhanaExceptThePrintedOutlier() {
        for (row in table) {
            if (row.l == 29 && row.b == 43) continue
            val r = Aayadi.calculate(row.l.toDouble(), row.b.toDouble())
            assertEquals("${row.l}x${row.b}", row.dhana.toDouble(), r.dhana, 0.0001)
        }
    }

    @Test fun thirtyByFortyIsSquareFeet() {
        val r = Aayadi.calculate(30.0, 40.0)
        assertEquals(1200.0, r.padam, 0.0001)
        assertEquals(8.0, r.aaya, 0.0001)
        assertEquals(12.0, r.dhana, 0.0001)
        assertEquals(8.0, r.runa, 0.0001)
        assertEquals(120.0, r.ayush, 0.0001)
        assertEquals(10.0, r.vyaya!!, 0.0001)
    }

    @Test fun alertFlagsFollowTheParameters() {
        // 17 x 21: aaya 5 (auspicious), vyaya 3 (< aaya), ayush 93 (> 60): nothing to alert.
        val ok = Aayadi.calculate(17.0, 21.0)
        assertEquals(true, ok.aayaOk)
        assertEquals(true, ok.vyayaOk)
        assertEquals(true, ok.ayushOk)
        // 15 x 21: aaya 3 (auspicious), vyaya 5 (not less than aaya): vyaya alert.
        val vyayaAlert = Aayadi.calculate(15.0, 21.0)
        assertEquals(true, vyayaAlert.aayaOk)
        assertEquals(false, vyayaAlert.vyayaOk)
        // 30 x 40: aaya 8 (not auspicious): aaya alert; vyaya 10 is not less than 8: vyaya alert.
        val aayaAlert = Aayadi.calculate(30.0, 40.0)
        assertEquals(false, aayaAlert.aayaOk)
        assertEquals(false, aayaAlert.vyayaOk)
        assertEquals(true, aayaAlert.ayushOk)
        // 13 x 21 is ayush 57 (not above 60): ayush alert.
        val ayushAlert = Aayadi.calculate(13.0, 21.0)
        assertEquals(false, ayushAlert.ayushOk)
    }

    @Test fun quarterFeetGiveAWholeAreaAndAreAccepted() {
        // 30'3" x 40' = 30.25 x 40 = 1210 sq ft: whole, so accepted.
        val r = Aayadi.calculate(30.0, 3.0, 40.0, 0.0)
        assertEquals(1210.0, r.padam, 0.0001)
        assertEquals(30.25, r.length, 0.0001)
    }

    @Test fun otherInchValuesAreRefused() {
        assertThrows(IllegalArgumentException::class.java) { Aayadi.calculate(30.0, 2.0, 40.0, 0.0) }
    }

    @Test fun fractionalLengthGivesNoVyaya() {
        // 30'3" x 40': the length is 30.25 ft, so vyaya is not calculated (shown as a dash, no alert).
        val r = Aayadi.calculate(30.0, 3.0, 40.0, 0.0)
        assertEquals(null, r.vyaya)
        assertEquals(null, r.vyayaOk)
    }

    @Test fun wholeAreaIsRequired() {
        // 30'3" x 40'6" = 30.25 x 40.5 = 1225.125 sq ft: not whole, so refused.
        assertThrows(IllegalArgumentException::class.java) { Aayadi.calculate(30.0, 3.0, 40.0, 6.0) }
    }

    @Test fun zeroDimensionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Aayadi.calculate(0.0, 20.0) }
    }
}
