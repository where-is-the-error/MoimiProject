const mongoose = require('mongoose');

const scheduleSchema = new mongoose.Schema({
    creator_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }, // 작성자
    date: { type: String, required: true },   // "2025-09-22"
    time: { type: String, required: true },   // "17:00"
    title: { type: String, required: true },  // "팀 회의"
    location: { type: String, required: true }, // "회의실 A"
    type: { type: String, enum: ['MEETING', 'CHECKLIST'], default: 'MEETING' }, // [추가됨] 일정 유형
    
    // 참여자 목록 (작성자 + 초대된 친구들)
    participants: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Schedule', scheduleSchema);