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

// --- 3. TMAP 경로 탐색 ---
data class TmapRouteResponse(val features: List<Feature>)
data class Feature(val type: String, val geometry: Geometry, val properties: Properties)
data class Geometry(val type: String, val coordinates: Any)
data class Properties(
    val totalDistance: Int? = null,
    val totalTime: Int? = null,
    val taxiFare: Int? = null,
    val pointType: String? = null,
    val name: String? = null,
    val description: String? = null
)

// --- 4. 채팅 관련 ---
data class ChatMessage(
    val content: String,
    val time: String,
    val isMe: Boolean,
    val senderName: String = ""
)

data class ChatRoom(
    val title: String,
    val lastMessage: String
)

// --- 5. 검색 관련 ---
data class TmapPoiResponse(
    val searchPoiInfo: SearchPoiInfo?
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