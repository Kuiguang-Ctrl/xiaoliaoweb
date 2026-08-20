package com.xiaoliao.api.m3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交今日好事请求
 */
@Data
public class GoodThingSubmitRequest {

    /** 好事内容 */
    @NotBlank(message = "好事内容不能为空")
    @Size(max = 200, message = "好事内容不能超过200字")
    private String content;
}