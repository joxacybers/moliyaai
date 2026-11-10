package com.example.myapplication.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.myapplication.R
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category

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
