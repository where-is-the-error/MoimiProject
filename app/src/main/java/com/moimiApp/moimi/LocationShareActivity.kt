package com.moimiApp.moimi

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine
import io.socket.client.Socket
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LocationShareActivity : BaseActivity() {

    private var tMapView: TMapView? = null
    private lateinit var rvUsers: RecyclerView
    private lateinit var adapter: LocationUserAdapter
    private lateinit var switchShare: SwitchCompat
    private lateinit var spinnerMeeting: Spinner
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var tvDepartureInfo: TextView? = null

    private var currentMeetingId: String = ""
    private var myUserId: String = ""
    private var mSocket: Socket? = null
    private var currentMeetingDateTime: String = ""

    private val routeColors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.DKGRAY
    )

    // 데이터 관리
    private val userMarkers = HashMap<String, TMapMarkerItem>()
    private val userColorMap = HashMap<String, Int>()
    private val userProfileMap = HashMap<String, String?>()

    // ★ [추가] 경로 순서 관리를 위해 생성된 경로들을 저장하는 맵
    private val userRouteLines = HashMap<String, TMapPolyLine>()

    private val meetingList = mutableListOf<MeetingItem>()

    private var destLat: Double = 0.0
    private var destLon: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_location_share)
            setupDrawer()

            currentMeetingId = intent.getStringExtra("meetingId") ?: ""
            myUserId = prefsManager.getUserId() ?: ""

            initViews()
            initTMap()
            initSocket()

            fetchMyMeetings()

        } catch (e: Exception) {
            Log.e("LocationShare", "초기화 오류", e)
            finish()
        }
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.btn_back_map).setOnClickListener { finish() }

        try { tvDepartureInfo = findViewById(R.id.tv_departure_info) } catch (e: Exception) {}

        val bottomSheet = findViewById<LinearLayout>(R.id.bottom_sheet_layout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val tvState = findViewById<TextView>(R.id.tv_sheet_state)
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    tvState.text = "지도로 내리기 ▼"
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    tvState.text = "목록 올리기 ▲"
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        rvUsers = findViewById(R.id.rv_location_users)
        rvUsers.layoutManager = LinearLayoutManager(this)
        adapter = LocationUserAdapter(myUserId, mutableListOf()) { targetUserId ->
            requestLocationShare(targetUserId)
        }
        rvUsers.adapter = adapter

        switchShare = findViewById(R.id.switch_share)
        spinnerMeeting = findViewById(R.id.spinner_meeting_select)

        val isSharing = prefsManager.isLocationSharing()
        switchShare.isChecked = isSharing
        updateSwitchColor(isSharing)

        switchShare.setOnClickListener {
            val isChecked = switchShare.isChecked
            updateSwitchColor(isChecked)
            toggleMyShareState(isChecked)
        }

        findViewById<FloatingActionButton>(R.id.btn_my_location).setOnClickListener {
            val myMarker = userMarkers[myUserId]
            if (myMarker != null) {
                tMapView?.setCenterPoint(myMarker.tMapPoint.longitude, myMarker.tMapPoint.latitude, true)
                tMapView?.zoomLevel = 15
            } else {
                Toast.makeText(this, "내 위치를 찾는 중입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMyMeetings() {
        val token = getAuthToken()
        RetrofitClient.instance.getMeetings(token).enqueue(object : Callback<MeetingListResponse> {
            override fun onResponse(call: Call<MeetingListResponse>, response: Response<MeetingListResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val meetings = response.body()?.meetings ?: emptyList()
                    meetingList.clear()
                    meetingList.addAll(meetings.filter { it.id.isNotEmpty() })
                    setupSpinner()
                }
            }
            override fun onFailure(call: Call<MeetingListResponse>, t: Throwable) {}
        })
    }

    private fun setupSpinner() {
        if (meetingList.isEmpty()) return

        val titles = meetingList.map { it.title }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, titles)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMeeting.adapter = spinnerAdapter

        if (currentMeetingId.isNotEmpty()) {
            val idx = meetingList.indexOfFirst { it.id == currentMeetingId }
            if (idx != -1) {
                spinnerMeeting.setSelection(idx, false)
                currentMeetingDateTime = meetingList[idx].dateTime
            }
        } else {
            currentMeetingId = meetingList[0].id
            currentMeetingDateTime = meetingList[0].dateTime
            switchMeetingRoom(currentMeetingId)
        }

        spinnerMeeting.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                try {
                    val selected = meetingList.getOrNull(position) ?: return
                    if (selected.id != currentMeetingId) {
                        currentMeetingDateTime = selected.dateTime
                        switchMeetingRoom(selected.id)
                    }
                } catch (e: Exception) {}
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun switchMeetingRoom(newId: String) {
        if (newId.isEmpty()) return

        leaveCurrentRoom()
        currentMeetingId = newId

        tMapView?.removeAllTMapMarkerItem()
        tMapView?.removeAllTMapPolyLine()
        userMarkers.clear()
        userColorMap.clear()
        userProfileMap.clear()
        userRouteLines.clear() // [수정] 경로 맵도 초기화

        destLat = 0.0
        destLon = 0.0

        tvDepartureInfo?.text = "위치 정보 및 경로를 계산 중입니다..."

        joinRoom()
        fetchMeetingDetailAndParticipants(newId)
    }

    private fun fetchMeetingDetailAndParticipants(meetingId: String) {
        val token = getAuthToken()
        RetrofitClient.instance.getMeetingDetail(token, meetingId).enqueue(object : Callback<SingleMeetingResponse> {
            override fun onResponse(call: Call<SingleMeetingResponse>, response: Response<SingleMeetingResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val meeting = response.body()?.meeting
                    val participants = meeting?.participants ?: emptyList()

                    participants.forEachIndexed { index, p ->
                        if (p.user_id != null) {
                            userColorMap[p.user_id.id] = routeColors[index % routeColors.size]
                            userProfileMap[p.user_id.id] = p.user_id.profile_img
                        }
                    }

                    val dest = meeting?.destination?.coordinates
                    if (dest != null && dest.size >= 2) {
                        val dLon = dest[0]
                        val dLat = dest[1]
                        if (dLat != 0.0 && dLon != 0.0) {
                            destLat = dLat
                            destLon = dLon
                            addDestinationMarker(dLat, dLon, meeting?.title ?: "도착지")
                        }
                    }

                    val uiList = participants.mapNotNull { p ->
                        if (p.user_id == null) return@mapNotNull null
                        LocationUserItem(
                            userId = p.user_id.id,
                            name = p.user_id.name,
                            profileImg = p.user_id.profile_img,
                            isSharing = p.isSharing
                        )
                    }
                    adapter.updateList(uiList)
                    findViewById<TextView>(R.id.tv_participant_count).text = "참여자 (${uiList.size})"

                    fetchLastLocations(meetingId)
                }
            }
            override fun onFailure(call: Call<SingleMeetingResponse>, t: Throwable) {}
        })
    }

    private fun fetchLastLocations(meetingId: String) {
        val token = getAuthToken()
        RetrofitClient.instance.getMeetingLocations(token, meetingId).enqueue(object : Callback<MeetingLocationResponse> {
            override fun onResponse(call: Call<MeetingLocationResponse>, response: Response<MeetingLocationResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val locations = response.body()?.locations ?: emptyList()
                    locations.forEach { userLoc ->
                        val coords = userLoc.location?.coordinates
                        if (coords != null && coords.size >= 2) {
                            val lon = coords[0]
                            val lat = coords[1]
                            if (lat != 0.0 && lon != 0.0) {
                                if (userLoc.profile_img != null) {
                                    userProfileMap[userLoc.userId] = userLoc.profile_img
                                }
                                runOnUiThread {
                                    // 마커 갱신
                                    userMarkers[userLoc.userId]?.let { tMapView?.removeTMapMarkerItem(it.id) }
                                    userMarkers.remove(userLoc.userId)

                                    updateMarkerAndRoute(userLoc.userId, userLoc.name, lat, lon)

                                    if (userLoc.userId == myUserId && destLat != 0.0) {
                                        calculateAndShowDepartureTime(lat, lon)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: Call<MeetingLocationResponse>, t: Throwable) {}
        })
    }

    private fun updateMarkerAndRoute(userId: String, name: String, lat: Double, lon: Double) {
        if (tMapView == null) return

        val point = TMapPoint(lat, lon)

        if (userMarkers.containsKey(userId)) {
            userMarkers[userId]?.tMapPoint = point
        } else {
            createProfileMarker(userId, name, point)
        }

        if (destLat != 0.0 && destLon != 0.0) {
            drawRoute(userId, point, TMapPoint(destLat, destLon))
        }
    }

    private fun createProfileMarker(userId: String, name: String, point: TMapPoint) {
        val profileUrl = userProfileMap[userId]

        val marker = TMapMarkerItem()
        marker.id = userId
        marker.tMapPoint = point
        marker.name = name

        marker.canShowCallout = true
        marker.calloutTitle = name
        marker.calloutSubTitle = null
        marker.setPosition(0.5f, 0.5f)

        val defaultBitmap = BitmapFactory.decodeResource(resources, R.drawable.profile)
        val resizedDefault = Bitmap.createScaledBitmap(defaultBitmap, 80, 80, true)
        marker.icon = resizedDefault

        // 일단 지도에 추가 (drawRoute에서 순서 조정함)
        tMapView?.addTMapMarkerItem(marker)
        userMarkers[userId] = marker

        if (!profileUrl.isNullOrEmpty()) {
            Glide.with(this)
                .asBitmap()
                .load(profileUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        try {
                            val scaledBitmap = Bitmap.createScaledBitmap(resource, 80, 80, true)
                            val safeBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            marker.icon = safeBitmap

                            tMapView?.removeTMapMarkerItem(userId)
                            tMapView?.addTMapMarkerItem(marker)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {}
                })
        }
    }

    // ★ [핵심 수정] 1. ID 충돌 방지 객체 생성 2. 마커가 위로 오도록 순서 제어 3. 내 경로가 최상단
    private fun drawRoute(userId: String, start: TMapPoint, end: TMapPoint) {
        val tMapData = TMapData()
        val color = userColorMap[userId] ?: Color.BLACK

        Thread {
            try {
                tMapData.findPathDataWithType(
                    TMapData.TMapPathType.CAR_PATH,
                    start,
                    end,
                    object : TMapData.OnFindPathDataWithTypeListener {
                        override fun onFindPathDataWithType(polyLine: TMapPolyLine?) {
                            if (polyLine != null) {
                                // ID 충돌 방지를 위해 userId를 ID로 하는 새 객체 생성
                                val newPolyLine = TMapPolyLine(userId, polyLine.linePointList)
                                newPolyLine.lineColor = color
                                newPolyLine.lineWidth = 15f

                                runOnUiThread {
                                    try {
                                        // 1. 계산된 경로 저장
                                        userRouteLines[userId] = newPolyLine

                                        // 2. 경로 지도에 추가
                                        tMapView?.addTMapPolyLine(newPolyLine)

                                        // ★ 순서 제어 로직 ★

                                        // [A] 내 경로를 다른 사람 경로보다 위로 올리기
                                        // 지금 그린 게 친구 경로라면, 혹시 가려졌을 내 경로를 다시 그린다.
                                        if (userId != myUserId) {
                                            val myPath = userRouteLines[myUserId]
                                            if (myPath != null) {
                                                tMapView?.removeTMapPolyLine(myUserId)
                                                tMapView?.addTMapPolyLine(myPath)
                                            }
                                        }

                                        // [B] 모든 마커(사람)를 경로보다 위로 올리기 (재등록)
                                        // TMap은 나중에 추가된 오버레이가 위에 그려짐
                                        for ((uid, marker) in userMarkers) {
                                            tMapView?.removeTMapMarkerItem(uid)
                                            tMapView?.addTMapMarkerItem(marker)
                                        }

                                    } catch (e: Exception) {
                                        Log.e("LocationShare", "경로 추가 실패", e)
                                    }
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("LocationShare", "TMap API Error", e)
            }
        }.start()
    }

    // ★ [수정] 다양한 날짜 포맷 지원으로 시간 계산 오류 해결
    private fun calculateAndShowDepartureTime(myLat: Double, myLon: Double) {
        if (tvDepartureInfo == null || destLat == 0.0 || destLon == 0.0) return

        val request = RouteRequest(
            startX = myLon, startY = myLat,
            endX = destLon, endY = destLat,
            totalValue = 2
        )

        TmapClient.instance.getRoute(Constants.TMAP_API_KEY, request).enqueue(object : Callback<TmapRouteResponse> {
            override fun onResponse(call: Call<TmapRouteResponse>, response: Response<TmapRouteResponse>) {
                val durationSeconds = response.body()?.features?.firstOrNull()?.properties?.totalTime ?: 0

                if (durationSeconds > 0 && currentMeetingDateTime.isNotEmpty()) {
                    try {
                        var meetingDate: java.util.Date? = null
                        // 서버에서 올 수 있는 다양한 포맷 시도
                        val formats = listOf(
                            "yyyy-MM-dd HH:mm:ss",
                            "yyyy-MM-dd HH:mm",
                            "yyyy-MM-dd'T'HH:mm:ss",
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
                        )

                        for (format in formats) {
                            try {
                                val sdf = SimpleDateFormat(format, Locale.getDefault())
                                meetingDate = sdf.parse(currentMeetingDateTime)
                                if (meetingDate != null) break
                            } catch (e: Exception) { continue }
                        }

                        if (meetingDate != null) {
                            val cal = Calendar.getInstance()
                            cal.time = meetingDate
                            cal.add(Calendar.MINUTE, -5)
                            cal.add(Calendar.SECOND, -durationSeconds)

                            val departureTime = SimpleDateFormat("a h:mm", Locale.getDefault()).format(cal.time)
                            val durationMin = durationSeconds / 60

                            tvDepartureInfo?.text = "소요 시간: 약 ${durationMin}분\n" +
                                    "5분 전 도착을 위해 ${departureTime}에 출발하세요!"
                        } else {
                            tvDepartureInfo?.text = "날짜 형식 오류 ($currentMeetingDateTime)"
                        }
                    } catch (e: Exception) {
                        tvDepartureInfo?.text = "시간 계산 중 오류 발생"
                    }
                }
            }
            override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) {}
        })
    }

    private fun addDestinationMarker(lat: Double, lon: Double, title: String) {
        val marker = TMapMarkerItem()
        marker.id = "destination"
        marker.tMapPoint = TMapPoint(lat, lon)
        marker.name = title
        marker.canShowCallout = true
        marker.calloutTitle = title

        try {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_location)
            val resizedDest = Bitmap.createScaledBitmap(bitmap, 80, 80, true)
            marker.icon = resizedDest
            marker.setPosition(0.5f, 1.0f)

            tMapView?.addTMapMarkerItem(marker)
            tMapView?.setCenterPoint(lon, lat, true)
        } catch (e: Exception) {}
    }

    private fun updateSwitchColor(isOn: Boolean) {
        if (isOn) {
            switchShare.trackTintList = ColorStateList.valueOf(Color.parseColor("#FFCDD2"))
            switchShare.thumbTintList = ColorStateList.valueOf(Color.parseColor("#FF8989"))
        } else {
            switchShare.trackTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
            switchShare.thumbTintList = ColorStateList.valueOf(Color.parseColor("#B0B0B0"))
        }
    }

    private fun toggleMyShareState(isSharing: Boolean) {
        val token = getAuthToken()
        if (isSharing) startLocationService() else stopLocationService()

        if (currentMeetingId.isNotEmpty()) {
            RetrofitClient.instance.toggleLocationShare(token, currentMeetingId, mapOf("isSharing" to isSharing))
                .enqueue(object : Callback<CommonResponse> {
                    override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                        if (response.isSuccessful) adapter.updateUserStatus(myUserId, isSharing)
                    }
                    override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                        switchShare.isChecked = !isSharing
                        updateSwitchColor(!isSharing)
                    }
                })
        }
    }

    private fun requestLocationShare(targetUserId: String) {
        val token = getAuthToken()
        RetrofitClient.instance.requestLocationShare(token, currentMeetingId, mapOf("targetUserId" to targetUserId))
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    if (response.isSuccessful) Toast.makeText(this@LocationShareActivity, "요청 완료", Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {}
            })
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        intent.putExtra("meetingId", currentMeetingId)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        prefsManager.setLocationSharing(true)
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        prefsManager.setLocationSharing(false)
    }

    private fun initTMap() {
        try {
            val container = findViewById<FrameLayout>(R.id.map_container_share)
            tMapView = TMapView(this)
            tMapView?.setSKTMapApiKey(Constants.TMAP_API_KEY)
            container.addView(tMapView)

            tMapView?.setOnMapReadyListener {
                tMapView?.zoomLevel = 15
                if (currentMeetingId.isNotEmpty()) fetchLastLocations(currentMeetingId)
            }
        } catch (e: Exception) { Log.e("LocationShare", "TMap Init Error", e) }
    }

    private fun initSocket() {
        try {
            SocketHandler.setSocket()
            SocketHandler.establishConnection()
            mSocket = SocketHandler.getSocket()
            if (mSocket?.connected() == true && currentMeetingId.isNotEmpty()) joinRoom()
            mSocket?.on(Socket.EVENT_CONNECT) { if (currentMeetingId.isNotEmpty()) joinRoom() }

            mSocket?.on("updateLocation") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val uid = data.optString("userId")
                    val lat = data.optDouble("latitude")
                    val lon = data.optDouble("longitude")
                    val name = data.optString("userName")
                    runOnUiThread {
                        updateMarkerAndRoute(uid, name, lat, lon)
                        if (uid == myUserId && destLat != 0.0) {
                            calculateAndShowDepartureTime(lat, lon)
                        }
                    }
                }
            }
            mSocket?.on("sharingStatusChanged") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val uid = data.optString("userId")
                    val sharing = data.optBoolean("isSharing")
                    runOnUiThread { adapter.updateUserStatus(uid, sharing) }
                }
            }
        } catch (e: Exception) {}
    }

    private fun joinRoom() {
        if (currentMeetingId.isEmpty()) return
        val data = JSONObject()
        data.put("roomId", currentMeetingId)
        data.put("userId", myUserId)
        mSocket?.emit("joinRoom", data)
    }

    private fun leaveCurrentRoom() {
        if (currentMeetingId.isNotEmpty()) {
            val data = JSONObject()
            data.put("roomId", currentMeetingId)
            mSocket?.emit("leaveRoom", data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket?.off("updateLocation")
        mSocket?.off("sharingStatusChanged")
    }
}