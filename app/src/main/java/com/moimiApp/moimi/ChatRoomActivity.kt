package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// BaseActivity 상속
class ChatRoomActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)

        // 1. 공통 기능 (메뉴, 로고, 벨)
        setupDrawer()

        // 2. 뷰 연결
        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        val etInput = findViewById<EditText>(R.id.et_chat_input)
        val rvMessages = findViewById<RecyclerView>(R.id.rv_chat_room_messages)

        // 3. 리사이클러뷰 설정
        rvMessages.layoutManager = LinearLayoutManager(this)

        // 4. [가짜 데이터] 채팅 목록 만들기
        // (나중에 서버에서 받아온 대화 내용으로 교체될 부분)
        val msgList = mutableListOf(
            ChatMessage("안녕?", "오후 2:00", false, "철수"),
            ChatMessage("어디야?", "오후 2:01", false, "철수"),
            ChatMessage("나 거의 다 왔어!", "오후 2:02", true)
        )

        // 5. 어댑터 연결 (목록을 화면에 보여줌)
        val adapter = ChatAdapter(msgList)
        rvMessages.adapter = adapter

        // 6. 전송 버튼 클릭 기능 (기존 토스트 코드 대신 이걸 사용!)
        btnSend.setOnClickListener {
            val text = etInput.text.toString()

            if (text.isNotEmpty()) {
                // 1) 데이터 리스트에 내 메시지 추가 ("방금", true)
                msgList.add(ChatMessage(text, "방금", true))

                // 2) 어댑터에게 "새 데이터 들어왔어!" 알림 (화면 갱신)
                adapter.notifyItemInserted(msgList.size - 1)

                // 3) 스크롤을 맨 아래로 이동 (최신 메시지 보이게)
                rvMessages.scrollToPosition(msgList.size - 1)

                // 4) 입력창 비우기
                etInput.text.clear()
            }
        }
    }
}