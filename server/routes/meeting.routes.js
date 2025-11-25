const express = require('express');
const router = express.Router();

// ⚠️ 경로 수정됨 (../)
const Meeting = require('../models/Meeting');
const User = require('../models/User');
const authenticateToken = require('../middleware/auth');

// 1. 약속 생성
router.post('/', authenticateToken, async (req, res) => {
    const { title, location, dateTime, reservationRequired } = req.body;
    const creatorId = req.user.userId;

    try {
        const meeting = await Meeting.create({
            title,
            location,
            date_time: dateTime,
            creator_id: creatorId,
            reservation_required: reservationRequired,
            participants: [{ user_id: creatorId, role: 'host' }]
        });

        res.status(201).json({ success: true, meetingId: meeting._id });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false });
    }
});

// 2. 목록 조회
router.get('/', authenticateToken, async (req, res) => {
    try {
        const meetings = await Meeting.find({ 'participants.user_id': req.user.userId })
            .sort({ date_time: 1 });
            
        res.json({ success: true, meetings });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

// 3. 상세 조회
router.get('/:meetingId', authenticateToken, async (req, res) => {
    try {
        const meeting = await Meeting.findById(req.params.meetingId)
            .populate('participants.user_id', 'name profile_img');

        if (!meeting) return res.status(404).json({ success: false });

        res.json({ success: true, meeting });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

// 4. 참여자 추가
router.post('/:meetingId/participants', authenticateToken, async (req, res) => {
    const { userId: guestId } = req.body;
    
    try {
        await Meeting.findByIdAndUpdate(req.params.meetingId, {
            $push: { participants: { user_id: guestId, role: 'guest' } }
        });
        res.status(201).json({ success: true });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

// 5. 출석 체크 & 신뢰 점수
router.post('/:meetingId/attendance', authenticateToken, async (req, res) => {
    const { targetUserId, status } = req.body;
    const score = status === 'attended' ? 10 : -5;

    try {
        await Meeting.updateOne(
            { _id: req.params.meetingId, 'participants.user_id': targetUserId },
            { $set: { 'participants.$.status': status } }
        );

        await User.findByIdAndUpdate(targetUserId, { $inc: { trust_score: score } });

        res.json({ success: true, message: "점수 반영 완료" });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

// 6. 참여자 위치 조회
router.get('/:meetingId/locations', authenticateToken, async (req, res) => {
    try {
        const meeting = await Meeting.findById(req.params.meetingId);
        const participantIds = meeting.participants.map(p => p.user_id);

        const locations = await User.find(
            { _id: { $in: participantIds } },
            'name location'
        );

        res.json({ success: true, locations });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

module.exports = router;