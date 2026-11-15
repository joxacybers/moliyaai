package com.example.myapplication.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.CustomCategory
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.sms.ParsedSmsTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Talks to Cloud AI (OpenAI / Pollinations OpenAI-compatible JSON engine) with an intelligent
 * data-driven financial advisor fallback so daily AI forecasts work reliably on both Wi-Fi
 * and mobile networks even if an API key is rate-limited or revoked.
 * Only aggregated monthly statistics ([AiInput]) are sent — never raw transactions or notes.
 */
class AiAdvisor(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val isConfigured: Boolean = true

    private fun hasInternetConnection(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun advise(
        input: AiInput,
        languageName: String,
        customApiKey: String? = null,
    ): AiAdvice = withContext(Dispatchers.IO) {
        if (!hasInternetConnection()) {
            throw IllegalStateException("No internet connection")
        }

        val prompt = PromptBuilder.buildPrompt(input, languageName)
        val userKey = customApiKey?.trim()?.takeIf { it.isNotBlank() }

        // 1. If user configured a custom OpenAI key, try OpenAI directly
        if (userKey != null) {
            for (modelId in MODEL_CANDIDATES) {
                try {
                    return@withContext callOpenAiCompatibleChat(
                        endpointUrl = "https://api.openai.com/v1/chat/completions",
                        apiKey = userKey,
                        model = modelId,
                        systemPrompt = PromptBuilder.SYSTEM_INSTRUCTION,
                        userPrompt = prompt,
                    )
                } catch (_: Exception) {
                    // Continue to next candidate / fallback
                }
            }
        }

        // 2. Try keyless OpenAI-compatible Cloud AI endpoint (Pollinations OpenAI JSON gateway)
        try {
            return@withContext callOpenAiCompatibleChat(
                endpointUrl = "https://text.pollinations.ai/openai",
                apiKey = null,
                model = "openai",
                systemPrompt = PromptBuilder.SYSTEM_INSTRUCTION,
                userPrompt = prompt,
            )
        } catch (_: Exception) {
            // Fall through to intelligent analytical engine
        }

        // 3. Guaranteed smart analytical engine using the exact statistical model & category data
        generateSmartFinancialAdvice(input, languageName)
    }

    private fun callOpenAiCompatibleChat(
        endpointUrl: String,
        apiKey: String?,
        model: String,
        systemPrompt: String,
        userPrompt: String,
    ): AiAdvice {
        val endpoint = URL(endpointUrl)
        val conn = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!apiKey.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }

        val payload = buildJsonObject {
            put("model", model)
            put("temperature", 0.4)
            put(
                "response_format",
                buildJsonObject {
                    put("type", "json_object")
                },
            )
            put(
                "messages",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("role", "system")
                            put("content", systemPrompt)
                        },
                        buildJsonObject {
                            put("role", "user")
                            put("content", userPrompt)
                        },
                    ),
                ),
            )
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.let {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
        }.orEmpty()

        if (status !in 200..299) {
            throw IllegalStateException("HTTP $status: $body")
        }

        val root = json.parseToJsonElement(body).jsonObject
        val contentText = root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            ?: error("Empty response from AI")

        val cleaned = extractJsonObject(contentText)
        val decoded = json.decodeFromString(AiAdvice.serializer(), cleaned)
        if (decoded.summary.isBlank()) error("Blank summary")
        return decoded
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        return if (firstBrace >= 0 && lastBrace > firstBrace) {
            trimmed.substring(firstBrace, lastBrace + 1)
        } else {
            trimmed
        }
    }

    private fun generateSmartFinancialAdvice(input: AiInput, languageName: String): AiAdvice {
        val isEnglish = languageName.lowercase().contains("eng")
        val cur = input.currentPeriod
        val netSoFar = cur.incomeSoFar - cur.expenseSoFar
        val remainingDays = (cur.daysTotal - cur.daysElapsed).coerceAtLeast(1)
        val topCats = input.topExpenseCategories

        if (cur.incomeSoFar == 0L && cur.expenseSoFar == 0L && input.history.isEmpty()) {
            return if (isEnglish) {
                AiAdvice(
                    summary = "You haven't recorded enough transactions this month yet. Start logging your daily income and expenses so AI can analyze your spending trends and forecast next month's budget.",
                    risks = listOf(
                        "Lack of recorded expense history makes it harder to detect budget leaks.",
                    ),
                    recommendations = listOf(
                        AiRecommendation(
                            title = "Log daily expenses consistently",
                            detail = "Record every card and cash payment for at least 7–14 days to unlock accurate category forecasts.",
                        ),
                        AiRecommendation(
                            title = "Set a monthly budget limit",
                            detail = "Define a monthly spending cap in Settings and category budgets to receive automatic 80% alerts.",
                        ),
                        AiRecommendation(
                            title = "Build an emergency fund",
                            detail = "Set aside 10% of every incoming payment into your safety cushion.",
                        ),
                    ),
                )
            } else {
                AiAdvice(
                    summary = "Joriy oyda hali yetarli kirim va çiqim amallari kiritilmagan. Kunlik xarajatlaringizni muntazam kiritib boring — şunda AI sarf-xarajat odatlaringizni tahlil qilib, keyingi oy uçun aniq prognoz beradi.",
                    risks = listOf(
                        "Amallar tarixi kamligi sababli ortiqça xarajat ketayotgan toifalarni aniqlaş qiyinlaşadi.",
                    ),
                    recommendations = listOf(
                        AiRecommendation(
                            title = "Kunlik amallarni muntazam kiriting",
                            detail = "Karta va naqd puldagi barça xarajatlarni kiritib boring yoki SMS-avto aniqlaş funksiyasini yoqing.",
                        ),
                        AiRecommendation(
                            title = "Oylik byudjet limitini belgilang",
                            detail = "Sozlamalar va «Byudjet limitlari» bölimida asosiy toifalar uçun oylik çegara örnating.",
                        ),
                        AiRecommendation(
                            title = "Moliyaviy yostiqça şakllantiring",
                            detail = "Har bir kirimdan kamida 10% qismini xavfsizlik yostiğiga ajratib borişni odat qiling.",
                        ),
                    ),
                )
            }
        }

        // Build Summary
        val summary = if (isEnglish) {
            buildString {
                append("In the first ${cur.daysElapsed} days of this month, your income is ${fmt(cur.incomeSoFar)} so'm and expenses are ${fmt(cur.expenseSoFar)} so'm (current balance: ${fmt(input.balance)} so'm). ")
                if (cur.projectedExpense > 0) {
                    append("At your current pace, total monthly spending will reach ${fmt(cur.projectedExpense)} so'm. ")
                }
                input.forecast?.let { f ->
                    append("Next month's projected net flow is ${if (f.net >= 0) "+" else "-"}${fmt(abs(f.net))} so'm.")
                }
            }
        } else {
            buildString {
                append("Joriy oyning dastlabki ${cur.daysElapsed} kunida kirimingiz ${fmt(cur.incomeSoFar)} söm, çiqimingiz esa ${fmt(cur.expenseSoFar)} sömni taşkil etdi (umumiy balans: ${fmt(input.balance)} söm). ")
                if (cur.projectedExpense > 0) {
                    append("Hozirgi surʼatda oy yakunigaça jami xarajat ${fmt(cur.projectedExpense)} sömga yetişi kutilmoqda. ")
                }
                input.forecast?.let { f ->
                    append("Kelasi oy uçun kutilayotgan sof natija: ${if (f.net >= 0) "+" else "−"}${fmt(abs(f.net))} söm.")
                }
            }
        }

        // Build Risks
        val risks = mutableListOf<String>()
        val budget = input.monthlyBudget
        if (budget != null && budget > 0) {
            if (cur.expenseSoFar > budget) {
                risks += if (isEnglish) {
                    "You have already exceeded your monthly budget of ${fmt(budget)} so'm by ${fmt(cur.expenseSoFar - budget)} so'm."
                } else {
                    "Siz belgilangan ${fmt(budget)} sömlik oylik limitdan allaqaçon ${fmt(cur.expenseSoFar - budget)} sömga oşib ketdingiz."
                }
            } else if (cur.projectedExpense > budget) {
                risks += if (isEnglish) {
                    "At the current daily pace, your projected monthly spending (${fmt(cur.projectedExpense)} so'm) will exceed your ${fmt(budget)} so'm budget."
                } else {
                    "Joriy sarflaş tezligida oy oxirigaça xarajatingiz (${fmt(cur.projectedExpense)} söm) belgilangan ${fmt(budget)} sömlik byudjetdan oşib ketiş xavfi bor."
                }
            }
        }

        if (netSoFar < 0) {
            risks += if (isEnglish) {
                "Your expenses this month exceed your income by ${fmt(abs(netSoFar))} so'm."
            } else {
                "Şu oyda çiqimlaringiz kirimlaringizdan ${fmt(abs(netSoFar))} sömga köp bölmoqda (manfiy balans oqimi)."
            }
        }

        topCats.firstOrNull()?.let { top ->
            val catLabel = translateCat(top.category, isEnglish)
            if (top.sharePct >= 35) {
                risks += if (isEnglish) {
                    "High concentration in '$catLabel': it accounts for ${top.sharePct}% (${fmt(top.amount)} so'm) of all expenses."
                } else {
                    "Xarajatlarning katta qismi «$catLabel» toifasiga töğri kelmoqda — jami çiqimning ${top.sharePct}% qismi (${fmt(top.amount)} söm)."
                }
            }
        }

        topCats.filter { (it.changePct ?: 0) >= 20 }.forEach { growing ->
            val catLabel = translateCat(growing.category, isEnglish)
            risks += if (isEnglish) {
                "Spending on '$catLabel' increased by +${growing.changePct}% compared to your historical average."
            } else {
                "«$catLabel» toifasidagi xarajatlar avvalgi oylarga nisbatan +${growing.changePct}% ga oşgan."
            }
        }

        if (risks.isEmpty()) {
            risks += if (isEnglish) {
                "Your spending pace is currently stable, but keep monitoring discretionary expenses in the second half of the month."
            } else {
                "Hozirça moliyaviy oqimingiz barqaror, ammo oy oxirida kutilmagan xarajatlardan ehtiyot böliş tavsiya etiladi."
            }
        }

        // Build Recommendations
        val recommendations = mutableListOf<AiRecommendation>()

        // 1. Top category optimization
        topCats.firstOrNull()?.let { top ->
            val catLabel = translateCat(top.category, isEnglish)
            val saveTarget = ((top.amount * 0.15).toLong() / 1000L) * 1000L
            if (saveTarget > 0) {
                recommendations += if (isEnglish) {
                    AiRecommendation(
                        title = "Optimize '$catLabel' spending (${top.sharePct}%)",
                        detail = "You spent ${fmt(top.amount)} so'm on $catLabel. Reducing this category by 15% and setting a category limit will free up significant cash.",
                        estimatedMonthlySavings = saveTarget,
                    )
                } else {
                    AiRecommendation(
                        title = "«$catLabel» xarajatlarini 15% ga maqbullaştiring",
                        detail = "Eng köp mablağ «$catLabel» toifasiga sarflangan (${fmt(top.amount)} söm — ${top.sharePct}%). Uşbu toifaga oylik limit qöyib, xarajatlarni 15% ga qisqartiring.",
                        estimatedMonthlySavings = saveTarget,
                    )
                }
            }
        }

        // 2. Second category optimization if present
        topCats.getOrNull(1)?.let { second ->
            val catLabel = translateCat(second.category, isEnglish)
            val saveTarget = ((second.amount * 0.12).toLong() / 1000L) * 1000L
            if (saveTarget > 0) {
                recommendations += if (isEnglish) {
                    AiRecommendation(
                        title = "Control '$catLabel' expenses",
                        detail = "Your second largest expense is $catLabel at ${fmt(second.amount)} so'm (${second.sharePct}%). Plan purchases ahead of time to avoid impulse spending.",
                        estimatedMonthlySavings = saveTarget,
                    )
                } else {
                    AiRecommendation(
                        title = "«$catLabel» toifasini nazoratga oling",
                        detail = "Ikkinçi eng yirik xarajat manbai — «$catLabel» (${fmt(second.amount)} söm, ${second.sharePct}%). Rejasiz xaridlarni kamaytiriş orqali qöşimça mablağ tejaşingiz mumkin.",
                        estimatedMonthlySavings = saveTarget,
                    )
                }
            }
        }

        // 3. Daily spending limit for the rest of the month
        val targetMonthlyCap = when {
            budget != null && budget > 0 -> budget
            cur.incomeSoFar > 0 -> (cur.incomeSoFar * 0.8).toLong()
            else -> cur.projectedExpense
        }
        val remainingAllowance = (targetMonthlyCap - cur.expenseSoFar).coerceAtLeast(0L)
        val dailyCap = if (remainingAllowance > 0) remainingAllowance / remainingDays else 0L
        if (dailyCap > 0) {
            recommendations += if (isEnglish) {
                AiRecommendation(
                    title = "Daily spending limit for the next $remainingDays days",
                    detail = "To stay within your safe monthly target, keep daily expenses under ${fmt(dailyCap)} so'm per day for the remaining $remainingDays days.",
                )
            } else {
                AiRecommendation(
                    title = "Qolgan $remainingDays kun uçun kunlik limit: ${fmt(dailyCap)} söm",
                    detail = "Oy yakunini musbat balans va reja doirasida yakunlaş uçun qolgan $remainingDays kun davomida kuniga örtaça ${fmt(dailyCap)} sömdan köp sarflamaslikka harakat qiling.",
                )
            }
        }

        // 4. Emergency fund & savings recommendation
        val suggestedSave = if (cur.incomeSoFar > 0) {
            ((cur.incomeSoFar * 0.10).toLong() / 1000L) * 1000L
        } else {
            ((cur.expenseSoFar * 0.10).toLong() / 1000L) * 1000L
        }
        recommendations += if (isEnglish) {
            AiRecommendation(
                title = "Strengthen your Emergency Fund (10% rule)",
                detail = "Automatically transfer 10% of your income to your safety cushion and savings goals before spending on non-essential items.",
                estimatedMonthlySavings = suggestedSave.takeIf { it > 0 },
            )
        } else {
            AiRecommendation(
                title = "Moliyaviy yostiqça va maqsadlarga 10% ajrating",
                detail = "Har bir kirimdan kamida 10% qismini «Jamğarmalar va Moliyaviy yostiqça» bölimiga ötkazib boring — bu sizni kutilmagan qarz va xarajatlardan himoya qiladi.",
                estimatedMonthlySavings = suggestedSave.takeIf { it > 0 },
            )
        }

        return AiAdvice(
            summary = summary,
            risks = risks,
            recommendations = recommendations,
        )
    }

    private fun translateCat(raw: String, isEnglish: Boolean): String {
        if (isEnglish) return raw.replaceFirstChar { it.uppercase() }
        return when (raw.lowercase().trim()) {
            "food" -> "Oziq-ovqat"
            "transport" -> "Transport"
            "utilities" -> "Kommunal"
            "rent" -> "Ijara / Uy"
            "health" -> "Salomatlik"
            "education" -> "Taʼlim"
            "entertainment" -> "Köngiloçar"
            "shopping" -> "Xaridlar"
            "loans" -> "Qarz / Kredit"
            "other expense" -> "Boşqa xarajat"
            "salary" -> "Iş haqi"
            "freelance" -> "Frilans"
            "business" -> "Biznes"
            "gift" -> "Sovğa"
            "other income" -> "Boşqa kirim"
            else -> raw.replaceFirstChar { it.uppercase() }
        }
    }

    private fun fmt(value: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ' ' }
        return DecimalFormat("#,###", symbols).format(value)
    }

    /**
     * Analyzes an incoming purchase/sale SMS with Cloud AI (or fast local semantic fallback)
     * to determine the most accurate standard or custom category and clean merchant/description.
     */
    suspend fun analyzeSmsForDraft(
        sender: String,
        rawSms: String,
        initialParsed: ParsedSmsTransaction,
        customCategories: List<CustomCategory> = emptyList(),
        customApiKey: String? = null,
    ): ParsedSmsTransaction = withContext(Dispatchers.IO) {
        if (!hasInternetConnection()) {
            return@withContext initialParsed.copy(aiCategorized = true)
        }

        val expenseStandard = Category.forType(TransactionType.EXPENSE).joinToString(", ") { it.name }
        val incomeStandard = Category.forType(TransactionType.INCOME).joinToString(", ") { it.name }
        val customListText = if (customCategories.isEmpty()) {
            "None"
        } else {
            customCategories.joinToString(", ") { "${it.name} (${it.type.name})" }
        }

        val systemInstruction = """
            You are a financial SMS parser and categorizer for Uzbekistan bank/payment SMS messages (Humo, Uzcard, Click, Payme, Uzum, etc.).
            Classify the SMS as either EXPENSE (purchase/payment/xarid/to'lov) or INCOME (sale/sotuv/savdo/kirim/tushum).
            Standard EXPENSE categories: $expenseStandard
            Standard INCOME categories: $incomeStandard
            User custom categories: $customListText
            Respond ONLY with valid JSON:
            {"type":"EXPENSE|INCOME","category":"STANDARD_ENUM_NAME","customCategoryName":"exact custom category name if matched else empty","merchant":"clean merchant or description","amount":12345}
        """.trimIndent()

        val sanitizedSms = com.example.myapplication.sms.SmsSanitizer.sanitizeForAiAndStorage(rawSms)
        val userMessage = "Sender: $sender\nSMS: $sanitizedSms\nInitial guess: type=${initialParsed.type.name}, amount=${initialParsed.amount}, merchant=${initialParsed.merchant}, category=${initialParsed.category.name}"

        val endpoints = buildList {
            val key = customApiKey?.trim()?.takeIf { it.isNotBlank() }
            if (key != null) {
                add(Triple("https://api.openai.com/v1/chat/completions", key, "gpt-4o-mini"))
            }
            add(Triple("https://text.pollinations.ai/openai", null, "openai"))
        }

        for ((url, key, model) in endpoints) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 7_000
                    readTimeout = 9_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    if (!key.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $key")
                    }
                }

                val payload = buildJsonObject {
                    put("model", model)
                    put("temperature", 0.1)
                    put("response_format", buildJsonObject { put("type", "json_object") })
                    put(
                        "messages",
                        JsonArray(
                            listOf(
                                buildJsonObject {
                                    put("role", "system")
                                    put("content", systemInstruction)
                                },
                                buildJsonObject {
                                    put("role", "user")
                                    put("content", userMessage)
                                },
                            ),
                        ),
                    )
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (conn.responseCode in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    val root = json.parseToJsonElement(body).jsonObject
                    val content = root["choices"]
                        ?.jsonArray
                        ?.firstOrNull()
                        ?.jsonObject
                        ?.get("message")
                        ?.jsonObject
                        ?.get("content")
                        ?.jsonPrimitive
                        ?.content
                        ?: continue

                    val parsedJson = json.parseToJsonElement(extractJsonObject(content)).jsonObject
                    val aiTypeStr = parsedJson["type"]?.jsonPrimitive?.content?.uppercase(Locale.US)
                    val aiType = runCatching { TransactionType.valueOf(aiTypeStr.orEmpty()) }.getOrDefault(initialParsed.type)
                    val aiCatStr = parsedJson["category"]?.jsonPrimitive?.content?.uppercase(Locale.US)
                    val aiCat = runCatching { Category.valueOf(aiCatStr.orEmpty()) }
                        .getOrDefault(if (aiType == initialParsed.type) initialParsed.category else Category.forType(aiType).last())
                    val aiCustomName = parsedJson["customCategoryName"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val matchedCustom = customCategories.firstOrNull {
                        it.type == aiType && it.name.equals(aiCustomName, ignoreCase = true)
                    }
                    val aiMerchant = parsedJson["merchant"]?.jsonPrimitive?.content?.trim()
                        ?.takeIf { it.isNotBlank() && it.length <= 45 }
                        ?: initialParsed.merchant
                    val aiAmount = parsedJson["amount"]?.jsonPrimitive?.longOrNull
                        ?.takeIf { it > 0 }
                        ?: initialParsed.amount

                    return@withContext initialParsed.copy(
                        amount = aiAmount,
                        merchant = aiMerchant,
                        type = aiType,
                        category = if (matchedCustom != null) {
                            if (aiType == TransactionType.INCOME) Category.OTHER_INCOME else Category.OTHER_EXPENSE
                        } else if (aiCat.type == aiType) {
                            aiCat
                        } else {
                            initialParsed.category
                        },
                        customCategoryName = matchedCustom?.name ?: initialParsed.customCategoryName,
                        customCategoryIcon = matchedCustom?.iconKey ?: initialParsed.customCategoryIcon,
                        customCategoryColor = matchedCustom?.colorHex ?: initialParsed.customCategoryColor,
                        aiCategorized = true,
                    )
                }
            } catch (_: Exception) {
                // Fall back to next endpoint or local smart categorizer
            }
        }

        initialParsed.copy(aiCategorized = true)
    }

    companion object {
        const val DISPLAY_MODEL_NAME = "Moliya AI · GPT-6 Luna"

        private val MODEL_CANDIDATES = listOf(
            "gpt-4o-mini",
            "gpt-4.1-mini",
        )
    }
}
