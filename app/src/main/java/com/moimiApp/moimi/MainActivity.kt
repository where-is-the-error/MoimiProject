package com.moimiApp.moimi

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skt.tmap.TMapView

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
    }

    private fun initMiniMap() {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_main)

        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        // ✅ [수정됨] 카메라 팩토리 없이, 가장 단순한 방법으로 이동
        tMapView.setOnMapReadyListener {
            // 1. 줌 레벨 설정 (숫자만 넣으세요)
            tMapView.zoomLevel = 15

            // 2. 중심점 이동 (객체 말고, 위도/경도 숫자 2개를 순서대로 넣으세요)
            // 순서 주의: (경도 Longitude, 위도 Latitude) 순서일 수 있습니다.
            // 보통 T Map은 setCenterPoint(경도, 위도) 입니다.
            tMapView.setCenterPoint(126.9780, 37.5665)
        }
    }

    private fun updateDummyUI() {
        val tvWeather = findViewById<TextView>(R.id.tv_weather_info)
        tvWeather.text = "24°C 맑음"

        val tvTransport = findViewById<TextView>(R.id.tv_transport_info)
        tvTransport.text = "강남역까지 택시"

        // 혹시 XML에 tv_transport_time ID가 있다면 주석 해제
        // val tvTime = findViewById<TextView>(R.id.tv_transport_time)
        // tvTime.text = "약 25분 소요"
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
}