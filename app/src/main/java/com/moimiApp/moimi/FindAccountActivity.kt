package com.moimiApp.moimi

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.tabs.TabLayout

class FindAccountActivity : BaseActivity() { // BaseActivity 상속

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_account)

        // 1. 뷰 연결 (XML ID와 정확히 일치)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout_find)

        val layoutFindId = findViewById<LinearLayout>(R.id.layout_find_id_input)
        val layoutFindPw = findViewById<LinearLayout>(R.id.layout_find_pw_input)

        val etName = findViewById<EditText>(R.id.et_name)
        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etEmail = findViewById<EditText>(R.id.et_email)

        val btnSend = findViewById<AppCompatButton>(R.id.btn_send_auth)

        // 2. 뒤로가기 버튼
        btnBack.setOnClickListener {
            finish()
        }

        // 3. 탭 선택 리스너 (아이디 찾기 <-> 비밀번호 찾기 전환)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // [아이디 찾기] 탭
                        layoutFindId.visibility = View.VISIBLE
                        layoutFindPw.visibility = View.GONE
                    }
                    1 -> { // [비밀번호 찾기] 탭
                        layoutFindId.visibility = View.GONE
                        layoutFindPw.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 4. 인증번호 발송 버튼 클릭 로직
        btnSend.setOnClickListener {
            val name = etName.text.toString()

            // 공통: 이름 입력 확인
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 현재 탭 확인 (0: 아이디 찾기, 1: 비밀번호 찾기)
            if (tabLayout.selectedTabPosition == 0) {
                // [아이디 찾기 모드]
                val phone = etPhone.text.toString()
                if (phone.isEmpty()) {
                    Toast.makeText(this, "휴대폰 번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "아이디 찾기 인증번호를 발송했습니다.", Toast.LENGTH_SHORT).show()
                    // TODO: 서버 연동 (아이디 찾기 API 호출)
                }
            } else {
                // [비밀번호 찾기 모드]
                val email = etEmail.text.toString()
                if (email.isEmpty()) {
                    Toast.makeText(this, "이메일(아이디)을 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "비밀번호 재설정 인증번호를 발송했습니다.", Toast.LENGTH_SHORT).show()
                    // TODO: 서버 연동 (비밀번호 찾기 API 호출)
                }
            }
        }
    }
}