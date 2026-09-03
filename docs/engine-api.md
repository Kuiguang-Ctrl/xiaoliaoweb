# 小辽 AI 引擎对接：只需实现 1 个接口

## 1. 接口

```
POST /v1/greeting
Authorization: Bearer <API_TOKEN>

请求（可选）：
{
  "user_id": "7764978f-...",   // 用户ID（预留个性化）
  "trigger": "chat_page_open", // 触发场景：用户刚打开聊天页
  "context": {"scene": "welcome", "goal": "greet_and_guide"}
}

成功：200 {"reply": "早上好呀，我是小辽～ 今天想先和您聊聊天，看看照片。"}

失败：沿用现有 error_body 契约
{"error_code": "...", "request_id": "...", "message": "..."}
错误码：401 AGENT_UNAUTHORIZED / 502 AGENT_MODEL_UNAVAILABLE / 504 AGENT_MODEL_TIMEOUT
```

**重要：这个接口的语义就是"用户刚打开小程序聊天页"——问候要引导老人开口说话，
用开放性问题结尾（如"今天有什么想跟小辽说的吗？""早上吃了什么好吃的？"），
而不是只说一句"你好"就结束。**

## 2. 生成逻辑（关键，别做错）

**不要走主对话管线**（不做意图识别、RAG、安全质检、联网查询），直接用主模型
（Hunyuan）生成一句问候。主对话管线会把指令误判为用户对话（实测返回过天气、
"没听清"等）。

> 说明：**不是废弃主对话管线**。用户正常聊天仍走原 `/chat` 管线（意图识别、RAG、
> 安全质检、联网查询），原样保留、不受影响。区别只在于：
> 管线是"回答用户的问题"——它先判断用户想干什么再回复；而主动问候**没有用户输入**，
> 后端只发来一句"生成问候"的指令，把它当用户消息处理就会被误判成"查天气/闲聊"。
> 所以 `/v1/greeting` 是管线旁边新增的独立快捷接口：不分析意图，直接让主模型说一句
> 问候。现有 `/chat` 及管线代码全部不用动。

```python
GREETING_SYSTEM_PROMPT = (
    "你是小辽，一位温暖、耐心、口语化的老年陪伴助手。"
    "请主动向老人发一条早安问候。"
    "要求：30-60字，温暖亲切，口语化；"
    "结尾用开放性问题引导老人开口说话（如'今天有什么想跟小辽说的吗？'）；"
    "可以邀请老人签到、聊聊近况或玩个小游戏；"
    "不要出现天气、日期、括号、表情符号。"
)

def generate_greeting(agent) -> str:
    reply = agent.main_client.chat([
        {"role": "system", "content": GREETING_SYSTEM_PROMPT},
        {"role": "user", "content": "请给老人发一条早安问候。"},
    ]).strip()
    if not reply:
        raise RuntimeError("empty greeting")
    return reply
```

参数建议：`temperature` 0.8~0.9、`max_tokens` 120；超时沿用主模型配置。
响应契约：`{"reply": "..."}`，可加到 `api_contract.py` 的 `V1GreetingResponse`。

## 3. 自测

```bash
curl -X POST http://127.0.0.1:8081/v1/greeting \
  -H "Authorization: Bearer <API_TOKEN>" \
  -H "Content-Type: application/json" -d '{}'
```

应返回 `{"reply": "..."}`：一句完整问候（30-60 字），无天气/日期/括号/表情。

## 4. 以后扩展（现在不用做）

主动消息以后会有"签到提醒、心情关怀"等场景，Java 已预留扩展点；
需要时按 `docs/ai-proactive-greeting.md` §6 把接口泛化为
`/v1/proactive/message`（带 `scene` 参数）。首版只做 `/v1/greeting` 即可。

未实现期间 Java 用本地模板兜底，全链路不阻塞；接口就绪后自动切换。

> **这个接口的语义就是"用户刚打开小程序聊天页"：问候要引导老人开口，
> 用开放性问题结尾（如"今天有什么想跟小辽说的吗？""早上吃了什么好吃的？"），
> 而不是只说一句"你好"就结束。**
