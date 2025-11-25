package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Toast
import android.widget.LinearLayout
import com.skt.Tmap.TMapView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.skt.Tmap.TMapGpsManager // T Map의 위치 관리자
import com.skt.Tmap.TMapPoint // T Map의 좌표 클래스

// AppCompatActivity와 OnMapReadyCallback 제거, BaseActivity만 상속
class MapDetailActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = "여기에_발급받은_TMap_AppKey_입력" // ⚠️ 여기에 실제 T Map Key를 입력하세요

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_detail)

        // [BaseActivity 기능] 상단바, 햄버거 메뉴 연결
        setupDrawer()

        // ----------------------------------------------------
        // [T Map 지도 초기화]
        // ----------------------------------------------------

        // 1. TMapView 객체 생성 및 설정
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)

        // 2. XML 컨테이너에 TMapView 추가
        // (XML에서 map_fragment ID가 map_container로 변경되었다고 가정합니다.)
        val mapContainer = findViewById<LinearLayout>(R.id.map_container)
        mapContainer.addView(tMapView)

        // 3. 지도 기능 설정 (권한 요청 및 위치 추적 시작)
        setupTMapLocation()
    }

    // 지도 준비 및 위치 추적 설정 함수 (Naver Map의 onMapReady 대체)
    private fun setupTMapLocation() {

        // T Map UI 설정 (줌 레벨 등)
        tMapView.zoomLevel = 15

        // 권한 확인 및 요청
        if (checkLocationPermission()) {
            startLocationTracking()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        // ACCESS_FINE_LOCATION 권한이 있는지 확인
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        // 권한 요청 다이얼로그 띄우기
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationTracking() {
        // T Map의 내장 위치 추적 기능 활성화
        tMapView.setTrackingMode(true) // 지도 중심이 사용자의 위치를 따라가도록 설정
        tMapView.setSightVisible(true) // 현재 위치 마커(파란 점) 보이게 설정

        Toast.makeText(this, "T Map 위치 추적 시작", Toast.LENGTH_SHORT).show()
    }

    // 네이버 맵 FusedLocationSource 대신 Android 표준 권한 요청 결과를 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용됨 -> 위치 추적 시작
                startLocationTracking()
            } else {
                // 권한 거부됨 -> 사용자에게 알림 및 추적 비활성화
                Toast.makeText(this, "위치 권한이 거부되어 현위치 추적이 제한됩니다.", Toast.LENGTH_LONG).show()
                tMapView.setTrackingMode(false)
            }
        }
    }
}