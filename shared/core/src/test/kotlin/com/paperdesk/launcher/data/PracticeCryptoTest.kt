package com.paperdesk.launcher.data

import com.paperdesk.launcher.data.PracticeCrypto.CryptoException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.SecureRandom

class PracticeCryptoTest {
    private val plain = "#madhva-practice-export\nschema=1\nಓಂ ನಮೋ ನಾರಾಯಣಾಯ\n".toByteArray(Charsets.UTF_8)

    @Test fun roundTripRestoresTheExactBytes() {
        val sealed = PracticeCrypto.seal(plain, "correct horse".toCharArray(), SecureRandom())
        assertTrue(PracticeCrypto.isSealed(sealed))
        assertArrayEquals(plain, PracticeCrypto.open(sealed, "correct horse".toCharArray()))
    }

    @Test fun sameInputGivesDifferentOutputEachTime() {
        val a = PracticeCrypto.seal(plain, "correct horse".toCharArray())
        val b = PracticeCrypto.seal(plain, "correct horse".toCharArray())
        assertFalse(a.contentEquals(b))
    }

    @Test fun wrongPassphraseIsRejected() {
        val sealed = PracticeCrypto.seal(plain, "correct horse".toCharArray())
        expectFailure { PracticeCrypto.open(sealed, "wrong horse".toCharArray()) }
    }

    @Test fun changedByteIsRejected() {
        val sealed = PracticeCrypto.seal(plain, "correct horse".toCharArray())
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 1).toByte()
        expectFailure { PracticeCrypto.open(sealed, "correct horse".toCharArray()) }
    }

    @Test fun shortPassphraseIsRefused() {
        expectFailure { PracticeCrypto.seal(plain, "abc".toCharArray()) }
    }

    @Test fun plainTextIsNotSealed() {
        assertFalse(PracticeCrypto.isSealed(plain))
        expectFailure { PracticeCrypto.open(plain, "correct horse".toCharArray()) }
    }

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
            fail("expected a CryptoException")
        } catch (_: CryptoException) {
        }
    }
}
