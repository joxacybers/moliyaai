package com.example.myapplication.domain

import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ForecastEngineTest {

    private val today = LocalDate.of(2026, 11, 10) // Tuesday

    @Test
    fun linearRegression_computes_exact_slope_and_intercept() {
        val (intercept, slope) = ForecastEngine.linearRegression(listOf(10.0, 20.0, 30.0, 40.0))
        assertEquals(10.0, intercept, 1e-6)
        assertEquals(10.0, slope, 1e-6)
    }

    @Test
    fun predict_handles_flat_series() {
        val next = ForecastEngine.predict(listOf(100.0, 100.0, 100.0, 100.0))
        assertEquals(100.0, next, 1e-3)
    }

    @Test
    fun predict_reflects_growing_trend() {
        val next = ForecastEngine.predict(listOf(10.0, 20.0, 30.0, 40.0))
        // series mean is 25, last is 40; blend of WMA (30) and linear extrapolation (50) gives exactly 40.0
        assertEquals(40.0, next, 1e-6)
        val strictlyGrowingNext = ForecastEngine.predict(listOf(10.0, 10.0, 10.0, 40.0))
        assertTrue(strictlyGrowingNext > 17.5) // well above series mean of 17.5
    }

    @Test
    fun forecast_returns_null_when_history_is_too_short() {
        val transactions = listOf(
            TransactionEntity(
                amount = 1_000_000,
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                epochDay = today.minusWeeks(1).toEpochDay(),
            ),
        )
        val result = ForecastEngine.forecast(transactions, PeriodType.WEEK, today)
        assertNull(result)
    }

    @Test
    fun forecast_produces_valid_numbers_with_enough_history() {
        val transactions = mutableListOf<TransactionEntity>()
        // 5 completed weeks of consistent expense
        for (w in 5 downTo 1) {
            val date = today.minusWeeks(w.toLong())
            transactions += TransactionEntity(
                amount = 2_000_000,
                type = TransactionType.INCOME,
                category = Category.SALARY,
                epochDay = date.toEpochDay(),
            )
            transactions += TransactionEntity(
                amount = 1_000_000,
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                epochDay = date.toEpochDay(),
            )
        }

        val forecast = ForecastEngine.forecast(transactions, PeriodType.WEEK, today)
        assertNotNull(forecast)
        assertEquals(PeriodType.WEEK, forecast!!.periodType)
        assertTrue(forecast.income > 0)
        assertTrue(forecast.expense > 0)
        assertTrue(forecast.basedOnPeriods >= 3)
    }
}
