package com.example.myapplication

import com.example.myapplication.data.backup.BackupManager
import com.example.myapplication.security.SecurityCryptoHelper
import com.example.myapplication.sms.SmsParser
import com.example.myapplication.sms.SmsSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTest {

    @Test
    fun `hashPin produces salted PBKDF2 hash and verifyPin validates accurately`() {
        val rawPin = "2580"
        val hash1 = SecurityCryptoHelper.hashPin(rawPin)
        val hash2 = SecurityCryptoHelper.hashPin(rawPin)

        assertTrue(SecurityCryptoHelper.isHashedPin(hash1))
        assertNotEquals("Salted hashes of identical PINs must differ", hash1, hash2)
        assertFalse("Raw PIN must not appear in hash", hash1.contains(rawPin))

        assertTrue(SecurityCryptoHelper.verifyPin("2580", hash1))
        assertTrue(SecurityCryptoHelper.verifyPin("2580", hash2))
        assertFalse(SecurityCryptoHelper.verifyPin("1234", hash1))
        assertFalse(SecurityCryptoHelper.verifyPin("", hash1))
    }

    @Test
    fun `verifyPin supports legacy plaintext PINs for backward compatibility`() {
        val legacyPin = "9090"
        assertFalse(SecurityCryptoHelper.isHashedPin(legacyPin))
        assertTrue(SecurityCryptoHelper.verifyPin("9090", legacyPin))
        assertFalse(SecurityCryptoHelper.verifyPin("0000", legacyPin))
    }

    @Test
    fun `SmsSanitizer blocks OTP and secret verification codes`() {
        assertTrue(SmsSanitizer.containsSensitiveOtpOrSecret("Click: Tasdiqlash kodi: 84920. Hech kimga bermang!"))
        assertTrue(SmsSanitizer.containsSensitiveOtpOrSecret("Uzum: Tasdiqlaş kodi 1234. Heç kimga bermang"))
        assertTrue(SmsSanitizer.containsSensitiveOtpOrSecret("Your verification code is 492811. Do not share."))
        assertTrue(SmsSanitizer.containsSensitiveOtpOrSecret("3D-Secure code: 554422 for purchase"))

        // Normal purchase SMS should not be blocked
        val purchaseSms = "HUMO: 55 000 UZS tölandi. KORZINKA. Balans: 1 200 000 UZS"
        assertFalse(SmsSanitizer.containsSensitiveOtpOrSecret(purchaseSms))
    }

    @Test
    fun `SmsSanitizer masks card numbers, balances, and phone numbers while preserving purchase data`() {
        val rawSms = "HUMO *4821 yoki 8600 12** **** 3456: 85 000 UZS tölandi. Joy: EVOS. Tel: +998901234567. Balans: 14 520 000.00 UZS"
        val sanitized = SmsSanitizer.sanitizeForAiAndStorage(rawSms)

        assertFalse("Card tail *4821 must be redacted", sanitized.contains("*4821"))
        assertFalse("Masked PAN 8600... must be redacted", sanitized.contains("3456"))
        assertFalse("Account balance 14 520 000 must be redacted", sanitized.contains("14 520 000"))
        assertFalse("Phone number must be redacted", sanitized.contains("998901234567"))

        assertTrue("Transaction amount must remain intact", sanitized.contains("85 000 UZS"))
        assertTrue("Merchant EVOS must remain intact", sanitized.contains("EVOS"))

        // Verify SmsParser still parses the transaction cleanly and stores sanitized rawText
        val parsed = SmsParser.parse("HUMO", rawSms)
        assertNotNull(parsed)
        assertEquals(85_000L, parsed?.amount)
        assertFalse(parsed!!.rawText.contains("14 520 000"))
        assertFalse(parsed.rawText.contains("*4821"))

        // Verify OTP SMS returns null in SmsParser
        assertNull(SmsParser.parse("HUMO", "Tasdiqlaş kodi: 44112. 55 000 UZS tölandi deb aldanmang"))
    }

    @Test
    fun `BackupManager sanitizeCsvFormula neutralizes spreadsheet formula injection`() {
        assertEquals("'=cmd|' /C calc'!A0", BackupManager.sanitizeCsvFormula("=cmd|' /C calc'!A0"))
        assertEquals("'+SUM(A1:A10)", BackupManager.sanitizeCsvFormula("+SUM(A1:A10)"))
        assertEquals("'-1000", BackupManager.sanitizeCsvFormula("-1000"))
        assertEquals("'@IMPORTXML()", BackupManager.sanitizeCsvFormula("@IMPORTXML()"))
        assertEquals("Korzinka xaridi", BackupManager.sanitizeCsvFormula("Korzinka xaridi"))
    }
}
