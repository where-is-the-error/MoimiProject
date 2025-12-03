const express = require('express');
const router = express.Router();
const User = require('../models/User');
const Notification = require('../models/Notification');
const authenticateToken = require('../middleware/auth');
const admin = require('../config/firebaseConfig');

// 1. 알림 생성 (기존 유지)
router.post('/', authenticateToken, async (req, res) => {
    const { targetUserId, title, message, type, metadata } = req.body;
    try {
        const user = await User.findById(targetUserId);
        if (!user || !user.fcm_token) {
            return res.json({ success: false, message: "토큰 없음" }); 
        }
        await admin.messaging().send({
            token: user.fcm_token,
            notification: { title: title || "알림", body: message }
        });
        await Notification.create({
            user_id: targetUserId,
            message,
            type: type || 'NORMAL',
            metadata: metadata || {}
        });
        res.json({ success: true });
    } catch (e) {
        console.error(e);
        res.status(500).json({ success: false, error: e.message });
    }
});

// 2. 알림 목록 조회 (기존 유지)
router.get('/', authenticateToken, async (req, res) => {
    try {
        const notifications = await Notification.find({ user_id: req.user.userId })
            .sort({ created_at: -1 }); 
        res.json({ success: true, notifications });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

// ✅ [신규] 3. 특정 알림 읽음 처리
router.put('/:id/read', authenticateToken, async (req, res) => {
    try {
        await Notification.findByIdAndUpdate(req.params.id, { is_read: true });
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ success: false });
    }
});

// ✅ [신규] 4. 특정 알림 삭제 (X 버튼)
router.delete('/:id', authenticateToken, async (req, res) => {
    try {
        await Notification.findByIdAndDelete(req.params.id);
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ success: false });
    }
});

// ✅ [신규] 5. 읽은 알림 전체 삭제
router.delete('/read/all', authenticateToken, async (req, res) => {
    try {
        // 내 알림 중 is_read가 true인 것만 삭제
        await Notification.deleteMany({ user_id: req.user.userId, is_read: true });
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ success: false });
    }
});

module.exports = router;