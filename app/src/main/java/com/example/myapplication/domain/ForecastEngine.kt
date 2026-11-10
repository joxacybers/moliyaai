package com.example.myapplication.domain

import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt

enum class Confidence { LOW, MEDIUM, HIGH }

data class Forecast(
    val periodType: PeriodType,
    val nextPeriodStart: LocalDate,
    val income: Long,
    val expense: Long,
    val expenseByCategory: List<CategoryAmount>,
    /** Average change per period relative to the mean, in percent. */
    val incomeTrendPct: Double,
    val expenseTrendPct: Double,
    /** Expense of the current (unfinished) period extrapolated at the current pace. */
    val currentPeriodProjectedExpense: Long,
    val basedOnPeriods: Int,
    val confidence: Confidence,
) {
    val net: Long get() = income - expense
}

/**
 * Deterministic forecast: a 50/50 blend of a linearly weighted moving average and a
 * least-squares linear trend over the last completed periods. The LLM never computes
 * numbers — it only explains the output of this engine.
 */
object ForecastEngine {

    const val MIN_PERIODS = 3
    private const val ROUND_TO = 1_000L

    fun historyLength(type: PeriodType): Int = when (type) {
        PeriodType.WEEK -> 12
        PeriodType.MONTH -> 6
    }

    /** Returns null when there are fewer than [MIN_PERIODS] completed periods with data. */
    fun forecast(transactions: List<TransactionEntity>, type: PeriodType, today: LocalDate): Forecast? {
        val history = completedHistory(transactions, type, today)
        if (history.size < MIN_PERIODS) return null

        val incomeSeries = history.map { it.income.toDouble() }
        val expenseSeries = history.map { it.expense.toDouble() }

        val categoryPredictions = Category.forType(TransactionType.EXPENSE).associateWith { category ->
            val series = history.map { period ->
                StatsCalculator.categoryAmounts(transactions, period.start, period.endInclusive)[category]
                    ?.toDouble() ?: 0.0
            }
            roundAmount(predict(series))
        }

        val currentStart = StatsCalculator.periodStart(today, type)
        return Forecast(
            periodType = type,
            nextPeriodStart = StatsCalculator.shift(currentStart, type, 1),
            income = roundAmount(predict(incomeSeries)),
            expense = roundAmount(predict(expenseSeries)),
            expenseByCategory = StatsCalculator.toShares(categoryPredictions),
            incomeTrendPct = trendPct(incomeSeries),
            expenseTrendPct = trendPct(expenseSeries),
            currentPeriodProjectedExpense = projectCurrentPeriod(transactions, type, today),
            basedOnPeriods = history.size,
            confidence = confidence(expenseSeries),
        )
    }

    /**
     * Completed periods (excluding the current one), starting from the period of the first
     * transaction and limited to [historyLength]. Oldest → newest.
     */
    fun completedHistory(
        transactions: List<TransactionEntity>,
        type: PeriodType,
        today: LocalDate,
    ): List<PeriodSummary> {
        val firstDay = transactions.minOfOrNull { it.epochDay } ?: return emptyList()
        val firstStart = StatsCalculator.periodStart(LocalDate.ofEpochDay(firstDay), type)
        val length = historyLength(type)
        return StatsCalculator.summaries(transactions, type, length + 1, today)
            .dropLast(1)
            .filter { !it.start.isBefore(firstStart) }
    }

    /** Extrapolates the current period's expense linearly by elapsed days. */
    fun projectCurrentPeriod(transactions: List<TransactionEntity>, type: PeriodType, today: LocalDate): Long {
        val current = StatsCalculator.summaryFor(transactions, type, today)
        val elapsed = ChronoUnit.DAYS.between(current.start, today) + 1
        val total = StatsCalculator.periodLengthDays(current.start, type)
        if (elapsed <= 0) return current.expense
        return roundAmount(current.expense.toDouble() / elapsed * total)
    }

    internal fun predict(series: List<Double>): Double {
        if (series.isEmpty()) return 0.0
        val n = series.size
        val weightSum = n * (n + 1) / 2.0
        val wma = series.mapIndexed { i, v -> v * (i + 1) }.sum() / weightSum
        val (intercept, slope) = linearRegression(series)
        val trendNext = intercept + slope * n
        return max(0.0, 0.5 * wma + 0.5 * trendNext)
    }

    /** Least squares over x = 0..n-1. Returns (intercept, slope). */
    internal fun linearRegression(series: List<Double>): Pair<Double, Double> {
        val n = series.size
        if (n < 2) return (series.firstOrNull() ?: 0.0) to 0.0
        val xMean = (n - 1) / 2.0
        val yMean = series.average()
        var num = 0.0
        var den = 0.0
        series.forEachIndexed { x, y ->
            num += (x - xMean) * (y - yMean)
            den += (x - xMean) * (x - xMean)
        }
        val slope = if (den == 0.0) 0.0 else num / den
        return (yMean - slope * xMean) to slope
    }

    internal fun trendPct(series: List<Double>): Double {
        val mean = series.average()
        if (series.size < 2 || mean == 0.0) return 0.0
        return linearRegression(series).second / mean * 100.0
    }

    internal fun confidence(series: List<Double>): Confidence {
        val mean = series.average()
        if (mean <= 0.0) return Confidence.LOW
        val sd = sqrt(series.sumOf { (it - mean) * (it - mean) } / series.size)
        val cv = sd / mean
        return when {
            cv >= 0.45 -> Confidence.LOW
            series.size < 4 -> Confidence.MEDIUM
            cv < 0.2 -> Confidence.HIGH
            else -> Confidence.MEDIUM
        }
    }

    private fun roundAmount(value: Double): Long = (value / ROUND_TO).roundToLong() * ROUND_TO
}
