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

    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefsManager = SharedPreferencesManager(this)

        // 자동 로그인 체크
        if (prefsManager.getToken() != null) {
            goToMainActivity()
            return
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignUp = findViewById<TextView>(R.id.textView3)
        val btnFind = findViewById<TextView>(R.id.textFind)

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        btnFind.setOnClickListener {
            startActivity(Intent(this, FindAccountActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // DataModels.kt의 LoginRequest(userId, userPw) 사용
            // userId 필드에 이메일을 넣어서 보냅니다.
            val requestData = LoginRequest(userId = email, userPw = password)

            RetrofitClient.instance.login(requestData).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    val result = response.body()

                    if (response.isSuccessful && result?.success == true) {
                        val token = result.token
                        val userId = result.userId
                        val userName = result.username

                        if (token != null && userId != null && userName != null) {
                            prefsManager.saveSession(token, userId, userName)
                            Toast.makeText(this@LoginActivity, "${userName}님 환영합니다!", Toast.LENGTH_SHORT).show()
                            goToMainActivity()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "로그인 실패: 아이디/비번을 확인하세요.", Toast.LENGTH_SHORT).show()
                        Log.e("LOGIN", "Fail: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "서버 연결 오류", Toast.LENGTH_LONG).show()
                    Log.e("LOGIN", "Error", t)
                }
            })
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}