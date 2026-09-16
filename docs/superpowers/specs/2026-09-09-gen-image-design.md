# 「小辽画照片」——对话内文生图 设计文档

> 日期：2026-09-09 ｜ 范围：对话(chat.html) + 后端 /api/v1/m4/gen-image
> 定位：老人在对话里描述一段回忆场景，小辽生成一张"照片"（默认老照片写实风），满意可收进「我的时光/年代记忆」。
> 原则：跟视频作品同一种技术形态（前端对话触发 → 后端生成 → 落盘/落库 → 返回 URL 展示），但不做配乐/字幕/视频合成。
> 状态：后端已落地（编译打包通过）；前端已落地（chat.html 画照片全流程 + time.html 按节点/年代展示照片）；DashScope 直连验证同步出图成功；端到端联调待验收。

## 0. 一句话产品定义
老人用说话或打字描述"当年那个场景"，小辽把它变成一张可以保存的照片；不满意可以改描述重画。

## 1. 对话交互流程（chat.html 内）
1. 老人表达："帮我画一张年轻时在厂里上班的照片"（入口：对话普通消息识别 + 「🖼 让小辽画张照片」按钮兜底）。
2. 小辽用对话模型（DeepSeek/通用）向老人确认关键信息，信息不足时只问一轮：时间年代 / 地点 / 人物与关系 / 在做什么 / 天气或氛围（≤1~2 个问题，不啰嗦）。
3. 小辽把信息整理成画面提示词 prompt（≤500 字），自动追加风格尾缀（默认"老照片风格，略带岁月感；人物写实"；如果老人明确说彩色/现代/漫画，则尊重原话不追加）。
4. 前端调 POST /api/v1/m4/gen-image → 对话出现"🎨 小辽正在画…"等待态 → 出图（图片消息）。
5. 结果操作（图片下方按钮）：
   - ✅ 满意，保存 → 弹出放哪：人生时光某个节点 / 新建节点 / 年代记忆 → 保存成功提示；
   - 🔁 再画一张（同描述重生成）；
   - ✏️ 改一下 → 老人补充/修改描述 → 小辽更新 prompt 重画；
   - （可选）先看看不保存。
6. 失败态：后端超时/报错 → 对话提示"没画成，再试一次？"，可重试；不打断聊天。

## 2. 后端设计
### 2.1 生成器接口（可插拔，对标 M4VideoComposer 职责单一风格）
```java
public interface ImageGenerator {
    /** @return 生成的图片 URL（已持久化到 uploads 下） */
    String generate(String prompt, String style, String userId);
}
```
- 首个实现：`DashScopeQwenImageGenerator`（阿里云百炼 qwen-image，HTTP 直连，风格与 `DashScopeAsrService` 完全一致：hutool HttpRequest + Bearer key）。
- 以后可换即梦/可灵：新增实现 + 配置切换，前端无感。

### 2.2 配置（application.yml，dev 用 application-dev.yml）
```yaml
xiaoliao:
  image:
    api-key: ${QWEN_API_KEY:}        # 与 ASR/AI 引擎共用阿里云百炼 Key
    model: ${IMAGE_MODEL:qwen-image}
    http-url: ${IMAGE_HTTP_URL:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}
    timeout: 60s
    size: "1280*720"                  # 或厂商支持比例，联调定
```
> 说明：QWEN_API_KEY 已存在（ASR 在用），不新增密钥体系；正式上线用环境变量注入。

### 2.3 接口
`POST /api/v1/m4/gen-image`（application/json，用户身份走 X-Mock-User-Id 头，与现有 m4 接口一致）
```json
// 入参
{ "prompt": "1985年，国营棉纺厂车间，一位30岁女工在织布机前…", "style": "old_photo" }
// 出参 code=200
{ "data": { "photoId": 12345, "url": "/uploads/images/202609/abc/video…png" } }
```
- 校验：prompt 必填 ≤500 字；style 白名单：old_photo(默认)/color/sketch，空=old_photo 之外由小辽在 prompt 描述即可（保留 style 字段以便前端/对话传"保持彩色"等）。
- 流程：校验 → 调厂商接口（≤60s 超时）→ 下载/保存成图到 `uploads/images/<yyyyMM>/<uuid>.png` → 写 m4_photo（type=gen 或沿用 user 字段语义 + 新增来源标记，见 2.4）→ 返回。
- 失败：统一 BusinessException 500 + 人类可读 message（对话可直接展示）；日志 + @ApiMetric 记录。

### 2.4 数据落库
- 复用现有 `m4_photo` 表（M4Photo 实体已有软删/归属），新增区分来源：
  - 简单方案：photo 字段/描述里存 `gen:<prompt摘要>`，或复用现有 user 枚举加 `gen`（user=老人上传 / stock=示意图 / gen=小辽生成）。
  - 不新建表；时间线/照片列表接口按现有规则即可展示（time 页照片流已存在）。
- 归属：userId（X-Mock-User-Id 隔离）；node_id/era_id 为空=先放"我的照片"；保存时若选了位置再补挂。

## 3. 前端改动（chat.html）
1. 消息类型扩展：新增"图片生成"消息（状态：generating→done→error），出图即图片消息 + 操作按钮行。
2. 新函数（命名沿用小写前缀风格）：
   - `aiGenOpen()`/`aiGenFromText()`：识别"帮我画/画一张"意图时接管；
   - `aiGenSubmit(prompt)`：调 `apiFetch('/api/v1/m4/gen-image',{method:'POST',data:{prompt,style}})`；
   - `aiGenButtons(photoId,url)`：满意保存/再画/改描述；
   - `aiGenSave(url)`：弹存放位置（复用 goWorks 的位置选择逻辑），POST 到现有照片保存接口（m4/photos 或新增的带 gen 来源字段）。
3. 等待态 UI 与语音条风格一致；失败 toast 可重试。
4. 不动 vcJob/视频流程、不动配乐。

### 3.1 前端落地说明（2026-09-09）
- chat.html：新增「🖼 画照片」快捷入口；自由聊天识别"画/生成…照片"意图；对话引导描述画面 → 调 gen-image → 对话内图片卡（点图可放大）+ 按钮（收进年代记忆 / 收进人生时光 / 再画一张 / 改一改再画 / 先不要）。
- 收进年代记忆：问年份/年代 → 转"八十年代"式名称 → 已有 era 直接挂 eraId，没有则 POST /eras 新建再挂。
- 收进人生时光：GET /timeline 列出现有节点供选择；没有合适的可"另起一段"（POST /timeline/nodes 自定义节点）再挂 nodeId。
- 保存：POST /api/v1/m4/photos {url, source:"gen", nodeId 或 eraId(+eraTag)}，与"我的时光"同一数据源。
- time.html（time.js）：节点详情/年代详情新增"🖼 这站的照片/这个年代的照片"缩略区（点击放大），列表卡显示"N 视频 · M 照片"；详情页新增"🖼 去和小辽画张照片"按钮（跳 chat.html?do=photo 直接开画）。

## 4. 非目标（本期不做）
- 生成图自动进视频素材/带字幕配乐的作品；
- 批量/多图生成、放大修复、局部重绘、参考图；
- 广场分享卡片；
- 生产容器/队列/横向扩展；
- 图片鉴黄/合规审核（生成方负责，提示词侧可加基础关键词拦截）。

## 5. 验收标准
1. 对话说"帮我画一张…"能出图；不满意改描述能重画；
2. 保存后「我的时光」对应位置能看到该照片（多测试用户隔离正常）；
3. 后端没配 Key 时给明确报错且对话可重试；Key 无效时错误信息可读；
4. 接口调用时长 ≤60s，图片可访问（本地 8080 与 natapp 都能显示，走 absUrl 兼容）。

## 6. 落地顺序
1. 后端：ImageGenerator + DashScope 实现 + gen-image 接口 + m4_photo 来源标记 → curl 联调出图；
2. 前端：chat.html 消息类型 + 按钮 + 保存位置；
3. 端到端验收（对话出图→保存→我的时光可见）；
4. 更新 README/使用说明；按 TAPD 约定同步（M4 下子需求/任务：生图后端接口、对话画照片、示意图替换可后置）。
