// ... import 문들 ...
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moimi.LoginRequest
import com.example.moimi.LoginResponse
import com.moimiApp.moimi.R
import com.moimiApp.moimi.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    // 뷰 바인딩이나 findViewById를 사용한다고 가정
    // private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 👇 [수정] XML에 적힌 진짜 ID로 변경하세요!
        val etEmail = findViewById<EditText>(R.id.etEmail)        // et_email (X) -> etEmail (O)
        val etPassword = findViewById<EditText>(R.id.etPassword)  // et_password (X) -> etPassword (O)
        val btnLogin = findViewById<Button>(R.id.btnLogin)        // btn_login (X) -> btnLogin (O)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            // 1. 보낼 데이터 포장
            val requestData = LoginRequest(email, password)

            // 2. 서버에 전송!
            RetrofitClient.instance.login(requestData).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    val result = response.body()

                    if (response.isSuccessful && result?.success == true) {
                        // 🎉 로그인 성공!
                        Toast.makeText(this@LoginActivity, "${result.username}님 환영합니다!", Toast.LENGTH_SHORT).show()
                        Log.d("LOGIN", "토큰: ${result.token}")

                        // TODO: 다음 화면(메인)으로 이동하는 코드 넣기
                    } else {
                        // 😭 로그인 실패 (비번 틀림 등)
                        Toast.makeText(this@LoginActivity, "실패: ${result?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // 😱 통신 오류 (서버 꺼짐, 인터넷 안됨 등)
                    Toast.makeText(this@LoginActivity, "오류: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("LOGIN", "에러 발생", t)
                }
            })
        }
    }
}