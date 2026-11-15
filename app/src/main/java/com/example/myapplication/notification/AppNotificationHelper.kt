package com.example.myapplication.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.FinanceApp
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.db.AppNotificationEntity
import com.example.myapplication.data.db.AppNotificationType
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.sms.ParsedSmsTransaction
import com.example.myapplication.sms.SmsExpenseActionReceiver
import com.example.myapplication.ui.common.formatAmount
import com.example.myapplication.ui.common.labelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppNotificationHelper {
    private const val CHANNEL_BUDGET_ID = "budget_alerts_channel"
    private const val CHANNEL_REMINDERS_ID = "reminders_channel"
    private const val CHANNEL_SMS_ID = "sms_transactions_channel"

    private fun formatMoneyText(amount: Long): String = "${formatAmount(amount)} soʻm"

    fun createChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET_ID,
            context.getString(R.string.notification_channel_budget),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_budget_desc)
            enableVibration(true)
        }

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS_ID,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_desc)
        }

        val smsChannel = NotificationChannel(
            CHANNEL_SMS_ID,
            context.getString(R.string.notification_channel_sms),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_sms_desc)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(budgetChannel)
        notificationManager.createNotificationChannel(remindersChannel)
        notificationManager.createNotificationChannel(smsChannel)
    }

    suspend fun saveAndShowSmsDraft(
        context: Context,
        parsed: ParsedSmsTransaction,
    ): Long {
        val catLabel = parsed.customCategoryName.ifBlank { context.getString(parsed.category.labelRes) }
        val isIncome = parsed.type == TransactionType.INCOME
        val title = if (isIncome) {
            "💰 Yangi sotuv / tushum: ${formatMoneyText(parsed.amount)}"
        } else {
            "💳 Yangi xarid: ${formatMoneyText(parsed.amount)}"
        }
        val message = "${parsed.merchant} • Toifa: $catLabel (AI qoralama tayyor — tekshirib tasdiqlang)"

        val app = context.applicationContext as? FinanceApp
        val dbId = app?.container?.notificationRepository?.insert(
            AppNotificationEntity(
                type = AppNotificationType.SMS_DRAFT,
                title = title,
                message = message,
                draftAmount = parsed.amount,
                draftTransactionType = parsed.type,
                draftCategory = parsed.category,
                draftCustomCategoryName = parsed.customCategoryName,
                draftCustomCategoryIcon = parsed.customCategoryIcon,
                draftCustomCategoryColor = parsed.customCategoryColor,
                draftAccount = parsed.account,
                draftMerchantOrNote = parsed.merchant,
                rawSmsText = parsed.rawText,
            ),
        ) ?: 0L

        postSmsPushNotification(context, parsed, title, message, dbId)
        return dbId
    }

    fun showSmsExpenseDetected(
        context: Context,
        parsed: ParsedSmsTransaction,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            saveAndShowSmsDraft(context, parsed)
        }
    }

    private fun postSmsPushNotification(
        context: Context,
        parsed: ParsedSmsTransaction,
        title: String,
        message: String,
        appNotificationId: Long,
    ) {
        createChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationId = 5000 + ((parsed.merchant.hashCode() xor parsed.amount.hashCode()) and 0x7FFF)

        // Action: Confirm (Add transaction directly)
        val confirmIntent = Intent(context, SmsExpenseActionReceiver::class.java).apply {
            action = SmsExpenseActionReceiver.ACTION_CONFIRM
            putExtra(SmsExpenseActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SmsExpenseActionReceiver.EXTRA_APP_NOTIFICATION_ID, appNotificationId)
            putExtra(SmsExpenseActionReceiver.EXTRA_AMOUNT, parsed.amount)
            putExtra(SmsExpenseActionReceiver.EXTRA_MERCHANT, parsed.merchant)
            putExtra(SmsExpenseActionReceiver.EXTRA_CATEGORY, parsed.category.name)
            putExtra(SmsExpenseActionReceiver.EXTRA_CUSTOM_CATEGORY_NAME, parsed.customCategoryName)
            putExtra(SmsExpenseActionReceiver.EXTRA_CUSTOM_CATEGORY_ICON, parsed.customCategoryIcon)
            putExtra(SmsExpenseActionReceiver.EXTRA_CUSTOM_CATEGORY_COLOR, parsed.customCategoryColor)
            putExtra(SmsExpenseActionReceiver.EXTRA_TYPE, parsed.type.name)
            putExtra(SmsExpenseActionReceiver.EXTRA_ACCOUNT, parsed.account.name)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action: Dismiss
        val dismissIntent = Intent(context, SmsExpenseActionReceiver::class.java).apply {
            action = SmsExpenseActionReceiver.ACTION_DISMISS
            putExtra(SmsExpenseActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SmsExpenseActionReceiver.EXTRA_APP_NOTIFICATION_ID, appNotificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Content click: open app
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 3,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SMS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "✅ Tasdiqlash", confirmPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Keyinroq", dismissPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    fun showBudgetAlert(
        context: Context,
        categoryName: String,
        percent: Int,
        spent: Long,
        limit: Long,
    ) {
        val title = if (percent >= 100) {
            "⚠️ Byudjet limiti tugadi! ($categoryName)"
        } else {
            "⚡ Byudjet ogohlantirishi: $percent% ($categoryName)"
        }

        val message = if (percent >= 100) {
            "$categoryName uchun belgilangan limit toʻliq sarflandi: ${formatMoneyText(spent)} / ${formatMoneyText(limit)}"
        } else {
            "$categoryName boʻyicha limitning $percent% qismi sarflandi (${formatMoneyText(spent)} / ${formatMoneyText(limit)})"
        }

        val app = context.applicationContext as? FinanceApp
        if (app != null) {
            CoroutineScope(Dispatchers.IO).launch {
                app.container.notificationRepository.insert(
                    AppNotificationEntity(
                        type = AppNotificationType.BUDGET_ALERT,
                        title = title,
                        message = message,
                        draftAmount = spent,
                    ),
                )
            }
        }

        createChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2000 + categoryName.hashCode(), notification)
        } catch (_: SecurityException) {}
    }

    fun showDebtReminder(
        context: Context,
        personName: String,
        amount: Long,
        isLent: Boolean,
    ) {
        val title = if (isLent) "🤝 Qarzni qaytarib olish eslatmasi" else "💳 Qarzni toʻlash eslatmasi"
        val message = if (isLent) {
            "$personName sizga ${formatMoneyText(amount)} qarzini qaytarish muddati keldi"
        } else {
            "$personName ga ${formatMoneyText(amount)} qarzni qaytarish muddati keldi"
        }

        val app = context.applicationContext as? FinanceApp
        if (app != null) {
            CoroutineScope(Dispatchers.IO).launch {
                app.container.notificationRepository.insert(
                    AppNotificationEntity(
                        type = AppNotificationType.DEBT_REMINDER,
                        title = title,
                        message = message,
                        draftAmount = amount,
                        draftMerchantOrNote = personName,
                    ),
                )
            }
        }

        createChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(3000 + personName.hashCode(), notification)
        } catch (_: SecurityException) {}
    }

    fun showSubscriptionReminder(
        context: Context,
        name: String,
        amount: Long,
    ) {
        val title = "📅 Doimiy toʻlov kuni: $name"
        val message = "Bugun $name uchun toʻlov sanasi (${formatMoneyText(amount)})"

        val app = context.applicationContext as? FinanceApp
        if (app != null) {
            CoroutineScope(Dispatchers.IO).launch {
                app.container.notificationRepository.insert(
                    AppNotificationEntity(
                        type = AppNotificationType.SUBSCRIPTION,
                        title = title,
                        message = message,
                        draftAmount = amount,
                        draftMerchantOrNote = name,
                    ),
                )
            }
        }

        createChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            103,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(4000 + name.hashCode(), notification)
        } catch (_: SecurityException) {}
    }

    fun showEmergencyFundReminder(
        context: Context,
        incomeAmount: Long,
        suggestedDeposit: Long,
        percent: Int,
    ) {
        val title = "🛡️ Moliyaviy yostiqcha eslatmasi"
        val message = "Yangi ${formatMoneyText(incomeAmount)} daromaddan ${percent}% (${formatMoneyText(suggestedDeposit)}) ni xavfsizlik jamgʻarmasiga qoʻshib qoʻyishni unutmang!"

        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as? FinanceApp
            val dbId = app?.container?.notificationRepository?.insert(
                AppNotificationEntity(
                    type = AppNotificationType.EMERGENCY_FUND,
                    title = title,
                    message = message,
                    draftAmount = suggestedDeposit,
                ),
            ) ?: 0L

            createChannels(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return@launch
            }

            val notificationId = 6000 + ((incomeAmount / 1000) % 1000).toInt()

            val addIntent = Intent(context, com.example.myapplication.savings.EmergencyFundActionReceiver::class.java).apply {
                action = com.example.myapplication.savings.EmergencyFundActionReceiver.ACTION_ADD_EMERGENCY
                putExtra(com.example.myapplication.savings.EmergencyFundActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(com.example.myapplication.savings.EmergencyFundActionReceiver.EXTRA_APP_NOTIFICATION_ID, dbId)
                putExtra(com.example.myapplication.savings.EmergencyFundActionReceiver.EXTRA_AMOUNT, suggestedDeposit)
            }
            val addPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 1,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val openIntent = Intent(context, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(
                context,
                notificationId * 10 + 2,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_launcher_foreground, "✅ Yostiqchaga qoʻshish", addPendingIntent)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (_: SecurityException) {}
        }
    }
}
