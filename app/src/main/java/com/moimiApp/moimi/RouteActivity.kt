package com.moimiApp.moimi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine
import com.skt.tmap.poi.TMapPOIItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.sqrt

class RouteActivity : BaseActivity() {

    private var tMapView: TMapView? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private val tmapData by lazy { TMapData() }

    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private lateinit var tvTotalInfo: TextView

    private lateinit var btnTaxi: TextView
    private lateinit var btnTransit: TextView
    private lateinit var btnWalk: TextView

    private lateinit var rvRouteSteps: RecyclerView
    private lateinit var cardMap: View

    private lateinit var btnNavi: MaterialButton
    private lateinit var layoutNaviControl: LinearLayout
    private lateinit var btnStopNavi: MaterialButton
    private lateinit var btnGoMain: MaterialButton

    private var startLat: Double = 37.5665
    private var startLon: Double = 126.9780
    private var startName: String = "내 위치"

    private var destLat: Double = 0.0
    private var destLon: Double = 0.0
    private var destName: String = "도착지 설정 필요"
    private var destTitle: String? = null

    private var currentMode = "TAXI"
    private var isSelectingStart = false
    private var isTrackingMode = false // 현재 안내 중인지 상태

    private var locationMarkerBitmap: Bitmap? = null

    private val autoTrackingHandler = Handler(Looper.getMainLooper())
    private val autoTrackingRunnable = Runnable {
        if (btnNavi.visibility == View.GONE) { // 안내 중일 때만
            Log.d("RouteActivity", "⏰ 5초 타이머: 시점 자동 복귀")
            isTrackingMode = true
            if (startLat != 0.0 && startLon != 0.0) {
                tMapView?.setCenterPoint(startLon, startLat, true)
                tMapView?.zoomLevel = 18
                Toast.makeText(this@RouteActivity, "내 위치로 시점이 복귀됩니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val name = result.data?.getStringExtra("locationName") ?: ""
            val lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lon = result.data?.getDoubleExtra("lon", 0.0) ?: 0.0

            if (lat != 0.0 && lon != 0.0) {
                if (isSelectingStart) {
                    startName = name
                    startLat = lat
                    startLon = lon
                    tvStart.text = "출발: $startName"
                } else {
                    destName = name
                    destLat = lat
                    destLon = lon
                    tvEnd.text = "도착: $destName"
                }
                refreshRoute()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_main)
        setupDrawer()

        try {
            locationMarkerBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_location)
        } catch (e: Exception) {
            Log.e("RouteActivity", "비트맵 로드 실패", e)
        }

        val intentName = intent.getStringExtra("destName")
        val intentTitle = intent.getStringExtra("destTitle")

        if (!intentName.isNullOrEmpty()) {
            destName = intentTitle ?: intentName
            destTitle = intentTitle
        }

        initViews()
        setupListeners()

        val mapContainer = findViewById<FrameLayout>(R.id.map_container)
        mapContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mapContainer.width > 0 && mapContainer.height > 0) {
                    mapContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    initTMapActual(mapContainer)
                }
            }
        })
    }

    private fun initViews() {
        tvStart = findViewById(R.id.tv_route_start)
        tvEnd = findViewById(R.id.tv_route_end)
        tvTotalInfo = findViewById(R.id.tv_total_info)

        btnTaxi = findViewById(R.id.btn_mode_taxi)
        btnTransit = findViewById(R.id.btn_mode_transit)
        btnWalk = findViewById(R.id.btn_mode_walk)

        rvRouteSteps = findViewById(R.id.rv_route_steps)
        cardMap = findViewById(R.id.card_map)

        btnNavi = findViewById(R.id.btn_start_navi)
        layoutNaviControl = findViewById(R.id.layout_navi_control)
        btnStopNavi = findViewById(R.id.btn_stop_navi)
        btnGoMain = findViewById(R.id.btn_go_main)

        tvStart.text = "출발: $startName"
        tvEnd.text = "도착: $destName"

        btnNavi.text = "안내 시작 (따라가기)"
        btnNavi.visibility = View.VISIBLE
        layoutNaviControl.visibility = View.GONE

        findViewById<ImageView>(R.id.btn_home_logo).setOnClickListener { finish() }
    }

    private fun setupListeners() {
        btnTaxi.setOnClickListener { changeMode("TAXI") }
        btnTransit.setOnClickListener { changeMode("TRANSIT") }
        btnWalk.setOnClickListener { changeMode("WALK") }

        findViewById<View>(R.id.tv_route_start).setOnClickListener {
            isSelectingStart = true
            openSearch()
        }

        val openDestSearch = View.OnClickListener {
            isSelectingStart = false
            openSearch()
        }
        findViewById<View>(R.id.tv_route_end).setOnClickListener(openDestSearch)
        findViewById<View>(R.id.card_locations).setOnClickListener(openDestSearch)

        btnNavi.setOnClickListener {
            if (destLat == 0.0) {
                Toast.makeText(this, "도착지를 설정해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startNavigationMode()
        }

        btnStopNavi.setOnClickListener {
            stopNavigationMode()
        }

        // ✅ [로그 추가] 메인으로 버튼 클릭 시
        btnGoMain.setOnClickListener {
            Log.e("RouteActivity", "🔙 메인으로 돌아갑니다. isTracking=$isTrackingMode")
            val intent = Intent()
            intent.putExtra("isTracking", isTrackingMode)
            intent.putExtra("destLat", destLat)
            intent.putExtra("destLon", destLon)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun startNavigationMode() {
        isTrackingMode = true
        btnNavi.visibility = View.GONE
        layoutNaviControl.visibility = View.VISIBLE
        Toast.makeText(this, "경로 안내를 시작합니다.", Toast.LENGTH_SHORT).show()

        tMapView?.zoomLevel = 18
        tMapView?.setCenterPoint(startLon, startLat, true)
    }

    private fun stopNavigationMode() {
        isTrackingMode = false
        btnNavi.visibility = View.VISIBLE
        layoutNaviControl.visibility = View.GONE
        btnNavi.text = "안내 시작 (따라가기)"
        try {
            btnNavi.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF8989"))
        } catch (e: Exception) {}
        Toast.makeText(this, "안내를 종료합니다.", Toast.LENGTH_SHORT).show()

        refreshRoute()
    }

    private fun openSearch() {
        val intent = Intent(this, SearchLocationActivity::class.java)
        searchLauncher.launch(intent)
    }

    private fun changeMode(mode: String) {
        currentMode = mode
        val inactiveBg = R.drawable.bg_input_rounded
        val activeBg = R.drawable.bg_button_red
        val inactiveColor = ContextCompat.getColor(this, R.color.black)
        val activeColor = Color.WHITE

        btnTaxi.setBackgroundResource(if (mode == "TAXI") activeBg else inactiveBg)
        btnTaxi.setTextColor(if (mode == "TAXI") activeColor else inactiveColor)

        btnTransit.setBackgroundResource(if (mode == "TRANSIT") activeBg else inactiveBg)
        btnTransit.setTextColor(if (mode == "TRANSIT") activeColor else inactiveColor)

        btnWalk.setBackgroundResource(if (mode == "WALK") activeBg else inactiveBg)
        btnWalk.setTextColor(if (mode == "WALK") activeColor else inactiveColor)

        stopNavigationMode()
        refreshRoute()
    }

    private fun refreshRoute() {
        if (destLat == 0.0 || destLon == 0.0) return
        val start = TMapPoint(startLat, startLon)
        val end = TMapPoint(destLat, destLon)

        when (currentMode) {
            "TAXI" -> {
                cardMap.visibility = View.VISIBLE
                rvRouteSteps.visibility = View.GONE
                drawPolyLine(start, end, TMapData.TMapPathType.CAR_PATH)
                fetchRouteInfo(start, end)
            }
            "WALK" -> {
                cardMap.visibility = View.VISIBLE
                rvRouteSteps.visibility = View.GONE
                drawPolyLine(start, end, TMapData.TMapPathType.PEDESTRIAN_PATH)
                fetchWalkInfo(start, end)
            }
            "TRANSIT" -> {
                cardMap.visibility = View.VISIBLE
                rvRouteSteps.visibility = View.VISIBLE
                tMapView?.removeAllTMapPolyLine()
            }
        }
    }

    private fun drawPolyLine(start: TMapPoint, end: TMapPoint, type: TMapData.TMapPathType) {
        Thread {
            try {
                tmapData.findPathDataWithType(type, start, end, object : TMapData.OnFindPathDataWithTypeListener {
                    override fun onFindPathDataWithType(polyLine: TMapPolyLine?) {
                        polyLine?.let {
                            it.lineColor = if (type == TMapData.TMapPathType.CAR_PATH)
                                Color.parseColor("#4285F4")
                            else
                                Color.parseColor("#34A853")
                            it.lineWidth = 14f

                            runOnUiThread {
                                try {
                                    tMapView?.removeAllTMapPolyLine()
                                    tMapView?.addTMapPolyLine(it)
                                    if (!isTrackingMode) {
                                        zoomToSpan(start.latitude, start.longitude, end.latitude, end.longitude)
                                    }
                                } catch (e: Exception) {
                                    Log.e("RouteActivity", "PolyLine Drawing Error", e)
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) { Log.e("RouteActivity", "FindPath SDK Error", e) }
        }.start()
    }

    private fun fetchRouteInfo(start: TMapPoint, end: TMapPoint) {
        val request = RouteRequest(startX = start.longitude, startY = start.latitude, endX = end.longitude, endY = end.latitude, totalValue = 2)
        TmapClient.instance.getRoute(tMapApiKey, request).enqueue(object : Callback<TmapRouteResponse> {
            override fun onResponse(call: Call<TmapRouteResponse>, response: Response<TmapRouteResponse>) {
                val props = response.body()?.features?.firstOrNull()?.properties
                props?.let {
                    val timeMin = (it.totalTime ?: 0) / 60
                    val fare = it.taxiFare ?: 0
                    tvTotalInfo.text = "${timeMin}분  |  택시 약 ${String.format("%,d", fare)}원"
                }
            }
            override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) {}
        })
    }

    private fun fetchWalkInfo(start: TMapPoint, end: TMapPoint) {
        val request = RouteRequest(startX = start.longitude, startY = start.latitude, endX = end.longitude, endY = end.latitude, startName = "출발", endName = "도착")
        TmapClient.instance.getPedestrianRoute(tMapApiKey, request).enqueue(object : Callback<TmapRouteResponse> {
            override fun onResponse(call: Call<TmapRouteResponse>, response: Response<TmapRouteResponse>) {
                val props = response.body()?.features?.firstOrNull()?.properties
                props?.let {
                    val timeMin = (it.totalTime ?: 0) / 60
                    tvTotalInfo.text = "${timeMin}분  |  0원"
                }
            }
            override fun onFailure(call: Call<TmapRouteResponse>, t: Throwable) {}
        })
    }

    private fun zoomToSpan(lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
        try {
            val centerLat = (lat1 + lat2) / 2
            val centerLon = (lon1 + lon2) / 2
            tMapView?.setCenterPoint(centerLon, centerLat)
            val distance = sqrt(Math.pow(lat1 - lat2, 2.0) + Math.pow(lon1 - lon2, 2.0))
            tMapView?.zoomLevel = when {
                distance < 0.01 -> 15
                distance < 0.05 -> 13
                distance < 0.1 -> 11
                else -> 9
            }
        } catch (e: Exception) { Log.e("RouteActivity", "Zoom Error", e) }
    }

    private fun initTMapActual(container: FrameLayout) {
        try {
            container.removeAllViews()
            tMapView = TMapView(this)
            tMapView?.setSKTMapApiKey(tMapApiKey)
            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            container.addView(tMapView, params)

            tMapView?.setOnTouchListener { v, event ->
                if (btnNavi.visibility == View.GONE) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            isTrackingMode = false
                            autoTrackingHandler.removeCallbacks(autoTrackingRunnable)
                            v.parent.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent.requestDisallowInterceptTouchEvent(false)
                            autoTrackingHandler.removeCallbacks(autoTrackingRunnable)
                            autoTrackingHandler.postDelayed(autoTrackingRunnable, 5000L)
                        }
                    }
                }
                false
            }

            tMapView?.setOnMapReadyListener {
                tMapView?.zoomLevel = 15
                tMapView?.setCenterPoint(startLon, startLat)
                startTrackingMyLocation()
                val intentDest = intent.getStringExtra("destName")
                if (!intentDest.isNullOrEmpty() && destLat == 0.0) {
                    searchDestinationByName(intentDest)
                }
            }
        } catch (e: Exception) { Log.e("RouteActivity", "TMap Error", e) }
    }

    private fun startTrackingMyLocation() {
        if (locationManager == null) locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationListener == null) {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    startLat = location.latitude
                    startLon = location.longitude
                    runOnUiThread {
                        if (tMapView != null) {
                            val marker = TMapMarkerItem().apply {
                                id = "my_location_route"
                                setTMapPoint(TMapPoint(startLat, startLon))
                                if (locationMarkerBitmap != null) icon = locationMarkerBitmap
                                setPosition(0.5f, 1.0f)
                            }
                            tMapView?.removeTMapMarkerItem("my_location_route")
                            tMapView?.addTMapMarkerItem(marker)

                            if (isTrackingMode) {
                                tMapView?.setCenterPoint(startLon, startLat, true)
                            }
                        }
                    }
                }
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }
        }
        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, locationListener!!)
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 2f, locationListener!!)
        } catch (e: SecurityException) { }
    }

    private fun searchDestinationByName(keyword: String) {
        val tmapData = TMapData()
        tmapData.findAllPOI(keyword, object : TMapData.OnFindAllPOIListener {
            override fun onFindAllPOI(poiList: ArrayList<TMapPOIItem>?) {
                runOnUiThread {
                    if (!poiList.isNullOrEmpty()) {
                        val poi = poiList[0]
                        destLat = poi.poiPoint.latitude
                        destLon = poi.poiPoint.longitude
                        if (destName.contains("설정 필요")) {
                            destName = poi.poiName
                            tvEnd.text = "도착: $destName"
                        }
                        changeMode("TAXI")
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.let { locationManager?.removeUpdates(it) }
        try {
            val mapContainer = findViewById<FrameLayout>(R.id.map_container)
            mapContainer?.removeAllViews()
            tMapView = null
            autoTrackingHandler.removeCallbacks(autoTrackingRunnable)
        } catch (e: Exception) {}
    }

    data class RouteStep(val title: String, val detail: String, val iconRes: Int)

    inner class RouteStepAdapter(private val steps: List<RouteStep>) : RecyclerView.Adapter<RouteStepAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_step_title)
            val tvDetail: TextView = view.findViewById(R.id.tv_step_detail)
            val ivIcon: ImageView = view.findViewById(R.id.iv_step_icon)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route_step, parent, false)
            return Holder(view)
        }
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = steps[position]
            holder.tvTitle.text = item.title
            holder.tvDetail.text = item.detail
            holder.ivIcon.setImageResource(item.iconRes)
        }
        override fun getItemCount() = steps.size
    }
}