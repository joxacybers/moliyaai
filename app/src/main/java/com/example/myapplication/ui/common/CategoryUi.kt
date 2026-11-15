package com.example.myapplication.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.CustomCategory
import com.example.myapplication.data.model.TransactionType

@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.SALARY -> R.string.cat_salary
        Category.FREELANCE -> R.string.cat_freelance
        Category.BUSINESS -> R.string.cat_business
        Category.GIFT -> R.string.cat_gift
        Category.OTHER_INCOME -> R.string.cat_other_income
        Category.FOOD -> R.string.cat_food
        Category.TRANSPORT -> R.string.cat_transport
        Category.UTILITIES -> R.string.cat_utilities
        Category.RENT -> R.string.cat_rent
        Category.HEALTH -> R.string.cat_health
        Category.EDUCATION -> R.string.cat_education
        Category.ENTERTAINMENT -> R.string.cat_entertainment
        Category.SHOPPING -> R.string.cat_shopping
        Category.LOANS -> R.string.cat_loans
        Category.OTHER_EXPENSE -> R.string.cat_other_expense
    }

@get:DrawableRes
val Category.iconRes: Int
    get() = when (this) {
        Category.SALARY -> R.drawable.ic_cat_salary
        Category.FREELANCE -> R.drawable.ic_cat_freelance
        Category.BUSINESS -> R.drawable.ic_cat_business
        Category.GIFT -> R.drawable.ic_cat_gift
        Category.OTHER_INCOME -> R.drawable.ic_cat_other
        Category.FOOD -> R.drawable.ic_cat_food
        Category.TRANSPORT -> R.drawable.ic_cat_transport
        Category.UTILITIES -> R.drawable.ic_cat_utilities
        Category.RENT -> R.drawable.ic_cat_rent
        Category.HEALTH -> R.drawable.ic_cat_health
        Category.EDUCATION -> R.drawable.ic_cat_education
        Category.ENTERTAINMENT -> R.drawable.ic_cat_entertainment
        Category.SHOPPING -> R.drawable.ic_cat_shopping
        Category.LOANS -> R.drawable.ic_cat_loans
        Category.OTHER_EXPENSE -> R.drawable.ic_cat_other
    }

val Category.tintColor: Color
    get() = when (this) {
        Category.SALARY -> Color(0xFF2E7D32)
        Category.FREELANCE -> Color(0xFF00796B)
        Category.BUSINESS -> Color(0xFF0288D1)
        Category.GIFT -> Color(0xFFE91E63)
        Category.OTHER_INCOME -> Color(0xFF43A047)
        Category.FOOD -> Color(0xFFE65100)
        Category.TRANSPORT -> Color(0xFF1976D2)
        Category.UTILITIES -> Color(0xFFF57F17)
        Category.RENT -> Color(0xFF7B1FA2)
        Category.HEALTH -> Color(0xFFD32F2F)
        Category.EDUCATION -> Color(0xFF0097A7)
        Category.ENTERTAINMENT -> Color(0xFFC2185B)
        Category.SHOPPING -> Color(0xFF689F38)
        Category.LOANS -> Color(0xFF5D4037)
        Category.OTHER_EXPENSE -> Color(0xFF455A64)
    }

val Category.containerColor: Color
    get() = tintColor.copy(alpha = 0.15f)

val Category.emoji: String
    get() = when (this) {
        Category.SALARY -> "💼"
        Category.FREELANCE -> "💻"
        Category.BUSINESS -> "🏪"
        Category.GIFT -> "🎁"
        Category.OTHER_INCOME -> "💰"
        Category.FOOD -> "🍽️"
        Category.TRANSPORT -> "🚕"
        Category.UTILITIES -> "💡"
        Category.RENT -> "🏠"
        Category.HEALTH -> "💊"
        Category.EDUCATION -> "🎓"
        Category.ENTERTAINMENT -> "🎬"
        Category.SHOPPING -> "🛍️"
        Category.LOANS -> "🏦"
        Category.OTHER_EXPENSE -> "📦"
    }

val Account.tintColor: Color
    get() = when (this) {
        Account.CARD -> Color(0xFF1976D2)
        Account.CASH -> Color(0xFF2E7D32)
        Account.DEBT -> Color(0xFFE65100)
    }

val Account.containerColor: Color
    get() = tintColor.copy(alpha = 0.12f)

data class CustomCategoryIconOption(
    val key: String,
    @DrawableRes val iconRes: Int,
    val defaultColorHex: Long,
)

val CUSTOM_CATEGORY_ICONS = listOf(
    CustomCategoryIconOption("food", R.drawable.ic_cat_food, 0xFFE65100),
    CustomCategoryIconOption("coffee", R.drawable.ic_coffee, 0xFF6D4C41),
    CustomCategoryIconOption("shopping", R.drawable.ic_cat_shopping, 0xFF689F38),
    CustomCategoryIconOption("transport", R.drawable.ic_cat_transport, 0xFF1976D2),
    CustomCategoryIconOption("plane", R.drawable.ic_plane, 0xFF00897B),
    CustomCategoryIconOption("home", R.drawable.ic_cat_rent, 0xFF7B1FA2),
    CustomCategoryIconOption("utilities", R.drawable.ic_cat_utilities, 0xFFF57F17),
    CustomCategoryIconOption("wifi", R.drawable.ic_wifi, 0xFF0288D1),
    CustomCategoryIconOption("phone", R.drawable.ic_phone_android, 0xFF5E35B1),
    CustomCategoryIconOption("health", R.drawable.ic_cat_health, 0xFFD32F2F),
    CustomCategoryIconOption("fitness", R.drawable.ic_fitness, 0xFF0097A7),
    CustomCategoryIconOption("education", R.drawable.ic_cat_education, 0xFF303F9F),
    CustomCategoryIconOption("entertainment", R.drawable.ic_cat_entertainment, 0xFFC2185B),
    CustomCategoryIconOption("child", R.drawable.ic_child, 0xFF8E24AA),
    CustomCategoryIconOption("pets", R.drawable.ic_pets, 0xFF795548),
    CustomCategoryIconOption("build", R.drawable.ic_build, 0xFF546E7A),
    CustomCategoryIconOption("gift", R.drawable.ic_cat_gift, 0xFFE91E63),
    CustomCategoryIconOption("heart", R.drawable.ic_heart, 0xFFD81B60),
    CustomCategoryIconOption("diamond", R.drawable.ic_diamond, 0xFFF9A825),
    CustomCategoryIconOption("business", R.drawable.ic_cat_business, 0xFF1565C0),
    CustomCategoryIconOption("freelance", R.drawable.ic_cat_freelance, 0xFF00796B),
    CustomCategoryIconOption("salary", R.drawable.ic_cat_salary, 0xFF2E7D32),
    CustomCategoryIconOption("savings", R.drawable.ic_savings, 0xFF388E3C),
    CustomCategoryIconOption("trophy", R.drawable.ic_trophy, 0xFFF57C00),
    CustomCategoryIconOption("other", R.drawable.ic_cat_other, 0xFF455A64),
)

@DrawableRes
fun customCategoryIconRes(iconKey: String): Int =
    CUSTOM_CATEGORY_ICONS.firstOrNull { it.key == iconKey }?.iconRes ?: R.drawable.ic_cat_other

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCustomCategoryDialog(
    initialType: TransactionType = TransactionType.EXPENSE,
    showTypeSelector: Boolean = true,
    onDismiss: () -> Unit,
    onAdd: (CustomCategory) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedIconKey by remember { mutableStateOf("coffee") }
    var selectedColorHex by remember { mutableLongStateOf(0xFF6D4C41) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Yangi toifa qoʻshish", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showTypeSelector) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = {
                                Text(
                                    text = stringResource(R.string.expense),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            label = {
                                Text(
                                    text = stringResource(R.string.income),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Toifa nomi") },
                    placeholder = { Text("Masalan: Kafe, Yonilgʻi, Ehson...") },
                    isError = nameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Mos ikonkani tanlang:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CUSTOM_CATEGORY_ICONS.forEach { option ->
                        val isSelected = selectedIconKey == option.key
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
                                    selectedIconKey = option.key
                                    selectedColorHex = option.defaultColorHex
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
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        nameError = true
                    } else {
                        onAdd(
                            CustomCategory(
                                name = trimmed,
                                type = selectedType,
                                iconKey = selectedIconKey,
                                colorHex = selectedColorHex,
                            ),
                        )
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
