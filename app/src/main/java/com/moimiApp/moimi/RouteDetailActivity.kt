package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.LinearLayout
import com.skt.Tmap.TMapMarkerItem
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapView

class RouteDetailActivity : BaseActivity() { // OnMapReadyCallback 제거됨

    private lateinit var tMapView: TMapView
    private val tMapKey = "여기에_발급받은_TMap_AppKey_입력" // ⚠️ T Map Key 설정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail_taxi)

        // [BaseActivity 기능] 상단바, 햄버거 메뉴 연결
        setupDrawer()

        // ----------------------------------------------------
        // [T Map 지도 초기화]
        // ----------------------------------------------------

        // 1. XML 컨테이너에 TMapView 추가
        val mapContainer = findViewById<LinearLayout>(R.id.map_container_detail)

        // 2. TMapView 객체 생성 및 설정
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)

        // 3. TMapView를 컨테이너에 추가
        mapContainer.addView(tMapView)

        // 4. 지도 기능 설정
        setupTMap()

        // [길 안내 시작 버튼 기능 구현]
        val btnStartNav = findViewById<Button>(R.id.btn_start_navigation)

        btnStartNav.setOnClickListener {
            Toast.makeText(this, "T Map 길 안내를 시작합니다!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupTMap() {
        // 예시 좌표: 상세 구간 위치 (임의 좌표)
        val tMapPoint = TMapPoint(37.4982, 126.8671)

        // 카메라 이동 및 줌 레벨 설정
        tMapView.setCenterPoint(tMapPoint.longitude, tMapPoint.latitude)
        tMapView.zoomLevel = 15

        // 마커 찍기
        val marker = TMapMarkerItem()
        marker.tMapPoint = tMapPoint
        marker.name = "상세 구간"
        tMapView.addMarkerItem("detail_marker_1", marker)
    }

}