package com.moimiApp.moimi

import android.os.Bundle

// ✅ BaseActivity 상속 (이게 핵심!)
class RestaurantActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // XML 파일 이름과 맞춰줍니다
        setContentView(R.layout.activity_restaurant)

        // ✅ 메뉴 기능 활성화 (이 함수 한 줄이면 됩니다)
        setupDrawer()
    }
}