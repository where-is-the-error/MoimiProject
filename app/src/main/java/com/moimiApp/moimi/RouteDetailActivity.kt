package com.moimiApp.moimi

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
// ✅ [중요] T Map 관련 임포트
import com.skt.tmap.TMapView
import com.skt.tmap.TMapPoint
import com.skt.tmap.overlay.TMapPolyLine

class RouteDetailActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"

    // 뷰 변수
    private lateinit var tvTitle: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail)

        setupDrawer()

        // 뷰 연결
        tvTitle = findViewById(R.id.tv_detail_title)
        tvTime = findViewById(R.id.tv_detail_time)
        tvDistance = findViewById(R.id.tv_detail_distance)

        // 지도 초기화
        initTMap()
    }

    private fun initTMap() {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_detail)

        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        tMapView.setOnMapReadyListener {
            // 1. 중심점 이동 (서울 시청)
            tMapView.setCenterPoint(126.9780, 37.5665)
            tMapView.zoomLevel = 13

            // 2. 텍스트 업데이트 (UI 스레드)
            runOnUiThread {
                tvTitle.text = "서울 시청 ➔ 강남역"
                tvTime.text = "25분"
                tvDistance.text = "9.5km\n약 12,000원"
            }

            // 3. 경로선 그리기 (빨간색)
            val startPoint = TMapPoint(37.5665, 126.9780)
            val midPoint = TMapPoint(37.5384, 127.0025) // 경유지
            val endPoint = TMapPoint(37.4979, 127.0276)

            val polyLine = TMapPolyLine()
            polyLine.lineColor = Color.RED
            polyLine.lineWidth = 10f

            polyLine.addLinePoint(startPoint)
            polyLine.addLinePoint(midPoint)
            polyLine.addLinePoint(endPoint)

            tMapView.addTMapPolyLine( polyLine)
        }
    }
}