package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

// BaseActivity를 상속받아 메뉴 기능 자동 장착!
class ChatRoomActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)

        // 1. 공통 기능 연결 (메뉴, 로고, 벨)
        setupDrawer()

        // 2. 채팅방 고유 기능 (메시지 전송 버튼 등)
        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        val etInput = findViewById<EditText>(R.id.et_chat_input)

        btnSend.setOnClickListener {
            val message = etInput.text.toString()
            if (message.isNotEmpty()) {
                // 메시지 전송 로직 (일단 토스트 메시지로 확인)
                Toast.makeText(this, "전송됨: $message", Toast.LENGTH_SHORT).show()
                etInput.text.clear() // 입력창 비우기
            }
        }
    }
}