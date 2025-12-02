package com.moimiApp.moimi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Random

class MainActivity : BaseActivity() {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var myProfileBitmap: Bitmap? = null

    private lateinit var tMapView: TMapView
    private val myLocationMarker = TMapMarkerItem()

    // UI 요소
    private lateinit var loadingOverlay: View
    private lateinit var tvLoadingPercent: TextView
    private lateinit var tvLoadingTip: TextView

    private lateinit var tvNoti1: TextView
    private lateinit var tvNoti2: TextView
    private lateinit var tvNoti3: TextView

    private var nextMeetingLocation: String? = null
    private var nextMeetingTitle: String? = null

    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isLoadingFinished = false
    private var isWeatherFetched = false

    private val tips = listOf(
        "Tip! 누군가의 차를 얻어탈때는\n차도 옆까지 10분 전에는 도착해있어야 해요!",
        "Tip! 약속 시간에 늦을 것 같다면\n미리 채팅으로 친구들에게 알려주세요!",
        "Tip! 모임 장소가 헷갈릴 땐\n지도를 확대해서 확인해보세요.",
        "Tip! 출발하기 전에\n소지품을 한 번 더 확인해보세요!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (prefsManager.getToken() == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        handleDeepLink(intent)

        initLoadingScreen()
        setupDrawer()
        checkPermissionAndStartService()

        tvNoti1 = findViewById(R.id.tv_noti_1)
        tvNoti2 = findViewById(R.id.tv_noti_2)
        tvNoti3 = findViewById(R.id.tv_noti_3)

        fetchDashboardData()

        val mapContainer = findViewById<ViewGroup>(R.id.map_container)
        val mapOverlay = findViewById<View>(R.id.view_map_overlay)

        mapOverlay.setOnClickListener {
            val intent = Intent(this, RouteActivity::class.java)
            if (nextMeetingLocation != null) {
                intent.putExtra("destName", nextMeetingLocation)
                intent.putExtra("destTitle", nextMeetingTitle)
            }
            startActivity(intent)
        }

        try {
            myProfileBitmap = BitmapFactory.decodeResource(resources, R.drawable.profile)
        } catch (e: Exception) {
            Log.e("MainActivity", "이미지 로드 실패: ${e.message}")
        }

        try {
            tMapView = TMapView(this)
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            mapContainer.addView(tMapView, params)
            tMapView.setSKTMapApiKey(tMapApiKey)

            tMapView.setOnMapReadyListener {
                Log.d("MainActivity", "TMAP 로딩 완료")
                tMapView.zoomLevel = 15
                startTrackingMyLocation()
                completeLoading()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "TMAP 초기화 오류: ${e.message}")
            completeLoading()
        }

        handler.postDelayed({
            if (!isLoadingFinished) completeLoading()
        }, 4000)
    }

    // ⭐ [수정] 인자 타입을 Intent (non-null)로 수정하여 오버라이드 오류 해결
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "http" && data.host == "moimi.app") {
            val pathSegments = data.pathSegments
            if (pathSegments.size >= 2 && pathSegments[0] == "invite") {
                val scheduleId = pathSegments[1]
                joinAndShowSchedule(scheduleId)
            }
        }
    }

    private fun joinAndShowSchedule(scheduleId: String) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        RetrofitClient.scheduleInstance.joinSchedule(token, scheduleId)
            .enqueue(object : Callback<JoinScheduleResponse> {
                override fun onResponse(call: Call<JoinScheduleResponse>, response: Response<JoinScheduleResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@MainActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                        fetchScheduleAndMove(token, scheduleId)
                    } else {
                        Toast.makeText(this@MainActivity, "일정 확인 중...", Toast.LENGTH_SHORT).show()
                        fetchScheduleAndMove(token, scheduleId)
                    }
                }
                override fun onFailure(call: Call<JoinScheduleResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchScheduleAndMove(token: String, scheduleId: String) {
        RetrofitClient.scheduleInstance.getSchedule(token, scheduleId)
            .enqueue(object : Callback<SingleScheduleResponse> {
                override fun onResponse(call: Call<SingleScheduleResponse>, response: Response<SingleScheduleResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val item = response.body()!!.schedule
                        if (item != null) {
                            val intent = Intent(this@MainActivity, ScheduleDetailActivity::class.java).apply {
                                putExtra("title", item.title)
                                putExtra("date", item.date ?: "")
                                putExtra("time", item.time)
                                putExtra("location", item.location)
                                putExtra("scheduleId", item.id)
                                // inviteCode는 필수가 아니므로 없을 수 있음
                                putExtra("inviteCode", item.inviteCode ?: "")
                            }
                            startActivity(intent)
                        }
                    }
                }
                override fun onFailure(call: Call<SingleScheduleResponse>, t: Throwable) {}
            })
    }

    private fun fetchDashboardData() {
        val token = getAuthToken()
        RetrofitClient.notificationInstance.getNotifications(token).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val notiList = response.body()!!.notifications
                    if (notiList.isNotEmpty()) {
                        tvNoti1.text = notiList[0].message
                        if (notiList.size > 1) tvNoti2.text = notiList[1].message
                    } else {
                        tvNoti1.text = "새로운 알림이 없습니다."
                        tvNoti2.text = ""
                    }
                }
            }
            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {}
        })

        RetrofitClient.instance.getMeetings(token).enqueue(object : Callback<MeetingListResponse> {
            override fun onResponse(call: Call<MeetingListResponse>, response: Response<MeetingListResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val meetings = response.body()!!.meetings
                    if (!meetings.isNullOrEmpty()) {
                        val nextMeeting = meetings[0]
                        tvNoti3.text = "${nextMeeting.dateTime} ${nextMeeting.title}"
                        nextMeetingLocation = nextMeeting.location
                        nextMeetingTitle = nextMeeting.title
                    } else {
                        tvNoti3.text = "예정된 모임이 없습니다."
                    }
                }
            }
            override fun onFailure(call: Call<MeetingListResponse>, t: Throwable) {}
        })
    }

    private fun initLoadingScreen() {
        loadingOverlay = findViewById(R.id.loading_overlay)
        tvLoadingPercent = findViewById(R.id.tv_loading_percent)
        tvLoadingTip = findViewById(R.id.tv_loading_tip)
        val randomTip = tips[Random().nextInt(tips.size)]
        tvLoadingTip.text = randomTip
        Thread {
            while (progressStatus < 90) {
                if (isLoadingFinished) break
                progressStatus += 1
                try { if (progressStatus < 50) Thread.sleep(20) else Thread.sleep(40) } catch (e: Exception) {}
                handler.post { tvLoadingPercent.text = "로딩 $progressStatus%" }
            }
        }.start()
    }

    private fun completeLoading() {
        if (isLoadingFinished) return
        isLoadingFinished = true
        Thread {
            while (progressStatus < 100) {
                progressStatus += 2
                try { Thread.sleep(5) } catch (e: Exception) {}
                handler.post { tvLoadingPercent.text = "로딩 $progressStatus%" }
            }
            try { Thread.sleep(200) } catch (e: Exception) {}
            handler.post { hideLoadingWithAnimation() }
        }.start()
    }

    private fun hideLoadingWithAnimation() {
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 500
        loadingOverlay.startAnimation(fadeOut)
        loadingOverlay.visibility = View.GONE
    }

    private fun startTrackingMyLocation() {
        if (locationManager == null) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        if (locationListener == null) {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    if (isFinishing || isDestroyed) return
                    Log.d("MainActivity", "위치 수신: ${location.latitude}, ${location.longitude}")
                    if (::tMapView.isInitialized && myProfileBitmap != null) {
                        runOnUiThread {
                            if (tMapView.windowToken == null) return@runOnUiThread
                            tMapView.setCenterPoint(location.longitude, location.latitude)
                            tMapView.removeTMapMarkerItem("my_location")
                            val marker = com.skt.tmap.overlay.TMapMarkerItem().apply {
                                id = "my_location"
                                setTMapPoint(TMapPoint(location.latitude, location.longitude))
                                icon = myProfileBitmap
                                setPosition(0.5f, 0.5f)
                                name = "내 위치"
                            }
                            tMapView.addTMapMarkerItem(marker)
                        }
                        if (!isWeatherFetched) {
                            isWeatherFetched = true
                            fetchWeatherData(location.latitude, location.longitude)
                        }
                    }
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
        }

        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            locationListener?.let { listener ->
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 5f, listener)
                locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, listener)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "위치 요청 실패", e)
        }
    }

    private fun stopLocationUpdates() {
        locationListener?.let {
            locationManager?.removeUpdates(it)
        }
    }

    private fun checkPermissionAndStartService() {
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val denied = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (denied.isEmpty()) {
            startLocationService()
        } else {
            ActivityCompat.requestPermissions(this, denied.toTypedArray(), 1001)
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

    private fun fetchWeatherData(lat: Double, lon: Double) {
        OpenWeatherClient.instance.getCurrentWeather(lat, lon).enqueue(object : Callback<OpenWeatherResponse> {
            override fun onResponse(call: Call<OpenWeatherResponse>, response: Response<OpenWeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {
                        val tvWeatherInfo = findViewById<TextView>(R.id.tv_weather_info)
                        val ivWeatherIcon = findViewById<android.widget.ImageView>(R.id.iv_weather_icon)
                        val temp = it.main.temp.toInt()
                        val desc = it.weather[0].detail
                        val iconCode = it.weather[0].icon
                        tvWeatherInfo.text = "$temp°C $desc"
                        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                        try {
                            com.bumptech.glide.Glide.with(this@MainActivity).load(iconUrl).into(ivWeatherIcon)
                        } catch (e: NoClassDefFoundError) {
                            Log.e("Weather", "Glide 오류")
                        }
                    }
                }
            }
            override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {}
        })
    }

    override fun onResume() {
        super.onResume()
        if (::tMapView.isInitialized) {
            requestLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
}