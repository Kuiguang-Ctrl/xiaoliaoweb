-- ============================================================
-- 时光花园演示数据清理脚本（只清用户内容，保留素材库/系统表）
-- 执行：psql -d xiaoliao -f xiaoliao-api/scripts/reset-demo-data.sql
-- ============================================================
BEGIN;

-- M4 用户内容（删除顺序：先子表后主表；m4_stock_image 素材库保留）
DELETE FROM m4_share_help;
DELETE FROM m4_moment_copy;
DELETE FROM m4_story;
DELETE FROM m4_photo;
DELETE FROM m4_era;
DELETE FROM m4_privacy_consent;
DELETE FROM m4_timeline_node;

-- M1~M3 用户内容（本项目演示数据，一并清空）
DELETE FROM proactive_messages;
DELETE FROM m3_good_thing_records;
DELETE FROM m3_gratitude_notes;
DELETE FROM m3_achievement_collect;
DELETE FROM m2_user_game_achievement;
DELETE FROM m2_user_daily_train;
DELETE FROM m2_user_game_record;
DELETE FROM checkin_records;
DELETE FROM users;

COMMIT;
