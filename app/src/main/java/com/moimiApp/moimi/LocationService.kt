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

    private var meetingId: String = ""

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefsManager = SharedPreferencesManager(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    sendLocationViaSocket(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        meetingId = intent?.getStringExtra("meetingId") ?: ""
        prefsManager.setLocationSharing(true)

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "LocationServiceChannel")
            .setContentTitle("위치 공유 중")
            .setContentText("모임 멤버들에게 실시간 위치를 전송하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1234, notification)

        // ✅ [확인] 5초마다, 10미터 이상 이동 시 업데이트
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
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
        if (meetingId.isNotEmpty() && SocketHandler.getSocket().connected()) {
            try {
                val json = JSONObject()
                json.put("roomId", meetingId)
                json.put("latitude", lat)
                json.put("longitude", lon)
                json.put("userId", prefsManager.getUserId())
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