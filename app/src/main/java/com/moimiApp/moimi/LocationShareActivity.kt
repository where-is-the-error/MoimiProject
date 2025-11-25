package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback

// 1. BaseActivity 상속 (메뉴 기능)
// 2. OnMapReadyCallback 구현 (지도 기능)
class LocationShareActivity : BaseActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share) // XML 파일 연결

        // [공통 기능] 햄버거 메뉴, 로고, 벨 연결
        setupDrawer()

        // [지도 연결]
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment_location) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.map_fragment_location, it).commit()
            }
        mapFragment.getMapAsync(this)

        // [위치 공유 스위치 기능]
        val switchShare = findViewById<SwitchCompat>(R.id.switch_location_share)
        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show()
                // 실제 위치 공유 로직 (서버 전송 등)
            } else {
                Toast.makeText(this, "위치 공유를 중단합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // [참여자 목록 (리사이클러뷰)]
        val rvUsers = findViewById<RecyclerView>(R.id.rv_location_users)
        rvUsers.layoutManager = LinearLayoutManager(this)
        // 나중에 어댑터 연결: rvUsers.adapter = LocationUserAdapter(userList)
    }

    // 지도 로딩 완료 시 실행
    override fun onMapReady(naverMap: NaverMap) {
        // 여기에 내 위치 표시 마커 등을 추가하면 됩니다.
        Toast.makeText(this, "지도가 준비되었습니다.", Toast.LENGTH_SHORT).show()
    }
}