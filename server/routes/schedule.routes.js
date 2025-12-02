const express = require('express');
const router = express.Router();
const Schedule = require('../models/Schedule');
const authenticateToken = require('../middleware/auth'); 

// 1. 일정 추가
router.post('/', authenticateToken, async (req, res) => {
    const { date, time, title, location, type } = req.body;

    if (!date || !time || !title) {
        return res.status(400).json({ success: false, message: "필수 정보 누락" });
    }

    try {
        // 생성 시 inviteCode는 스키마 default 함수에 의해 자동 생성됨
        const newSchedule = await Schedule.create({
            creator_id: req.user.userId,
            date, time, title,
            location: location || "",
            type: type || 'MEETING',
            participants: [req.user.userId]
        });

        res.json({ 
            success: true, 
            message: "저장 성공", 
            scheduleId: newSchedule._id,
            inviteCode: newSchedule.inviteCode // 클라에 코드 전달
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// 2. 일정 조회
router.get('/', authenticateToken, async (req, res) => {
    const { date } = req.query;
    try {
        const schedules = await Schedule.find({
            date: date,
            participants: req.user.userId
        })
        .populate('creator_id', 'name')
        .populate('participants', 'name');

        const formattedSchedules = schedules.map(s => ({
            id: s._id,
            time: s.time,
            title: s.title,
            location: s.location,
            inviteCode: s.inviteCode, // ⭐ 코드 전달
            leaderName: s.creator_id.name,
            leaderId: s.creator_id._id,
            isLeader: s.creator_id._id.toString() === req.user.userId,
            memberNames: s.participants.map(p => p.name)
        }));

        res.json({ success: true, schedules: formattedSchedules });
    } catch (err) {
        res.status(500).json({ success: false, message: "오류" });
    }
});

// ⭐ [신규] 3. 초대 코드로 참여하기
router.post('/join/code', authenticateToken, async (req, res) => {
    const { inviteCode } = req.body;

    if (!inviteCode) return res.status(400).json({ success: false, message: "코드를 입력해주세요." });

    try {
        const schedule = await Schedule.findOne({ inviteCode });
        if (!schedule) {
            return res.status(404).json({ success: false, message: "유효하지 않은 코드입니다." });
        }

        // 이미 참여 중인지 확인
        if (schedule.participants.includes(req.user.userId)) {
            return res.json({ success: true, message: "이미 참여 중인 일정입니다." });
        }

        // 참여자 추가
        schedule.participants.push(req.user.userId);
        await schedule.save();

        res.json({ success: true, message: `"${schedule.title}" 일정에 참여했습니다!` });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

module.exports = router;