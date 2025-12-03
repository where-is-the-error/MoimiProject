const express = require('express');
const router = express.Router();
const Message = require('../models/Message');
const Meeting = require('../models/Meeting');
const User = require('../models/User');
const auth = require('../middleware/auth');

router.get('/:meetingId', auth, async (req, res) => {
    try {
        const { meetingId } = req.params;
        // ✅ senderId를 populate하여 name과 profile_img를 가져옴
        const messages = await Message.find({ meetingId })
            .populate('senderId', 'name profile_img') 
            .sort({ timestamp: 1 });

        // 클라이언트에 보낼 형식으로 매핑
        const chats = messages.map(m => ({
            content: m.content,
            timestamp: m.timestamp,
            senderName: m.senderId ? m.senderId.name : m.senderName,
            senderProfileImg: m.senderId ? m.senderId.profile_img : null // ✅ 추가
        }));

        res.json({ success: true, chats });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: '조회 실패' });
    }
});

// 2. 메시지 전송 (수정됨)
router.post('/send', auth, async (req, res) => {
    try {
        const { roomId, message } = req.body;
        const userId = req.user.userId;
        
        const user = await User.findById(userId);
        const senderName = user ? user.name : "알 수 없음";
        const senderProfileImg = user ? user.profile_img : null; // ✅ 추가

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
                sender: { 
                    name: newMessage.senderName,
                    profileImg: senderProfileImg // ✅ 소켓으로 프사 전송
                },
                createdAt: newMessage.timestamp
            });
        }
        
        await Meeting.updateOne(
            { _id: roomId, 'participants.user_id': userId },
            { $set: { 'participants.$.lastReadAt': new Date() } }
        );

        res.json({ success: true, chat: newMessage });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "전송 실패" });
    }
});

// 3. 1:1 채팅방 만들기
router.post('/private', auth, async (req, res) => {
    const { targetEmail } = req.body;
    const myId = req.user.userId;

    if (!targetEmail) return res.status(400).json({ success: false, message: "이메일 입력 필요" });

    try {
        const targetUser = await User.findOne({ email: targetEmail });
        if (!targetUser) return res.status(404).json({ success: false, message: "사용자 없음" });
        if (targetUser._id.toString() === myId) return res.status(400).json({ success: false, message: "자신과 대화 불가" });

        // 채팅방 생성 (1:1)
        const newChatRoom = await Meeting.create({
            title: `${targetUser.name}님과의 대화`,
            location: "Online",
            date_time: new Date(),
            creator_id: myId,
            participants: [
                { user_id: myId, role: 'host', status: 'attended', lastReadAt: new Date() },
                { user_id: targetUser._id, role: 'guest', status: 'attended', lastReadAt: new Date() }
            ]
        });

        res.json({ success: true, message: "채팅방 생성", roomId: newChatRoom._id, title: newChatRoom.title });
    } catch (err) {
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// ✅ [신규] 4. 채팅방 읽음 처리 (채팅방 입장 시 호출)
router.put('/read/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const userId = req.user.userId;

        // 내 참여 정보의 lastReadAt을 현재 시간으로 업데이트
        await Meeting.updateOne(
            { _id: roomId, 'participants.user_id': userId },
            { $set: { 'participants.$.lastReadAt': new Date() } }
        );

        res.json({ success: true });
    } catch (e) {
        console.error(e);
        res.status(500).json({ success: false });
    }
});

// ⭐ [대폭 수정] 5. 내 채팅방 목록 (프사, 안읽음 배지, 시간 포함)
router.get('/rooms/my', auth, async (req, res) => {
    try {
        // 내가 참여한 방 조회 (참여자 정보 + 프사 포함)
        const rooms = await Meeting.find({ 'participants.user_id': req.user.userId })
            .populate('participants.user_id', 'name profile_img') // ✅ 프사 가져오기
            .sort({ created_at: -1 });

        const chatRooms = await Promise.all(rooms.map(async (room) => {
            const lastMsg = await Message.findOne({ meetingId: room._id }).sort({ timestamp: -1 });
            
            // 내 정보 찾기 (마지막 읽은 시간 확인용)
            const myInfo = room.participants.find(p => p.user_id && p.user_id._id.toString() === req.user.userId);
            const lastReadAt = myInfo ? new Date(myInfo.lastReadAt) : new Date(0); 

            let displayTitle = room.title;
            let meetingInfo = null;
            let hasUnread = false;
            let lastMessageTime = null;
            let profileImg = null; // ✅ 프사 변수

            const isPrivate = room.location === "Online";

            if (isPrivate) {
                // [1:1] 상대방 정보 (이름, 프사) 사용
                const partner = room.participants.find(p => p.user_id && p.user_id._id.toString() !== req.user.userId);
                if (partner && partner.user_id) {
                    displayTitle = partner.user_id.name;
                    profileImg = partner.user_id.profile_img; // ✅ 상대방 프사
                } else {
                    displayTitle = "알 수 없는 사용자";
                }
            } else {
                // [그룹] 참여자 이름 나열
                const names = room.participants
                    .filter(p => p.user_id)
                    .map(p => p.user_id.name)
                    .join(", ");
                
                displayTitle = names || room.title;
                // 그룹 채팅은 프사 null (앱에서 기본 아이콘 처리)

                const date = new Date(room.date_time);
                meetingInfo = `${date.getMonth() + 1}/${date.getDate()} ${room.location}`;
            }

            let lastMessageContent = "대화를 시작해보세요!";
            if (lastMsg) {
                lastMessageContent = lastMsg.content;
                lastMessageTime = lastMsg.timestamp; // ✅ 시간 전달

                // ⭐ [핵심] 메시지 시간이 내가 읽은 시간보다 뒤면 '안 읽음' 처리
                if (new Date(lastMsg.timestamp) > lastReadAt) {
                    hasUnread = true;
                }
            }

            return {
                id: room._id,
                title: displayTitle,
                profileImg: profileImg, // ✅ 안드로이드로 프사 URL 전달
                lastMessage: lastMessageContent,
                meetingInfo: meetingInfo,
                hasUnread: hasUnread,
                lastMessageTime: lastMessageTime // ✅ 안드로이드로 시간 전달
            };
        }));

        res.json({ success: true, rooms: chatRooms });

    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "목록 조회 실패" });
    }
});

module.exports = router;