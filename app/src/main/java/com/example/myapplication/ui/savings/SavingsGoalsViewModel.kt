package com.example.myapplication.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.SavingsRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.SavingsGoalEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SavingsGoalsUiState(
    val goals: List<SavingsGoalEntity> = emptyList(),
    val emergencyFundBalance: Long = 0L,
    val emergencyFundPercent: Int = 10,
    val recommendedEmergencyFund: Long = 0L,
    val totalGoalsTarget: Long = 0L,
    val totalGoalsSaved: Long = 0L,
)

class SavingsGoalsViewModel(
    private val savingsRepo: SavingsRepository,
    private val transactionRepo: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<SavingsGoalsUiState> = combine(
        savingsRepo.allGoals,
        savingsRepo.emergencyFundBalance,
        savingsRepo.emergencyFundPercent,
        transactionRepo.observeAll(),
    ) { goals, emergencyBalance, emergencyPercent, transactions ->
        val now = LocalDate.now()
        // Calculate average monthly expense for the last 3 months
        val threeMonthsAgo = now.minusMonths(3)
        val recentExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.date.isAfter(threeMonthsAgo)
        }.sumOf { it.amount }

        val monthlyAvg = if (recentExpenses > 0) recentExpenses / 3 else 3_000_000L
        // Recommended 3 months of emergency cushion
        val recommendedEmergency = monthlyAvg * 3

        val totalTarget = goals.sumOf { it.targetAmount }
        val totalSaved = goals.sumOf { it.currentAmount }

        SavingsGoalsUiState(
            goals = goals,
            emergencyFundBalance = emergencyBalance,
            emergencyFundPercent = emergencyPercent,
            recommendedEmergencyFund = recommendedEmergency,
            totalGoalsTarget = totalTarget,
            totalGoalsSaved = totalSaved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsGoalsUiState(),
    )

    fun addGoal(
        name: String,
        targetAmount: Long,
        iconKey: String,
        colorHex: Long,
        targetDateEpochMillis: Long?,
    ) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                name = name.trim(),
                targetAmount = targetAmount,
                iconKey = iconKey,
                colorHex = colorHex,
                targetDateEpochMillis = targetDateEpochMillis,
            )
            savingsRepo.addGoal(goal)
        }
    }

    fun addFundsToGoal(goalId: Long, amount: Long) {
        viewModelScope.launch {
            savingsRepo.addFunds(goalId, amount)
        }
    }

    fun withdrawFundsFromGoal(goalId: Long, amount: Long) {
        viewModelScope.launch {
            savingsRepo.withdrawFunds(goalId, amount)
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            savingsRepo.deleteGoal(goalId)
        }
    }

    fun addEmergencyFunds(amount: Long) {
        viewModelScope.launch {
            savingsRepo.addEmergencyFunds(amount)
        }
    }

    fun withdrawEmergencyFunds(amount: Long) {
        viewModelScope.launch {
            savingsRepo.withdrawEmergencyFunds(amount)
        }
    }

    fun setEmergencyPercent(percent: Int) {
        viewModelScope.launch {
            savingsRepo.setEmergencyFundPercent(percent)
        }
    }
}
