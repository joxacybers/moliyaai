package com.example.myapplication.sms

import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.CustomCategory
import com.example.myapplication.data.model.TransactionType
import java.util.Locale

object SmsParser {

    private val KNOWN_SENDERS = setOf(
        "HUMO", "UZCARD", "CLICK", "PAYME", "UZUM", "UZUMBANK", "TBC", "TBCBANK",
        "ANORBANK", "ANOR", "KAPITALBANK", "KAPITAL", "IPOTEKA", "IPOTEKABANK",
        "SQB", "AGROBANK", "NBU", "MILLIY", "HAMKORBANK", "HAMKOR", "ASAKABANK",
        "ASAKA", "OSON", "PAYNET", "ALIF", "ORIENT", "DAVRBANK", "ALOQABANK",
        "OCTO", "PAYLOV", "PLUM", "CARD", "BANK", "SOLIQ", "MULTICARD", "CHAKANAPAY",
    )

    fun isBankSender(sender: String?): Boolean {
        if (sender == null) return false
        val cleanSender = sender.trim().uppercase(Locale.US)
        return KNOWN_SENDERS.any { cleanSender.contains(it) } || cleanSender.matches(Regex("^[A-Z0-9_-]{3,12}$"))
    }

    fun parse(
        sender: String,
        messageBody: String,
        customCategories: List<CustomCategory> = emptyList(),
    ): ParsedSmsTransaction? {
        val body = messageBody.trim()
        val lowerBody = body.lowercase(Locale.ROOT)

        // Ignore OTP / verification code / 3D-Secure / password SMS messages
        if (SmsSanitizer.containsSensitiveOtpOrSecret(body)) {
            return null
        }

        // 1. Check if it's a financial transaction SMS (Purchase / Payment vs Sale / Income)
        val isExpense = lowerBody.contains("to'landi") ||
            lowerBody.contains("tolandi") ||
            lowerBody.contains("toʻlandi") ||
            lowerBody.contains("tölandi") ||
            lowerBody.contains("yechildi") ||
            lowerBody.contains("yeçildi") ||
            lowerBody.contains("echib olindi") ||
            lowerBody.contains("eçib olindi") ||
            lowerBody.contains("oplata") ||
            lowerBody.contains("spisanie") ||
            lowerBody.contains("pokupka") ||
            lowerBody.contains("xarid") ||
            lowerBody.contains("harid") ||
            lowerBody.contains("v polzu") ||
            lowerBody.contains("to'lov") ||
            lowerBody.contains("tolov") ||
            lowerBody.contains("toʻlov") ||
            lowerBody.contains("tölov") ||
            lowerBody.contains("sarflandi") ||
            lowerBody.contains("chiqim") ||
            lowerBody.contains("çiqim")

        val isIncome = lowerBody.contains("popolnenie") ||
            lowerBody.contains("zachislenie") ||
            lowerBody.contains("tushdi") ||
            lowerBody.contains("tuşdi") ||
            lowerBody.contains("kirim") ||
            lowerBody.contains("postuplenie") ||
            lowerBody.contains("sotuv") ||
            lowerBody.contains("sotildi") ||
            lowerBody.contains("savdo") ||
            lowerBody.contains("tushum") ||
            lowerBody.contains("tuşum") ||
            lowerBody.contains("to'ldirildi") ||
            lowerBody.contains("toldirildi") ||
            lowerBody.contains("toʻldirildi") ||
            lowerBody.contains("töldirildi") ||
            lowerBody.contains("perevod na kartu") ||
            lowerBody.contains("kartaga o'tkazma") ||
            lowerBody.contains("kartaga otkazma") ||
            lowerBody.contains("kartaga ötkazma")

        if (!isExpense && !isIncome) {
            // Not a financial confirmation SMS
            return null
        }

        val type = if (isIncome && !isExpense) TransactionType.INCOME else TransactionType.EXPENSE

        // 2. Extract Amount
        val amount = extractAmount(body) ?: return null
        if (amount <= 0) return null

        // 3. Extract Merchant / Location / Counterparty
        val merchant = extractMerchant(body, sender, type)

        // 4. First check if any user-defined CustomCategory matches keywords in merchant or body
        val matchedCustom = matchCustomCategory(merchant, lowerBody, type, customCategories)

        // 5. Detect standard Category
        val category = if (matchedCustom != null) {
            if (type == TransactionType.INCOME) Category.OTHER_INCOME else Category.OTHER_EXPENSE
        } else if (type == TransactionType.INCOME) {
            detectIncomeCategory(merchant, lowerBody)
        } else {
            detectExpenseCategory(merchant, lowerBody)
        }

        val sanitizedRawText = SmsSanitizer.sanitizeForAiAndStorage(body)

        return ParsedSmsTransaction(
            amount = amount,
            merchant = merchant,
            category = category,
            type = type,
            account = Account.CARD,
            rawText = sanitizedRawText,
            customCategoryName = matchedCustom?.name.orEmpty(),
            customCategoryIcon = matchedCustom?.iconKey.orEmpty(),
            customCategoryColor = matchedCustom?.colorHex ?: 0L,
            aiCategorized = false,
        )
    }

    private fun matchCustomCategory(
        merchant: String,
        lowerBody: String,
        type: TransactionType,
        customCategories: List<CustomCategory>,
    ): CustomCategory? {
        val target = (merchant + " " + lowerBody).lowercase(Locale.ROOT)
        return customCategories
            .filter { it.type == type }
            .firstOrNull { cat ->
                val cleanName = cat.name.trim().lowercase(Locale.ROOT)
                cleanName.length >= 3 && target.contains(cleanName)
            }
    }

    private fun extractAmount(body: String): Long? {
        // Examples:
        // "55 000.00 UZS", "55 000 UZS", "55000 UZS", "55 000 so'm", "55 000 söm", "55,000.00 sum"
        val regex = Regex("""([0-9]{1,3}(?:[ \u00A0,][0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+)\s*(?:uzs|so['`ʻ]?m|söm|sum)""", RegexOption.IGNORE_CASE)
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

    private fun extractMerchant(body: String, sender: String, type: TransactionType): String {
        // Patterns like:
        // "Terminal: KORZINKA", "Mesto: EVOS", "Joy: SAFIA", "Xizmat: Beeline", "v polzu EVOS", "Sotuv: ..."
        val explicitPatterns = listOf(
            Regex("""(?:terminal|mesto|joy|xizmat|usluga|v polzu|punkt|do'kon|dokon|dökon|magazin|mijoz|otpravitel|kimdan)[:\s]+([^;,\.\n]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:to'landi|tolandi|toʻlandi|tölandi|yechildi|yeçildi|tushdi|tuşdi|sotildi)[\.\s]+([^;,\.\n0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""HUMO:[\s0-9\.,UZS]+(?:to'landi|tölandi|yechildi|yeçildi|tushdi|tuşdi)[\.\s]+([^;,\.\n]+)""", RegexOption.IGNORE_CASE),
        )

        for (pattern in explicitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotBlank() &&
                    candidate.length in 2..40 &&
                    !candidate.lowercase().contains("balans") &&
                    !candidate.lowercase().contains("qoldiq") &&
                    !candidate.lowercase().contains("ostatok") &&
                    !candidate.lowercase().contains("karta")
                ) {
                    return candidate
                }
            }
        }

        // Fallback: Sender or generic
        return when {
            sender.isNotBlank() -> sender.uppercase(Locale.US)
            type == TransactionType.INCOME -> "Karta tuşumi"
            else -> "Karta xaridi"
        }
    }

    fun detectExpenseCategory(merchant: String, body: String): Category {
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
                target.contains("non") || target.contains("oziq") || target.contains("ovqat") ||
                target.contains("coffee") || target.contains("kofe") || target.contains("choyxona") ||
                target.contains("milliy taom") -> Category.FOOD

            // Transport & Fuel
            target.contains("yandex") || target.contains("mytaxi") || target.contains("taxi") ||
                target.contains("uklon") || target.contains("metro") || target.contains("atto") ||
                target.contains("toshshahartrans") || target.contains("zapravka") ||
                target.contains("ung") || target.contains("lukoil") || target.contains("benzovoz") ||
                target.contains("metan") || target.contains("propan") || target.contains("benzin") ||
                target.contains("moyka") || target.contains("avto") || target.contains("parking") -> Category.TRANSPORT

            // Utilities & Mobile/Internet
            target.contains("beeline") || target.contains("ucell") || target.contains("uztelecom") ||
                target.contains("mobiuz") || target.contains("humans") || target.contains("uzonline") ||
                target.contains("comnet") || target.contains("sarkor") || target.contains("turon") ||
                target.contains("tps") || target.contains("elektr") || target.contains("hududgaz") ||
                target.contains("gaz") || target.contains("suv") || target.contains("issiqlik") ||
                target.contains("mahsustrans") || target.contains("kommunal") ||
                target.contains("paynet") -> Category.UTILITIES

            // Rent / Housing
            target.contains("ijara") || target.contains("arenda") || target.contains("kvartira") -> Category.RENT

            // Health & Pharmacy
            target.contains("apteka") || target.contains("dorixona") || target.contains("farm") ||
                target.contains("dori") || target.contains("shifo") || target.contains("med") ||
                target.contains("klinika") || target.contains("stomatolog") || target.contains("tibbiyot") ||
                target.contains("grandpharm") || target.contains("oxymed") || target.contains("akfa medline") -> Category.HEALTH

            // Education
            target.contains("kurs") || target.contains("maktab") || target.contains("universitet") ||
                target.contains("institut") || target.contains("kontrakt") || target.contains("ta'lim") ||
                target.contains("talim") || target.contains("ustoz") || target.contains("kitob") -> Category.EDUCATION

            // Shopping
            target.contains("uzum market") || target.contains("asaxiy") || target.contains("zoodmall") ||
                target.contains("texnomart") || target.contains("mediapark") || target.contains("idea") ||
                target.contains("radius") || target.contains("waikiki") || target.contains("zara") ||
                target.contains("defacto") || target.contains("terra pro") || target.contains("kiyim") ||
                target.contains("mall") || target.contains("bazar") || target.contains("bozor") ||
                target.contains("magazin") || target.contains("do'kon") || target.contains("dokon") ||
                target.contains("xarid") -> Category.SHOPPING

            // Entertainment
            target.contains("kino") || target.contains("cinema") || target.contains("park") ||
                target.contains("bowling") || target.contains("playstation") || target.contains("club") ||
                target.contains("bilet") || target.contains("iticket") || target.contains("teatr") ||
                target.contains("netflix") || target.contains("spotify") || target.contains("youtube") -> Category.ENTERTAINMENT

            // Loans
            target.contains("kredit") || target.contains("ssuda") || target.contains("loan") ||
                target.contains("nasiya") || target.contains("muddatli") || target.contains("qarz") -> Category.LOANS

            else -> Category.OTHER_EXPENSE
        }
    }

    fun detectIncomeCategory(merchant: String, body: String): Category {
        val target = (merchant + " " + body).lowercase(Locale.ROOT)
        return when {
            target.contains("salary") || target.contains("zarplata") || target.contains("maosh") ||
                target.contains("oylik") || target.contains("avans") || target.contains("stipendiya") -> Category.SALARY
            target.contains("sotuv") || target.contains("sotildi") || target.contains("savdo") ||
                target.contains("tushum") || target.contains("biznes") || target.contains("do'kon") ||
                target.contains("magazin") || target.contains("qr") || target.contains("terminal") -> Category.BUSINESS
            target.contains("frilans") || target.contains("freelance") || target.contains("loyiha") ||
                target.contains("buyurtma") || target.contains("xizmat haqi") -> Category.FREELANCE
            target.contains("sovg'a") || target.contains("sovga") || target.contains("hadya") ||
                target.contains("mukofot") || target.contains("bonus") || target.contains("cashback") ||
                target.contains("keshbek") -> Category.GIFT
            else -> Category.OTHER_INCOME
        }
    }
}
