package com.example.myapplication.ui.savings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
    GoalPreset("Noutbuk / Kompyuter", 18_000_000L, "laptop", 0xFF0288D1),
    GoalPreset("Toʻy / Marosim", 50_000_000L, "heart", 0xFFC2185B),
    GoalPreset("Biznes boshlash", 100_000_000L, "business", 0xFF2E7D32),
)

private data class GoalIconOption(
    val key: String,
    @DrawableRes val iconRes: Int,
    val defaultColorHex: Long,
)

private val GOAL_ICON_OPTIONS = listOf(
    GoalIconOption("car", R.drawable.ic_cat_transport, 0xFF1976D2),
    GoalIconOption("plane", R.drawable.ic_plane, 0xFF00897B),
    GoalIconOption("phone", R.drawable.ic_phone_android, 0xFF7B1FA2),
    GoalIconOption("home", R.drawable.ic_cat_rent, 0xFFE65100),
    GoalIconOption("study", R.drawable.ic_cat_education, 0xFF303F9F),
    GoalIconOption("laptop", R.drawable.ic_cat_freelance, 0xFF0288D1),
    GoalIconOption("business", R.drawable.ic_cat_business, 0xFF2E7D32),
    GoalIconOption("gift", R.drawable.ic_cat_gift, 0xFFE91E63),
    GoalIconOption("heart", R.drawable.ic_heart, 0xFFC2185B),
    GoalIconOption("diamond", R.drawable.ic_diamond, 0xFFF57F17),
    GoalIconOption("shopping", R.drawable.ic_cat_shopping, 0xFF689F38),
    GoalIconOption("health", R.drawable.ic_cat_health, 0xFFD32F2F),
    GoalIconOption("fitness", R.drawable.ic_fitness, 0xFF0097A7),
    GoalIconOption("child", R.drawable.ic_child, 0xFF5E35B1),
    GoalIconOption("build", R.drawable.ic_build, 0xFF5D4037),
    GoalIconOption("general", R.drawable.ic_trophy, 0xFF1E88E5),
)

@DrawableRes
private fun goalIconRes(iconKey: String): Int =
    GOAL_ICON_OPTIONS.firstOrNull { it.key == iconKey }?.iconRes ?: R.drawable.ic_trophy

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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Moliyaviy yostiqchaga mablagʻ qoʻshish")
                }
            },
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
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
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shield),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Moliyaviy yostiqcha",
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
    val goalColor = Color(goal.colorHex)
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(goalColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(goalIconRes(goal.iconKey)),
                        contentDescription = goal.name,
                        tint = goalColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (goal.isCompleted) {
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_trophy),
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Erishildi!",
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
                color = goalColor,
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
                    color = goalColor,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, targetAmount: Long, iconKey: String, colorHex: Long) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("car") }
    var selectedColor by remember { mutableLongStateOf(0xFF1976D2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yangi maqsad qoʻshish", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Horizontal scrollable presets so chip labels never wrap vertically
                Text(
                    text = "Tezkor tanlov (ustiga bosing):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GOAL_PRESETS.forEach { preset ->
                        val isSelected = name == preset.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                name = preset.name
                                targetText = preset.targetAmount.toString()
                                selectedIcon = preset.iconKey
                                selectedColor = preset.colorHex
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(goalIconRes(preset.iconKey)),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(preset.colorHex),
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = preset.name,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Maqsad nomi") },
                    placeholder = { Text("Masalan: Avtomobil olish") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                    label = { Text("Kerakli summa") },
                    placeholder = { Text("50 000 000") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    singleLine = true,
                    suffix = { Text("soʻm") },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Vector Icon Grid Selection (16 icons)
                Text(
                    text = "Ikonka tanlang:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GOAL_ICON_OPTIONS.forEach { option ->
                        val isSelected = selectedIcon == option.key
                        val tint = Color(option.defaultColorHex)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) tint.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .then(
                                    if (isSelected) Modifier.border(2.dp, tint, CircleShape)
                                    else Modifier
                                )
                                .clickable {
                                    selectedIcon = option.key
                                    selectedColor = option.defaultColorHex
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(option.iconRes),
                                contentDescription = option.key,
                                tint = if (isSelected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
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
                        onAdd(name.trim(), amount, selectedIcon, selectedColor)
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
