package com.moimiApp.moimi // ⚠️ 패키지명 확인

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ⚠️ XML 파일 이름이 activity_sign_up.xml 이라고 가정합니다.
class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // 1. 필요한 View ID들을 연결 (XML ID에 맞춰 수정하세요!)
        val etName = findViewById<EditText>(R.id.etName) // 이름 입력창 ID
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignup) // 회원가입 버튼 ID

        btnSignUp.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 서버로 보낼 데이터 준비
            val requestData = RegisterRequest(name, email, password)

            // 3. Retrofit 호출
            RetrofitClient.instance.register(requestData).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    val result = response.body()

                    if (response.isSuccessful && result?.success == true) {
                        Toast.makeText(this@SignUpActivity, "회원가입 성공!", Toast.LENGTH_LONG).show()
                        finish() // 회원가입 성공 후 이전 화면(로그인)으로 돌아가기
                    } else {
                        // 서버 오류 (409 중복 등)
                        Toast.makeText(this@SignUpActivity, "회원가입 실패: ${result?.message}", Toast.LENGTH_LONG).show()
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