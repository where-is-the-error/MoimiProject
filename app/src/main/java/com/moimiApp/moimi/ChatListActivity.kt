package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatListActivity : BaseActivity() {

    // 기존 ChatRoom 대신 서버 데이터 모델인 ChatRoomItem 사용
    private val chatRoomList = mutableListOf<ChatRoomItem>()
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list_screen)
        setupDrawer()

        val recyclerView = findViewById<RecyclerView>(R.id.rv_chat_list_container)
        val btnAddChat = findViewById<ImageView>(R.id.btn_add_chat)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 어댑터 설정 (기존 ChatListAdapter를 ChatRoomItem을 받도록 수정해야 할 수도 있음)
        // 여기서는 ChatListAdapter가 ChatRoom(데이터클래스)을 받는다고 가정하고 변환해서 넣거나,
        // ChatListAdapter 자체를 ChatRoomItem을 받도록 수정하는 것이 좋습니다.
        // 편의상 ChatListAdapter 코드를 아래 6번 항목에서 ChatRoomItem으로 수정해 드립니다.
        adapter = ChatListAdapter(chatRoomList) { chatRoom ->
            val intent = Intent(this, ChatRoomActivity::class.java)
            intent.putExtra("roomId", chatRoom.id) // Room ID 전달
            intent.putExtra("roomTitle", chatRoom.title)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // ⭐ [신규] 채팅방 추가 버튼 클릭
        btnAddChat.setOnClickListener {
            showCreateChatDialog()
        }

        // 화면 진입 시 목록 로드
        fetchChatRooms()
    }

    override fun onResume() {
        super.onResume()
        fetchChatRooms() // 목록 갱신
    }

    // ⭐ [신규] 이메일 입력 다이얼로그
    private fun showCreateChatDialog() {
        val input = EditText(this)
        input.hint = "친구 이메일 입력"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(this)
            .setTitle("새로운 대화 시작")
            .setMessage("대화할 상대방의 이메일을 입력해주세요.")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    createPrivateChat(email)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ⭐ [신규] 채팅방 생성 API 호출
    private fun createPrivateChat(targetEmail: String) {
        val token = getAuthToken()
        val request = CreatePrivateChatRequest(targetEmail)

        RetrofitClient.chatInstance.createPrivateChat(token, request)
            .enqueue(object : Callback<CreatePrivateChatResponse> {
                override fun onResponse(call: Call<CreatePrivateChatResponse>, response: Response<CreatePrivateChatResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        Toast.makeText(this@ChatListActivity, body.message, Toast.LENGTH_SHORT).show()

                        // 생성된 방으로 바로 이동
                        val intent = Intent(this@ChatListActivity, ChatRoomActivity::class.java)
                        intent.putExtra("roomId", body.roomId)
                        intent.putExtra("roomTitle", body.title)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@ChatListActivity, response.body()?.message ?: "생성 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CreatePrivateChatResponse>, t: Throwable) {
                    Toast.makeText(this@ChatListActivity, "오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ⭐ [신규] 채팅방 목록 불러오기
    private fun fetchChatRooms() {
        val token = getAuthToken()
        RetrofitClient.chatInstance.getMyChatRooms(token)
            .enqueue(object : Callback<ChatRoomListResponse> {
                override fun onResponse(call: Call<ChatRoomListResponse>, response: Response<ChatRoomListResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val rooms = response.body()!!.rooms
                        chatRoomList.clear()
                        chatRoomList.addAll(rooms)
                        adapter.notifyDataSetChanged()
                    }
                }
                override fun onFailure(call: Call<ChatRoomListResponse>, t: Throwable) {}
            })
    }
}