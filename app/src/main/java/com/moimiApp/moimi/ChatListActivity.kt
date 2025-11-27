package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatListActivity : BaseActivity() {

    private val chatRoomList = mutableListOf<ChatRoom>()
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list_screen)

        setupDrawer()

        val rvChatList = findViewById<RecyclerView>(R.id.rv_chat_list_container)
        rvChatList.layoutManager = LinearLayoutManager(this)

        // 1. ChatListAdapter 초기화 (오류 해결: 필요한 2개의 매개변수를 전달)
        adapter = ChatListAdapter(chatRoomList) { clickedRoom ->
            // 클릭 시 채팅방으로 이동
            val intent = Intent(this, ChatRoomActivity::class.java)
            intent.putExtra("roomId", clickedRoom.id)
            intent.putExtra("roomTitle", clickedRoom.title)
            startActivity(intent)
        }
        rvChatList.adapter = adapter

        // ⭐ 2. 서버에서 채팅방 목록 로드 시작 ⭐
        fetchChatRoomList()
    }

    // ----------------------------------------------------------------
    // [추가] 서버에서 채팅방 목록을 가져오는 함수
    // ----------------------------------------------------------------
    private fun fetchChatRoomList() {
        // BaseActivity의 getAuthToken()을 사용하여 토큰을 가져옵니다.
        val token = getAuthToken()

        // API 호출 (서버의 meetings 리스트를 채팅방 목록으로 사용)
        RetrofitClient.instance.getMeetings(token)
            .enqueue(object : Callback<MeetingListResponse> {
                override fun onResponse(call: Call<MeetingListResponse>, response: Response<MeetingListResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val meetings = response.body()!!.meetings

                        chatRoomList.clear()

                        // Meeting DTO를 ChatRoom DTO로 변환하여 리스트에 추가
                        for (meeting in meetings) {
                            // ChatRoom DTO에 맞춰 데이터 매핑
                            chatRoomList.add(
                                ChatRoom(
                                    id = meeting.id,
                                    title = meeting.title,
                                    lastMessage = "대화 내역을 불러오는 중...", // 초기 값
                                    time = meeting.dateTime,
                                    userCount = 0
                                )
                            )
                        }

                        adapter.notifyDataSetChanged()
                    } else {
                        // 로그인 토큰 오류, 서버 응답 오류 처리
                        Toast.makeText(this@ChatListActivity, "목록 로드 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MeetingListResponse>, t: Throwable) {
                    Toast.makeText(this@ChatListActivity, "서버 연결 오류", Toast.LENGTH_LONG).show()
                    Log.e("ChatList", "Error: ${t.message}")
                }
            })
    }
}