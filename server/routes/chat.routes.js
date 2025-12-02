const express = require('express');
const router = express.Router();
const Message = require('../models/Message');
const Meeting = require('../models/Meeting');
const User = require('../models/User');
const auth = require('../middleware/auth');

// 1. 채팅 기록 가져오기 (기존 유지)
router.get('/:meetingId', auth, async (req, res) => {
    try {
        const { meetingId } = req.params;
        const messages = await Message.find({ meetingId })
            .select('senderName content timestamp')
            .sort({ timestamp: 1 });
        res.json({ success: true, chats: messages });
    } catch (error) {
        res.status(500).json({ success: false, message: '조회 실패' });
    }
});

// 2. 메시지 전송 (기존 유지)
router.post('/send', auth, async (req, res) => {
    try {
        const { roomId, message } = req.body;
        const userId = req.user.userId;
        
        const user = await User.findById(userId);
        const senderName = user ? user.name : "알 수 없음";

        const newMessage = await Message.create({
            meetingId: roomId,
            senderId: userId,
            senderName: senderName,
            content: message
        });

        const io = req.app.get('io'); 
        if (io) {
            io.to(roomId).emit('chatMessage', {
                message: newMessage.content,
                sender: { name: newMessage.senderName },
                createdAt: newMessage.timestamp
            });
        }
        res.json({ success: true, chat: newMessage });
    } catch (error) {
        res.status(500).json({ success: false, message: "전송 실패" });
    }
});

// 3. 1:1 채팅방 만들기 (기존 유지)
router.post('/private', auth, async (req, res) => {
    const { targetEmail } = req.body;
    const myId = req.user.userId;

    if (!targetEmail) return res.status(400).json({ success: false, message: "이메일 입력 필요" });

    try {
        const targetUser = await User.findOne({ email: targetEmail });
        if (!targetUser) return res.status(404).json({ success: false, message: "사용자 없음" });
        if (targetUser._id.toString() === myId) return res.status(400).json({ success: false, message: "자신과 대화 불가" });

        const newChatRoom = await Meeting.create({
            title: `${targetUser.name}님과의 대화`,
            location: "Online",
            date_time: new Date(),
            creator_id: myId,
            participants: [
                { user_id: myId, role: 'host', status: 'attended' },
                { user_id: targetUser._id, role: 'guest', status: 'attended' }
            ]
        });

        res.json({ success: true, message: "채팅방 생성", roomId: newChatRoom._id, title: newChatRoom.title });
    } catch (err) {
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// ⭐ [대폭 수정] 4. 내 채팅방 목록 (커스텀 제목, 약속 정보, 읽음 상태 추가)
router.get('/rooms/my', auth, async (req, res) => {
    try {
        // 1. 내가 참여한 방 조회 (참여자 정보 포함)
        const rooms = await Meeting.find({ 'participants.user_id': req.user.userId })
            .populate('participants.user_id', 'name') // 참여자 이름 가져오기
            .sort({ created_at: -1 });

        // 2. 각 방의 정보 가공 (비동기 처리를 위해 Promise.all 사용)
        const chatRooms = await Promise.all(rooms.map(async (room) => {
            // 마지막 메시지 조회
            const lastMsg = await Message.findOne({ meetingId: room._id }).sort({ timestamp: -1 });
            
            let displayTitle = room.title;
            let meetingInfo = null; // 1:1 채팅은 약속 정보 없음
            let hasUnread = false;

            const isPrivate = room.location === "Online"; // 1:1 채팅 구분

            if (isPrivate) {
                // [1:1] 상대방 이름 찾기
                const partner = room.participants.find(p => p.user_id && p.user_id._id.toString() !== req.user.userId);
                displayTitle = partner ? partner.user_id.name : "알 수 없는 사용자";
            } else {
                // [모임] 참여자 이름 나열 (나 포함 or 제외 선택 가능. 여기선 전체 나열)
                // 예: "철수, 영희, 짱구"
                const names = room.participants
                    .filter(p => p.user_id) // 탈퇴한 유저 방지
                    .map(p => p.user_id.name)
                    .join(", ");
                
                displayTitle = names || room.title; // 참여자 없으면 원래 제목

                // 약속 정보 포맷팅 (예: "9/22 강남역")
                const date = new Date(room.date_time);
                const dateStr = `${date.getMonth() + 1}/${date.getDate()}`;
                meetingInfo = `${dateStr} ${room.location}`;
            }

            // 마지막 메시지 처리
            let lastMessageContent = "대화를 시작해보세요!";
            if (lastMsg) {
                lastMessageContent = lastMsg.content;
                // 내가 보낸 메시지가 아니면 '읽지 않음'으로 간주 (간단한 로직)
                if (lastMsg.senderId.toString() !== req.user.userId) {
                    hasUnread = true;
                }
            }

            return {
                id: room._id,
                title: displayTitle,
                lastMessage: lastMessageContent,
                meetingInfo: meetingInfo, // 추가된 필드
                hasUnread: hasUnread      // 추가된 필드
            };
        }));

        res.json({ success: true, rooms: chatRooms });

    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "목록 조회 실패" });
    }
});

module.exports = router;