package com.example.myapplication.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.db.BillingCycle
import com.example.myapplication.data.model.Account
import com.example.myapplication.ui.common.EmptyState
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.expenseColor
import com.example.myapplication.ui.common.formatDate
import com.example.myapplication.ui.common.incomeColor
import com.example.myapplication.ui.common.money
import com.example.myapplication.ui.common.tintColor

private data class ServicePreset(
    val name: String,
    val defaultAmountUzs: Long,
    val displayNote: String,
    val colorHex: Long,
)

private val SERVICE_PRESETS = listOf(
    ServicePreset("Uy ijarasi (Kvartira / Ofis)", 3_500_000L, "3 500 000 soʻm", 0xFF673AB7),
    ServicePreset("Internet / Wi-Fi", 150_000L, "150 000 soʻm", 0xFF0288D1),
    ServicePreset("Kommunal (Elektr, Gaz, Suv)", 250_000L, "250 000 soʻm", 0xFFFF9800),
    ServicePreset("Mobil aloqa / Tarif", 50_000L, "50 000 soʻm", 0xFF4CAF50),
    ServicePreset("Sport zal / Fitnes", 300_000L, "300 000 soʻm", 0xFFE91E63),
    ServicePreset("Oʻquv kursi / Repetitor", 500_000L, "500 000 soʻm", 0xFF009688),
    ServicePreset("Yandex Plus / Music", 15_990L, "15 990 soʻm", 0xFFFC3F1D),
    ServicePreset("Spotify Premium", 65_000L, "$4.99", 0xFF1DB954),
    ServicePreset("Netflix", 125_000L, "$9.99", 0xFFE50914),
    ServicePreset("YouTube Premium", 78_000L, "$5.99", 0xFFFF0000),
    ServicePreset("Telegram Premium", 45_000L, "45 000 soʻm", 0xFF0088CC),
    ServicePreset("ChatGPT Plus", 260_000L, "$20.00", 0xFF10A37F),
    ServicePreset("iCloud+", 13_000L, "$0.99", 0xFF007AFF),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: SubscriptionsViewModel = viewModel {
        SubscriptionsViewModel(container.subscriptionRepository, container.transactionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<SubscriptionItemUi?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_subscriptions), fontWeight = FontWeight.Bold) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Summary KPI Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.sub_monthly_total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = money(state.totalMonthlyCost),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier
                            .height(42.dp)
                            .padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.sub_yearly_total),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = money(state.totalYearlyCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                EmptyState(stringResource(R.string.sub_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.subscription.id }) { item ->
                        SubscriptionRowCard(
                            item = item,
                            onClick = { selectedItemForDetail = item },
                            onPayNow = { viewModel.recordPaymentNow(item.subscription) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, amount, displayPrice, cycle, day, account, color ->
                viewModel.addSubscription(name, amount, displayPrice, cycle, day, account, color)
                showAddDialog = false
            },
        )
    }

    selectedItemForDetail?.let { item ->
        SubscriptionDetailSheet(
            item = item,
            onDismiss = { selectedItemForDetail = null },
            onPayNow = {
                viewModel.recordPaymentNow(item.subscription)
                selectedItemForDetail = null
            },
            onDelete = {
                viewModel.deleteSubscription(item.subscription)
                selectedItemForDetail = null
            },
        )
    }
}

@Composable
private fun SubscriptionRowCard(
    item: SubscriptionItemUi,
    onClick: () -> Unit,
    onPayNow: () -> Unit,
) {
    val sub = item.subscription
    val brandColor = Color(sub.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Brand initial circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sub.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = brandColor,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (sub.displayPrice.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = sub.displayPrice,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(sub.account.iconRes),
                        contentDescription = null,
                        tint = sub.account.tintColor,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    val daysText = when (item.daysUntilNext) {
                        0 -> stringResource(R.string.today)
                        else -> stringResource(R.string.sub_days_left, item.daysUntilNext)
                    }
                    Text(
                        text = "${stringResource(sub.account.labelRes)} · $daysText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.daysUntilNext <= 3 && !item.isPaidThisMonth) {
                            expenseColor()
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(sub.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.height(4.dp))
                if (item.isPaidThisMonth) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = incomeColor(),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = stringResource(R.string.sub_paid_this_month),
                            style = MaterialTheme.typography.labelSmall,
                            color = incomeColor(),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Surface(
                        onClick = onPayNow,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.sub_pay_now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        amount: Long,
        displayPrice: String,
        billingCycle: BillingCycle,
        billingDay: Int,
        account: Account,
        colorHex: Long,
    ) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var displayPrice by remember { mutableStateOf("") }
    var billingDayText by remember { mutableStateOf("15") }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var account by remember { mutableStateOf(Account.CARD) }
    var colorHex by remember { mutableLongStateOf(0xFF1976D2L) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sub_add_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.sub_popular_services),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SERVICE_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = name == preset.name,
                            onClick = {
                                name = preset.name
                                amountText = preset.defaultAmountUzs.toString()
                                displayPrice = preset.displayNote
                                colorHex = preset.colorHex
                            },
                            label = { Text(preset.name) },
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = false },
                    label = { Text(stringResource(R.string.sub_service_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }; error = false },
                    label = { Text(stringResource(R.string.amount)) },
                    suffix = { Text(stringResource(R.string.currency_suffix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billingDayText,
                        onValueChange = { billingDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text(stringResource(R.string.sub_billing_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = displayPrice,
                        onValueChange = { displayPrice = it },
                        label = { Text(stringResource(R.string.sub_original_price)) },
                        placeholder = { Text("$4.99 / 15.00") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                    )
                }

                // Cycle selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = cycle == BillingCycle.MONTHLY,
                        onClick = { cycle = BillingCycle.MONTHLY },
                        label = {
                            Text(
                                stringResource(R.string.period_monthly),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = cycle == BillingCycle.YEARLY,
                        onClick = { cycle = BillingCycle.YEARLY },
                        label = {
                            Text(
                                stringResource(R.string.sub_yearly),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Account selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Account.entries.forEach { acc ->
                        val selected = account == acc
                        FilterChip(
                            selected = selected,
                            onClick = { account = acc },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(acc.iconRes),
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else acc.tintColor,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            label = { Text(stringResource(acc.labelRes)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (error) {
                    Text(
                        text = stringResource(R.string.amount_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    val day = (billingDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                    if (name.isBlank() || amt <= 0L) {
                        error = true
                    } else {
                        onSave(name, amt, displayPrice, cycle, day, account, colorHex)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionDetailSheet(
    item: SubscriptionItemUi,
    onDismiss: () -> Unit,
    onPayNow: () -> Unit,
    onDelete: () -> Unit,
) {
    val sub = item.subscription
    val brandColor = Color(sub.colorHex)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sub.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = brandColor,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(sub.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = money(sub.amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.sub_next_payment), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDate(item.nextBillingDate), fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.account), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(sub.account.labelRes), fontWeight = FontWeight.SemiBold)
                    }
                    if (sub.displayPrice.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.sub_original_price), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(sub.displayPrice, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = onPayNow,
                    enabled = !item.isPaidThisMonth,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (item.isPaidThisMonth) {
                            stringResource(R.string.sub_paid_this_month)
                        } else {
                            stringResource(R.string.sub_pay_now)
                        },
                    )
                }
            }
        }
    }
}
