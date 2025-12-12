const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true }, // 실제론 bcrypt 암호화 권장
  phone: { type: String },
  trust_score: { type: Number, default: 0 }, // 신뢰도 점수
  
  // 📍 위치 정보 (GeoJSON 형식)
  location: {
    type: {
      type: String,
      enum: ['Point'], // 'Point'여야 함
      default: 'Point'
    },
    coordinates: {
      type: [Number], // [경도(lng), 위도(lat)] 순서 주의!
      default: [0, 0]
    },
    name: { type: String }, // 장소 이름 (예: 서울역)
    address: { type: String } // 상세 주소
  },
  
  created_at: { type: Date, default: Date.now }
});

// 위치 기반 검색을 위한 인덱스 설정
userSchema.index({ location: '2dsphere' });

module.exports = mongoose.model('User', userSchema);