const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const User = require('../models/User'); 
const authenticateToken = require('../middleware/auth'); 

// 1. 회원가입
router.post('/register', async (req, res) => {
    // 💡 받은 데이터 디버깅 로직 (개발 시 유용)
    console.log("회원가입 요청 데이터:", req.body);
    
    try {
        // ⚠️ 필드 이름 변경: 클라이언트에서 'nickname'을 보낸다고 가정
        const { nickname, email, password } = req.body; 
        
        // --- 1단계: 필수 정보 누락 체크 및 기본 유효성 검사 ---
        if (!nickname || !email || !password || 
            nickname.trim() === '' || email.trim() === '' || password.trim() === '') {
            return res.status(400).json({ success: false, message: "이름, 이메일, 비밀번호를 모두 입력해야 합니다." });
        }
        
        // (선택) 비밀번호 길이 검사 등 추가 가능
        if (password.length < 6) {
            return res.status(400).json({ success: false, message: "비밀번호는 최소 6자 이상이어야 합니다." });
        }
        
        // --- 2단계: 중복 이메일 체크 ---
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            return res.status(409).json({ success: false, message: "이미 존재하는 이메일입니다." });
        }

        // --- 3단계: 사용자 생성 및 DB 저장 ---
        const hashedPassword = await bcrypt.hash(password, 10);
        
        const user = await User.create({
            name: nickname, // MongoDB User 모델의 name 필드에 nickname 저장
            email,
            password: hashedPassword
        });

        res.status(201).json({ success: true, message: "회원가입이 완료되었습니다.", userId: user._id });
    } catch (error) {
        console.error("회원가입 서버 오류:", error);
        res.status(500).json({ success: false, message: "서버 오류가 발생했습니다." });
    }
});

// 2. 로그인
router.post('/login', async (req, res) => {
    // 💡 받은 데이터 디버깅 로직 (개발 시 유용)
    console.log("로그인 요청 데이터:", req.body);
    
    try {
        const { email, password } = req.body;
        
        // --- 1단계: 필수 정보 누락 체크 ---
        if (!email || !password || email.trim() === '' || password.trim() === '') {
            return res.status(400).json({ success: false, message: "이메일과 비밀번호를 모두 입력해주세요." });
        }
        
        // --- 2단계: 사용자 조회 ---
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(401).json({ success: false, message: "가입되지 않은 이메일입니다." });
        }

        // --- 3단계: 비밀번호 일치 체크 ---
        const match = await bcrypt.compare(password, user.password);
        if (!match) {
            return res.status(401).json({ success: false, message: "비밀번호가 일치하지 않습니다." });
        }

        // --- 4단계: JWT 토큰 발급 ---
        const token = jwt.sign({ userId: user._id, email: user.email }, process.env.JWT_SECRET, { expiresIn: '1h' });
        
        res.status(200).json({ 
            success: true, 
            message: "로그인 성공", 
            token, 
            userId: user._id, 
            nickname: user.name // 닉네임 필드를 name 대신 nickname으로 반환 (클라이언트 편의성)
        });
    } catch (error) {
        console.error("로그인 서버 오류:", error);
        res.status(500).json({ success: false, message: "서버 오류가 발생했습니다." });
    }
});

// 3. 상태 확인
router.get('/check', authenticateToken, (req, res) => {
    res.status(200).json({ success: true, message: "인증 확인됨", user: req.user });
});

module.exports = router;