package com.example.myapplication.domain

import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class PeriodType { WEEK, MONTH }

data class PeriodSummary(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val income: Long,
    val expense: Long,
) {
    val net: Long get() = income - expense
    val hasActivity: Boolean get() = income != 0L || expense != 0L
}

data class CategoryAmount(
    val category: Category,
    val amount: Long,
    /** Share of the total in range 0..1. */
    val share: Float,
)

/** Pure, deterministic aggregation helpers. Weeks start on Monday. */
object StatsCalculator {

    fun periodStart(date: LocalDate, type: PeriodType): LocalDate = when (type) {
        PeriodType.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        PeriodType.MONTH -> date.withDayOfMonth(1)
    }

    fun periodEnd(start: LocalDate, type: PeriodType): LocalDate = when (type) {
        PeriodType.WEEK -> start.plusDays(6)
        PeriodType.MONTH -> start.plusMonths(1).minusDays(1)
    }

    fun shift(start: LocalDate, type: PeriodType, periods: Long): LocalDate = when (type) {
        PeriodType.WEEK -> start.plusWeeks(periods)
        PeriodType.MONTH -> start.plusMonths(periods)
    }

    fun periodLengthDays(start: LocalDate, type: PeriodType): Int =
        (ChronoUnit.DAYS.between(start, periodEnd(start, type)) + 1).toInt()

    /**
     * Returns the last [count] periods ending with the period that contains [today],
     * ordered oldest → newest. Periods without transactions are included with zeros.
     */
    fun summaries(
        transactions: List<TransactionEntity>,
        type: PeriodType,
        count: Int,
        today: LocalDate,
    ): List<PeriodSummary> {
        val currentStart = periodStart(today, type)
        val grouped = transactions.groupBy { periodStart(it.date, type) }
        return (count - 1 downTo 0).map { back ->
            val start = shift(currentStart, type, -back.toLong())
            val items = grouped[start].orEmpty()
            PeriodSummary(
                start = start,
                endInclusive = periodEnd(start, type),
                income = items.sumOf(TransactionType.INCOME),
                expense = items.sumOf(TransactionType.EXPENSE),
            )
        }
    }

    fun summaryFor(transactions: List<TransactionEntity>, type: PeriodType, date: LocalDate): PeriodSummary =
        summaries(transactions, type, 1, date).single()

    fun categoryBreakdown(
        transactions: List<TransactionEntity>,
        from: LocalDate,
        to: LocalDate,
        type: TransactionType = TransactionType.EXPENSE,
    ): List<CategoryAmount> {
        val amounts = categoryAmounts(transactions, from, to, type)
        return toShares(amounts)
    }

    fun categoryAmounts(
        transactions: List<TransactionEntity>,
        from: LocalDate,
        to: LocalDate,
        type: TransactionType = TransactionType.EXPENSE,
    ): Map<Category, Long> {
        val fromDay = from.toEpochDay()
        val toDay = to.toEpochDay()
        return transactions
            .filter { it.type == type && it.epochDay in fromDay..toDay }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
    }

    fun toShares(amounts: Map<Category, Long>): List<CategoryAmount> {
        val total = amounts.values.sum()
        if (total <= 0L) return emptyList()
        return amounts
            .filterValues { it > 0L }
            .map { (category, amount) -> CategoryAmount(category, amount, amount.toFloat() / total) }
            .sortedByDescending { it.amount }
    }

    fun balance(transactions: List<TransactionEntity>): Long =
        transactions.sumOf(TransactionType.INCOME) - transactions.sumOf(TransactionType.EXPENSE)

    fun accountBalance(transactions: List<TransactionEntity>, account: Account): Long {
        val accTransactions = transactions.filter { it.account == account }
        return accTransactions.sumOf(TransactionType.INCOME) - accTransactions.sumOf(TransactionType.EXPENSE)
    }

    fun accountBalances(transactions: List<TransactionEntity>): Map<Account, Long> =
        Account.entries.associateWith { accountBalance(transactions, it) }

    private fun List<TransactionEntity>.sumOf(type: TransactionType): Long =
        filter { it.type == type }.sumOf { it.amount }
}
