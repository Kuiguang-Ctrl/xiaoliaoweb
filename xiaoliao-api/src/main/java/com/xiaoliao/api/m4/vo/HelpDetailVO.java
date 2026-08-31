package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家人帮忙详情 VO（亲友端/老人端共用）
 */
@Data
public class HelpDetailVO {

    private Long id;

    private Long nodeId;

    /** 节点展示名 */
    private String nodeName;

    /** 亲友补的文字/提示 */
    private String helperNote;

    /** 亲友补的照片URL */
    private String helperPhotoUrl;

    private LocalDateTime expiredAt;

    /** 0待补 1老人已确认 2已作废 */
    private Integer status;
}