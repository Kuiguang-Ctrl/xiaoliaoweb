# AGENTS.md

## 项目关键路径(永远记住)
- 后端仓库根:`C:\Users\Administrator\Desktop\杨胜威\demo\xiaoliao-elderly-agent`(Java 17 / Spring Boot 3.2 / Maven 多模块)
- 小程序前端(uni-app):`D:\nui-app-xiaoliao\LiaoAssistant`

## 当前架构方向（2026-09-02 决定，后续开发以此为准）
- 前端以「时光花园」为准：小程序只显示 3 个原生页面 `pages/shiguang/chat|time|garden.vue`（对话 / 我的时光 / 我的花园），带原生 tabBar。
- 原 chat/m4/checkin/game/m3 等页面与 `pages/demo/demo.vue`（web-view 壳）代码全部保留在磁盘，但已从 `pages.json` 移除（小程序里不可见）。恢复旧版 = 把 `pages.json` 换成旧列表即可。
- 个人主体小程序不支持 web-view（无业务域名入口），因此 Demo 不能走 web-view 内嵌，必须原生页面。
- AI 对话 = 小程序直连 DeepSeek `https://api.deepseek.com/chat/completions`（Key 常量在 `pages/shiguang/common.js`，内嵌演示 Key，可被 `hgs_ds_key` 存储覆盖）；RAG 知识库从后端 `uploads/rag_index.json` 拉取（`https://xiaoliao.natapp1.cc/uploads/rag_index.json`，需后端 + natapp 运行，失败则通用回复兜底）。【2026-09-10 更新：对话已统一走后端 `POST /api/ai/chat`（后端调 DeepSeek，RAG 也在后端）；此直连方式保留为回退开关（chat.vue 里 `CHAT_VIA_BACKEND=false`），详见下方「AI 对话后端化」章节。】
- 语音输入 = 按住说话 → 上传后端 `/api/audio/asr`（natapp 域名，需后端运行 + application-dev.yml 里 QWEN_API_KEY）。
- 数据：画像/故事/花园全部存小程序本地（`hgs_*` key），不依赖后端接口。后端接口（签到/M2/M3/M4/企微）暂不接入前端。
- 测试：上传体验版前需在小程序后台把 `api.deepseek.com` 加进 request 合法域名（`xiaoliao.natapp1.cc` 已配置）。
- 演示用 DeepSeek Key 公开，正式上线前必须替换。


## TAPD 同步约定（2026-09-08 杨胜威确定，默认执行）
- 对本项目代码/功能做出修改并落地（含已完成的模块、功能调整、缺陷修复）后，默认把对应改动同步登记到 TAPD「小辽项目」：workspace_id = 42527164，访问方式为本机 Codex MCP 服务器 `tapd`（OAuth 账号：杨胜威）。
- 同步规则：新完成的功能新增需求并置「已结束」(status_4)；未完结的功能保持/置「进行中」(status_3)；缺陷类改动登记缺陷；涉及批量补录或删除前，先把拟写入清单展示给用户确认，确认后再写。
- 补充：TAPD 中「rag存储」workspace_id = 32558665；小辽项目与代码的对应关系见 C:\Users\Administrator\Documents\ChatGPT\TAPD\项目映射.md。

## 视频/照片生成后端化（2026-09-11 已实施并验证）
- 结论：视频与照片的「生成」都在后端，前端只负责预览/展示。
  - 照片：网页「画照片」→ `POST /api/v1/m4/gen-image`（DashScope qwen-image，落盘 `uploads/images/yyyyMM/`，返回图片 url）；点「收进年代记忆/人生时光」→ `POST /api/v1/m4/photos`（source=gen，挂 nodeId/eraId）写 m4_photo。
  - 视频：网页「做视频」保存 → `vcCloudSync()` → `POST /api/v1/m4/videos`（multipart：destKind/nodeId|eraName/title/caption/captions[]/photos[]/bgm），后端 `M4VideoComposer` 用 ffmpeg 合成 720p mp4 + poster（`uploads/videos/yyyyMM/<token>/`）并写 m4_video；本地 canvas+MediaRecorder 只做页内预览，不再是成片来源。
- 本次修复（两个阻塞点）：
  1. 本机没有 ffmpeg（合成必 500、m4_video 长期 0 条）→ 已装到 `C:\Users\Administrator\Tools\ffmpeg\bin\ffmpeg.exe`（含 libx264/aac）并加入用户 PATH；新增配置 `xiaoliao.ffmpeg.path`（默认 `${XIAOLIAO_FFMPEG_PATH:ffmpeg}`，见 application.yml），`run-backend.bat` 已内置该环境变量。IDEA 里跑的后端需重启（或在运行配置里加 `XIAOLIAO_FFMPEG_PATH` 指向上面的 exe），否则找不到 ffmpeg。
  2. `M4VideoComposer.FONT_CANDIDATES` 原来只有 macOS/Linux 路径，Windows 上直接报「缺少中文字幕字体」→ 已加 `C:/Windows/Fonts/msyh.ttc`、`simhei.ttf`、`Deng.ttf`。字幕是 Java AWT 渲染透明 PNG 再 overlay（不依赖 ffmpeg drawtext/libfreetype）。
- 已验证（2026-09-11，临时实例 8080）：`/api/v1/m4/videos` 实测出片 2 支（m4_video id=1/2，7s 1280x720 h264+aac）；对成片抽帧 OCR，字幕识别为「第一张照片的故事」「第二张照片的故事」，中文正常；`/api/v1/m4/gen-image` 实测 8.1s 返回 1280x720 PNG。
- 旧「浏览器直传」方案作废（不需要新增 videos/upload 接口）。

## AI 对话后端化（2026-09-10 杨胜威确定，已实施）
- 目标：前端（小程序 + 网页）不再直连 DeepSeek；对话统一走后端，Key / RAG / 提示词组装都在后端，后端可拿到每次对话。
- 后端：`POST /api/ai/chat`（`AiChatController` → `DeepSeekChatService`）。请求体 `{message, conversationHistory, system?, maxTokens?, temperature?}`；system 为空 → 后端用与小程序 `buildSystemPrompt` 相同文案组装（`RagService` 直读 `uploads/rag_index.json`，1:1 复刻前端 `ragSearch`：关键词计分、对话指南优先、取 3 段）；system 传入 → 原样透传（网页版行为不变）。调用参数与前端一致：deepseek-chat / max_tokens 300（网页传 200）/ temperature 0.7 / stream=false / 30s 超时。
- 配置：`xiaoliao.deepseek.api-key`（dev 明文沿用前端原演示 Key；生产用环境变量 DEEPSEEK_API_KEY）、model、http-url、timeout。
- 前端：小程序 `common.js` 新增 `askBackendChat()`，`chat.vue` 顶部开关 `CHAT_VIA_BACKEND=true`（false=回退旧直连）；网页 `chat.html` 的 `llmChat` / `llmExtract` 已切 `askBackendChat`（system 透传）。旧函数、旧代码全部保留未删。
- 保留不动：旧 `/api/chat`（Python 引擎链路）与前端旧直连函数；网页右上角 DeepSeek Key 自检 ping 仍走直连。
- 已验证（2026-09-10，临时实例 8082 实测）：小程序模式与网页透传模式均正常返回；RAG 加载 1157KB、约 0.9s 响应；日志 `[RAG]` / `[AI-BACKEND]` 可查对话内容。正式使用前需重启 IDEA 后端，并在小程序（natapp 运行）+ 网页各验一次。
- 遗留：流式输出（拟扩展 `/ws/chat`）、会话落库、正式鉴权开启；演示 Key 曾在前端公开，上线前必须更换。
- TAPD：待办 1105「将 AI 对话转变成后端」已加进展评论（保持进行中）。

## 朋友圈文案前端对接（2026-09-11 杨胜威确定，已实施）
- 接口：`POST /api/v1/m4/moments/generate`（`{storyId?, photoId?, style?}`，style=simple/warm/humorous/proud；每次插 3 条进 `m4_moment_copy` 并返回，限流 20 次/天）、`GET /api/v1/m4/moments`、`POST /api/v1/m4/moments/{id}/select`。
- 关键：后端按故事 `polishedText`（空则 `originalText`）套风格框架拼 3 条；**必须先 `POST /api/v1/m4/stories` 落库拿 storyId**，不带 storyId 只会得到通用模板兜底文案。
- 网页 `static/chat.html`：`lifeFinishStory()` / `eraFinish()` →「📤 做成故事卡发朋友圈」→ `makeMoments(kind)`：故事落库（节点缺失自动 `POST /timeline/nodes` 建 work/school，年代缺失自动 `POST /eras` 建「80 年代 · 老纺织厂」）→ 生成 3 条 → `pickMoment(i)` 调 select → `showShare(kind, content)` 分享卡新增文案区（`#scText`）；`🔄 换一组说法` 轮换风格；后端连不上自动退回原演示文案。
- 网页入口修复：聊天菜单「接下来想做点什么」新增 `👤 讲讲我的一辈子`(startLife) 与 `🕰 说说我怀念的年代`(startEraContrast)——这两个叙事演示出口在改版时被摘掉、流程不可达，已接回。
- 小程序 `pages/shiguang/common.js` 新增 `m4Request / m4Timeline / m4SaveNode / m4Eras / m4SaveEra / m4SaveStory / m4GenerateMoments / m4SelectMoment`（uni.request 直连 `https://xiaoliao.natapp1.cc`，dev 后端免 token）；`chat.vue` 收好故事后走同一流程，选中文案弹分享卡、可一键复制。原 `shareToast()` 演示兜底保留。
- 验证：2026-09-11 公网 https://xiaoliao.natapp1.cc/chat.html 实测通过（人生时光 + 年代记忆两条链路：生成→选中→分享卡；换一组说法正常）；小程序侧仅语法校验，待开发者工具/真机验证。
- 运维：改 `static/*` 后必须 `mvn -DskipTests -pl xiaoliao-api -am package` 重新打包并重启后端（跑的是 `xiaoliao-api/target/xiaoliao-api-0.1.0-SNAPSHOT.jar`，natapp 才生效）。

## 说几句/发照片 → 优化文案（独立接口，可复制）（2026-09-11 杨胜威确定，已实施）
- 口径：**不做发朋友圈**。功能 = 老人说几句（可带一张照片）→ 后端按他的原话用 AI 优化出 3 条候选 → 选中一条**复制**走。网页/小程序的入口文案已全部中性化为「✍️ 帮我写一段 / 说几句，帮我写一段」，后续别再写「发朋友圈」。
- 接口：`POST /api/v1/m4/moments/photo`（`M4Controller` → `M4ServiceImpl.generateMomentsFromPhoto`，指标 `m4.moments.photo`）。请求 `{photoId? | photoUrl?, description 必填≤5000, title?, style?, nodeId?, eraId?, saveStory?默认 true}`；返回 `{photoId, photoUrl, storyId, moments[3]}`。photoUrl 分支先 `savePhoto` 入库；描述默认落成一条故事（`saveStory=false` 可跳过）；**独立接口，其它页面直接复用**。
- AI 改写：`M4ServiceImpl.aiMomentCopies(style, storyText, title, withPhoto)` 调 `DeepSeekChatService`，小辽人设 prompt：第一人称口语、只用老人原话不编造、≤90 字、严格 3 条且行首 `1. 2. 3.`；返回后正则清洗行首编号。失败/未配 Key 返回 null → `momentCopyForStory()` + `momentTemplates` 规则兜底凑满 3 条去重。
- 网页 `static/chat.html`：快捷 chip / 聊天菜单「✍️ 帮我写一段」→ `momentPhotoStart()`（📁 从手机里选 = 隐藏 `input#photoFile` → `/api/upload`；📚 从我的照片里选 = `M4.photos()`）→ 请老人描述（`awaitingInput='mpdesc'`，`sendText`/`stopRec` 两条入口都认）→ `momentPhotoGenerate()`（`api.js` 的 `M4.momentsFromPhoto`）→ 3 条候选 → `pickMoment(i)` **自动复制** + 弹分享卡；分享卡新增「📋 复制这段文案」按钮，`copyText()` 优先 `navigator.clipboard`、失败退回 textarea+`execCommand`（`fallbackCopy`）；后端连不上退回演示文案（`renderMomentFallback`，同样可复制）。
- 小程序 `pages/shiguang/`：`common.js` 新增 `m4Photos / m4MomentsFromPhoto / m4UploadImage / fixImgUrl`；`chat.vue` 快捷 chip「✍️ 帮我写一段」+ `momentPhotoStart / pickMomentPhoto / uploadMomentPhoto / momentPhotoLib / pickMomentPhotoFromLib / askMomentPhotoDesc / generateMomentFromPhoto / pickMomentPhotoCopy`，选中即 `uni.setClipboardData`，分享卡「📋 复制文案」+ 照片。
- 验证（2026-09-11 公网 chat.html 实测）：chip → 从照片库选（photoId 19）→ 打字描述 → 3 条按原话改写的文案 → 选①自动复制（toast「📋 文案已复制…」）→ 分享卡带真图（`#scImg` 1920x920）+ 复制按钮。小程序侧仅 `node --check` 语法校验，待微信开发者工具/真机验证。
- 运维：改了 AI prompt（`M4ServiceImpl`）或 `static/*` 都要重新 `mvn -DskipTests -pl xiaoliao-api -am package` + 重启后端，natapp 上才生效。

### 入口改版（2026-09-11 二次确认）：发照片 → 弹层问「想做什么」
- 口径：**底栏不要再加按钮**（越加越多）。原加的快捷 chip「✍️ 帮我写一段」已撤掉，快捷行恢复 4 个 chip（做视频/画照片/看作品/去广场）；拍照/相册入口收进输入栏左侧的 📷 小圆按钮（`.pic-input-btn` → `photoInput()`，比 🎙 多一个图标，不占底栏）。
- 网页流程：老人发照片（📷 或从「我的照片」里选）→ 对话里先显示这张照片（`addMyPhoto`）+ 弹层 `#photoAskModal`（沿用页面已有 `.modal` 弹层样式）问「这张照片，您想做什么？」：
  `✍️ 优化朋友圈文案`（`photoToMoment()` → 请老人说两句 → 3 条候选 → 选中自动复制 + 分享卡）
  `🎬 做视频`（`photoToVideo()` 把这张塞进视频流程当第一张，后面还能再添）
  `＋ 其他`（`photoAskOther()` 展开：📁 收进我的时光 `photoSaveToLife()` / 📚 换一张照片 / ‹ 返回）
  `💬 先不做什么`。
- 其它页面/入口复用：直接调 `photoEntryReceive({url, photoId})` 即可走同一套弹层；「故事卡」那条链路（人生时光/年代记忆收好故事）的按钮也统一叫「✍️ 优化朋友圈文案」。
- 坑：相册来的照片只有 URL，进视频流程前要用 `photoUrlToThumb()` 取回 dataURL——**先按同源 `/uploads/...` 路径 fetch**（https 页面取 http 图片会被浏览器按混合内容拦掉），失败再退回 `absUrl()` 完整地址。
- 小程序同一套：`chat.vue` 输入栏 `.sg_pic` 📷 → `photoEntryStart()` → `photoEntryUpload()` → 弹层 `photoAsk`（`✍️ 优化朋友圈文案` / `＋ 其他`：📁 收进我的时光 `photoSaveToLife()`、📚 换一张照片 / `💬 先不做什么`）。小程序端暂不做视频，所以弹层里没有「做视频」；`common.js` 新增 `m4SavePhoto()`。

### 年代记忆 · 每个年代里也能「传照片写文案 / 讲个故事」（2026-09-11 已实施，网页版）
- 网页 `static/web/time.html` + `js/time.js`：点进任一**年代**（`openEra()`）后，详情页顶部新增两个入口 ——
  `📷 传张照片，让小辽写文案`（`openEraPhoto()` → 弹层 `#eraPhotoMask`：选图 `pickEraPhoto()`→`/api/upload`、说两句（可点 `micEraPhoto()` 说话转文字）→ `genEraPhotoCopies()` 调 `M4.momentsFromPhoto({photoUrl, description, eraId, title})` → 3 条候选，**点一下那一条即复制**（`copyEraCopy(i)`，同时 `selectMoment` 落库）+「🔄 换一组说法」）；
  `📖 讲讲这个年代的故事`（`openEraStory()` → 弹层 `#eraStoryMask`：标题可选 + 正文（`micEraStory()` 可说话转文字）→ `saveEraStory()` 调 `M4.saveStory({eraId, title, originalText, polishedText})`）。
- 年代详情新增「📖 这个年代的故事」区（`renderStories()`，带删除 `deleteEraStory(id)`），顶部统计也加了「N 故事」。
- 后端配套：`GET /api/v1/m4/stories` 增加 `eraId` 过滤（`M4Controller` / `M4Service.listStories(userId, nodeId, eraId)`）；`web/js/api.js` 的 `M4.stories(nodeId, eraId)` 同步。
- 网页端「说话转文字」= 浏览器 `webkitSpeechRecognition`（Chrome 可用）；小程序端年代记忆目前是本地「对照卡」模型（`loadEras()`），要接这两个入口需改成后端 `m4_era` 或按年代名自动建，待定。

### 人生时光 · 每一站同样有「传照片写文案 / 讲个故事」（2026-09-11 已实施，网页版）
- 同上一节的能力，抽成了通用实现：`photoTarget` / `storyTarget = {kind:'era'|'node', id, name}`，两个弹层（`#eraPhotoMask` / `#eraStoryMask`）复用一套 UI 和逻辑。
- 入口：`openNode()` 详情页新增 `📷 传张照片，让小辽写文案`（`openNodePhoto()`，payload 带 `nodeId`）与 `📖 讲讲这一站的故事`（`openNodeStory()`，`saveStory({nodeId,...})`），并新增「📖 这一站的故事」区；顶部统计加「N 故事」。年代那边同名字段走 `eraId`。
- 实测（2026-09-11）：`工作/当兵` 站写故事正常（统计 0 视频 · 0 照片 · 2 故事）；`POST /api/v1/m4/moments/photo {photoUrl, description, nodeId:86}` 返回 3 条文案，照片落 nodeId=86（photoId=22）、故事落 nodeId=86（storyId=25，`stories?nodeId=86` 查得 3 条）；弹层副标题正确显示节点名。
