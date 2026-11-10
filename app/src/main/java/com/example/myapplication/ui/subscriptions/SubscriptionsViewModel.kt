package com.example.myapplication.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.SubscriptionRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.BillingCycle
import com.example.myapplication.data.db.SubscriptionEntity
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class SubscriptionItemUi(
    val subscription: SubscriptionEntity,
    val nextBillingDate: LocalDate,
    val daysUntilNext: Int,
    val monthlyEquivalent: Long,
    val isPaidThisMonth: Boolean,
)

data class SubscriptionsUiState(
    val items: List<SubscriptionItemUi> = emptyList(),
    val totalMonthlyCost: Long = 0,
    val totalYearlyCost: Long = 0,
    val activeCount: Int = 0,
)

class SubscriptionsViewModel(
    private val subRepo: SubscriptionRepository,
    private val txRepo: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<SubscriptionsUiState> = subRepo.observeAll().map { subs ->
        val today = LocalDate.now()
        val currentEpochMonth = YearMonth.from(today).let { it.year * 12L + it.monthValue }

        val uiItems = subs.map { sub ->
            val nextDate = computeNextBillingDate(sub.billingDay, today)
            val daysLeft = ChronoUnit.DAYS.between(today, nextDate).toInt().coerceAtLeast(0)
            val monthlyEq = if (sub.billingCycle == BillingCycle.YEARLY) sub.amount / 12 else sub.amount
            SubscriptionItemUi(
                subscription = sub,
                nextBillingDate = nextDate,
                daysUntilNext = daysLeft,
                monthlyEquivalent = monthlyEq,
                isPaidThisMonth = sub.lastPaidEpochMonth >= currentEpochMonth,
            )
        }.sortedWith(
            compareByDescending<SubscriptionItemUi> { it.subscription.isActive }
                .thenBy { it.daysUntilNext },
        )

        val activeItems = uiItems.filter { it.subscription.isActive }
        val monthlyTotal = activeItems.sumOf { it.monthlyEquivalent }

        SubscriptionsUiState(
            items = uiItems,
            totalMonthlyCost = monthlyTotal,
            totalYearlyCost = monthlyTotal * 12,
            activeCount = activeItems.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsUiState())

    fun addSubscription(
        name: String,
        amount: Long,
        displayPrice: String,
        billingCycle: BillingCycle,
        billingDay: Int,
        account: Account,
        colorHex: Long,
    ) {
        if (name.isBlank() || amount <= 0) return
        viewModelScope.launch {
            subRepo.save(
                SubscriptionEntity(
                    name = name.trim(),
                    amount = amount,
                    displayPrice = displayPrice.trim(),
                    billingCycle = billingCycle,
                    billingDay = billingDay.coerceIn(1, 31),
                    account = account,
                    colorHex = colorHex,
                    isActive = true,
                ),
            )
        }
    }

    fun recordPaymentNow(sub: SubscriptionEntity) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val currentEpochMonth = YearMonth.from(today).let { it.year * 12L + it.monthValue }
            // Insert expense transaction
            txRepo.save(
                TransactionEntity(
                    amount = sub.amount,
                    type = TransactionType.EXPENSE,
                    category = Category.ENTERTAINMENT,
                    account = sub.account,
                    epochDay = today.toEpochDay(),
                    note = "${sub.name} (Obuna)",
                ),
            )
            // Update last paid month
            subRepo.save(sub.copy(lastPaidEpochMonth = currentEpochMonth))
        }
    }

    fun deleteSubscription(sub: SubscriptionEntity) {
        viewModelScope.launch {
            subRepo.delete(sub)
        }
    }

    private fun computeNextBillingDate(billingDay: Int, today: LocalDate): LocalDate {
        val thisYearMonth = YearMonth.from(today)
        val clampedDayThisMonth = billingDay.coerceIn(1, thisYearMonth.lengthOfMonth())
        val candidate = thisYearMonth.atDay(clampedDayThisMonth)
        return if (!candidate.isBefore(today)) {
            candidate
        } else {
            val nextMonth = thisYearMonth.plusMonths(1)
            val clampedNext = billingDay.coerceIn(1, nextMonth.lengthOfMonth())
            nextMonth.atDay(clampedNext)
        }
    }
}
