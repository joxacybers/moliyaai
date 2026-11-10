package com.example.myapplication.ai

import kotlinx.serialization.Serializable

/** Structured response returned by the model (enforced via response schema). */
@Serializable
data class AiAdvice(
    val summary: String,
    val risks: List<String> = emptyList(),
    val recommendations: List<AiRecommendation> = emptyList(),
)

@Serializable
data class AiRecommendation(
    val title: String,
    val detail: String,
    val estimatedMonthlySavings: Long? = null,
)

/** Last successful advice, cached locally so the screen works offline and saves quota. */
@Serializable
data class CachedAdvice(
    val generatedAt: Long,
    val advice: AiAdvice,
)

class AiNotConfiguredException : Exception("Firebase is not configured (google-services.json missing)")

// ---- Compact, anonymised input sent to the model (aggregates only, no notes/raw rows) ----

@Serializable
data class AiInput(
    val currency: String = "UZS",
    val periodType: String,
    val today: String,
    val balance: Long,
    val monthlyBudget: Long?,
    val history: List<AiPeriod>,
    val currentPeriod: AiCurrentPeriod,
    val topExpenseCategories: List<AiCategory>,
    val forecast: AiForecast?,
)

@Serializable
data class AiPeriod(val start: String, val income: Long, val expense: Long)

@Serializable
data class AiCurrentPeriod(
    val start: String,
    val daysElapsed: Int,
    val daysTotal: Int,
    val incomeSoFar: Long,
    val expenseSoFar: Long,
    val projectedExpense: Long,
)

@Serializable
data class AiCategory(
    val category: String,
    val amount: Long,
    val sharePct: Int,
    /** Last completed period vs. the average of earlier periods, in percent. */
    val changePct: Int? = null,
)

@Serializable
data class AiForecast(
    val nextPeriodStart: String,
    val income: Long,
    val expense: Long,
    val net: Long,
    val incomeTrendPctPerPeriod: Double,
    val expenseTrendPctPerPeriod: Double,
    val confidence: String,
    val basedOnPeriods: Int,
    val expenseByCategory: List<AiCategory>,
)
