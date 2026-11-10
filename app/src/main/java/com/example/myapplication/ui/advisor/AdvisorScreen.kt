package com.example.myapplication.ui.advisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.ai.AiRecommendation
import com.example.myapplication.domain.Confidence
import com.example.myapplication.domain.Forecast
import com.example.myapplication.domain.PeriodType
import com.example.myapplication.ui.common.CategoryBreakdownList
import com.example.myapplication.ui.common.SectionTitle
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.expenseColor
import com.example.myapplication.ui.common.formatDate
import com.example.myapplication.ui.common.formatPercent
import com.example.myapplication.ui.common.incomeColor
import com.example.myapplication.ui.common.money
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisorScreen(modifier: Modifier = Modifier) {
    val container = appContainer()
    val viewModel: AdvisorViewModel = viewModel {
        AdvisorViewModel(container.transactionRepository, container.settingsRepository, container.aiAdvisor)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val languageName = stringResource(R.string.ai_language_name)

    LaunchedEffect(languageName) {
        viewModel.checkAutoMonthlyAnalysis(languageName)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.nav_advisor), fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = state.modelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Automatic monthly AI analysis notice banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_auto_awesome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ai_monthly_auto_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.ai_monthly_auto_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }

            // Statistical monthly forecast card
            item {
                if (state.forecast != null) {
                    ForecastCard(forecast = state.forecast!!)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.forecast_next_month),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            val msg = stringResource(R.string.not_enough_data_month, state.minPeriodsNeeded)
                            Text(
                                text = "$msg (${state.completedPeriodsCount}/${state.minPeriodsNeeded})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Expected breakdown by category (if forecast is available)
            if (state.forecast != null && state.forecast!!.expenseByCategory.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.forecast_by_category))
                    Spacer(Modifier.height(8.dp))
                    CategoryBreakdownList(state.forecast!!.expenseByCategory)
                }
            }

            // AI recommendations section header + 1/day button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(
                        text = stringResource(R.string.ai_section_title),
                        modifier = Modifier.weight(1f),
                    )
                    if (state.cachedAdvice != null && state.requestState !is AiRequestState.Loading) {
                        OutlinedButton(
                            onClick = { viewModel.requestAdvice(languageName) },
                            enabled = state.canRequestToday,
                        ) {
                            Text(
                                text = if (state.canRequestToday) {
                                    stringResource(R.string.ai_refresh_daily)
                                } else {
                                    stringResource(R.string.ai_daily_limit_used)
                                },
                            )
                        }
                    }
                }
            }

            // AI request state / button / card
            item {
                when (val req = state.requestState) {
                    is AiRequestState.Loading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.ai_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                    is AiRequestState.DailyLimitReached -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        ) {
                            Text(
                                text = stringResource(R.string.ai_daily_limit_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    is AiRequestState.NotConfigured -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        ) {
                            Text(
                                text = stringResource(R.string.ai_not_configured),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    is AiRequestState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.ai_error),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.requestAdvice(languageName) },
                                    enabled = state.canRequestToday,
                                ) {
                                    Text(stringResource(R.string.ai_refresh))
                                }
                            }
                        }
                    }
                    AiRequestState.Idle -> {
                        if (state.cachedAdvice == null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.ai_empty_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.requestAdvice(languageName) },
                                        enabled = state.canRequestToday,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(painterResource(R.drawable.ic_auto_awesome), contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (state.canRequestToday) {
                                                stringResource(R.string.ai_get_advice)
                                            } else {
                                                stringResource(R.string.ai_daily_limit_used)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Render cached advice if present
            state.cachedAdvice?.let { cached ->
                item {
                    val date = Instant.ofEpochMilli(cached.generatedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    Text(
                        text = stringResource(R.string.ai_updated_at, formatDate(date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.ai_summary),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = cached.advice.summary,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }

                if (cached.advice.risks.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.ai_risks),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                cached.advice.risks.forEach { risk ->
                                    Text(
                                        text = "• $risk",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                if (cached.advice.recommendations.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.ai_recommendations),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(cached.advice.recommendations) { rec ->
                        RecommendationCard(rec)
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.ai_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastCard(forecast: Forecast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (forecast.periodType == PeriodType.WEEK) {
                        stringResource(R.string.forecast_next_week)
                    } else {
                        stringResource(R.string.forecast_next_month)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(
                        R.string.confidence_label,
                        when (forecast.confidence) {
                            Confidence.LOW -> stringResource(R.string.confidence_low)
                            Confidence.MEDIUM -> stringResource(R.string.confidence_medium)
                            Confidence.HIGH -> stringResource(R.string.confidence_high)
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        stringResource(R.string.expected_income),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "+${money(forecast.income)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = incomeColor(),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.expected_expense),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "−${money(forecast.expense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = expenseColor(),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.expected_net),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    money(forecast.net),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (forecast.net >= 0) incomeColor() else expenseColor(),
                )
            }

            Text(
                text = stringResource(R.string.trend_income, formatPercent(forecast.incomeTrendPct)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.trend_expense, formatPercent(forecast.expenseTrendPct)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Text(
                text = stringResource(R.string.current_pace_month, money(forecast.currentPeriodProjectedExpense)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun RecommendationCard(rec: AiRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = rec.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = rec.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rec.estimatedMonthlySavings?.let { savings ->
                if (savings > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.ai_savings, money(savings)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = incomeColor(),
                    )
                }
            }
        }
    }
}
