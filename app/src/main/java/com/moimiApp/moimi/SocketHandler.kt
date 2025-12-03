package com.moimiApp.moimi

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketHandler {
    private var mSocket: Socket? = null

    @Synchronized
    fun setSocket() {
        try {
            // ✅ 이미 연결되어 있다면 새로 만들지 않고 재사용 (중요!)
            if (mSocket != null && (mSocket!!.connected() || mSocket!!.isActive)) {
                Log.d("SocketHandler", "⚠️ 소켓이 이미 활성 상태입니다. 재사용합니다.")
                return
            }

            // 옵션: 자동 재연결 설정
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                timeout = 20000
                forceNew = false // 기존 연결 공유
            }

            mSocket = IO.socket(Constants.SOCKET_URL, options)
            Log.d("SocketHandler", "✅ 소켓 인스턴스 생성 완료: ${Constants.SOCKET_URL}")

        } catch (e: URISyntaxException) {
            e.printStackTrace()
            Log.e("SocketHandler", "❌ 소켓 URL 문법 오류")
        }
    }

    @Synchronized
    fun getSocket(): Socket {
        if (mSocket == null) {
            setSocket()
        }
        return mSocket!!
    }

    @Synchronized
    fun establishConnection() {
        mSocket?.let {
            if (!it.connected()) {
                it.connect()
                Log.d("SocketHandler", "🔄 소켓 연결 시도 중...")
            } else {
                Log.d("SocketHandler", "ℹ️ 소켓이 이미 연결되어 있습니다.")
            }
        }
    }

    @Synchronized
    fun closeConnection() {
        mSocket?.disconnect()
        Log.d("SocketHandler", "🔌 소켓 연결 종료")
    }
}