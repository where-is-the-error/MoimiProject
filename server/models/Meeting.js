const mongoose = require('mongoose');

const meetingSchema = new mongoose.Schema({
    title: { type: String, required: true },
    location: { type: String },
    
    // 📍 지도 시연용 목적지 좌표
    destination: {
        name: String,
        type: { type: String, enum: ['Point'], default: 'Point' },
        coordinates: { type: [Number], default: [0, 0] } 
    },

    date_time: { type: Date },
    creator_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
    
    participants: [{
        user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
        role: { type: String, enum: ['host', 'guest'], default: 'guest' },
        status: { type: String, enum: ['attended', 'absent', 'pending'], default: 'pending' },
        lastReadAt: { type: Date, default: Date.now },
        
        // ⭐ [추가] 위치 공유 켜짐 여부 (기본값 false)
        isSharing: { type: Boolean, default: false } 
    }],
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Meeting', meetingSchema);