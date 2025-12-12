const express = require('express');
const router = express.Router();
const Message = require('../models/Message');
const Meeting = require('../models/Meeting');
const User = require('../models/User');
const auth = require('../middleware/auth');
const admin = require('../config/firebaseConfig'); // FCM 발송용

// 1. 대화 기록 조회
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

// 3. 1:1 채팅방 만들기 (참여자 상태 pending 및 알림 발송 로직 추가)
router.post('/private', auth, async (req, res) => {
    const { targetEmail } = req.body;
    const myId = req.user.userId;

    if (!targetEmail) return res.status(400).json({ success: false, message: "이메일 입력 필요" });

    try {
        const targetUser = await User.findOne({ email: targetEmail });
        if (!targetUser) return res.status(404).json({ success: false, message: "사용자 없음" });
        if (targetUser._id.toString() === myId) return res.status(400).json({ success: false, message: "자신과 대화 불가" });
        
        const myUser = await User.findById(myId);

        // [중복 확인] 이미 두 사용자 간의 채팅방이 있는지 확인
        const existingRoom = await Meeting.findOne({
            location: "Online", // 1:1 채팅방의 특징
            $and: [
                { 'participants.user_id': myId },
                { 'participants.user_id': targetUser._id }
            ]
        });

        if (existingRoom) {
            // [개선] 이미 있는 방이라도 상대방 상태가 attended라면 메시지 전송 가능
             const partner = existingRoom.participants.find(p => p.user_id.toString() === targetUser._id.toString());
             if (partner && partner.status === 'attended') {
                 return res.json({ 
                    success: true, 
                    message: "이미 채팅방이 존재합니다. 바로 입장하세요.", 
                    roomId: existingRoom._id, 
                    title: existingRoom.title 
                });
             }
        }


        // 채팅방 생성 (1:1)
        const newChatRoom = await Meeting.create({
            title: `${targetUser.name}님과의 대화`,
            location: "Online", // 1:1 채팅방임을 표시
            date_time: new Date(),
            creator_id: myId,
            participants: [
                // 요청자: 즉시 참여 (attended)
                { user_id: myId, role: 'host', status: 'attended', lastReadAt: new Date() },
                // 대상자: 수락 대기 (pending)
                { user_id: targetUser._id, role: 'guest', status: 'pending', lastReadAt: new Date() }
            ]
        });

        // ✅ [신규] 대상자에게 알림 발송 (FCM 및 DB 알림)
        if (targetUser.fcm_token) {
             await admin.messaging().send({
                token: targetUser.fcm_token,
                notification: { 
                    title: "새로운 채팅 요청", 
                    body: `${myUser.name}님과의 대화를 수락해주세요.` 
                }
            });
        }
        await Notification.create({
            user_id: targetUser._id,
            message: `${myUser.name}님과의 대화를 시작하시겠습니까?`,
            type: 'CHAT_REQUEST',
            metadata: { 
                roomId: newChatRoom._id.toString(), 
                requesterId: myId,
                requesterName: myUser.name
            }
        });


        res.json({ 
            success: true, 
            message: "대화 요청을 보냈습니다.", 
            roomId: newChatRoom._id, 
            title: newChatRoom.title 
        });

    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// 4. 채팅방 읽음 처리 (채팅방 입장 시 호출)
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

// ⭐ [대폭 수정] 5. 내 채팅방 목록 (프사, 안읽음 배지, 시간 포함 + pending 상태 처리)
router.get('/rooms/my', auth, async (req, res) => {
    try {
        const myId = req.user.userId;
        // 내가 참여한 방 조회 (참여자 정보 + 프사 포함)
        const rooms = await Meeting.find({ 'participants.user_id': myId })
            .populate('participants.user_id', 'name profile_img') // ✅ 프사 가져오기
            .sort({ created_at: -1 });

        const chatRooms = await Promise.all(rooms.map(async (room) => {
            const lastMsg = await Message.findOne({ meetingId: room._id }).sort({ timestamp: -1 });
            
            // 내 정보 찾기 (마지막 읽은 시간, 현재 상태 확인용)
            const myInfo = room.participants.find(p => p.user_id && p.user_id._id.toString() === myId);
            const lastReadAt = myInfo ? new Date(myInfo.lastReadAt) : new Date(0); 
            const myStatus = myInfo ? myInfo.status : 'attended'; // 내 상태

            let displayTitle = room.title;
            let meetingInfo = null;
            let hasUnread = false;
            let lastMessageTime = null;
            let profileImg = null; 

            const isPrivate = room.location === "Online";
            let lastMessageContent = ""; 
            
            // 상대방 정보 (1:1 채팅일 때 필요)
            const partner = room.participants.find(p => p.user_id && p.user_id._id.toString() !== myId);
            const partnerName = partner && partner.user_id ? partner.user_id.name : "알 수 없는 사용자";
            const partnerStatus = partner ? partner.status : 'attended';
            
            // 1. 상태 메시지 결정
            if (isPrivate) {
                displayTitle = partnerName;
                profileImg = partner && partner.user_id ? partner.user_id.profile_img : null;
                
                if (myStatus === 'pending') {
                    // 내가 받은 요청: "OOO님과의 대화 요청을 수락해주세요!"
                    lastMessageContent = `${partnerName}님과의 대화 요청을 수락해주세요!`;
                } else if (partnerStatus === 'pending') {
                    // 내가 보낸 요청: "OOO님의 수락을 기다리는 중입니다."
                    lastMessageContent = `${partnerName}님의 수락을 기다리는 중입니다.`;
                }
            } else {
                // [그룹] 참여자 이름 나열
                const names = room.participants
                    .filter(p => p.user_id)
                    .map(p => p.user_id.name)
                    .join(", ");
                
                displayTitle = names || room.title;
                
                const date = new Date(room.date_time);
                meetingInfo = `${date.getMonth() + 1}/${date.getDate()} ${room.location}`;
            }
            
            // 2. 실제 메시지 내용 결정 (상태 메시지가 아닐 경우)
            if (myStatus === 'attended' && partnerStatus === 'attended') {
                if (lastMsg) {
                    lastMessageContent = lastMsg.content;
                } else {
                    lastMessageContent = "대화를 시작해보세요!";
                }
            } else if (lastMsg) {
                // 요청/수락 대기 중이라도 마지막 메시지가 있으면 표시 (가장 최근 상태를 반영)
                 lastMessageContent = lastMsg.content;
            } else if (lastMessageContent === "") {
                // 상태 메시지가 결정되지 않은 순수한 빈 방일 경우 기본 메시지
                 lastMessageContent = "대화를 시작해보세요!";
            }


            // 3. 안 읽음 및 시간 결정
            if (lastMsg) {
                lastMessageTime = lastMsg.timestamp; 
                if (new Date(lastMsg.timestamp) > lastReadAt) {
                    hasUnread = true;
                }
            }


            return {
                id: room._id,
                title: displayTitle,
                profileImg: profileImg, 
                lastMessage: lastMessageContent, 
                meetingInfo: meetingInfo,
                hasUnread: hasUnread,
                lastMessageTime: lastMessageTime 
            };
        }));

        res.json({ success: true, rooms: chatRooms });

    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "목록 조회 실패" });
    }
});

module.exports = router;