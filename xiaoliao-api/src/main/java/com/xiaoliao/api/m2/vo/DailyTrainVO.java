package com.xiaoliao.api.m2.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 每日训练汇总
 */
@Data
@Schema(description = "每日训练汇总")
public class DailyTrainVO {

    @Schema(description = "训练日期", example = "2026-08-03")
    private String trainDate;

    @Schema(description = "当日总时长（秒）")
    private Integer totalSecond;

    @Schema(description = "玩过的游戏种类数")
    private Integer gameCount;

    @Schema(description = "完成局数")
    private Integer finishRound;

    @Schema(description = "放弃局数")
    private Integer abandonRound;

    @Schema(description = "记忆均分")
    private Integer memoryScore;

    @Schema(description = "注意力均分")
    private Integer attentionScore;

    @Schema(description = "推理均分")
    private Integer reasonScore;

    @Schema(description = "语言均分")
    private Integer languageScore;

    @Schema(description = "训练均衡度 0-100，越高越均衡")
    private Integer balanceLevel;

    @Schema(description = "均衡度提示文案")
    private String balanceTip;
}
