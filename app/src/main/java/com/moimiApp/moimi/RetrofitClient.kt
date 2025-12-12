package com.moimiApp.moimi

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// ==========================================
// 1. [내 서버] Node.js API
// ==========================================
interface ApiService {
    
    @PUT("api/meetings/{meetingId}/share-location")
    fun toggleLocationShare(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: String,
        @Body body: Map<String, Boolean> // {"isSharing": true}
    ): Call<CommonResponse>

    // [신규] 2. 위치 공유 요청 (콕 찌르기)
    @POST("api/meetings/{meetingId}/request-location")
    fun requestLocationShare(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: String,
        @Body body: Map<String, String> // {"targetUserId": "..."}
    ): Call<CommonResponse>

    // [신규] 3. 모임 상세 정보(참여자 상태 포함) 가져오기
    @GET("api/meetings/{meetingId}")
    fun getMeetingDetail(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: String
    ): Call<SingleMeetingResponse>

    @GET("api/meetings/{meetingId}/locations")
    fun getMeetingLocations(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: String
    ): Call<MeetingLocationResponse>

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("api/users/locations")
    fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: LocationRequest
    ): Call<LocationResponse>

    @GET("api/meetings")
    fun getMeetings(
        @Header("Authorization") token: String
    ): Call<MeetingListResponse>

    @POST("api/meetings")
    fun createMeeting(
        @Header("Authorization") token: String,
        @Body request: CreateMeetingRequest
    ): Call<MeetingCreationResponse>

    // ⭐ [추가] 이메일로 모임 초대
    @POST("api/meetings/{meetingId}/invite-email")
    fun inviteByEmail(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: String,
        @Body request: InviteByEmailRequest
    ): Call<InviteResponse>

    // ✅ [신규] 모임(채팅방) 참여자 상태 변경 (채팅 요청 수락용)
    @PUT("api/meetings/{meetingId}/participant-status")
    fun updateParticipantStatus(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: String,
        @Body request: UpdateParticipantStatusRequest
    ): Call<CommonResponse>
}

// ==========================================
// 2. [채팅] 채팅 API
// ==========================================
interface ChatApiService {
    @PUT("api/chats/read/{roomId}")
    fun markAsRead(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String
    ): Call<CommonResponse>

    @GET("api/chats/{roomId}")
    fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String
    ): Call<ChatHistoryResponse>

    @POST("api/chats/send")
    fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: SendMessageRequest
    ): Call<SendMessageResponse>

    // ⭐ [신규] 1:1 채팅방 만들기
    @POST("api/chats/private")
    fun createPrivateChat(
        @Header("Authorization") token: String,
        @Body request: CreatePrivateChatRequest
    ): Call<CreatePrivateChatResponse>

    // ⭐ [신규] 내 채팅방 목록 가져오기
    @GET("api/chats/rooms/my")
    fun getMyChatRooms(
        @Header("Authorization") token: String
    ): Call<ChatRoomListResponse>
}

// ==========================================
// 3. [일정] 일정 API (수정됨)
// ==========================================
interface ScheduleApiService {
    @POST("api/schedules")
    fun addSchedule(@Header("Authorization") token: String, @Body request: AddScheduleRequest): Call<ScheduleResponse>

    @GET("api/schedules")
    fun getSchedules(
        @Header("Authorization") token: String,
        @Query("date") date: String?
    ): Call<ScheduleResponse>

    @GET("api/schedules/{scheduleId}")
    fun getSchedule(@Header("Authorization") token: String, @Path("scheduleId") scheduleId: String): Call<SingleScheduleResponse>

    @POST("api/schedules/{scheduleId}/join")
    fun joinSchedule(@Header("Authorization") token: String, @Path("scheduleId") scheduleId: String): Call<JoinScheduleResponse>

    @POST("api/schedules/join/code")
    fun joinScheduleByCode(@Header("Authorization") token: String, @Body request: JoinByCodeRequest): Call<JoinScheduleResponse>

    // ⭐ [신규] 일정 수정
    @retrofit2.http.PUT("api/schedules/{scheduleId}")
    fun updateSchedule(@Header("Authorization") token: String, @Path("scheduleId") scheduleId: String, @Body request: UpdateScheduleRequest): Call<ScheduleResponse>

    // ⭐ [신규] 일정 삭제
    @retrofit2.http.DELETE("api/schedules/{scheduleId}")
    fun deleteSchedule(@Header("Authorization") token: String, @Path("scheduleId") scheduleId: String): Call<ScheduleResponse>

    // ⭐ [신규] 멤버 강퇴
    @retrofit2.http.DELETE("api/schedules/{scheduleId}/members/{userId}")
    fun kickMember(@Header("Authorization") token: String, @Path("scheduleId") scheduleId: String, @Path("userId") userId: String): Call<ScheduleResponse>
}

// ==========================================
// 4. [알림] 알림 API
// ==========================================
interface NotificationApiService {
    @GET("api/notifications")
    fun getNotifications(@Header("Authorization") token: String): Call<NotificationResponse>

    // ✅ [추가] 읽음 처리
    @PUT("api/notifications/{id}/read")
    fun markAsRead(@Header("Authorization") token: String, @Path("id") id: String): Call<CommonResponse>

    // ✅ [추가] 개별 삭제
    @DELETE("api/notifications/{id}")
    fun deleteNotification(@Header("Authorization") token: String, @Path("id") id: String): Call<CommonResponse>

    // ✅ [추가] 읽은 알림 전체 삭제
    @DELETE("api/notifications/read/all")
    fun deleteAllRead(@Header("Authorization") token: String): Call<CommonResponse>
}

// ==========================================
// 5. [네이버] 검색 API
// ==========================================
interface NaverSearchApi {
    @GET("v1/search/local.json")
    fun searchLocal(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int = 5
    ): Call<SearchResponse>
}

// ==========================================
// 6. [TMAP] 경로/장소 API
// ==========================================
interface TmapApiService {
    @POST("tmap/routes?version=1&format=json")
    fun getRoute(
        @Header("appKey") appKey: String,
        @Body body: RouteRequest
    ): Call<TmapRouteResponse>

    @POST("tmap/routes/pedestrian?version=1&format=json")
    fun getPedestrianRoute(
        @Header("appKey") appKey: String,
        @Body body: RouteRequest
    ): Call<TmapRouteResponse>

    @GET("tmap/pois?version=1&format=json")
    fun searchPOI(
        @Header("appKey") appKey: String,
        @Query("searchKeyword") keyword: String,
        @Query("count") count: Int = 1
    ): Call<TmapPoiResponse>
}
// ==========================================
// ⭐ [신규] 8. 사용자(프로필) API
// ==========================================
interface UserApiService {
    // 프로필 이름 수정
    @PUT("api/users/{userId}")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Body body: Map<String, String> // {"name": "새이름"}
    ): Call<ScheduleResponse> // 응답 형식은 {success, message} 형태면 됨

    // 프로필 이미지 업로드
    @Multipart
    @POST("api/users/{userId}/profile-img")
    fun uploadProfileImage(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Part image: MultipartBody.Part
    ): Call<UploadProfileResponse>
}

// 이미지 업로드 응답용 데이터 클래스
data class UploadProfileResponse(val success: Boolean, val profileImgUrl: String?)

// ==========================================
// [Retrofit 객체 모음]
// ==========================================
object RetrofitClient {
    private const val BASE_URL_SERVER = Constants.BASE_URL

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SERVER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: ApiService by lazy { retrofit.create(ApiService::class.java) }
    val chatInstance: ChatApiService by lazy { retrofit.create(ChatApiService::class.java) }
    val scheduleInstance: ScheduleApiService by lazy { retrofit.create(ScheduleApiService::class.java) }
    val notificationInstance: NotificationApiService by lazy { retrofit.create(NotificationApiService::class.java) }

    // ⭐ [추가] 사용자 API 인스턴스
    val userInstance: UserApiService by lazy { retrofit.create(UserApiService::class.java) }
}

object NaverClient {
    private const val BASE_URL_NAVER = "https://openapi.naver.com/"

    val instance: NaverSearchApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_NAVER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NaverSearchApi::class.java)
    }
}

object TmapClient {
    private const val BASE_URL_TMAP = "https://apis.openapi.sk.com/"

    val instance: TmapApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_TMAP)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmapApiService::class.java)
    }
}

// ==========================================
// 7. [OpenWeatherMap] 날씨 API
// ==========================================
interface OpenWeatherMapApi {
    @GET("data/2.5/weather")
    fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String = Constants.OPENWEATHER_API_KEY,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "kr"
    ): Call<OpenWeatherResponse>
}

object OpenWeatherClient {
    private const val BASE_URL_OPENWEATHER = "https://api.openweathermap.org/"

    val instance: OpenWeatherMapApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_OPENWEATHER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenWeatherMapApi::class.java)
    }
}