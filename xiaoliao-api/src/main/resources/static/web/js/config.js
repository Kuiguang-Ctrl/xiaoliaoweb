/**
 * 时光花园 · 网页版 配置
 * ============================================================
 * 后端地址现在会自动探测，一般不用改：
 *   手填 BASE_URL（若有）→ 本页同域 → http://127.0.0.1:8080
 *   → http://localhost:8080 → https://xiaoliao.natapp1.cc
 * 只要后端在其中一个地址跑着，页面就会自动连上。
 * ============================================================
 */
var CONFIG = {
  BASE_URL: 'http://192.168.10.10:8080',      // 本机演示：电脑局域网 IP 的后端（手机/电脑都能连；IP 变了需改这里）
  AUTO_FALLBACK: true,                       // 自动在 本页同域 / 本地8080 / natapp 隧道 间查找后端

  TOKEN: '',                                 // 后端开启鉴权时填 JWT（现在留空即可）

  DS_API: 'https://api.deepseek.com/chat/completions',
  DS_MODEL: 'deepseek-chat',
  DS_DEMO_KEY: '',                          // 演示 Key 请自行填写，或用 hgs_ds_key 覆盖

  RAG_URL: 'https://xiaoliao.natapp1.cc/uploads/rag_index.json' // 知识库索引（失败自动兜底）
};
