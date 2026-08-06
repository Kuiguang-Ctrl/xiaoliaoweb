-- M2 脑力小游戏相关表（PostgreSQL）
-- 主键 BIGSERIAL，逻辑删除 deleted，无物理外键

-- 1. 游戏基础信息
CREATE TABLE IF NOT EXISTS m2_game_info (
    id              BIGSERIAL PRIMARY KEY,
    game_code       VARCHAR(32)  NOT NULL,
    game_name       VARCHAR(64)  NOT NULL,
    cognitive_domain VARCHAR(32) NOT NULL,
    game_desc       TEXT,
    single_minute   SMALLINT     DEFAULT 3,
    max_minute      SMALLINT     DEFAULT 5,
    error_punish    SMALLINT     DEFAULT 0,
    star_total      SMALLINT     DEFAULT 3,
    sort            INT          DEFAULT 0,
    is_enable       SMALLINT     DEFAULT 1,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m2_game_info IS 'M2游戏基础信息';
CREATE UNIQUE INDEX uk_m2_game_code ON m2_game_info(game_code) WHERE deleted = 0;

-- 2. 自适应难度配置
CREATE TABLE IF NOT EXISTS m2_game_level (
    id              BIGSERIAL PRIMARY KEY,
    game_id         BIGINT       NOT NULL,
    level_no        SMALLINT     NOT NULL,
    level_name      VARCHAR(32)  NOT NULL,
    config_json     TEXT,
    pass_rate_up    SMALLINT     DEFAULT 80,
    pass_rate_down  SMALLINT     DEFAULT 40,
    sort            INT          DEFAULT 0,
    is_enable       SMALLINT     DEFAULT 1,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m2_game_level IS 'M2自适应难度配置';
CREATE INDEX idx_m2_level_game ON m2_game_level(game_id, level_no) WHERE deleted = 0;

-- 3. 游戏素材
CREATE TABLE IF NOT EXISTS m2_game_resource (
    id              BIGSERIAL PRIMARY KEY,
    game_id         BIGINT       NOT NULL,
    res_type        VARCHAR(32)  NOT NULL,
    res_content     TEXT         NOT NULL,
    res_tag         VARCHAR(128),
    is_enable       SMALLINT     DEFAULT 1,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m2_game_resource IS 'M2游戏素材';
CREATE INDEX idx_m2_res_game ON m2_game_resource(game_id) WHERE deleted = 0;

-- 4. 用户单局记录（自适应核心）
CREATE TABLE IF NOT EXISTS m2_user_game_record (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    game_id             BIGINT       NOT NULL,
    level_id            BIGINT,
    pre_level_no        SMALLINT,
    next_level_no       SMALLINT,
    cognitive_type      VARCHAR(32),
    play_date           DATE         NOT NULL,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    cost_second         INT,
    total_question      SMALLINT,
    correct_question    SMALLINT,
    wrong_question      SMALLINT,
    pass_rate           SMALLINT,
    star                SMALLINT,
    game_status         SMALLINT     DEFAULT 0,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m2_user_game_record IS 'M2用户单局游戏记录';
CREATE INDEX idx_m2_record_user_game ON m2_user_game_record(user_id, game_id, end_time DESC) WHERE deleted = 0;
CREATE INDEX idx_m2_record_user_date ON m2_user_game_record(user_id, play_date) WHERE deleted = 0;

-- 5. 每日训练汇总
CREATE TABLE IF NOT EXISTS m2_user_daily_train (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    train_date      DATE         NOT NULL,
    total_second    INT          DEFAULT 0,
    game_count      SMALLINT     DEFAULT 0,
    finish_round    SMALLINT     DEFAULT 0,
    abandon_round   SMALLINT     DEFAULT 0,
    memory_score    SMALLINT     DEFAULT 0,
    attention_score SMALLINT     DEFAULT 0,
    reason_score    SMALLINT     DEFAULT 0,
    language_score  SMALLINT     DEFAULT 0,
    balance_level   SMALLINT     DEFAULT 0,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m2_user_daily_train IS 'M2每日训练汇总';
CREATE UNIQUE INDEX uk_m2_daily_user_date ON m2_user_daily_train(user_id, train_date) WHERE deleted = 0;

-- 6. 游戏成就
CREATE TABLE IF NOT EXISTS m2_user_game_achievement (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    record_id           BIGINT,
    game_id             BIGINT,
    star                SMALLINT,
    achievement_desc    VARCHAR(128),
    share_status        SMALLINT     DEFAULT 0,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m2_user_game_achievement IS 'M2游戏成就';
CREATE INDEX idx_m2_ach_user ON m2_user_game_achievement(user_id) WHERE deleted = 0;

-- ========== 初始化 4 款游戏（首期） ==========
INSERT INTO m2_game_info (game_code, game_name, cognitive_domain, game_desc, single_minute, max_minute, error_punish, star_total, sort, is_enable)
VALUES
('memory',   '翻牌配对',     '记忆',   '翻开卡片找出相同图案，练练记忆力', 3, 5, 0, 3, 1, 1),
('attention','找不同',       '注意力', '在格子中找出不一样的那个，练注意力', 3, 5, 0, 3, 2, 1),
('reason',   '分类小能手',   '推理',   '把物品拖到对应的分类里，练推理能力', 3, 5, 0, 3, 3, 1),
('language', '看图说词',     '语言',   '看图选择正确的词语，练语言表达', 3, 5, 0, 3, 4, 1);

-- ========== 难度配置（适配前端实际玩法） ==========

-- 翻牌配对：前端按 rows×cols 生成网格，pairCount 为配对数
INSERT INTO m2_game_level (game_id, level_no, level_name, config_json, pass_rate_up, pass_rate_down, sort, is_enable)
SELECT id, 1, '入门', '{"rows":2,"cols":2,"pairCount":2}', 80, 40, 1, 1 FROM m2_game_info WHERE game_code = 'memory'
UNION ALL
SELECT id, 2, '进阶', '{"rows":2,"cols":3,"pairCount":3}', 80, 40, 2, 1 FROM m2_game_info WHERE game_code = 'memory'
UNION ALL
SELECT id, 3, '高手', '{"rows":3,"cols":4,"pairCount":6}', 80, 40, 3, 1 FROM m2_game_info WHERE game_code = 'memory'
UNION ALL
SELECT id, 4, '达人', '{"rows":4,"cols":4,"pairCount":8}', 80, 40, 4, 1 FROM m2_game_info WHERE game_code = 'memory';

-- 找不同：前端按 gridSize 生成 NxN 网格，roundCount 为本关轮次
INSERT INTO m2_game_level (game_id, level_no, level_name, config_json, pass_rate_up, pass_rate_down, sort, is_enable)
SELECT id, 1, '入门', '{"gridSize":3,"roundCount":3}', 80, 40, 1, 1 FROM m2_game_info WHERE game_code = 'attention'
UNION ALL
SELECT id, 2, '进阶', '{"gridSize":4,"roundCount":4}', 80, 40, 2, 1 FROM m2_game_info WHERE game_code = 'attention'
UNION ALL
SELECT id, 3, '高手', '{"gridSize":5,"roundCount":5}', 80, 40, 3, 1 FROM m2_game_info WHERE game_code = 'attention'
UNION ALL
SELECT id, 4, '达人', '{"gridSize":6,"roundCount":6}', 80, 40, 4, 1 FROM m2_game_info WHERE game_code = 'attention';

-- 分类小能手：前端按 items 列表 + categories 分类进行游戏
INSERT INTO m2_game_level (game_id, level_no, level_name, config_json, pass_rate_up, pass_rate_down, sort, is_enable)
SELECT id, 1, '入门', '{"items":[{"name":"苹果","emoji":"🍎","type":"fruit"},{"name":"香蕉","emoji":"🍌","type":"fruit"},{"name":"白菜","emoji":"🥬","type":"vegetable"},{"name":"葡萄","emoji":"🍇","type":"fruit"},{"name":"胡萝卜","emoji":"🥕","type":"vegetable"},{"name":"西红柿","emoji":"🍅","type":"vegetable"}],"categories":[{"key":"fruit","name":"水果","icon":"🍎"},{"key":"vegetable","name":"蔬菜","icon":"🥬"}]}', 80, 40, 1, 1 FROM m2_game_info WHERE game_code = 'reason'
UNION ALL
SELECT id, 2, '进阶', '{"items":[{"name":"苹果","emoji":"🍎","type":"fruit"},{"name":"香蕉","emoji":"🍌","type":"fruit"},{"name":"白菜","emoji":"🥬","type":"vegetable"},{"name":"葡萄","emoji":"🍇","type":"fruit"},{"name":"胡萝卜","emoji":"🥕","type":"vegetable"},{"name":"西红柿","emoji":"🍅","type":"vegetable"},{"name":"草莓","emoji":"🍓","type":"fruit"},{"name":"茄子","emoji":"🍆","type":"vegetable"}],"categories":[{"key":"fruit","name":"水果","icon":"🍎"},{"key":"vegetable","name":"蔬菜","icon":"🥬"}]}', 80, 40, 2, 1 FROM m2_game_info WHERE game_code = 'reason'
UNION ALL
SELECT id, 3, '高手', '{"items":[{"name":"苹果","emoji":"🍎","type":"fruit"},{"name":"香蕉","emoji":"🍌","type":"fruit"},{"name":"白菜","emoji":"🥬","type":"vegetable"},{"name":"葡萄","emoji":"🍇","type":"fruit"},{"name":"胡萝卜","emoji":"🥕","type":"vegetable"},{"name":"西红柿","emoji":"🍅","type":"vegetable"},{"name":"草莓","emoji":"🍓","type":"fruit"},{"name":"茄子","emoji":"🍆","type":"vegetable"},{"name":"西瓜","emoji":"🍉","type":"fruit"},{"name":"玉米","emoji":"🌽","type":"vegetable"}],"categories":[{"key":"fruit","name":"水果","icon":"🍎"},{"key":"vegetable","name":"蔬菜","icon":"🥬"}]}', 80, 40, 3, 1 FROM m2_game_info WHERE game_code = 'reason'
UNION ALL
SELECT id, 4, '达人', '{"items":[{"name":"苹果","emoji":"🍎","type":"fruit"},{"name":"香蕉","emoji":"🍌","type":"fruit"},{"name":"白菜","emoji":"🥬","type":"vegetable"},{"name":"葡萄","emoji":"🍇","type":"fruit"},{"name":"胡萝卜","emoji":"🥕","type":"vegetable"},{"name":"西红柿","emoji":"🍅","type":"vegetable"},{"name":"草莓","emoji":"🍓","type":"fruit"},{"name":"茄子","emoji":"🍆","type":"vegetable"},{"name":"西瓜","emoji":"🍉","type":"fruit"},{"name":"玉米","emoji":"🌽","type":"vegetable"},{"name":"桃子","emoji":"🍑","type":"fruit"},{"name":"洋葱","emoji":"🧅","type":"vegetable"}],"categories":[{"key":"fruit","name":"水果","icon":"🍎"},{"key":"vegetable","name":"蔬菜","icon":"🥬"}]}', 80, 40, 4, 1 FROM m2_game_info WHERE game_code = 'reason';

-- 看图说词：前端按 questions 列表做选择题，timeLimitSec 为限时
INSERT INTO m2_game_level (game_id, level_no, level_name, config_json, pass_rate_up, pass_rate_down, sort, is_enable)
SELECT id, 1, '入门', '{"questions":[{"emoji":"🐱","answer":"小猫","options":["小猫","小狗","兔子","小鸟"]},{"emoji":"🚗","answer":"汽车","options":["自行车","汽车","火车","飞机"]},{"emoji":"🍚","answer":"米饭","options":["面条","馒头","米饭","饺子"]}],"timeLimitSec":60}', 80, 40, 1, 1 FROM m2_game_info WHERE game_code = 'language'
UNION ALL
SELECT id, 2, '进阶', '{"questions":[{"emoji":"🐱","answer":"小猫","options":["小猫","小狗","兔子","小鸟"]},{"emoji":"🚗","answer":"汽车","options":["自行车","汽车","火车","飞机"]},{"emoji":"🍚","answer":"米饭","options":["面条","馒头","米饭","饺子"]},{"emoji":"🌞","answer":"太阳","options":["月亮","星星","太阳","云朵"]}],"timeLimitSec":50}', 80, 40, 2, 1 FROM m2_game_info WHERE game_code = 'language'
UNION ALL
SELECT id, 3, '高手', '{"questions":[{"emoji":"🐱","answer":"小猫","options":["小猫","小狗","兔子","小鸟"]},{"emoji":"🚗","answer":"汽车","options":["自行车","汽车","火车","飞机"]},{"emoji":"🍚","answer":"米饭","options":["面条","馒头","米饭","饺子"]},{"emoji":"🌞","answer":"太阳","options":["月亮","星星","太阳","云朵"]},{"emoji":"🏠","answer":"房子","options":["学校","房子","医院","商店"]},{"emoji":"🌸","answer":"花","options":["树","草","花","叶"]}],"timeLimitSec":40}', 80, 40, 3, 1 FROM m2_game_info WHERE game_code = 'language'
UNION ALL
SELECT id, 4, '达人', '{"questions":[{"emoji":"🐱","answer":"小猫","options":["小猫","小狗","兔子","小鸟"]},{"emoji":"🚗","answer":"汽车","options":["自行车","汽车","火车","飞机"]},{"emoji":"🍚","answer":"米饭","options":["面条","馒头","米饭","饺子"]},{"emoji":"🌞","answer":"太阳","options":["月亮","星星","太阳","云朵"]},{"emoji":"🏠","answer":"房子","options":["学校","房子","医院","商店"]},{"emoji":"🌸","answer":"花","options":["树","草","花","叶"]},{"emoji":"🎸","answer":"吉他","options":["钢琴","吉他","笛子","小提琴"]},{"emoji":"⛰️","answer":"山","options":["河","山","湖","海"]}],"timeLimitSec":30}', 80, 40, 4, 1 FROM m2_game_info WHERE game_code = 'language';
