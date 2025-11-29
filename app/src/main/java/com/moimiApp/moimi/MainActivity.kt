package com.moimiApp.moimi

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val myLocationMarker = TMapMarkerItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐ [추가됨] 로그인 상태 확인 로직
        // 저장된 토큰이 없으면 로그인 화면으로 이동하고, 메인 화면은 종료(finish)합니다.
        if (prefsManager.getToken() == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // 뒤로가기 눌러도 메인으로 못 오게 종료
            return
        }

        setContentView(R.layout.activity_main)
        setupDrawer()
        checkPermissionAndStartService()

        // 알림 가져오기
        fetchNotifications()

        // 지도 초기화
        val mapContainer = findViewById<ViewGroup>(R.id.map_container)

        // TMAP 초기화 시 예외 처리 추가 (지도 백지화 방지 안전장치)
        try {
            tMapView = TMapView(this)
            mapContainer.addView(tMapView)
            tMapView.setSKTMapApiKey(tMapApiKey)

            tMapView.setOnMapReadyListener {
                tMapView.zoomLevel = 15
                startTrackingMyLocation()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "TMAP 초기화 오류: ${e.message}")
        }
    }

    private fun startTrackingMyLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        // TMAP이 준비된 상태인지 확인 후 사용
                        if (::tMapView.isInitialized) {
                            tMapView.setCenterPoint(location.longitude, location.latitude)

                            myLocationMarker.id = "my_location"
                            myLocationMarker.setTMapPoint(TMapPoint(location.latitude, location.longitude))
                            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.profile) // 프로필 이미지 사용
                            myLocationMarker.icon = bitmap
                            myLocationMarker.setPosition(0.5f, 0.5f)

                            tMapView.addTMapMarkerItem(myLocationMarker)
                        }
                    }
                }
            }, Looper.getMainLooper())
        }
    }

    private fun checkPermissionAndStartService() {
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val denied = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (denied.isEmpty()) {
            startLocationService()
        } else {
            ActivityCompat.requestPermissions(this, denied.toTypedArray(), 1001)
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        }
    }

    private fun fetchNotifications() {
        val tvNoti1 = findViewById<TextView>(R.id.tv_noti_1)
        val tvNoti2 = findViewById<TextView>(R.id.tv_noti_2)
        val token = getAuthToken() // BaseActivity의 함수 사용

        RetrofitClient.notificationInstance.getNotifications(token)
            .enqueue(object : Callback<NotificationResponse> {
                override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val notiList = response.body()!!.notifications
                        if (notiList.isNotEmpty()) tvNoti1.text = notiList[0].message
                        else tvNoti1.text = "새로운 알림이 없습니다."

                        if (notiList.size >= 2) tvNoti2.text = notiList[1].message
                        else tvNoti2.text = ""
                    } else {
                        tvNoti1.text = "알림 로드 실패"
                    }
                }
                override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                    tvNoti1.text = "서버 연결 실패"
                    Log.e("MAIN", "Noti Error: ${t.message}")
                }
            })
    }
}