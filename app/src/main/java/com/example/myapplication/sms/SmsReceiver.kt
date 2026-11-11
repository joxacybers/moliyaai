package com.example.myapplication.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.myapplication.FinanceApp
import com.example.myapplication.notification.AppNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages.firstOrNull()?.displayOriginatingAddress ?: ""
        val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }

        if (fullBody.isBlank()) return

        val app = context.applicationContext as? FinanceApp ?: return
        val container = app.container

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isEnabled = container.settingsRepository.smsAutoDetectEnabled.first()
                if (!isEnabled) return@launch

                val parsed = SmsParser.parse(sender, fullBody) ?: return@launch
                AppNotificationHelper.showSmsExpenseDetected(context, parsed)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
