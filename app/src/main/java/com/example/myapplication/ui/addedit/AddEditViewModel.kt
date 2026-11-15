package com.example.myapplication.ui.addedit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.BudgetRepository
import com.example.myapplication.data.SavingsRepository
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.CustomCategory
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.notification.AppNotificationHelper
import com.example.myapplication.ui.common.labelRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditUiState(
    val id: Long = 0,
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.FOOD,
    val selectedCustomCategory: CustomCategory? = null,
    val customCategories: List<CustomCategory> = emptyList(),
    val account: Account? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val amountError: Boolean = false,
    val accountError: Boolean = false,
    val isEditing: Boolean = false,
)

class AddEditViewModel(
    private val repo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val savingsRepo: SavingsRepository,
    private val settingsRepo: SettingsRepository,
    private val appContext: Context,
    private val transactionId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState(id = transactionId, isEditing = transactionId > 0))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Unit>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepo.customCategories.collect { customList ->
                _uiState.value = _uiState.value.copy(customCategories = customList)
            }
        }
        if (transactionId > 0) {
            viewModelScope.launch {
                repo.get(transactionId)?.let { t ->
                    val customCat = if (t.customCategoryName.isNotBlank()) {
                        CustomCategory(
                            name = t.customCategoryName,
                            type = t.type,
                            iconKey = t.customCategoryIcon.ifBlank { "other" },
                            colorHex = if (t.customCategoryColor != 0L) t.customCategoryColor else 0xFF1976D2,
                        )
                    } else null

                    _uiState.value = _uiState.value.copy(
                        id = t.id,
                        amountText = t.amount.toString(),
                        type = t.type,
                        category = t.category,
                        selectedCustomCategory = customCat,
                        account = t.account,
                        date = t.date,
                        note = t.note,
                        isEditing = true,
                    )
                }
            }
        }
    }

    fun setAmount(text: String) {
        val digitsOnly = text.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(amountText = digitsOnly, amountError = false)
    }

    fun setType(type: TransactionType) {
        val currentCat = _uiState.value.category
        val newCategory = if (currentCat.type == type) currentCat else Category.forType(type).first()
        val currentCustom = _uiState.value.selectedCustomCategory
        val newCustom = if (currentCustom?.type == type) currentCustom else null
        _uiState.value = _uiState.value.copy(
            type = type,
            category = newCategory,
            selectedCustomCategory = newCustom,
        )
    }

    fun setCategory(category: Category) {
        _uiState.value = _uiState.value.copy(
            category = category,
            selectedCustomCategory = null,
        )
    }

    fun selectCustomCategory(customCategory: CustomCategory) {
        val fallbackEnum = if (customCategory.type == TransactionType.INCOME) {
            Category.OTHER_INCOME
        } else {
            Category.OTHER_EXPENSE
        }
        _uiState.value = _uiState.value.copy(
            category = fallbackEnum,
            selectedCustomCategory = customCategory,
        )
    }

    fun addAndSelectCustomCategory(customCategory: CustomCategory) {
        viewModelScope.launch {
            settingsRepo.addCustomCategory(customCategory)
            selectCustomCategory(customCategory)
        }
    }

    fun setAccount(account: Account) {
        _uiState.value = _uiState.value.copy(account = account, accountError = false)
    }

    fun setDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun setNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun save() {
        val amount = _uiState.value.amountText.toLongOrNull() ?: 0L
        val account = _uiState.value.account

        var hasError = false
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(amountError = true)
            hasError = true
        }
        if (account == null) {
            _uiState.value = _uiState.value.copy(accountError = true)
            hasError = true
        }
        if (hasError) return

        val customCat = _uiState.value.selectedCustomCategory
        val entity = TransactionEntity(
            id = _uiState.value.id,
            amount = amount,
            type = _uiState.value.type,
            category = _uiState.value.category,
            account = account!!,
            epochDay = _uiState.value.date.toEpochDay(),
            note = _uiState.value.note.trim(),
            customCategoryName = customCat?.name.orEmpty(),
            customCategoryIcon = customCat?.iconKey.orEmpty(),
            customCategoryColor = customCat?.colorHex ?: 0L,
        )
        viewModelScope.launch {
            repo.save(entity)

            // Check budget limit alert if it's an expense
            if (entity.type == TransactionType.EXPENSE) {
                checkBudgetAlert(entity)
            } else if (entity.type == TransactionType.INCOME) {
                checkEmergencyFundReminder(entity)
            }

            _events.emit(Unit)
        }
    }

    private suspend fun checkEmergencyFundReminder(entity: TransactionEntity) {
        try {
            val percent = savingsRepo.emergencyFundPercent.first()
            val suggested = (entity.amount * percent) / 100
            if (suggested > 0) {
                AppNotificationHelper.showEmergencyFundReminder(
                    context = appContext,
                    incomeAmount = entity.amount,
                    suggestedDeposit = suggested,
                    percent = percent,
                )
            }
        } catch (_: Exception) {}
    }

    private suspend fun checkBudgetAlert(entity: TransactionEntity) {
        try {
            val budget = budgetRepo.getForCategory(entity.category) ?: return
            if (budget.limitAmount <= 0) return

            val allTx = repo.getAll()
            val txDate = entity.date
            val monthSpent = allTx.filter {
                it.type == TransactionType.EXPENSE &&
                    it.category == entity.category &&
                    it.date.year == txDate.year &&
                    it.date.monthValue == txDate.monthValue
            }.sumOf { it.amount }

            val percent = (monthSpent * 100 / budget.limitAmount).toInt()
            if (percent >= 80) {
                val catName = entity.customCategoryName.ifBlank {
                    appContext.getString(entity.category.labelRes)
                }
                AppNotificationHelper.showBudgetAlert(
                    context = appContext,
                    categoryName = catName,
                    percent = percent,
                    spent = monthSpent,
                    limit = budget.limitAmount,
                )
            }
        } catch (_: Exception) {}
    }

    fun delete() {
        if (transactionId <= 0) return
        viewModelScope.launch {
            repo.get(transactionId)?.let { repo.delete(it) }
            _events.emit(Unit)
        }
    }
}
