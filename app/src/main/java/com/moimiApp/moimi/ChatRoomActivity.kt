package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView // 제목 변경용
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatRoomActivity : BaseActivity() {

    private val msgList = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView

    // [수정] 하드코딩 제거 -> 실제 변수로 선언
    private var roomId: String = ""
    private var myName: String = "" // 내 이름 (SharedPreferences에서 가져와야 함)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen) // XML 파일명 확인!

        setupDrawer()

        // 🟢 [추가] 저장된 내 이름 가져오기
        myName = prefsManager.getUserName() ?: "알 수 없음"

        // (선택) 이전 화면에서 방 제목/ID 넘겨받기
        intent.getStringExtra("roomTitle")?.let {
            // 툴바 제목 변경 로직이 있다면 여기에 작성
        }
        // [추가] Intent로 전달받은 방 ID와 제목 가져오기
        roomId = intent.getStringExtra("roomId") ?: ""
        val roomTitle = intent.getStringExtra("roomTitle") ?: "채팅방"

        // [추가] 내 이름 가져오기 (메시지 보낼 때 '나'인지 확인용)
        myName = prefsManager.getUserName() ?: "나"

        // 방 제목 설정
        val tvTitle = findViewById<TextView>(R.id.tv_chat_room_title)
        tvTitle.text = roomTitle

        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        val etInput = findViewById<EditText>(R.id.et_chat_input)
        rvMessages = findViewById(R.id.rv_chat_room_messages)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // 아래부터 쌓임
        rvMessages.layoutManager = layoutManager

        adapter = ChatAdapter(msgList)
        rvMessages.adapter = adapter

        fetchChatHistory()

        // 서버에서 채팅 내역 불러오기
        if (roomId.isNotEmpty()) {
            fetchChatHistory()
        } else {
            Toast.makeText(this, "잘못된 채팅방 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 전송 버튼 클릭
        btnSend.setOnClickListener {
            val text = etInput.text.toString()
            if (text.isNotEmpty()) {
                sendMessageToServer(text)
                etInput.text.clear()
            }
        }
    }

    private fun fetchChatHistory() {
        val token = getAuthToken() // [수정] 실제 토큰 사용

        RetrofitClient.chatInstance.getChatHistory(token, roomId)
            .enqueue(object : Callback<ChatHistoryResponse> {
                override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverChats = response.body()!!.chats
                        msgList.clear()
                        for (chat in serverChats) {
                            // 내 메시지인지 판단
                            val isMe = (chat.sender.name == myName)
                            msgList.add(
                                ChatMessage(
                                    content = chat.message,
                                    time = chat.createdAt,
                                    isMe = isMe,
                                    senderName = chat.sender.name
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                        if (msgList.isNotEmpty()) rvMessages.scrollToPosition(msgList.size - 1)
                    }
                }
                override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                    Toast.makeText(this@ChatRoomActivity, "채팅 로드 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessageToServer(message: String) {
        val token = getAuthToken() // [수정] 실제 토큰 사용
        val request = SendMessageRequest(roomId, message)

        RetrofitClient.chatInstance.sendMessage(token, request)
            .enqueue(object : Callback<SendMessageResponse> {
                override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val newChat = response.body()!!.chat
                        val myMsg = ChatMessage(newChat.message, "방금", true, myName)
                        msgList.add(myMsg)
                        adapter.notifyItemInserted(msgList.size - 1)
                        rvMessages.smoothScrollToPosition(msgList.size - 1)
                    }
                }
                override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                    Toast.makeText(this@ChatRoomActivity, "전송 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }
}