package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家人帮忙分享结果 VO（老人端）
 */
@Data
public class ShareHelpVO {

    private Long id;

    private Long nodeId;

    /** 节点展示名 */
    private String nodeName;

    /** 分享 token（拼成链接给亲友） */
    private String shareToken;

    /** 过期时间（7天） */
    private LocalDateTime expiredAt;

    /** 0待补 1老人已确认 2已作废 */
    private Integer status;
}