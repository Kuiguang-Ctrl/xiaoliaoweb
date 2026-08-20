package com.xiaoliao.api.m3.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 成就收藏/取消收藏请求
 */
@Data
public class CollectRequest {

    /** 成就来源类型：game=游戏成就，milestone=动态里程碑 */
    @NotBlank(message = "sourceType 不能为空")
    private String sourceType;

    /** 来源标识 */
    @NotBlank(message = "sourceId 不能为空")
    private String sourceId;

    /** true=收藏，false=取消收藏，缺省按收藏处理 */
    private Boolean collect;
}