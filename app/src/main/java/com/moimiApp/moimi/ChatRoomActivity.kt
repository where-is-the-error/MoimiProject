package com.moimiApp.moimi

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

// 1. BaseActivity 상속 유지 (메뉴 기능 살리기 위해)
class ChatRoomActivity : BaseActivity() {

    private lateinit var mSocket: Socket
    private lateinit var etInput: EditText

    // ⚠️ 테스트용 하드코딩 값 (나중에는 Intent로 받아오도록 수정 필요)
    private val meetingId = "69287ea89977eee0911c7689" // 실제 모임 ID (Postman 테스트 했던 것)
    private val myUserId = "69287e009977eee0911c7682"  // 내 유저 ID (Postman 테스트 했던 것)
    private val myName = "철수"                         // 내 닉네임

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room_screen)

        // 2. BaseActivity의 공통 기능(메뉴, 로고 등) 연결
        setupDrawer()

        // 3. 뷰 연결 (findViewById 사용 - 기존 XML ID 유지)
        val btnSend = findViewById<Button>(R.id.btn_chat_send)
        etInput = findViewById<EditText>(R.id.et_chat_input)

        // 4. 소켓 초기화 및 연결
        // (SocketHandler 파일이 반드시 있어야 합니다!)
        try {
            SocketHandler.setSocket()
            SocketHandler.establishConnection()
            mSocket = SocketHandler.getSocket()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "소켓 연결 실패", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. 채팅방 입장 (Join Room)
        mSocket.emit("join_room", meetingId)

        // 6. 메시지 수신 리스너 등록
        mSocket.on("receive_message", onNewMessage)

        // 7. 전송 버튼 클릭 이벤트
        btnSend.setOnClickListener {
            val content = etInput.text.toString()
            if (content.isNotEmpty()) {
                sendMessage(content)
                etInput.text.clear() // 입력창 비우기
            }
        }
    }

    // 메시지 전송 함수 (Emit)
    private fun sendMessage(content: String) {
        val messageJson = JSONObject()
        try {
            messageJson.put("meetingId", meetingId)
            messageJson.put("senderId", myUserId)
            messageJson.put("senderName", myName)
            messageJson.put("content", content)

            mSocket.emit("send_message", messageJson)

            // (선택) 내가 보낸 메시지도 화면에 바로 띄우고 싶다면 여기서 처리 가능
            // 하지만 보통은 receive_message에서 받아서 처리하는 것이 정석입니다.

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 메시지 수신 리스너 (서버 -> 나)
    private val onNewMessage = Emitter.Listener { args ->
        // UI 업데이트는 반드시 메인 스레드(runOnUiThread)에서!
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                val senderName = data.getString("senderName")
                val content = data.getString("content")

                Log.d("Chat", "수신됨 - $senderName: $content")

                // 일단 토스트로 확인 (성공하면 나중에 리사이클러뷰로 교체)
                Toast.makeText(this@ChatRoomActivity, "$senderName: $content", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 앱 종료 시 소켓 정리
        if (::mSocket.isInitialized) {
            mSocket.off("receive_message", onNewMessage)
            SocketHandler.closeConnection()
        }
    }
}