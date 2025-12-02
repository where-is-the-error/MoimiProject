const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema({
    user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    message: { type: String, required: true },
    // ⭐ [신규] 알림 유형 (CHAT_REQUEST, NORMAL 등)
    type: { type: String, default: 'NORMAL' }, 
    // ⭐ [신규] 추가 데이터 (roomId 등)
    metadata: { type: Map, of: String }, 
    read: { type: Boolean, default: false },
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Notification', notificationSchema);