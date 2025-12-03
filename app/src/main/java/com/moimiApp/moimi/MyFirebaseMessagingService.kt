package com.moimiApp.moimi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 서버에서 보낸 알림 내용 추출
        val title = remoteMessage.notification?.title ?: "알림"
        val body = remoteMessage.notification?.body ?: "새로운 메시지가 도착했습니다."

        // 1. [상단 바] 시스템 알림 띄우기
        showNotification(title, body)

        // 2. [앱 내] 메인 화면에 "빨간 점 켜라"고 신호 보내기 (앱이 켜져 있을 때)
        val intent = Intent("com.moimiApp.moimi.NEW_NOTIFICATION")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 토큰이 갱신되면 저장소에 저장 (나중에 로그인 시 서버로 전송됨)
        SharedPreferencesManager(applicationContext).saveFcmToken(token)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "moimi_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "모이미 알림", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 클릭 시 이동할 화면 (MainActivity)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // 아이콘 설정
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}