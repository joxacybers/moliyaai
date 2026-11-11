package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.AppContainer
import com.example.myapplication.notification.AppNotificationHelper
import com.example.myapplication.notification.FinanceReminderWorker
import com.example.myapplication.updater.UpdateCheckWorker
import com.example.myapplication.updater.UpdateNotificationHelper

class FinanceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        UpdateNotificationHelper.createNotificationChannel(this)
        AppNotificationHelper.createChannels(this)
        UpdateCheckWorker.schedule(this)
        FinanceReminderWorker.schedule(this)
    }
}
