package com.xiaoliao.api.m3.vo;

import lombok.Data;

/**
 * 成就墙条目（游戏成就 + 动态里程碑统一结构）
 */
@Data
public class AchievementVO {

    /** game=游戏成就，milestone=动态里程碑 */
    private String sourceType;

    /** game 指向 m2_user_game_achievement.id；milestone 为徽章标识 */
    private String sourceId;

    private String title;

    private String description;

    /** 展示用 emoji/星标 */
    private String emoji;

    /** 游戏成就的星级（1-3），里程碑为 null */
    private Integer star;

    /** yyyy-MM-dd HH:mm */
    private String createTime;

    /** 是否已收藏 */
    private boolean collected;
}