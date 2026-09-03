# 小辽 AI 主动问候 — 引擎接口设计与对接文档

> 目标：用户打开小程序聊天页时，小辽主动发一条由大模型生成的问候。
> **引擎对接请直接看 `docs/engine-api.md`**（只含引擎要做的事）；本文档保留整体方案
> 与 Java/前端实现说明，供联调时参考。

---

## 1. 现状（已完成，无需再改）

### 1.1 前端（uni-app，已生效）
- 聊天页打开时（`pages/chat/chat.vue` 的 `onLoad`）调用 `GET /api/chat/greeting`（`api/chat.js` 的 `fetchGreeting`）。
- 拿到问候后以打字机效果展示为小辽气泡。
- 无需再改。

### 1.2 Java 后端（已实现，待接 AI 生成）
- 接口：`GET /api/chat/greeting`（`ProactiveController.java`）
- 逻辑（`ProactiveMessageService.java`）：
  1. 查 `proactive_messages` 表，当天已有问候 → 直接返回（每天每人最多一条）；
  2. 当天没有 → 生成问候并入库返回。
- 当前生成方式：**本地模板按日期轮换**（`TEMPLATE_GREETINGS`），属于临时兜底。
- 数据表：`proactive_messages`（`V8__proactive_messages.sql`）：`id BIGSERIAL`、`user_id`、`content`、`created_at`。
- 引擎实现 AI 接口后，只需把"模板生成"替换为"调引擎生成，失败再回退模板"（见 §3）。

---

## 2. 引擎侧待实现接口（唯一需要引擎做的事）

### 2.1 接口定义

```
POST /v1/greeting
```

| 项 | 说明 |
|---|---|
| 鉴权 | 与其他接口一致：`Authorization: Bearer <API_TOKEN>` |
| 请求体 | 可选 `{"user_id": "..."}`（预留个性化，首版可忽略） |
| 成功响应 | `200`，`{"reply": "早上好呀，我是小辽～ 今天想先和您聊聊天，或者去签个到，都可以的。"}` |
| 失败响应 | 沿用现有 `error_body` 契约：`{"error_code": "...", "request_id": "...", "message": "..."}` |

建议错误码：
- `401 AGENT_UNAUTHORIZED`：token 缺失/错误
- `502 AGENT_MODEL_UNAVAILABLE`：主模型不可用
- `504 AGENT_MODEL_TIMEOUT`：主模型超时

### 2.2 生成逻辑建议（关键）

**不要走主对话管线**（不做意图识别、RAG、安全质检、联网查询），直接用主模型（Hunyuan）生成一句问候。原因是主对话管线会把指令误判为用户对话（实测返回过天气信息、"没听清"等）。

建议实现（参考现有代码风格）：

```python
GREETING_SYSTEM_PROMPT = (
    "你是小辽，一位温暖、耐心、口语化的老年陪伴助手。"
    "请主动向老人发一条早安问候。"
    "要求：30-60字，温暖亲切，口语化，可以邀请老人签到、聊聊近况或玩个小游戏；"
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

参数建议：`temperature` 0.8~0.9、`max_tokens` 120 左右；超时沿用主模型配置。

### 2.3 响应契约字段（建议加到 `api_contract.py`）

```python
class V1GreetingResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    reply: str
```

---

## 3. Java 侧对接（引擎实现后，改 Java 两处）

### 3.1 `AiService.java` 增加方法

```java
public String greeting() {
    try {
        Map<String, Object> resp = aiRestClient.post()
                .uri("/v1/greeting")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        String reply = resp == null ? null : (String) resp.get("reply");
        return (reply == null || reply.isBlank()) ? null : reply.trim();
    } catch (Exception e) {
        log.warn("[AI] 主动问候生成失败，回退模板 根因={}", e.getMessage());
        return null;
    }
}
```

（`aiRestClient` 已自带 Bearer Token，无需额外配置。）

### 3.2 `ProactiveMessageService.java` 生成逻辑改为

```java
String content = aiService.greeting();
if (content == null || content.isBlank()) {
    content = TEMPLATE_GREETINGS[...]; // 现有模板兜底
}
```

其余逻辑（当天去重、入库、返回）保持不变。表结构、前端都不用动。

---

## 4. 测试步骤

1. 引擎侧：
   ```
   curl -X POST http://127.0.0.1:8081/v1/greeting \
     -H "Authorization: Bearer <API_TOKEN>" \
     -H "Content-Type: application/json" -d '{}'
   ```
   应返回 `{"reply": "..."}` 且内容是一句正常问候（无天气/括号/表情）。
2. 重启 Java 后端，访问 `http://localhost:8080/api/chat/greeting`，应返回 AI 生成的问候并入库。
3. 小程序重新打开聊天页，应看到小辽打字机打出问候。
4. 同一天重复打开：返回同一条（每天每人一条，符合预期）。

---

## 5. 注意事项

- 引擎返回空/超时/报错时，Java 自动回退模板，**不会阻塞聊天页**，可放心先上线模板版。
- 问候接口无状态、不写用户会话记忆（首版）；后续如需个性化（按用户记忆/情绪生成问候），再在请求体加 `user_id` 并让引擎按需读取。
- 接口鉴权必须保留（`API_TOKEN`），避免被滥用刷模型费用。

---

## 6. 主动消息体系（长期方向，建议按此设计）

目标不是"打开页面问候一句"，而是**小辽在合适的时间主动找老人聊**。建议引擎侧按"一个通用接口 + 场景参数"设计，一次到位：

### 6.1 场景（触发源，Java 定时任务触发）

| 场景 scene | 示例触发 | 频率建议 |
|---|---|---|
| `morning_greeting` | 每天 9:00 早安问候 | 每天 1 条 |
| `checkin_reminder` | 当天未签到，15:00 提醒 | 每天 1 条 |
| `mood_care` | 连续 2-3 天负面情绪签到 | 每天 1 条 |
| `inactive_recall` | 超过 N 天未互动 | 每周 1-2 条 |
| `festival` | 节日/节气 | 按需 |
| `game_recommend` | 完成训练后次日推荐 | 按需 |

### 6.2 引擎侧接口建议（把 §2 的 /v1/greeting 泛化）

```
POST /v1/proactive/message
{
  "user_id": "xxx",          // 预留：按用户记忆/情绪个性化
  "scene": "morning_greeting",
  "context": { }             // 预留：签到状态、最近互动等（Java 提供）
}
→ 200 {"reply": "..."}
```

- 生成逻辑与 §2.2 相同：**绕开对话管线，直接用主模型**按 scene 对应 System Prompt 生成。
- 建议每个 scene 维护一个 prompt（参考现有 `prompt_versions` 管理方式）。
- 响应契约、鉴权、错误码与 §2 一致。

### 6.3 下发通道（当前只做聊天页内）

1. **聊天页内（唯一通道）**：Java 把主动消息写入 `proactive_messages`，聊天页打开时
   `GET /api/chat/pushes` 补拉；**聊天页打开期间**由 WebSocket（`wss://…/ws/chat`）
   实时推送（详见 `docs/chat-websocket.md`，已实现）。
   限制：老人必须打开小程序聊天页才能看到；**不做微信订阅消息**，产品上接受这一形态。
2. **企业微信客服（已有能力，不在本次范围）**：企微会话内的主动问候/提醒已存在
   （`WeComMessageService`、引擎 `WECOM_ACTIVE_GREETING`），保留即可。

### 6.4 Java 侧扩展点（后续实现）

- `proactive_messages` 表加 `scene`、`status` 字段（或新表）。
- 新增 `ProactiveScheduler`：按 cron 跑各场景 → 生成（调引擎）→ 入库/下发。
- 频率与去重控制：每个用户每场景每天 N 条，避免打扰老人。
- 前端：`fetchGreeting` 泛化为 `fetchProactive(scene)`，聊天页轮询展示。

### 6.5 原则

- **触达要克制**：老人场景宁少勿多，默认每天最多 1-2 条主动消息。
- **引擎生成失败不阻塞**：Java 一律模板兜底。
- **通道只做聊天页内**：不做微信订阅消息；"老人没打开小程序就收不到"是当前形态的已知限制。
