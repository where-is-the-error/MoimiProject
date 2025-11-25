package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Toast
import android.widget.LinearLayout // TMap을 담을 LinearLayout import
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skt.Tmap.TMapView
// Naver Map 관련 import (MapFragment, NaverMap, OnMapReadyCallback)는 모두 제거됩니다.

// OnMapReadyCallback 인터페이스 제거
class LocationShareActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = "여기에_발급받은_TMap_AppKey_입력" // ⚠️ 여기에 실제 T Map Key를 입력하세요

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share) // XML 파일 연결

        // [공통 기능] 햄버거 메뉴, 로고, 벨 연결
        setupDrawer()

        // ----------------------------------------------------
        // [T Map 지도 초기화]
        // ----------------------------------------------------

        // 1. TMapView 객체 생성 및 설정
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapKey)

        // 2. XML 컨테이너에 TMapView 추가
        // map_fragment_location ID 대신 map_container_location ID를 사용한다고 가정합니다.
        val mapContainer = findViewById<LinearLayout>(R.id.map_container_location)
        mapContainer.addView(tMapView)

        // 3. 지도 기능 설정 (기본 줌 레벨 등)
        setupTMap()

        // [위치 공유 스위치 기능] (T Map으로 바뀌어도 이 로직은 그대로 유지됩니다.)
        val switchShare = findViewById<SwitchCompat>(R.id.switch_location_share)
        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "T Map 기반 위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show()
                // T Map SDK를 사용하여 실제 위치 공유/전송 로직을 여기에 구현
            } else {
                Toast.makeText(this, "위치 공유를 중단합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // [참여자 목록 (리사이클러뷰)] (지도와 무관하므로 그대로 유지됩니다.)
        val rvUsers = findViewById<RecyclerView>(R.id.rv_location_users)
        rvUsers.layoutManager = LinearLayoutManager(this)
        // 나중에 어댑터 연결: rvUsers.adapter = LocationUserAdapter(userList)
    }

    // 지도 초기 설정 함수 (T Map용)
    private fun setupTMap() {
        // T Map UI 설정
        tMapView.zoomLevel = 14 // 적절한 줌 레벨 설정

        // 여기에 T Map의 특정 기능 (예: 내 위치 표시, 지도 이벤트 리스너)을 추가할 수 있습니다.
        Toast.makeText(this, "T Map 지도가 준비되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // 기존 Naver Map의 onMapReady 함수는 T Map 코드로 대체되었으므로, 이 파일에는 존재하지 않습니다.
}