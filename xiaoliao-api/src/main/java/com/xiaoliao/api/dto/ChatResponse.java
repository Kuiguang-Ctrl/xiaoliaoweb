package com.xiaoliao.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Python AI 引擎返回的对话结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /** AI 生成的回复文本 */
    private String reply;

    /** 意图标识：chat / checkin / game / exercise / assessment / community */
    private String intent;

    /** 副Agent 5维检验结果 */
    private Map<String, Object> inspection;

    /** RAG 检索到的上下文列表 */
    private List<String> retrievedContexts;
}
