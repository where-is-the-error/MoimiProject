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

data class RouteRequest(
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    val reqCoordType: String = "WGS84GEO",
    val resCoordType: String = "WGS84GEO",
    val startName: String = "출발지",
    val endName: String = "도착지",
    val searchOption: Int = 0,
    val totalValue: Int = 2
)

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

// ⭐ [신규] 이메일로 초대 요청/응답
data class InviteByEmailRequest(val email: String)
data class InviteResponse(val success: Boolean, val message: String)

// --- 5. 검색 관련 ---
data class TmapPoiResponse(val searchPoiInfo: SearchPoiInfo?)
data class SearchResponse(val searchPoiInfo: SearchPoiInfo?)
data class SearchPoiInfo(val pois: Pois?)
data class Pois(val poi: List<Poi>?)
data class Poi(val name: String?, val frontLat: String?, val frontLon: String?)

// --- 6. 일정 관련 ---
data class AddScheduleRequest(
    val date: String,
    val time: String,
    val title: String,
    val location: String,
    val type: String = "MEETING"
)

data class ScheduleItem(
    val id: String = "",
    val time: String,
    val date: String? = null,
    val title: String,
    val location: String,
    val inviteCode: String? = null,
    val leaderName: String = "",
    val leaderId: String = "",
    val memberNames: List<String> = emptyList(),
    val isLeader: Boolean = false,
    val type: String = "MEETING"
)

data class ScheduleResponse(
    val success: Boolean,
    val message: String? = null,
    val schedules: List<ScheduleItem>? = null,
    val scheduleId: String? = null,
    val inviteCode: String? = null
)

data class SingleScheduleResponse(
    val success: Boolean,
    val schedule: ScheduleItem?
)

data class JoinScheduleResponse(
    val success: Boolean,
    val message: String,
    val scheduleId: String? = null
)

data class JoinByCodeRequest(val inviteCode: String)

// --- 7. 모임(예약) 관련 ---
data class CreateMeetingRequest(val title: String, val location: String, val dateTime: String, val reservationRequired: Boolean)
data class MeetingCreationResponse(val success: Boolean, val message: String)

data class MeetingItem(
    val id: String,
    val title: String,
    val dateTime: String,
    val location: String
)
// ⭐ [신규] 1:1 채팅방 생성 요청
data class CreatePrivateChatRequest(
    val targetEmail: String
)

// ⭐ [신규] 1:1 채팅방 생성 응답
data class CreatePrivateChatResponse(
    val success: Boolean,
    val message: String,
    val roomId: String?,
    val title: String?
)

// ⭐ [신규] 채팅방 목록 아이템 (기존 ChatRoom 클래스 대체 또는 수정)
data class ChatRoomItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("lastMessage") val lastMessage: String
)

data class ChatRoomListResponse(
    val success: Boolean,
    val rooms: List<ChatRoomItem>
)
data class MeetingListResponse(val success: Boolean, val meetings: List<MeetingItem>?)

// --- 8. 알림 관련 ---
data class NotificationResponse(val success: Boolean, val notifications: List<NotificationItem>)
data class NotificationItem(val message: String)

// --- 9. 날씨 관련 ---
data class OpenWeatherResponse(
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("main") val main: WeatherMain,
    @SerializedName("wind") val wind: WindInfo,
    @SerializedName("name") val cityName: String
)

data class WeatherMain(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("humidity") val humidity: Int
)

data class WeatherDescription(
    @SerializedName("main") val condition: String,
    @SerializedName("description") val detail: String,
    @SerializedName("icon") val icon: String
)

data class WindInfo(
    @SerializedName("speed") val speed: Double
)