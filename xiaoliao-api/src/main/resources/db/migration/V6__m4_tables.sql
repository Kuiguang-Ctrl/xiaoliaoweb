-- =============================================
-- 小辽 M4 回忆与传承（云存储版）
-- 人生时光（节点+故事）+ 年代记忆（年代+对照）+ 朋友圈文案 + 素材库 + 家人帮忙
-- =============================================
-- 约定：BIGSERIAL 主键、deleted 逻辑删除、user_id VARCHAR(36)（与 M2/M3 一致）

-- ① 人生时光节点（预设阶段，可增删改）
CREATE TABLE IF NOT EXISTS m4_timeline_node (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    stage       VARCHAR(32)  NOT NULL,
    custom_name VARCHAR(64),
    sort_order  INT          DEFAULT 0,
    year_from   INT,
    year_to     INT,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m4_timeline_node IS 'M4人生时光节点';
COMMENT ON COLUMN m4_timeline_node.stage IS '预设阶段：childhood/school/work/marriage/parenting/grandchildren/retirement';
COMMENT ON COLUMN m4_timeline_node.custom_name IS '自定义阶段名（老人改过时优先展示）';
CREATE INDEX idx_m4_node_user ON m4_timeline_node(user_id, sort_order) WHERE deleted = 0;

-- ② 年代记忆（年代列表，可增删改名）
CREATE TABLE IF NOT EXISTS m4_era (
    id             BIGSERIAL PRIMARY KEY,
    user_id        VARCHAR(36) NOT NULL,
    name           VARCHAR(64) NOT NULL,
    cover_photo_id BIGINT,
    create_time    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT    DEFAULT 0
);
COMMENT ON TABLE m4_era IS 'M4年代记忆';
COMMENT ON COLUMN m4_era.name IS '年代名，如：我最怀念的80年代';
CREATE INDEX idx_m4_era_user ON m4_era(user_id) WHERE deleted = 0;

-- ③ 照片（老人上传 or 系统示意图）
CREATE TABLE IF NOT EXISTS m4_photo (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL,
    source          VARCHAR(16)  NOT NULL,
    url             VARCHAR(512) NOT NULL,
    is_illustration SMALLINT     DEFAULT 0,
    era_tag         VARCHAR(32),
    node_id         BIGINT,
    era_id          BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m4_photo IS 'M4照片（用户上传/示意图）';
COMMENT ON COLUMN m4_photo.source IS 'user=老人上传 stock=系统示意图';
COMMENT ON COLUMN m4_photo.is_illustration IS '是否年代示意图 0否 1是';
CREATE INDEX idx_m4_photo_user ON m4_photo(user_id) WHERE deleted = 0;

-- ④ 故事（节点下/年代下，原文+润色）
CREATE TABLE IF NOT EXISTS m4_story (
    id            BIGSERIAL PRIMARY KEY,
    user_id       VARCHAR(36)  NOT NULL,
    node_id       BIGINT,
    era_id        BIGINT,
    photo_id      BIGINT,
    title         VARCHAR(64),
    original_text TEXT,
    polished_text TEXT,
    summary       VARCHAR(200),
    mood          VARCHAR(8),
    music         VARCHAR(128),
    status        SMALLINT     DEFAULT 0,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m4_story IS 'M4故事';
COMMENT ON COLUMN m4_story.node_id IS '所属人生时光节点（可选）';
COMMENT ON COLUMN m4_story.era_id IS '所属年代记忆（可选）';
COMMENT ON COLUMN m4_story.mood IS '讲完情绪：happy/flat/sad';
COMMENT ON COLUMN m4_story.music IS '背景音乐标识（可选）';
COMMENT ON COLUMN m4_story.status IS '0草稿 1已收好';
CREATE INDEX idx_m4_story_user_node ON m4_story(user_id, node_id) WHERE deleted = 0;
CREATE INDEX idx_m4_story_user_era ON m4_story(user_id, era_id) WHERE deleted = 0;

-- ⑤ 朋友圈文案
CREATE TABLE IF NOT EXISTS m4_moment_copy (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    photo_id    BIGINT,
    story_id    BIGINT,
    content     TEXT        NOT NULL,
    style       VARCHAR(16),
    selected    SMALLINT    DEFAULT 0,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT    DEFAULT 0
);
COMMENT ON TABLE m4_moment_copy IS 'M4朋友圈文案';
COMMENT ON COLUMN m4_moment_copy.style IS '风格：simple/warm/humorous/proud';
CREATE INDEX idx_m4_moment_user ON m4_moment_copy(user_id) WHERE deleted = 0;

-- ⑥ 请家人帮忙（分享链接协作，免登录）
CREATE TABLE IF NOT EXISTS m4_share_help (
    id               BIGSERIAL PRIMARY KEY,
    user_id          VARCHAR(36)  NOT NULL,
    node_id          BIGINT       NOT NULL,
    share_token      VARCHAR(64)  NOT NULL,
    helper_note      TEXT,
    helper_photo_url VARCHAR(512),
    expired_at       TIMESTAMP    NOT NULL,
    status           SMALLINT     DEFAULT 0,
    create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m4_share_help IS 'M4家人帮忙协作';
COMMENT ON COLUMN m4_share_help.status IS '0待补 1老人已确认 2已作废';
CREATE UNIQUE INDEX uk_m4_help_token ON m4_share_help(share_token);

-- ⑦ 素材库（运营预筛示意图，公版/免授权）
CREATE TABLE IF NOT EXISTS m4_stock_image (
    id          BIGSERIAL PRIMARY KEY,
    keywords    TEXT         NOT NULL,
    era         VARCHAR(32),
    scene       VARCHAR(32),
    url         VARCHAR(512) NOT NULL,
    license     VARCHAR(64)  DEFAULT 'CC0',
    source      VARCHAR(64),
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE m4_stock_image IS 'M4年代示意图素材库（公版/免授权）';
COMMENT ON COLUMN m4_stock_image.keywords IS '逗号分隔标签：年代/场景/人物形象';

-- 素材库种子数据（占位URL，上线前由运营替换为真实公版图）
INSERT INTO m4_stock_image (keywords, era, scene, url, license, source) VALUES
('土房,老屋,泥墙,农村','1960s','house','https://cdn.example.com/m4/tufang-1.jpg','CC0','placeholder'),
('车间,工厂,车床,工装','1960s','factory','https://cdn.example.com/m4/chejian-1.jpg','CC0','placeholder'),
('绿皮火车,火车站','1970s','station','https://cdn.example.com/m4/lvpi-1.jpg','CC0','placeholder'),
('自行车,二八大杠,街道','1980s','street','https://cdn.example.com/m4/zixingche-1.jpg','CC0','placeholder'),
('全家福,老照片,黑白','1960s','family','https://cdn.example.com/m4/quanjiafu-1.jpg','CC0','placeholder'),
('军装,当兵,军营','1970s','army','https://cdn.example.com/m4/junying-1.jpg','CC0','placeholder');