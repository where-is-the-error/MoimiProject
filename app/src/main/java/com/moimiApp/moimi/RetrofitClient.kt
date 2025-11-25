package com.moimiApp.moimi // ⚠️ 패키지 이름 확인

import com.moimiApp.moimi.LoginRequest
import com.moimiApp.moimi.LoginResponse

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
// 모든 HTTP 관련 어노테이션 임포트
import retrofit2.http.*

// 1. 로그인/유저 서버 요청 기능 정의 (Node.js 서버)
interface ApiService {
    // Note: LoginRequest, LoginResponse DTO는 같은 패키지에 있다고 가정합니다.
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>
    @POST("api/users/locations")
    fun updateLocation(@Body request: LocationRequest): Call<LocationResponse>
}

// 2. 네이버 지역 검색 API 요청 기능 정의
interface NaverSearchApi {
    @GET("v1/search/local.json")
    fun searchLocal(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int = 5
    ): Call<SearchResponse> // Note: SearchResponse DTO는 같은 패키지에 있다고 가정합니다.
}

// --- 3. 통신 기계 인스턴스 생성 ---

// Node.js 백엔드 서버 통신 기계
object RetrofitClient {
    // ⚠️ Node.js 서버 주소 (에뮬레이터: 10.0.2.2, 실제 폰: PC의 IP)
    private const val BASE_URL_SERVER = "http://10.0.2.2:3000/"

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