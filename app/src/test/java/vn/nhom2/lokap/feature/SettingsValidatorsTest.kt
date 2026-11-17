package vn.nhom2.lokap.feature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import vn.nhom2.lokap.feature.auth.AuthValidators
import vn.nhom2.lokap.feature.settings.SettingsValidators

class SettingsValidatorsTest {
    @Test
    fun `recovery phone rejects the nine letter regression and mixed text`() {
        listOf("abcdefghi", "0912abc678", "0912345678ext9", "٠٩١٢٣٤٥٦٧٨").forEach {
            assertNotNull(it, SettingsValidators.validateRecoveryPhone(it))
        }
        assertEquals("abcdefghi", SettingsValidators.normalizeRecoveryPhone(" abcdefghi "))
    }

    @Test
    fun `recovery phone accepts formatted domestic mobile and landline`() {
        mapOf(
            "0912345678" to "0912345678",
            " 0912 345 678 " to "0912345678",
            "0912-345-678" to "0912345678",
            "0912.345.678" to "0912345678",
            "(024) 1234 5678" to "02412345678"
        ).forEach { (input, normalized) ->
            assertNull(input, SettingsValidators.validateRecoveryPhone(input))
            assertEquals(normalized, SettingsValidators.normalizeRecoveryPhone(input))
        }
    }

    @Test
    fun `recovery phone accepts international code with plus`() {
        listOf("+84912345678", "+84 912 345 678", "+1 (415) 555-2671", "+442079460958").forEach {
            assertNull(it, SettingsValidators.validateRecoveryPhone(it))
        }
        assertEquals("+14155552671", SettingsValidators.normalizeRecoveryPhone("+1 (415) 555-2671"))
    }

    @Test
    fun `recovery phone rejects empty short long and malformed values`() {
        listOf(
            "", "   ", "123456789", "091234567", "091234567890", "+1234567",
            "+1234567890123456", "+0123456789", "++84912345678", "84+912345678",
            "0912/345/678", "0912,345,678", "(024 1234 5678", "024) 1234 5678", "((024))12345678"
        ).forEach { assertNotNull(it, SettingsValidators.validateRecoveryPhone(it)) }
    }

    @Test
    fun `linked email rejects reported and related malformed addresses`() {
        listOf(
            "", " ", "@.com", "a@.com", "a@b..com", "a@@b.com", "a b@lokap.vn",
            ".a@lokap.vn", "a.@lokap.vn", "a..b@lokap.vn", "a@-lokap.vn", "a@lokap-.vn",
            "a@lokap.vn.", "a@lokap", "a@lokap.1", "a@under_score.vn"
        ).forEach {
            assertNotNull(it, SettingsValidators.validateLinkedEmail(it))
            assertNotNull(it, AuthValidators.validateEmail(it))
        }
    }

    @Test
    fun `linked email keeps common valid addresses including plus aliases and subdomains`() {
        listOf(
            "ban@lokap.vn", "  ban@lokap.vn ", "first.last+photos@example.com",
            "first_last@sub.example.co.uk", "O'Connor@example.com", "A-B@EXAMPLE.COM"
        ).forEach {
            assertNull(it, SettingsValidators.validateLinkedEmail(it))
            assertNull(it, AuthValidators.validateEmail(it))
        }
    }

    @Test
    fun `email enforces local and domain label size limits`() {
        assertNull(AuthValidators.validateEmail("a".repeat(64) + "@example.com"))
        assertNotNull(AuthValidators.validateEmail("a".repeat(65) + "@example.com"))
        assertNull(AuthValidators.validateEmail("a@" + "b".repeat(63) + ".com"))
        assertNotNull(AuthValidators.validateEmail("a@" + "b".repeat(64) + ".com"))
    }
}
