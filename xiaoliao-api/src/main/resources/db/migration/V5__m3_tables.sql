-- =============================================
-- 小辽 M3 积极心理练习（不含正念冥想）
-- 三件好事 + 感恩留言 + 成就墙收藏
-- =============================================
-- 约定：BIGSERIAL 主键、deleted 逻辑删除、user_id VARCHAR(36)（与 M2 一致）

-- ① 三件好事记录（每天最多 3 条，业务层校验）
CREATE TABLE IF NOT EXISTS m3_good_thing_records (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    content     TEXT        NOT NULL,
    record_date DATE        NOT NULL,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT    DEFAULT 0
);
COMMENT ON TABLE m3_good_thing_records IS 'M3三件好事记录';
COMMENT ON COLUMN m3_good_thing_records.content IS '好事内容（本期纯文本；语音转文字二期接入）';
COMMENT ON COLUMN m3_good_thing_records.record_date IS '记录日期（当天可修改/删除，温和无惩罚）';
CREATE INDEX idx_m3_good_user_date ON m3_good_thing_records(user_id, record_date) WHERE deleted = 0;

-- ② 感恩留言
CREATE TABLE IF NOT EXISTS m3_gratitude_notes (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    target_name VARCHAR(64)  NOT NULL,
    content     TEXT         NOT NULL,
    voice_url   VARCHAR(512),
    shared      SMALLINT     DEFAULT 0,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m3_gratitude_notes IS 'M3感恩留言';
COMMENT ON COLUMN m3_gratitude_notes.target_name IS '感谢对象：老伴/女儿/儿子/老友/自定义';
COMMENT ON COLUMN m3_gratitude_notes.voice_url IS '语音留言URL（本期预留，二期语音转文字接入）';
COMMENT ON COLUMN m3_gratitude_notes.shared IS '是否已分享给家人 0未分享 1已分享（M8家人连线接入）';
CREATE INDEX idx_m3_grat_user ON m3_gratitude_notes(user_id, create_time DESC) WHERE deleted = 0;

-- ③ 成就墙收藏（跨模块聚合：source_type=game 时 source_id 指向 m2_user_game_achievement.id；
--    source_type=milestone 时 source_id 为动态徽章标识，如 checkin-streak-7 / goodthing-10 / gratitude-3）
CREATE TABLE IF NOT EXISTS m3_achievement_collect (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id   VARCHAR(64) NOT NULL,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE m3_achievement_collect IS 'M3成就墙收藏';
COMMENT ON COLUMN m3_achievement_collect.source_type IS '成就来源类型：game=游戏成就，milestone=动态里程碑';
CREATE UNIQUE INDEX uk_m3_collect ON m3_achievement_collect(user_id, source_type, source_id);