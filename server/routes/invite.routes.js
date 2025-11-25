const express = require('express');
const router = express.Router();
const { randomUUID } = require('crypto');

// ⚠️ 경로 수정됨 (../)
const InviteLink = require('../models/InviteLink');
const authenticateToken = require('../middleware/auth');

router.post('/', authenticateToken, async (req, res) => {
    const { targetType, targetId, expiredAt } = req.body;
    const inviteId = randomUUID();
    const url = `https://moimi.app/invite/${inviteId}`;
    
    try {
        await InviteLink.create({
            invite_id: inviteId,
            inviter_id: req.user.userId,
            target_type: targetType,
            target_id: targetId,
            invite_url: url,
            expired_at: expiredAt
        });
            
        res.status(201).json({ success: true, inviteId, inviteUrl: url });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false });
    }
});

router.get('/:inviteId', async (req, res) => {
    try {
        const invite = await InviteLink.findOne({ invite_id: req.params.inviteId });
        if (!invite) return res.status(404).json({ success: false });
        
        res.json({ success: true, invite });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

module.exports = router;