package com.moimiApp.moimi

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class MyPageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        setupDrawer() // 메뉴 연결

        // 1. 뷰 연결
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val tvName = findViewById<TextView>(R.id.tv_my_name)
        val tvEmail = findViewById<TextView>(R.id.tv_my_email)

        // 2. 뒤로가기
        btnBack.setOnClickListener { finish() }

        // 3. 저장된 내 정보 가져오기 (SharedPreferencesManager 사용)
        // (저장된 이름 키가 "user_name"이고 아이디가 "user_id"라고 가정)
        val myName = prefsManager.getUserName() ?: "이름 없음"
        val myId = prefsManager.getUserId() ?: "아이디 없음"

        // 4. 화면에 표시
        tvName.text = myName
        tvEmail.text = myId
    }
}