const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    name: { type: String, required: true }, // 앱의 'username'과 매핑
    email: { type: String, required: true, unique: true }, // 앱의 'userId' 대신 이메일 사용
    password: { type: String, required: true },
    phone: { type: String, default: "" }, // [추가됨] 전화번호 필드
    profile_img: { type: String, default: null },
    trust_score: { type: Number, default: 0 },
    fcm_token: { type: String, default: null },
    location: {
        latitude: Number,
        longitude: Number,
        updated_at: Date
    },
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', userSchema);