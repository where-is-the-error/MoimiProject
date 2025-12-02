package com.moimiApp.moimi

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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine
import com.skt.tmap.poi.TMapPOIItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class RouteActivity : BaseActivity() {

    private lateinit var tMapView: TMapView

    // UI 변수
    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvTimeWalk: TextView
    private lateinit var tvTimeTransit: TextView
    private lateinit var tvTimeTaxi: TextView
    private lateinit var tvDetailDist: TextView

    // [수정 1] 목적지 기본값을 null로 변경 (서울역/신도림역 강제 안내 제거)
    private var destName: String? = null
    private var destTitle: String? = null

    // 출발지 좌표 (초기값: 서울시청) - GPS 수신 전 안전장치
    private var startLat: Double = 37.5665
    private var startLon: Double = 126.9780

    // 도착지 좌표 (검색 결과로 덮어씌워짐)
    private var destLat: Double = 0.0
    private var destLon: Double = 0.0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isRouteInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [수정 2] 올바른 레이아웃 파일 연결 확인 (activity_route_main)
        setContentView(R.layout.activity_route_main)

        setupDrawer()

        // 1. Intent 데이터 수신 (MainActivity에서 넘어온 목적지 정보)
        val iName = intent.getStringExtra("destName")
        val iTitle = intent.getStringExtra("destTitle")

        // 데이터가 넘어왔을 때만 변수에 할당
        if (!iName.isNullOrEmpty()) {
            destName = iName
            destTitle = iTitle
        }

        // 2. 뷰 초기화
        initViews()

        tvStart.text = "출발지: 위치 확인 중..."

        // 목적지 유무에 따라 텍스트 표시
        if (destTitle != null) {
            tvEnd.text = "도착지: $destTitle"
            tvSummary.text = "내 위치 ➔ $destTitle"
        } else {
            tvEnd.text = "도착지: (설정 안 됨)"
            tvSummary.text = "목적지가 설정되지 않았습니다"
        }

        // 3. 지도 컨테이너 연결
        val mapContainer = findViewById<ViewGroup>(R.id.map_container)
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapApiKey)
        mapContainer.addView(tMapView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 15
            tMapView.setIconVisibility(true) // 내 위치 파란 점 표시
            tMapView.setSightVisible(true)   // 나침반 방향 표시

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

        // 경로 요약 버튼 (목적지가 있을 때만 이동)
        tvSummary.setOnClickListener {
            if (destTitle != null && destLat != 0.0) {
                zoomToSpan(startLat, startLon, destLat, destLon)
            } else {
                Toast.makeText(this, "목적지가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        val naviListener = { startTMapNavigation() }
        cardInfo.setOnClickListener { naviListener() }
        layoutTaxi.setOnClickListener { naviListener() }
    }

    private fun checkLocationAndStart() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "위치 권한이 없어 기본 위치를 표시합니다.", Toast.LENGTH_SHORT).show()
            // 권한 없으면 기본 위치로 초기화 (경로는 안 그림)
            initializeRoute(null)
            return
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
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

                        // 처음 위치 잡았을 때 한 번만 경로 탐색 로직 실행
                        if (!isRouteInitialized) {
                            tvStart.text = "출발지: 내 위치"
                            initializeRoute(location)
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

        val sLat = location?.latitude ?: 37.5665
        val sLon = location?.longitude ?: 126.9780
        val startPoint = TMapPoint(sLat, sLon)

        // [수정 3] 목적지(destName)가 있을 때만 경로 탐색 실행
        // 목적지가 없으면 지도 중심만 내 위치로 옮기고 종료
        if (destName != null) {
            searchDestinationAndDrawRoute(startPoint, destName!!)
        } else {
            runOnUiThread {
                tMapView.setCenterPoint(sLon, sLat)
                // Toast.makeText(this, "목적지가 없어 현재 위치만 표시합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchDestinationAndDrawRoute(startPoint: TMapPoint, keyword: String) {
        val tmapData = TMapData()

        // ⭐ [핵심 수정] findAllPOI 결과는 백그라운드 스레드에서 옴
        tmapData.findAllPOI(keyword, object : TMapData.OnFindAllPOIListener {
            override fun onFindAllPOI(poiList: ArrayList<TMapPOIItem>?) {

                // ⭐ 여기서 UI 쓰레드로 전환하지 않으면 앱이 터짐 (특히 drawPolyLine 호출 시)
                runOnUiThread {
                    if (!poiList.isNullOrEmpty()) {
                        val poi = poiList[0]
                        destLat = poi.poiPoint.latitude
                        destLon = poi.poiPoint.longitude

                        val endPoint = TMapPoint(destLat, destLon)

                        // 이제 안전하게 경로를 그릴 수 있음 (변수 충돌/스레드 문제 해결)
                        drawPolyLine(startPoint, endPoint)
                        fetchCarRouteInfo(startPoint, endPoint)
                        fetchWalkRouteInfo(startPoint, endPoint)

                        zoomToSpan(startPoint.latitude, startPoint.longitude, destLat, destLon)

                    } else {
                        Log.e("RouteActivity", "목적지 검색 실패: $keyword")
                        Toast.makeText(this@RouteActivity, "목적지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // RouteActivity.kt 내부의 drawPolyLine 함수 수정

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

                        // ⭐ [수정] UI 작업은 반드시 Main Thread에서 실행해야 함!
                        runOnUiThread {
                            tMapView.addTMapPolyLine(it)
                        }
                    }
                }
            }
        )
    }

    private fun zoomToSpan(lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
        // 이미 위에서 runOnUiThread로 감싸져서 호출되지만, 안전을 위해 유지
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

                    // 텍스트뷰 업데이트
                    tvTimeTaxi.text = "${time}분"
                    tvDetailDist.text = "${dist}km\n택시 약 ${String.format("%,d", fare)}원"
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
                    tvTimeWalk.text = "${time}분"
                    tvTimeTransit.text = "${(time * 0.4).toInt()}분"
                }
            }
            override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) {
                Log.e("Route", "Walk API Fail: ${t.message}")
            }
        })
    }

    private fun startTMapNavigation() {
        if (destName == null || destLat == 0.0) {
            Toast.makeText(this, "목적지가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

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