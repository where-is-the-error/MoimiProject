plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.moimiApp.moimi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moimiApp.moimi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false // 디버그 모드에서는 코드 삭제 금지!
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(files("libs/vsm-tmap-sdk-v2-android-1.7.45.aar"))
    implementation(files("libs/tmap-sdk-3.0.aar"))
    //implementation("com.google.android.gms:play-services-maps:18.2.0")
    // 위치 및 통신 라이브러리
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // 안드로이드 기본 라이브러리
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Socket.io 클라이언트 (안정적인 버전)
    implementation("io.socket:socket.io-client:2.1.0") {
        // org.json 충돌 방지 (안드로이드 내장 JSON과 충돌할 수 있음)
        exclude(group = "org.json", module = "json")
    }
    implementation("com.google.code.gson:gson:2.10.1")

}