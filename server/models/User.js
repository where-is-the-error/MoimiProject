const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    name: { type: String, required: true },
    phone: { type: String },
    profile_img: { type: String }, // ✅ [필수] 이 필드가 있어야 사진 URL이 저장됨
    fcm_token: { type: String },
    location: {
        latitude: { type: Number },
        longitude: { type: Number },
        updated_at: { type: Date }
    }
});

module.exports = mongoose.model('User', userSchema);