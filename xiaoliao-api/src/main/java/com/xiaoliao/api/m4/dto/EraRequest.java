package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 年代记忆请求（新增）
 */
@Data
public class EraRequest {

    /** 年代名，如：我最怀念的80年代 */
    @NotBlank(message = "年代名不能为空")
    @Size(max = 64, message = "年代名不能超过64字")
    private String name;
}