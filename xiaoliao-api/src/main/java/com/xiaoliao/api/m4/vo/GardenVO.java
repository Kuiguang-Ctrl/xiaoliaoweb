package com.xiaoliao.api.m4.vo;

import lombok.Data;

/**
 * 我的花园计数（复利可视化数据）
 */
@Data
public class GardenVO {

    /** 故事总数（一朵花） */
    private Long storyCount;

    /** 人生时光节点数 */
    private Long nodeCount;

    /** 年代记忆数 */
    private Long eraCount;

    /** 照片总数 */
    private Long photoCount;

    /** 已选中的朋友圈文案数 */
    private Long momentCount;
}