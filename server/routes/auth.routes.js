const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const User = require('../models/User'); 
const authenticateToken = require('../middleware/auth'); 

// 1. 회원가입
router.post('/register', async (req, res) => {
    console.log("회원가입 요청 데이터:", req.body);
    
    try {
        // [수정] 안드로이드 DataModels.kt와 변수명을 정확히 일치시킴
        // RegisterRequest(email, password, name, phone)
        const { email, password, name, phone } = req.body; 
        
        // 유효성 검사
        if (!email || !password || !name) {
            return res.status(400).json({ success: false, message: "이메일, 비밀번호, 이름은 필수입니다." });
        }
        
        // 중복 이메일 체크
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            return res.status(409).json({ success: false, message: "이미 존재하는 이메일입니다." });
        }

        // 비밀번호 암호화
        const hashedPassword = await bcrypt.hash(password, 10);
        
        // DB 저장
        const user = await User.create({
            name,
            email,
            password: hashedPassword,
            phone: phone || "" // 전화번호 추가
        });

        res.status(201).json({ success: true, message: "회원가입 성공", userId: user._id });
    } catch (error) {
        console.error("회원가입 에러:", error);
        res.status(500).json({ success: false, message: "서버 오류 발생" });
    }
});

// 2. 로그인
router.post('/login', async (req, res) => {
    console.log("로그인 요청 데이터:", req.body);
    
    try {
        // [수정] 안드로이드 LoginRequest(userId, userPw) -> userId는 email로 처리
        const { userId, userPw } = req.body;
        
        // 변수 매핑 (앱에서는 userId라고 보내지만 실제로는 email)
        const email = userId;
        const password = userPw;

        if (!email || !password) {
            return res.status(400).json({ success: false, message: "아이디와 비밀번호를 입력하세요." });
        }
        
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(401).json({ success: false, message: "존재하지 않는 사용자입니다." });
        }

        const match = await bcrypt.compare(password, user.password);
        if (!match) {
            return res.status(401).json({ success: false, message: "비밀번호가 틀렸습니다." });
        }

        // 토큰 발급
        const token = jwt.sign({ userId: user._id, email: user.email }, process.env.JWT_SECRET, { expiresIn: '1y' });
        
        res.status(200).json({ 
            success: true, 
            message: "로그인 성공", 
            token, 
            userId: user._id, 
            username: user.name 
        });
    } catch (error) {
        console.error("로그인 에러:", error);
        res.status(500).json({ success: false, message: "서버 오류 발생" });
    }
});

module.exports = router;