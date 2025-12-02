const mongoose = require('mongoose');

const scheduleSchema = new mongoose.Schema({
    creator_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    date: { type: String, required: true },
    time: { type: String, required: true },
    title: { type: String, required: true },
    location: { type: String, required: true },
    type: { type: String, enum: ['MEETING', 'CHECKLIST'], default: 'MEETING' },
    
    // ⭐ [추가] 6자리 초대 코드 (랜덤 숫자) - 중복 방지 로직은 간단하게 처리
    inviteCode: { 
        type: String, 
        unique: true,
        default: () => Math.floor(100000 + Math.random() * 900000).toString() 
    },

    participants: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Schedule', scheduleSchema);