const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    name: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    profile_img: { type: String, default: null },
    trust_score: { type: Number, default: 0 },
    fcm_token: { type: String, default: null },
    // 위치 정보는 유저 문서 안에 내장 (Embedded)
    location: {
        latitude: Number,
        longitude: Number,
        updated_at: Date
    },
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', userSchema);