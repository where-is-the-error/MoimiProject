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
import android.widget.TextView
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

    // 로딩 관련 변수
    private lateinit var loadingOverlay: View
    private lateinit var tvLoadingPercent: TextView
    private lateinit var tvLoadingTip: TextView
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isLoadingFinished = false // 로딩 중복 종료 방지

    private val tips = listOf(
        "Tip! 누군가의 차를 얻어탈때는\n차도 옆까지 10분 전에는 도착해있어야 해요!",
        "Tip! 약속 시간에 늦을 것 같다면\n미리 채팅으로 친구들에게 알려주세요!",
        "Tip! 모임 장소가 헷갈릴 땐\n지도를 확대해서 확인해보세요.",
        "Tip! 출발하기 전에\n소지품을 한 번 더 확인해보세요!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 로그인 체크
        if (prefsManager.getToken() == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // 1. 로딩 화면 시작
        initLoadingScreen()

        setupDrawer()
        checkPermissionAndStartService()
        fetchNotifications()

        // 지도 초기화
        val mapContainer = findViewById<ViewGroup>(R.id.map_container)

        try {
            tMapView = TMapView(this)
            mapContainer.addView(tMapView)
            tMapView.setSKTMapApiKey(tMapApiKey)

            tMapView.setOnMapReadyListener {
                tMapView.zoomLevel = 15
                startTrackingMyLocation()
                // 지도 로딩 성공 시 로딩 종료
                completeLoading()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "TMAP 초기화 오류: ${e.message}")
            completeLoading()
        }

        // ⭐ [추가] 안전장치: 4초가 지나도 지도가 안 뜨면 강제로 로딩 종료 (90% 멈춤 방지)
        handler.postDelayed({
            if (!isLoadingFinished) {
                Log.w("MainActivity", "지도 로딩 시간 초과 -> 강제 진입")
                completeLoading()
            }
        }, 4000) // 4초 대기
    }

    private fun initLoadingScreen() {
        loadingOverlay = findViewById(R.id.loading_overlay)
        tvLoadingPercent = findViewById(R.id.tv_loading_percent)
        tvLoadingTip = findViewById(R.id.tv_loading_tip)

        val randomTip = tips[Random().nextInt(tips.size)]
        tvLoadingTip.text = randomTip

        Thread {
            while (progressStatus < 90) {
                if (isLoadingFinished) break // 로딩 끝났으면 루프 탈출

                progressStatus += 1
                try {
                    if (progressStatus < 50) Thread.sleep(20)
                    else Thread.sleep(40)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                handler.post {
                    tvLoadingPercent.text = "로딩 $progressStatus%"
                }
            }
        }.start()
    }

    private fun completeLoading() {
        if (isLoadingFinished) return // 이미 끝났으면 실행 X
        isLoadingFinished = true

        Thread {
            // 90% -> 100% 빠르게 채우기
            while (progressStatus < 100) {
                progressStatus += 2
                try { Thread.sleep(5) } catch (e: Exception) {}
                handler.post {
                    tvLoadingPercent.text = "로딩 $progressStatus%"
                }
            }

            // 100% 보여주고 잠시 뒤 사라짐
            try { Thread.sleep(200) } catch (e: Exception) {}

            handler.post {
                hideLoadingWithAnimation()
            }
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
                        }
                    }
                }
            }, Looper.getMainLooper())
        }
    }

    // ... 기존 코드 (권한, 서비스, 알림 등) 유지 ...
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

    private fun fetchNotifications() {
        val tvNoti1 = findViewById<TextView>(R.id.tv_noti_1)
        val tvNoti2 = findViewById<TextView>(R.id.tv_noti_2)
        val token = getAuthToken()

        RetrofitClient.notificationInstance.getNotifications(token)
            .enqueue(object : Callback<NotificationResponse> {
                override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val notiList = response.body()!!.notifications
                        if (notiList.isNotEmpty()) tvNoti1.text = notiList[0].message
                        else tvNoti1.text = "새로운 알림이 없습니다."

                        if (notiList.size >= 2) tvNoti2.text = notiList[1].message
                        else tvNoti2.text = ""
                    } else {
                        tvNoti1.text = "알림 로드 실패"
                    }
                }
                override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                    tvNoti1.text = "서버 연결 실패"
                    Log.e("MAIN", "Noti Error: ${t.message}")
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (prefsManager.getToken() != null) {
            fetchNotifications()
        }
    }

    // 👇 날씨 정보를 가져오는 함수 (위도, 경도 필요)
    private fun fetchWeatherData(lat: Double, lon: Double) {

        // Retrofit 클라이언트 호출
        OpenWeatherClient.instance.getCurrentWeather(lat, lon).enqueue(object : Callback<OpenWeatherResponse> {
            override fun onResponse(call: Call<OpenWeatherResponse>, response: Response<OpenWeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {
                        // 1. 로그로 데이터 확인
                        Log.d("Weather", "지역: ${it.cityName}, 온도: ${it.main.temp}, 날씨: ${it.weather[0].detail}")

                        // 2. UI 업데이트 (TextView 예시)
                        // binding.tvTemp.text = "${it.main.temp.toInt()}°C"
                        // binding.tvDescription.text = it.weather[0].detail

                        // 3. 날씨 아이콘 이미지 불러오기 (Glide 사용)
                        val iconCode = it.weather[0].icon // 예: "10d"
                        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"

                        // ImageView에 이미지 넣기 (binding.ivWeatherIcon 이 있다고 가정)
                        /* Glide.with(this@MainActivity)
                            .load(iconUrl)
                            .into(binding.ivWeatherIcon)
                        */
                    }
                } else {
                    Log.e("Weather", "응답 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {
                Log.e("Weather", "통신 오류: ${t.message}")
            }
        })
    }
}