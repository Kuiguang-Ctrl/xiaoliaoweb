package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.util.List;

/**
 * 数据导出 VO（用户可带走全部数据）
 */
@Data
public class ExportVO {

    private List<NodeVO> nodes;

    private List<StoryVO> stories;

    private List<EraVO> eras;

    private List<PhotoVO> photos;

    private List<MomentVO> moments;

    private List<ConsentVO> consents;
}