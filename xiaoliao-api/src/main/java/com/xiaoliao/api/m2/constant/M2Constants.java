package com.xiaoliao.api.m2.constant;

/**
 * M2 模块常量
 */
public final class M2Constants {

    private M2Constants() {
    }

    /** 默认升级正确率阈值 */
    public static final int DEFAULT_PASS_RATE_UP = 80;

    /** 默认降级正确率阈值 */
    public static final int DEFAULT_PASS_RATE_DOWN = 40;

    /** 最低难度序号 */
    public static final int MIN_LEVEL_NO = 1;

    /** 单局最高星级 */
    public static final int MAX_STAR = 3;

    /** 成就未分享 */
    public static final int SHARE_NOT = 0;

    /** 成就已分享 */
    public static final int SHARE_YES = 1;

    /** 启用 */
    public static final int ENABLE = 1;

    /** 禁用 */
    public static final int DISABLE = 0;
}
