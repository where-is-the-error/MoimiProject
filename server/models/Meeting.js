const mongoose = require('mongoose');

const meetingSchema = new mongoose.Schema({
    title: { type: String, required: true },
    location: { type: String },
    date_time: { type: Date },
    creator_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
    participants: [{
        user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
        role: { type: String, enum: ['host', 'guest'], default: 'guest' },
        status: { type: String, enum: ['attended', 'absent', 'pending'], default: 'pending' },
        lastReadAt: { type: Date, default: Date.now } // ✅ [추가] 마지막 읽은 시간
    }],
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Meeting', meetingSchema);