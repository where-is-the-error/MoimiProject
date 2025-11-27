package com.moimiApp.moimi

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ==========================================
// 0. [앱 UI용] 화면에 보여주기 위한 데이터 모델
// ==========================================

// 채팅방 목록 (ChatListActivity용)
data class ChatRoom(
    val id: String,          // 방 ID
    val title: String,       // 방 제목
    val lastMessage: String, // 마지막 대화
    val time: String,        // 시간
    val userCount: Int       // 인원수
)

// 채팅 메시지 (ChatRoomActivity용)
data class ChatMessage(
    val content: String,
    val time: String,
    val isMe: Boolean,       // 내가 보낸 건지?
    val senderName: String = ""
)


// ==========================================
// 1. [내 서버] 로그인 & 회원가입 & 위치
// ==========================================

data class LoginRequest(
    val email: String,
    val pwd: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val userId: String?,
    val username: String?
)

data class RegisterRequest(
    val email: String,
    val pwd: String,
    val name: String,
    val phone: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String
)

data class LocationRequest(
    val latitude: Double,
    val longitude: Double
)

data class LocationResponse(
    val success: Boolean,
    val message: String
)


// ==========================================
// 2. [네이버] 검색 API
// ==========================================

data class SearchResponse(
    val items: List<SearchItem>
)

data class SearchItem(
    val title: String,
    val roadAddress: String,
    val mapx: String,
    val mapy: String
)


// ==========================================
// 3. [TMAP] 경로 탐색 API (자동차 경로)
// ==========================================

data class RouteRequest(
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    val totalValue: Int = 2
)

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
    val coordinates: JsonElement // Any 대신 JsonElement 사용
)

data class Properties(
    @SerializedName("totalDistance")
    val totalDistance: Int? = null,

    @SerializedName("totalTime")
    val totalTime: Int? = null,

    @SerializedName("taxiFare")
    val taxiFare: Int? = null,

    val index: Int? = null,
    val name: String? = null,
    val description: String? = null
)


// ==========================================
// 4. [TMAP] POI 검색 응답 데이터
// ==========================================

data class TmapPoiResponse(
    val searchPoiInfo: SearchPoiInfo
)

data class SearchPoiInfo(
    val totalCount: String,
    val count: String,
    val page: String,
    val pois: Pois
)

data class Pois(
    val poi: List<PoiItem>
)

data class PoiItem(
    val id: String,
    val name: String,
    val telNo: String?,
    val frontLat: String,
    val frontLon: String,
    val newAddressList: List<NewAddress>?
)

data class NewAddress(
    val fullAddressRoad: String
)


// ==========================================
// 5. [채팅] 서버 응답용 데이터 (DTO)
// ==========================================

// 채팅방 목록 조회 (서버 -> 앱)
data class ChatRoomResponse(
    val roomId: String,
    val title: String,
    val lastMessage: String?,
    val time: String,
    val userCount: Int
)

// 채팅 메시지 데이터 (서버 -> 앱)
data class ChatMessageResponse(
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: String
)

// 일정 아이템 (ScheduleAdapter용)
data class ScheduleItem(
    val time: String,
    val title: String,
    val location: String
)


// ==========================================
// 6. [모임] 친구 위치 & 도착 정보
// ==========================================

data class GroupLocationResponse(
    val groupId: String,
    val users: List<UserLocation>
)

data class UserLocation(
    val userId: String,
    val userName: String,
    val latitude: Double,
    val longitude: Double,
    val isArrived: Boolean
)

// ==========================================
// 7. [채팅 API] 서버 통신용 데이터 모델 (추가됨)
// ==========================================

// (1) 채팅 메시지 하나 (DB 데이터)
data class Chat(
    @SerializedName("_id") val id: String,
    @SerializedName("sender_id") val sender: UserSimple, // 보낸 사람 정보
    @SerializedName("message") val message: String,
    @SerializedName("created_at") val createdAt: String
)

// (2) 유저 간단 정보 (채팅 보낸 사람)
data class UserSimple(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("profile_img") val profileImg: String?
)

// (3) 메시지 전송 요청 (보낼 때)
data class SendMessageRequest(
    val roomId: String,
    val message: String
)

// (4) 메시지 전송 응답 (성공 시)
data class SendMessageResponse(
    val success: Boolean,
    val chat: Chat
)

// (5) 채팅 내역 조회 응답 (목록 불러올 때)
data class ChatHistoryResponse(
    val success: Boolean,
    val chats: List<Chat>
)

// ==========================================
// 8. [일정 API] 데이터 모델
// ==========================================

// 일정 추가 요청 (보낼 때)
data class AddScheduleRequest(
    val date: String,    // "2023-09-22"
    val time: String,    // "17:00"
    val title: String,
    val location: String
)

// 일정 응답 (받을 때) - DB에 저장된 모양
data class ScheduleResponse(
    val success: Boolean,
    val schedules: List<ScheduleData>? = null, // 조회용 리스트
    val schedule: ScheduleData? = null         // 추가용 단일 객체
)

data class ScheduleData(
    @SerializedName("_id") val id: String,
    val date: String,
    val time: String,
    val title: String,
    val location: String
)

// ==========================================
// 9. [알림 API] 데이터 모델 (추가됨 ✅)
// ==========================================

data class NotificationResponse(
    val success: Boolean,
    val notifications: List<NotificationItem>
)

data class NotificationItem(
    @SerializedName("_id") val id: String,
    val message: String,  // "철수님이 약속을 확인했습니다!"
    val read: Boolean,
    @SerializedName("created_at") val createdAt: String
)

// ==========================================
// 10. [모임] 약속/예약 생성 요청/응답
// ==========================================

// 1. 서버에 보낼 예약 정보 (Request)
data class CreateMeetingRequest(
    val title: String,
    val location: String,
    val dateTime: String, // 서버가 인식할 수 있는 포맷 (예: 2025-09-22 17:00:00)
    val reservationRequired: Boolean
)

// 2. 예약 성공 시 서버 응답 (Response)
data class MeetingCreationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("meetingId") val meetingId: String?
)

// ==========================================
// 10. [모임] 약속 목록 조회 API DTO
// ==========================================

data class MeetingListResponse(
    val success: Boolean,
    val meetings: List<MeetingItem> // 서버가 반환하는 모임 리스트
)

data class MeetingItem(
    @SerializedName("meeting_id") val id: String,
    val title: String,
    val location: String,
    @SerializedName("date_time") val dateTime: String
)