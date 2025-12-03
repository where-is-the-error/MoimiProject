package com.moimiApp.moimi

import android.os.Bundle
import android.util.Log
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
    private var myUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)

        // Intent 데이터 수신
        roomId = intent.getStringExtra("roomId") ?: ""
        roomTitle = intent.getStringExtra("roomTitle") ?: "채팅방"
        myUserId = prefsManager.getUserId() ?: ""
        myUserName = prefsManager.getUserName() ?: ""

        // 뷰 초기화
        val tvTitle = findViewById<TextView>(R.id.tv_chat_room_name)
        val btnBack = findViewById<ImageButton>(R.id.btn_back_chat)
        rvChat = findViewById(R.id.rv_chat_messages)
        etMessage = findViewById(R.id.et_chat_input)
        btnSend = findViewById(R.id.btn_chat_send)

        tvTitle.text = roomTitle
        btnBack.setOnClickListener { finish() }

        // 리사이클러뷰 설정
        adapter = ChatAdapter(chatList, myUserId)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // 키보드 올라올 때 메시지가 위로 밀려 올라가도록 설정
        rvChat.layoutManager = layoutManager
        rvChat.adapter = adapter

        // 1. 소켓 연결 및 리스너 등록
        initSocket()

        // 2. HTTP로 이전 대화 내용 불러오기
        loadChatHistory()

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
        // 소켓 인스턴스 가져오기 (싱글톤)
        SocketHandler.setSocket()
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()

        // 🟢 [1] 소켓 연결 성공 시 -> 방 입장 시도
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.d("ChatRoom", "🟢 소켓 연결됨 (${mSocket.id()}). 방 입장 시도: $roomId")
            joinRoom()
        }

        // 🔴 [2] 연결 에러
        mSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("ChatRoom", "🔴 소켓 연결 에러: ${args.firstOrNull()}")
        }

        // 📩 [3] 실시간 메시지 수신 (남이 보낸 것)
        mSocket.on("chatMessage") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                Log.d("ChatRoom", "📥 실시간 메시지 수신: $data")

                val message = data.optString("message")
                val senderObj = data.optJSONObject("sender")
                val senderName = senderObj?.optString("name") ?: "알 수 없음"
                val senderProfileImg = senderObj?.optString("profileImg", null)
                val time = data.optString("createdAt") // ISO 포맷

                // 내가 보낸 메시지가 소켓으로 다시 돌아올 경우 중복 표시 방지
                if (senderName == myUserName) {
                    return@on
                }

                runOnUiThread {
                    addMessageToView(ChatMessage(
                        content = message,
                        time = formatTime(time),
                        rawDate = time,
                        isMe = false, // 타인 메시지
                        senderName = senderName,
                        senderProfileImg = senderProfileImg
                    ))
                    // 새 메시지 왔으므로 읽음 처리
                    markAsRead()
                }
            }
        }

        // 화면에 들어왔는데 이미 소켓이 연결된 상태라면 즉시 입장 요청
        if (mSocket.connected()) {
            joinRoom()
        }
    }

    // 서버로 'joinRoom' 이벤트 전송
    private fun joinRoom() {
        try {
            val joinData = JSONObject()
            joinData.put("roomId", roomId)
            joinData.put("userId", myUserId)

            // 서버 코드 수정으로 이제 이 객체를 정상적으로 처리할 수 있음
            mSocket.emit("joinRoom", joinData)
            Log.d("ChatRoom", "🚪 joinRoom 요청 보냄: $joinData")
        } catch (e: Exception) {
            Log.e("ChatRoom", "joinRoom 데이터 생성 실패", e)
        }
    }

    // HTTP: 이전 대화 기록 로드
    private fun loadChatHistory() {
        val token = getAuthToken()
        RetrofitClient.chatInstance.getChatHistory(token, roomId).enqueue(object : Callback<ChatHistoryResponse> {
            override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val logs = response.body()?.chats ?: emptyList()
                    chatList.clear()

                    for (log in logs) {
                        val isMe = (log.senderName == myUserName)
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
                    if (chatList.isNotEmpty()) {
                        rvChat.scrollToPosition(chatList.size - 1)
                    }
                    markAsRead()
                }
            }
            override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                Log.e("ChatRoom", "대화 기록 로드 실패", t)
            }
        })
    }

    // 메시지 전송 (HTTP API 호출) -> 성공 시 서버가 소켓으로 브로드캐스트 해줄 것임
    private fun sendMessage(msg: String) {
        val token = getAuthToken()
        val request = SendMessageRequest(roomId, msg)

        // 1. 내 화면에는 즉시 추가 (UX 향상)
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        addMessageToView(ChatMessage(msg, formatTime(now), now, true, "나"))

        // 2. 서버로 전송
        RetrofitClient.chatInstance.sendMessage(token, request).enqueue(object : Callback<SendMessageResponse> {
            override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@ChatRoomActivity, "메시지 전송 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                Toast.makeText(this@ChatRoomActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMessageToView(msg: ChatMessage) {
        chatList.add(msg)
        adapter.notifyItemInserted(chatList.size - 1)
        rvChat.scrollToPosition(chatList.size - 1)
    }

    // 메시지 읽음 처리
    private fun markAsRead() {
        val token = getAuthToken()
        RetrofitClient.chatInstance.markAsRead(token, roomId).enqueue(object : Callback<CommonResponse> {
            override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {}
            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {}
        })
    }

    // 시간 포맷 (ISO -> "오후 3:00")
    private fun formatTime(isoString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // 서버 시간은 UTC라고 가정
            val date = inputFormat.parse(isoString)

            val outputFormat = SimpleDateFormat("a h:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            "" // 변환 실패 시 공백
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 방을 나가는 처리는 하지 않음 (앱이 백그라운드에 있어도 소켓 유지)
        // 채팅방 화면 리스너만 제거
        mSocket.off("chatMessage")
    }
}