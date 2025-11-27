package com.moimiApp.moimi

import android.content.Context
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. View 연결 (XML ID 확인 필수)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // 회원가입 & 찾기 버튼 연결
        // (아까 XML에서 회원가입은 textView3, 찾기는 textFind로 설정했습니다)
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

            // 입력값 유효성 검사
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 요청 데이터 객체 생성
            val requestData = LoginRequest(email, password)

            // Retrofit 비동기 요청
            RetrofitClient.instance.login(requestData).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    val result = response.body()

                    // 로그인 성공 조건 확인 (HTTP 200 OK + success 필드 true)
                    if (response.isSuccessful && result?.success == true) {
                        // 토큰 저장
                        saveAuthTokens(result.token, result.userId)

                        Toast.makeText(this@LoginActivity, "${result.username}님 환영합니다!", Toast.LENGTH_SHORT).show()

                        // 메인 화면으로 이동 및 로그인 화면 종료
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        // 로그인 실패 (비밀번호 틀림 등)
                        Toast.makeText(this@LoginActivity, "로그인 실패: ${result?.message ?: "오류 발생"}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // 서버 통신 오류 (인터넷 끊김, 서버 다운 등)
                    Toast.makeText(this@LoginActivity, "서버 연결 오류: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("LOGIN", "에러 발생", t)
                }
            })
        }
    }

    // 토큰 저장 함수 (SharedPreferences)
    private fun saveAuthTokens(token: String?, userId: String?) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("jwt_token", token)
            putString("user_id", userId)
            apply() // 비동기 저장
        }
    }
}