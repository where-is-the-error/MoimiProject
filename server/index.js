// index.js (또는 app.js)
const http = require('http');
const { Server } = require('socket.io');
const app = require('./app'); // 기존 express app 불러오기

const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// Socket.IO 전역 객체로 설정 (라우터에서 사용하기 위함)
app.set('io', io);

io.on('connection', (socket) => {
    console.log('새로운 소켓 연결:', socket.id);

    // 1. 채팅방 입장
    socket.on('joinRoom', (roomId) => {
        socket.join(roomId);
        console.log(`소켓 ${socket.id}가 방 ${roomId}에 입장함`);
    });

    // 2. 채팅방 퇴장
    socket.on('leaveRoom', (roomId) => {
        socket.leave(roomId);
        console.log(`소켓 ${socket.id}가 방 ${roomId}에서 퇴장함`);
    });

    socket.on('disconnect', () => {
        console.log('소켓 연결 해제:', socket.id);
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});