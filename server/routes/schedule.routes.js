const express = require('express');
const router = express.Router();
const Schedule = require('../models/Schedule');
const authenticateToken = require('../middleware/auth');

// 1. 일정 추가
router.post('/', authenticateToken, async (req, res) => {
    const { date, time, title, location, type, inviteUserIds } = req.body;
    if (!date || !time || !title) return res.status(400).json({ success: false, message: "필수 정보 누락" });

    try {
        let participants = [req.user.userId];
        if (inviteUserIds && Array.isArray(inviteUserIds)) {
            participants = participants.concat(inviteUserIds);
        }
        const newSchedule = await Schedule.create({
            creator_id: req.user.userId,
            date, time, title, location: location || "", type: type || 'MEETING', participants
        });
        res.json({ success: true, message: "저장 성공", scheduleId: newSchedule._id, inviteCode: newSchedule.inviteCode });
    } catch (err) { res.status(500).json({ success: false, message: "서버 오류" }); }
});

// 2. 일정 목록 조회
router.get('/', authenticateToken, async (req, res) => {
    const { date } = req.query;
    
    // [수정] 쿼리 조건 동적 생성
    const query = { participants: req.user.userId };

    if (date) {
        query.date = date; // 날짜가 지정되면 해당 날짜만 조회
    } else {
        // 날짜가 없으면 "오늘 이후"의 모든 일정 조회 (가장 가까운 일정 찾기용)
        // YYYY-MM-DD 문자열 비교를 위해 한국 시간 기준 오늘 날짜 생성
        const now = new Date();
        const offset = 1000 * 60 * 60 * 9; // KST UTC+9
        const koreaNow = new Date(now.getTime() + offset);
        const todayStr = koreaNow.toISOString().split('T')[0];
        
        query.date = { $gte: todayStr }; 
    }

    try {
        // [수정] 날짜(date)와 시간(time) 순으로 정렬하여 조회
        const schedules = await Schedule.find(query)
            .populate('creator_id', 'name')
            .populate('participants', 'name')
            .sort({ date: 1, time: 1 }); // 오름차순 정렬
        
        const formatted = schedules.map(s => ({
            id: s._id, 
            time: s.time, 
            date: s.date, 
            title: s.title, 
            location: s.location,
            inviteCode: s.inviteCode, 
            leaderName: s.creator_id ? s.creator_id.name : "알 수 없음", 
            leaderId: s.creator_id ? s.creator_id._id : "",
            isLeader: s.creator_id ? s.creator_id._id.toString() === req.user.userId : false,
            memberNames: s.participants.map(p => p.name),
            members: s.participants.map(p => ({ id: p._id, name: p.name })),
            type: s.type // [중요] 일정 타입(MEETING, CHECKLIST) 포함
        }));
        res.json({ success: true, schedules: formatted });
    } catch (err) { 
        console.error(err);
        res.status(500).json({ success: false, message: "오류" }); 
    }
});

// 3. 단건 조회
router.get('/:scheduleId', authenticateToken, async (req, res) => {
    try {
        const s = await Schedule.findById(req.params.scheduleId).populate('creator_id', 'name').populate('participants', 'name');
        if (!s) return res.status(404).json({ success: false });
        
        const item = {
            id: s._id, time: s.time, date: s.date, title: s.title, location: s.location,
            inviteCode: s.inviteCode, leaderName: s.creator_id.name, leaderId: s.creator_id._id,
            isLeader: s.creator_id._id.toString() === req.user.userId,
            memberNames: s.participants.map(p => p.name),
            members: s.participants.map(p => ({ id: p._id, name: p.name }))
        };
        res.json({ success: true, schedule: item });
    } catch (err) { res.status(500).json({ success: false }); }
});

// 4. 일정 참여 (ID)
router.post('/:scheduleId/join', authenticateToken, async (req, res) => {
    try {
        const s = await Schedule.findById(req.params.scheduleId);
        if (!s) return res.status(404).json({ success: false, message: "일정 없음" });
        if (s.participants.includes(req.user.userId)) return res.json({ success: true, message: "이미 참여 중" });
        
        s.participants.push(req.user.userId);
        await s.save();
        res.json({ success: true, message: "참여 완료", scheduleId: s._id });
    } catch (e) { res.status(500).json({ success: false }); }
});

// 5. 일정 참여 (코드)
router.post('/join/code', authenticateToken, async (req, res) => {
    const { inviteCode } = req.body;
    try {
        const s = await Schedule.findOne({ inviteCode });
        if (!s) return res.status(404).json({ success: false, message: "코드 오류" });
        if (s.participants.includes(req.user.userId)) return res.json({ success: true, message: "이미 참여 중" });
        
        s.participants.push(req.user.userId);
        await s.save();
        res.json({ success: true, message: `"${s.title}" 참여 완료` });
    } catch (e) { res.status(500).json({ success: false }); }
});

// ⭐ [신규] 6. 일정 수정
router.put('/:scheduleId', authenticateToken, async (req, res) => {
    const { date, time, title, location } = req.body;
    try {
        const s = await Schedule.findById(req.params.scheduleId);
        if (!s) return res.status(404).json({ success: false, message: "일정 없음" });
        if (s.creator_id.toString() !== req.user.userId) return res.status(403).json({ success: false, message: "권한 없음" });

        s.date = date || s.date;
        s.time = time || s.time;
        s.title = title || s.title;
        s.location = location || s.location;
        await s.save();
        res.json({ success: true, message: "수정 완료" });
    } catch (e) { res.status(500).json({ success: false }); }
});

// ⭐ [신규] 7. 일정 삭제
router.delete('/:scheduleId', authenticateToken, async (req, res) => {
    try {
        const s = await Schedule.findById(req.params.scheduleId);
        if (!s) return res.status(404).json({ success: false });
        if (s.creator_id.toString() !== req.user.userId) return res.status(403).json({ success: false, message: "권한 없음" });

        await Schedule.deleteOne({ _id: req.params.scheduleId });
        res.json({ success: true, message: "삭제 완료" });
    } catch (e) { res.status(500).json({ success: false }); }
});

// ⭐ [신규] 8. 멤버 강퇴
router.delete('/:scheduleId/members/:userId', authenticateToken, async (req, res) => {
    try {
        const s = await Schedule.findById(req.params.scheduleId);
        if (!s) return res.status(404).json({ success: false });
        if (s.creator_id.toString() !== req.user.userId) return res.status(403).json({ success: false, message: "권한 없음" });

        s.participants = s.participants.filter(id => id.toString() !== req.params.userId);
        await s.save();
        res.json({ success: true, message: "멤버 제외 완료" });
    } catch (e) { res.status(500).json({ success: false }); }
});

module.exports = router;