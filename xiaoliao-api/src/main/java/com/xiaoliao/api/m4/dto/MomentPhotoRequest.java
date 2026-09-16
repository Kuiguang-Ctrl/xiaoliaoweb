package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 照片 + 老人描述 → 朋友圈文案请求
 * <p>独立入口：老人发一张照片、说两句照片里的故事，小辽据此改写成可发朋友圈的文案。
 * 照片、故事、文案都会落库，供网页/小程序/其他页面复用同一接口。
 */
@Data
public class MomentPhotoRequest {

    /** 已入库照片 ID（与 photoUrl 二选一） */
    private Long photoId;

    /** 已上传的图片 URL（来自 /api/upload；与 photoId 二选一，填了会先入照片库） */
    @Size(max = 512, message = "图片URL不能超过512字符")
    private String photoUrl;

    /** 老人的描述/口述（必填） */
    @NotBlank(message = "说说这张照片里的故事吧")
    @Size(max = 5000, message = "描述不能超过5000字")
    private String description;

    /** 文案标题（可选） */
    @Size(max = 64, message = "标题不能超过64字")
    private String title;

    /** 风格：simple=朴实 warm=温暖 humorous=幽默 proud=自豪 */
    @Size(max = 16, message = "风格标识不合法")
    private String style;

    /** 是否把这段描述收成故事落库（默认 true；只想要文案的页面可传 false） */
    private Boolean saveStory;

    /** 关联人生时光节点（可选） */
    private Long nodeId;

    /** 关联年代记忆（可选） */
    private Long eraId;
}
