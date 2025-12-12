const mongoose = require('mongoose');

const planSchema = new mongoose.Schema({
  title: { type: String, required: true },
  description: String,
  date: { type: Date, required: true }, // 약속 시간
  
  // 📍 목적지 (구일역 등)
  destination: {
    name: String,
    address: String,
    type: { type: String, enum: ['Point'], default: 'Point' },
    coordinates: { type: [Number] } // [경도, 위도]
  },

  // 👥 참여자 (User 모델의 _id들을 저장)
  participants: [
    { type: mongoose.Schema.Types.ObjectId, ref: 'User' }
  ],
  
  status: { type: String, enum: ['pending', 'confirmed', 'completed'], default: 'confirmed' },
  created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Plan', planSchema);