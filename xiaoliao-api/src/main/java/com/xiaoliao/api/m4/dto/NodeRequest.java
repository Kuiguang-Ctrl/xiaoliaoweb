package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 人生时光节点请求（新增/修改）
 */
@Data
public class NodeRequest {

    /** 阶段标识：childhood/school/work/marriage/parenting/grandchildren/retirement 或自定义 */
    @NotBlank(message = "阶段标识不能为空")
    @Size(max = 32, message = "阶段标识不能超过32字符")
    private String stage;

    /** 自定义阶段名（老人改过时优先展示） */
    @Size(max = 64, message = "阶段名不能超过64字")
    private String customName;

    private Integer sortOrder;

    private Integer yearFrom;

    private Integer yearTo;
}