package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 生成朋友圈文案请求
 */
@Data
public class MomentGenerateRequest {

    /** 关联照片（可选） */
    private Long photoId;

    /** 关联故事（可选） */
    private Long storyId;

    /** 风格：simple=朴实 warm=温暖 humorous=幽默 proud=自豪 */
    @Size(max = 16, message = "风格标识不合法")
    private String style;
}