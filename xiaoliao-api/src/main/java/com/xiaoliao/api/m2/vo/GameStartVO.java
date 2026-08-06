package com.xiaoliao.api.m2.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 开始一局 — 响应
 */
@Data
@Schema(description = "开始一局响应")
public class GameStartVO {

    @Schema(description = "本局记录ID，提交结果时带回", example = "1001")
    private Long recordId;

    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "游戏名称")
    private String gameName;

    @Schema(description = "认知域")
    private String cognitiveDomain;

    @Schema(description = "难度ID")
    private Long levelId;

    @Schema(description = "难度序号", example = "2")
    private Integer levelNo;

    @Schema(description = "难度名称", example = "进阶")
    private String levelName;

    @Schema(description = "难度配置JSON，前端按此生成题目")
    private String configJson;

    @Schema(description = "标准时长（分钟）")
    private Integer singleMinute;

    @Schema(description = "最大时长（分钟）")
    private Integer maxMinute;

    @Schema(description = "是否无错学习（不扣分）", example = "true")
    private Boolean errorless;

    @Schema(description = "开局时间")
    private String startTime;
}
