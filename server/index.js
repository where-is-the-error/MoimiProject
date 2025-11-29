require('dotenv').config();
const express = require('express');
const cors = require('cors');
const http = require('http'); // HTTP 모듈 추가 (Socket.io 통합을 위해 필요)
const { Server } = require("socket.io"); // Socket.io 서버 객체 추가
const connectDB = require('./config/db');

// 기존 라우트 임포트
const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const meetingRoutes = require('./routes/meeting.routes');
const inviteRoutes = require('./routes/invite.routes');
const notificationRoutes = require('./routes/notification.routes');
const chatRoutes = require('./routes/chat.routes'); // ✅ 채팅 기록 라우트 임포트

const Message = require('./models/Message'); // ✅ 메시지 모델 임포트

const app = express();
const server = http.createServer(app); // ✅ Express 앱으로 HTTP 서버 생성
const io = new Server(server, {        // ✅ HTTP 서버를 기반으로 Socket.io 서버 생성
    cors: {
        origin: "*", // 모든 출처 허용 (안드로이드 클라이언트가 접속할 수 있도록)
        methods: ["GET", "POST"]
    }
});

const PORT = process.env.PORT || 3000;

connectDB();

app.use(cors());
app.use(express.json());

// 로깅 미들웨어
app.use((req, res, next) => {
    const now = new Date().toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' });
    console.log(`[${now}] ${req.method} ${req.originalUrl}`);
    next();
});

// 라우트 설정
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/meetings', meetingRoutes);
app.use('/api/invites', inviteRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/chats', chatRoutes); // ✅ 채팅 기록 API 연결


// -----------------------------------------------------
// 🚀 Socket.io 실시간 채팅 로직
// -----------------------------------------------------
io.on('connection', (socket) => {
    console.log(`User Connected: ${socket.id}`);

    // [Step 1] 클라이언트가 특정 모임(채팅방)에 입장할 때 호출
    socket.on('join_room', (meetingId) => {
        socket.join(meetingId);
        console.log(`User with ID: ${socket.id} joined room: ${meetingId}`);
    });

    // [Step 2] 클라이언트가 메시지를 보낼 때 호출
    socket.on('send_message', async (data) => {
        // data 구조: { meetingId, senderId, senderName, content }
        console.log("Message Received:", data);

        try {
            // 1. DB에 메시지 저장
            const newMessage = new Message({
                meetingId: data.meetingId,
                senderId: data.senderId,
                senderName: data.senderName,
                content: data.content,
                timestamp: new Date()
            });
            await newMessage.save();

            // 2. 같은 방에 있는 모든 클라이언트에게 메시지 전송 (실시간)
            io.to(data.meetingId).emit('receive_message', data);
            
        } catch (error) {
            console.error("메시지 저장 및 전송 실패:", error);
        }
    });

    socket.on('disconnect', () => {
        console.log("User Disconnected", socket.id);
    });
});

app.listen(PORT, '0.0.0.0', () => { // 👈 여기에 '0.0.0.0'을 추가해야 합니다!
    console.log(`🚀 모이미 서버 실행 중: http://localhost:${PORT}`);
    console.log(`(외부 접속 주소: 0.0.0.0:${PORT})`); // 확인용 로그 추가
    });