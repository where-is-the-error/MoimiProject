package com.moimiApp.moimi // ⚠️ 패키지 이름 확인

import com.example.moimi.LoginRequest
import com.example.moimi.LoginResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 1. 서버에 요청할 기능 정의 (API 명세서 역할)
interface ApiService {
    @POST("api/auth/login") // Node.js의 라우터 경로
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}

// 2. 통신 기계 생성 (싱글톤)
object RetrofitClient {
    // ⚠️ 에뮬레이터라면 "10.0.2.2", 실제 폰이라면 "PC의 IP주소"를 적어야 합니다!
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}