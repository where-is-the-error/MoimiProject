const express = require('express');
const router = express.Router();

// ⚠️ 경로 수정됨 (../)
const User = require('../models/User');
const authenticateToken = require('../middleware/auth');
const upload = require('../config/uploadConfig');

// 1. 프로필 조회
router.get('/:userId', authenticateToken, async (req, res) => {
    if (req.params.userId !== req.user.userId) return res.status(403).json({ success: false, message: "권한 없음" });
    
    const user = await User.findById(req.params.userId).select('-password'); 
    res.json({ success: true, user });
});

// 2. 프로필 수정
router.put('/:userId', authenticateToken, async (req, res) => {
    if (req.params.userId !== req.user.userId) return res.status(403).json({ success: false, message: "권한 없음" });

    await User.findByIdAndUpdate(req.params.userId, { name: req.body.name });
    res.json({ success: true, message: "수정 완료" });
});

// 3. 이미지 업로드
router.post('/:userId/profile-img', authenticateToken, upload.single('profileImage'), async (req, res) => {
    if (req.params.userId !== req.user.userId) return res.status(403).json({ success: false, message: "권한 없음" });
    if (!req.file) return res.status(400).json({ success: false, message: "파일 없음" });

    const url = req.file.location;
    await User.findByIdAndUpdate(req.params.userId, { profile_img: url });
    res.json({ success: true, profileImgUrl: url });
});

// 4. FCM 토큰 업데이트
router.post('/fcm-token', authenticateToken, async (req, res) => {
    await User.findByIdAndUpdate(req.user.userId, { fcm_token: req.body.fcmToken });
    res.json({ success: true });
});

// 5. 위치 업데이트
router.post('/locations', authenticateToken, async (req, res) => {
    const { latitude, longitude } = req.body;
    
    await User.findByIdAndUpdate(req.user.userId, {
        location: {
            latitude,
            longitude,
            updated_at: new Date()
        }
    });
    res.json({ success: true });
});

module.exports = router;