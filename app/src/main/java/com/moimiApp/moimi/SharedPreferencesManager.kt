package com.moimiApp.moimi

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {

    // SharedPreferences 파일 이름 정의
    private val PREFS_NAME = "moimi_app_prefs"
    private val TOKEN_KEY = "auth_token"
    private val USER_ID_KEY = "user_id"
    private val USER_NAME_KEY = "user_name" // 사용자 이름 저장

    // MODE_PRIVATE: 이 앱에서만 파일 접근 가능
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- 저장 함수 ---
    fun saveSession(token: String, userId: String, userName: String) {
        prefs.edit().apply {
            putString(TOKEN_KEY, token)
            putString(USER_ID_KEY, userId)
            putString(USER_NAME_KEY, userName)
            apply()
        }
    }

    // --- 토큰 가져오기 (API 호출 시 사용) ---
    fun getToken(): String? {
        return prefs.getString(TOKEN_KEY, null)
    }

    // --- 세션 삭제 (로그아웃 시 사용) ---
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}