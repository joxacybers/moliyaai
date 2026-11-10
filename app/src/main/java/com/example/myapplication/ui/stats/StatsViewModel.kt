package com.example.myapplication.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.domain.CategoryAmount
import com.example.myapplication.domain.PeriodSummary
import com.example.myapplication.domain.PeriodType
import com.example.myapplication.domain.StatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatsUiState(
    val periodType: PeriodType = PeriodType.MONTH,
    val categoryType: TransactionType = TransactionType.EXPENSE,
    val summaries: List<PeriodSummary> = emptyList(),
    val currentPeriod: PeriodSummary = PeriodSummary(LocalDate.now(), LocalDate.now(), 0, 0),
    val previousPeriod: PeriodSummary = PeriodSummary(LocalDate.now(), LocalDate.now(), 0, 0),
    val categoryBreakdown: List<CategoryAmount> = emptyList(),
    val categoryTotal: Long = 0L,
    val expenseChangePct: Double? = null,
    val averageExpense: Long = 0,
)

class StatsViewModel(repo: TransactionRepository) : ViewModel() {

    private val periodType = MutableStateFlow(PeriodType.MONTH)
    private val categoryType = MutableStateFlow(TransactionType.EXPENSE)

    val uiState: StateFlow<StatsUiState> = combine(
        repo.observeAll(),
        periodType,
        categoryType,
    ) { transactions, type, catType ->
        val today = LocalDate.now()
        val count = if (type == PeriodType.WEEK) 8 else 6
        val summaries = StatsCalculator.summaries(transactions, type, count, today)
        val current = summaries.lastOrNull() ?: PeriodSummary(today, today, 0, 0)
        val previous = summaries.getOrNull(summaries.size - 2) ?: PeriodSummary(today, today, 0, 0)

        val breakdown = StatsCalculator.categoryBreakdown(
            transactions = transactions,
            from = current.start,
            to = current.endInclusive,
            type = catType,
        )
        val catTotal = breakdown.sumOf { it.amount }

        val changePct = if (previous.expense > 0) {
            ((current.expense - previous.expense).toDouble() / previous.expense) * 100
        } else {
            null
        }

        val completedExpenses = summaries.dropLast(1).map { it.expense }.filter { it > 0 }
        val avgExpense = if (completedExpenses.isNotEmpty()) completedExpenses.average().toLong() else current.expense

        StatsUiState(
            periodType = type,
            categoryType = catType,
            summaries = summaries,
            currentPeriod = current,
            previousPeriod = previous,
            categoryBreakdown = breakdown,
            categoryTotal = catTotal,
            expenseChangePct = changePct,
            averageExpense = avgExpense,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setPeriodType(type: PeriodType) {
        periodType.value = type
    }

    fun setCategoryType(type: TransactionType) {
        categoryType.value = type
    }
}
