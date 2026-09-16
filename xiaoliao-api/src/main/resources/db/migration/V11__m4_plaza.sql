-- =============================================
-- V11：广场（作品分享 + 点赞）
-- 网页版 plaza.html 原来纯 localStorage 演示（hgs_plaza_v1），这里落库：
--   作品流 = 老人把自己做好的视频作品「挂到广场」；点赞一人一作品一条；「我收到的赞」= 我作品上的赞合计
-- 约定：BIGSERIAL 主键、deleted 逻辑删除、user_id VARCHAR(36)（与 M4 其它表一致）
-- =============================================
CREATE TABLE IF NOT EXISTS m4_plaza_work (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    video_id    BIGINT       NOT NULL,
    title       VARCHAR(64),
    video_url   VARCHAR(512),
    cover_url   VARCHAR(512),
    duration    INT          DEFAULT 0,
    like_count  INT          DEFAULT 0,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     DEFAULT 0
);
COMMENT ON TABLE  m4_plaza_work IS '广场作品（老人从自己的视频作品里发布出来的）';
COMMENT ON COLUMN m4_plaza_work.video_id IS '来源 m4_video.id（同一作品同时只挂一次）';
COMMENT ON COLUMN m4_plaza_work.title IS '作品标题（发布时快照，源作品改名不影响）';
COMMENT ON COLUMN m4_plaza_work.cover_url IS '封面图（视频首帧）';
COMMENT ON COLUMN m4_plaza_work.like_count IS '点赞数（冗余计数，明细以 m4_plaza_like 为准）';
-- 同一支作品只能挂一次（撤下后再发算新的一条）
CREATE UNIQUE INDEX uk_m4_plaza_video ON m4_plaza_work(video_id) WHERE deleted = 0;
CREATE INDEX idx_m4_plaza_user_time ON m4_plaza_work(user_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_m4_plaza_time ON m4_plaza_work(create_time DESC) WHERE deleted = 0;

CREATE TABLE IF NOT EXISTS m4_plaza_like (
    id          BIGSERIAL PRIMARY KEY,
    work_id     BIGINT       NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  m4_plaza_like IS '广场点赞（一人一作品一条，取消点赞即物理删除）';
CREATE UNIQUE INDEX uk_m4_plaza_like ON m4_plaza_like(work_id, user_id);
CREATE INDEX idx_m4_plaza_like_user ON m4_plaza_like(user_id);
