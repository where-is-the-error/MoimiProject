package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

    // 🟢 [수정] 하드코딩 제거하고 실제 값 사용 (나중에 Intent로 roomId 받아야 함)
    private var roomId = "111111111111111111111112" // 임시 방 ID (데이터베이스에 있는 모임 ID)
    private var myName = "" // SharedPreferences에서 가져올 예정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)

        setupDrawer()

        // 🟢 [추가] 저장된 내 이름 가져오기
        myName = prefsManager.getUserName() ?: "알 수 없음"

        // (선택) 이전 화면에서 방 제목/ID 넘겨받기
        intent.getStringExtra("roomTitle")?.let {
            // 툴바 제목 변경 로직이 있다면 여기에 작성
        }

        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        val etInput = findViewById<EditText>(R.id.et_chat_input)
        rvMessages = findViewById(R.id.rv_chat_room_messages)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager

        adapter = ChatAdapter(msgList)
        rvMessages.adapter = adapter

        fetchChatHistory()

        btnSend.setOnClickListener {
            val text = etInput.text.toString()
            if (text.isNotEmpty()) {
                sendMessageToServer(text)
                etInput.text.clear()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchChatHistory()
    }

    private fun fetchChatHistory() {
        val token = getAuthToken() // 🟢 토큰 사용
        RetrofitClient.chatInstance.getChatHistory(token, roomId)
            .enqueue(object : Callback<ChatHistoryResponse> {
                override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverChats = response.body()!!.chats
                        msgList.clear()
                        for (chat in serverChats) {
                            val isMe = (chat.sender.name == myName) // 🟢 내 이름과 비교
                            msgList.add(ChatMessage(chat.message, chat.createdAt, isMe, chat.sender.name))
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
        val token = getAuthToken() // 🟢 토큰 사용
        val request = SendMessageRequest(roomId, message)

        RetrofitClient.chatInstance.sendMessage(token, request)
            .enqueue(object : Callback<SendMessageResponse> {
                override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val newChat = response.body()!!.chat
                        val myMsg = ChatMessage(newChat.message, "방금", true, myName)
                        msgList.add(myMsg)
                        adapter.notifyItemInserted(msgList.size - 1)
                        rvMessages.scrollToPosition(msgList.size - 1)
                    }
                }
                override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                    Toast.makeText(this@ChatRoomActivity, "전송 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }
}