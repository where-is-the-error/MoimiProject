package com.moimiApp.moimi

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        // ⭐ [추가] 프로필 이미지 URL 키
        private const val KEY_USER_PROFILE_IMG = "user_profile_img"
        private const val KEY_IS_SHARING = "is_location_sharing"
    }

    fun saveSession(token: String, userId: String, userName: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            apply()
        }
    }

    // ⭐ [추가] 이름 업데이트
    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    // ⭐ [추가] 프로필 이미지 저장/조회
    fun saveUserProfileImg(url: String) {
        prefs.edit().putString(KEY_USER_PROFILE_IMG, url).apply()
    }
    fun getUserProfileImg(): String? = prefs.getString(KEY_USER_PROFILE_IMG, null)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun setLocationSharing(isSharing: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SHARING, isSharing).apply()
    }

    fun isLocationSharing(): Boolean = prefs.getBoolean(KEY_IS_SHARING, false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}