package com.moimiApp.moimi

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import com.skt.Tmap.TMapMarkerItem
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapView

class RouteActivity : BaseActivity() { // OnMapReadyCallback 제거됨

    private lateinit var tMapView: TMapView
    private val tMapKey = "여기에_발급받은_TMap_AppKey_입력" // ⚠️ T Map Key 설정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_main)

        // [BaseActivity 기능] 상단바, 햄버거 메뉴 연결
        setupDrawer()

        // ----------------------------------------------------
        // [T Map 지도 초기화]
        // ----------------------------------------------------

        // 1. TMapView 객체 생성 및 설정
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)

        // 2. XML 컨테이너에 TMapView 추가
        // map_container ID를 가진 LinearLayout에 지도를 삽입합니다.
        val mapContainer = findViewById<LinearLayout>(R.id.map_container)
        mapContainer.addView(tMapView)

        // 3. 지도 기능 설정
        setupTMap()
    }

    private fun setupTMap() {
        // 예시 좌표: 고척스카이돔 (37.498, 126.867)
        val tMapPoint = TMapPoint(37.4982, 126.8671)

        // 카메라 이동 및 줌 레벨 설정
        tMapView.setCenterPoint(tMapPoint.longitude, tMapPoint.latitude)
        tMapView.zoomLevel = 15

        // 마커 찍기 (출발지 표시)
        val marker = TMapMarkerItem()
        marker.tMapPoint = tMapPoint
        marker.name = "출발: 고척스카이돔"
        tMapView.addMarkerItem("start_marker", marker)

        Toast.makeText(this, "경로 요약 지도가 로드되었습니다. (T Map)", Toast.LENGTH_SHORT).show()
    }

}