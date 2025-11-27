const express = require('express');
const router = express.Router();
const Message = require('../models/Message');
const auth = require('../middleware/auth'); // 사용자 인증 미들웨어 (토큰 검증)

// 특정 모임의 채팅 기록 가져오기
// 엔드포인트: GET /api/chats/:meetingId
router.get('/:meetingId', auth, async (req, res) => {
    try {
        const { meetingId } = req.params;
        
        // 해당 모임의 메시지를 시간순(timestamp: 1)으로 조회
        const messages = await Message.find({ meetingId })
            .select('senderName content timestamp') // 필요한 필드만 선택
            .sort({ timestamp: 1 }); 

        res.json(messages);
    } catch (error) {
        console.error("채팅 기록 조회 실패:", error);
        // 클라이언트에게 오류 메시지 전달
        res.status(500).json({ message: '채팅 기록을 불러오는데 실패했습니다.' });
    }
});

module.exports = router;