package com.xiaoliao.api.m4.vo;

import lombok.Data;

/**
 * 素材库示意图 VO
 */
@Data
public class StockImageVO {

    private Long id;

    private String keywords;

    private String era;

    private String scene;

    private String url;

    private String license;
}