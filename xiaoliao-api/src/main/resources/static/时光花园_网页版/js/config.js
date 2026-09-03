/**
 * 时光花园 · 网页版 配置
 * ============================================================
 * 后端地址：先用本地（后端 dev 跑在 8080，鉴权默认关闭）。
 * 以后上隧道时，只改 BASE_URL 这一处即可，例如：
 *   BASE_URL: 'https://xiaoliao.natapp1.cc'
 * 注意：若后端在隧道后，后端 upload.base-url 也需是同一域名，
 *       否则照片 URL 仍会指向 localhost。
 * ============================================================
 */
var CONFIG = {
  BASE_URL: '',                              // 同域：页面由后端 8080 托管时本地/公网都生效
  // BASE_URL: 'https://xiaoliao.natapp1.cc', // 若页面由 python 8731 等其它端口托管时再改回

  TOKEN: '',                                 // 后端开启鉴权时填 JWT（现在留空即可）

  DS_API: 'https://api.deepseek.com/chat/completions',
  DS_MODEL: 'deepseek-chat',
  DS_DEMO_KEY: 'sk-24383836e4be472c814980a63b99c4fb', // 演示 Key，可用 hgs_ds_key 覆盖

  RAG_URL: 'https://xiaoliao.natapp1.cc/uploads/rag_index.json' // 知识库索引（失败自动兜底）
};
