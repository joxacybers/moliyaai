package com.example.myapplication.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ai.AiAdvisor
import com.example.myapplication.data.DemoDataGenerator
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.data.SubscriptionRepository
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.backup.BackupManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data class BackupSaved(val count: Int) : SettingsEvent
    data class Restored(val count: Int) : SettingsEvent
    data class CsvSaved(val count: Int) : SettingsEvent
    data object OperationFailed : SettingsEvent
    data object DemoLoaded : SettingsEvent
    data object DataCleared : SettingsEvent
    data object BudgetSaved : SettingsEvent
    data object PinSaved : SettingsEvent
    data object PinRemoved : SettingsEvent
}

data class SettingsUiState(
    val monthlyBudget: Long = 0,
    val monthlyBudgetText: String = "",
    val pinCode: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isAiConfigured: Boolean = true,
    val modelName: String = AiAdvisor.DISPLAY_MODEL_NAME,
)

class SettingsViewModel(
    private val transactionsRepo: TransactionRepository,
    private val subscriptionsRepo: SubscriptionRepository,
    private val settingsRepo: SettingsRepository,
    private val backupManager: BackupManager,
    private val aiAdvisor: AiAdvisor,
) : ViewModel() {

    private val budgetInput = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepo.monthlyBudget,
        budgetInput,
        settingsRepo.pinCode,
        settingsRepo.themeMode,
    ) { budget, input, pin, theme ->
        SettingsUiState(
            monthlyBudget = budget,
            monthlyBudgetText = input ?: if (budget > 0) budget.toString() else "",
            pinCode = pin,
            themeMode = theme,
            isAiConfigured = aiAdvisor.isConfigured,
            modelName = AiAdvisor.DISPLAY_MODEL_NAME,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setBudgetText(text: String) {
        budgetInput.value = text.filter { it.isDigit() }
    }

    fun saveBudget() {
        val raw = budgetInput.value ?: uiState.value.monthlyBudgetText
        val parsed = raw.toLongOrNull() ?: 0L
        viewModelScope.launch {
            settingsRepo.setMonthlyBudget(parsed)
            budgetInput.value = null
            _events.emit(SettingsEvent.BudgetSaved)
        }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            settingsRepo.setPinCode(pin)
            if (pin.isEmpty()) {
                _events.emit(SettingsEvent.PinRemoved)
            } else {
                _events.emit(SettingsEvent.PinSaved)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepo.setThemeMode(mode)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.exportJson(uri) }
                .onSuccess { count -> _events.emit(SettingsEvent.BackupSaved(count)) }
                .onFailure { _events.emit(SettingsEvent.OperationFailed) }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.importJson(uri) }
                .onSuccess { count -> _events.emit(SettingsEvent.Restored(count)) }
                .onFailure { _events.emit(SettingsEvent.OperationFailed) }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.exportCsv(uri) }
                .onSuccess { count -> _events.emit(SettingsEvent.CsvSaved(count)) }
                .onFailure { _events.emit(SettingsEvent.OperationFailed) }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            val demo = DemoDataGenerator.generate()
            transactionsRepo.replaceAll(demo)
            settingsRepo.setMonthlyBudget(6_500_000)
            settingsRepo.clearAdvice()
            _events.emit(SettingsEvent.DemoLoaded)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            transactionsRepo.deleteAll()
            subscriptionsRepo.deleteAll()
            settingsRepo.setMonthlyBudget(0)
            settingsRepo.clearAdvice()
            _events.emit(SettingsEvent.DataCleared)
        }
    }
}
