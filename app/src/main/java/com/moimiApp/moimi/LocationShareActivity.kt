package com.moimiApp.moimi // ⚠️ 패키지명 확인 (본인 프로젝트 패키지명으로!)

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// ❌ 네이버 지도 import 구문 삭제 (또는 주석 처리)
// import com.naver.maps.map.NaverMap
// import com.naver.maps.map.OnMapReadyCallback

class LocationShareActivity : AppCompatActivity() { // , OnMapReadyCallback 제거

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share) // XML 파일 이름 확인!

        // ❌ 네이버 지도 관련 코드 삭제 (또는 주석 처리)
        /*
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }
        mapFragment.getMapAsync(this)
        */

        // TODO: 추후 Tmap이나 구글 맵으로 위치 공유 기능 구현 예정
    }

    // ❌ onMapReady 함수 삭제 (또는 주석 처리)
    /*
    override fun onMapReady(naverMap: NaverMap) {
        // ... 지도 설정 코드 ...
    }
    */
}