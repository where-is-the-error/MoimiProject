package com.moimiApp.moimi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "일정 알림"
        val message = intent.getStringExtra("message") ?: "예정된 일정이 있습니다."
        val scheduleId = intent.getStringExtra("scheduleId") ?: ""

        showNotification(context, title, message, scheduleId)
    }

    private fun showNotification(context: Context, title: String, message: String, scheduleId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "schedule_notification_channel"

        // 오레오 버전 이상을 위한 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "일정 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "일정 미리 알림 채널입니다."
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        // 알림 클릭 시 이동할 화면 (MainActivity 또는 상세 화면)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(), // 고유 ID 사용
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // 앱 아이콘
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 알림 표시 (ID를 다르게 주어 여러 알림이 쌓이게 함)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}