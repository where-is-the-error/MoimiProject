plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    
    namespace = "com.moimiApp.moimi"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moimiApp.moimi"
        minSdk = 26 // 오레오 이상 권장
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // 뷰 바인딩 사용 설정
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(files("libs/tmap.aar"))
    implementation(files("libs/vsm.aar"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0") // 새로고침 레이아웃
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // 2. 로컬 브로드캐스트 (MainActivity 오류 해결용)

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // ✅ Firebase Messaging (버전 번호 없이!)
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // Retrofit (네트워크)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Glide (이미지 로딩)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // TMap SDK (libs 폴더에 jar가 있다면 implementation fileTree 사용)
    // implementation(files("libs/tmap-sdk-1.0

    // Socket.io
    implementation("io.socket:socket.io-client:2.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}