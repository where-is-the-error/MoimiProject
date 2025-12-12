package com.moimiApp.moimi

object Constants {
    private const val SERVER_IP = "172.20.10.2"
    private const val SERVER_PORT = "3000"

    //const val BASE_URL = "http://$SERVER_IP:$SERVER_PORT/"

    //const val SOCKET_URL = "http://$SERVER_IP:$SERVER_PORT"


    const val BASE_URL = "https://unmagnifying-precarious-jaymie.ngrok-free.dev/"

    // 소켓 URL (http/https 제거 없이 그대로 사용 가능)
    const val SOCKET_URL = "https://unmagnifying-precarious-jaymie.ngrok-free.dev"


    const val TMAP_API_KEY = "AOdyR4LyWR1eENpQe8bDA1Jm8AkL2GssabUhp15X"
    const val OPENWEATHER_API_KEY="4511add96f9a93c2529d1e72c19aac6f"

    const val PREFS_NAME = "moimi_app_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
}