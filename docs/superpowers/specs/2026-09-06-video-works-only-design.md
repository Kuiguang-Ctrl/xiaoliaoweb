# 作品 = 后端合成视频（标题 + 视频 + 文案）设计文档

> 日期：2026-09-06 ｜ 范围：`xiaoliaoweb-src-2026-09-06`
> 背景：现"回忆视频"作品在后端以 `m4_story`+封面照片+标题+文字+配乐字段存储，视频文件本身从未上传；「我的时光」只能显示成"故事卡+照片"。产品决策：**作品以视频文件为核心**。

## 0. 产品形态（已与需求方确认）

- 在对话（小辽）里制作的回忆视频，是"作品"的唯一形态。
- 每条作品保存：**标题 + 视频文件(mp4) + 文案** + 收进位置（人生时光节点 / 年代记忆）+ 封面帧（展示用）。
- 不再保存：封面照片、文字故事字段、配乐名等旧 `m4_story` 维度的元数据。
- 「我的时光」节点 / 年代的内容单元统一为视频：**取消手工"传照片 / 讲故事 / 找示意图对照卡"入口**，界面只展示视频流。
- 视频仍保留配乐音轨与逐句字幕（烧录进画面）。

## 1. 数据流

```
对话端 chat.html
  选照片(≤5, 有序) → 关键问答 → 文案/字幕生成 → 起名 → 选配乐(WebAudio 合成旋律) → 选收进位置
    ↓ 点“保存”
  ① 本地即时预览/播放（现状保留，vcJobFile/webm 本机回放）
  ② POST /api/v1/m4/videos   (multipart)
       photos[0..4]  有序照片(jpg,≤720px, 前端已压缩)
       captions[0..4] 每张照片对应的字幕文案
       title         视频标题
       caption       完整文案(作品详情展示)
       bgm           配乐音频文件（前端 OfflineAudioContext 渲染的 wav/mp3）
       nodeId | eraName(收进位置二选一，规则沿用现 works)
    ↓
后端
  素材落盘(uploads/yyyyMM/) → 调 ffmpeg 合成 720p mp4
    （每张照片 3.5s 转场 + 字幕烧录 + bgm 混音为音轨）
  → ffmpeg 抽首帧 poster.jpg → 写 m4_video → 返回 {videoId, videoUrl, posterUrl, duration}
    ↓
「我的时光」time 页
  节点/年代详情 = 视频流列表（poster 封面 + title）
  点开 → <video controls> 播放 mp4 + 展示完整 caption 文案
```

## 2. 数据模型

新增表 `m4_video`（Flyway **V10__m4_video.sql**），风格沿用现有实体（软删、时间戳）：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| user_id | varchar(64) | 与 users.id 一致（dev 用 X-Mock-User-Id） |
| node_id | bigint NULL | 人生时光节点（与 era_id 二选一） |
| era_id | bigint NULL | 年代记忆 |
| title | varchar(64) | 作品标题 |
| caption | text | 完整文案（作品详情页展示） |
| video_url | varchar(512) | /uploads/...mp4 |
| poster_url | varchar(512) | 首帧封面 |
| duration | int | 秒 |
| create_time / update_time | timestamp | |
| deleted | int | @TableLogic 软删 |

配套：`M4Video` 实体、`M4VideoMapper`、`VideoVO`、`VideoSaveRequest`（multipart 形式）。

timeline/eras 计数：把返回给前端展示的故事计数切换为 `m4_video` 计数（NodeVO/相应 VO 加 videoCount 语义）。

## 3. 后端接口（M4Controller / api/v1/m4）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/videos` | 素材包上传 → 合成 → 入库（multipart） |
| GET | `/videos?nodeId=&eraId=` | 按位置列视频（含 nodeId/eraId 均空 = 我的全部） |
| GET | `/videos/{id}` | 单条详情（含文案） |
| DELETE | `/videos/{id}` | 删除（软删，按 user_id 校验归属） |

- 视频合成器 `M4VideoService`：`ProcessBuilder` 调本机 ffmpeg，逐张照片生成带字幕片段 → concat → 混 bgm 音轨 → 抽首帧。含超时（合成≤60s 上限）与失败兜底（返回明确错误码）。
- 素材校验沿用现有风格：照片 1–5 张、类型/大小白名单、标题≤64、文案≤5000。
- 位置归属逻辑复用现 works 的规则（life：nodeId 精确挂载/不存在报错；era：按名找或自动建年代）。

## 4. 合成规格（默认值）

- 输出：`mp4`，H.264 + AAC，720p(1280x720) 或按最大照片尺寸自适应，兼容 iOS/安卓/微信。
- 每张照片展示 3.5s（fade 转场），≤5 张 → 最长约 18–20s。
- 字幕：烧录底部（白色文字 + 半透明黑底条），文案按每张照片的字幕文本逐张展示。
- 配乐：前端 `OfflineAudioContext` 渲染选中旋律为 wav/mp3（≤60s）上传；ffmpeg 混音为音轨（音量 ≤20% 主音量可配）。
- ffmpeg 来源：开发本机 `brew install ffmpeg`；Dockerfile.api 后续追加 ffmpeg 层（生产路径，本轮不做容器验证）。

## 5. 前端改动（chat.html）

- 保存流程（vcStore/vcCloudSync 改造）：改为组装素材包上传 `/api/v1/m4/videos`：
  - photos：现 vcJob.photos（dataURL，前端已压 ≤720px）转 File
  - captions：vcCaptions() 每照片字幕
  - title：vcJob.title；caption：vcJob.script/润色完整文案
  - bgm：选中配乐 → `vcRenderBgmFile()`（OfflineAudioContext 渲染为 wav blob，复用 vcMusicPat 的 notes/step/vol/低音逻辑，与预览音色一致）
- 成功后：作品落在后端节点/年代；仍保留本地 hgs_works_* 兜底与 vcAddToPlaza（广场后续再议）。
- 未选照片/合成不支持等错误，沿用现有 toast 提示文案风格。

## 6. 展示端改动（time.html / time.js / css）

- 节点详情 & 年代详情内容区重构为**视频流**：
  - 视频卡 = poster 封面图 + title，点击展开播放（`<video controls>`）+ 完整 caption。
  - 每张卡带删除按钮（确认后 DELETE）。
- 移除/停用旧内容入口与区块：传照片（pickPhoto）、讲故事（openStory）、对照卡/找示意图（openMatch 及其 era 按钮）、story 卡渲染与 story 相关操作、朋友圈文案按钮（momentForStory 若依附 story 则一并停用，页面不再显示）。
- 保留：节点改名/删除/新增自定义、年代新建/改名/删除、页面框架与后端探测（config.js/api.js）。
- 空态文案更新为视频语境（如"这站还没有视频，去和小辽做一个吧"）。

## 7. 停用与保留清单（后端）

- 保留不删（本轮页面不调用，稳定后再清理）：`/works`、`/stories`、`/photos`、`/match`、`/moments` 系列、m4_story/m4_photo/m4_stock_image 表及实体。
- garden 计数：从 story 计数改为 m4_video 计数（如适用；花园页 garden.js 同步适配）。
- 存量数据：当前数据库作品/故事数据为空，无需迁移。

## 8. 非目标（本轮不做）

- 视频分享卡/广场/家人帮忙与视频的结合。
- 后端生成"人生长图/家书"等衍生品。
- 生产容器 ffmpeg 镜像验证、横向扩展/队列。
- 旧接口与旧表物理清理。

## 9. 验收标准

1. 对话里做完视频 → 选收进节点/年代 → 保存：后端出现 m4_video 记录 + mp4/poster 文件，节点/年代计数 +1。
2. 「我的时光」节点/年代显示视频流：封面+标题；点开能播放、看到文案；可删除。
3. 手机（iOS/安卓微信）与电脑浏览器均能播放 mp4。
4. 多测试用户隔离仍然有效（X-Mock-User-Id）。
5. 页面不再出现讲故事/传照片/对照卡入口。
