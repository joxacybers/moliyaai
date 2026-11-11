package com.example.myapplication.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.model.Category
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.formatFullMonthYear
import com.example.myapplication.ui.common.iconRes
import com.example.myapplication.ui.common.labelRes
import com.example.myapplication.ui.common.money
import com.example.myapplication.ui.common.tintColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBudgetScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: CategoryBudgetViewModel = viewModel {
        CategoryBudgetViewModel(container.budgetRepository, container.transactionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var currentLimitInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_budgets_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header: Total Budget Card
            item {
                TotalBudgetCard(
                    totalBudget = state.totalBudget,
                    totalSpent = state.totalSpent,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.budget_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(state.items, key = { it.category.name }) { item ->
                CategoryBudgetItemCard(
                    item = item,
                    onClick = {
                        editingCategory = item.category
                        currentLimitInput = if (item.limitAmount > 0) item.limitAmount.toString() else ""
                    },
                )
            }
        }
    }

    // Set / Edit Budget Dialog
    editingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cat.tintColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(cat.iconRes),
                            contentDescription = null,
                            tint = cat.tintColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(cat.labelRes))
                }
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.budget_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentLimitInput,
                        onValueChange = { currentLimitInput = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(R.string.budget_limit)) },
                        placeholder = { Text("2 000 000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("soʻm") },
                    )
                    Spacer(Modifier.height(12.dp))
                    // Quick presets
                    Text(
                        text = "Tezkor tanlov:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(500_000L, 1_000_000L, 2_000_000L, 5_000_000L).forEach { amount ->
                            FilterChip(
                                selected = currentLimitInput == amount.toString(),
                                onClick = { currentLimitInput = amount.toString() },
                                label = { Text(if (amount >= 1_000_000) "${amount / 1_000_000} mln" else "${amount / 1000} ming") },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = currentLimitInput.toLongOrNull() ?: 0L
                        viewModel.setBudget(cat, amount)
                        editingCategory = null
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                Row {
                    val existing = state.items.find { it.category == cat }
                    if (existing != null && existing.limitAmount > 0) {
                        TextButton(
                            onClick = {
                                viewModel.removeBudget(cat)
                                editingCategory = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.budget_remove_limit))
                        }
                    }
                    TextButton(onClick = { editingCategory = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
        )
    }
}

@Composable
private fun TotalBudgetCard(
    totalBudget: Long,
    totalSpent: Long,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Oylik jami byudjet rejalari",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = money(totalBudget),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (totalBudget > 0) {
                    val percent = ((totalSpent * 100) / totalBudget).toInt()
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalSpent > totalBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (totalBudget > 0) {
                Spacer(Modifier.height(12.dp))
                val progress = (totalSpent.toFloat() / totalBudget).coerceIn(0f, 1f)
                val isExceeded = totalSpent > totalBudget
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = when {
                        isExceeded -> MaterialTheme.colorScheme.error
                        progress >= 0.8f -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Sarflangan: ${money(totalSpent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = if (isExceeded) {
                            "Limitdan oshdi: ${money(totalSpent - totalBudget)}"
                        } else {
                            "Qolgan: ${money(totalBudget - totalSpent)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.budget_no_limits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetItemCard(
    item: CategoryBudgetItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(item.category.tintColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(item.category.iconRes),
                        contentDescription = null,
                        tint = item.category.tintColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(item.category.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (item.limitAmount > 0) {
                            "${stringResource(R.string.budget_spent)}: ${money(item.spentAmount)} / ${money(item.limitAmount)}"
                        } else {
                            "${stringResource(R.string.budget_spent)}: ${money(item.spentAmount)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (item.limitAmount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${item.percent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                item.isExceeded -> MaterialTheme.colorScheme.error
                                item.isWarning -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(stringResource(R.string.budget_set_limit), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (item.limitAmount > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        item.isExceeded -> MaterialTheme.colorScheme.error
                        item.isWarning -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    drawStopIndicator = {},
                )

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    when {
                        item.isExceeded -> {
                            Text(
                                text = stringResource(R.string.budget_exceeded_by, money(item.spentAmount - item.limitAmount)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        item.isWarning -> {
                            Text(
                                text = "⚠️ ${stringResource(R.string.budget_warning_80)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFA000),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        else -> {
                            Text(
                                text = "${stringResource(R.string.budget_remaining)}: ${money(item.remainingAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
