package com.xiaoliao.api.m3.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 某月好事记录
 */
@Data
public class GoodThingMonthVO {

    /** yyyy-MM */
    private String month;

    private int totalCount;

    private List<GoodThingRecordVO> records = new ArrayList<>();
}