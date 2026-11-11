package com.example.myapplication.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myapplication.FinanceApp
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class FinanceReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinanceApp ?: return Result.success()
        val container = app.container

        val today = LocalDate.now()
        val todayDay = today.dayOfMonth

        // 1. Check Subscriptions for today
        try {
            val subscriptions = container.subscriptionRepository.getAll()
            for (sub in subscriptions) {
                if (sub.isActive && sub.billingDay == todayDay) {
                    AppNotificationHelper.showSubscriptionReminder(
                        context = applicationContext,
                        name = sub.name,
                        amount = sub.amount,
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Check Debts due today
        try {
            val debts = container.debtRepository.allDebts.first()
            val zone = ZoneId.systemDefault()
            for (debt in debts) {
                if (!debt.isSettled && debt.dueDateEpochMillis != null) {
                    val dueLocalDate = Instant.ofEpochMilli(debt.dueDateEpochMillis).atZone(zone).toLocalDate()
                    if (dueLocalDate == today) {
                        AppNotificationHelper.showDebtReminder(
                            context = applicationContext,
                            personName = debt.personName,
                            amount = debt.remainingAmount,
                            isLent = debt.type == com.example.myapplication.data.db.DebtType.LENT,
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "FinanceDailyReminderWork"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FinanceReminderWorker>(12, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
