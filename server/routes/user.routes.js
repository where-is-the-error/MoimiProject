// routes/user.routes.js
const express = require('express');
const router = express.Router();
const User = require('../models/User'); // ⚠️ 경로 확인 (../models/User)
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

// ⭐ 5. 위치 업데이트 (치명적 오류 수정됨)
router.post('/locations', authenticateToken, async (req, res) => {
    const { latitude, longitude } = req.body;
    
    // User 모델의 GeoJSON 스키마에 맞춰서 데이터 구조 변경
    try {
        await User.findByIdAndUpdate(req.user.userId, {
            $set: {
                location: {
                    type: 'Point',
                    // ⚠️ MongoDB는 [경도(lng), 위도(lat)] 순서입니다!
                    coordinates: [parseFloat(longitude), parseFloat(latitude)],
                    updated_at: new Date()
                }
            }
        });
        res.json({ success: true });
    } catch (err) {
        console.error("위치 업데이트 실패:", err);
        res.status(500).json({ success: false, message: "위치 저장 실패" });
    }
});

module.exports = router;