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

    /** 直接给一段描述/口述（可选：没有 storyId 时按它生成，如"发照片写朋友圈"） */
    @Size(max = 5000, message = "描述不能超过5000字")
    private String description;

    /** 风格：simple=朴实 warm=温暖 humorous=幽默 proud=自豪 */
    @Size(max = 16, message = "风格标识不合法")
    private String style;
}
