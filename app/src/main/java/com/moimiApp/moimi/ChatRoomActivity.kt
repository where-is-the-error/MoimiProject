package com.moimiApp.moimi

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatRoomActivity : BaseActivity() {

    private val msgList = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var tvFirstGreeting: TextView

    private var roomId: String = ""
    private var myName: String = ""
    private lateinit var mSocket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)
        setupDrawer()

        roomId = intent.getStringExtra("roomId") ?: ""
        val roomTitle = intent.getStringExtra("roomTitle") ?: "채팅방"
        myName = prefsManager.getUserName() ?: ""

        try {
            SocketHandler.setSocket()
            mSocket = SocketHandler.getSocket()
            if (!mSocket.connected()) mSocket.connect()
            mSocket.emit("joinRoom", roomId)
            mSocket.on("chatMessage", onNewMessage)
        } catch (e: Exception) {
            Log.e("ChatRoom", "소켓 연결 오류", e)
        }

        val tvTitle = findViewById<TextView>(R.id.tv_chat_room_title)
        tvTitle.text = roomTitle

        tvFirstGreeting = findViewById(R.id.tv_first_greeting)
        val partnerName = roomTitle.replace("님과의 대화", "")
        tvFirstGreeting.text = "${partnerName}님과의 첫 대화입니다\n반갑게 인사해보세요!!"

        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        val btnAddFriend = findViewById<ImageButton>(R.id.btn_chat_add_friend)
        val etInput = findViewById<EditText>(R.id.et_chat_input)
        rvMessages = findViewById(R.id.rv_chat_room_messages)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
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

    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                val message = data.getString("message")

                val senderName = if (data.has("sender")) {
                    val senderObj = data.getJSONObject("sender")
                    senderObj.getString("name")
                } else {
                    data.optString("senderName", "알 수 없음")
                }

                val createdAt = data.optString("createdAt", data.optString("timestamp"))

                if (senderName != myName) {
                    val parsedDate = parseIsoTime(createdAt)
                    val timeStr = formatTime(parsedDate)
                    val dateStr = formatDate(parsedDate)

                    // DataModels.kt 수정으로 파라미터가 5개여야 정상입니다.
                    val chatMsg = ChatMessage(message, timeStr, dateStr, false, senderName)
                    msgList.add(chatMsg)
                    adapter.notifyItemInserted(msgList.size - 1)
                    rvMessages.smoothScrollToPosition(msgList.size - 1)
                    tvFirstGreeting.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("Socket", "메시지 수신 에러", e)
            }
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
                            val isMe = (chat.senderName == myName)
                            val parsedDate = parseIsoTime(chat.timestamp)
                            val timeStr = formatTime(parsedDate)
                            val dateStr = formatDate(parsedDate)

                            msgList.add(ChatMessage(chat.content, timeStr, dateStr, isMe, chat.senderName))
                        }
                        adapter.notifyDataSetChanged()

                        if (msgList.isNotEmpty()) {
                            rvMessages.scrollToPosition(msgList.size - 1)
                            tvFirstGreeting.visibility = View.GONE
                        } else {
                            tvFirstGreeting.visibility = View.VISIBLE
                        }
                    }
                }
                override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {}
            })
    }

    private fun sendMessageToServer(message: String) {
        val token = getAuthToken()
        val request = SendMessageRequest(roomId, message)

        RetrofitClient.chatInstance.sendMessage(token, request)
            .enqueue(object : Callback<SendMessageResponse> {
                override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val newChat = response.body()!!.chat
                        val parsedDate = parseIsoTime(newChat.timestamp)
                        val timeStr = formatTime(parsedDate)
                        val dateStr = formatDate(parsedDate)

                        val myMsg = ChatMessage(newChat.content, timeStr, dateStr, true, myName)
                        msgList.add(myMsg)
                        adapter.notifyItemInserted(msgList.size - 1)
                        rvMessages.smoothScrollToPosition(msgList.size - 1)
                        tvFirstGreeting.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                    Toast.makeText(this@ChatRoomActivity, "전송 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showInviteDialog() {
        // ... (친구 초대 로직 - 이전과 동일하거나 생략 가능)
        Toast.makeText(this, "초대 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }

    // 시간 변환 유틸리티
    private fun parseIsoTime(isoTime: String): Date? {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            inputFormat.parse(isoTime)
        } catch (e: Exception) {
            Date()
        }
    }

    private fun formatTime(date: Date?): String {
        val outputFormat = SimpleDateFormat("a h:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return outputFormat.format(date ?: Date())
    }

    private fun formatDate(date: Date?): String {
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return outputFormat.format(date ?: Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mSocket.emit("leaveRoom", roomId)
            mSocket.off("chatMessage", onNewMessage)
        } catch (e: Exception) {}
    }
}