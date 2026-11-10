package com.example.myapplication.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.domain.PeriodType
import com.example.myapplication.domain.StatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HistoryFilter { ALL, INCOME, EXPENSE }

data class DayGroup(
    val date: LocalDate,
    val totalIncome: Long,
    val totalExpense: Long,
    val transactions: List<TransactionEntity>,
)

data class HistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val dayGroups: List<DayGroup> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val periodType: PeriodType? = PeriodType.MONTH,
    val periodOffset: Long = 0,
    val activePeriodRange: Pair<LocalDate, LocalDate>? = null,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
)

class HistoryViewModel(
    private val repo: TransactionRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val selectedCategory = MutableStateFlow<Category?>(null)
    private val selectedAccount = MutableStateFlow<Account?>(null)
    private val periodType = MutableStateFlow<PeriodType?>(PeriodType.MONTH)
    private val periodOffset = MutableStateFlow(0L)

    val uiState: StateFlow<HistoryUiState> = combine(
        repo.observeAll(),
        filter,
        selectedCategory,
        selectedAccount,
        combine(periodType, periodOffset, ::Pair),
    ) { all, currentFilter, cat, acc, (period, offset) ->
        val today = LocalDate.now()
        val periodRange: Pair<LocalDate, LocalDate>? = if (period != null) {
            val base = StatsCalculator.periodStart(today, period)
            val targetStart = StatsCalculator.shift(base, period, offset)
            val targetEnd = StatsCalculator.periodEnd(targetStart, period)
            Pair(targetStart, targetEnd)
        } else {
            null
        }

        val filtered = all.filter { item ->
            val matchesType = when (currentFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.INCOME -> item.type == TransactionType.INCOME
                HistoryFilter.EXPENSE -> item.type == TransactionType.EXPENSE
            }
            val matchesCategory = cat == null || item.category == cat
            val matchesAccount = acc == null || item.account == acc
            val matchesPeriod = if (periodRange == null) {
                true
            } else {
                item.epochDay in periodRange.first.toEpochDay()..periodRange.second.toEpochDay()
            }
            matchesType && matchesCategory && matchesAccount && matchesPeriod
        }.sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.createdAt })

        val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val groups = filtered.groupBy { it.date }.map { (day, txs) ->
            DayGroup(
                date = day,
                totalIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                totalExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                transactions = txs,
            )
        }

        HistoryUiState(
            transactions = filtered,
            dayGroups = groups,
            filter = currentFilter,
            selectedCategory = cat,
            selectedAccount = acc,
            periodType = period,
            periodOffset = offset,
            activePeriodRange = periodRange,
            totalIncome = income,
            totalExpense = expense,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(f: HistoryFilter) {
        filter.value = f
        if (selectedCategory.value?.type != null) {
            val catType = selectedCategory.value?.type
            if (f == HistoryFilter.INCOME && catType != TransactionType.INCOME) selectedCategory.value = null
            if (f == HistoryFilter.EXPENSE && catType != TransactionType.EXPENSE) selectedCategory.value = null
        }
    }

    fun setCategory(c: Category?) {
        selectedCategory.value = c
    }

    fun setAccount(a: Account?) {
        selectedAccount.value = a
    }

    fun setPeriodType(type: PeriodType?) {
        periodType.value = type
        periodOffset.value = 0L
    }

    fun previousPeriod() {
        periodOffset.value -= 1L
    }

    fun nextPeriod() {
        periodOffset.value += 1L
    }

    fun resetPeriod() {
        periodOffset.value = 0L
    }

    fun delete(transaction: TransactionEntity) {
        viewModelScope.launch { repo.delete(transaction) }
    }
}
