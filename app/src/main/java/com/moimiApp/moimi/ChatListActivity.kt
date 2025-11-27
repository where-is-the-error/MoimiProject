package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatListActivity : BaseActivity() { // BaseActivity 상속

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list_screen)

        // 1. 메뉴 설정
        setupDrawer()

        // 2. 리사이클러뷰 설정
        val rvChatList = findViewById<RecyclerView>(R.id.rv_chat_list_container)
        rvChatList.layoutManager = LinearLayoutManager(this)

        // 3. 더미 데이터 (나중에 API로 교체)
        val chatRooms = listOf(
            ChatRoom("1", "동양식당 팟", "다들 어디야?", "오후 6:00", 4),
            ChatRoom("2", "개발 스터디", "오늘 모임 취소입니다", "오후 2:00", 6)
        )

        // 4. 어댑터 연결 (ChatListAdapter는 아까 만드셨다고 가정)
        rvChatList.adapter = ChatListAdapter(chatRooms) { chatRoom ->
            // 클릭 시 채팅방으로 이동
            val intent = Intent(this, ChatRoomActivity::class.java)
            intent.putExtra("roomTitle", chatRoom.title) // 방 제목 넘기기
            startActivity(intent)
        }
    }
}