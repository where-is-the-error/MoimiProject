package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.skt.tmap.TMapView

class LocationShareActivity : BaseActivity() { // BaseActivity 상속

    private lateinit var tMapView: TMapView
    private val tMapKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share)

        // 1. 메뉴 활성화
        setupDrawer()

        // 2. 지도 초기화
        initTMap()

        // 3. 위치 공유 스위치 기능 연결
        val switchShare = findViewById<SwitchCompat>(R.id.switch_share)
        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startLocationService()
                Toast.makeText(this, "위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show()
            } else {
                stopLocationService()
                Toast.makeText(this, "위치 공유를 종료합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. 초대 링크 버튼 연결
        val btnInvite = findViewById<Button>(R.id.btn_invite_link)
        btnInvite.setOnClickListener {
            shareInviteLink()
        }
    }

    private fun initTMap() {
        // 🟢 [중요] 이제 XML에 이 ID가 있으므로 오류가 나지 않습니다.
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_share)

        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 15
            tMapView.setCenterPoint(126.9780, 37.5665)
        }
    }

    // 초대 링크 공유 함수
    private fun shareInviteLink() {
        val inviteText = """
            [모이미] 위치 공유방에 초대합니다!
            같이 위치 확인하고 만나요 📍
            
            참여 코드: 123456 (임시)
            앱 링크: http://moimi.app/invite/123456
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        startActivity(Intent.createChooser(intent, "친구에게 초대 링크 보내기"))
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }
}