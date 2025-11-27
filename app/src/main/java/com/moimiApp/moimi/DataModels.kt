package com.moimiApp.moimi

// --- 1. 로그인/회원가입 ---
data class LoginRequest(val userId: String, val userPw: String)
data class LoginResponse(val success: Boolean, val token: String?, val message: String?, val userId: String?, val username: String?)
data class RegisterRequest(val userId: String, val userPw: String, val username: String, val skinType: String? = null)
data class RegisterResponse(val success: Boolean, val message: String)

// --- 2. 위치/기타 ---
data class LocationRequest(val latitude: Double, val longitude: Double)
data class LocationResponse(val success: Boolean, val message: String)
data class RouteRequest(val startX: Double, val startY: Double, val endX: Double, val endY: Double)

// --- 3. TMAP 경로 탐색 (작성해주신 부분) ---
data class TmapRouteResponse(val features: List<Feature>)
data class Feature(val type: String, val geometry: Geometry, val properties: Properties)
data class Geometry(val type: String, val coordinates: Any) // Any로 두면 나중에 파싱할 때 주의 필요
data class Properties(
    val totalDistance: Int? = null,
    val totalTime: Int? = null,
    val taxiFare: Int? = null,
    val pointType: String? = null,
    val name: String? = null,
    val description: String? = null
)
data class SearchResponse(
    val searchPoiInfo: SearchPoiInfo?
)

data class SearchPoiInfo(
    val pois: Pois?
)

data class Pois(
    val poi: List<Poi>?
)

data class Poi(
    val name: String?,
    val frontLat: String?,
    val frontLon: String?
)

// 예: 현재 날씨를 가져오는 Current Weather Data API의 JSON 구조에 맞게 작성
data class WeatherData(
    val name: String, // 도시 이름
    val main: Main,   // 온도 정보
    val weather: List<Weather> // 날씨 상태 목록
)

data class Main(
    val temp: Double  // 현재 기온
)

data class Weather(
    val description: String, // 상세 날씨 설명
    val icon: String // 날씨 아이콘 코드
)