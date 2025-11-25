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
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
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
            // ✅ 최신 API: 경로(FQN)를 명확히 적어서 오류 방지
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(10000L)
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) { Log.e("LOC", "권한 없음") }
    }

    private fun sendLocationToServer(lat: Double, lng: Double) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)

        if (token == null) return

        val authHeader = "Bearer $token"
        val request = LocationRequest(lat, lng)

        RetrofitClient.instance.updateLocation(authHeader, request).enqueue(object : Callback<LocationResponse> {
            override fun onResponse(call: Call<LocationResponse>, response: Response<LocationResponse>) {}
            override fun onFailure(call: Call<LocationResponse>, t: Throwable) {}
        })
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("LocCh", "위치", NotificationManager.IMPORTANCE_LOW))
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "LocCh")
            .setContentTitle("모이미")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}