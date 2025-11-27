package com.moimiApp.moimi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.Toast
// ✅ [중요] T Map 관련 임포트 (이것들만 있으면 됩니다)
import com.skt.tmap.TMapView
import com.skt.tmap.TMapPoint
import com.skt.tmap.overlay.TMapPolyLine

class RouteActivity : BaseActivity() { // MapInitListener 제거 (필요 없음)

    private lateinit var tMapView: TMapView
    private val tMapKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_main)

        // 1. 공통 메뉴 연결
        setupDrawer()

        // 2. 지도 띄우기 (MainActivity와 같은 방식)
        initTMap()

        // 3. 택시 버튼 클릭 시 -> 상세 화면 이동
        val btnTaxi = findViewById<LinearLayout>(R.id.layout_btn_taxi)
        btnTaxi.setOnClickListener {
            val intent = Intent(this, RouteDetailActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initTMap() {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container)

        // TMapView 생성 및 설정
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)
        mapContainer.addView(tMapView)

        // 지도가 준비되면 실행
        tMapView.setOnMapReadyListener {
            // 1. 중심점 이동 (고척돔)
            tMapView.setCenterPoint(126.8671, 37.4982) // 경도, 위도 순서 주의
            tMapView.zoomLevel = 14

            // 2. 경로선 그리기 (파란색)
            val startPoint = TMapPoint(37.4982, 126.8671) // 고척돔
            val endPoint = TMapPoint(37.5020, 126.8780)   // 안양천

            val polyLine = TMapPolyLine()
            polyLine.lineColor = Color.BLUE
            polyLine.lineWidth = 10f
            polyLine.addLinePoint(startPoint)
            polyLine.addLinePoint(endPoint)

            // 지도에 선 추가 (ID, 선 객체)
            tMapView.addTMapPolyLine( polyLine)
        }
    }
}