package com.example.myapplication.ui.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ai.AiAdvice
import com.example.myapplication.ai.AiAdvisor
import com.example.myapplication.ai.AiNotConfiguredException
import com.example.myapplication.ai.CachedAdvice
import com.example.myapplication.ai.PromptBuilder
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.domain.Forecast
import com.example.myapplication.domain.ForecastEngine
import com.example.myapplication.domain.PeriodType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

sealed interface AiRequestState {
    data object Idle : AiRequestState
    data object Loading : AiRequestState
    data class Error(val message: String) : AiRequestState
    data object NotConfigured : AiRequestState
    data object DailyLimitReached : AiRequestState
}

data class AdvisorUiState(
    val periodType: PeriodType = PeriodType.MONTH,
    val forecast: Forecast? = null,
    val minPeriodsNeeded: Int = ForecastEngine.MIN_PERIODS,
    val completedPeriodsCount: Int = 0,
    val cachedAdvice: CachedAdvice? = null,
    val requestState: AiRequestState = AiRequestState.Idle,
    val isAiConfigured: Boolean = true,
    val canRequestToday: Boolean = true,
    val modelName: String = AiAdvisor.DISPLAY_MODEL_NAME,
)

class AdvisorViewModel(
    private val transactionsRepo: TransactionRepository,
    private val settingsRepo: SettingsRepository,
    private val aiAdvisor: AiAdvisor,
) : ViewModel() {

    private val requestState = MutableStateFlow<AiRequestState>(AiRequestState.Idle)
    private var autoAnalyzeTriggered = false

    val uiState: StateFlow<AdvisorUiState> = combine(
        transactionsRepo.observeAll(),
        settingsRepo.cachedAdvice(PeriodType.MONTH),
        settingsRepo.lastAiRequestEpochDay,
        requestState,
    ) { transactions, cached, lastAiDay, reqState ->
        val today = LocalDate.now()
        val forecast = ForecastEngine.forecast(transactions, PeriodType.MONTH, today)
        val history = ForecastEngine.completedHistory(transactions, PeriodType.MONTH, today)
        val canRequest = lastAiDay < today.toEpochDay()

        AdvisorUiState(
            periodType = PeriodType.MONTH,
            forecast = forecast,
            minPeriodsNeeded = ForecastEngine.MIN_PERIODS,
            completedPeriodsCount = history.size,
            cachedAdvice = cached,
            requestState = reqState,
            isAiConfigured = aiAdvisor.isConfigured,
            canRequestToday = canRequest,
            modelName = AiAdvisor.DISPLAY_MODEL_NAME,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvisorUiState())

    /**
     * Automatically runs a monthly analysis when a previous month has ended and hasn't been
     * analyzed yet. Does not consume the user's manual 1-per-day test quota.
     */
    fun checkAutoMonthlyAnalysis(languageName: String) {
        if (autoAnalyzeTriggered) return
        autoAnalyzeTriggered = true
        viewModelScope.launch {
            val today = LocalDate.now()
            val lastCompletedMonth = YearMonth.from(today).minusMonths(1).toString()
            val alreadyAnalyzed = settingsRepo.lastAutoAnalyzedMonth.first()
            if (alreadyAnalyzed == lastCompletedMonth) return@launch

            val transactions = transactionsRepo.getAll()
            if (transactions.isEmpty()) return@launch

            try {
                requestState.value = AiRequestState.Loading
                val budget = settingsRepo.monthlyBudget.first()
                val customKey = settingsRepo.customOpenAiKey.first()
                val input = PromptBuilder.buildInput(
                    transactions = transactions,
                    type = PeriodType.MONTH,
                    today = today,
                    monthlyBudget = budget,
                )
                val advice = aiAdvisor.advise(input, languageName, customKey)
                val cached = CachedAdvice(generatedAt = System.currentTimeMillis(), advice = advice)
                settingsRepo.saveAdvice(PeriodType.MONTH, cached)
                settingsRepo.setLastAutoAnalyzedMonth(lastCompletedMonth)
                requestState.value = AiRequestState.Idle
            } catch (_: Exception) {
                requestState.value = AiRequestState.Idle
            }
        }
    }

    fun requestAdvice(languageName: String) {
        viewModelScope.launch {
            val todayEpoch = LocalDate.now().toEpochDay()
            val lastReqDay = settingsRepo.lastAiRequestEpochDay.first()
            if (lastReqDay >= todayEpoch) {
                requestState.value = AiRequestState.DailyLimitReached
                return@launch
            }

            requestState.value = AiRequestState.Loading
            try {
                val transactions = transactionsRepo.getAll()
                val budget = settingsRepo.monthlyBudget.first()
                val customKey = settingsRepo.customOpenAiKey.first()
                val input = PromptBuilder.buildInput(
                    transactions = transactions,
                    type = PeriodType.MONTH,
                    today = LocalDate.now(),
                    monthlyBudget = budget,
                )
                val advice = aiAdvisor.advise(input, languageName, customKey)
                val cached = CachedAdvice(generatedAt = System.currentTimeMillis(), advice = advice)
                settingsRepo.saveAdvice(PeriodType.MONTH, cached)
                settingsRepo.setLastAiRequestEpochDay(todayEpoch)
                requestState.value = AiRequestState.Idle
            } catch (e: AiNotConfiguredException) {
                requestState.value = AiRequestState.NotConfigured
            } catch (e: Exception) {
                requestState.value = AiRequestState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
