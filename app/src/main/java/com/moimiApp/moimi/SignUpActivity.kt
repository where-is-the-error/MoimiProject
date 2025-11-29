package com.moimiApp.moimi

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // 1. View ID 연결 (XML에 etPhone이 있어야 합니다!)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etNum = findViewById<EditText>(R.id.etNum) // ✅ 추가됨
        val btnSignUp = findViewById<Button>(R.id.btnSignup)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val phone = etNum.text.toString() // ✅ 추가됨

            // 빈칸 체크
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 서버로 보낼 데이터 준비
            // 순서 주의: (이메일, 비번, 이름, 폰번호) -> DataModels.kt 순서와 같아야 함
            val requestData = RegisterRequest(email, password, name, phone)

            // 3. Retrofit 호출
            RetrofitClient.instance.register(requestData).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    val result = response.body()

                    if (response.isSuccessful && result?.success == true) {
                        Toast.makeText(this@SignUpActivity, "회원가입 성공!", Toast.LENGTH_LONG).show()
                        finish() // 성공 시 창 닫고 로그인 화면으로 복귀
                    } else {
                        Toast.makeText(this@SignUpActivity, "실패: ${result?.message}", Toast.LENGTH_LONG).show()
                        Log.e("SIGNUP", "실패 응답: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Toast.makeText(this@SignUpActivity, "통신 오류: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("SIGNUP", "통신 에러", t)
                }
            })
        }
    }
}