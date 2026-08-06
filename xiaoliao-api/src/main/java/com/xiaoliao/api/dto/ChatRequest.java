package com.xiaoliao.api.dto;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String userId;
    private String message;

    /** 历史消息，Python 端要求必须为数组（不能为 null），不传时给空列表 */
    @Builder.Default
    private List<Map<String, String>> conversationHistory = new ArrayList<>();
}
