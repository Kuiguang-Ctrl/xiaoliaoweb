# 小辽 —— 老年积极心理 AI 助手

面向老年群体的积极心理陪伴微信小程序，基于企业微信客服入口 + 微信小程序，实现签到打卡、AI 对话陪伴、三件好事、感恩留言、成就墙、脑力游戏、回忆与传承的完整功能闭环。

## 技术栈

- 后端：Java 17 / Spring Boot 3.2 / MyBatis-Plus / Maven 多模块
- 数据：PostgreSQL 16（pgvector）/ Redis / Flyway 版本化迁移
- 企微：WxJava 企微客服消息闭环
- 前端：uni-app（Vue3）小程序 / H5 双端（hui-xingfu 分支）
- 可观测：Actuator / Micrometer / Prometheus / 自定义 AOP 指标注解
- 其他：JWT、Knife4j（OpenAPI3 文档）、Docker Compose

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `xiaoliao-api` | 主服务：企微回调、REST API、AI 客户端、业务逻辑 |
| `xiaoliao-common` | 公共模块：统一 Result / 异常 / 枚举 |
| `xiaoliao-task` | 定时任务模块 |
| `hui-xingfu` | 小程序前端（独立分支） |

## 业务模块矩阵

| 模块 | 功能 | 状态 |
| --- | --- | --- |
| M1 | 签到打卡、AI 对话网关、JWT 无感鉴权、企微客服闭环 | ✅ 已上线联调 |
| M2 | 脑力小游戏（找不同/找规律/翻牌/文字游戏）+ 每日训练 | ✅ 已上线联调 |
| M3 | 积极心理练习：三件好事、感恩留言、成就墙、每周小结 | ✅ 已上线联调 |
| M4 | 回忆与传承：年代相册、人生时光轴、朋友圈文案、家人帮忙协作 | 🚧 后端完成，前端开发中 |

## 架构图

```
                        ┌────────────────────────────────────────────┐
   企微用户 ──消息──▶   │  WeComController 回调（5s 内快速响应 success）│
                        └──────────────────┬─────────────────────────┘
                                           │ @Async 异步
                                           ▼
                        ┌────────────────────────────────────────────┐
                        │  WeComAsyncHandler：意图识别 → 文本回复/卡片 │
                        └──────────────────┬─────────────────────────┘
                                           ▼
                        ┌────────────────────────────────────────────┐
                        │  AiService ──▶ Python AI 引擎（多模态/知识库）│
                        └────────────────────────────────────────────┘

   小程序 ──JWT 无感登录──▶  AuthInterceptor ──▶  REST API（/api/**）
                                                     │
                                ┌────────────────────┼────────────────────┐
                                ▼                    ▼                    ▼
                         PostgreSQL（Flyway 迁移）   Redis（分布式锁/缓存）   AI 引擎
```

## 技术亮点

1. **企微客服消息闭环**：基于 WxJava 实现「消息轮询（增量拉取+游标）→ 意图识别 → 文本回复+小程序卡片」，`@Async` 异步处理保证回调 5s 内快速响应。
2. **分布式幂等**：定时提醒基于 Redis 分布式锁（`SET NX EX` 一条命令原子完成），key 按「日期+目标」设计、TTL 30h 租约，Lua 脚本比对 requestId 释放防误删；多实例/重启不重复推送，异常降级直发。
3. **JWT 无感登录**：`external_userid` 首条消息自动注册、7 天 token 随小程序卡片传递，`AuthInterceptor` 统一校验并透传 userId，兼容 `wx.login` 双身份体系。
4. **数据幂等与软删除**：打卡/每日训练/成就收藏采用「业务校验 + 唯一索引」双保险，并发重复提交由唯一索引拦截返回 409；`@TableLogic` + PostgreSQL 部分唯一索引（deleted=0）防软删除后重复。
5. **敏感字段加密**：AES-256-GCM（随机 IV）+ MyBatis `TypeHandler` 透明加解密，`enc:` 前缀兼容存量明文，密钥经环境变量注入，生产可轮换。
6. **可观测性**：自定义 `@ApiMetric` 注解 + AOP 切面统一采集接口调用量/耗时/失败数（Micrometer），经 `/actuator/prometheus` 暴露，可接入 Prometheus + Grafana 监控大盘；AI 引擎调用单独埋点 `ai.engine.chat`。
7. **数据合规**：隐私授权、数据导出（可带走）、被遗忘权物理删除、家人帮忙分享链接 7 天有效且可作废。
8. **工程质量**：Flyway 7 版迁移脚本、核心工具类单测（JWT / 分布式锁 / 敏感字段加密）+ Jacoco 覆盖率、GitHub Actions CI、Docker Compose 一键部署。

## 快速启动

```bash
# 1. 启动依赖（PostgreSQL + Redis）
docker compose up -d

# 2. 配置环境变量（参考 .env.example 复制为 .env）

# 3. 启动主服务
mvn -pl xiaoliao-api spring-boot:run

# 4. 接口文档（Knife4j）
#    http://localhost:8080/doc.html

# 5. 监控指标（Prometheus 抓取端点）
#    http://localhost:8080/actuator/prometheus
```

## 测试与 CI

```bash
mvn verify
# Jacoco 覆盖率报告：xiaoliao-api/target/site/jacoco/index.html
```

- `.github/workflows/ci.yml`：push / PR 自动执行 `mvn verify`，并上传覆盖率报告产物。
- 单测覆盖：`TokenUtil`（JWT 解析容错）、`RedisLock`（加锁/释放/降级）、`CryptoTypeHandler`（加解密往返/旧数据兼容）。
