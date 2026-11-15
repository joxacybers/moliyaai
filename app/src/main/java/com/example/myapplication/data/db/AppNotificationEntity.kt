package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType

enum class AppNotificationType {
    SMS_DRAFT,
    EMERGENCY_FUND,
    BUDGET_ALERT,
    DEBT_REMINDER,
    SUBSCRIPTION,
    GENERAL,
}

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AppNotificationType,
    val title: String,
    val message: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isActionCompleted: Boolean = false,
    val draftAmount: Long = 0L,
    val draftTransactionType: TransactionType = TransactionType.EXPENSE,
    val draftCategory: Category = Category.FOOD,
    val draftCustomCategoryName: String = "",
    val draftCustomCategoryIcon: String = "",
    val draftCustomCategoryColor: Long = 0L,
    val draftAccount: Account = Account.CARD,
    val draftMerchantOrNote: String = "",
    val rawSmsText: String = "",
)
