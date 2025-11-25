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

app.listen(PORT, () => {
    console.log(`🚀 모이미 서버 실행 중: http://localhost:${PORT}`);
});