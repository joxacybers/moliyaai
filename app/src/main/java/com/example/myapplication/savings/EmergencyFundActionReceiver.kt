package com.example.myapplication.savings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.FinanceApp
import com.example.myapplication.ui.common.formatAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmergencyFundActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        if (intent.action == ACTION_ADD_EMERGENCY) {
            val amount = intent.getLongExtra(EXTRA_AMOUNT, 0L)
            if (amount <= 0) return

            val app = context.applicationContext as? FinanceApp ?: return
            val container = app.container

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    container.savingsRepository.addEmergencyFunds(amount)
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "🛡️ ${formatAmount(amount)} soʻm moliyaviy yostiqchaga qoʻshildi",
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
        const val ACTION_ADD_EMERGENCY = "com.example.myapplication.savings.ACTION_ADD_EMERGENCY"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_AMOUNT = "extra_amount"
    }
}
