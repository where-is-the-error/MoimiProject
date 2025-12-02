package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ScheduleDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_detail)

        // 1. Intent 데이터 받기
        val title = intent.getStringExtra("title") ?: "제목 없음"
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val location = intent.getStringExtra("location") ?: "장소 정보 없음"
        val scheduleId = intent.getStringExtra("scheduleId") ?: ""

        // 2. 뷰 연결
        val tvTitle = findViewById<TextView>(R.id.tv_detail_title)
        val tvDateTime = findViewById<TextView>(R.id.tv_detail_date_time)
        val tvLocation = findViewById<TextView>(R.id.tv_detail_location)
        val btnClose = findViewById<ImageView>(R.id.btn_close)
        val btnInvite = findViewById<MaterialButton>(R.id.btn_invite_friends)

        // 3. 데이터 표시
        tvTitle.text = title
        tvDateTime.text = "$date  |  $time"
        tvLocation.text = location

        // 4. 닫기 버튼
        btnClose.setOnClickListener {
            finish()
        }

        // 5. 초대하기 버튼 (링크 공유)
        btnInvite.setOnClickListener {
            if (scheduleId.isNotEmpty()) {
                shareInviteLink(title, scheduleId)
            } else {
                Toast.makeText(this, "일정 ID가 없어 초대할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareInviteLink(title: String, scheduleId: String) {
        val inviteUrl = "http://moimi.app/invite/$scheduleId"

        val shareText = """
            [모이미 초대장]
            '$title' 일정에 초대합니다!
            
            참여 코드: $scheduleId
            링크: $inviteUrl
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "친구에게 초대장 보내기"))
    }
}