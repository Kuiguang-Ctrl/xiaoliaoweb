package com.xiaoliao.common.enums;

import lombok.Getter;

/**
 * AI 识别的用户意图类型
 */
@Getter
public enum IntentType {

    /** 纯聊天、问候、倾诉 */
    CHAT("chat", "纯聊天", null),

    /** 每日签到/记录情绪 */
    CHECKIN("checkin", "签到情绪", "pages/checkin/checkin"),

    /** 脑力游戏 */
    GAME("game", "脑力游戏", "pages/game/gameList"),

    /** 积极心理练习（M3 已上线，指到三件好事主页） */
    EXERCISE("exercise", "心理练习", "pages/m3/goodthings"),

    /** 心理测评（未上线，指到占位页） */
    ASSESSMENT("assessment", "心理测评", "pages/placeholder/placeholder"),

    /** 社区（未上线，指到占位页） */
    COMMUNITY("community", "社区", "pages/placeholder/placeholder"),

    /** 未知，兜底走聊天 */
    UNKNOWN("unknown", "未知", null);

    /** AI 返回的意图标识 */
    private final String code;

    /** 中文描述 */
    private final String label;

    /** 小程序页面路径，null 表示不发卡片 */
    private final String pagePath;

    IntentType(String code, String label, String pagePath) {
        this.code = code;
        this.label = label;
        this.pagePath = pagePath;
    }

    public static IntentType fromCode(String code) {
        for (IntentType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /** 是否需要发小程序卡片 */
    public boolean needsCard() {
        return pagePath != null;
    }
}
