package com.moimiApp.moimi

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine

class RouteDetailActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private lateinit var tvTitle: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML 파일명이 activity_route_detail_taxi인지 activity_route_detail인지 확인하세요!
        // 여기서는 detail로 가정합니다.
        setContentView(R.layout.activity_route_detail)
        setupDrawer()

        tvTitle = findViewById(R.id.tv_detail_title)
        tvTime = findViewById(R.id.tv_detail_time)
        tvDistance = findViewById(R.id.tv_detail_distance)

        val mapContainer = findViewById<ViewGroup>(R.id.map_container_detail)

        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapApiKey)

        tMapView.setOnMapReadyListener {
            val startPoint = TMapPoint(37.5665, 126.9780)
            val endPoint = TMapPoint(37.4979, 127.0276)
            tMapView.setCenterPoint(startPoint.longitude, startPoint.latitude)
            tMapView.zoomLevel = 13

            drawRoute(startPoint, endPoint)

            runOnUiThread {
                tvTitle.text = "서울 시청 ➔ 강남역"
                tvTime.text = "25분"
                tvDistance.text = "9.5km\n약 12,000원"
            }
        }
        mapContainer.addView(tMapView)
    }

    private fun drawRoute(start: TMapPoint, end: TMapPoint) {
        val polyLine = TMapPolyLine()
        polyLine.lineColor = Color.RED
        polyLine.lineWidth = 10f
        polyLine.addLinePoint(start)
        polyLine.addLinePoint(TMapPoint(37.5384, 127.0025))
        polyLine.addLinePoint(end)
        tMapView.addTMapPolyLine(polyLine)
    }
}