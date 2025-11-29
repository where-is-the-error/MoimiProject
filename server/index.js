require('dotenv').config();
const express = require('express');
const cors = require('cors');
// config 폴더로 경로 변경
const connectDB = require('./config/db'); 

// routes 폴더로 경로 변경
const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const meetingRoutes = require('./routes/meeting.routes');
const inviteRoutes = require('./routes/invite.routes');
const notificationRoutes = require('./routes/notification.routes');

const app = express();
const PORT = process.env.PORT || 3000;

connectDB();

app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
    const now = new Date().toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' });
    console.log(`[${now}] ${req.method} ${req.originalUrl}`);
    next();
});

app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/meetings', meetingRoutes);
app.use('/api/invites', inviteRoutes);
app.use('/api/notifications', notificationRoutes);

// require('dotenv').config();
// ... (기존 require 및 use 코드) ...

app.listen(PORT, '0.0.0.0', () => { // 👈 여기에 '0.0.0.0'을 추가해야 합니다!
    console.log(`🚀 모이미 서버 실행 중: http://localhost:${PORT}`);
    console.log(`(외부 접속 주소: 0.0.0.0:${PORT})`); // 확인용 로그 추가
});