-- =============================================
-- V10：视频作品表（作品 = 后端合成的 mp4）
-- 设计文档：docs/superpowers/specs/2026-09-06-video-works-only-design.md
-- =============================================
CREATE TABLE IF NOT EXISTS m4_video (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    node_id     BIGINT,
    era_id      BIGINT,
    title       VARCHAR(64),
    caption     TEXT,
    video_url   VARCHAR(512) NOT NULL,
    poster_url  VARCHAR(512),
    duration    INT          DEFAULT 0,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m4_video IS 'M4视频作品（后端ffmpeg合成）';
COMMENT ON COLUMN m4_video.node_id IS '所属人生时光节点（与 era_id 二选一）';
COMMENT ON COLUMN m4_video.era_id IS '所属年代记忆';
COMMENT ON COLUMN m4_video.title IS '作品标题';
COMMENT ON COLUMN m4_video.caption IS '完整文案（详情页展示）';
COMMENT ON COLUMN m4_video.video_url IS '/uploads/videos/...mp4';
COMMENT ON COLUMN m4_video.poster_url IS '视频首帧封面';
CREATE INDEX idx_m4_video_user_node ON m4_video(user_id, node_id) WHERE deleted = 0;
CREATE INDEX idx_m4_video_user_era  ON m4_video(user_id, era_id) WHERE deleted = 0;
