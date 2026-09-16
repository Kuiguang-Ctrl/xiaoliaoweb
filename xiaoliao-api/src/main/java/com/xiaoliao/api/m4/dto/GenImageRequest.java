package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 对话内文生图请求：老人描述一段场景，小辽生成一张照片
 */
@Data
public class GenImageRequest {

    /** 画面描述（小辽整理或老人原话），必填 ≤500 字 */
    @NotBlank(message = "画面描述不能为空")
    @Size(max = 500, message = "画面描述请控制在 500 字以内")
    private String prompt;

    /** 风格：old_photo(默认 老照片) / color(彩色) / sketch(素描)；空则按 old_photo */
    private String style;
}