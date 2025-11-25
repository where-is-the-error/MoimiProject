const express = require('express');
const router = express.Router();

// ⚠️ 경로 수정됨 (../)
const User = require('../models/User');
const Notification = require('../models/Notification');
const authenticateToken = require('../middleware/auth');
const admin = require('../config/firebaseConfig');

router.post('/', authenticateToken, async (req, res) => {
    const { targetUserId, title, message } = req.body;
    
    try {
        const user = await User.findById(targetUserId);
        
        if (!user || !user.fcm_token) {
            console.log("FCM 전송 실패: 토큰 없음");
            return res.json({ success: false, message: "토큰 없음" }); 
        }
        
        // Firebase 전송
        await admin.messaging().send({
            token: user.fcm_token,
            notification: { title: title || "알림", body: message }
        });
        
        // DB 저장
        await Notification.create({
            user_id: targetUserId,
            message
        });

        res.json({ success: true });
    } catch (e) {
        console.error(e);
        res.status(500).json({ success: false, error: e.message });
    }
});

module.exports = router;