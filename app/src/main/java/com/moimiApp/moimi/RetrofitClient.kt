package com.moimiApp.moimi

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// 1. [내 서버] 로그인/유저 서버 요청 (Node.js)
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
}

// 2. [네이버] 지역 검색 API
interface NaverSearchApi {
    @GET("v1/search/local.json")
    fun searchLocal(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int = 5
    ): Call<SearchResponse>
}

// 3. [TMAP] 통합 API (경로 탐색 + 장소 검색) ✅ 수정됨
interface TmapApiService {

    // (1) 경로 탐색 (기존 기능)
    @POST("tmap/routes?version=1&format=json")
    fun getRoute(
        @Header("appKey") appKey: String,
        @Body body: RouteRequest
    ): Call<TmapRouteResponse>

    // (2) 장소(POI) 검색 (추가된 기능)
    // 식당 이름, 주소, 좌표 등을 가져옵니다.
    @GET("tmap/pois?version=1&format=json")
    fun searchPOI(
        @Header("appKey") appKey: String,
        @Query("searchKeyword") keyword: String, // 검색어 (예: 동양식당)
        @Query("count") count: Int = 1           // 1개만 가져옴
    ): Call<TmapPoiResponse>
}

// --- 통신 기계 인스턴스 ---

// Node.js 백엔드
object RetrofitClient {
    private const val BASE_URL_SERVER = "http://10.0.2.2:3000/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SERVER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// 네이버 클라이언트
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

// TMAP 클라이언트 ✅ 수정됨
object TmapClient {
    private const val BASE_URL_TMAP = "https://apis.openapi.sk.com/"

    val instance: TmapApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_TMAP)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmapApiService::class.java) // 이름 바뀐 인터페이스 연결
    }
}