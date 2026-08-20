-- M2 用户表 user_id 改 VARCHAR(36)：与 JWT/签到统一（JWT userId 是 UUID 字符串，原 BIGINT 存不下）
-- dev 阶段 mock 用户从零开始，历史 BIGINT 值会自动转字符串，不阻塞迁移
ALTER TABLE m2_user_game_record      ALTER COLUMN user_id TYPE VARCHAR(36);
ALTER TABLE m2_user_daily_train      ALTER COLUMN user_id TYPE VARCHAR(36);
ALTER TABLE m2_user_game_achievement ALTER COLUMN user_id TYPE VARCHAR(36);
