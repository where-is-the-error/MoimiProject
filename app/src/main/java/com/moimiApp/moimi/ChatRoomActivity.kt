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

    // 데이터 리스트 (화면에 보여질 것)
    private val msgList = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView

    // ⚠️ [중요] 실제로는 로그인 후 저장된 토큰과, 이전 화면에서 넘겨받은 방 ID를 써야 합니다.
    private val myToken = "Bearer 여기에_실제_토큰_입력"
    private val roomId = "여기에_실제_방ID_입력"
    private val myName = "철수" // 내 이름 (isMe 판단용)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)

        setupDrawer() // 공통 기능

        // 1. 뷰 연결
        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        val etInput = findViewById<EditText>(R.id.et_chat_input)
        rvMessages = findViewById(R.id.rv_chat_room_messages)

        // 2. 리사이클러뷰 설정
        // stackFromEnd = true : 키보드 올라오거나 채팅 왔을 때 아래부터 채워짐
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager

        adapter = ChatAdapter(msgList)
        rvMessages.adapter = adapter

        // 3. 서버에서 채팅 내역 불러오기 (가짜 데이터 삭제됨)
        fetchChatHistory()

        // 4. 전송 버튼 클릭
        btnSend.setOnClickListener {
            val text = etInput.text.toString()
            if (text.isNotEmpty()) {
                sendMessageToServer(text) // 서버 전송 요청
                etInput.text.clear()      // 입력창 비우기
            }
        }
    }

    // [기능 1] 채팅 내역 불러오기
    private fun fetchChatHistory() {
        RetrofitClient.chatInstance.getChatHistory(myToken, roomId)
            .enqueue(object : Callback<ChatHistoryResponse> {
                override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverChats = response.body()!!.chats

                        // 서버 데이터를 화면용 데이터(ChatMessage)로 변환
                        msgList.clear()
                        for (chat in serverChats) {
                            val isMe = (chat.sender.name == myName) // 이름으로 내 메시지인지 확인 (임시)
                            msgList.add(
                                ChatMessage(
                                    content = chat.message,
                                    time = chat.createdAt, // 필요시 시간 포맷팅(오후 2:00) 변환 함수 추가 권장
                                    isMe = isMe,
                                    senderName = chat.sender.name
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                        if (msgList.isNotEmpty()) {
                            rvMessages.scrollToPosition(msgList.size - 1)
                        }
                    }
                }
                override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                    Toast.makeText(this@ChatRoomActivity, "채팅 불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // [기능 2] 메시지 서버로 전송하기
    private fun sendMessageToServer(message: String) {
        val request = SendMessageRequest(roomId, message)

        RetrofitClient.chatInstance.sendMessage(myToken, request)
            .enqueue(object : Callback<SendMessageResponse> {
                override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // 전송 성공! 화면에 내 메시지 추가
                        val newChat = response.body()!!.chat

                        val myMsg = ChatMessage(
                            content = newChat.message,
                            time = "방금",
                            isMe = true,
                            senderName = myName
                        )
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