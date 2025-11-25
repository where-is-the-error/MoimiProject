package com.moimiApp.moimi // ⚠️ 패키지명 확인

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource

class MapDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_detail) // XML 파일 이름 확인!

        // 1. 네이버 지도가 제공하는 '위치 소스' 생성
        // (이게 권한 요청이랑 위치 갱신을 알아서 처리해줍니다)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        // 2. 지도 객체 가져오기
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }

        // 3. 지도가 준비되면 onMapReady 호출
        mapFragment.getMapAsync(this)
    }

    // 지도가 로딩 완료되면 실행되는 함수
    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap

        // 4. 지도에 위치 소스 연결
        naverMap.locationSource = locationSource

        // 5. 위치 추적 모드 켜기 (파란 점 + 카메라 이동)
        // Follow: 위치를 따라 카메라가 이동함
        // Face: 위치랑 방위(나침반)까지 따라감
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        // 6. UI 설정 (현위치 버튼 등)
        naverMap.uiSettings.isLocationButtonEnabled = true
    }

    // 권한 요청 결과 처리 (네이버 locationSource에게 토스)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}