package com.moimiApp.moimi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine
import com.skt.tmap.poi.TMapPOIItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : BaseActivity() {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var tMapView: TMapView? = null
    private var myProfileBitmap: Bitmap? = null

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

    // 만약 위치가 아직 안 잡혔는데 경로를 그려야 할 경우를 대비한 변수
    private var pendingDestLat: Double = 0.0
    private var pendingDestLon: Double = 0.0

    // 트래킹 모드 (내 위치 따라가기)
    private var isTrackingMode = true
    private val autoTrackingHandler = Handler(Looper.getMainLooper())
    private val autoTrackingRunnable = Runnable {
        Log.d("MainActivity", "⏰ 5초 타이머 작동: 트래킹 모드 복귀")
        isTrackingMode = true
        if (currentLat != 0.0 && currentLon != 0.0) {
            // 부드럽게 이동 (animation = true)
            tMapView?.setCenterPoint(currentLon, currentLat, true)
            Toast.makeText(this@MainActivity, "내 위치 중심으로 복귀합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // UI 요소
    private lateinit var loadingOverlay: View
    private lateinit var tvLoadingPercent: TextView
    private lateinit var tvLoadingTip: TextView
    private lateinit var tvNoti3: TextView
    private lateinit var notiBadge: View
    private lateinit var tvTransportTime: TextView
    private lateinit var tvTransportInfo: TextView
    private lateinit var tvClickGuide: TextView
    private lateinit var layoutTransport: LinearLayout
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvWeatherDesc: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var rvWeekCalendar: RecyclerView
    private lateinit var tvNearestSchedule: TextView
    private lateinit var tvNearestScheduleTime: TextView

    private var weekAdapter: WeekCalendarAdapter? = null
    private var nextMeetingLocation: String? = null
    private var nextMeetingTitle: String? = null
    private var fetchedNotifications: List<NotificationItem> = emptyList()
    private var fetchedSchedules: List<ScheduleItem> = emptyList()

    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isLoadingFinished = false
    private var isWeatherFetched = false

    // 알림 수신 리시버
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (::notiBadge.isInitialized) {
                notiBadge.visibility = View.VISIBLE
            }
        }
    }

    private val tips = listOf("로딩 중...", "잠시만 기다려주세요.", "오늘의 일정은 무엇인가요?", "친구들과 약속을 잡아보세요!")
    private val PERMISSION_REQUEST_CODE = 1001

    // 📥 길찾기 화면에서 돌아왔을 때 결과 처리
    private val routeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.e("MainActivity", "📥 routeLauncher 결과 수신: resultCode=${result.resultCode}")

        if (result.resultCode == RESULT_OK) {
            val isTracking = result.data?.getBooleanExtra("isTracking", false) ?: false
            val destLat = result.data?.getDoubleExtra("destLat", 0.0) ?: 0.0
            val destLon = result.data?.getDoubleExtra("destLon", 0.0) ?: 0.0

            Log.e("MainActivity", "데이터 확인: isTracking=$isTracking, dest=$destLat, $destLon")

            if (isTracking && destLat != 0.0) {
                // 안내 중이면 -> 경로 그리기 & 트래킹 켜기
                isTrackingMode = true
                startTrackingMyLocation(forceZoom = true)

                // 내 위치가 아직 0.0이면 나중에 그리기 위해 저장
                if (currentLat == 0.0 || currentLon == 0.0) {
                    Log.w("MainActivity", "⚠️ 아직 내 위치가 0.0이라 경로 그리기를 보류합니다.")
                    pendingDestLat = destLat
                    pendingDestLon = destLon
                } else {
                    drawPolyLineToDestination(destLat, destLon)
                }
                Toast.makeText(this, "경로 안내를 계속합니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 안내 종료이면 -> 경로 지우기
                Log.e("MainActivity", "🗑️ 안내 종료됨 -> 경로 삭제")
                tMapView?.removeAllTMapPolyLine()
                pendingDestLat = 0.0
                pendingDestLon = 0.0
                Toast.makeText(this, "안내가 종료되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("MainActivity", "🔥 onCreate 실행")

        if (prefsManager.getToken() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        initViews()
        initLoadingScreen()
        setupDrawer()

        checkPermissionAndStartService()
        loadProfileMarker() // 마커용 이미지 로드

        setupWeekCalendar()
        fetchDashboardData()

        findViewById<View>(R.id.btn_notification).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        layoutTransport.setOnClickListener { moveToRouteActivity() }

        val mapContainer = findViewById<FrameLayout>(R.id.map_container)
        mapContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mapContainer.width > 0 && mapContainer.height > 0) {
                    mapContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    initTMapActual(mapContainer)
                }
            }
        })

        // 로딩이 너무 오래 걸리면 강제 종료
        handler.postDelayed({
            if (!isLoadingFinished) {
                Log.w("MainActivity", "⚠️ 로딩 타임아웃")
                completeLoading()
            }
        }, 5000)
    }

    // ✅ [신규] 두 좌표 간의 거리를 계산 (Haversine 공식 간소화)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // 지구 반경 (km)
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * acos(kotlin.math.sqrt(a))
        return R * c
    }

    // ✅ [신규] 두 TMapPoint를 포함하도록 지도를 확대/축소
    private fun zoomToSpan(start: TMapPoint, end: TMapPoint) {
        try {
            val dist = calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude)

            val zoomLevel = when {
                dist < 0.5 -> 17 // 500m 미만
                dist < 1.0 -> 16 // 1km 미만
                dist < 3.0 -> 15 // 3km 미만
                dist < 7.0 -> 13 // 7km 미만
                dist < 15.0 -> 11 // 15km 미만
                else -> 9
            }

            val centerLat = (start.latitude + end.latitude) / 2
            val centerLon = (start.longitude + end.longitude) / 2

            // 경로 그리기 후 딱 한 번만 줌 레벨을 설정하고 중앙으로 이동
            if (!isTrackingMode) {
                tMapView?.zoomLevel = zoomLevel
                tMapView?.setCenterPoint(centerLon, centerLat, true)
            }
        } catch (e: Exception) { Log.e("MainActivity", "Zoom Error", e) }
    }


    // 🗺️ 메인 화면에 경로(Polyline) 그리기
    private fun drawPolyLineToDestination(destLat: Double, destLon: Double) {
        if (currentLat == 0.0 || currentLon == 0.0) {
            Log.e("MainActivity", "❌ drawPolyLine 실패: 내 위치가 없음 (0.0)")
            return
        }

        Log.e("MainActivity", "🎨 경로 그리기 시작: ($currentLat, $currentLon) -> ($destLat, $destLon)")

        Thread {
            try {
                val tMapData = TMapData()
                val start = TMapPoint(currentLat, currentLon)
                val end = TMapPoint(destLat, destLon)

                // 자동차 경로(CAR_PATH)로 탐색
                tMapData.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, start, end, object : TMapData.OnFindPathDataWithTypeListener {
                    override fun onFindPathDataWithType(polyLine: TMapPolyLine?) {
                        if (polyLine == null) {
                            Log.e("MainActivity", "❌ TMapData: PolyLine 데이터가 null입니다.")
                            return
                        }

                        Log.e("MainActivity", "✅ 경로 데이터 수신 성공! 점 개수 : ${polyLine.linePointList.size}")

                        polyLine.lineColor = Color.BLUE
                        polyLine.lineWidth = 14f
                        runOnUiThread {
                            tMapView?.removeAllTMapPolyLine()
                            tMapView?.addTMapPolyLine(polyLine)
                            // ✅ 경로가 그려지면 지도 시점을 조정
                            zoomToSpan(start, end)
                            Log.e("MainActivity", "✅ 지도에 PolyLine 추가 완료 및 시점 조정")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Polyline 그리기 중 에러", e)
            }
        }.start()
    }

    // 👤 프로필 이미지를 비트맵으로 변환하여 마커 준비
    private fun loadProfileMarker() {
        val profileUrl = prefsManager.getUserProfileImg()

        if (profileUrl.isNullOrEmpty()) {
            val options = BitmapFactory.Options()
            myProfileBitmap = BitmapFactory.decodeResource(resources, R.drawable.profile, options)
            return
        }

        Glide.with(this)
            .asBitmap()
            .load(profileUrl)
            .circleCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // 마커 크기에 맞게 리사이징 (100x100)
                    myProfileBitmap = Bitmap.createScaledBitmap(resource, 100, 100, true)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun moveToRouteActivity() {
        val intent = Intent(this, RouteActivity::class.java)
        if (nextMeetingLocation != null) {
            intent.putExtra("destName", nextMeetingLocation)
            val formattedTitle = "$nextMeetingLocation ($nextMeetingTitle)"
            intent.putExtra("destTitle", formattedTitle)
        }
        routeLauncher.launch(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTMapActual(container: FrameLayout) {
        try {
            // 마커 이미지가 아직 로드 안 됐다면 기본 이미지 사용
            if (myProfileBitmap == null) {
                val options = BitmapFactory.Options()
                myProfileBitmap = BitmapFactory.decodeResource(resources, R.drawable.profile, options)
            }

            container.removeAllViews()
            tMapView = TMapView(this)
            tMapView?.setSKTMapApiKey(tMapApiKey)
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            container.addView(tMapView, params)

            // ⭐ 지도 터치 시 트래킹 모드 해제 & 5초 타이머 시작
            tMapView?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        isTrackingMode = false
                        autoTrackingHandler.removeCallbacks(autoTrackingRunnable)
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        autoTrackingHandler.removeCallbacks(autoTrackingRunnable)
                        autoTrackingHandler.postDelayed(autoTrackingRunnable, 5000L) // 5초 후 복귀
                    }
                }
                false
            }

            tMapView?.setOnMapReadyListener {
                Log.e("MainActivity", "TMap 준비 완료")
                tMapView?.zoomLevel = 17
                tMapView?.setCenterPoint(126.9780, 37.5665)
                startTrackingMyLocation(forceZoom = true)
                completeLoading()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "TMap Init Fail", e)
            completeLoading()
        }
    }

    private fun startTrackingMyLocation(forceZoom: Boolean = false) {
        try {
            if (locationManager == null) locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (locationListener == null) {
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        currentLat = location.latitude
                        currentLon = location.longitude

                        // ✅ 보류된 경로 그리기가 있다면 실행
                        if (pendingDestLat != 0.0 && pendingDestLon != 0.0) {
                            Log.e("MainActivity", "📍 위치 잡힘! 보류된 경로 그리기 실행")
                            drawPolyLineToDestination(pendingDestLat, pendingDestLon)
                            pendingDestLat = 0.0
                            pendingDestLon = 0.0
                        }

                        if (isFinishing || isDestroyed) return

                        // 날씨는 최초 1회
                        if (!isWeatherFetched) {
                            isWeatherFetched = true
                            fetchWeatherData(location.latitude, location.longitude)
                        }

                        runOnUiThread {
                            try {
                                if (tMapView != null) {
                                    // 트래킹 모드일 때만 지도 중심 이동
                                    if (isTrackingMode) {
                                        tMapView?.setCenterPoint(location.longitude, location.latitude, true)
                                        if (forceZoom || tMapView?.zoomLevel!! < 15) {
                                            tMapView?.zoomLevel = 17
                                        }
                                    }

                                    // 마커는 항상 최신 위치에 표시 (내 프로필 사진)
                                    if (myProfileBitmap != null) {
                                        val marker = TMapMarkerItem().apply {
                                            id = "my_location"
                                            setTMapPoint(TMapPoint(location.latitude, location.longitude))
                                            icon = myProfileBitmap
                                            setPosition(0.5f, 0.5f)
                                        }
                                        tMapView?.removeTMapMarkerItem("my_location")
                                        tMapView?.addTMapMarkerItem(marker)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "지도 업데이트 에러", e)
                            }
                        }
                    }
                    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) {}
                }
            }
            requestLocationUpdates()
        } catch (e: Exception) {
            Log.e("MainActivity", "위치 추적 에러", e)
        }
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissionAndStartService()
            return
        }
        locationListener?.let { listener ->
            try {
                // ✅ 5초 / 10m 업데이트
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, listener)
                locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, listener)
            } catch (e: Exception) {
                Log.e("MainActivity", "위치 요청 에러", e)
            }
        }
    }

    private fun checkPermissionAndStartService() {
        val permissions = mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startLocationService()
            if (tMapView != null) startTrackingMyLocation()
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        OpenWeatherClient.instance.getCurrentWeather(lat, lon).enqueue(object : Callback<OpenWeatherResponse> {
            override fun onResponse(call: Call<OpenWeatherResponse>, response: Response<OpenWeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {
                        tvWeatherTemp.text = "${it.main.temp.toInt()}°C"
                        tvWeatherDesc.text = it.weather[0].detail
                        Glide.with(this@MainActivity).load("https://openweathermap.org/img/wn/${it.weather[0].icon}@2x.png").into(ivWeatherIcon)
                    }
                }
            }
            override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {
                Log.e("MainActivity", "날씨 조회 실패", t)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter("com.moimiApp.moimi.NEW_NOTIFICATION"))
        fetchDashboardData()
        loadProfileMarker() // 프로필 변경 후 돌아왔을 때 갱신
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        autoTrackingHandler.removeCallbacks(autoTrackingRunnable)
    }

    private fun initViews() {
        tvNoti3 = findViewById(R.id.tv_noti_3)
        notiBadge = findViewById(R.id.view_noti_badge)
        tvTransportTime = findViewById(R.id.tv_transport_time)
        tvTransportInfo = findViewById(R.id.tv_transport_info)
        tvClickGuide = findViewById(R.id.tv_click_guide)
        layoutTransport = findViewById(R.id.layout_transport_container)
        tvWeatherTemp = findViewById(R.id.tv_weather_temp)
        tvWeatherDesc = findViewById(R.id.tv_weather_desc)
        ivWeatherIcon = findViewById(R.id.iv_weather_icon)
        tvCurrentMonth = findViewById(R.id.tv_current_month)
        rvWeekCalendar = findViewById(R.id.rv_week_calendar)
        tvNearestSchedule = findViewById(R.id.tv_nearest_schedule)
        tvNearestScheduleTime = findViewById(R.id.tv_nearest_schedule_time)
        loadingOverlay = findViewById(R.id.loading_overlay)
        tvLoadingPercent = findViewById(R.id.tv_loading_percent)
        tvLoadingTip = findViewById(R.id.tv_loading_tip)
    }

    private fun setupWeekCalendar() {
        try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            val days = mutableListOf<Date>()
            for (i in 0..6) {
                days.add(cal.time)
                cal.add(Calendar.DATE, 1)
            }
            tvCurrentMonth.text = SimpleDateFormat("yyyy년 M월", Locale.getDefault()).format(days[0])
            rvWeekCalendar.layoutManager = GridLayoutManager(this, 7)
            weekAdapter = WeekCalendarAdapter(days, emptySet())
            rvWeekCalendar.adapter = weekAdapter
        } catch (e: Exception) {
            Log.e("MainActivity", "달력 설정 오류", e)
        }
    }

    private fun fetchDashboardData() {
        val token = getAuthToken()
        RetrofitClient.notificationInstance.getNotifications(token).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful) {
                    // [수정] 안 읽은 알림만 필터링하여 갯수를 셉니다.
                    fetchedNotifications = response.body()?.notifications?.filter { !it.is_read } ?: emptyList()
                    updateMainDashboard()
                }
            }
            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {}
        })
        RetrofitClient.scheduleInstance.getSchedules(token, null).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                if (response.isSuccessful) {
                    fetchedSchedules = response.body()?.schedules ?: emptyList()
                    updateMainDashboard()
                    val eventSet = mutableSetOf<String>()
                    fetchedSchedules.forEach { s ->
                        if (!s.date.isNullOrEmpty()) eventSet.add(s.date)
                    }
                    weekAdapter?.updateEvents(eventSet)
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                Log.e("MainActivity", "일정 조회 실패", t)
                Toast.makeText(this@MainActivity, "서버 연결 불안정: 일정을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateMainDashboard() {
        // [수정] 안 읽은 알림 갯수 반영
        val unreadCount = fetchedNotifications.size
        if (::notiBadge.isInitialized) notiBadge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE

        val next = fetchedSchedules.firstOrNull()
        if (next != null) {
            tvNoti3.text = next.title
            tvNearestSchedule.text = next.title
            tvNearestScheduleTime.text = "${next.date} ${next.time}"

            if (next.type == "MEETING") {
                nextMeetingLocation = next.location
                nextMeetingTitle = next.title
                tvTransportInfo.text = "${next.time}까지 도착"
            } else {
                nextMeetingLocation = null
                nextMeetingTitle = null
                tvTransportInfo.text = "일정 정보"
            }

            if (currentLat != 0.0 && !next.location.isNullOrEmpty()) {
                tvTransportTime.text = "계산 중..."
                fetchTravelTime(next.location)
            } else if (!next.location.isNullOrEmpty()) {
                tvTransportTime.text = next.location
            } else {
                tvTransportTime.text = "--"
            }
            tvClickGuide.visibility = View.VISIBLE
        } else {
            tvNoti3.text = "예정된 약속 없음"
            tvNearestSchedule.text = "일정 없음"
            tvNearestScheduleTime.text = ""
            tvTransportTime.text = "--"
            tvTransportInfo.text = ""
            tvClickGuide.visibility = View.GONE
            nextMeetingLocation = null
            nextMeetingTitle = null
        }
    }

    private fun fetchTravelTime(destinationName: String) {
        val tMapData = TMapData()
        tMapData.findAllPOI(destinationName, object : TMapData.OnFindAllPOIListener {
            override fun onFindAllPOI(poiList: ArrayList<TMapPOIItem>?) {
                if (!poiList.isNullOrEmpty()) {
                    val destPoi = poiList[0]
                    val request = RouteRequest(
                        startX = currentLon, startY = currentLat,
                        endX = destPoi.poiPoint.longitude, endY = destPoi.poiPoint.latitude,
                        totalValue = 2
                    )
                    TmapClient.instance.getRoute(tMapApiKey, request).enqueue(object : Callback<TmapRouteResponse> {
                        override fun onResponse(call: Call<TmapRouteResponse>, response: Response<TmapRouteResponse>) {
                            val props = response.body()?.features?.firstOrNull()?.properties
                            props?.let { runOnUiThread { tvTransportTime.text = "약 ${(it.totalTime ?: 0) / 60}분 소요" } }
                        }
                        override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) { runOnUiThread { tvTransportTime.text = "시간 정보 없음" } }
                    })
                } else {
                    runOnUiThread { tvTransportTime.text = "장소 불명" }
                }
            }
        })
    }

    private fun initLoadingScreen() {
        loadingOverlay.visibility = View.VISIBLE
        tvLoadingTip.text = tips[Random().nextInt(tips.size)]
        Thread {
            while (progressStatus < 90 && !isLoadingFinished) {
                progressStatus++
                try { Thread.sleep(30) } catch (e: Exception) {}
                handler.post { tvLoadingPercent.text = "$progressStatus%" }
            }
        }.start()
    }

    private fun completeLoading() {
        if (isLoadingFinished) return
        isLoadingFinished = true
        runOnUiThread { loadingOverlay.visibility = View.GONE }
    }

    private fun startLocationService() {
        try {
            val intent = Intent(this, LocationService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } catch (e: Exception) { Log.e("MainActivity", "Service Error", e) }
    }

    private fun stopLocationUpdates() {
        locationListener?.let { locationManager?.removeUpdates(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopLocationUpdates()
            val mapContainer = findViewById<ViewGroup>(R.id.map_container)
            mapContainer.removeAllViews()
            tMapView = null
            autoTrackingHandler.removeCallbacks(autoTrackingRunnable)
        } catch (e: Exception) {}
    }
}