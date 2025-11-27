package com.moimiApp.moimi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. View ID 연결
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // 2. 로그인 버튼 클릭 리스너
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
                        saveAuthTokens(result.token, result.userId)

                        Toast.makeText(this@LoginActivity, "${result.username}님 환영합니다!", Toast.LENGTH_SHORT).show()

                        // 로그인 성공 후 메인 화면으로 이동
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this@LoginActivity, "실패: ${result?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "오류: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("LOGIN", "에러 발생", t)
                }
            })
        }

        // TODO: 회원가입 링크 연결 로직 (XML의 textView5 ID 사용)
    }

    private fun saveAuthTokens(token: String?, userId: String?) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("jwt_token", token)
            putString("user_id", userId)
            apply()
        }
    }
}