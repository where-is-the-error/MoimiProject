package com.moimiApp.moimi // ⚠️ 패키지명 확인

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.moimiApp.moimi.LoginRequest
import com.moimiApp.moimi.LoginResponse
import com.moimiApp.moimi.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ⚠️ R.layout.activity_map_detail 이 빨간 줄이면, MapDetailActivity.kt 파일이 있어야 합니다.

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // --- 1. View ID 연결 ---
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // --- 2. 로그인 버튼 클릭 리스너 ---
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
                        // 🎉 로그인 성공!

                        // [추가된 로직 1] SharedPreferences에 토큰 및 ID 저장 (세션 유지)
                        saveAuthTokens(result.token, result.userId)

                        Toast.makeText(this@LoginActivity, "${result.username}님 환영합니다!", Toast.LENGTH_SHORT).show()
                        Log.d("LOGIN", "토큰 저장 완료: ${result.token}")

                        // [추가된 로직 2] 다음 화면으로 이동 후 현재 액티비티 종료
                        // TODO: MapDetailActivity 대신 메인 화면 (지도 화면)으로 바꿔주세요.
                        val intent = Intent(this@LoginActivity, MapDetailActivity::class.java)
                        startActivity(intent)
                        finish() // 로그인 액티비티는 닫기 (뒤로가기 방지)

                    } else {
                        // 😭 로그인 실패 (비번 틀림 등)
                        Toast.makeText(this@LoginActivity, "실패: ${result?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // 😱 통신 오류
                    Toast.makeText(this@LoginActivity, "오류: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("LOGIN", "에러 발생", t)
                }
            })
        }
    }

    // [추가된 함수] SharedPreferences에 인증 정보 저장
    private fun saveAuthTokens(token: String?, userId: String?) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("jwt_token", token)
            putString("user_id", userId)
            apply()
        }
    }
}