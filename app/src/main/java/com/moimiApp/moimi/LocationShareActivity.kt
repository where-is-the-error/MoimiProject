package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.skt.tmap.TMapView

class LocationShareActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share)

        // 1. 메뉴 활성화
        setupDrawer()

        // 2. 지도 초기화
        initTMap()

        // 3. 스위치 기능 연결
        val switchShare = findViewById<SwitchCompat>(R.id.switch_share)

        // (선택) 앱 켤 때 이미 서비스가 돌고 있다면 스위치를 켜둔 상태로 보여주기
        // (복잡하면 일단 기본 false로 둡니다)

        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 스위치 ON -> 위치 공유 서비스 시작! (서버 전송 시작)
                startLocationService()
                Toast.makeText(this, "위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 스위치 OFF -> 서비스 종료 (서버 전송 중단)
                stopLocationService()
                Toast.makeText(this, "위치 공유를 종료합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initTMap() {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_share)
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        // 지도 설정 (줌, 로고 등)
        tMapView.zoomLevel = 15
        // 필요 시 중심점 이동 코드 추가
        tMapView.setCenterPoint(126.9780, 37.5665)
    }

    // 서비스 켜기
    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // 서비스 끄기
    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }
}