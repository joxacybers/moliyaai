package com.example.myapplication.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DebtRepository
import com.example.myapplication.data.db.DebtEntity
import com.example.myapplication.data.db.DebtType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtsUiState(
    val lentDebts: List<DebtEntity> = emptyList(),
    val borrowedDebts: List<DebtEntity> = emptyList(),
    val totalToReceive: Long = 0L,
    val totalToPay: Long = 0L,
    val selectedTab: Int = 0,
)

class DebtsViewModel(
    private val repo: DebtRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<DebtsUiState> = combine(
        repo.allDebts,
        _selectedTab,
    ) { allDebts, tab ->
        val lent = allDebts.filter { it.type == DebtType.LENT }
        val borrowed = allDebts.filter { it.type == DebtType.BORROWED }

        val toReceive = lent.filter { !it.isSettled }.sumOf { it.remainingAmount }
        val toPay = borrowed.filter { !it.isSettled }.sumOf { it.remainingAmount }

        DebtsUiState(
            lentDebts = lent,
            borrowedDebts = borrowed,
            totalToReceive = toReceive,
            totalToPay = toPay,
            selectedTab = tab,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtsUiState(),
    )

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun addDebt(
        personName: String,
        phoneNumber: String,
        type: DebtType,
        amount: Long,
        dueDateEpochMillis: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            val debt = DebtEntity(
                personName = personName.trim(),
                phoneNumber = phoneNumber.trim(),
                type = type,
                amount = amount,
                dueDateEpochMillis = dueDateEpochMillis,
                note = note.trim(),
            )
            repo.addDebt(debt)
        }
    }

    fun recordPayment(debtId: Long, amount: Long) {
        viewModelScope.launch {
            repo.recordPayment(debtId, amount)
        }
    }

    fun toggleSettled(debtId: Long) {
        viewModelScope.launch {
            repo.toggleSettled(debtId)
        }
    }

    fun deleteDebt(debtId: Long) {
        viewModelScope.launch {
            repo.deleteDebt(debtId)
        }
    }
}
