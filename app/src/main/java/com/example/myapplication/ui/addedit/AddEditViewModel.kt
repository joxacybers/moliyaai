package com.example.myapplication.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditUiState(
    val id: Long = 0,
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.FOOD,
    val account: Account? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val amountError: Boolean = false,
    val accountError: Boolean = false,
    val isEditing: Boolean = false,
)

class AddEditViewModel(
    private val repo: TransactionRepository,
    private val transactionId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState(id = transactionId, isEditing = transactionId > 0))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Unit>()
    val events = _events.asSharedFlow()

    init {
        if (transactionId > 0) {
            viewModelScope.launch {
                repo.get(transactionId)?.let { t ->
                    _uiState.value = AddEditUiState(
                        id = t.id,
                        amountText = t.amount.toString(),
                        type = t.type,
                        category = t.category,
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
        _uiState.value = _uiState.value.copy(type = type, category = newCategory)
    }

    fun setCategory(category: Category) {
        _uiState.value = _uiState.value.copy(category = category)
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

        val entity = TransactionEntity(
            id = _uiState.value.id,
            amount = amount,
            type = _uiState.value.type,
            category = _uiState.value.category,
            account = account!!,
            epochDay = _uiState.value.date.toEpochDay(),
            note = _uiState.value.note.trim(),
        )
        viewModelScope.launch {
            repo.save(entity)
            _events.emit(Unit)
        }
    }

    fun delete() {
        if (transactionId <= 0) return
        viewModelScope.launch {
            repo.get(transactionId)?.let { repo.delete(it) }
            _events.emit(Unit)
        }
    }
}
