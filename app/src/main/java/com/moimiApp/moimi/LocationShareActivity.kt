package com.moimiApp.moimi

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import io.socket.client.Socket
import org.json.JSONObject

class LocationShareActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = Constants.TMAP_API_KEY

    // 소켓 및 데이터
    private lateinit var mSocket: Socket
    private var currentMeetingId: String = ""
    private var myUserId: String = ""
    private var myUserName: String = ""

    // 마커 관리용 맵 (UserID -> Marker)
    private val userMarkers = HashMap<String, TMapMarkerItem>()

    // 리스트 어댑터
    private val userList = mutableListOf<LocationUser>()
    private lateinit var userAdapter: LocationUserAdapter

    // 마커 아이콘 비트맵 (미리 로딩)
    private var markerBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share)

        setupDrawer() // BaseActivity 기능

        // 0. 내 정보 가져오기 (SharedPreferences)
        myUserId = prefsManager.getUserId() ?: ""
        myUserName = prefsManager.getUserName() ?: "나"

        // 1. 데이터 수신
        currentMeetingId = intent.getStringExtra("meetingId") ?: ""
        val meetingTitle = intent.getStringExtra("meetingTitle") ?: "위치 공유"

        findViewById<TextView>(R.id.tv_share_label).text = meetingTitle

        // 2. 리소스 초기화
        markerBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_location) // 마커 이미지
        initRecyclerView()
        initTMap()
        initSocket() // 소켓 연결 시작

        // 3. 스위치 리스너 (내 위치 전송 ON/OFF)
        val switchShare = findViewById<SwitchCompat>(R.id.switch_share)
        // 기존 상태 반영
        switchShare.isChecked = isServiceRunning(LocationService::class.java)

        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startLocationService()
                Toast.makeText(this, "내 위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show()
            } else {
                stopLocationService()
                Toast.makeText(this, "내 위치 공유를 끕니다.", Toast.LENGTH_SHORT).show()
                // (선택) 지도에서 내 마커 제거 로직 추가 가능
            }
        }

        // 4. 초대 링크 공유
        findViewById<Button>(R.id.btn_invite_link).setOnClickListener {
            shareInviteLink(meetingTitle)
        }
    }

    private fun initRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_location_users)
        rv.layoutManager = LinearLayoutManager(this)
        userAdapter = LocationUserAdapter(userList)
        rv.adapter = userAdapter
    }

    private fun initTMap() {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_share)
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 15
            tMapView.setCenterPoint(126.9780, 37.5665) // 기본 서울시청 (GPS 잡히면 이동됨)
        }
    }

    // ⭐ [핵심] 소켓 연결 및 이벤트 수신
    private fun initSocket() {
        SocketHandler.setSocket()
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()

        // 1. 방 입장 (서버에 joinRoom 이벤트 전송)
        val joinData = JSONObject()
        joinData.put("roomId", currentMeetingId)
        joinData.put("userId", myUserId)
        mSocket.emit("joinRoom", joinData)

        // 2. 다른 사람 위치 수신 (locationUpdate 이벤트)
        mSocket.on("locationUpdate") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val userId = data.optString("userId")
                val lat = data.optDouble("latitude")
                val lon = data.optDouble("longitude")
                val userName = data.optString("userName", "익명")

                // UI 업데이트는 반드시 MainThread에서!
                runOnUiThread {
                    updateUserLocationOnMap(userId, userName, lat, lon)
                }
            }
        }

        // 3. (선택) 방에 누군가 입장/퇴장했을 때 이벤트 처리 가능
        // mSocket.on("userJoined") { ... }
    }

    // ⭐ 지도에 마커 찍기/이동하기
    private fun updateUserLocationOnMap(userId: String, userName: String, lat: Double, lon: Double) {
        // 나 자신이면 지도 중심만 이동 (선택 사항)
        if (userId == myUserId) {
            // tMapView.setCenterPoint(lon, lat, true) // 내 위치로 계속 이동하면 불편할 수 있음
            return
        }

        // 리스트 업데이트
        userAdapter.updateUser(userId, userName)

        // 마커 업데이트
        val point = TMapPoint(lat, lon)

        if (userMarkers.containsKey(userId)) {
            // 이미 있는 마커면 위치만 이동
            val marker = userMarkers[userId]
            marker?.tMapPoint = point
            // TMap은 마커 속성 변경 후 다시 add할 필요 없음 (자동 갱신되거나 refresh 필요할 수 있음)
            // tMapView.addTMapMarkerItem(marker) // 혹시 갱신 안되면 주석 해제
        } else {
            // 새로운 마커 생성
            val marker = TMapMarkerItem()
            marker.id = userId
            marker.icon = markerBitmap
            marker.setPosition(0.5f, 1.0f) // 하단 중앙이 좌표
            marker.tMapPoint = point
            marker.name = userName
            marker.canShowCallout = true // 말풍선 보이기
            marker.calloutTitle = userName // 말풍선에 이름 표시

            tMapView.addTMapMarkerItem(marker)
            userMarkers[userId] = marker
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        // 서비스에도 방 ID를 넘겨줘야 서버로 전송할 때 쓸 수 있음
        intent.putExtra("meetingId", currentMeetingId)
        intent.putExtra("userId", myUserId) // 내 ID 전달

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

    private fun shareInviteLink(title: String) {
        val inviteText = """
            [모이미] '$title' 위치 공유방에 초대합니다!
            참여 코드: $currentMeetingId
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        startActivity(Intent.createChooser(intent, "초대하기"))
    }

    // 서비스 실행 중인지 확인하는 함수 (유틸리티)
    @Suppress("DEPRECATION")
    private fun <T> isServiceRunning(service: Class<T>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (serviceInfo in manager.getRunningServices(Int.MAX_VALUE)) {
            if (service.name == serviceInfo.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        // 액티비티 종료 시 소켓 방 나가기
        val leaveData = JSONObject()
        leaveData.put("roomId", currentMeetingId)
        mSocket.emit("leaveRoom", leaveData)

        // 소켓 끊기는 SocketHandler나 앱 종료 시점에 관리
        // SocketHandler.closeConnection()

        // ⚠️ 주의: 여기서 stopLocationService()를 호출하면
        // 백그라운드 위치 공유가 안 됨. 사용자가 스위치로 끄게 놔두는 게 좋음.
    }
}