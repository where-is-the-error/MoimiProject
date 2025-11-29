package com.moimiApp.moimi

object Constants {
    // ==========================
    // 1. 서버 및 네트워크 설정
    // ==========================
    // ⭐ [중요] 사용 환경에 따라 이 IP만 변경하세요!
    // - 에뮬레이터 내부 테스트: "10.0.2.2"
    // - 실제 기기(태블릿/폰) 테스트: "192.168.0.5" (본인 PC IP)
    private const val SERVER_IP = "192.168.0.41"
    private const val SERVER_PORT = "3000"

    // Retrofit 통신용 URL (끝에 / 포함)
    const val BASE_URL = "http://$SERVER_IP:$SERVER_PORT/"

    // 소켓 통신용 URL (끝에 / 없음)
    const val SOCKET_URL = "http://$SERVER_IP:$SERVER_PORT"

    // ==========================
    // 2. API 키 설정
    // ==========================
    const val TMAP_API_KEY = "AOdyR4LyWR1eENpQe8bDA1Jm8AkL2GssabUhp15X"

    // ==========================
    // 3. 내부 저장소(SharedPreferences) 키 설정
    // ==========================
    const val PREFS_NAME = "moimi_app_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
}