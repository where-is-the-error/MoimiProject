package com.moimiApp.moimi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout // Import 추가
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine

class RouteActivity : BaseActivity() {

    private lateinit var tMapView: TMapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_main)
        setupDrawer()

        val mapContainer = findViewById<ViewGroup>(R.id.map_container)

        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapApiKey)

        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 13
            val startPoint = TMapPoint(37.5665, 126.9780)
            val endPoint = TMapPoint(37.4979, 127.0276)
            tMapView.setCenterPoint(startPoint.longitude, startPoint.latitude)
            drawRoute(startPoint, endPoint)
        }
        mapContainer.addView(tMapView)

        // 택시 버튼 (혹시 XML에 없으면 이 부분에서 에러날 수 있으니 확인 필요)
        val btnTaxi = findViewById<LinearLayout>(R.id.layout_btn_taxi)
        btnTaxi?.setOnClickListener {
            val intent = Intent(this, RouteDetailActivity::class.java)
            startActivity(intent)
        }
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