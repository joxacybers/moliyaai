package com.example.myapplication.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ai.CachedAdvice
import com.example.myapplication.data.NotificationRepository
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.AppNotificationType
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.domain.CategoryAmount
import com.example.myapplication.domain.PeriodSummary
import com.example.myapplication.domain.PeriodType
import com.example.myapplication.domain.StatsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val balance: Long = 0,
    val accountBalances: Map<Account, Long> = emptyMap(),
    val thisWeek: PeriodSummary = PeriodSummary(LocalDate.now(), LocalDate.now(), 0, 0),
    val thisMonth: PeriodSummary = PeriodSummary(LocalDate.now(), LocalDate.now(), 0, 0),
    val monthlyBudget: Long = 0,
    val topCategoriesThisMonth: List<CategoryAmount> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val latestAdvice: CachedAdvice? = null,
    val unreadNotificationsCount: Int = 0,
    val pendingSmsDraftsCount: Int = 0,
)

class DashboardViewModel(
    private val transactionsRepo: TransactionRepository,
    settingsRepo: SettingsRepository,
    notificationRepo: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionsRepo.observeAll(),
        settingsRepo.monthlyBudget,
        settingsRepo.cachedAdvice(PeriodType.MONTH),
        notificationRepo.observeAll(),
    ) { transactions, budget, advice, notifications ->
        val today = LocalDate.now()
        val week = StatsCalculator.summaryFor(transactions, PeriodType.WEEK, today)
        val month = StatsCalculator.summaryFor(transactions, PeriodType.MONTH, today)
        val topCategories = StatsCalculator.categoryBreakdown(
            transactions = transactions,
            from = month.start,
            to = month.endInclusive,
            type = TransactionType.EXPENSE,
        ).take(5)

        val unreadCount = notifications.count { !it.isRead }
        val pendingDrafts = notifications.count {
            it.type == AppNotificationType.SMS_DRAFT && !it.isActionCompleted
        }

        DashboardUiState(
            balance = StatsCalculator.balance(transactions),
            accountBalances = StatsCalculator.accountBalances(transactions),
            thisWeek = week,
            thisMonth = month,
            monthlyBudget = budget,
            topCategoriesThisMonth = topCategories,
            recentTransactions = transactions.take(5),
            latestAdvice = advice,
            unreadNotificationsCount = unreadCount,
            pendingSmsDraftsCount = pendingDrafts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionsRepo.delete(transaction)
        }
    }
}
