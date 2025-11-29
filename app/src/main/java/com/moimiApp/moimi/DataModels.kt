package com.moimiApp.moimi

import com.google.gson.annotations.SerializedName

// --- 1. 로그인/회원가입 ---
data class LoginRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("userPw") val userPw: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val userId: String?,
    val username: String?
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val userPw: String,
    @SerializedName("name") val username: String,
    @SerializedName("phone") val phone: String? = null
)

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
    val name: String? = null,
    val description: String? = null
)

// --- 4. 채팅 관련 ---
data class ChatMessage(val content: String, val time: String, val isMe: Boolean, val senderName: String = "")
data class ChatRoom(val title: String, val lastMessage: String)
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

// --- 6. 일정 관련 (모임장/참여자 기능 추가) ---
// src/main/java/com/moimiApp/moimi/DataModels.kt

// [수정] AddScheduleRequest에 type 필드 추가
data class AddScheduleRequest(
    val date: String,
    val time: String,
    val title: String,
    val location: String,
    val inviteUserIds: List<String>? = null,

    // ⭐ [추가] 일정 유형 (예: "MEETING", "CHECKLIST")
    val type: String = "MEETING"
)

// [수정] ScheduleItem에도 type 추가 (받아올 때 필요)
data class ScheduleItem(
    val id: String = "",
    val time: String,
    val title: String,
    val location: String,
    val leaderName: String = "",
    val leaderId: String = "",
    val memberNames: List<String> = emptyList(),
    val isLeader: Boolean = false,

    // ⭐ [추가]
    val type: String = "MEETING"
)

data class ScheduleResponse(val success: Boolean, val schedules: List<ScheduleItem>?)

// --- 7. 모임(예약) 관련 ---
data class CreateMeetingRequest(val title: String, val location: String, val dateTime: String, val reservationRequired: Boolean)
data class MeetingCreationResponse(val success: Boolean, val message: String)

// [수정] MeetingItem 정의 및 적용
data class MeetingItem(
    val id: String,
    val title: String,
    val dateTime: String,
    val location: String
)
data class MeetingListResponse(val success: Boolean, val meetings: List<MeetingItem>?)

// --- 8. 알림 관련 ---
data class NotificationResponse(val success: Boolean, val notifications: List<NotificationItem>)
data class NotificationItem(val message: String)

// --- 9. OpenWeatherMap 날씨 관련 ---

data class OpenWeatherResponse(
    // ⚠️ @SerializedName을 사용하여 JSON 필드명과 Kotlin 변수명을 연결합니다.
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("main") val main: WeatherMain,
    @SerializedName("wind") val wind: WindInfo,
    @SerializedName("name") val cityName: String // 도시 이름
)

// 온도, 습도, 기압 등의 메인 정보
data class WeatherMain(
    @SerializedName("temp") val temp: Double, // 현재 온도 (예: 섭씨)
    @SerializedName("feels_like") val feelsLike: Double, // 체감 온도
    @SerializedName("temp_min") val tempMin: Double, // 최저 온도
    @SerializedName("temp_max") val tempMax: Double, // 최고 온도
    @SerializedName("humidity") val humidity: Int // 습도 (%)
)

// 날씨 상태 (맑음, 구름, 비 등)
data class WeatherDescription(
    @SerializedName("main") val condition: String, // 주요 날씨 상태 (예: Clouds, Rain)
    @SerializedName("description") val detail: String, // 상세 설명 (예: overcast clouds)
    @SerializedName("icon") val icon: String // 날씨 아이콘 ID
)

// 바람 정보
data class WindInfo(
    @SerializedName("speed") val speed: Double // 풍속
)
