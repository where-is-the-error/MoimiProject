package com.moimiApp.moimi

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// [중요] AppCompatActivity 대신 BaseActivity 상속!
class ChatListActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list_screen)

        // 1. 햄버거 메뉴, 로고, 알림 버튼 기능 자동 장착
        setupDrawer()

        // 2. 리사이클러뷰(채팅 목록) 설정
        val rvChatList = findViewById<RecyclerView>(R.id.rv_chat_list_container)
        rvChatList.layoutManager = LinearLayoutManager(this)

        // 나중에 여기에 어댑터 연결 코드 작성 (rvChatList.adapter = ...)
    }
}