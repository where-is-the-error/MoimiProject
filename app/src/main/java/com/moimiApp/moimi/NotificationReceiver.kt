
package com.moimiApp.moimi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("msg") ?: "일정 알림"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}