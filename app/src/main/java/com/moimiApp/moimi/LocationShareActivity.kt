package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.skt.tmap.TMapView

class LocationShareActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    // ⚠️ [중요] 실제 T Map Key가 맞는지 확인하세요
    private val tMapKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"

    // 넘어온 모임 ID 저장용 변수
    private var currentMeetingId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share)

        setupDrawer()

        // 1. 이전 화면에서 넘겨준 모임 정보 받기
        currentMeetingId = intent.getStringExtra("meetingId") ?: ""
        val meetingTitle = intent.getStringExtra("meetingTitle") ?: "위치 공유"

        // 2. 화면 제목을 모임 이름으로 변경
        val tvTitle = findViewById<TextView>(R.id.tv_share_label)
        tvTitle.text = meetingTitle

        // 3. 지도 초기화
        initTMap()

        // 4. 위치 공유 스위치 기능 연결
        val switchShare = findViewById<SwitchCompat>(R.id.switch_share)
        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startLocationService()
                Toast.makeText(this, "'$meetingTitle' 위치 공유 시작", Toast.LENGTH_SHORT).show()
                // TODO: currentMeetingId를 이용해 서버에 내 위치 전송 시작
            } else {
                stopLocationService()
                Toast.makeText(this, "위치 공유 종료", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. 초대 링크 버튼 연결
        val btnInvite = findViewById<Button>(R.id.btn_invite_link)
        btnInvite.setOnClickListener {
            shareInviteLink(meetingTitle) // 제목을 같이 공유
        }
    }

    private fun shareInviteLink(title: String) {
        val inviteText = """
            [모이미] '$title' 위치 공유방에 초대합니다!
            같이 위치 확인하고 만나요 📍
            
            참여 코드: $currentMeetingId
            앱 링크: http://moimi.app/invite/$currentMeetingId
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        startActivity(Intent.createChooser(intent, "친구에게 초대 링크 보내기"))
    }

    private fun initTMap() {
        // XML ID 확인 (map_container_share)
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_share)

        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 15
            tMapView.setCenterPoint(126.9780, 37.5665)
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        // 필요한 경우 서비스에 방 ID 전달
        // intent.putExtra("meetingId", currentMeetingId)
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