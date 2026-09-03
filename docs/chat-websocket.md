# 小辽聊天页双向通讯 — WebSocket 设计与对接文档

> 目标：小辽（后端）在用户**打开聊天页期间**主动发消息，形成"双向通讯"：
> 用户发消息走 HTTP（一问一答），后端主动消息走 WebSocket 实时到达。
> 本文档说明已实现的通道与协议；**Python AI 引擎不需要改**（只负责"生成内容"，
> 主动消息的调度与下发全部在 Java 后端完成）。

---

## 1. 总体架构

```
微信小程序聊天页
  ├─ 用户发消息   → HTTP POST /api/chat            （原有，Java 转发引擎）
  ├─ 打开页面问候 → GET  /api/chat/greeting        （原有，每天每人一条）
  ├─ 打开页面补拉 → GET  /api/chat/pushes?afterId= （新增，WS 掉线兜底）
  └─ 实时收推送   → WebSocket wss://…/ws/chat      （新增，后端主动推）

Java 后端
  ├─ ChatWsHandler         在线连接管理 + 主动推送
  ├─ ProactivePushService  主动消息：入库 + 推送（在线实时 / 离线仅入库）
  ├─ ProactiveScheduler    定时任务（当前：每天 9:00 给在线用户推问候）
  └─ ProactiveController   /greeting /pushes /push（测试/运营）

Python AI 引擎（不改）
  └─ 生成主动消息内容（模板版暂由 Java 兜底；引擎按 ai-proactive-greeting.md 实现
     POST /v1/greeting 或 /v1/proactive/message 后，Java 改为调引擎生成）
```

## 2. WebSocket 通道

### 2.1 端点与鉴权

```
wss://xiaoliao.natapp1.cc/ws/chat?token=<JWT>
```

- 小程序 WebSocket 不能带自定义 Header，token 走 query 参数。
- 鉴权逻辑与 HTTP 一致（`ChatWsInterceptor` 复用 `TokenUtil`）：
  - 生产（`xiaoliao.auth.enabled=true`）：token 无效直接拒绝握手。
  - 开发（`enabled=false`）：无 token 时用 `mock-user-id`，方便联调。

### 2.2 帧协议

| 方向 | 帧 | 说明 |
|---|---|---|
| 客户端 → 服务端 | `{"type":"ping"}` | 心跳，每 30s 一次，防微信断开空闲连接 |
| 服务端 → 客户端 | `{"type":"pong"}` | 心跳应答（前端忽略） |
| 服务端 → 客户端 | `{"type":"connected","userId":"…"}` | 连接建立确认（前端忽略） |
| 服务端 → 客户端 | `{"type":"proactive","id":5,"scene":"greeting","content":"早上好呀…","createdAt":"2026-09-01T09:00:00"}` | **主动消息** |

### 2.3 可靠性

- 断线自动重连：3 秒退避重连（前端 `api/chat-socket.js`），页面卸载后停止。
- 主动消息**先落库再推送**：在线用户实时收到；掉线/未打开页面的用户，下次打开
  聊天页通过 `GET /api/chat/pushes?afterId=` 补拉，不丢消息。
- 前端三路去重（greeting / pushes / WebSocket 按 `id` 去重），同一条只展示一次。

## 3. 后端接口

| 接口 | 说明 |
|---|---|
| `GET /api/chat/greeting` | 打开页面问候（原有），每天每人一条 |
| `GET /api/chat/pushes?afterId=0` | 补拉 afterId 之后的主动消息（按 id 升序） |
| `POST /api/chat/push` `{"scene":"greeting","content":"…"}` | **测试/运营用**：给当前用户推一条（在线实时，离线仅入库） |

## 4. 定时任务（ProactiveScheduler）

当前：每天 9:00，对所有**在线**用户推当天问候（当天已有问候则跳过）；
不在线用户打开页面时由 `/greeting` 兜底。

后续扩展（按 `ai-proactive-greeting.md` §6.1 场景表）：
签到提醒（15:00）、心情关怀、久未互动召回、节日问候等，均在 Java 加 cron，
生成内容调引擎（模板兜底），再走 `ProactivePushService.pushAndSave` 下发。

## 5. 小程序侧配置与测试

### 5.1 微信后台（正式版）

- 小程序后台 → 开发管理 → 开发设置 → 服务器域名 → **socket 合法域名**：
  `wss://xiaoliao.natapp1.cc`
- 开发调试期：开发者工具「详情 → 本地设置 → 不校验合法域名」勾选即可，
  真机开发版同样生效（正式版必须配置合法域名）。

### 5.2 本地验证（后端启动后）

1. 打开后端：`http://localhost:8080/ws/chat?token=`（dev 模式可空 token），
   或直接用小程序聊天页（F12 控制台看 `[WS] 聊天通道已连接`）。
2. 触发一条主动消息（在线用户会实时收到）：
   ```
   curl -X POST http://localhost:8080/api/chat/push \
     -H "Content-Type: application/json" \
     -d '{"scene":"greeting","content":"小辽想您啦，来聊聊天吧～"}'
   ```
3. 断网/关页面重开：消息不丢（`/pushes` 补拉）。

## 6. 引擎接入（可选，非本次范围）

引擎只需按 `docs/ai-proactive-greeting.md` §2/§6.2 实现生成接口
（`POST /v1/greeting` 或泛化的 `POST /v1/proactive/message`），
Java 在 `ProactiveScheduler` / 业务事件里调用引擎生成内容 → `pushAndSave` 下发。
通道、协议、去重、补拉均无需再改。
