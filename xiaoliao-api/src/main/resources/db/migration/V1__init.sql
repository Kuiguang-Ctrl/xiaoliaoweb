-- =============================================
-- 小辽 V1 初始迁移 — 核心表结构
-- =============================================

-- pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- ─── 用户表 ───
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    openid      VARCHAR(64) UNIQUE NOT NULL,
    nickname    VARCHAR(64),
    age         INT,
    gender      VARCHAR(8),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.openid IS '企业微信客服用户 openid';
COMMENT ON COLUMN users.nickname IS '用户昵称';

CREATE INDEX idx_users_openid ON users(openid);

-- ─── 对话日志表 ───
CREATE TABLE conversation_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    user_msg        TEXT NOT NULL,
    agent_reply     TEXT NOT NULL,
    intent          VARCHAR(32),
    inspection_json JSONB,
    error_tags      TEXT[],
    score           INT CHECK (score >= 1 AND score <= 5),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE conversation_logs IS '对话日志 — 记录每次用户与AI的完整对话';
COMMENT ON COLUMN conversation_logs.intent IS 'AI 识别的意图';
COMMENT ON COLUMN conversation_logs.inspection_json IS '副Agent 5维检验原始结果';
COMMENT ON COLUMN conversation_logs.error_tags IS '错误标签数组，如 {suggested_too_early, ignored_emotion}';
COMMENT ON COLUMN conversation_logs.score IS '质检综合评分 1-5';

CREATE INDEX idx_clogs_user_time ON conversation_logs(user_id, created_at);
CREATE INDEX idx_clogs_score ON conversation_logs(score) WHERE score IS NOT NULL;
CREATE INDEX idx_clogs_created ON conversation_logs(created_at);

-- ─── 教训库 ───
CREATE TABLE lessons (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    error_tag     VARCHAR(64) NOT NULL,
    patch_content TEXT NOT NULL,
    embedding     VECTOR(1024),
    frequency     INT NOT NULL DEFAULT 1,
    status        VARCHAR(16) NOT NULL DEFAULT 'pending',
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE lessons IS '教训库 — 副Agent 聚类生成的 Prompt 补丁';
COMMENT ON COLUMN lessons.error_tag IS '错误模式标签';
COMMENT ON COLUMN lessons.patch_content IS '生成的 Prompt 补丁内容';
COMMENT ON COLUMN lessons.embedding IS 'bge-large-zh 向量，用于 RAG 检索';
COMMENT ON COLUMN lessons.frequency IS '错误触发次数';
COMMENT ON COLUMN lessons.status IS 'pending / verified / rejected';

CREATE INDEX idx_lessons_status ON lessons(status);
-- pgvector 索引稍后手动创建，IVFFlat 需要数据量达标

-- ─── 以下功能模块表待重新设计 ───
-- checkin_records  (M1 签到情绪)
-- game_records      (M2 脑力游戏)
-- exercise_records  (M3 心理练习)
-- assessment_records(M9 心理测评)
