# 小辽 · 老年积极心理 AI 助手

> 面向老年群体的积极心理陪伴服务。老人从**企业微信客服**进入，由 AI 陪伴对话，并通过签到打卡、脑力训练、积极心理练习与「回忆与传承」记录人生故事。

本仓库是项目的 **Java 后端服务 + Web 网页版**：多模块 Spring Boot 应用，同时以静态页面形式直接托管网页版。uni-app 小程序前端与 Python AI 引擎不在本仓库。

## 业务模块矩阵

| 模块 | 功能 | 状态 |
| --- | --- | --- |
| M1 | 签到打卡、AI 对话网关、JWT 无感鉴权、企微客服消息闭环 | ✅ 已完成 |
| M2 | 脑力小游戏（找不同 / 找规律 / 翻牌 / 文字游戏）+ 每日训练 | ✅ 已完成 |
| M3 | 积极心理练习：三件好事、感恩留言、成就墙、每周小结 | ✅ 已完成 |
| M4 | 回忆与传承：年代相册、人生时光轴、朋友圈文案、AI 文生图与视频合成、作品广场、隐私授权与数据导出 | ✅ 后端与网页版已完成 |

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 / 框架 | Java 17、Spring Boot 3.2、Maven 多模块 |
| 数据 | PostgreSQL 16 + pgvector、Redis、Flyway（11 个版本化迁移） |
| ORM | MyBatis-Plus（逻辑删除、字段自动填充、TypeHandler 透明加解密） |
| 企业微信 | WxJava（微信客服消息收发、小程序卡片） |
| AI 能力 | 对话大模型、通义千问 ASR 语音识别、qwen-image 文生图、ffmpeg 视频合成 |
| 接口文档 | Knife4j / springdoc-openapi |
| 可观测 | Actuator、Micrometer、Prometheus、自定义 `@ApiMetric` AOP 埋点 |
| 部署 | Docker Compose（PostgreSQL + Redis + 应用） |

## 目录结构

```
xiaoliaoweb
├── xiaoliao-api/                # 主服务：企微回调、REST API、AI 客户端、业务逻辑
│   └── src/main
│       ├── java/com/xiaoliao/api
│       │   ├── wecom/           # 企微客服回调与消息处理
│       │   ├── auth/  user/     # 鉴权与用户
│       │   ├── chat/            # AI 对话（REST + WebSocket + 主动问候）
│       │   ├── checkin/         # M1 签到打卡
│       │   ├── m2/ m3/ m4/      # 各业务模块
│       │   ├── audio/ upload/   # 语音识别、文件上传
│       │   ├── metrics/ config/ # 指标埋点与全局配置
│       │   └── util/            # JWT、Redis 锁、加密等工具
│       └── resources
│           ├── application*.yml # 主 / 开发 / 生产配置
│           ├── db/migration/    # Flyway 迁移脚本 V1 ~ V11
│           └── static/          # Web 网页版静态页面
├── xiaoliao-common/             # 统一 Result / 异常 / 枚举
├── xiaoliao-task/               # 定时任务模块
└── docs/                        # 设计文档
```

### Web 网页版

由后端直接托管 `xiaoliao-api/src/main/resources/static` 下的静态页面：

| 页面 | 路径 |
| --- | --- |
| AI 对话 | `/chat.html` |
| 我的时光 | `/web/time.html` |
| 我的花园 | `/web/garden.html` |
| 作品广场 | `/web/plaza.html` |

用户上传的图片通过 `/uploads/**` 映射到本地磁盘目录，访问形如 `http://host:port/uploads/yyyyMM/xxx.jpg`。

## 技术亮点

1. **企微客服消息闭环**：基于 WxJava 实现「消息轮询（增量拉取 + 游标）→ 意图识别 → 文本回复 + 小程序卡片」，`@Async` 异步处理保证回调能在 5s 内快速响应。
2. **分布式幂等**：定时提醒基于 Redis 分布式锁（`SET NX EX` 一条命令原子完成），key 按「日期 + 目标」设计、TTL 30h 租约，Lua 脚本比对 requestId 释放防误删；多实例 / 重启不重复推送，异常降级直发。
3. **JWT 无感登录**：`external_userid` 首条消息自动注册，7 天 token 随小程序卡片传递，`AuthInterceptor` 统一校验并透传 userId，兼容双身份体系。
4. **数据幂等与软删除**：打卡 / 每日训练 / 成就收藏采用「业务校验 + 唯一索引」双保险，并发重复提交由唯一索引拦截返回 409；`@TableLogic` + PostgreSQL 部分唯一索引（`deleted = 0`）防止软删除后重复。
5. **敏感字段加密**：AES-256-GCM（随机 IV）+ MyBatis `TypeHandler` 透明加解密，`enc:` 前缀兼容存量明文，密钥经环境变量注入，生产可轮换。
6. **可观测性**：自定义 `@ApiMetric` 注解 + AOP 切面统一采集接口调用量 / 耗时 / 失败数（Micrometer），经 `/actuator/prometheus` 暴露，可接入 Prometheus + Grafana 监控大盘；AI 引擎调用单独埋点。
7. **数据合规**：隐私授权、数据导出（可带走）、被遗忘权物理删除、家人帮忙分享链接 7 天有效且可作废。
8. **工程质量**：Flyway 11 个版本化迁移脚本；核心工具类单元测试 + Jacoco 覆盖率；Docker Compose 一键部署。

## 快速开始

### 环境要求

JDK 17+、Maven 3.9+、Docker（用于启动依赖服务）

### 1. 启动依赖（PostgreSQL + Redis）

```bash
docker compose up -d
```

### 2. 配置环境变量

```bash
cp .env.example .env   # 按注释填写各项配置
```

`.env` 已被 `.gitignore` 排除，不会进入版本库。主要变量见下方「配置说明」。

### 3. 启动服务

```bash
mvn -pl xiaoliao-api -am spring-boot:run
```

### 4. 访问入口

| 入口 | 地址 |
| --- | --- |
| 接口文档（Knife4j） | http://localhost:8080/doc.html |
| 监控指标（Prometheus） | http://localhost:8080/actuator/prometheus |
| AI 对话网页版 | http://localhost:8080/chat.html |

> `dev` 配置（默认激活）会关闭鉴权并指定 `mock-user-id`，方便本地不依赖企微直接联调；`prod` 配置保持鉴权开启。

## 配置说明

`application.yml` 中所有敏感配置均为 `${环境变量:默认值}` 形式，支持环境变量注入：

| 变量 | 说明 |
| --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | PostgreSQL 连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 |
| `SPRING_PROFILES_ACTIVE` | 激活配置（`dev` / `prod`） |
| `WECOM_CORP_ID` / `WECOM_SECRET` / `WECOM_TOKEN` / `WECOM_AES_KEY` | 企微客服回调 |
| `WECOM_KF_OPEN_KFID` / `WECOM_MINIPROGRAM_APPID` | 客服账号与小程序卡片 |
| `AI_SERVICE_URL` / `AI_API_TOKEN` | AI 引擎地址与鉴权 |
| `DEEPSEEK_API_KEY` | 对话模型 |
| `QWEN_API_KEY` | 语音识别（阿里云百炼） |
| `XIAOLIAO_DATA_KEY` | 敏感字段加密密钥（生产必须覆盖） |
| `XIAOLIAO_JWT_SECRET` | JWT 签名密钥 |
| `XIAOLIAO_PEXELS_KEY` | Pexels 图库 API Key |
| `XIAOLIAO_UPLOAD_DIR` / `XIAOLIAO_UPLOAD_BASE_URL` | 上传目录与对外访问地址 |
| `XIAOLIAO_FFMPEG_PATH` | ffmpeg 可执行文件路径（视频合成用） |

完整清单见 `.env.example`。

## 鉴权说明

`AuthInterceptor` 拦截 `/api/**`，以下路径放行：

- `/api/auth/**` —— 登录相关
- `/api/v1/m4/help/**` —— 家人帮忙的分享链接（需在有效期内）

其余接口通过 `Authorization` 头携带 JWT 访问。

## 测试

```bash
mvn verify
# Jacoco 覆盖率报告：xiaoliao-api/target/site/jacoco/index.html
```

单测覆盖 JWT 解析容错、Redis 分布式锁加锁 / 释放 / 降级、敏感字段加解密往返与旧数据兼容、签到服务。

## 设计文档

`docs/` 目录记录了各模块的设计与决策：

| 文档 | 内容 |
| --- | --- |
| `chat-websocket.md` | 对话 WebSocket 通道设计 |
| `ai-proactive-greeting.md` | AI 主动问候机制 |
| `asr-voice-input.md` | 语音输入（ASR）接入 |
| `m4-memory-design.md` | M4 回忆与传承整体设计 |
| `m4-video-memory.md` | 视频作品生成方案 |
| `m4-feature-spec.md` | M4 功能规格 |
| `engine-api.md` | AI 引擎接口约定 |
| `superpowers/` | 视频作品与文生图的方案设计与实施计划 |

## 部署

```bash
cp .env.example .env   # 填写生产配置
docker compose up -d --build
```

`docker-compose.yml` 会一并拉起 PostgreSQL（pgvector 镜像）、Redis 与应用，并在依赖健康检查通过后再启动应用。

> 生产环境请务必通过环境变量覆盖 `XIAOLIAO_DATA_KEY`、`XIAOLIAO_JWT_SECRET` 以及各第三方平台密钥。
