package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatListActivity : BaseActivity() {

    private val chatRoomList = mutableListOf<ChatRoomItem>()
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list_screen)
        setupDrawer()

        val recyclerView = findViewById<RecyclerView>(R.id.rv_chat_list_container)
        val btnAddChat = findViewById<ImageView>(R.id.btn_add_chat)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ChatListAdapter(chatRoomList) { chatRoom ->
            val intent = Intent(this, ChatRoomActivity::class.java)
            intent.putExtra("roomId", chatRoom.id)
            intent.putExtra("roomTitle", chatRoom.title)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // 채팅방 추가 버튼 클릭
        btnAddChat.setOnClickListener {
            showCreateChatDialog()
        }

        // 화면 진입 시 목록 로드
        fetchChatRooms()
    }

    override fun onResume() {
        super.onResume()
        fetchChatRooms()
    }

    private fun showCreateChatDialog() {
        val input = EditText(this)
        input.hint = "친구 이메일 입력"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(this)
            .setTitle("새로운 대화 시작")
            .setMessage("대화할 상대방의 이메일을 입력해주세요.")
            .setView(input)
            // ⭐ [수정] 버튼 텍스트 변경: "확인" -> "요청"
            .setPositiveButton("요청") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    createPrivateChat(email)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ⭐ [수정] 에러 메시지를 상세하게 파싱하여 토스트로 출력
    private fun createPrivateChat(targetEmail: String) {
        val token = getAuthToken()
        val request = CreatePrivateChatRequest(targetEmail)

        RetrofitClient.chatInstance.createPrivateChat(token, request)
            .enqueue(object : Callback<CreatePrivateChatResponse> {
                override fun onResponse(call: Call<CreatePrivateChatResponse>, response: Response<CreatePrivateChatResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        Toast.makeText(this@ChatListActivity, body.message, Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@ChatListActivity, ChatRoomActivity::class.java)
                        intent.putExtra("roomId", body.roomId)
                        intent.putExtra("roomTitle", body.title)
                        startActivity(intent)
                    } else {
                        // 실패 시 서버가 보낸 진짜 이유(message)를 추출
                        val errorMsg = try {
                            val errorBody = response.errorBody()?.string()
                            val json = Gson().fromJson(errorBody, JsonObject::class.java)
                            json.get("message").asString
                        } catch (e: Exception) {
                            "생성 실패 (코드: ${response.code()})"
                        }
                        Toast.makeText(this@ChatListActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e("ChatList", "채팅방 생성 실패: $errorMsg")
                    }
                }
                override fun onFailure(call: Call<CreatePrivateChatResponse>, t: Throwable) {
                    Toast.makeText(this@ChatListActivity, "오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

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