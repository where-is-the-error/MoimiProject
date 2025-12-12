const mongoose = require('mongoose');

const scheduleSchema = new mongoose.Schema({
    creator_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    date: { type: String, required: true },
    time: { type: String, required: true },
    title: { type: String, required: true },
    location: { type: String, required: true },
    type: { type: String, enum: ['MEETING', 'CHECKLIST'], default: 'MEETING' },
    
    // ⭐ [신규] Meeting 컬렉션과 연동 (참조 ID 저장)
    // 이 필드에 Meeting의 _id를 저장해두면, 나중에 두 데이터를 연결할 수 있습니다.
    meeting_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Meeting' },

    // 6자리 초대 코드
    inviteCode: { 
        type: String, 
        unique: true,
        default: () => Math.floor(100000 + Math.random() * 900000).toString() 
    },

    participants: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Schedule', scheduleSchema);