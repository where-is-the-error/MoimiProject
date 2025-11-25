package com.moimiApp.moimi

import android.os.Bundle
import android.widget.CalendarView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.moimiApp.moimi.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddScheduleActivity : AppCompatActivity() {

    // 변수 선언
    private lateinit var etTitle: EditText
    private lateinit var etMemo: EditText
    private lateinit var calendarView: CalendarView
    private lateinit var btnAdd: TextView
    private var selectedDate: String = "" // 선택된 날짜 저장 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule) // XML 파일 연결

        // 1. View 연결 (XML의 ID와 코드를 연결)
        etTitle = findViewById(R.id.et_title)
        etMemo = findViewById(R.id.et_memo)
        calendarView = findViewById(R.id.calendarView)
        btnAdd = findViewById(R.id.btn_add)

        // 2. 날짜 선택 이벤트 리스너 (기본값: 오늘 날짜)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        selectedDate = today

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // 월(Month)은 0부터 시작하므로 +1 해줘야 함
            selectedDate = "$year-${month + 1}-$dayOfMonth"
        }

        // 3. '추가' 버튼 클릭 이벤트
        btnAdd.setOnClickListener {
            val title = etTitle.text.toString()
            val memo = etMemo.text.toString()

            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- [중요] 여기에 파이어베이스 저장 코드가 들어갑니다 ---
            // 예: saveScheduleToFirebase(title, memo, selectedDate)
            Toast.makeText(this, "제목: $title, 메모: $memo, 날짜: $selectedDate", Toast.LENGTH_LONG).show()


        }

        // 4. '취소' 버튼 기능 (화면 닫기)
        findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            finish()
        }
    }
}