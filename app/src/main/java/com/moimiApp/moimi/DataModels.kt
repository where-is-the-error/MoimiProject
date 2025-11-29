package com.moimiApp.moimi

import com.google.gson.annotations.SerializedName

// --- 1. 로그인/회원가입 ---
// 서버 auth.routes.js의 '/login'에서 받는 변수명(userId, userPw)과 일치
data class LoginRequest(
    @SerializedName("userId") val userId: String, // 실제로는 이메일이 들어감
    @SerializedName("userPw") val userPw: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val userId: String?,
    val username: String?
)

// 서버 auth.routes.js의 '/register'에서 받는 변수명(email, password, name, phone)과 일치
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val userPw: String,
    @SerializedName("name") val username: String,
    @SerializedName("phone") val phone: String? = null
)

data class RegisterResponse(
    val success: Boolean,
    val message: String
)

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

data class ChatHistoryResponse(val success: Boolean, val chats: List<ChatLog>)
data class ChatLog(val message: String, val createdAt: String, val sender: Sender)
data class Sender(val name: String)

data class SendMessageRequest(val roomId: String, val message: String)
data class SendMessageResponse(val success: Boolean, val chat: ChatLog)

// --- 5. 검색 관련 ---
data class TmapPoiResponse(val searchPoiInfo: SearchPoiInfo?)
data class SearchResponse(val searchPoiInfo: SearchPoiInfo?)
data class SearchPoiInfo(val pois: Pois?)
data class Pois(val poi: List<Poi>?)
data class Poi(val name: String?, val frontLat: String?, val frontLon: String?)

// --- 6. 일정 관련 ---
data class ScheduleItem(val time: String, val title: String, val location: String)
data class AddScheduleRequest(val date: String, val time: String, val title: String, val location: String)
data class ScheduleResponse(val success: Boolean, val schedules: List<ScheduleItem>?)

// --- 7. 모임(예약) 관련 ---
data class CreateMeetingRequest(val title: String, val location: String, val dateTime: String, val reservationRequired: Boolean)
data class MeetingCreationResponse(val success: Boolean, val message: String)
data class MeetingListResponse(val success: Boolean, val meetings: List<Any>?)

// --- 8. 알림 관련 ---
data class NotificationResponse(val success: Boolean, val notifications: List<NotificationItem>)
data class NotificationItem(val message: String)