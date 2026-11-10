package com.example.myapplication.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.domain.PeriodType
import com.example.myapplication.ui.common.CategoryBreakdownList
import com.example.myapplication.ui.common.CategoryDonutChart
import com.example.myapplication.ui.common.ChartLegend
import com.example.myapplication.ui.common.EmptyState
import com.example.myapplication.ui.common.IncomeExpenseBarChart
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.expenseColor
import com.example.myapplication.ui.common.formatPercent
import com.example.myapplication.ui.common.incomeColor
import com.example.myapplication.ui.common.money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val container = appContainer()
    val viewModel: StatsViewModel = viewModel {
        StatsViewModel(container.transactionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_stats), fontWeight = FontWeight.Bold) },
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.periodType == PeriodType.MONTH,
                        onClick = { viewModel.setPeriodType(PeriodType.MONTH) },
                        label = {
                            Text(
                                text = stringResource(R.string.period_monthly),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = if (state.periodType == PeriodType.MONTH) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = state.periodType == PeriodType.WEEK,
                        onClick = { viewModel.setPeriodType(PeriodType.WEEK) },
                        label = {
                            Text(
                                text = stringResource(R.string.period_weekly),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = if (state.periodType == PeriodType.WEEK) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Category Percentage Donut Chart + Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.category_breakdown),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        // Toggle between Expenses and Income categories
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = state.categoryType == TransactionType.EXPENSE,
                                onClick = { viewModel.setCategoryType(TransactionType.EXPENSE) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.expense),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (state.categoryType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = state.categoryType == TransactionType.INCOME,
                                onClick = { viewModel.setCategoryType(TransactionType.INCOME) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.income),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (state.categoryType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (state.categoryBreakdown.isEmpty()) {
                            EmptyState(stringResource(R.string.no_expenses_period))
                        } else {
                            CategoryDonutChart(
                                items = state.categoryBreakdown,
                                totalAmount = state.categoryTotal,
                                totalLabel = if (state.categoryType == TransactionType.EXPENSE) {
                                    stringResource(R.string.expense)
                                } else {
                                    stringResource(R.string.income)
                                },
                            )

                            HorizontalDivider()

                            // Total summary row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${stringResource(R.string.all)} (${state.categoryBreakdown.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${money(state.categoryTotal)} · 100%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (state.categoryType == TransactionType.EXPENSE) expenseColor() else incomeColor(),
                                )
                            }

                            // Category rows with name, percentage share, and total amount
                            CategoryBreakdownList(state.categoryBreakdown)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.chart_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            ChartLegend()
                        }
                        Spacer(Modifier.height(16.dp))
                        IncomeExpenseBarChart(
                            summaries = state.summaries,
                            periodType = state.periodType,
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.current_period), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "+${money(state.currentPeriod.income)}  −${money(state.currentPeriod.expense)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.previous_period), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "+${money(state.previousPeriod.income)}  −${money(state.previousPeriod.expense)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        state.expenseChangePct?.let { pct ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    stringResource(R.string.expense_change, formatPercent(pct)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (pct > 0) expenseColor() else incomeColor(),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.average_expense), style = MaterialTheme.typography.bodySmall)
                            Text(money(state.averageExpense), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
