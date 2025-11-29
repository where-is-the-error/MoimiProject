package com.moimiApp.moimi

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// 1. 로그인/유저 서버 요청 기능 정의 (Node.js 서버)
interface ApiService {
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    // ✅ 위치 업데이트 API (토큰을 헤더로 받음)
    @POST("api/users/locations")
    fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: LocationRequest
    ): Call<LocationResponse>
}

// 2. 네이버 지역 검색 API 요청 기능 정의
interface NaverSearchApi {
    @GET("v1/search/local.json")
    fun searchLocal(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int = 5
    ): Call<SearchResponse>
}

// 3. TMAP 경로 탐색 API 요청 기능 정의 (추가됨)
interface TmapRouteApi {
    @POST("tmap/routes?version=1&format=json")
    fun getRoute(
        @Header("appKey") appKey: String,
        @Body body: RouteRequest
    ): Call<TmapRouteResponse>
}

// --- 통신 기계 인스턴스 생성 ---

// Node.js 백엔드 서버 통신 기계
object RetrofitClient {
    private const val BASE_URL_SERVER = "http://10.0.2.2:3000/" // 에뮬레이터용

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SERVER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// 네이버 Open API 통신 기계
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

// TMAP Open API 통신 기계
object TmapClient {
    private const val BASE_URL_TMAP = "https://apis.openapi.sk.com/"

    val instance: TmapRouteApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_TMAP)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmapRouteApi::class.java)
    }
}
