package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Account
import com.example.myapplication.domain.PeriodSummary
import com.example.myapplication.ui.common.CategoryBreakdownList
import com.example.myapplication.ui.common.EmptyState
import com.example.myapplication.ui.common.SectionTitle
import com.example.myapplication.ui.common.TransactionDetailSheet
import com.example.myapplication.ui.common.TransactionRow
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.expenseColor
import com.example.myapplication.ui.common.incomeColor
import com.example.myapplication.ui.common.money
import com.example.myapplication.ui.common.tintColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onOpenAdvisor: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAllHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(container.transactionRepository, container.settingsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                BalanceCard(
                    balance = state.balance,
                    accountBalances = state.accountBalances,
                    thisWeek = state.thisWeek,
                    thisMonth = state.thisMonth,
                    budget = state.monthlyBudget,
                )
            }

            item {
                AiBannerCard(
                    summary = state.latestAdvice?.advice?.summary,
                    onClick = onOpenAdvisor,
                )
            }

            if (state.topCategoriesThisMonth.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.top_expenses_month))
                    Spacer(Modifier.height(8.dp))
                    CategoryBreakdownList(state.topCategoriesThisMonth)
                }
            }

            item {
                SectionTitle(
                    text = stringResource(R.string.recent_transactions),
                    action = {
                        if (state.recentTransactions.isNotEmpty()) {
                            TextButton(onClick = onSeeAllHistory) {
                                Text(stringResource(R.string.see_all))
                            }
                        }
                    },
                )
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyState(stringResource(R.string.empty_transactions))
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { transaction ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        TransactionRow(
                            transaction = transaction,
                            onClick = { selectedTransaction = transaction },
                        )
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
                viewModel.deleteTransaction(it)
                selectedTransaction = null
            },
        )
    }
}

@Composable
private fun BalanceCard(
    balance: Long,
    accountBalances: Map<Account, Long>,
    thisWeek: PeriodSummary,
    thisMonth: PeriodSummary,
    budget: Long,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Total Balance Label & Big Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.total_balance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = money(balance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(Modifier.height(16.dp))

            // Line 1: Accounts breakdown in one symmetrical row (Card, Cash, Debt)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Account.entries.forEach { acc ->
                    val accBal = accountBalances[acc] ?: 0L
                    AccountMiniCard(
                        account = acc,
                        amount = accBal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                thickness = 1.dp,
            )
            Spacer(Modifier.height(14.dp))

            // Line 2: Week & Month side-by-side in one aligned row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PeriodFlowCard(
                    title = stringResource(R.string.this_week),
                    income = thisWeek.income,
                    expense = thisWeek.expense,
                    modifier = Modifier.weight(1f),
                )
                PeriodFlowCard(
                    title = stringResource(R.string.this_month),
                    income = thisMonth.income,
                    expense = thisMonth.expense,
                    modifier = Modifier.weight(1f),
                )
            }

            // Optional Monthly Budget Progress
            if (budget > 0) {
                Spacer(Modifier.height(16.dp))
                val progress = (thisMonth.expense.toFloat() / budget).coerceIn(0f, 1f)
                val exceeded = thisMonth.expense > budget
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.monthly_budget_progress, money(thisMonth.expense), money(budget)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    drawStopIndicator = {},
                )
                if (exceeded) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.budget_exceeded),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountMiniCard(
    account: Account,
    amount: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(account.iconRes),
                    contentDescription = null,
                    tint = account.tintColor,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(account.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = money(amount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (amount < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PeriodFlowCard(
    title: String,
    income: Long,
    expense: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.income),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "+${money(income)}",
                style = MaterialTheme.typography.labelSmall,
                color = incomeColor(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.expense),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "−${money(expense)}",
                style = MaterialTheme.typography.labelSmall,
                color = expenseColor(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AiBannerCard(summary: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auto_awesome),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary ?: stringResource(R.string.ai_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 2,
                )
            }
        }
    }
}
