package com.moimiApp.moimi

// import com.moimiApp.moimi.BuildConfig // ❌ BuildConfig 제거 (필요 없음)
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ==========================================
// 1. [내 서버] Node.js API
// ==========================================
interface ApiService {
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
}

// ==========================================
// 2. [채팅] 채팅 API
// ==========================================
interface ChatApiService {
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
}

// ==========================================
// 3. [일정] 일정 API
// ==========================================
interface ScheduleApiService {
    @POST("api/schedules")
    fun addSchedule(
        @Header("Authorization") token: String,
        @Body request: AddScheduleRequest
    ): Call<ScheduleResponse>

    @GET("api/schedules")
    fun getSchedules(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Call<ScheduleResponse>
}

// ==========================================
// 4. [알림] 알림 API
// ==========================================
interface NotificationApiService {
    @GET("api/notifications")
    fun getNotifications(
        @Header("Authorization") token: String
    ): Call<NotificationResponse>
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
    // 1. 자동차(택시) 경로
    @POST("tmap/routes?version=1&format=json")
    fun getRoute(
        @Header("appKey") appKey: String,
        @Body body: RouteRequest
    ): Call<TmapRouteResponse>

    // ⭐ [추가] 보행자(도보) 경로 API 추가
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
// [Retrofit 객체 모음]
// ==========================================
object RetrofitClient {
    // 🟢 [수정] 전역 상수 파일에서 가져오기
    private const val BASE_URL_SERVER = Constants.BASE_URL

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SERVER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val chatInstance: ChatApiService by lazy {
        retrofit.create(ChatApiService::class.java)
    }

    val scheduleInstance: ScheduleApiService by lazy {
        retrofit.create(ScheduleApiService::class.java)
    }

    val notificationInstance: NotificationApiService by lazy {
        retrofit.create(NotificationApiService::class.java)
    }
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
        // ⭐ [수정] BuildConfig 대신 Constants의 키 사용
        @Query("appid") apiKey: String = Constants.OPENWEATHER_API_KEY,
        @Query("units") units: String = "metric", // 섭씨 온도 사용
        @Query("lang") lang: String = "kr"        // 한국어 응답
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