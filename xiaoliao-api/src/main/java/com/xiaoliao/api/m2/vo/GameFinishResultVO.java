package com.xiaoliao.api.m2.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 结束一局 — 响应（含自适应结果与鼓励文案）
 */
@Data
@Schema(description = "结束一局结果")
public class GameFinishResultVO {

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "本局正确率 0-100", example = "88")
    private Integer passRate;

    @Schema(description = "获得星级 0-3", example = "3")
    private Integer star;

    @Schema(description = "本局难度序号")
    private Integer preLevelNo;

    @Schema(description = "下一局推荐难度序号", example = "3")
    private Integer nextLevelNo;

    @Schema(description = "下一局推荐难度名称", example = "高手")
    private String nextLevelName;

    @Schema(description = "是否升级", example = "true")
    private Boolean levelUp;

    @Schema(description = "是否降级", example = "false")
    private Boolean levelDown;

    @Schema(description = "今日大脑训练能力提示", example = "今天练了记忆力")
    private String cognitiveTip;

    @Schema(description = "鼓励文案", example = "太棒了！再来一局也行哦")
    private String encourageMsg;

    @Schema(description = "成就ID，有星级时生成", example = "501")
    private Long achievementId;
}
