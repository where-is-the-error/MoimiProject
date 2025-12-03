package com.moimiApp.moimi

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import io.socket.client.Socket
import org.json.JSONObject

class LocationShareActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = Constants.TMAP_API_KEY

    private lateinit var mSocket: Socket
    private var currentMeetingId: String = ""
    private var myUserId: String = ""
    private var myUserName: String = ""

    private val userMarkers = HashMap<String, TMapMarkerItem>()
    private val userList = mutableListOf<LocationUser>()
    private lateinit var userAdapter: LocationUserAdapter
    private var markerBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share)

        setupDrawer()

        myUserId = prefsManager.getUserId() ?: ""
        myUserName = prefsManager.getUserName() ?: "나"

        currentMeetingId = intent.getStringExtra("meetingId") ?: ""
        val meetingTitle = intent.getStringExtra("meetingTitle") ?: "위치 공유"

        findViewById<TextView>(R.id.tv_share_label).text = meetingTitle

        markerBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_location)
        initRecyclerView()
        initTMap()

        // ✅ 소켓 초기화 및 디버깅 로그 연결
        initSocket()

        val switchShare = findViewById<SwitchCompat>(R.id.switch_share)
        switchShare.isChecked = isServiceRunning(LocationService::class.java)

        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startLocationService()
                Toast.makeText(this, "내 위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show()
            } else {
                stopLocationService()
                Toast.makeText(this, "내 위치 공유를 끕니다.", Toast.LENGTH_SHORT).show()
            }
        }

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
            tMapView.setCenterPoint(126.9780, 37.5665)
        }
    }

    private fun initSocket() {
        SocketHandler.setSocket()
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()

        // ✅ [디버깅] 연결 상태 로그 찍기
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.e("SocketDebug", "🟢 소켓 연결 성공! ID: ${mSocket.id()}")

            // 연결되면 방 입장 시도
            val joinData = JSONObject()
            joinData.put("roomId", currentMeetingId)
            joinData.put("userId", myUserId)
            mSocket.emit("joinRoom", joinData)
            Log.e("SocketDebug", "🚪 방 입장 요청 보냄: $currentMeetingId")
        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SocketDebug", "🔴 소켓 연결 실패: ${args.firstOrNull()}")
        }

        mSocket.on(Socket.EVENT_DISCONNECT) {
            Log.e("SocketDebug", "⚪ 소켓 연결 끊김")
        }

        // 위치 업데이트 수신
        mSocket.on("locationUpdate") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val userId = data.optString("userId")
                val lat = data.optDouble("latitude")
                val lon = data.optDouble("longitude")
                val userName = data.optString("userName", "익명")

                Log.d("SocketDebug", "📍 위치 수신: $userName ($lat, $lon)")

                runOnUiThread {
                    updateUserLocationOnMap(userId, userName, lat, lon)
                }
            }
        }

        // 혹시 이미 연결된 상태라면 바로 방 입장
        if (mSocket.connected()) {
            val joinData = JSONObject()
            joinData.put("roomId", currentMeetingId)
            joinData.put("userId", myUserId)
            mSocket.emit("joinRoom", joinData)
        }
    }

    private fun updateUserLocationOnMap(userId: String, userName: String, lat: Double, lon: Double) {
        if (userId == myUserId) return

        userAdapter.updateUser(userId, userName)
        val point = TMapPoint(lat, lon)

        if (userMarkers.containsKey(userId)) {
            val marker = userMarkers[userId]
            marker?.tMapPoint = point
        } else {
            val marker = TMapMarkerItem()
            marker.id = userId
            marker.icon = markerBitmap
            marker.setPosition(0.5f, 1.0f)
            marker.tMapPoint = point
            marker.name = userName
            marker.canShowCallout = true
            marker.calloutTitle = userName

            tMapView.addTMapMarkerItem(marker)
            userMarkers[userId] = marker
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        intent.putExtra("meetingId", currentMeetingId)
        intent.putExtra("userId", myUserId)

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
        val inviteText = "[모이미] '$title' 위치 공유방 참여 코드: $currentMeetingId"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        startActivity(Intent.createChooser(intent, "초대하기"))
    }

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
        // ⚠️ [수정] Activity가 꺼져도 Service가 돌고 있으면 방을 나가면 안 됨!
        // 여기 있던 mSocket.emit("leaveRoom", ...) 코드를 삭제함.
        // 위치 공유 스위치를 끌 때만 나가게 하거나, 서비스 종료 시 처리해야 함.

        // 메모리 누수 방지를 위해 이벤트 리스너만 해제
        mSocket.off("locationUpdate")
    }
}