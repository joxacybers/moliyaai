package com.example.myapplication.ui.addedit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.CustomCategory
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.ui.common.AddCustomCategoryDialog
import com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.customCategoryIconRes
import com.example.myapplication.ui.common.formatDate
import com.example.myapplication.ui.common.iconRes
import com.example.myapplication.ui.common.labelRes
import com.example.myapplication.ui.common.tintColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEditScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val noteFocusRequester = remember { FocusRequester() }
    val noteBringIntoViewRequester = remember { BringIntoViewRequester() }

    val viewModel: AddEditViewModel = viewModel(key = "add_edit_$transactionId") {
        AddEditViewModel(
            repo = container.transactionRepository,
            budgetRepo = container.budgetRepository,
            savingsRepo = container.savingsRepository,
            settingsRepo = container.settingsRepository,
            appContext = context.applicationContext,
            transactionId = transactionId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { onNavigateBack() }
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
            TypeSelectorRow(
                selectedType = state.type,
                onSelectType = viewModel::setType,
            )

            // Isolated amount input: typing digits only recomposes this composable, never the categories/chips below
            AmountInputField(
                amountProvider = { viewModel.amountText },
                isError = state.amountError,
                onAmountChange = viewModel::setAmount,
                onNext = {
                    if (state.account == null) {
                        viewModel.setAccount(Account.CARD)
                    }
                    noteFocusRequester.requestFocus()
                },
            )

            AccountSelectorSection(
                selectedAccount = state.account,
                accountError = state.accountError,
                onSelectAccount = viewModel::setAccount,
            )

            CategorySelectorSection(
                type = state.type,
                selectedCategory = state.category,
                selectedCustomCategory = state.selectedCustomCategory,
                customCategories = state.customCategories,
                onSelectCategory = viewModel::setCategory,
                onSelectCustomCategory = viewModel::selectCustomCategory,
                onAddCustomCategoryClick = { showAddCategoryDialog = true },
            )

            DateSelectorSection(
                selectedDate = state.date,
                onSelectDate = viewModel::setDate,
                onOpenDatePicker = { showDatePicker = true },
            )

            // Isolated note input: typing characters only recomposes this composable
            NoteInputField(
                noteProvider = { viewModel.noteText },
                onNoteChange = viewModel::setNote,
                onDone = { focusManager.clearFocus() },
                focusRequester = noteFocusRequester,
                bringIntoViewRequester = noteBringIntoViewRequester,
                onFocused = {
                    scope.launch {
                        noteBringIntoViewRequester.bringIntoView()
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddCategoryDialog) {
        AddCustomCategoryDialog(
            initialType = state.type,
            showTypeSelector = false,
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { newCat ->
                viewModel.addAndSelectCustomCategory(newCat)
                showAddCategoryDialog = false
            },
        )
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

@Composable
private fun TypeSelectorRow(
    selectedType: TransactionType,
    onSelectType: (TransactionType) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedType == TransactionType.EXPENSE,
            onClick = { onSelectType(TransactionType.EXPENSE) },
            label = {
                Text(
                    stringResource(R.string.expense),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                )
            },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = selectedType == TransactionType.INCOME,
            onClick = { onSelectType(TransactionType.INCOME) },
            label = {
                Text(
                    stringResource(R.string.income),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AmountInputField(
    amountProvider: () -> String,
    isError: Boolean,
    onAmountChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val visualTransformation = remember { ThousandsSeparatorVisualTransformation() }
    val keyboardOptions = remember {
        KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        )
    }
    val keyboardActions = remember(onNext) {
        KeyboardActions(onNext = { onNext() })
    }

    OutlinedTextField(
        value = amountProvider(),
        onValueChange = onAmountChange,
        label = { Text(stringResource(R.string.amount)) },
        suffix = { Text(stringResource(R.string.currency_suffix)) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.amount_error)) }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun AccountSelectorSection(
    selectedAccount: Account?,
    accountError: Boolean,
    onSelectAccount: (Account) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${stringResource(R.string.account)} *",
            style = MaterialTheme.typography.labelLarge,
            color = if (accountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Account.entries.forEach { acc ->
                val isSelected = selectedAccount == acc
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectAccount(acc) },
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
                    colors = if (accountError) {
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
        if (accountError) {
            Text(
                text = stringResource(R.string.account_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelectorSection(
    type: TransactionType,
    selectedCategory: Category,
    selectedCustomCategory: CustomCategory?,
    customCategories: List<CustomCategory>,
    onSelectCategory: (Category) -> Unit,
    onSelectCustomCategory: (CustomCategory) -> Unit,
    onAddCustomCategoryClick: () -> Unit,
) {
    val standardCategories = remember(type) { Category.forType(type) }
    val filteredCustomCategories = remember(type, customCategories) {
        customCategories.filter { it.type == type }
    }

    Text(stringResource(R.string.category), style = MaterialTheme.typography.labelLarge)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        standardCategories.forEach { cat ->
            val isCatSelected = selectedCustomCategory == null && selectedCategory == cat
            FilterChip(
                selected = isCatSelected,
                onClick = { onSelectCategory(cat) },
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

        filteredCustomCategories.forEach { customCat ->
            val isCustomSelected = selectedCustomCategory?.name.equals(customCat.name, ignoreCase = true)
            FilterChip(
                selected = isCustomSelected,
                onClick = { onSelectCustomCategory(customCat) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(customCategoryIconRes(customCat.iconKey)),
                        contentDescription = null,
                        tint = if (isCustomSelected) MaterialTheme.colorScheme.primary else Color(customCat.colorHex),
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Text(
                        text = customCat.name,
                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
        }

        FilterChip(
            selected = false,
            onClick = onAddCustomCategoryClick,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = {
                Text(
                    text = "Yangi toifa",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
        )
    }
}

@Composable
private fun DateSelectorSection(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDatePicker: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val yesterday = remember(today) { today.minusDays(1) }
    val isToday = selectedDate == today
    val isYesterday = selectedDate == yesterday
    val isOther = !isToday && !isYesterday

    Text(stringResource(R.string.date), style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = isToday,
            onClick = { onSelectDate(today) },
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
            onClick = { onSelectDate(yesterday) },
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
            onClick = onOpenDatePicker,
            label = {
                Text(
                    text = if (isOther) formatDate(selectedDate) else stringResource(R.string.custom_date),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = if (isOther) FontWeight.Bold else FontWeight.Normal,
                )
            },
            modifier = Modifier.weight(1.2f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteInputField(
    noteProvider: () -> String,
    onNoteChange: (String) -> Unit,
    onDone: () -> Unit,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    onFocused: () -> Unit,
) {
    val keyboardOptions = remember { KeyboardOptions(imeAction = ImeAction.Done) }
    val keyboardActions = remember(onDone) { KeyboardActions(onDone = { onDone() }) }

    OutlinedTextField(
        value = noteProvider(),
        onValueChange = onNoteChange,
        label = { Text(stringResource(R.string.note_optional)) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    onFocused()
                }
            },
        singleLine = true,
    )
}
