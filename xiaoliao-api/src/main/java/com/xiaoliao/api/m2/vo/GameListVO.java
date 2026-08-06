package com.xiaoliao.api.m2.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 游戏列表项
 */
@Data
@Schema(description = "游戏列表项")
public class GameListVO {

    @Schema(description = "游戏ID", example = "1")
    private Long id;

    @Schema(description = "游戏编码", example = "memory")
    private String gameCode;

    @Schema(description = "游戏名称", example = "翻牌配对")
    private String gameName;

    @Schema(description = "认知域", example = "记忆")
    private String cognitiveDomain;

    @Schema(description = "游戏简介")
    private String gameDesc;

    @Schema(description = "标准单局时长（分钟）", example = "3")
    private Integer singleMinute;

    @Schema(description = "用户当前推荐难度序号", example = "2")
    private Integer recommendLevelNo;

    @Schema(description = "用户当前推荐难度名称", example = "进阶")
    private String recommendLevelName;
}
