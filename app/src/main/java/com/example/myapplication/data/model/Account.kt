package com.example.myapplication.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.myapplication.R

enum class Account {
    CARD,
    CASH,
    DEBT;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            CARD -> R.string.account_card
            CASH -> R.string.account_cash
            DEBT -> R.string.account_debt
        }

    @get:DrawableRes
    val iconRes: Int
        get() = when (this) {
            CARD -> R.drawable.ic_card
            CASH -> R.drawable.ic_cash
            DEBT -> R.drawable.ic_debt
        }

    val emoji: String
        get() = when (this) {
            CARD -> "💳"
            CASH -> "💵"
            DEBT -> "🤝"
        }
}
