package com.moimiApp.moimi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // 서버로 위치 전송
                    sendLocationToServer(location.latitude, location.longitude)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            // ✅ [최신 API 적용] LocationRequest Builder 사용 (경로 명시)
            // 10000L = 10초마다 위치 업데이트
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(10000L)
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("LOC", "위치 권한이 없어 업데이트를 시작할 수 없습니다.")
        }
    }

    private fun sendLocationToServer(lat: Double, lng: Double) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)

        if (token == null) {
            Log.w("LOCATION", "JWT Token not found. Skipping location update.")
            return
        }

        // ✅ [토큰 인증] Authorization 헤더 생성 (Bearer 방식)
        val authHeader = "Bearer $token"
        val request = LocationRequest(lat, lng) // DTO 객체 생성

        // ✅ Retrofit 호출 (Authorization 헤더를 인자로 넘김)
        RetrofitClient.instance.updateLocation(authHeader, request).enqueue(object : Callback<LocationResponse> {
            override fun onResponse(call: Call<LocationResponse>, response: Response<LocationResponse>) {
                if(response.isSuccessful) {
                    Log.d("LOC", "서버 전송 성공: $lat, $lng")
                } else {
                    Log.w("LOC", "전송 실패 (인증/서버 오류): ${response.code()}")
                }
            }
            override fun onFailure(call: Call<LocationResponse>, t: Throwable) {
                Log.e("LOC", "전송 통신 오류", t)
            }
        })
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel("LocCh", "위치 공유 알림", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "LocCh")
            .setContentTitle("모이미 실행 중")
            .setContentText("실시간 위치를 서버와 공유하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher) // 아이콘 변경 가능
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}