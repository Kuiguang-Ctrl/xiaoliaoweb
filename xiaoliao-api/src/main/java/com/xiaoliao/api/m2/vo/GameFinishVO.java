package com.xiaoliao.api.m2.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 结束一局 — 请求
 */
@Data
@Schema(description = "结束一局请求")
public class GameFinishVO {

    @NotNull(message = "记录ID不能为空")
    @Schema(description = "开始接口返回的 recordId", example = "1001")
    private Long recordId;

    @NotNull(message = "总题数不能为空")
    @Min(value = 1, message = "总题数至少为1")
    @Schema(description = "总题数", example = "8")
    private Integer totalQuestion;

    @NotNull(message = "答对题数不能为空")
    @Min(value = 0, message = "答对题数不能为负")
    @Schema(description = "答对题数", example = "7")
    private Integer correctQuestion;

    @NotNull(message = "答错题数不能为空")
    @Min(value = 0, message = "答错题数不能为负")
    @Schema(description = "答错题数", example = "1")
    private Integer wrongQuestion;

    @NotNull(message = "耗时不能为空")
    @Min(value = 1, message = "耗时至少1秒")
    @Schema(description = "耗时秒", example = "95")
    private Integer costSecond;

    /**
     * 1 完成  2 放弃
     * 完成走自适应；放弃不降级
     */
    @NotNull(message = "状态不能为空")
    @Min(1)
    @Max(2)
    @Schema(description = "1完成 2放弃", example = "1")
    private Integer gameStatus;
}
