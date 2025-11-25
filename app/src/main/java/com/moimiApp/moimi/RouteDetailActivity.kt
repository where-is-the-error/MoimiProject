package com.moimiApp.moimi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker

class RouteDetailActivity : BaseActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail_taxi)

        // [BaseActivity 기능] 상단바, 햄버거 메뉴 연결
        setupDrawer()

        // [지도 연결] XML에 있는 map_fragment_detail을 가져옵니다.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment_detail) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.map_fragment_detail, it).commit()
            }
        mapFragment.getMapAsync(this)


        // [길 안내 시작 버튼 기능 구현]
        val btnStartNav = findViewById<Button>(R.id.btn_start_navigation)

        btnStartNav.setOnClickListener {
            // 기능 1: 간단한 안내 메시지 띄우기
            Toast.makeText(this, "길 안내를 시작합니다!", Toast.LENGTH_LONG).show()

        }
    }

    // [지도 준비 완료]
    override fun onMapReady(naverMap: NaverMap) {
        // 예시 좌표: 상세 구간 위치 (임의 좌표)
        val coord = LatLng(37.4982, 126.8671)

        val cameraUpdate = CameraUpdate.scrollTo(coord)
        naverMap.moveCamera(cameraUpdate)

        val marker = Marker()
        marker.position = coord
        marker.captionText = "상세 구간"
        marker.map = naverMap
    }
}