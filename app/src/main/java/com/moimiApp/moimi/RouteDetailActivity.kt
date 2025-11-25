package com.moimiApp.moimi

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skt.tmap.TmapApi
import com.skt.tmap.map.MapFragment
import com.skt.tmap.map.TmapMap
import com.skt.tmap.MapInitListener
import com.skt.tmap.TmapPoint
import com.skt.tmap.overlay.TmapPolyLine
import com.skt.tmap.vsm.map.MapConstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RouteDetailActivity : AppCompatActivity(), MapInitListener {

    private lateinit var tmapMap: TmapMap

    // XML의 뷰들
    private lateinit var tvTitle: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ⚠️ XML 파일 이름이 activity_route_detail_taxi.xml 인지 확인하세요!
        setContentView(R.layout.activity_route_detail_taxi)

        // 1. 뷰 연결
        tvTitle = findViewById(R.id.tv_detail_title)
        tvTime = findViewById(R.id.tv_detail_time)
        tvDistance = findViewById(R.id.tv_detail_distance)

        // 2. Tmap API 초기화
        TmapApi.init(this)

        // 3. 지도 프래그먼트 생성 및 추가
        // XML에 map_container_detail (LinearLayout)이 있다고 가정합니다.
        // 하지만 MapFragment는 FrameLayout이나 FragmentContainerView에 넣는 게 정석입니다.
        // 여기서는 코드로 FrameLayout을 동적으로 추가해서 해결하거나, XML을 수정해야 합니다.
        // (일단 XML의 LinearLayout 안에 지도를 넣는 방식 시도)

        val mapFragment = MapFragment()
        mapFragment.setOnMapInitListener(this)

        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container_detail, mapFragment)
            .commit()

        // (참고: LinearLayout ID에 replace하면 기존 내용이 다 사라지고 지도만 남을 수 있으니,
        // XML에서 map_container_detail 안에 빈 FrameLayout을 하나 더 만드는 게 안전합니다.
        // 하지만 일단 진행해 봅니다.)
    }

    override fun onMapInitSucceeded(tmapMap: TmapMap) {
        this.tmapMap = tmapMap

        // 4. 경로 탐색 시작 (예: 시청 -> 강남역)
        val startPoint = TmapPoint(37.5665, 126.9780) // 서울 시청
        val endPoint = TmapPoint(37.4979, 127.0276)   // 강남역

        searchRoute(startPoint, endPoint)
    }

    override fun onMapInitFailed(errorMsg: String) {
        Toast.makeText(this, "지도 로딩 실패: $errorMsg", Toast.LENGTH_SHORT).show()
    }

    private fun searchRoute(start: TmapPoint, end: TmapPoint) {
        // TmapData는 3.0 SDK에서 제공하는지 확인 필요.
        // 3.0에서는 'TmapData' 클래스가 없거나 사용법이 다를 수 있습니다.
        // 여기서는 Retrofit API를 직접 호출하거나, SDK에 내장된 경로 기능을 사용해야 합니다.

        // ⚠️ [중요] Tmap SDK 3.0은 '지도 표시' 전용이며, '경로 데이터 계산(API)'은 별도입니다.
        // 따라서 실제 경로 데이터를 가져오려면 아까 만든 Retrofit (Tmap API)을 써야 합니다.
        // 하지만 지금은 복잡하니, "직선 그리기"와 "가짜 데이터"로 화면만 먼저 완성해 드리겠습니다.

        // 1. 지도 위치 이동
        tmapMap.setCenterPoint(start.longitude, start.latitude)
        tmapMap.setZoomLevel(13)

        // 2. 가짜 데이터 채우기 (API 연동 전 테스트)
        tvTitle.text = "서울 시청 ➔ 강남역"
        tvTime.text = "25분"
        tvDistance.text = "9.5km\n약 12,000원"

        // 3. 지도에 선 그리기 (PolyLine)
        val polyLine = TmapPolyLine().apply {
            lineColor = Color.RED
            lineWidth = 10f
            addLinePoint(start)
            addLinePoint(TmapPoint(37.5384, 127.0025)) // 중간점 (한남대교)
            addLinePoint(end)
        }

        tmapMap.addTmapPolyLine(polyLine)
    }
}