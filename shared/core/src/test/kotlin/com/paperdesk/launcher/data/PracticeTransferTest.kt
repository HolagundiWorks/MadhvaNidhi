package com.paperdesk.launcher.data

import com.paperdesk.launcher.data.PracticeTransfer.Entry
import com.paperdesk.launcher.data.PracticeTransfer.FileSnapshot
import com.paperdesk.launcher.data.PracticeTransfer.TransferException
import com.paperdesk.launcher.data.PracticeTransfer.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PracticeTransferTest {
    private val allowed = mapOf("paperdesk_japa" to false, "paperdesk_japa_enc" to true)

    private fun sample(): String = PracticeTransfer.encode(
        source = "in.hcworks.madhvanidhi",
        createdAt = "2026-11-20T10:00:00Z",
        files = listOf(
            FileSnapshot(
                "paperdesk_japa", false, listOf(
                    Entry(Type.STRING, "mantra", "ಓಂ ನಮೋ\tನಾರಾಯಣಾಯ\nline2 \\ back"),
                    Entry(Type.LONG, "target", "108"),
                    Entry(Type.BOOLEAN, "target_enabled", "true")
                )
            ),
            FileSnapshot("paperdesk_japa_enc", true, listOf(Entry(Type.INT, "procedure", "2")))
        )
    )

    @Test fun roundTripKeepsEveryValueExactly() {
        val batch = PracticeTransfer.decode(sample(), allowed)
        assertEquals("in.hcworks.madhvanidhi", batch.source)
        assertEquals("2026-11-20T10:00:00Z", batch.createdAt)
        assertEquals(2, batch.files.size)
        val japa = batch.files[0].entries
        assertEquals("ಓಂ ನಮೋ\tನಾರಾಯಣಾಯ\nline2 \\ back", japa[0].value)
        assertEquals(Type.LONG, japa[1].type)
        assertEquals("108", japa[1].value)
        assertTrue(batch.files[1].encrypted)
    }

    @Test fun batchIdIsTheChecksumAndIsStable() {
        val text = sample()
        val first = PracticeTransfer.decode(text, allowed).batchId
        assertEquals(first, PracticeTransfer.decode(text, allowed).batchId)
        assertEquals(64, first.length)
    }

    @Test fun damagedContentIsRejected() {
        expectFailure(sample().replace("108", "109"), "checksum does not match")
    }

    @Test fun newerSchemaIsRejectedWithAClearMessage() {
        // A well-formed export from a future version: same layout, schema 2, checksum recomputed.
        val body = sample().substringBefore("checksum=").replace("schema=1", "schema=2")
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(body.toByteArray(Charsets.UTF_8))
        val checksum = digest.joinToString("") { "%02x".format(it) }
        expectFailure(body + "checksum=" + checksum + "\n", "newer version")
    }

    @Test fun fileNotOnTheAllowListIsRejected() {
        val text = PracticeTransfer.encode("x", "t", listOf(FileSnapshot("paperdesk_reader_x", false, emptyList())))
        expectFailure(text, "does not accept")
    }

    @Test fun encryptionFlagMismatchIsRejected() {
        val text = PracticeTransfer.encode("x", "t", listOf(FileSnapshot("paperdesk_japa", true, emptyList())))
        expectFailure(text, "Encryption flag")
    }

    @Test fun truncatedFileIsRejected() {
        expectFailure(sample().substringBefore("checksum="), "incomplete")
    }

    @Test fun notAnExportIsRejected() {
        expectFailure("hello world", "Not a practice export")
    }

    @Test fun controlCharactersInKeysCannotBeWritten() {
        try {
            PracticeTransfer.encode("x", "t", listOf(FileSnapshot("paperdesk_japa", false, listOf(Entry(Type.STRING, "bad\tkey", "v")))))
            fail("expected rejection")
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun expectFailure(text: String, fragment: String) {
        try {
            PracticeTransfer.decode(text, allowed)
            fail("expected a TransferException containing '$fragment'")
        } catch (e: TransferException) {
            assertTrue("message was: ${e.message}", e.message!!.contains(fragment))
        }
    }
}
