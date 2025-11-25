package com.moimiApp.moimi // 패키지명 확인

// 네이버 검색 결과 전체 (껍데기)
data class SearchResponse(
    val total: Int,
    val start: Int,
    val display: Int,
    val items: List<RestaurantItem> // 알맹이 목록
)

// 음식점 하나하나의 정보 (알맹이)
data class RestaurantItem(
    val title: String,       // 가게 이름 (HTML 태그 포함될 수 있음)
    val link: String,        // 네이버 지도 링크
    val category: String,    // 카테고리 (한식>고기)
    val description: String, // 설명
    val telephone: String,   // 전화번호
    val address: String,     // 지번 주소
    val roadAddress: String, // 도로명 주소
    val mapx: String,        // X 좌표 (KATECH 좌표계 - 나중에 변환 필요)
    val mapy: String         // Y 좌표
)