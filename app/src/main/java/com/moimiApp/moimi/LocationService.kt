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
    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefsManager = SharedPreferencesManager(this) // 매니저 초기화

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    sendLocationToServer(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // [추가] 위치 공유 상태 ON
        prefsManager.setLocationSharing(true)

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "LocationServiceChannel")
            .setContentTitle("모이미 위치 서비스")
            .setContentText("실시간 위치를 공유하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1234, notification)

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        val token = prefsManager.getToken()

        if (token != null) {
            val bearerToken = "Bearer $token"
            val request = com.moimiApp.moimi.LocationRequest(lat, lon)

            RetrofitClient.instance.updateLocation(bearerToken, request).enqueue(object : Callback<LocationResponse> {
                override fun onResponse(call: Call<LocationResponse>, response: Response<LocationResponse>) {
                    // 성공 로그
                }
                override fun onFailure(call: Call<LocationResponse>, t: Throwable) {
                    Log.e("LocationService", "통신 에러: ${t.message}")
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // [추가] 위치 공유 상태 OFF
        prefsManager.setLocationSharing(false)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

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