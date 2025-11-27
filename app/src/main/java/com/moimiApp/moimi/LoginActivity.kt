package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    // [추가] 세션 관리자 객체
    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // SharedPreferencesManager 초기화
        prefsManager = SharedPreferencesManager(this)

        // ⚠️ [필수 추가] 앱 시작 시 이미 토큰이 있다면 바로 메인으로 이동 (자동 로그인)
        if (prefsManager.getToken() != null) {
            goToMainActivity()
            return
        }

        // 1. View 연결
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // 회원가입 & 찾기 버튼 연결
        val btnSignUp = findViewById<TextView>(R.id.textView3)
        val btnFind = findViewById<TextView>(R.id.textFind)

        // 2. [화면 이동] 회원가입 버튼 클릭 시
        btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // 3. [화면 이동] 아이디/비번 찾기 버튼 클릭 시
        btnFind.setOnClickListener {
            val intent = Intent(this, FindAccountActivity::class.java)
            startActivity(intent)
        }

        // 4. [서버 통신] 로그인 버튼 클릭 리스너
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requestData = LoginRequest(email, password)
            RetrofitClient.instance.login(requestData).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    val result = response.body()

                    if (response.isSuccessful && result?.success == true) {
                        val token = result.token
                        val userId = result.userId
                        val userName = result.username

                        if (token != null && userId != null && userName != null) {
                            // ⭐ [수정됨] SharedPreferencesManager를 사용하여 세션 저장 ⭐
                            prefsManager.saveSession(token, userId, userName)

                            Toast.makeText(this@LoginActivity, "${userName}님 환영합니다!", Toast.LENGTH_SHORT).show()

                            // 메인 화면으로 이동
                            goToMainActivity()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "로그인 실패: 아이디 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "서버 연결 오류: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("LOGIN", "에러 발생", t)
                }
            })
        }
    }

    // [삭제] 기존의 수동 저장 함수는 삭제해야 합니다.
    // private fun saveAuthTokens(token: String?, userId: String?) { ... }

    // [추가] 메인 화면 이동 함수
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // 로그인 성공 후 뒤로가기로 돌아오지 않도록 스택 정리
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}