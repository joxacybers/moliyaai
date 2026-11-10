package com.example.myapplication.domain

import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsCalculatorTest {

    private val today = LocalDate.of(2026, 11, 10) // Tuesday

    @Test
    fun periodStart_and_end_for_week() {
        val start = StatsCalculator.periodStart(today, PeriodType.WEEK)
        val end = StatsCalculator.periodEnd(start, PeriodType.WEEK)
        assertEquals(LocalDate.of(2026, 11, 9), start) // Monday
        assertEquals(LocalDate.of(2026, 11, 15), end) // Sunday
    }

    @Test
    fun periodStart_and_end_for_month() {
        val start = StatsCalculator.periodStart(today, PeriodType.MONTH)
        val end = StatsCalculator.periodEnd(start, PeriodType.MONTH)
        assertEquals(LocalDate.of(2026, 11, 1), start)
        assertEquals(LocalDate.of(2026, 11, 30), end)
    }

    @Test
    fun balance_sums_incomes_minus_expenses() {
        val list = listOf(
            TransactionEntity(amount = 5_000_000, type = TransactionType.INCOME, category = Category.SALARY, epochDay = today.toEpochDay()),
            TransactionEntity(amount = 1_200_000, type = TransactionType.EXPENSE, category = Category.FOOD, epochDay = today.toEpochDay()),
            TransactionEntity(amount = 300_000, type = TransactionType.EXPENSE, category = Category.TRANSPORT, epochDay = today.toEpochDay()),
        )
        assertEquals(3_500_000L, StatsCalculator.balance(list))
    }

    @Test
    fun summaries_returns_consecutive_periods_with_zeros_for_empty_ones() {
        val t1 = TransactionEntity(
            amount = 100_000,
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            epochDay = today.minusWeeks(2).toEpochDay(),
        )
        val summaries = StatsCalculator.summaries(listOf(t1), PeriodType.WEEK, 4, today)
        assertEquals(4, summaries.size)
        assertEquals(100_000L, summaries[1].expense)
        assertEquals(0L, summaries[0].expense)
        assertEquals(0L, summaries[2].expense)
        assertEquals(0L, summaries[3].expense)
    }

    @Test
    fun categoryBreakdown_calculates_shares_correctly() {
        val list = listOf(
            TransactionEntity(amount = 600_000, type = TransactionType.EXPENSE, category = Category.FOOD, epochDay = today.toEpochDay()),
            TransactionEntity(amount = 400_000, type = TransactionType.EXPENSE, category = Category.TRANSPORT, epochDay = today.toEpochDay()),
        )
        val breakdown = StatsCalculator.categoryBreakdown(list, today.minusDays(1), today.plusDays(1))
        assertEquals(2, breakdown.size)
        assertEquals(Category.FOOD, breakdown[0].category)
        assertEquals(0.6f, breakdown[0].share, 0.001f)
        assertEquals(Category.TRANSPORT, breakdown[1].category)
        assertEquals(0.4f, breakdown[1].share, 0.001f)
    }
}
