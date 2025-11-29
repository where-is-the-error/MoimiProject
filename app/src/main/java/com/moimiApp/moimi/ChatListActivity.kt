package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list_screen)
        setupDrawer()

        val recyclerView = findViewById<RecyclerView>(R.id.rv_chat_list_container)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ⚠️ ChatRoom은 이제 (제목, 내용) 2개만 받습니다. 에러 해결!
        val chatRooms = listOf(
            ChatRoom("철수와의 대화", "안녕? 뭐해?"),
            ChatRoom("모이미 프로젝트방", "내일 회의 시간 언제인가요?"),
            ChatRoom("가족 단톡방", "오늘 저녁 뭐 먹을래?")
        )

        val adapter = ChatListAdapter(chatRooms) { chatRoom ->
            val intent = Intent(this, ChatRoomActivity::class.java)
            intent.putExtra("roomTitle", chatRoom.title)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }
}