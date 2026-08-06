-- =============================================
-- 小辽 V1 初始迁移 — 用户表 + 签到表
-- =============================================
-- 注：原 V1 中的 conversation_logs / lessons / game_records /
--     exercise_records / assessment_records / vector 扩展已移除，
--     需要时再建（旧版完整脚本可参考 git 历史 commit 6d9c25c）。

-- ─── 用户表 ───
-- 说明：id 用 VARCHAR(36) 而非 UUID —— Java 实体 id 是 String（MyBatis-Plus ASSIGN_UUID），
--       UUID 列 + varchar 参数会报 "操作符不存在: uuid = character varying"
CREATE TABLE users (
    id          VARCHAR(36) PRIMARY KEY,
    openid      VARCHAR(64) UNIQUE NOT NULL,
    nickname    VARCHAR(64),
    age         INT,
    gender      VARCHAR(8),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.openid IS '企业微信客服用户 openid';

CREATE INDEX idx_users_openid ON users(openid);

-- ─── 签到记录表（情绪天气 · 快捷签到）───
-- mood 可空：不选情绪纯签到为 NULL；取值 sunny/cloudy/overcast/rain/storm
CREATE TABLE checkin_records (
    id           VARCHAR(36) PRIMARY KEY,
    user_id      VARCHAR(36) NOT NULL REFERENCES users(id),
    mood         VARCHAR(16),
    mood_note    TEXT,
    checkin_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE checkin_records IS '每日签到与情绪记录';
COMMENT ON COLUMN checkin_records.mood IS '情绪: sunny/cloudy/overcast/rain/storm，NULL=纯签到未选情绪';
COMMENT ON COLUMN checkin_records.mood_note IS '情绪备注';

CREATE INDEX idx_checkin_user_date ON checkin_records(user_id, checkin_date);
-- 一天只能签一次
CREATE UNIQUE INDEX uq_checkin_user_date ON checkin_records(user_id, checkin_date);
