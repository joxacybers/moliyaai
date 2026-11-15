package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.ai.AiAdvisor
import com.example.myapplication.data.BudgetRepository
import com.example.myapplication.data.DebtRepository
import com.example.myapplication.data.NotificationRepository
import com.example.myapplication.data.SavingsRepository
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.data.SubscriptionRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.backup.BackupManager
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.updater.UpdateManager

/** Simple manual DI container — enough for a single-module app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }
    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(database.subscriptionDao())
    }
    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.budgetDao())
    }
    val debtRepository: DebtRepository by lazy {
        DebtRepository(database.debtDao())
    }
    val savingsRepository: SavingsRepository by lazy {
        SavingsRepository(database.savingsGoalDao(), appContext)
    }
    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(database.appNotificationDao())
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val backupManager: BackupManager by lazy {
        BackupManager(appContext, transactionRepository, settingsRepository)
    }
    val aiAdvisor: AiAdvisor by lazy { AiAdvisor(appContext) }
    val updateManager: UpdateManager by lazy {
        UpdateManager(appContext)
    }
}
