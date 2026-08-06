package com.xiaoliao.api.m2.enums;

import lombok.Getter;

/**
 * 单局游戏状态
 */
@Getter
public enum GameStatusEnum {

    PLAYING(0, "进行中"),
    FINISHED(1, "完成"),
    ABANDONED(2, "放弃");

    private final Integer value;
    private final String label;

    GameStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
