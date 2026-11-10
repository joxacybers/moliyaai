package com.example.myapplication.ai

import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Category
import com.example.myapplication.domain.ForecastEngine
import com.example.myapplication.domain.PeriodType
import com.example.myapplication.domain.StatsCalculator
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object PromptBuilder {

    private val json = Json { encodeDefaults = true; explicitNulls = false }

    val SYSTEM_INSTRUCTION = """
        You are a careful, friendly personal finance advisor inside a budgeting app used in Uzbekistan.
        All amounts are in Uzbek so'm (UZS).
        Rules:
        - Use ONLY the numbers present in the provided data. Never invent transactions or amounts.
        - The forecast was already computed by the app with a statistical model. Explain and
          interpret it; do not recompute or contradict it.
        - Give 3 to 5 specific, actionable recommendations tied to the categories and trends in the
          data. Avoid generic advice. Do not recommend specific banks, stocks, crypto or loans.
        - If the data is limited (few periods or low confidence), say so briefly.
        - Keep "summary" to 2-3 sentences. Keep each recommendation "detail" to at most 2 sentences.
        - estimatedMonthlySavings is an optional rough monthly amount in UZS; omit it if unsure.
        - Format numbers with spaces as thousand separators (e.g. 1 250 000 so'm).
        - You MUST respond strictly in valid JSON matching this structure:
          {
            "summary": "string",
            "risks": ["string", "string"],
            "recommendations": [
              {
                "title": "string",
                "detail": "string",
                "estimatedMonthlySavings": 150000
              }
            ]
          }
    """.trimIndent()

    fun buildPrompt(input: AiInput, languageName: String): String = """
        Analyze my monthly income and expenses and the forecast for the next month.
        Identify risks (overspending, negative net, budget overrun, fast-growing categories) and
        give practical monthly recommendations. Respond in $languageName as a JSON object.

        DATA (JSON):
        ${json.encodeToString(AiInput.serializer(), input)}
    """.trimIndent()

    fun buildInput(
        transactions: List<TransactionEntity>,
        type: PeriodType,
        today: LocalDate,
        monthlyBudget: Long,
    ): AiInput {
        val history = ForecastEngine.completedHistory(transactions, type, today)
        val current = StatsCalculator.summaryFor(transactions, type, today)
        val windowStart = history.firstOrNull()?.start ?: current.start

        val categories = StatsCalculator.categoryBreakdown(transactions, windowStart, current.endInclusive)
            .take(6)
            .map { share ->
                AiCategory(
                    category = share.category.promptName(),
                    amount = share.amount,
                    sharePct = (share.share * 100).roundToInt(),
                    changePct = categoryChangePct(transactions, share.category, history),
                )
            }

        val forecast = ForecastEngine.forecast(transactions, type, today)?.let { f ->
            AiForecast(
                nextPeriodStart = f.nextPeriodStart.toString(),
                income = f.income,
                expense = f.expense,
                net = f.net,
                incomeTrendPctPerPeriod = f.incomeTrendPct.round1(),
                expenseTrendPctPerPeriod = f.expenseTrendPct.round1(),
                confidence = f.confidence.name.lowercase(),
                basedOnPeriods = f.basedOnPeriods,
                expenseByCategory = f.expenseByCategory.take(6).map {
                    AiCategory(it.category.promptName(), it.amount, (it.share * 100).roundToInt())
                },
            )
        }

        return AiInput(
            periodType = "monthly",
            today = today.toString(),
            balance = StatsCalculator.balance(transactions),
            monthlyBudget = monthlyBudget.takeIf { it > 0 },
            history = history.map { AiPeriod(it.start.toString(), it.income, it.expense) },
            currentPeriod = AiCurrentPeriod(
                start = current.start.toString(),
                daysElapsed = (ChronoUnit.DAYS.between(current.start, today) + 1).toInt(),
                daysTotal = StatsCalculator.periodLengthDays(current.start, type),
                incomeSoFar = current.income,
                expenseSoFar = current.expense,
                projectedExpense = ForecastEngine.projectCurrentPeriod(transactions, type, today),
            ),
            topExpenseCategories = categories,
            forecast = forecast,
        )
    }

    private fun categoryChangePct(
        transactions: List<TransactionEntity>,
        category: Category,
        history: List<com.example.myapplication.domain.PeriodSummary>,
    ): Int? {
        if (history.size < 2) return null
        val amounts = history.map {
            StatsCalculator.categoryAmounts(transactions, it.start, it.endInclusive)[category] ?: 0L
        }
        val last = amounts.last()
        val previousAvg = amounts.dropLast(1).average()
        if (previousAvg <= 0.0) return null
        return ((last - previousAvg) / previousAvg * 100).roundToInt()
    }

    private fun Category.promptName(): String = name.lowercase().replace('_', ' ')

    private fun Double.round1(): Double = (this * 10).roundToInt() / 10.0
}
