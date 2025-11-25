const mongoose = require('mongoose');

const meetingSchema = new mongoose.Schema({
    title: { type: String, required: true },
    location: { type: String, required: true },
    date_time: { type: Date, required: true },
    creator_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    reservation_required: { type: Boolean, default: false },
    // 참여자 목록을 배열로 관리
    participants: [{
        user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
        role: { type: String, enum: ['host', 'guest'], default: 'guest' },
        status: { type: String, default: 'pending' } // attended, absent 등
    }],
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Meeting', meetingSchema);