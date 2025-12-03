// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {

    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false

    // 코틀린 플러그인
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false

    // ✅ 구글 서비스 플러그인 (FCM용) - 중복되지 않게 한 번만 선언
    id("com.google.gms.google-services") version "4.4.4" apply false
}