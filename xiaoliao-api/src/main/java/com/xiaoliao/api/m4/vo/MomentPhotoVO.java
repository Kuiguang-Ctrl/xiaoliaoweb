package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.util.List;

/**
 * 照片+描述生成朋友圈文案的返回：照片/故事已落库，文案是 3 条候选
 */
@Data
public class MomentPhotoVO {

    /** 入库后的照片 ID（photoUrl 入的照片也在此返回） */
    private Long photoId;

    /** 照片可访问地址，页面直接展示 */
    private String photoUrl;

    /** 老人这段描述落成的故事 ID */
    private Long storyId;

    /** 3 条候选文案，点选后调 POST /moments/{id}/select */
    private List<MomentVO> moments;
}
