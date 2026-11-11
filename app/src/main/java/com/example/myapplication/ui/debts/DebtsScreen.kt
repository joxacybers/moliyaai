package com.example.myapplication.ui.debts

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.db.DebtEntity
import com.example.myapplication.data.db.DebtType
import com.example.myapplication.ui.common.EmptyState
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.formatAmount
import com.example.myapplication.ui.common.formatDayMonth
import com.example.myapplication.ui.common.incomeColor
import com.example.myapplication.ui.common.money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: DebtsViewModel = viewModel {
        DebtsViewModel(container.debtRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var recordingPaymentDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.debt_add_title))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DebtSummaryCard(
                    title = stringResource(R.string.debt_total_to_receive),
                    amount = state.totalToReceive,
                    containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    contentColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                )
                DebtSummaryCard(
                    title = stringResource(R.string.debt_total_to_pay),
                    amount = state.totalToPay,
                    containerColor = Color(0xFFD84315).copy(alpha = 0.12f),
                    contentColor = Color(0xFFD84315),
                    modifier = Modifier.weight(1f),
                )
            }

            // Tabs
            SecondaryTabRow(
                selectedTabIndex = state.selectedTab,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = "${stringResource(R.string.debt_tab_lent)} (${state.lentDebts.count { !it.isSettled }})",
                            fontWeight = if (state.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            text = "${stringResource(R.string.debt_tab_borrowed)} (${state.borrowedDebts.count { !it.isSettled }})",
                            fontWeight = if (state.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }

            val currentList = if (state.selectedTab == 0) state.lentDebts else state.borrowedDebts

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        text = if (state.selectedTab == 0) {
                            stringResource(R.string.debt_empty_lent)
                        } else {
                            stringResource(R.string.debt_empty_borrowed)
                        },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(currentList, key = { it.id }) { debt ->
                        DebtCard(
                            debt = debt,
                            onRecordPayment = {
                                recordingPaymentDebt = debt
                                paymentAmountInput = ""
                            },
                            onToggleSettled = { viewModel.toggleSettled(debt.id) },
                            onDelete = { viewModel.deleteDebt(debt.id) },
                        )
                    }
                }
            }
        }
    }

    // Record Payment Dialog
    recordingPaymentDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { recordingPaymentDebt = null },
            title = { Text(stringResource(R.string.debt_record_payment)) },
            text = {
                Column {
                    Text(
                        text = "${debt.personName}: qolgan qarz ${money(debt.remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.amount)) },
                        placeholder = { Text(debt.remainingAmount.toString()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("soʻm") },
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { paymentAmountInput = debt.remainingAmount.toString() }) {
                        Text("Toʻliq qoplash (${money(debt.remainingAmount)})")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            viewModel.recordPayment(debt.id, amount)
                        }
                        recordingPaymentDebt = null
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingPaymentDebt = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Add Debt Dialog
    if (showAddDialog) {
        AddDebtDialog(
            defaultType = if (state.selectedTab == 0) DebtType.LENT else DebtType.BORROWED,
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, type, amount, dueDate, note ->
                viewModel.addDebt(name, phone, type, amount, dueDate, note)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun DebtSummaryCard(
    title: String,
    amount: Long,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = money(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun DebtCard(
    debt: DebtEntity,
    onRecordPayment: () -> Unit,
    onToggleSettled: () -> Unit,
    onDelete: () -> Unit,
) {
    val isLent = debt.type == DebtType.LENT
    val todayEpoch = LocalDate.now().toEpochDay()
    val isOverdue = debt.dueDateEpochMillis != null &&
        !debt.isSettled &&
        Instant.ofEpochMilli(debt.dueDateEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() < todayEpoch

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (debt.isSettled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
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
                        .background(
                            if (debt.isSettled) {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            } else if (isLent) {
                                Color(0xFF2E7D32).copy(alpha = 0.15f)
                            } else {
                                Color(0xFFD84315).copy(alpha = 0.15f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = if (debt.isSettled) MaterialTheme.colorScheme.outline else if (isLent) Color(0xFF2E7D32) else Color(0xFFD84315),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = debt.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (debt.isSettled) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.debt_settled_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (debt.phoneNumber.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = debt.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Amount Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = if (isLent) "Olinadigan summa" else "Qaytariladigan summa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = money(debt.remainingAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (debt.isSettled) MaterialTheme.colorScheme.outline else if (isLent) Color(0xFF2E7D32) else Color(0xFFD84315),
                    )
                }
                if (debt.paidAmount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Jami qarz",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = money(debt.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (debt.amount > 0 && debt.paidAmount > 0 && !debt.isSettled) {
                Spacer(Modifier.height(8.dp))
                val progress = (debt.paidAmount.toFloat() / debt.amount).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF2E7D32),
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Qaytarildi: ${money(debt.paidAmount)} (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Due Date & Note
            if (debt.dueDateEpochMillis != null || debt.note.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (debt.dueDateEpochMillis != null) {
                        val dueLocalDate = Instant.ofEpochMilli(debt.dueDateEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        Text(
                            text = if (isOverdue) "⚠️ Muddat oʻtdi: ${formatDayMonth(dueLocalDate)}" else "Muddat: ${formatDayMonth(dueLocalDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (debt.note.isNotEmpty()) {
                        Text(
                            text = debt.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!debt.isSettled) {
                    OutlinedButton(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(stringResource(R.string.debt_record_payment), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(
                    onClick = onToggleSettled,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (debt.isSettled) "Qayta ochish" else stringResource(R.string.debt_settle_action),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtDialog(
    defaultType: DebtType,
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, type: DebtType, amount: Long, dueDate: Long?, note: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(defaultType) }
    var dueDateEpochMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_add_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Selector (Lent vs Borrowed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { selectedType = DebtType.LENT },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedType == DebtType.LENT) {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.15f))
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                    ) {
                        Text("Bergan qarzim", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { selectedType = DebtType.BORROWED },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedType == DebtType.BORROWED) {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD84315).copy(alpha = 0.15f))
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                    ) {
                        Text("Olgan qarzim", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.debt_person_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.amount)) },
                    placeholder = { Text("1 000 000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation(),
                    singleLine = true,
                    suffix = { Text("soʻm") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.debt_phone)) },
                    placeholder = { Text("+998 90 123 45 67") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Due Date picker row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.debt_due_date),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            text = if (dueDateEpochMillis != null) {
                                val d = Instant.ofEpochMilli(dueDateEpochMillis!!).atZone(ZoneId.systemDefault()).toLocalDate()
                                formatDayMonth(d)
                            } else {
                                stringResource(R.string.debt_no_due_date)
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.detail_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    if (name.isNotBlank() && amt > 0) {
                        onAdd(name, phone, selectedType, amt, dueDateEpochMillis, note)
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateEpochMillis ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDateEpochMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    },
                ) {
                    Text("Tanlash")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dueDateEpochMillis = null
                        showDatePicker = false
                    },
                ) {
                    Text("Tozalash")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
