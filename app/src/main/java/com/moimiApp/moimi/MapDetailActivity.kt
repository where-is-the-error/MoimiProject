package com.moimiApp.moimi // ⚠️ 패키지명 확인

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skt.tmap.TMapView

class MapDetailActivity : AppCompatActivity() {

    private lateinit var tMapView: TMapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_detail)

        // 1. 지도가 들어갈 컨테이너 찾기
        val container = findViewById<FrameLayout>(R.id.tmap_container)

        // 2. 티맵 뷰 생성
        tMapView = TMapView(this)

        // 3. API 키 설정 (Manifest에 넣었지만, 코드에서도 확인 사살)
        tMapView.setSKTMapApiKey("QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL")

        // 4. 지도 설정 및 리스너
        tMapView.setOnMapReadyListener {
            // 지도가 로딩되면 실행되는 곳
            Toast.makeText(this, "티맵 로딩 완료!", Toast.LENGTH_SHORT).show()

            // 예: 서울 시청으로 이동 (위도, 경도)
            tMapView.setCenterPoint(126.9780, 37.5665)
            tMapView.zoomLevel = 15
        }

        // 5. 화면에 지도 추가 (이 코드가 있어야 화면에 보임!)
        container.addView(tMapView)
    }
}