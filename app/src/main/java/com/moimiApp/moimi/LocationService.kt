package com.moimiApp.moimi

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefsManager: SharedPreferencesManager

    // 현재 공유 중인 방 ID
    private var meetingId: String = ""

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefsManager = SharedPreferencesManager(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // 소켓으로 위치 전송
                    sendLocationViaSocket(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Activity에서 전달받은 방 ID 저장
        meetingId = intent?.getStringExtra("meetingId") ?: ""

        // 공유 상태 저장
        prefsManager.setLocationSharing(true)

        // 알림바 생성 (포그라운드 서비스 필수)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "LocationServiceChannel")
            .setContentTitle("위치 공유 중")
            .setContentText("모임 멤버들에게 실시간 위치를 전송하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1234, notification)

        // 위치 업데이트 요청 (5초마다)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY
    }

    private fun sendLocationViaSocket(lat: Double, lon: Double) {
        // 방 ID가 있고 소켓이 연결되어 있을 때만 전송
        if (meetingId.isNotEmpty() && SocketHandler.getSocket().connected()) {
            try {
                val json = JSONObject()
                json.put("roomId", meetingId)
                json.put("latitude", lat)
                json.put("longitude", lon)
                json.put("userId", prefsManager.getUserId())
                // 이름도 같이 보내주면 지도에 표시하기 좋음
                json.put("userName", prefsManager.getUserName() ?: "익명")

                SocketHandler.getSocket().emit("updateLocation", json)
                Log.d("LocationService", "위치 전송: $lat, $lon (Room: $meetingId)")
            } catch (e: Exception) {
                Log.e("LocationService", "소켓 전송 실패", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefsManager.setLocationSharing(false)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "LocationServiceChannel",
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}