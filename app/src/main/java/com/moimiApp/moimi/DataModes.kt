package com.moimiApp.moimi

// 모든 데이터 클래스를 여기 하나에 모았습니다.

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val success: Boolean, val message: String, val token: String?, val userId: String?, val username: String?
)

data class RegisterRequest(val name: String, val email: String, val password: String)

data class RegisterResponse(val success: Boolean, val message: String, val userId: String?)

data class LocationRequest(val latitude: Double, val longitude: Double)

data class LocationResponse(val success: Boolean)

data class SearchResponse(val total: Int, val start: Int, val display: Int, val items: List<RestaurantItem>)

data class RestaurantItem(
    val title: String, val link: String, val category: String, val roadAddress: String, val mapx: String, val mapy: String
)