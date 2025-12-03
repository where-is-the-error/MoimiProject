package com.moimiApp.moimi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.poi.TMapPOIItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random


class MainActivity : BaseActivity() {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var tMapView: TMapView? = null
    private var myProfileBitmap: Bitmap? = null

    // 내 현재 위치 저장용 변수
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

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
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 빨간 점 즉시 표시
            if (::notiBadge.isInitialized) {
                notiBadge.visibility = View.VISIBLE
            }
            // (선택) 알림 목록 데이터를 새로고침하고 싶다면:
            // fetchDashboardData()
        }
    }
    private val tips = listOf("로딩 중...", "잠시만 기다려주세요.")

    // 권한 요청 코드
    private val PERMISSION_REQUEST_CODE = 1001

    private val routeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val isTracking = result.data?.getBooleanExtra("isTracking", false) ?: false
            if (isTracking) {
                Toast.makeText(this, "경로 안내를 계속합니다.", Toast.LENGTH_SHORT).show()
                val destLat = result.data?.getDoubleExtra("destLat", 0.0) ?: 0.0
                val destLon = result.data?.getDoubleExtra("destLon", 0.0) ?: 0.0

                // 다시 메인으로 돌아왔을 때도 추적 시작
                if (destLat != 0.0) {
                    startTrackingMyLocation(forceZoom = true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Log.e("MainActivity", "🔥 [1] onCreate 실행됨")

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

        setupWeekCalendar()
        fetchDashboardData()

        findViewById<View>(R.id.btn_notification).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        // ✅ [수정] 오버레이 제거하여 지도 터치 허용
        val mapOverlay = findViewById<View>(R.id.view_map_overlay)
        mapOverlay.visibility = View.GONE

        // 카드 전체 영역 클릭 시 길찾기로 이동 (지도 외 영역 클릭 시)
        layoutTransport.setOnClickListener { moveToRouteActivity() }

        val mapContainer = findViewById<FrameLayout>(R.id.map_container)
        mapContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mapContainer.width > 0 && mapContainer.height > 0) {
                    mapContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Log.d("MainActivity", "📍 지도 컨테이너 크기 확정: ${mapContainer.width}x${mapContainer.height}")
                    initTMapActual(mapContainer)
                }
            }
        })

        handler.postDelayed({
            if (!isLoadingFinished) {
                Log.w("MainActivity", "⚠️ 로딩 타임아웃 발생 -> 강제 로딩 종료")
                completeLoading()
            }
        }, 5000)
    }

    private fun moveToRouteActivity() {
        val intent = Intent(this, RouteActivity::class.java)
        if (nextMeetingLocation != null) {
            intent.putExtra("destName", nextMeetingLocation)
            val formattedTitle = "$nextMeetingLocation ($nextMeetingTitle)"
            intent.putExtra("destTitle", formattedTitle)
        } else {
            Toast.makeText(this, "설정된 목적지가 없어 기본 길찾기로 이동합니다.", Toast.LENGTH_SHORT).show()
        }
        routeLauncher.launch(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTMapActual(container: FrameLayout) {
        try {
            Log.d("MainActivity", "🗺️ TMap 초기화 시작")

            try {
                // 마커 이미지 미리 로드
                val options = BitmapFactory.Options()
                // 원본 크기 그대로 로드 (필요시 리사이징)
                myProfileBitmap = BitmapFactory.decodeResource(resources, R.drawable.profile, options)
            } catch (e: Exception) {
                Log.e("MainActivity", "비트맵 로딩 실패", e)
            }

            container.removeAllViews()
            tMapView = TMapView(this)
            tMapView?.setSKTMapApiKey(tMapApiKey)

            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            container.addView(tMapView, params)

            // ✅ [추가] 지도 터치 시 부모 스크롤뷰(NestedScrollView)가 움직이지 않도록 설정
            tMapView?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        // 부모 뷰에게 터치 이벤트를 가로채지 말라고 요청
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false // 지도 자체의 터치 이벤트는 정상 처리되도록 false 반환
            }

            tMapView?.setOnMapReadyListener {
                Log.e("MainActivity", "✅ [2] TMap 로딩 성공 (onMapReady)")

                try {
                    // 초기 위치 설정 (서울 시청)
                    tMapView?.zoomLevel = 17
                    tMapView?.setCenterPoint(126.9780, 37.5665)
                    startTrackingMyLocation(forceZoom = true)
                } catch (e: Exception) {
                    Log.e("MainActivity", "TMap 설정 중 오류", e)
                }

                completeLoading()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ TMap 초기화 실패", e)
            completeLoading()
        }
    }

    private fun startTrackingMyLocation(forceZoom: Boolean = false) {
        Log.e("MainActivity", "🚀 [3] startTrackingMyLocation 호출됨")

        try {
            if (locationManager == null) locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (!isGpsEnabled && !isNetworkEnabled) {
                // 위치 서비스 꺼짐 처리 (필요 시 알림)
            }

            if (locationListener == null) {
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        currentLat = location.latitude
                        currentLon = location.longitude

                        if (isFinishing || isDestroyed) return

                        // 날씨는 최초 1회만 가져오기
                        if (!isWeatherFetched) {
                            isWeatherFetched = true
                            fetchWeatherData(location.latitude, location.longitude)
                        }

                        runOnUiThread {
                            try {
                                if (tMapView != null) {
                                    // ⭐ [수정] 내 위치로 지도 중심 이동 및 줌 레벨 설정
                                    // 지속적으로 내 위치를 따라다니도록 설정
                                    tMapView?.setCenterPoint(location.longitude, location.latitude)

                                    // 줌 레벨이 사용자가 변경한 게 아니라면 17로 유지 (화면 꽉 차게)
                                    // 강제 줌이 필요하거나 현재 줌이 너무 멀리 있다면 당겨줌
                                    if (forceZoom || tMapView?.zoomLevel!! < 15) {
                                        tMapView?.zoomLevel = 17
                                    }

                                    if (myProfileBitmap != null) {
                                        val marker = TMapMarkerItem().apply {
                                            id = "my_location"
                                            setTMapPoint(TMapPoint(location.latitude, location.longitude))
                                            icon = myProfileBitmap
                                            setPosition(0.5f, 0.5f) // 마커 중심점 설정
                                        }
                                        // 기존 마커 제거 후 새로 추가 (깜빡임 방지 위해 ID 관리)
                                        tMapView?.removeTMapMarkerItem("my_location")
                                        tMapView?.addTMapMarkerItem(marker)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "❌ 지도 업데이트 오류", e)
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
            Log.e("MainActivity", "위치 추적 시작 실패", e)
        }
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "❌ 위치 권한 없음! 권한 요청 시작")
            checkPermissionAndStartService()
            return
        }

        locationListener?.let { listener ->
            try {
                Log.d("MainActivity", "📡 위치 업데이트 요청 중 (GPS & Network)...")
                // 2초마다, 5미터 이상 이동 시 업데이트
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, listener)
                locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, listener)
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ 위치 요청 실패", e)
            }
        }
    }

    private fun checkPermissionAndStartService() {
        val requiredPermissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.d("MainActivity", "🚨 권한 요청: $missingPermissions")
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            Log.d("MainActivity", "✅ 모든 권한 보유 중")
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("MainActivity", "🎉 사용자 권한 승인함")
                startLocationService()
                if (tMapView != null) {
                    startTrackingMyLocation()
                }
            } else {
                Log.e("MainActivity", "🚫 사용자 권한 거부함")
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        OpenWeatherClient.instance.getCurrentWeather(lat, lon).enqueue(object : Callback<OpenWeatherResponse> {
            override fun onResponse(call: Call<OpenWeatherResponse>, response: Response<OpenWeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {
                        val temp = it.main.temp.toInt()
                        val condition = it.weather[0].condition
                        val iconCode = it.weather[0].icon
                        val desc = it.weather[0].detail

                        val msg = when {
                            condition.contains("Rain", true) -> "비가 와요 ☔\n우산 챙기세요!"
                            condition.contains("Snow", true) -> "눈이 와요 ☃️\n따뜻하게 입으세요!"
                            temp <= 5 -> "너무 추워요 ❄️\n감기 조심하세요!"
                            else -> "좋은 날씨예요 ☀️ ($desc)"
                        }

                        tvWeatherTemp.text = "$temp°C"
                        tvWeatherDesc.text = msg

                        if (!isDestroyed && !isFinishing) {
                            Glide.with(this@MainActivity)
                                .load("https://openweathermap.org/img/wn/$iconCode@2x.png")
                                .into(ivWeatherIcon)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {
                Log.e("MainActivity", "날씨 통신 오류", t)
            }
        })
    }
    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            notificationReceiver,
            IntentFilter("com.moimiApp.moimi.NEW_NOTIFICATION")
        )
        // 화면 돌아올 때마다 데이터 갱신
        fetchDashboardData()
    }

    // ✅ [3] 화면이 안 보일 때 리시버 해제 (메모리 누수 방지)
    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
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
        } catch (e: Exception) { Log.e("MainActivity", "Calendar Error", e) }
    }

    private fun fetchDashboardData() {
        val token = getAuthToken()
        RetrofitClient.notificationInstance.getNotifications(token).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful) {
                    fetchedNotifications = response.body()?.notifications ?: emptyList()
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
                    updateCalendarMarkers()
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {}
        })
    }

    private fun updateCalendarMarkers() {
        val eventSet = mutableSetOf<String>()
        fetchedSchedules.forEach { s ->
            if (!s.date.isNullOrEmpty()) eventSet.add(s.date)
        }
        weekAdapter?.updateEvents(eventSet)
    }

    private fun updateMainDashboard() {
        if (::notiBadge.isInitialized) {
            notiBadge.visibility = if (fetchedNotifications.isNotEmpty()) View.VISIBLE else View.GONE
        }

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
                    val destLat = destPoi.poiPoint.latitude
                    val destLon = destPoi.poiPoint.longitude

                    val request = RouteRequest(
                        startX = currentLon,
                        startY = currentLat,
                        endX = destLon,
                        endY = destLat,
                        totalValue = 2
                    )

                    TmapClient.instance.getRoute(tMapApiKey, request).enqueue(object : Callback<TmapRouteResponse> {
                        override fun onResponse(call: Call<TmapRouteResponse>, response: Response<TmapRouteResponse>) {
                            val props = response.body()?.features?.firstOrNull()?.properties
                            props?.let {
                                val totalTimeSec = it.totalTime ?: 0
                                val timeMin = totalTimeSec / 60

                                runOnUiThread {
                                    tvTransportTime.text = "약 ${timeMin}분 소요"
                                }
                            }
                        }
                        override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) {
                            runOnUiThread { tvTransportTime.text = "시간 정보 없음" }
                        }
                    })
                } else {
                    runOnUiThread { tvTransportTime.text = "장소 불명" }
                }
            }
        })
    }

    private fun initLoadingScreen() {
        loadingOverlay.visibility = View.VISIBLE
        val randomTip = tips[Random().nextInt(tips.size)]
        tvLoadingTip.text = randomTip
        Thread {
            while(progressStatus < 90 && !isLoadingFinished) {
                progressStatus++
                try { Thread.sleep(30) } catch(e:Exception){}
                handler.post { tvLoadingPercent.text = "$progressStatus%" }
            }
        }.start()
    }

    private fun completeLoading() {
        if (isLoadingFinished) return
        isLoadingFinished = true
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
        }
    }

    private fun startLocationService() {
        try {
            val intent = Intent(this, LocationService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Service Start Error", e)
        }
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
        } catch (e: Exception) {}
    }
}