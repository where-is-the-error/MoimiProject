package com.moimiApp.moimi

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Looper
import android.view.ViewGroup
import android.util.Log // Log import 추가
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skt.tmap.TMapView
import com.skt.tmap.TMapPoint
import com.skt.tmap.overlay.TMapMarkerItem
import retrofit2.Call // Retrofit Import
import retrofit2.Callback // Retrofit Import
import retrofit2.Response // Retrofit Import

class MainActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val myLocationMarker = TMapMarkerItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupDrawer()
        checkPermissionAndStartService()

        // ⭐ [필수 추가] 5. 서버에서 알림 가져오기 (가장 마지막에 실행)
        fetchNotifications()
    }

        val mapContainer = findViewById<ViewGroup>(R.id.map_container)

        tMapView = TMapView(this)
        mapContainer.addView(tMapView) // 뷰 먼저 추가
        tMapView.setSKTMapApiKey(tMapApiKey)

        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 15
            startTrackingMyLocation()
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
                        tMapView.setCenterPoint(location.longitude, location.latitude)

                        myLocationMarker.id = "my_location"
                        myLocationMarker.setTMapPoint(TMapPoint(location.latitude, location.longitude))
                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.profile) // 프로필 이미지 마커
                        myLocationMarker.icon = bitmap
                        myLocationMarker.setPosition(0.5f, 0.5f)

                        tMapView.addTMapMarkerItem(myLocationMarker)
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

    // ----------------------------------------------------------------
    // [추가된 기능] 서버에서 알림 목록을 가져와서 화면에 표시하는 함수
    // ----------------------------------------------------------------
    private fun fetchNotifications() {
        // XML에 있는 알림 텍스트뷰 ID를 찾아 연결
        val tvNoti1 = findViewById<TextView>(R.id.tv_noti_1)
        val tvNoti2 = findViewById<TextView>(R.id.tv_noti_2)

        // BaseActivity의 getAuthToken() 함수를 사용하여 토큰을 가져옵니다.
        val token = getAuthToken()

        RetrofitClient.notificationInstance.getNotifications(token)
            .enqueue(object : Callback<NotificationResponse> {
                override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val notiList = response.body()!!.notifications

                        // 첫 번째 알림 설정
                        if (notiList.isNotEmpty()) {
                            tvNoti1.text = notiList[0].message
                        } else {
                            tvNoti1.text = "새로운 알림이 없습니다."
                        }

                        // 두 번째 알림 설정
                        if (notiList.size >= 2) {
                            tvNoti2.text = notiList[1].message
                        } else {
                            tvNoti2.text = "" // 두 번째 알림 없으면 비움
                        }
                    } else {
                        tvNoti1.text = "알림 로드 실패 (인증 오류)"
                    }
                }

                override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                    tvNoti1.text = "알림 서버 연결 실패"
                    Log.e("MAIN", "Noti Error: ${t.message}")
                }
            })
    }
}