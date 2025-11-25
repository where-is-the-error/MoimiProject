package com.moimiApp.moimi

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.skt.tmap.TMapView // 3.0 AAR에 포함된 클래스

class RouteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        val container = findViewById<FrameLayout>(R.id.tmap_container)

        // 1. TMapView 생성
        val tMapView = TMapView(this)

        // 2. API 키 설정
        tMapView.setSKTMapApiKey("QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL")

        // 3. 지도 설정
        tMapView.setOnMapReadyListener {
            // 지도 로딩 완료 시 실행
            tMapView.setCenterPoint(126.9780, 37.5665) // 서울 시청
            tMapView.zoomLevel = 15
        }

        // 4. 화면에 붙이기
        container.addView(tMapView)
    }
}