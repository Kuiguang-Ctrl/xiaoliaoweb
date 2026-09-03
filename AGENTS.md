# AGENTS.md

## 项目关键路径(永远记住)
- 后端仓库根:`C:\Users\Administrator\Desktop\杨胜威\demo\xiaoliao-elderly-agent`(Java 17 / Spring Boot 3.2 / Maven 多模块)
- 小程序前端(uni-app):`D:\nui-app-xiaoliao\LiaoAssistant`

## 当前架构方向（2026-09-02 决定，后续开发以此为准）
- 前端以「时光花园」为准：小程序只显示 3 个原生页面 `pages/shiguang/chat|time|garden.vue`（对话 / 我的时光 / 我的花园），带原生 tabBar。
- 原 chat/m4/checkin/game/m3 等页面与 `pages/demo/demo.vue`（web-view 壳）代码全部保留在磁盘，但已从 `pages.json` 移除（小程序里不可见）。恢复旧版 = 把 `pages.json` 换成旧列表即可。
- 个人主体小程序不支持 web-view（无业务域名入口），因此 Demo 不能走 web-view 内嵌，必须原生页面。
- AI 对话 = 小程序直连 DeepSeek `https://api.deepseek.com/chat/completions`（Key 常量在 `pages/shiguang/common.js`，内嵌演示 Key，可被 `hgs_ds_key` 存储覆盖）；RAG 知识库从后端 `uploads/rag_index.json` 拉取（`https://xiaoliao.natapp1.cc/uploads/rag_index.json`，需后端 + natapp 运行，失败则通用回复兜底）。
- 语音输入 = 按住说话 → 上传后端 `/api/audio/asr`（natapp 域名，需后端运行 + application-dev.yml 里 QWEN_API_KEY）。
- 数据：画像/故事/花园全部存小程序本地（`hgs_*` key），不依赖后端接口。后端接口（签到/M2/M3/M4/企微）暂不接入前端。
- 测试：上传体验版前需在小程序后台把 `api.deepseek.com` 加进 request 合法域名（`xiaoliao.natapp1.cc` 已配置）。
- 演示用 DeepSeek Key 公开，正式上线前必须替换。
