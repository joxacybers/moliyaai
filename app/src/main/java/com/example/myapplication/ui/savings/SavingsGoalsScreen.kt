package com.example.myapplication.ui.savings

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
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
import com.example.myapplication.data.db.SavingsGoalEntity
import com.example.myapplication.ui.common.EmptyState
import com.example.myapplication.ui.common.SectionTitle
import com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.money

private data class GoalPreset(
    val name: String,
    val targetAmount: Long,
    val iconKey: String,
    val colorHex: Long,
)

private val GOAL_PRESETS = listOf(
    GoalPreset("Avtomobil olish", 80_000_000L, "car", 0xFF1976D2),
    GoalPreset("Umra / Sayohat", 25_000_000L, "plane", 0xFF00897B),
    GoalPreset("Yangi telefon", 12_000_000L, "phone", 0xFF7B1FA2),
    GoalPreset("Uy taʼmiri", 40_000_000L, "home", 0xFFE65100),
    GoalPreset("Oʻqish / Taʼlim", 15_000_000L, "study", 0xFF303F9F),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: SavingsGoalsViewModel = viewModel {
        SavingsGoalsViewModel(container.savingsRepository, container.transactionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var addingFundsGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var addingEmergencyFunds by remember { mutableStateOf(false) }
    var fundsInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Jamgʻarmalar & Maqsadlar", fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, contentDescription = "Yangi maqsad")
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
            // 1. Emergency Fund Card (Moliyaviy yostiqcha)
            item {
                EmergencyFundCard(
                    balance = state.emergencyFundBalance,
                    recommended = state.recommendedEmergencyFund,
                    percent = state.emergencyFundPercent,
                    onAddFunds = {
                        addingEmergencyFunds = true
                        fundsInput = ""
                    },
                    onPercentChange = viewModel::setEmergencyPercent,
                )
            }

            // 2. Section: Savings Goals
            item {
                SectionTitle(
                    text = "Orzular va Maqsadlar",
                    action = {
                        Text(
                            text = "Jami: ${money(state.totalGoalsSaved)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }

            if (state.goals.isEmpty()) {
                item {
                    EmptyState(text = "Hozircha maqsadlar yoʻq. + tugmasini bosib yangi maqsad qoʻshing!")
                }
            } else {
                items(state.goals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onAddFunds = {
                            addingFundsGoal = goal
                            fundsInput = ""
                        },
                        onDelete = { viewModel.deleteGoal(goal.id) },
                    )
                }
            }
        }
    }

    // Add Funds to Goal Dialog
    addingFundsGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { addingFundsGoal = null },
            title = { Text("${goal.name} ga mablagʻ qoʻshish") },
            text = {
                Column {
                    Text(
                        text = "Qolgan summa: ${money(goal.remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fundsInput,
                        onValueChange = { fundsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Qoʻshiladigan summa") },
                        placeholder = { Text("500 000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("soʻm") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = fundsInput.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            viewModel.addFundsToGoal(goal.id, amount)
                        }
                        addingFundsGoal = null
                    },
                ) {
                    Text("Qoʻshish")
                }
            },
            dismissButton = {
                TextButton(onClick = { addingFundsGoal = null }) {
                    Text("Bekor qilish")
                }
            },
        )
    }

    // Add Emergency Funds Dialog
    if (addingEmergencyFunds) {
        AlertDialog(
            onDismissRequest = { addingEmergencyFunds = false },
            title = { Text("🛡️ Moliyaviy yostiqchaga mablagʻ qoʻshish") },
            text = {
                Column {
                    Text(
                        text = "Joriy zaxira: ${money(state.emergencyFundBalance)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fundsInput,
                        onValueChange = { fundsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Summa") },
                        placeholder = { Text("1 000 000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("soʻm") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = fundsInput.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            viewModel.addEmergencyFunds(amount)
                        }
                        addingEmergencyFunds = false
                    },
                ) {
                    Text("Saqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = { addingEmergencyFunds = false }) {
                    Text("Bekor qilish")
                }
            },
        )
    }

    // Create Goal Dialog
    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, target, icon, color ->
                viewModel.addGoal(name, target, icon, color, null)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun EmergencyFundCard(
    balance: Long,
    recommended: Long,
    percent: Int,
    onAddFunds: () -> Unit,
    onPercentChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🛡️ Moliyaviy yostiqcha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                OutlinedButton(
                    onClick = onAddFunds,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("+ Qoʻshish", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = money(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            if (recommended > 0) {
                Spacer(Modifier.height(8.dp))
                val progress = (balance.toFloat() / recommended).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Tavsiya (3 oylik zaxira): ${money(recommended)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(Modifier.height(10.dp))

            // Auto-save percentage selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Har kirimdan ajratish:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 20).forEach { p ->
                        FilterChip(
                            selected = percent == p,
                            onClick = { onPercentChange(p) },
                            label = { Text("$p%") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    onAddFunds: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconEmoji = when (goal.iconKey) {
                    "car" -> "🚗"
                    "plane" -> "✈️"
                    "phone" -> "📱"
                    "home" -> "🏠"
                    "study" -> "🎓"
                    else -> "🎯"
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(goal.colorHex).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = iconEmoji, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (goal.isCompleted) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "🎉 Erishildi!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${money(goal.currentAmount)} / ${money(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(goal.colorHex),
                drawStopIndicator = {},
            )

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (goal.isCompleted) "Toʻliq toʻplandi!" else "Qolgan: ${money(goal.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${goal.percent}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(goal.colorHex),
                )
            }

            if (!goal.isCompleted) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(
                        onClick = onAddFunds,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("+ Mablagʻ qoʻshish", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, targetAmount: Long, iconKey: String, colorHex: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("car") }
    var selectedColor by remember { mutableStateOf(0xFF1976D2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yangi maqsad qoʻshish", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Presets
                Text(
                    text = "Tezkor tanlov:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GOAL_PRESETS.take(3).forEach { preset ->
                        FilterChip(
                            selected = name == preset.name,
                            onClick = {
                                name = preset.name
                                targetText = preset.targetAmount.toString()
                                selectedIcon = preset.iconKey
                                selectedColor = preset.colorHex
                            },
                            label = { Text(preset.name.take(12)) },
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Maqsad nomi") },
                    placeholder = { Text("Avtomobil olish") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                    label = { Text("Kerakli summa") },
                    placeholder = { Text("50 000 000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    singleLine = true,
                    suffix = { Text("soʻm") },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Icon selection
                Text(
                    text = "Ikonka tanlang:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "car" to "🚗",
                        "plane" to "✈️",
                        "phone" to "📱",
                        "home" to "🏠",
                        "study" to "🎓",
                        "general" to "🎯",
                    ).forEach { (key, emoji) ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = emoji)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = targetText.toLongOrNull() ?: 0L
                    if (name.isNotBlank() && amount > 0) {
                        onAdd(name, amount, selectedIcon, selectedColor)
                    }
                },
            ) {
                Text("Saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        },
    )
}
