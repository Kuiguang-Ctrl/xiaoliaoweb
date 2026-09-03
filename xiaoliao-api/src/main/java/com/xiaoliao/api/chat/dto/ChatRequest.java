package com.xiaoliao.api.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 发给 Python AI 引擎的请求
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String userId;
    private String message;

    /** 本轮图片 URL 列表（老人发照片时最多 1 张；引擎支持多模态后使用） */
    private List<String> images;

    /** 会话ID：同一会话只弹一次推荐，前端每次进入聊天页生成一个即可 */
    private String sessionId;

    /** 历史消息，Python 端要求必须为数组（不能为 null），不传时给空列表 */
    @Builder.Default
    private List<Map<String, String>> conversationHistory = new ArrayList<>();
}
