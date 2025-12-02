const http = require('http');
const express = require('express');
const { Server } = require('socket.io');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

// 1. Express 앱 생성
const app = express();

// 2. HTTP 서버 생성 (Express 앱을 감쌈)
const server = http.createServer(app);

// 3. 미들웨어 설정
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 4. DB 연결
const connectDB = require('./config/db');
connectDB();

// 5. Socket.IO 설정
const io = new Server(server, {
    cors: {
        origin: "*", // 모든 곳에서 접속 허용
        methods: ["GET", "POST"]
    }
});

// 라우터 파일에서 io를 쓸 수 있게 전역 객체로 등록 (req.app.get('io')로 사용 가능)
app.set('io', io);

// Socket.IO 이벤트 리스너
io.on('connection', (socket) => {
    console.log('🟢 새로운 소켓 연결됨:', socket.id);

    // 채팅방 입장
    socket.on('joinRoom', (roomId) => {
        socket.join(roomId);
        console.log(`👤 소켓 ${socket.id} -> 방 ${roomId} 입장`);
    });

    // 채팅방 퇴장
    socket.on('leaveRoom', (roomId) => {
        socket.leave(roomId);
        console.log(`👋 소켓 ${socket.id} -> 방 ${roomId} 퇴장`);
    });

    socket.on('disconnect', () => {
        console.log('🔴 소켓 연결 해제:', socket.id);
    });
});

// 6. 라우터 등록 (모든 API 연결)
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/users', require('./routes/user.routes'));
app.use('/api/schedules', require('./routes/schedule.routes'));
app.use('/api/meetings', require('./routes/meeting.routes'));
app.use('/api/chats', require('./routes/chat.routes'));
app.use('/api/notifications', require('./routes/notification.routes'));
app.use('/api/invite', require('./routes/invite.routes'));

// 7. 서버 시작
// 주의: app.listen이 아니라 server.listen을 사용해야 소켓이 작동함
const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 서버가 실행 중입니다: http://0.0.0.0:${PORT}`);
});