package com.moimiApp.moimi

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.Socket
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatRoomActivity : BaseActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()

    private var roomId: String = ""
    private var roomTitle: String = ""
    private lateinit var mSocket: Socket
    private var myUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen) // ✅ XML 파일명 확인 필수!

        // Intent 데이터 수신
        roomId = intent.getStringExtra("roomId") ?: ""
        roomTitle = intent.getStringExtra("roomTitle") ?: "채팅방"
        myUserId = prefsManager.getUserId() ?: ""

        // ✅ 뷰 연결 (XML ID와 정확히 일치해야 함)
        val tvTitle = findViewById<TextView>(R.id.tv_chat_room_name)
        val btnBack = findViewById<ImageButton>(R.id.btn_back_chat)
        rvChat = findViewById(R.id.rv_chat_messages)
        etMessage = findViewById(R.id.et_chat_input)
        btnSend = findViewById(R.id.btn_chat_send)

        // 초기화
        tvTitle.text = roomTitle
        btnBack.setOnClickListener { finish() }

        adapter = ChatAdapter(chatList, myUserId)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        // 기능 실행
        try {
            initSocket()
            loadChatHistory()
            markAsRead()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "초기화 중 오류 발생", Toast.LENGTH_SHORT).show()
        }

        // 전송 버튼
        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                etMessage.text.clear()
            }
        }
    }

    private fun initSocket() {
        SocketHandler.setSocket()
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()

        val joinData = JSONObject()
        joinData.put("roomId", roomId)
        mSocket.emit("joinRoom", joinData)

        mSocket.on("chatMessage") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val message = data.optString("message")
                val senderObj = data.optJSONObject("sender")
                val senderName = senderObj?.optString("name") ?: "알 수 없음"
                val senderProfileImg = senderObj?.optString("profileImg", null)
                val time = data.optString("createdAt")

                runOnUiThread {
                    addMessageToView(ChatMessage(
                        content = message,
                        time = formatTime(time),
                        rawDate = time,
                        isMe = false,
                        senderName = senderName,
                        senderProfileImg = senderProfileImg
                    ))
                    markAsRead()
                }
            }
        }
    }

    private fun loadChatHistory() {
        val token = getAuthToken()
        RetrofitClient.chatInstance.getChatHistory(token, roomId).enqueue(object : Callback<ChatHistoryResponse> {
            override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val logs = response.body()?.chats ?: emptyList()
                    chatList.clear()
                    for (log in logs) {
                        val isMe = (log.senderName == prefsManager.getUserName())
                        chatList.add(ChatMessage(
                            content = log.content,
                            time = formatTime(log.timestamp),
                            rawDate = log.timestamp,
                            isMe = isMe,
                            senderName = log.senderName,
                            senderProfileImg = log.senderProfileImg
                        ))
                    }
                    adapter.notifyDataSetChanged()
                    if (chatList.isNotEmpty()) rvChat.scrollToPosition(chatList.size - 1)
                }
            }
            override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {}
        })
    }

    private fun sendMessage(msg: String) {
        val token = getAuthToken()
        val request = SendMessageRequest(roomId, msg)

        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        addMessageToView(ChatMessage(msg, formatTime(now), now, true, "나"))

        RetrofitClient.chatInstance.sendMessage(token, request).enqueue(object : Callback<SendMessageResponse> {
            override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {}
            override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                Toast.makeText(this@ChatRoomActivity, "전송 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMessageToView(msg: ChatMessage) {
        chatList.add(msg)
        adapter.notifyItemInserted(chatList.size - 1)
        rvChat.scrollToPosition(chatList.size - 1)
    }

    private fun markAsRead() {
        val token = getAuthToken()
        RetrofitClient.chatInstance.markAsRead(token, roomId).enqueue(object : Callback<CommonResponse> {
            override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {}
            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {}
        })
    }

    private fun formatTime(isoString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoString)
            val outputFormat = SimpleDateFormat("a h:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) { "" }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            val leaveData = JSONObject()
            leaveData.put("roomId", roomId)
            mSocket.emit("leaveRoom", leaveData)
        } catch (e: Exception) {}
    }
}