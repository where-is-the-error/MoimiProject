package com.moimiApp.moimi

// ... (기존 LoginRequest, LoginResponse 등은 그대로 유지) ...

// --- 5. TMAP 경로 탐색 DTO (자동차 경로) ---
data class TmapRouteResponse(
    val features: List<Feature>
)

data class Feature(
    val type: String,
    val geometry: Geometry,
    val properties: Properties
)

data class Geometry(
    val type: String,
    val coordinates: Any // Point: [lon, lat], LineString: [[lon, lat], ...] (복잡해서 Any로 받고 처리하거나 List<Any>로 받음)
)

data class Properties(
    val totalDistance: Int? = null, // 총 거리 (m)
    val totalTime: Int? = null,     // 총 소요시간 (초)
    val taxiFare: Int? = null,      // 택시 요금
    val pointType: String? = null,  // 지점 타입 (S: 시작, E: 도착, P: 경유)
    val name: String? = null,       // 지점 명칭
    val description: String? = null // 상세 설명
)