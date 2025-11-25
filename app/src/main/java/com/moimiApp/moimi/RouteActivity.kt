package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Toast
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker

// 1. OnMapReadyCallback을 추가해서 지도를 쓸 준비를 합니다.
class RouteActivity : BaseActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_main)

        // [BaseActivity 기능] 상단바, 햄버거 메뉴 연결
        setupDrawer()

        // [지도 연결] XML에 있는 map_fragment_main을 가져옵니다.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment_main) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.map_fragment_main, it).commit()
            }

        // 지도가 준비되면 onMapReady 함수를 호출해달라고 요청합니다.
        mapFragment.getMapAsync(this)
    }

    // [지도 준비 완료] 지도가 화면에 뜨면 이 함수가 실행됩니다.
    override fun onMapReady(naverMap: NaverMap) {
        // 예시 좌표: 고척스카이돔 (37.498, 126.867)
        val coord = LatLng(37.4982, 126.8671)

        // 1. 카메라 이동 (해당 좌표로 줌인)
        val cameraUpdate = CameraUpdate.scrollTo(coord)
        naverMap.moveCamera(cameraUpdate)

        // 2. 마커 찍기 (출발지 표시)
        val marker = Marker()
        marker.position = coord
        marker.captionText = "출발: 고척스카이돔"
        marker.map = naverMap

        Toast.makeText(this, "경로 요약 지도가 로드되었습니다.", Toast.LENGTH_SHORT).show()
    }
}

