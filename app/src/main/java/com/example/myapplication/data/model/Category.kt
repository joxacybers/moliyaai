package com.example.myapplication.data.model

import com.example.myapplication.data.model.TransactionType.EXPENSE
import com.example.myapplication.data.model.TransactionType.INCOME

enum class TransactionType { INCOME, EXPENSE }

/**
 * Fixed set of categories. Stored by [name] in the database and in backups, so
 * entries must never be renamed — only added.
 */
enum class Category(val type: TransactionType) {
    SALARY(INCOME),
    FREELANCE(INCOME),
    BUSINESS(INCOME),
    GIFT(INCOME),
    OTHER_INCOME(INCOME),

    FOOD(EXPENSE),
    TRANSPORT(EXPENSE),
    UTILITIES(EXPENSE),
    RENT(EXPENSE),
    HEALTH(EXPENSE),
    EDUCATION(EXPENSE),
    ENTERTAINMENT(EXPENSE),
    SHOPPING(EXPENSE),
    LOANS(EXPENSE),
    OTHER_EXPENSE(EXPENSE);

    companion object {
        fun forType(type: TransactionType): List<Category> = entries.filter { it.type == type }
    }
}
