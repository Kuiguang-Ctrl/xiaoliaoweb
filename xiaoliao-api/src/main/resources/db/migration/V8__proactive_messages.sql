-- =============================================
-- 小辽 V8 — AI 主动消息表（聊天页打开时的小辽主动问候）
-- =============================================
CREATE TABLE proactive_messages (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(64) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE proactive_messages IS 'AI 主动消息：小辽主动问候，聊天页打开时拉取展示';
COMMENT ON COLUMN proactive_messages.user_id IS '用户ID（企微 external_userid 或 mock id）';
CREATE INDEX idx_proactive_user_time ON proactive_messages(user_id, created_at);