package com.example.myapplication.sms

import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType

data class ParsedSmsTransaction(
    val amount: Long,
    val merchant: String,
    val category: Category,
    val type: TransactionType = TransactionType.EXPENSE,
    val account: Account = Account.CARD,
    val rawText: String,
)
