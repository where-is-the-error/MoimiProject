package com.moimiApp.moimi

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log // Log import 추가
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skt.tmap.TMapView
import retrofit2.Call // Retrofit Import
import retrofit2.Callback // Retrofit Import
import retrofit2.Response // Retrofit Import

class MainActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 공통 메뉴 활성화
        setupDrawer()

        // 2. 미니맵 띄우기
        initMiniMap()

        // 3. 위젯 임시 데이터 채우기
        updateDummyUI()

        // 4. 위치 권한 체크
        checkPermissionAndStartService()

        // ⭐ [필수 추가] 5. 서버에서 알림 가져오기 (가장 마지막에 실행)
        fetchNotifications()
    }

    private fun initMiniMap() {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_main)

        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        // T Map 준비 완료 리스너
        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 15
            // 중심점 이동 (경도, 위도 순서)
            tMapView.setCenterPoint(126.9780, 37.5665)
        }
    }

    private fun updateDummyUI() {
        val tvWeather = findViewById<TextView>(R.id.tv_weather_info)
        tvWeather.text = "24°C 맑음"

        val tvTransport = findViewById<TextView>(R.id.tv_transport_info)
        tvTransport.text = "강남역까지 택시"

        // (tv_transport_time 주석 해제는 그대로 유지)
    }

    private fun checkPermissionAndStartService() {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
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