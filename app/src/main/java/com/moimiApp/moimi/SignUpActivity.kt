package com.moimiApp.moimi

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val btnBack = findViewById<ImageView>(R.id.btn_back) // ✅ 뒤로가기 버튼
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etNum = findViewById<EditText>(R.id.etNum)
        val btnSignUp = findViewById<Button>(R.id.btnSignup)

        // 뒤로가기 기능 적용
        btnBack.setOnClickListener {
            finish()
        }

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val phone = etNum.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requestData = RegisterRequest(email, password, name, phone)

            RetrofitClient.instance.register(requestData).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@SignUpActivity, "회원가입 성공! 로그인해주세요.", Toast.LENGTH_LONG).show()
                        finish() // 로그인 화면으로 이동
                    } else {
                        val errorMsg = response.body()?.message ?: "회원가입 실패 (중복이거나 오류)"
                        Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                        Log.e("SIGNUP", "Fail: ${response.code()} ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Toast.makeText(this@SignUpActivity, "서버 연결 실패: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("SIGNUP", "Network Error", t)
                }
            })
        }
    }
}