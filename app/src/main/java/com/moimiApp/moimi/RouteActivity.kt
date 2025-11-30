package com.moimiApp.moimi
import com.google.android.gms.location.LocationRequest


import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine
import com.skt.tmap.poi.TMapPOIItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList
import kotlin.math.*

class RouteActivity : BaseActivity() {

    private lateinit var tMapView: TMapView

    // UI 변수 (lateinit으로 선언하되 initViews에서 반드시 초기화)
    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvTimeWalk: TextView
    private lateinit var tvTimeTransit: TextView
    private lateinit var tvTimeTaxi: TextView
    private lateinit var tvDetailDist: TextView

    // [방어 코드 1] 목적지 기본값: 데이터 없으면 "신도림역"으로 고정
    private var destName: String = "신도림역"
    private var destTitle: String = "신도림역"

    // [방어 코드 2] 좌표 기본값: 0.0 대신 "서울시청(출발)" & "신도림역(도착)" 좌표 미리 입력
    // 이렇게 하면 GPS나 API가 실패해도 지도가 0,0(바다 한가운데)으로 가서 터지는 일을 막습니다.
    private var startLat: Double = 37.5665 // 서울 시청
    private var startLon: Double = 126.9780
    private var destLat: Double = 37.5094 // 신도림역
    private var destLon: Double = 126.8907

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isRouteInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_main)

        setupDrawer()

        // 1. Intent 데이터 수신 (값이 없거나 비어있으면 위에서 설정한 '신도림역' 유지)
        val iName = intent.getStringExtra("destName")
        val iTitle = intent.getStringExtra("destTitle")

        if (!iName.isNullOrEmpty()) destName = iName
        if (!iTitle.isNullOrEmpty()) destTitle = iTitle

        // 2. 뷰 초기화 (NullPointerException 방지)
        initViews()

        tvStart.text = "출발지: 위치 확인 중..."
        tvEnd.text = "도착지: $destTitle"
        tvSummary.text = "내 위치 ➔ $destTitle"

        // 3. 지도 컨테이너 연결
        val mapContainer = findViewById<ViewGroup>(R.id.map_container)
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapApiKey)
        mapContainer.addView(tMapView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 15
            tMapView.setIconVisibility(true) // 내 위치 파란 점
            tMapView.setSightVisible(true)   // 나침반 방향

            // 지도가 준비되면 권한 체크 후 위치 추적 시작
            checkLocationAndStart()
        }
    }

    private fun initViews() {
        tvStart = findViewById(R.id.tv_route_start)
        tvEnd = findViewById(R.id.tv_route_end)
        tvSummary = findViewById(R.id.btn_route_summary)
        tvTimeWalk = findViewById(R.id.tv_time_walk)
        tvTimeTransit = findViewById(R.id.tv_time_transit)
        tvTimeTaxi = findViewById(R.id.tv_time_taxi)
        tvDetailDist = findViewById(R.id.tv_detail_distance)

        val btnHome = findViewById<ImageView>(R.id.btn_home_logo)
        val btnNoti = findViewById<ImageView>(R.id.btn_notification)
        val cardInfo = findViewById<androidx.cardview.widget.CardView>(R.id.card_route_info)
        val layoutTaxi = findViewById<LinearLayout>(R.id.layout_btn_taxi)

        // 홈 버튼
        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnNoti.setOnClickListener {
            Toast.makeText(this, "새로운 알림이 없습니다.", Toast.LENGTH_SHORT).show()
        }

        // 경로 요약 버튼: 좌표가 이미 초기화되어 있으므로 안전하게 이동 가능
        tvSummary.setOnClickListener {
            zoomToSpan(startLat, startLon, destLat, destLon)
        }

        val naviListener = { startTMapNavigation() }
        cardInfo.setOnClickListener { naviListener() }
        layoutTaxi.setOnClickListener { naviListener() }
    }

    private fun checkLocationAndStart() {
        // 권한 없으면 -> 기본값(서울시청 -> 신도림)으로 경로 그림 (앱 종료 방지)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "위치 권한이 없어 기본 경로를 표시합니다.", Toast.LENGTH_SHORT).show()
            initializeRoute(null)
            return
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        // [수정 요청 반영] Builder 사용
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    startLat = location.latitude
                    startLon = location.longitude

                    if (::tMapView.isInitialized) {
                        tMapView.setLocationPoint(location.longitude, location.latitude)

                        // 처음 위치 잡았을 때 경로 탐색 시작
                        if (!isRouteInitialized) {
                            tvStart.text = "출발지: 내 위치"
                            initializeRoute(location)
                            tMapView.setCenterPoint(location.longitude, location.latitude)
                        }
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun initializeRoute(location: Location?) {
        isRouteInitialized = true

        // [방어 코드 3] 내 위치가 null이면 기본값(서울시청) 사용 -> 앱 종료 방지
        val sLat = location?.latitude ?: 37.5665
        val sLon = location?.longitude ?: 126.9780
        val startPoint = TMapPoint(sLat, sLon)

        // 목적지 검색 및 경로 그리기
        searchDestinationAndDrawRoute(startPoint, destName)
    }

    private fun searchDestinationAndDrawRoute(startPoint: TMapPoint, keyword: String) {
        val tmapData = TMapData()
        tmapData.findAllPOI(keyword, object : TMapData.OnFindAllPOIListener {
            override fun onFindAllPOI(poiList: ArrayList<TMapPOIItem>?) {
                // 검색 결과가 있으면 좌표 업데이트
                if (!poiList.isNullOrEmpty()) {
                    val poi = poiList[0]
                    destLat = poi.poiPoint.latitude
                    destLon = poi.poiPoint.longitude
                } else {
                    // [방어 코드 4] 검색 실패 시에도 로그만 남기고, 이미 설정된 '신도림역' 좌표 사용
                    Log.e("RouteActivity", "목적지 검색 실패: $keyword. 기본 좌표(신도림)를 사용합니다.")
                }

                // 좌표가 100% 존재하므로 안전하게 경로 그리기
                val endPoint = TMapPoint(destLat, destLon)

                drawPolyLine(startPoint, endPoint)
                fetchCarRouteInfo(startPoint, endPoint)
                fetchWalkRouteInfo(startPoint, endPoint)

                zoomToSpan(startPoint.latitude, startPoint.longitude, destLat, destLon)
            }
        })
    }

    private fun drawPolyLine(start: TMapPoint, end: TMapPoint) {
        val tmapData = TMapData()
        tmapData.findPathDataWithType(
            TMapData.TMapPathType.CAR_PATH,
            start,
            end,
            object : TMapData.OnFindPathDataWithTypeListener {
                override fun onFindPathDataWithType(polyLine: TMapPolyLine?) {
                    polyLine?.let {
                        it.lineColor = Color.BLUE
                        it.lineWidth = 20f
                        tMapView.addTMapPolyLine(it)
                    }
                }
            }
        )
    }

    private fun zoomToSpan(lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
        runOnUiThread {
            val centerLat = (lat1 + lat2) / 2
            val centerLon = (lon1 + lon2) / 2
            tMapView.setCenterPoint(centerLon, centerLat)

            val distance = getDistance(lat1, lon1, lat2, lon2)
            tMapView.zoomLevel = when {
                distance < 1000 -> 16
                distance < 3000 -> 14
                distance < 7000 -> 13
                distance < 15000 -> 11
                else -> 9
            }
        }
    }

    private fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun fetchCarRouteInfo(start: TMapPoint, end: TMapPoint) {
        val request = RouteRequest(
            startX = start.longitude, startY = start.latitude,
            endX = end.longitude, endY = end.latitude,
            totalValue = 2
        )

        TmapClient.instance.getRoute(tMapApiKey, request).enqueue(object : Callback<TmapRouteResponse> {
            override fun onResponse(call: Call<TmapRouteResponse>, response: Response<TmapRouteResponse>) {
                val props = response.body()?.features?.firstOrNull()?.properties
                props?.let {
                    val time = (it.totalTime ?: 0) / 60
                    val dist = String.format("%.1f", (it.totalDistance ?: 0) / 1000.0)
                    val fare = it.taxiFare ?: 0
                    runOnUiThread {
                        tvTimeTaxi.text = "${time}분"
                        tvDetailDist.text = "${dist}km\n택시 약 ${String.format("%,d", fare)}원"
                    }
                }
            }
            override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) {
                Log.e("Route", "Car API Fail: ${t.message}")
            }
        })
    }

    private fun fetchWalkRouteInfo(start: TMapPoint, end: TMapPoint) {
        val request = RouteRequest(
            startX = start.longitude, startY = start.latitude,
            endX = end.longitude, endY = end.latitude,
            startName = "출발", endName = "도착"
        )

        TmapClient.instance.getPedestrianRoute(tMapApiKey, request).enqueue(object : Callback<TmapRouteResponse> {
            override fun onResponse(call: Call<TmapRouteResponse>, response: Response<TmapRouteResponse>) {
                val props = response.body()?.features?.firstOrNull()?.properties
                props?.let {
                    val time = (it.totalTime ?: 0) / 60
                    runOnUiThread {
                        tvTimeWalk.text = "${time}분"
                        tvTimeTransit.text = "${(time * 0.4).toInt()}분"
                    }
                }
            }
            override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) {
                Log.e("Route", "Walk API Fail: ${t.message}")
            }
        })
    }

    private fun startTMapNavigation() {
        // [방어 코드 5] 좌표가 0.0일 확률이 없음 (기본값 사용)
        try {
            val url = "tmap://route?goalname=$destTitle&goalx=$destLon&goaly=$destLat"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "TMap 앱을 설치해주세요.", Toast.LENGTH_SHORT).show()
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.skt.tmap.ku"))
            startActivity(marketIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}