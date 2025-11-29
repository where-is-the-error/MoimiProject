const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema({
    // 이 메시지가 속한 모임(채팅방) ID
    meetingId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Meeting',
        required: true
    },
    // 메시지를 보낸 사용자 ID
    senderId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    // 메시지 목록에 표시할 사용자 이름
    senderName: {
        type: String,
        required: true
    },
    // 실제 채팅 내용
    content: {
        type: String,
        required: true
    },
    // 메시지 전송 시각
    timestamp: {
        type: Date,
        default: Date.now
    }
});

module.exports = mongoose.model('Message', messageSchema);
