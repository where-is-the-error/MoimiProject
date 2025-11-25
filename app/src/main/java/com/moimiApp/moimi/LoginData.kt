package com.example.moimi // ⚠️ 본인의 패키지 이름으로 꼭 수정하세요!

// 서버로 보낼 때 (요청)
data class LoginRequest(
    val email: String,
    val password: String
)

// 서버에서 받을 때 (응답) - auth.routes.js 코드 기준
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,    // 로그인 실패하면 없을 수도 있으니 ? 붙임
    val userId: String?,
    val username: String?
)