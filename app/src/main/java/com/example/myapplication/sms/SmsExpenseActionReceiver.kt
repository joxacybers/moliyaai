package com.example.myapplication.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.FinanceApp
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.notification.AppNotificationHelper
import com.example.myapplication.ui.common.formatAmount
import com.example.myapplication.ui.common.labelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SmsExpenseActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        if (intent.action == ACTION_CONFIRM) {
            val amount = intent.getLongExtra(EXTRA_AMOUNT, 0L)
            val merchant = intent.getStringExtra(EXTRA_MERCHANT) ?: "Karta toʻlovi"
            val categoryName = intent.getStringExtra(EXTRA_CATEGORY) ?: Category.FOOD.name
            val typeName = intent.getStringExtra(EXTRA_TYPE) ?: TransactionType.EXPENSE.name
            val accountName = intent.getStringExtra(EXTRA_ACCOUNT) ?: Account.CARD.name

            val category = runCatching { Category.valueOf(categoryName) }.getOrDefault(Category.FOOD)
            val type = runCatching { TransactionType.valueOf(typeName) }.getOrDefault(TransactionType.EXPENSE)
            val account = runCatching { Account.valueOf(accountName) }.getOrDefault(Account.CARD)

            if (amount <= 0) return

            val app = context.applicationContext as? FinanceApp ?: return
            val container = app.container

            val entity = TransactionEntity(
                amount = amount,
                type = type,
                category = category,
                account = account,
                epochDay = LocalDate.now().toEpochDay(),
                note = merchant,
            )

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    container.transactionRepository.save(entity)

                    // Budget alert check if expense
                    if (type == TransactionType.EXPENSE) {
                        val budget = container.budgetRepository.getForCategory(category)
                        if (budget != null && budget.limitAmount > 0) {
                            val allTx = container.transactionRepository.getAll()
                            val now = LocalDate.now()
                            val monthSpent = allTx.filter {
                                it.type == TransactionType.EXPENSE &&
                                    it.category == category &&
                                    it.date.year == now.year &&
                                    it.date.monthValue == now.monthValue
                            }.sumOf { it.amount }

                            val percent = (monthSpent * 100 / budget.limitAmount).toInt()
                            if (percent >= 80) {
                                val catLabel = context.getString(category.labelRes)
                                AppNotificationHelper.showBudgetAlert(
                                    context = context,
                                    categoryName = catLabel,
                                    percent = percent,
                                    spent = monthSpent,
                                    limit = budget.limitAmount,
                                )
                            }
                        }
                    }

                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "✅ $merchant: ${formatAmount(amount)} soʻm kiritildi",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM = "com.example.myapplication.sms.ACTION_CONFIRM"
        const val ACTION_DISMISS = "com.example.myapplication.sms.ACTION_DISMISS"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_ACCOUNT = "extra_account"
    }
}
