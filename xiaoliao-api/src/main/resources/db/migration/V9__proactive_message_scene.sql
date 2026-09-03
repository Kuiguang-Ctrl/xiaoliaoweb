-- =============================================
-- 小辽 V9 — 主动消息加场景字段（WebSocket 双向通讯）
-- =============================================
ALTER TABLE proactive_messages ADD COLUMN scene VARCHAR(32) NOT NULL DEFAULT 'greeting';
COMMENT ON COLUMN proactive_messages.scene IS '主动消息场景：greeting/checkin_reminder/mood_care/inactive_recall/festival/game_recommend';
