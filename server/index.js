const http = require('http');
const express = require('express');
const { Server } = require('socket.io');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

// 모델 임포트 (시연용 데이터 초기화 API에서 사용)
const User = require('./models/User');
const Meeting = require('./models/Meeting'); // Plan 대신 Meeting 사용 (최신 로직 반영)

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

// 라우터 파일에서 io를 쓸 수 있게 전역 객체로 등록
app.set('io', io);

// ==========================================
// ⚡ Socket.IO 이벤트 리스너 (위치 공유 + 채팅)
// ==========================================
io.on('connection', (socket) => {
    console.log('🟢 새로운 소켓 연결됨:', socket.id);

    // 1. 방 입장
    socket.on('joinRoom', (data) => {
        const roomId = (typeof data === 'object') ? data.roomId : data;
        const userId = (typeof data === 'object') ? data.userId : 'Unknown';

        if (roomId) {
            socket.join(roomId);
            console.log(`👤 소켓 ${socket.id} -> 방 ${roomId} 입장 (User: ${userId})`);
        }
    });

    // 2. 방 퇴장
    socket.on('leaveRoom', (data) => {
        const roomId = (typeof data === 'object') ? data.roomId : data;
        if (roomId) {
            socket.leave(roomId);
            console.log(`👋 소켓 ${socket.id} -> 방 ${roomId} 퇴장`);
        }
    });

    // 3. 채팅 메시지 전송
    socket.on('chatMessage', (data) => {
        if (data.roomId) {
            // 나를 제외한 방 안의 사람들에게 전송
            socket.to(data.roomId).emit('chatMessage', data);
        }
    });

    // ⭐ 4. [신규] 실시간 위치 공유 로직
    socket.on('sendLocation', (data) => {
        // data 구조: { meetingId, latitude, longitude, userId }
        const { meetingId, latitude, longitude, userId } = data;

        // 로그 확인 (필요시 주석 해제)
        // console.log(`📍 위치 수신: ${userId} (${latitude}, ${longitude}) in ${meetingId}`);

        // 나를 제외한 방 안의 모든 사람에게 위치 전송
        socket.to(meetingId).emit('updateLocation', {
            userId: userId,
            latitude: latitude,
            longitude: longitude
        });
    });

    socket.on('disconnect', () => {
        console.log('🔴 소켓 연결 해제:', socket.id);
    });
});

// ==========================================
// 🛣️ 6. 라우터 등록
// ==========================================
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/users', require('./routes/user.routes'));
app.use('/api/schedules', require('./routes/schedule.routes'));
app.use('/api/meetings', require('./routes/meeting.routes'));
app.use('/api/chats', require('./routes/chat.routes'));
app.use('/api/notifications', require('./routes/notification.routes'));
app.use('/api/invite', require('./routes/invite.routes'));


// ==========================================
// 🛠️ 7. [통합됨] 시연용 데이터 초기화 API
// 요청: POST /api/init-demo
// ==========================================
app.post('/api/init-demo', async (req, res) => {
    try {
        // 1. 기존 데이터 삭제 (User, Meeting)
        await User.deleteMany({});
        await Meeting.deleteMany({});

        // 2. 사용자 생성
        const userMe = await User.create({
            name: "throw", email: "throw@11.11", password: "pass", phone: "12312312312",
            location: { type: 'Point', coordinates: [126.8912, 37.5089], name: "신도림역" } // 시작 위치
        });
        const user1 = await User.create({
            name: "테스트1", email: "test1@11.11", password: "pass", phone: "0101010101",
            location: { type: 'Point', coordinates: [126.9707, 37.5547], name: "서울역" }
        });
        const user2 = await User.create({
            name: "테스트2", email: "test2@11.11", password: "pass", phone: "4818484",
            location: { type: 'Point', coordinates: [126.7898, 37.3265], name: "안산역" }
        });

        // 3. 구일역 모임 생성 (Meeting 모델 사용)
        const newMeeting = await Meeting.create({
            title: "기능 시연용 모임 (구일역)",
            location: "구일역 1호선",
            destination: {
                type: 'Point',
                coordinates: [126.8709, 37.4967],
                name: "구일역 1호선"
            },
            date_time: new Date("2025-12-13T22:00:00.000+09:00"),
            creator_id: userMe._id,
            participants: [
                { user_id: userMe._id, role: 'host', status: 'attended', isSharing: false },
                { user_id: user1._id, role: 'guest', status: 'attended', isSharing: true }, // 시연용: 미리 켜둠
                { user_id: user2._id, role: 'guest', status: 'attended', isSharing: true }  // 시연용: 미리 켜둠
            ]
        });

        res.json({ message: "✅ 시연 데이터 리셋 완료!", meetingId: newMeeting._id });
    } catch (e) {
        console.error(e);
        res.status(500).json({ error: "초기화 실패" });
    }
});

// 8. 서버 시작
const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 메인 서버가 실행 중입니다: http://0.0.0.0:${PORT}`);
});