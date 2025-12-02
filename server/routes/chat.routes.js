// routes/chat.routes.js
const express = require('express');
const router = express.Router();
const Message = require('../models/Message');
const auth = require('../middleware/auth');
const Meeting = require('../models/Meeting');

// 1. 채팅 기록 가져오기 (기존 코드 유지)
router.get('/:meetingId', auth, async (req, res) => {
    try {
        const { meetingId } = req.params;
        const messages = await Message.find({ meetingId })
            .select('senderName content timestamp')
            .sort({ timestamp: 1 });
        res.json({ success: true, chats: messages }); // 안드로이드 형식에 맞춤
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: '기록 조회 실패' });
    }
});

// 2. [신규] 메시지 전송 및 소켓 브로드캐스팅
router.post('/send', auth, async (req, res) => {
    try {
        const { roomId, message } = req.body;
        const userId = req.user.userId;
        
        // 사용자 이름 찾기 (User 모델 필요)
        const User = require('../models/User');
        const user = await User.findById(userId);
        const senderName = user ? user.name : "알 수 없음";

        // 1) DB 저장
        const newMessage = await Message.create({
            meetingId: roomId,
            senderId: userId,
            senderName: senderName,
            content: message
        });

        // 2) Socket.IO로 같은 방 사람들에게 실시간 전송
        const io = req.app.get('io'); // app.set('io', io)로 저장한 객체 가져오기
        io.to(roomId).emit('chatMessage', {
            message: newMessage.content,
            sender: { name: newMessage.senderName },
            createdAt: newMessage.timestamp
        });

        res.json({ success: true, chat: newMessage });

    } catch (error) {
        console.error("메시지 전송 실패:", error);
        res.status(500).json({ success: false, message: "전송 실패" });
    }
});

router.post('/private', auth, async (req, res) => {
    const { targetEmail } = req.body;
    const myId = req.user.userId;

    try {
        // 1. 상대방 찾기
        const targetUser = await User.findOne({ email: targetEmail });
        if (!targetUser) {
            return res.status(404).json({ success: false, message: "해당 이메일의 사용자를 찾을 수 없습니다." });
        }

        if (targetUser._id.toString() === myId) {
            return res.status(400).json({ success: false, message: "자기 자신과는 대화할 수 없습니다." });
        }

        // 2. 이미 존재하는 1:1 방이 있는지 확인 (선택 사항 - 여기서는 항상 새로 생성으로 구현)
        
        // 3. 채팅방(Meeting) 생성
        // 1:1 채팅은 제목을 상대방 이름으로 하거나 별도 구분
        const newChatRoom = await Meeting.create({
            title: `${targetUser.name}님과의 대화`,
            location: "Online", // 온라인 채팅
            date_time: new Date(), // 생성 시간
            creator_id: myId,
            participants: [
                { user_id: myId, role: 'host', status: 'attended' },
                { user_id: targetUser._id, role: 'guest', status: 'attended' }
            ]
        });

        res.json({ success: true, message: "채팅방이 생성되었습니다.", roomId: newChatRoom._id, title: newChatRoom.title });

    } catch (err) {
        console.error("채팅방 생성 실패:", err);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// ⭐ [신규] 내 채팅방 목록 가져오기
router.get('/rooms/my', auth, async (req, res) => {
    try {
        // 내가 참여중인 모든 방 조회
        const rooms = await Meeting.find({ 'participants.user_id': req.user.userId })
            .sort({ created_at: -1 }); // 최신순

        const chatRooms = rooms.map(room => ({
            id: room._id,
            title: room.title,
            lastMessage: "대화를 시작보세요!" // (심화: 실제 마지막 메시지 조회 로직 필요)
        }));

        res.json({ success: true, rooms: chatRooms });
    } catch (err) {
        res.status(500).json({ success: false, message: "목록 조회 실패" });
    }
});


module.exports = router;