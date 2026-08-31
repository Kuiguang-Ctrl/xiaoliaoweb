package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.util.List;

/**
 * 年代示意图匹配结果（老人照片 + 候选示意图）
 */
@Data
public class MatchResultVO {

    /** 老人上传的照片（语境，可能为空） */
    private PhotoVO photo;

    /** 候选示意图列表（按匹配度排序） */
    private List<StockImageVO> suggestions;

    /** 是否找到相关示意图：false 时 suggestions 为空，前端提示"换个说法"而不是硬配图 */
    private boolean matched;
}
