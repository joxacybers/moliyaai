package com.example.myapplication.ui.history

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.domain.PeriodType
import com.example.myapplication.ui.common.EmptyState
import com.example.myapplication.ui.common.TransactionDetailSheet
import com.example.myapplication.ui.common.TransactionRow
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.expenseColor
import com.example.myapplication.ui.common.formatDayMonth
import com.example.myapplication.ui.common.formatFullMonthYear
import com.example.myapplication.ui.common.formatRelativeDay
import com.example.myapplication.ui.common.incomeColor
import com.example.myapplication.ui.common.iconRes
import com.example.myapplication.ui.common.labelRes
import com.example.myapplication.ui.common.money
import com.example.myapplication.ui.common.tintColor
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: HistoryViewModel = viewModel {
        HistoryViewModel(container.transactionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExtraFilters by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showExtraFilters = !showExtraFilters }) {
                        Icon(
                            imageVector = if (showExtraFilters) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(R.string.category),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Period Type Selector: [ Oy | Hafta | Barchasi ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.periodType == PeriodType.MONTH,
                    onClick = { viewModel.setPeriodType(PeriodType.MONTH) },
                    label = {
                        Text(
                            text = stringResource(R.string.period_month_short),
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
                            text = stringResource(R.string.period_week_short),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (state.periodType == PeriodType.WEEK) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.periodType == null,
                    onClick = { viewModel.setPeriodType(null) },
                    label = {
                        Text(
                            text = stringResource(R.string.period_all),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (state.periodType == null) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Period Navigator (if Month or Week is active)
            PeriodNavigator(
                periodType = state.periodType,
                periodRange = state.activePeriodRange,
                isCurrentPeriod = state.periodOffset == 0L,
                onPrevious = viewModel::previousPeriod,
                onNext = viewModel::nextPeriod,
                onReset = viewModel::resetPeriod,
            )

            // Main 3 Type Filter Buttons: [ Hammasi | Kirim | Chiqim ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter == HistoryFilter.ALL,
                    onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                    label = {
                        Text(
                            text = stringResource(R.string.all),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (state.filter == HistoryFilter.ALL) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.filter == HistoryFilter.INCOME,
                    onClick = { viewModel.setFilter(HistoryFilter.INCOME) },
                    label = {
                        Text(
                            text = stringResource(R.string.income),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (state.filter == HistoryFilter.INCOME) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.filter == HistoryFilter.EXPENSE,
                    onClick = { viewModel.setFilter(HistoryFilter.EXPENSE) },
                    label = {
                        Text(
                            text = stringResource(R.string.expense),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (state.filter == HistoryFilter.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Optional expanded filters: Accounts & Categories
            if (showExtraFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Accounts row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = state.selectedAccount == null,
                            onClick = { viewModel.setAccount(null) },
                            label = { Text(stringResource(R.string.all)) },
                        )
                        Account.entries.forEach { acc ->
                            val isAccSelected = state.selectedAccount == acc
                            FilterChip(
                                selected = isAccSelected,
                                onClick = { viewModel.setAccount(if (isAccSelected) null else acc) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(acc.iconRes),
                                        contentDescription = null,
                                        tint = if (isAccSelected) MaterialTheme.colorScheme.primary else acc.tintColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = { Text(stringResource(acc.labelRes)) },
                            )
                        }
                    }

                    // Categories row
                    val availableCategories = when (state.filter) {
                        HistoryFilter.ALL -> Category.entries
                        HistoryFilter.INCOME -> Category.forType(TransactionType.INCOME)
                        HistoryFilter.EXPENSE -> Category.forType(TransactionType.EXPENSE)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text(stringResource(R.string.all)) },
                        )
                        availableCategories.forEach { cat ->
                            val isCatSelected = state.selectedCategory == cat
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { viewModel.setCategory(if (isCatSelected) null else cat) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(cat.iconRes),
                                        contentDescription = null,
                                        tint = if (isCatSelected) MaterialTheme.colorScheme.primary else cat.tintColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = { Text(stringResource(cat.labelRes)) },
                            )
                        }
                    }
                }
            }

            // Aesthetic Status KPI Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Transactions count column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = state.transactions.size.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.recent_transactions),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                maxLines = 1,
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(36.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )

                        // Income column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.3f),
                        ) {
                            Text(
                                text = "+${money(state.totalIncome)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = incomeColor(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.income),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(36.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )

                        // Expense column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.3f),
                        ) {
                            Text(
                                text = "−${money(state.totalExpense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = expenseColor(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                    }

                    // Net line if both exist
                    if (state.totalIncome > 0 && state.totalExpense > 0) {
                        val net = state.totalIncome - state.totalExpense
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.net),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = (if (net >= 0) "+" else "−") + money(kotlin.math.abs(net)),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) incomeColor() else expenseColor(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Transactions list grouped by date
            if (state.dayGroups.isEmpty()) {
                EmptyState(stringResource(R.string.no_results))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.dayGroups, key = { it.date.toEpochDay() }) { group ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Date header with daily net total
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = formatRelativeDay(group.date),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val dailyNet = group.totalIncome - group.totalExpense
                                val netColor = if (dailyNet >= 0) incomeColor() else expenseColor()
                                Text(
                                    text = (if (dailyNet >= 0) "+" else "−") + money(kotlin.math.abs(dailyNet)),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = netColor,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }

                            // Day container card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                group.transactions.forEachIndexed { index, transaction ->
                                    TransactionRow(
                                        transaction = transaction,
                                        onClick = { selectedTransaction = transaction },
                                        showDate = false,
                                    )
                                    if (index < group.transactions.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            onDismiss = { selectedTransaction = null },
            onDelete = {
                viewModel.delete(it)
                selectedTransaction = null
            },
        )
    }
}

@Composable
private fun PeriodNavigator(
    periodType: PeriodType?,
    periodRange: Pair<LocalDate, LocalDate>?,
    isCurrentPeriod: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (periodType == null || periodRange == null) return

    val label = when (periodType) {
        PeriodType.MONTH -> formatFullMonthYear(periodRange.first)
        PeriodType.WEEK -> "${formatDayMonth(periodRange.first)} – ${formatDayMonth(periodRange.second)} ${periodRange.first.year}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!isCurrentPeriod) {
                Surface(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.today),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
