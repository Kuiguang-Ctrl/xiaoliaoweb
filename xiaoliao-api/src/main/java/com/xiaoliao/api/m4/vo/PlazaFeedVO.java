package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.util.List;

/**
 * 广场首页 VO（作品流 + 我的数据）
 */
@Data
public class PlazaFeedVO {

    /** 作品流（含自己的作品，自己的会标 mine=true） */
    private List<PlazaWorkVO> works;

    /** 广场作品总数 */
    private Long total;

    /** 我在广场上的作品数 */
    private Long myWorkCount;

    /** 我收到的赞合计 */
    private Integer receivedLikes;
}
