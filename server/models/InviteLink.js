const mongoose = require('mongoose');

const inviteLinkSchema = new mongoose.Schema({
    invite_id: { type: String, required: true, unique: true }, // UUID
    inviter_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    target_type: { type: String, enum: ['profile', 'meeting'], required: true },
    target_id: { type: String, required: true }, // ObjectId일 수도 있고 아닐 수도 있어서 String 처리
    invite_url: { type: String, required: true },
    expired_at: { type: Date, required: true },
    created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('InviteLink', inviteLinkSchema);