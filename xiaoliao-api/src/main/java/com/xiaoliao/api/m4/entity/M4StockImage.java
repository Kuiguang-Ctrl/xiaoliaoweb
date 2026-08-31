package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_stock_image")
public class M4StockImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逗号分隔标签：年代/场景/人物形象 */
    private String keywords;

    private String era;

    private String scene;

    private String url;

    private String license;

    private String source;

    private LocalDateTime createTime;
}
