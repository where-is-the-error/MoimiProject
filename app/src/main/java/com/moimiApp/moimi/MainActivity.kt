package com.moimiApp.moimi

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Random

class MainActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val myLocationMarker = TMapMarkerItem()

    // UI 요소
    private lateinit var loadingOverlay: View
    private lateinit var tvLoadingPercent: TextView
    private lateinit var tvLoadingTip: TextView

    // 알림/일정 표시용 뷰
    private lateinit var tvNoti1: TextView
    private lateinit var tvNoti2: TextView
    private lateinit var tvNoti3: TextView

    // 다음 모임 정보 저장용 (길찾기 전달용)
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

        initLoadingScreen()
        setupDrawer()
        checkPermissionAndStartService()

        // 뷰 연결
        tvNoti1 = findViewById(R.id.tv_noti_1)
        tvNoti2 = findViewById(R.id.tv_noti_2)
        tvNoti3 = findViewById(R.id.tv_noti_3)

        // 데이터 불러오기
        fetchDashboardData()

        // 지도 초기화
        val mapContainer = findViewById<ViewGroup>(R.id.map_container)
        val mapOverlay = findViewById<View>(R.id.view_map_overlay)

        // [핵심] 지도 클릭(오버레이 클릭) 시 길찾기 화면으로 이동
        mapOverlay.setOnClickListener {
            val intent = Intent(this, RouteActivity::class.java)

            if (nextMeetingLocation != null) {
                // 다음 모임이 있으면 그곳으로 안내
                intent.putExtra("destName", nextMeetingLocation)
                intent.putExtra("destTitle", nextMeetingTitle)
            } else {
                // 모임이 없으면 '서울역'을 기본 목적지로 설정하여 이동
                Toast.makeText(this, "예정된 모임이 없어 서울역으로 안내합니다.", Toast.LENGTH_SHORT).show()
                intent.putExtra("destName", "서울역")
                intent.putExtra("destTitle", "서울역 (테스트)")
            }
            startActivity(intent)
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

    private fun fetchDashboardData() {
        val token = getAuthToken()

        // 1. 알림 가져오기
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

        // 2. 내 모임(일정) 가져오기 -> tv_noti_3에 표시 및 길찾기 목적지 설정
        RetrofitClient.instance.getMeetings(token).enqueue(object : Callback<MeetingListResponse> {
            override fun onResponse(call: Call<MeetingListResponse>, response: Response<MeetingListResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val meetings = response.body()!!.meetings
                    if (!meetings.isNullOrEmpty()) {
                        // 가장 가까운 모임 하나 가져오기
                        val nextMeeting = meetings[0]
                        val displayStr = "${nextMeeting.dateTime} ${nextMeeting.title}"
                        tvNoti3.text = displayStr

                        // [중요] 길찾기용 변수 저장
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

    // ... (기존 initLoadingScreen, startTrackingMyLocation, fetchWeatherData 등 나머지 코드는 그대로 유지) ...

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
                try {
                    if (progressStatus < 50) Thread.sleep(20)
                    else Thread.sleep(40)
                } catch (e: InterruptedException) { e.printStackTrace() }

                handler.post {
                    tvLoadingPercent.text = "로딩 $progressStatus%"
                }
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        if (::tMapView.isInitialized) {
                            tMapView.setCenterPoint(location.longitude, location.latitude)
                            myLocationMarker.id = "my_location"
                            myLocationMarker.setTMapPoint(TMapPoint(location.latitude, location.longitude))
                            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.profile)
                            myLocationMarker.icon = bitmap
                            myLocationMarker.setPosition(0.5f, 0.5f)
                            tMapView.addTMapMarkerItem(myLocationMarker)

                            if (!isWeatherFetched) {
                                isWeatherFetched = true
                                fetchWeatherData(location.latitude, location.longitude)
                            }
                        }
                    }
                }
            }, Looper.getMainLooper())
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

    private fun fetchNotifications() { /* ... 위에서 이미 fetchDashboardData로 통합함 ... */ }

    // [수정] 날씨 정보를 가져와서 UI(아이콘 + 텍스트)에 적용
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
                            com.bumptech.glide.Glide.with(this@MainActivity)
                                .load(iconUrl)
                                .into(ivWeatherIcon)
                        } catch (e: NoClassDefFoundError) {
                            Log.e("Weather", "Glide 오류")
                        }
                    }
                }
            }
            override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {}
        })
    }
}