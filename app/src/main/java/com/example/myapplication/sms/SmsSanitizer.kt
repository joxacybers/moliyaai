package com.example.myapplication.sms

import java.util.Locale

/**
 * Privacy shield for SMS processing:
 * 1. Detects and blocks OTP / verification / 3D-Secure / password SMS messages so they are never read or stored.
 * 2. Redacts card numbers (PAN), remaining card balances, phone numbers, and secret codes before
 *    any SMS text is sent to Cloud AI or saved into the local notification database.
 */
object SmsSanitizer {

    private val OTP_BLOCKLIST_KEYWORDS = listOf(
        "tasdiqlash kodi",
        "tasdiqlaş kodi",
        "kod podtverjdeniya",
        "verification code",
        "hech kimga bermang",
        "heç kimga bermang",
        "nikomu ne soobshayte",
        "do not share",
        "don't share",
        "one-time password",
        "3d secure",
        "3d-secure",
        "3ds",
        "cvv",
        "cvc",
        "parol:",
        "password:",
        "pin-kod",
        "pin kod",
        "kirish kodi",
        "kiriş kodi",
        "avtorizatsiya kodi",
    )

    // Matches card numbers like "8600 12** **** 3456", "986012******3456", "8600123456789012", "*1234", "***1234"
    private val CARD_PAN_REGEX = Regex(
        """(?:\b(?:8600|9860|5614|6262|4[0-9]{3}|5[1-5][0-9]{2})[\s\-*0-9]{8,16}\b|\*{1,4}[0-9]{4}\b|[0-9]{4,6}\*{2,8}[0-9]{2,4})""",
    )

    // Matches balance segments like "Balans: 1 200 000.00 UZS", "Qoldiq: 500 000 so'm", "Ostatok: 45000 UZS", "Dostupno: 100 000 UZS"
    private val BALANCE_SEGMENT_REGEX = Regex(
        """(?i)\b(balans|balance|qoldiq|ostatok|dostupno|bal|hisobda)[:\s]+[0-9]{1,3}(?:[ \u00A0.,][0-9]{2,3})*(?:\.[0-9]{1,2})?\s*(?:uzs|so['`ʻ]?m|söm|sum|usd)?""",
    )

    // Matches Uzbekistan and international phone numbers like +998901234567, 998 90 123 45 67
    private val PHONE_NUMBER_REGEX = Regex(
        """(?:\+?998[\s\-]?\(?[0-9]{2}\)?[\s\-]?[0-9]{3}[\s\-]?[0-9]{2}[\s\-]?[0-9]{2})\b""",
    )

    // Matches explicit code patterns like "Kod: 123456", "Code 4829"
    private val EXPLICIT_CODE_REGEX = Regex(
        """(?i)\b(kod|code|otp|parol)[:\s#\-]*[0-9]{4,8}\b""",
    )

    /**
     * Returns true if the message looks like an OTP, login code, 3D-Secure prompt, or secret password.
     */
    fun containsSensitiveOtpOrSecret(messageBody: String): Boolean {
        val lower = messageBody.lowercase(Locale.ROOT)
        if (OTP_BLOCKLIST_KEYWORDS.any { lower.contains(it) }) {
            return true
        }
        if (EXPLICIT_CODE_REGEX.containsMatchIn(messageBody)) {
            return true
        }
        return false
    }

    /**
     * Redacts sensitive banking details (card numbers, remaining account balance, phone numbers, codes)
     * while preserving transaction amount, currency, transaction type keywords, and merchant name.
     */
    fun sanitizeForAiAndStorage(rawSms: String): String {
        if (rawSms.isBlank()) return ""
        var sanitized = rawSms.trim()

        // 1. Mask remaining card/account balance first so AI never sees total wealth on card
        sanitized = BALANCE_SEGMENT_REGEX.replace(sanitized) { match ->
            val label = match.groupValues[1]
            "$label: [YAŞIRILGAN]"
        }

        // 2. Mask card PANs and tail digits (*1234, 8600 **** 1234)
        sanitized = CARD_PAN_REGEX.replace(sanitized, "[KARTA]")

        // 3. Mask phone numbers
        sanitized = PHONE_NUMBER_REGEX.replace(sanitized, "[TEL]")

        // 4. Mask any explicit codes if present
        sanitized = EXPLICIT_CODE_REGEX.replace(sanitized) { match ->
            val label = match.groupValues[1]
            "$label: [MAXFIY]"
        }

        return sanitized
    }
}
