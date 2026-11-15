package com.example.myapplication.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.db.AppNotificationEntity
import com.example.myapplication.data.db.AppNotificationType
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.CustomCategory
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.ui.common.EmptyState
import com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.customCategoryIconRes
import com.example.myapplication.ui.common.expenseColor
import com.example.myapplication.ui.common.formatAmount
import com.example.myapplication.ui.common.iconRes
import com.example.myapplication.ui.common.incomeColor
import com.example.myapplication.ui.common.labelRes
import com.example.myapplication.ui.common.tintColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val context = LocalContext.current
    val viewModel: NotificationsViewModel = viewModel {
        NotificationsViewModel(
            notificationRepo = container.notificationRepository,
            transactionRepo = container.transactionRepository,
            budgetRepo = container.budgetRepository,
            savingsRepo = container.savingsRepository,
            settingsRepo = container.settingsRepository,
            aiAdvisor = container.aiAdvisor,
            appContext = context.applicationContext,
        )
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingDraft by remember { mutableStateOf<AppNotificationEntity?>(null) }
    var showTestSmsDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val readSmsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                viewModel.scanInboxSmsMessages()
            }
        },
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bildirishnomalar va Qoralamalar", fontWeight = FontWeight.Bold)
                        if (state.pendingDraftsCount > 0) {
                            Text(
                                text = "${state.pendingDraftsCount} ta SMS qoralama tasdiqlash kutilmoqda",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        IconButton(onClick = viewModel::markAllAsRead) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Barchasini oʻqilgan qilish",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (state.notifications.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Tozalash",
                                tint = MaterialTheme.colorScheme.outline,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // AI SMS Scanner & Manual Test Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_auto_awesome),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI SMS Xarid va Sotuv Qoralamalari",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Bank va toʻlov SMS xabarlari AI orqali avtomatik toifalarga ajratilib, qoralama sifatida shu yerda saqlanadi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    val hasReadSms = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_SMS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasReadSms) {
                                        viewModel.scanInboxSmsMessages()
                                    } else {
                                        readSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                    }
                                },
                                enabled = !state.isScanningSms,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (state.isScanningSms) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tahlil qilinmoqda...")
                                } else {
                                    Text("SMS xabarlarni tekshirish")
                                }
                            }
                            OutlinedButton(
                                onClick = { showTestSmsDialog = true },
                                enabled = !state.isScanningSms,
                            ) {
                                Text("SMS matn kiritish")
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.filter == NotificationFilter.ALL,
                        onClick = { viewModel.setFilter(NotificationFilter.ALL) },
                        label = { Text("Barchasi") },
                    )
                    FilterChip(
                        selected = state.filter == NotificationFilter.SMS_DRAFTS,
                        onClick = { viewModel.setFilter(NotificationFilter.SMS_DRAFTS) },
                        label = {
                            Text(
                                if (state.pendingDraftsCount > 0) {
                                    "SMS Qoralamalar (${state.pendingDraftsCount})"
                                } else {
                                    "SMS Qoralamalar"
                                },
                            )
                        },
                    )
                }
            }

            if (state.notifications.isEmpty()) {
                item {
                    EmptyState(
                        text = "Hozircha bildirishnomalar yoʻq.\nKelgan xarid/sotuv SMS qoralamalari, byudjet ogohlantirishlari va qarz eslatmalari shu yerda saqlanadi.",
                    )
                }
            } else {
                items(state.notifications, key = { it.id }) { item ->
                    NotificationItemCard(
                        item = item,
                        onMarkRead = { viewModel.markAsRead(item.id) },
                        onDelete = { viewModel.deleteNotification(item.id) },
                        onConfirmDraft = { viewModel.confirmSmsDraft(item) },
                        onEditDraft = { editingDraft = item },
                        onConfirmEmergencyFund = { viewModel.confirmEmergencyFundDeposit(item) },
                    )
                }
            }
        }
    }

    editingDraft?.let { draft ->
        EditSmsDraftDialog(
            draft = draft,
            customCategories = state.customCategories,
            onDismiss = { editingDraft = null },
            onSaveOnly = { amount, type, category, customCat, account, note ->
                viewModel.updateSmsDraftOnly(
                    notification = draft,
                    amount = amount,
                    type = type,
                    category = category,
                    customCategory = customCat,
                    account = account,
                    note = note,
                )
                editingDraft = null
            },
            onConfirmAndAdd = { amount, type, category, customCat, account, note ->
                viewModel.confirmSmsDraft(
                    notification = draft,
                    overrideAmount = amount,
                    overrideType = type,
                    overrideCategory = category,
                    overrideCustomCategory = customCat,
                    clearCustomCategory = customCat == null,
                    overrideAccount = account,
                    overrideNote = note,
                )
                editingDraft = null
            },
        )
    }

    if (showTestSmsDialog) {
        ManualSmsAnalyzeDialog(
            onDismiss = { showTestSmsDialog = false },
            onAnalyze = { sender, body ->
                viewModel.analyzeCustomSmsText(sender, body)
                showTestSmsDialog = false
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Bildirishnomalarni tozalash", fontWeight = FontWeight.Bold) },
            text = { Text("Barcha saqlangan bildirishnomalar va qoralamalarni oʻchirishni xohlaysizmi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun NotificationItemCard(
    item: AppNotificationEntity,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDraft: () -> Unit,
    onEditDraft: () -> Unit,
    onConfirmEmergencyFund: () -> Unit,
) {
    val isSmsDraft = item.type == AppNotificationType.SMS_DRAFT
    val isPendingDraft = isSmsDraft && !item.isActionCompleted

    val containerColor = when {
        isPendingDraft -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        !item.isRead -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!item.isRead) onMarkRead() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPendingDraft) 2.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header row: Icon + Title + Timestamp + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (iconRes, iconTint) = notificationIconAndTint(item)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!item.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = formatNotificationTime(item.createdAtEpochMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Oʻchirish",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Message body
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // If SMS Draft, show rich AI Draft preview & Confirm/Edit buttons
            if (isSmsDraft && item.draftAmount > 0L) {
                val txType = item.draftTransactionType
                val cat = item.draftCategory
                val catLabel = item.draftCustomCategoryName.ifBlank { stringResource(cat.labelRes) }
                val account = item.draftAccount
                val typeColor = if (txType == TransactionType.INCOME) incomeColor() else expenseColor()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.isActionCompleted) {
                                    incomeColor().copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                },
                            ) {
                                Text(
                                    text = if (item.isActionCompleted) {
                                        "✅ Tasdiqlangan va qoʻshilgan"
                                    } else {
                                        "🤖 AI Qoralama • Tekshiring"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isActionCompleted) incomeColor() else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }

                            Text(
                                text = (if (txType == TransactionType.INCOME) "+" else "−") +
                                    "${formatAmount(item.draftAmount)} soʻm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = typeColor,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Toifa: $catLabel",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${if (txType == TransactionType.INCOME) "Kirim / Sotuv" else "Chiqim / Xarid"} • ${stringResource(account.labelRes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (item.draftMerchantOrNote.isNotBlank()) {
                            Text(
                                text = "Izoh / Manba: ${item.draftMerchantOrNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (item.rawSmsText.isNotBlank()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                            Text(
                                text = "SMS: ${item.rawSmsText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (!item.isActionCompleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onEditDraft,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tahrirlash")
                        }
                        Button(
                            onClick = onConfirmDraft,
                            modifier = Modifier.weight(1.3f),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tasdiqlash")
                        }
                    }
                }
            }

            // If Emergency Fund reminder, allow 1-tap deposit
            if (item.type == AppNotificationType.EMERGENCY_FUND && !item.isActionCompleted && item.draftAmount > 0L) {
                Button(
                    onClick = onConfirmEmergencyFund,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Yostiqchaga qoʻshish (+${formatAmount(item.draftAmount)} soʻm)")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSmsDraftDialog(
    draft: AppNotificationEntity,
    customCategories: List<CustomCategory>,
    onDismiss: () -> Unit,
    onSaveOnly: (Long, TransactionType, Category, CustomCategory?, Account, String) -> Unit,
    onConfirmAndAdd: (Long, TransactionType, Category, CustomCategory?, Account, String) -> Unit,
) {
    var type by remember { mutableStateOf(draft.draftTransactionType) }
    var amountText by remember { mutableStateOf(draft.draftAmount.toString()) }
    var category by remember {
        mutableStateOf(draft.draftCategory)
    }
    var selectedCustomCat by remember {
        val initialCustom = if (draft.draftCustomCategoryName.isNotBlank()) {
            CustomCategory(
                name = draft.draftCustomCategoryName,
                type = type,
                iconKey = draft.draftCustomCategoryIcon.ifBlank { "other" },
                colorHex = if (draft.draftCustomCategoryColor != 0L) draft.draftCustomCategoryColor else 0xFF1976D2,
            )
        } else null
        mutableStateOf(initialCustom)
    }
    var account by remember { mutableStateOf(draft.draftAccount) }
    var note by remember { mutableStateOf(draft.draftMerchantOrNote) }

    val availableCategories = Category.forType(type)
    val availableCustomCategories = customCategories.filter { it.type == type }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMS Qoralamani tekshirish", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (draft.rawSmsText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = draft.rawSmsText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                // Type selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = {
                            type = TransactionType.EXPENSE
                            category = Category.forType(TransactionType.EXPENSE).first()
                            selectedCustomCat = null
                        },
                        label = { Text("Chiqim (Xarid)") },
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = {
                            type = TransactionType.INCOME
                            category = Category.forType(TransactionType.INCOME).first()
                            selectedCustomCat = null
                        },
                        label = { Text("Kirim (Sotuv)") },
                    )
                }

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Summa") },
                    suffix = { Text("soʻm") },
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Note / Merchant input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Doʻkon / Manba / Izoh") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Account selector
                Text("Hisob:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Account.entries.forEach { acc ->
                        FilterChip(
                            selected = account == acc,
                            onClick = { account = acc },
                            label = { Text(stringResource(acc.labelRes)) },
                        )
                    }
                }

                // Category selector
                Text("Toifa (Kategoriya):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCustomCat == null && category == cat,
                            onClick = {
                                category = cat
                                selectedCustomCat = null
                            },
                            label = { Text(stringResource(cat.labelRes)) },
                        )
                    }
                    availableCustomCategories.forEach { customCat ->
                        FilterChip(
                            selected = selectedCustomCat?.name == customCat.name,
                            onClick = {
                                selectedCustomCat = customCat
                                category = if (type == TransactionType.INCOME) Category.OTHER_INCOME else Category.OTHER_EXPENSE
                            },
                            label = { Text(customCat.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    if (amt > 0L) {
                        onConfirmAndAdd(amt, type, category, selectedCustomCat, account, note)
                    }
                },
            ) {
                Text("Tasdiqlash va Qoʻshish")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                TextButton(
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        if (amt > 0L) {
                            onSaveOnly(amt, type, category, selectedCustomCat, account, note)
                        }
                    },
                ) {
                    Text("Saqlash")
                }
            }
        },
    )
}

@Composable
private fun ManualSmsAnalyzeDialog(
    onDismiss: () -> Unit,
    onAnalyze: (String, String) -> Unit,
) {
    var sender by remember { mutableStateOf("HUMO") }
    var smsText by remember {
        mutableStateOf("HUMO: 85 000.00 UZS to'landi. KORZINKA SUPERMARKET. Balans: 1 450 000.00 UZS")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMS matnini AI orqali tahlil qilish", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Bank yoki toʻlov tizimidan kelgan xarid/sotuv SMS xabarini kiriting yoki namunani tanlang:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = sender == "HUMO",
                        onClick = {
                            sender = "HUMO"
                            smsText = "HUMO: 85 000.00 UZS to'landi. KORZINKA SUPERMARKET. Balans: 1 450 000.00 UZS"
                        },
                        label = { Text("Xarid (Korzinka)") },
                    )
                    FilterChip(
                        selected = sender == "CLICK",
                        onClick = {
                            sender = "CLICK"
                            smsText = "Click: Kartaga 450 000 so'm tushdi. Sotuv: Online do'kon buyurtma to'lovi. Balans: 2 100 000 so'm"
                        },
                        label = { Text("Sotuv (Kirim)") },
                    )
                }

                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Yuboruvchi (Masalan: HUMO, CLICK)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    label = { Text("SMS xabar matni") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (smsText.isNotBlank()) onAnalyze(sender, smsText) },
            ) {
                Text("AI orqali qoralamaga saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private fun notificationIconAndTint(item: AppNotificationEntity): Pair<Int, Color> {
    return when (item.type) {
        AppNotificationType.SMS_DRAFT -> {
            if (item.draftCustomCategoryIcon.isNotBlank()) {
                val res = customCategoryIconRes(item.draftCustomCategoryIcon)
                val color = if (item.draftCustomCategoryColor != 0L) {
                    Color(item.draftCustomCategoryColor)
                } else {
                    Color(0xFF1976D2)
                }
                res to color
            } else {
                val cat = item.draftCategory
                cat.iconRes to cat.tintColor
            }
        }
        AppNotificationType.EMERGENCY_FUND -> R.drawable.ic_shield to Color(0xFF00897B)
        AppNotificationType.BUDGET_ALERT -> R.drawable.ic_bar_chart to Color(0xFFE53935)
        AppNotificationType.DEBT_REMINDER -> R.drawable.ic_contacts to Color(0xFFFB8C00)
        AppNotificationType.SUBSCRIPTION -> R.drawable.ic_subscriptions to Color(0xFF3949AB)
        AppNotificationType.GENERAL -> R.drawable.ic_auto_awesome to Color(0xFF1E88E5)
    }
}

private fun formatNotificationTime(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()
    val today = LocalDate.now(zone)
    return if (dateTime.toLocalDate() == today) {
        "Bugun, " + dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }
}
