package com.moimiApp.moimi // ⚠️ 이 패키지명과 다른 파일 패키지명이 같아야 합니다!

// 서버로 위치를 보낼 때 (POST /api/users/locations)
data class LocationRequest(
    val latitude: Double,
    val longitude: Double
)

// 서버에서 받을 때 (응답)
data class LocationResponse(
    val success: Boolean
)