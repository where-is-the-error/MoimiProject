const express = require('express');
const mongoose = require('mongoose');
const User = require('./models/User');
const Plan = require('./models/Plan');

const app = express();
app.use(express.json());

// 👇 여기에 본인 MongoDB 접속 주소 넣기 (localhost 혹은 Atlas 주소)
const MONGO_URI = 'mongodb://127.0.0.1:27017/my_location_app'; 

mongoose.connect(MONGO_URI)
  .then(() => console.log('✅ MongoDB Connected!'))
  .catch(err => console.error(err));

// ==========================================
// 🚀 1. 기본 조회 API (앱에서 쓸 것들)
// ==========================================

// 모든 사용자 조회 (위치 포함)
app.get('/api/users', async (req, res) => {
  const users = await User.find({});
  res.json(users);
});

// 나의 약속 조회 (populate로 참여자 정보까지 싹 긁어옴)
app.get('/api/plans', async (req, res) => {
  // 실제 앱에선 로그인한 사람 것만 찾겠지만, 시연용으로 전체 조회
  const plans = await Plan.find({}).populate('participants');
  res.json(plans);
});


// ==========================================
// 🛠️ 2. [시연용 치트키] 데이터 초기화 API
// 요청: POST http://localhost:3000/api/init-demo
// 효과: 기존 데이터 삭제 후 12월 13일 시연 세팅 완벽 복구
// ==========================================
app.post('/api/init-demo', async (req, res) => {
  try {
    // 1. 기존 데이터 싹 비우기 (Clean Slate)
    await User.deleteMany({});
    await Plan.deleteMany({});

    // 2. 사용자 3명 생성 (비밀번호 해시는 편의상 생략 혹은 더미)
    const userMe = await User.create({
      name: "throw",
      email: "throw@11.11",
      password: "hashed_password_dummy",
      phone: "12312312312",
      location: { type: 'Point', coordinates: [0, 0], name: "위치미정" } // 나는 아직 이동 중
    });

    const userTest1 = await User.create({
      name: "테스트1",
      email: "test1@11.11",
      password: "hashed_password_dummy",
      phone: "0101010101",
      location: { 
        type: 'Point', 
        coordinates: [126.9707, 37.5547], // 서울역
        name: "서울역" 
      }
    });

    const userTest2 = await User.create({
      name: "테스트2",
      email: "test2@11.11",
      password: "hashed_password_dummy",
      phone: "4818484",
      location: { 
        type: 'Point', 
        coordinates: [126.7898, 37.3265], // 안산역
        name: "안산역" 
      }
    });

    // 3. 약속 생성 (구일역, 12월 13일 22시)
    const newPlan = await Plan.create({
      title: "기능 시연용 모임 (구일역)",
      description: "서울역/안산역 출발 경로 테스트",
      date: new Date("2025-12-13T22:00:00.000+09:00"),
      destination: {
        name: "구일역 1호선",
        type: 'Point',
        coordinates: [126.8709, 37.4967]
      },
      participants: [userMe._id, userTest1._id, userTest2._id]
    });

    res.json({ 
      message: "✅ 시연 데이터 세팅 완료! (서울역/안산역 -> 구일역)", 
      plan: newPlan 
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "세팅 실패" });
  }
});

const PORT = 3000;
app.listen(PORT, () => console.log(`🚀 Server running on port ${PORT}`));