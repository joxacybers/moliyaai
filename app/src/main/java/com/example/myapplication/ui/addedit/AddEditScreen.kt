package com.example.myapplication.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.formatDate
import com.example.myapplication.ui.common.iconRes
import com.example.myapplication.ui.common.labelRes
import com.example.myapplication.ui.common.tintColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: AddEditViewModel = viewModel(key = "add_edit_$transactionId") {
        AddEditViewModel(
            repo = container.transactionRepository,
            budgetRepo = container.budgetRepository,
            appContext = context.applicationContext,
            transactionId = transactionId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var isNoteFocused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(Unit) {
        viewModel.events.collect { onNavigateBack() }
    }

    LaunchedEffect(isNoteFocused, imeBottom, scrollState.maxValue, state.note) {
        if (isNoteFocused && imeBottom > 0) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (state.isEditing) R.string.edit_transaction else R.string.add_transaction),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Type toggle (Expense / Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(TransactionType.EXPENSE) },
                    label = {
                        Text(
                            stringResource(R.string.expense),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (state.type == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(TransactionType.INCOME) },
                    label = {
                        Text(
                            stringResource(R.string.income),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (state.type == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Amount field
            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                label = { Text(stringResource(R.string.amount)) },
                suffix = { Text(stringResource(R.string.currency_suffix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation(),
                isError = state.amountError,
                supportingText = if (state.amountError) {
                    { Text(stringResource(R.string.amount_error)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Account selector (MANDATORY: Card, Cash, Debt)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${stringResource(R.string.account)} *",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.accountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Account.entries.forEach { acc ->
                        val isSelected = state.account == acc
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAccount(acc) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(acc.iconRes),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else acc.tintColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(acc.labelRes),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = if (state.accountError) {
                                FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                )
                            } else {
                                FilterChipDefaults.filterChipColors()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (state.accountError) {
                    Text(
                        text = stringResource(R.string.account_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // Category selector (flow of chips with vector icons)
            Text(stringResource(R.string.category), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Category.forType(state.type).forEach { cat ->
                    val isCatSelected = state.category == cat
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { viewModel.setCategory(cat) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(cat.iconRes),
                                contentDescription = null,
                                tint = if (isCatSelected) MaterialTheme.colorScheme.primary else cat.tintColor,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = { Text(stringResource(cat.labelRes)) },
                    )
                }
            }

            // Date picker section with quick chips
            Text(stringResource(R.string.date), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val today = LocalDate.now()
                val isToday = state.date == today
                val isYesterday = state.date == today.minusDays(1)
                val isOther = !isToday && !isYesterday

                FilterChip(
                    selected = isToday,
                    onClick = { viewModel.setDate(today) },
                    label = {
                        Text(
                            text = stringResource(R.string.today),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = isYesterday,
                    onClick = { viewModel.setDate(today.minusDays(1)) },
                    label = {
                        Text(
                            text = stringResource(R.string.yesterday),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (isYesterday) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = isOther,
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            text = if (isOther) formatDate(state.date) else stringResource(R.string.custom_date),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (isOther) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = Modifier.weight(1.2f),
                )
            }

            // Optional note
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.note_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isNoteFocused = it.isFocused },
                maxLines = 3,
            )

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        viewModel.setDate(picked)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
