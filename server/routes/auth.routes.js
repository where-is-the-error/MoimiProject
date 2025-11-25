const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

// ⚠️ 상위 폴더(..)로 나가서 models와 middleware를 찾습니다.
const User = require('../models/User'); 
const authenticateToken = require('../middleware/auth'); 

// 1. 회원가입
router.post('/register', async (req, res) => {
    try {
        const { name, email, password } = req.body;
        if (!name || !email || !password) return res.status(400).json({ success: false, message: "정보 누락" });

        const existingUser = await User.findOne({ email });
        if (existingUser) return res.status(409).json({ success: false, message: "이미 존재하는 이메일" });

        const hashedPassword = await bcrypt.hash(password, 10);
        
        const user = await User.create({
            name,
            email,
            password: hashedPassword
        });

        res.status(201).json({ success: true, message: "회원가입 완료", userId: user._id });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// 2. 로그인
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        
        const user = await User.findOne({ email });
        if (!user) return res.status(401).json({ success: false, message: "이메일 불일치" });

        const match = await bcrypt.compare(password, user.password);
        if (!match) return res.status(401).json({ success: false, message: "비밀번호 불일치" });

        const token = jwt.sign({ userId: user._id, email: user.email }, process.env.JWT_SECRET, { expiresIn: '1h' });
        
        res.status(200).json({ success: true, message: "로그인 성공", token, userId: user._id, username: user.name });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "서버 오류" });
    }
});

// 3. 상태 확인
router.get('/check', authenticateToken, (req, res) => {
    res.status(200).json({ success: true, message: "인증 확인됨", user: req.user });
});

module.exports = router;