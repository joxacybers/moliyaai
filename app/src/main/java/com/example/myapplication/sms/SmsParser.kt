package com.example.myapplication.sms

import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import java.util.Locale

object SmsParser {

    private val KNOWN_SENDERS = setOf(
        "HUMO", "UZCARD", "CLICK", "PAYME", "UZUM", "UZUMBANK", "TBC", "TBCBANK",
        "ANORBANK", "ANOR", "KAPITALBANK", "KAPITAL", "IPOTEKA", "IPOTEKABANK",
        "SQB", "AGROBANK", "NBU", "MILLIY", "HAMKORBANK", "HAMKOR", "ASAKABANK",
        "ASAKA", "OSON", "PAYNET", "ALIF", "ORIENT", "DAVRBANK", "ALOQABANK",
        "OCTO", "PAYLOV", "PLUM", "CARD", "BANK",
    )

    fun isBankSender(sender: String?): Boolean {
        if (sender == null) return false
        val cleanSender = sender.trim().uppercase(Locale.US)
        return KNOWN_SENDERS.any { cleanSender.contains(it) } || cleanSender.matches(Regex("^[A-Z0-9_-]{3,12}$"))
    }

    fun parse(sender: String, messageBody: String): ParsedSmsTransaction? {
        val body = messageBody.trim()
        val lowerBody = body.lowercase(Locale.ROOT)

        // 1. Check if it's a financial transaction SMS
        val isExpense = lowerBody.contains("to'landi") ||
            lowerBody.contains("tolandi") ||
            lowerBody.contains("yechildi") ||
            lowerBody.contains("oplata") ||
            lowerBody.contains("spisanie") ||
            lowerBody.contains("pokupka") ||
            lowerBody.contains("xarid") ||
            lowerBody.contains("v polzu") ||
            lowerBody.contains("to'lov") ||
            lowerBody.contains("tolov")

        val isIncome = lowerBody.contains("popolnenie") ||
            lowerBody.contains("zachislenie") ||
            lowerBody.contains("tushdi") ||
            lowerBody.contains("kirim") ||
            lowerBody.contains("postuplenie")

        if (!isExpense && !isIncome) {
            // Not a financial confirmation SMS
            return null
        }

        val type = if (isIncome && !isExpense) TransactionType.INCOME else TransactionType.EXPENSE

        // 2. Extract Amount
        val amount = extractAmount(body) ?: return null
        if (amount <= 0) return null

        // 3. Extract Merchant / Location
        val merchant = extractMerchant(body, sender)

        // 4. Detect Category
        val category = if (type == TransactionType.INCOME) {
            detectIncomeCategory(merchant, lowerBody)
        } else {
            detectExpenseCategory(merchant, lowerBody)
        }

        return ParsedSmsTransaction(
            amount = amount,
            merchant = merchant,
            category = category,
            type = type,
            account = Account.CARD,
            rawText = body,
        )
    }

    private fun extractAmount(body: String): Long? {
        // Examples:
        // "55 000.00 UZS", "55 000 UZS", "55000 UZS", "55 000 so'm", "55,000.00 sum"
        val regex = Regex("""([0-9]{1,3}(?:[ \u00A0,][0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+)\s*(?:uzs|so['`ʻ]?m|sum)""", RegexOption.IGNORE_CASE)
        val match = regex.find(body) ?: return null

        val rawNumber = match.groupValues[1]
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace(",", "")

        // Truncate cents if has decimal e.g. "55000.00" -> 55000
        val cleanNumber = if (rawNumber.contains('.')) {
            rawNumber.substringBefore('.')
        } else {
            rawNumber
        }

        return cleanNumber.toLongOrNull()
    }

    private fun extractMerchant(body: String, sender: String): String {
        // Patterns like:
        // "Terminal: KORZINKA", "Mesto: EVOS", "Joy: SAFIA", "Xizmat: Beeline", "v polzu EVOS"
        val explicitPatterns = listOf(
            Regex("""(?:terminal|mesto|joy|xizmat|usluga|v polzu|punkt)[:\s]+([^;,\.\n]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:to'landi|tolandi|yechildi)[\.\s]+([^;,\.\n0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""HUMO:[\s0-9\.,UZS]+(?:to'landi|yechildi)[\.\s]+([^;,\.\n]+)""", RegexOption.IGNORE_CASE),
        )

        for (pattern in explicitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotBlank() && candidate.length in 2..40 && !candidate.lowercase().contains("balans") && !candidate.lowercase().contains("qoldiq")) {
                    return candidate
                }
            }
        }

        // Fallback: Sender or generic
        return if (sender.isNotBlank()) sender.uppercase(Locale.US) else "Karta toʻlovi"
    }

    private fun detectExpenseCategory(merchant: String, body: String): Category {
        val target = (merchant + " " + body).lowercase(Locale.ROOT)

        return when {
            // Food & Groceries
            target.contains("korzinka") || target.contains("makro") || target.contains("havas") ||
                target.contains("carrefour") || target.contains("baraka") || target.contains("tegen") ||
                target.contains("safia") || target.contains("bon!") || target.contains("evos") ||
                target.contains("oqtepa") || target.contains("feed up") || target.contains("kfc") ||
                target.contains("wendy") || target.contains("bellissimo") || target.contains("chopar") ||
                target.contains("max way") || target.contains("apex") || target.contains("lavash") ||
                target.contains("osh") || target.contains("shashlik") || target.contains("cafe") ||
                target.contains("kafe") || target.contains("resto") || target.contains("bistro") ||
                target.contains("market") || target.contains("supermarket") || target.contains("fastfood") ||
                target.contains("milliy taom") -> Category.FOOD

            // Transport & Fuel
            target.contains("yandex") || target.contains("mytaxi") || target.contains("taxi") ||
                target.contains("metro") || target.contains("toshshahartrans") || target.contains("zapravka") ||
                target.contains("ung") || target.contains("lukoil") || target.contains("benzovoz") ||
                target.contains("metan") || target.contains("propan") || target.contains("benzin") ||
                target.contains("moyka") || target.contains("avto") || target.contains("parking") -> Category.TRANSPORT

            // Utilities & Mobile/Internet
            target.contains("beeline") || target.contains("ucell") || target.contains("uztelecom") ||
                target.contains("mobiuz") || target.contains("uzonline") || target.contains("comnet") ||
                target.contains("tps") || target.contains("elektr") || target.contains("gaz") ||
                target.contains("suv") || target.contains("mahsustrans") || target.contains("kommunal") ||
                target.contains("paynet") -> Category.UTILITIES

            // Health & Pharmacy
            target.contains("apteka") || target.contains("dorixona") || target.contains("farm") ||
                target.contains("dori") || target.contains("shifo") || target.contains("med") ||
                target.contains("klinika") || target.contains("stomatolog") || target.contains("tibbiyot") -> Category.HEALTH

            // Shopping
            target.contains("uzum market") || target.contains("asaxiy") || target.contains("zoodmall") ||
                target.contains("texnomart") || target.contains("mediapark") || target.contains("idea") ||
                target.contains("radius") || target.contains("waikiki") || target.contains("zara") ||
                target.contains("defacto") || target.contains("terra pro") || target.contains("kiyim") ||
                target.contains("mall") || target.contains("bazar") || target.contains("bozor") -> Category.SHOPPING

            // Entertainment
            target.contains("kino") || target.contains("cinema") || target.contains("park") ||
                target.contains("bowling") || target.contains("playstation") || target.contains("club") ||
                target.contains("bilet") || target.contains("iticket") || target.contains("teatr") -> Category.ENTERTAINMENT

            // Loans
            target.contains("kredit") || target.contains("ssuda") || target.contains("loan") -> Category.LOANS

            else -> Category.OTHER_EXPENSE
        }
    }

    private fun detectIncomeCategory(merchant: String, body: String): Category {
        val target = (merchant + " " + body).lowercase(Locale.ROOT)
        return when {
            target.contains("salary") || target.contains("zarplata") || target.contains("maosh") || target.contains("avans") -> Category.SALARY
            target.contains("perevod") || target.contains("o'tkazma") || target.contains("otkazma") -> Category.OTHER_INCOME
            else -> Category.OTHER_INCOME
        }
    }
}
