# 语音输入（语音转文字 ASR）

## 现状
- 微信官方「微信同声传译」插件（WechatSI，appid `wx069ba97219f66d99`）已下架，新小程序无法添加。
- 本项目改用：**小程序原生录音 → 后端调用阿里云百炼 `qwen3-asr-flash`（HTTP 音频 base64 直传）转文字**，与聊天大模型共用同一把阿里云百炼 API Key，按音频时长单独计费（有免费额度）。

## 链路
```
老人按住 🎤 说话
  → uni.getRecorderManager() 录 mp3（16k/单声道）
  → 松手上传 POST /api/audio/asr（multipart file）
  → Java 把 mp3 以 base64 直传阿里云百炼 qwen3-asr-flash（HTTP，无需转码）
  → 返回识别文字 → 前端 sendText(text) 走原聊天链路
```

## 涉及文件
- 后端：`xiaoliao-api/src/main/java/com/xiaoliao/api/audio/`（AudioController / DashScopeAsrService）
- 前端：`pages/chat/chat.vue`（录音上传）、`api/chat.js`（`audioToText`）
- 配置：`xiaoliao.asr.*`（application.yml），Key 读取 `QWEN_API_KEY` 环境变量；dev 环境在 application-dev.yml 内置

## 配置项
| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `xiaoliao.asr.api-key` | `${QWEN_API_KEY:}` | 阿里云百炼 API Key |
| `xiaoliao.asr.model` | `qwen3-asr-flash` | 语音识别模型 |
| `xiaoliao.asr.http-url` | `https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation` | 识别 HTTP 地址 |
| `xiaoliao.asr.timeout` | `90s` | 单次识别超时 |

## 注意事项
- 阿里云百炼账户欠费时接口返回 `Arrearage`，需先充值/缴清欠款。
- mp3 直接 base64 上传识别，无需转码；无有效语音时返回空串，前端提示重新说。
- 老模型 `paraformer-realtime-v2` 已从百炼下线，新账号模型列表里不再提供（可用 `qwen3-asr-flash` / `fun-asr-flash` 等）。
