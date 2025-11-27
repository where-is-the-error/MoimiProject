package com.moimiApp.moimi

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine

class RouteDetailActivity : AppCompatActivity() {

    // 3.0에서는 TMapView가 메인입니다.
    private lateinit var tMapView: TMapView

    // XML의 뷰들
    private lateinit var tvTitle: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail_taxi)

        // 1. 뷰 연결
        tvTitle = findViewById(R.id.tv_detail_title)
        tvTime = findViewById(R.id.tv_detail_time)
        tvDistance = findViewById(R.id.tv_detail_distance)

        // 지도를 넣을 컨테이너 (XML에 있는 LinearLayout 혹은 FrameLayout)
        // findViewById<ViewGroup>을 쓰면 LinearLayout이든 FrameLayout이든 다 됩니다.
        val mapContainer = findViewById<ViewGroup>(R.id.map_container_detail)

        // 2. TMapView 생성 및 API 키 설정 (SDK 3.0 방식)
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey("QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTih") // 👈 꼭 넣어야 함!

        // 3. 컨테이너에 지도 뷰 추가
        mapContainer.addView(tMapView)

        // 4. 지도 로딩 완료 후 작업
        tMapView.setOnMapReadyListener {
            // 지도가 준비되면 경로를 그립니다.
            val startPoint = TMapPoint(37.5665, 126.9780) // 서울 시청
            val endPoint = TMapPoint(37.4979, 127.0276)   // 강남역

            drawRoute(startPoint, endPoint)
        }
    }

    private fun drawRoute(start: TMapPoint, end: TMapPoint) {
        // 1. 지도 중심점 이동 (주의: TMap은 setCenterPoint에 '경도(lon), 위도(lat)' 순서로 넣는 경우가 많음)
        // 하지만 3.0 일부 버전에서는 lat, lon일 수도 있으니 화면이 이상하면 순서를 바꿔보세요.
        tMapView.setCenterPoint(start.longitude, start.latitude)
        tMapView.zoomLevel = 13

        // 2. 가짜 데이터 채우기 (화면 표시용)
        tvTitle.text = "서울 시청 ➔ 강남역"
        tvTime.text = "25분"
        tvDistance.text = "9.5km\n약 12,000원"

        // 3. 지도에 선 그리기 (TMapPolyLine - 대문자 M 주의)
        val polyLine = TMapPolyLine()
        polyLine.lineColor = Color.RED
        polyLine.lineWidth = 10f

        // 경로 포인트 추가
        polyLine.addLinePoint(start)
        polyLine.addLinePoint(TMapPoint(37.5384, 127.0025)) // 중간점 (한남대교)
        polyLine.addLinePoint(end)

        // 지도에 선 추가 (ID를 지정해야 함)
       // tMapView.addTMapPolyLine("route_line_1", polyLine)

        // 시작점과 도착점에 마커를 찍고 싶다면 TMapMarkerItem을 사용하면 됩니다 (선택 사항)
    }
}