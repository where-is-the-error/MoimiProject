const express = require('express');
const router = express.Router();
const Schedule = require('../models/Schedule');
const authenticateToken = require('../middleware/auth'); // 토큰 인증

// 1. 일정 추가 (POST /api/schedules)
router.post('/', authenticateToken, async (req, res) => {
    console.log("[일정 추가 요청]", req.body);

    const { date, time, title, location, type, inviteUserIds } = req.body;

    // 필수 값 체크
    if (!date || !time || !title) {
        return res.status(400).json({ success: false, message: "필수 정보가 누락되었습니다." });
    }

    try {
        // 참여자 리스트 만들기 (나 + 초대된 사람들)
        let participants = [req.user.userId];
        if (inviteUserIds && Array.isArray(inviteUserIds)) {
            participants = participants.concat(inviteUserIds);
        }

        // DB에 저장
        await Schedule.create({
            creator_id: req.user.userId,
            date,
            time,
            title,
            location: location || "", // 장소는 없을 수도 있음
            type: type || 'MEETING',
            participants
        });

        res.json({ success: true, message: "저장 성공" });
    } catch (err) {
        console.error("일정 저장 에러:", err);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// 2. 일정 조회 (GET /api/schedules)
router.get('/', authenticateToken, async (req, res) => {
    const { date } = req.query; // ?date=2025-09-22

    try {
        // 내가 참여자로 포함되어 있고, 해당 날짜인 일정 검색
        const schedules = await Schedule.find({
            date: date,
            participants: req.user.userId
        })
        .populate('creator_id', 'name') // 작성자 이름 가져오기
        .populate('participants', 'name'); // 참여자 이름 가져오기

        // 안드로이드 앱에서 쓰는 이름(ScheduleItem)에 맞춰서 변환
        const formattedSchedules = schedules.map(s => ({
            id: s._id,
            time: s.time,
            title: s.title,
            location: s.location,
            type: s.type, // 유형(MEETING/CHECKLIST) 전달
            leaderName: s.creator_id.name,
            leaderId: s.creator_id._id,
            // 나 자신이 리더(작성자)인지 확인
            isLeader: s.creator_id._id.toString() === req.user.userId,
            memberNames: s.participants.map(p => p.name)
        }));

        res.json({ success: true, schedules: formattedSchedules });
    } catch (err) {
        console.error("일정 조회 에러:", err);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

module.exports = router;