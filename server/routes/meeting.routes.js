const express = require('express');
const router = express.Router();
// ⚠️ 경로 수정됨 (../)
const Meeting = require('../models/Meeting');
const User = require('../models/User'); // User 모델은 참여자 정보 조회 시 필요
const authenticateToken = require('../middleware/auth'); // JWT 인증 미들웨어

// --- 헬퍼 함수: 필수 필드 누락 검사 ---
const validateMeetingFields = (body) => {
    const { title, location, date_time } = body;
    if (!title || !location || !date_time) {
        return "title, location, date_time 필드는 필수입니다.";
    }
    // Mongoose가 날짜 유효성을 대신 검사하므로, 여기서는 존재 여부만 확인합니다.
    return null;
};

// 1. 약속 생성 (POST /api/meetings)
router.post('/', authenticateToken, async (req, res) => {
    // 💡 디버깅 로그: 서버가 받은 데이터를 터미널에 출력
    console.log("[POST /api/meetings] 요청 Body:", req.body); 
    
    // 1. 유효성 검사 (필수 필드 체크)
    const error = validateMeetingFields(req.body);
    if (error) {
        return res.status(400).json({ success: false, message: error });
    }

    // 2. 데이터 추출 및 변수 이름 통일 (DB 스키마: date_time)
    const { title, location, date_time, reservation_required } = req.body;
    
    // 3. 토큰에서 생성자 ID 추출 (미들웨어의 핵심 역할)
    // authenticateToken 미들웨어에서 req.user 객체에 userId가 담겨 있다고 가정합니다.
    const creatorId = req.user.userId; 
    
    try {
        const meeting = await Meeting.create({
            title,
            location,
            date_time, // Postman에서 보낸 date_time (snake_case) 사용
            creator_id: creatorId, // 토큰에서 추출한 ID 사용 (필수 항목 충족)
            reservation_required: reservation_required || false,
            // 생성자를 host로 참여자 목록에 자동 추가
            participants: [{ user_id: creatorId, role: 'host', status: 'attended' }] 
        });

        res.status(201).json({ success: true, meetingId: meeting._id });
    } catch (err) {
        // Mongoose validation error나 DB 오류를 자세히 출력
        console.error("모임 생성 서버 오류:", err); 
        // 400 대신 500을 반환하여 내부 오류임을 표시
        res.status(500).json({ success: false, message: "모임 생성 중 서버 오류 발생", error: err.message });
    }
});

// 2. 목록 조회 (GET /api/meetings) - 인증 필요
router.get('/', authenticateToken, async (req, res) => {
    try {
        // 사용자가 참여하고 있는 모임만 조회
        const meetings = await Meeting.find({ 'participants.user_id': req.user.userId })
            .sort({ date_time: 1 }); // 다가오는 모임 순으로 정렬
            
        res.json({ success: true, meetings });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

// 3. 상세 조회 (GET /api/meetings/:meetingId) - 인증 필요
router.get('/:meetingId', authenticateToken, async (req, res) => {
    try {
        const meeting = await Meeting.findById(req.params.meetingId)
            // participants.user_id 참조 필드를 User 모델의 'name', 'profile_img' 필드로 채웁니다.
            .populate('participants.user_id', 'name profile_img'); 

        if (!meeting) return res.status(404).json({ success: false, message: "모임을 찾을 수 없습니다." });

        res.json({ success: true, meeting });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: err.message });
    }
});

// 4. 참여자 추가 (POST /api/meetings/:meetingId/participants) - 인증 필요
router.post('/:meetingId/participants', authenticateToken, async (req, res) => {
    // 초대받는 사용자 ID를 Body에서 받습니다.
    const { userId: guestId } = req.body; 
    
    try {
        const updatedMeeting = await Meeting.findByIdAndUpdate(
            req.params.meetingId, 
            {
                $push: { participants: { user_id: guestId, role: 'guest' } }
            },
            { new: true } // 업데이트된 문서를 반환
        );

        if (!updatedMeeting) return res.status(404).json({ success: false, message: "모임을 찾을 수 없습니다." });

        res.status(201).json({ success: true, message: "참여자 추가 완료" });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: err.message });
    }
});

// 5. 출석 체크 & 신뢰 점수 (POST /api/meetings/:meetingId/attendance) - 인증 필요 (호스트 권한 체크 필요)
router.post('/:meetingId/attendance', authenticateToken, async (req, res) => {
    // 현재 로그인한 사용자(req.user.userId)는 호스트라고 가정합니다.
    const { targetUserId, status } = req.body; // 출석 체크 대상 ID와 상태 ('attended' 또는 'absent')
    
    // 점수 계산 (출석 +10, 결석 -5)
    const score = status === 'attended' ? 10 : -5; 

    try {
        // 1. 모임의 참여자 상태 업데이트
        await Meeting.updateOne(
            { _id: req.params.meetingId, 'participants.user_id': targetUserId },
            { $set: { 'participants.$.status': status } }
        );

        // 2. 사용자의 신뢰 점수 업데이트
        await User.findByIdAndUpdate(
            targetUserId, 
            { $inc: { trust_score: score } }
        );

        res.json({ success: true, message: `참여자 상태 및 점수(${score}) 반영 완료` });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: err.message });
    }
});

// 6. 참여자 위치 조회 (GET /api/meetings/:meetingId/locations) - 인증 필요
router.get('/:meetingId/locations', authenticateToken, async (req, res) => {
    try {
        const meeting = await Meeting.findById(req.params.meetingId);
        
        if (!meeting) return res.status(404).json({ success: false, message: "모임을 찾을 수 없습니다." });

        // 참여자 ID 목록 추출
        const participantIds = meeting.participants.map(p => p.user_id);

        // 해당 참여자들의 최신 위치 정보와 이름만 조회
        const locations = await User.find(
            { _id: { $in: participantIds } },
            'name location' // User 모델에서 name과 location 필드만 가져옵니다.
        );

        res.json({ success: true, locations });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: err.message });
    }
});

module.exports = router;