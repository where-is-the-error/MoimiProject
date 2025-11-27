package com.moimiApp.moimi

// 3.0 버전 Import (빨간줄 뜨면 Alt+Enter로 다시 잡으세요)
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine

class RouteActivity : AppCompatActivity() {

    // 3.0 방식: TMapView 선언
    private lateinit var tMapView: TMapView

    // XML의 뷰들
    private lateinit var tvTitle: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚠️ 주의: XML 파일명이 맞는지 꼭 확인하세요.
        // RouteActivity라면 activity_route.xml 일 수도 있습니다.
        // 여기서는 기존 코드대로 'activity_route_detail_taxi'를 유지합니다.
        setContentView(R.layout.activity_route_detail_taxi)

        // 1. 뷰 연결
        tvTitle = findViewById(R.id.tv_detail_title)
        tvTime = findViewById(R.id.tv_detail_time)
        tvDistance = findViewById(R.id.tv_detail_distance)

        // 2. 지도를 넣을 컨테이너 연결 (XML에 있는 ID)
        val mapContainer = findViewById<ViewGroup>(R.id.map_container_detail)

        // 3. TMapView 생성 및 API 키 설정 (SDK 3.0 필수)
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey("여기에_발급받은_TMAP_API_KEY를_넣으세요") // 👈 본인 키 입력 필수!

        // 4. 컨테이너에 지도 뷰 추가
        mapContainer.addView(tMapView)

        // 5. 지도가 준비되면 할 일 (리스너)
        tMapView.setOnMapReadyListener {
            // 지도가 로딩된 후 경로 탐색 로직 실행
            val startPoint = TMapPoint(37.5665, 126.9780) // 서울 시청
            val endPoint = TMapPoint(37.4979, 127.0276)   // 강남역

            drawRoute(startPoint, endPoint)
        }
    }

    private fun drawRoute(start: TMapPoint, end: TMapPoint) {
        // 1. 지도 중심점 및 줌 설정
        tMapView.setCenterPoint(start.longitude, start.latitude)
        tMapView.zoomLevel = 13

        // 2. 가짜 데이터 채우기 (테스트용)
        tvTitle.text = "서울 시청 ➔ 강남역"
        tvTime.text = "25분"
        tvDistance.text = "9.5km\n약 12,000원"

        // 3. 지도에 경로 선 그리기 (TMapPolyLine - 대문자 M)
        val polyLine = TMapPolyLine()
        polyLine.lineColor = Color.RED
        polyLine.lineWidth = 10f

        // 경로 포인트 추가
        polyLine.addLinePoint(start)
        polyLine.addLinePoint(TMapPoint(37.5384, 127.0025)) // 중간 경유지
        polyLine.addLinePoint(end)

        // 지도에 선 추가 (식별 ID, 선 객체)
        //tMapView.addTMapPolyLine("route_line_demo", polyLine)
    }
}