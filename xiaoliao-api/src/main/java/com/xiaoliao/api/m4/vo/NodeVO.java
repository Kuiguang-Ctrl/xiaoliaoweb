package com.xiaoliao.api.m4.vo;

import lombok.Data;

/**
 * 人生时光节点 VO（含展示名与故事数）
 */
@Data
public class NodeVO {

    private Long id;

    /** 阶段标识 */
    private String stage;

    /** 展示名：自定义名优先，否则默认阶段名 */
    private String displayName;

    private String customName;

    private Integer sortOrder;

    private Integer yearFrom;

    private Integer yearTo;

    /** 节点下视频作品数 */
    private Long videoCount;
}