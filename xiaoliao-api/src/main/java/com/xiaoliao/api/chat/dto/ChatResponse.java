package com.xiaoliao.api.chat.dto;

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

    /** 意图标识：chat / checkin / game / exercise / assessment / community / m4_* */
    private String intent;

    /** 副Agent 5维检验结果 */
    private Map<String, Object> inspection;

    /** RAG 检索到的上下文列表（每项含 chunk_id / heading / score） */
    private List<Map<String, Object>> retrievedContexts;

    /** AI 意图推荐（引擎 /v1/intent/recommend 返回）：recommended=true 时前端弹窗跳转；引擎未实现时为 null */
    private Map<String, Object> recommendation;

    /**
     * 结构化卡片数据（前端聊天页渲染用）
     * type=buttons: {type, title, buttons:[{key,label}]}
     * type=images:  {type, title, images:[{url,title,license}]}
     * type=moments: {type, title, moments:[{id,style,text,selected}]}
     */
    private Map<String, Object> card;
}
