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
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치가 업데이트될 때마다 실행될 동작 정의
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("LocationService", "내 위치: ${location.latitude}, ${location.longitude}")

                    // ⭐ [핵심] 서버로 내 위치 전송
                    sendLocationToServer(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. 백그라운드에서 죽지 않도록 알림(Notification) 띄우기
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "LocationServiceChannel")
            .setContentTitle("모이미 위치 서비스 실행 중")
            .setContentText("실시간으로 위치를 공유하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher) // 아이콘이 없으면 앱 런처 아이콘 사용
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // ID는 0이 아닌 정수여야 함
        startForeground(1234, notification)

        // 2. 위치 업데이트 요청 시작
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY // 서비스가 강제 종료되어도 다시 살아나게 함
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        // 토큰 가져오기 (로그인 때 저장한 토큰 사용)
        // 실제로는 SharedPreferences 등에서 꺼내와야 함. 여기서는 예시로 고정값 혹은 인텐트로 전달받는 구조가 필요.
        // 일단 "Bearer 토큰값" 형태로 넣어야 함.
        val token = "Bearer " + getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("jwt_token", "")

        val request = LocationRequest(lat, lon)
        RetrofitClient.instance.updateLocation(token, request).enqueue(object : Callback<LocationResponse> {
            override fun onResponse(call: Call<LocationResponse>, response: Response<LocationResponse>) {
                if (response.isSuccessful) {
                    Log.d("LocationService", "서버 전송 성공: ${response.body()?.message}")
                } else {
                    Log.e("LocationService", "서버 전송 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LocationResponse>, t: Throwable) {
                Log.e("LocationService", "통신 에러: ${t.message}")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 서비스 종료 시 위치 업데이트 중단
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // 알림 채널 생성 (안드로이드 8.0 이상 필수)
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