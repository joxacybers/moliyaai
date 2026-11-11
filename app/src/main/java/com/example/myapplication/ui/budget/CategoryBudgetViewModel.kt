package com.example.myapplication.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.BudgetRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.BudgetEntity
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CategoryBudgetItem(
    val category: Category,
    val limitAmount: Long,
    val spentAmount: Long,
) {
    val progress: Float
        get() = if (limitAmount > 0) (spentAmount.toFloat() / limitAmount).coerceIn(0f, 1f) else 0f

    val percent: Int
        get() = if (limitAmount > 0) ((spentAmount * 100) / limitAmount).toInt() else 0

    val remainingAmount: Long
        get() = (limitAmount - spentAmount).coerceAtLeast(0)

    val isExceeded: Boolean
        get() = limitAmount > 0 && spentAmount > limitAmount

    val isWarning: Boolean
        get() = limitAmount > 0 && percent in 80..99
}

data class CategoryBudgetUiState(
    val items: List<CategoryBudgetItem> = emptyList(),
    val totalBudget: Long = 0L,
    val totalSpent: Long = 0L,
    val currentMonth: LocalDate = LocalDate.now(),
)

class CategoryBudgetViewModel(
    private val budgetRepo: BudgetRepository,
    private val transactionRepo: TransactionRepository,
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<CategoryBudgetUiState> = combine(
        budgetRepo.allBudgets,
        transactionRepo.observeAll(),
    ) { budgets, transactions ->
        val budgetsMap = budgets.associateBy { it.category }

        val thisMonthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE &&
                it.date.year == now.year &&
                it.date.monthValue == now.monthValue
        }

        val expenseCategories = Category.forType(TransactionType.EXPENSE)
        val items = expenseCategories.map { category ->
            val limit = budgetsMap[category]?.limitAmount ?: 0L
            val spent = thisMonthExpenses.filter { it.category == category }.sumOf { it.amount }
            CategoryBudgetItem(
                category = category,
                limitAmount = limit,
                spentAmount = spent,
            )
        }.sortedWith(
            compareByDescending<CategoryBudgetItem> { it.limitAmount > 0 }
                .thenByDescending { it.percent }
                .thenByDescending { it.spentAmount }
        )

        val totalBudget = items.filter { it.limitAmount > 0 }.sumOf { it.limitAmount }
        val totalSpent = items.filter { it.limitAmount > 0 }.sumOf { it.spentAmount }

        CategoryBudgetUiState(
            items = items,
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            currentMonth = now,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryBudgetUiState(),
    )

    fun setBudget(category: Category, limitAmount: Long) {
        viewModelScope.launch {
            budgetRepo.setBudget(category, limitAmount)
        }
    }

    fun removeBudget(category: Category) {
        viewModelScope.launch {
            budgetRepo.setBudget(category, 0L)
        }
    }
}
