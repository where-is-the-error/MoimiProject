package com.moimiApp.moimi

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {

    // 🟢 [수정] 상수로 변경
    private val PREFS_NAME = Constants.PREFS_NAME
    private val TOKEN_KEY = Constants.KEY_AUTH_TOKEN
    private val USER_ID_KEY = Constants.KEY_USER_ID
    private val USER_NAME_KEY = Constants.KEY_USER_NAME

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

    // 사용자 정보 가져오기
    fun getUserId(): String? = prefs.getString(USER_ID_KEY, null)
    fun getUserName(): String? = prefs.getString(USER_NAME_KEY, null)
}