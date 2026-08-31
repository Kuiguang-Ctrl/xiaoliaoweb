package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 家人帮忙提交请求（免登录，凭 token）
 */
@Data
public class HelpSubmitRequest {

    /** 亲友补的文字/提示 */
    @Size(max = 1000, message = "提示不能超过1000字")
    private String helperNote;

    /** 亲友补的照片URL */
    @Size(max = 512, message = "图片URL不能超过512字符")
    private String helperPhotoUrl;
}