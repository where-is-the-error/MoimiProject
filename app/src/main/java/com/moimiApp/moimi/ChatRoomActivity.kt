package com.moimiApp.moimi

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRoomActivity : BaseActivity() {

    private val msgList = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView

    private var roomId: String = ""
    private var myName: String = ""

    // 소켓 객체
    private lateinit var mSocket: Socket
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)

        setupDrawer()

        roomId = intent.getStringExtra("roomId") ?: ""
        val roomTitle = intent.getStringExtra("roomTitle") ?: "채팅방"
        myName = prefsManager.getUserName() ?: ""

        // 1. 소켓 초기화 및 연결
        SocketHandler.setSocket()
        mSocket = SocketHandler.getSocket()
        mSocket.connect()

        // 2. 방 입장 이벤트 전송
        mSocket.emit("joinRoom", roomId)

        // 3. 메시지 수신 리스너 등록
        mSocket.on("chatMessage", onNewMessage)

        val tvTitle = findViewById<TextView>(R.id.tv_chat_room_title)
        tvTitle.text = roomTitle

        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        val btnAddFriend = findViewById<ImageButton>(R.id.btn_chat_add_friend)
        val etInput = findViewById<EditText>(R.id.et_chat_input)
        rvMessages = findViewById(R.id.rv_chat_room_messages)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // 키보드 올라올 때 대비
        rvMessages.layoutManager = layoutManager

        adapter = ChatAdapter(msgList)
        rvMessages.adapter = adapter

        if (roomId.isNotEmpty()) {
            fetchChatHistory()
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString()
            if (text.isNotEmpty()) {
                sendMessageToServer(text)
                etInput.text.clear()
            }
        }

        btnAddFriend.setOnClickListener {
            showInviteDialog()
        }
    }

    // 소켓 메시지 수신 리스너
    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                val message = data.getString("message")
                // sender가 객체인지 문자열인지 확인 필요 (서버 구현에 따라 다름)
                // 위 서버 코드에서는 sender: { name: ... } 형태임
                val senderObj = data.getJSONObject("sender")
                val senderName = senderObj.getString("name")
                val createdAt = data.getString("createdAt")

                // 내가 보낸 건지 확인 (서버에서 내가 보낸 것도 소켓으로 옴)
                // 하지만 Retrofit onSuccess에서 이미 추가했다면 중복될 수 있음.
                // 보통은 내가 보낸건 바로 추가하고, 소켓에서는 senderId 비교해서 내꺼면 무시하거나,
                // Retrofit에서는 추가 안하고 소켓으로만 받는 방식 사용.
                // 여기서는 "이름"으로 단순 비교합니다.

                if (senderName != myName) { // 내 이름이 아니면 추가 (나는 이미 Retrofit 콜백에서 추가함)
                    val chatMsg = ChatMessage(message, formatTime(createdAt), false, senderName)
                    msgList.add(chatMsg)
                    adapter.notifyItemInserted(msgList.size - 1)
                    rvMessages.smoothScrollToPosition(msgList.size - 1)
                }
            } catch (e: Exception) {
                Log.e("Socket", "메시지 파싱 에러", e)
            }
        }
    }

    // 시간 포맷 변환 (ISO 8601 -> 오전/오후 HH:mm)
    private fun formatTime(isoTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("a h:mm", Locale.getDefault())
            val date = inputFormat.parse(isoTime)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "방금"
        }
    }

    private fun fetchChatHistory() {
        val token = getAuthToken()
        RetrofitClient.chatInstance.getChatHistory(token, roomId)
            .enqueue(object : Callback<ChatHistoryResponse> {
                override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverChats = response.body()!!.chats
                        msgList.clear()
                        for (chat in serverChats) {
                            // 시간 포맷 변환 적용
                            val timeStr = formatTime(chat.createdAt)
                            val isMe = (chat.sender.name == myName)
                            msgList.add(ChatMessage(chat.message, timeStr, isMe, chat.sender.name))
                        }
                        adapter.notifyDataSetChanged()
                        if (msgList.isNotEmpty()) rvMessages.scrollToPosition(msgList.size - 1)
                    }
                }
                override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {}
            })
    }

    private fun sendMessageToServer(message: String) {
        val token = getAuthToken()
        val request = SendMessageRequest(roomId, message)

        // 1. 서버에 저장 요청 (HTTP POST)
        RetrofitClient.chatInstance.sendMessage(token, request)
            .enqueue(object : Callback<SendMessageResponse> {
                override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // 2. 성공 시 내 화면에 즉시 추가
                        val currentTime = SimpleDateFormat("a h:mm", Locale.getDefault()).format(Date())
                        val myMsg = ChatMessage(message, currentTime, true, myName)
                        msgList.add(myMsg)
                        adapter.notifyItemInserted(msgList.size - 1)
                        rvMessages.smoothScrollToPosition(msgList.size - 1)

                        // *서버에서 socket.emit을 해주므로 상대방에겐 소켓으로 감
                    }
                }
                override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                    Toast.makeText(this@ChatRoomActivity, "전송 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 기존 친구 초대 로직 유지 ...
    private fun showInviteDialog() { /* ... (이전 코드와 동일) ... */ }
    private fun inviteFriendByEmail(email: String) { /* ... (이전 코드와 동일) ... */ }

    override fun onDestroy() {
        super.onDestroy()
        // 액티비티 종료 시 소켓 연결 해제 및 방 퇴장
        mSocket.emit("leaveRoom", roomId)
        mSocket.disconnect()
        mSocket.off("chatMessage", onNewMessage)
    }
}