package com.xiaoliao.api.m3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存感恩留言请求
 */
@Data
public class GratitudeSaveRequest {

    /** 感谢对象 */
    @NotBlank(message = "感谢对象不能为空")
    @Size(max = 20, message = "感谢对象不能超过20字")
    private String targetName;

    /** 留言内容 */
    @NotBlank(message = "留言内容不能为空")
    @Size(max = 500, message = "留言内容不能超过500字")
    private String content;
}