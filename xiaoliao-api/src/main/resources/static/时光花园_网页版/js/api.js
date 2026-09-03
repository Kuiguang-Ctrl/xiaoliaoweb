/**
 * 后端接口封装 — 时光花园 · 网页版
 * 统一处理 Result{code,message,data}、鉴权头、超时与友好报错。
 * 用法：
 *   const nodes = await apiFetch('/api/v1/m4/timeline');
 *   await uploadImageFile(fileInput.files[0]);   // 返回 {url}
 */
(function (global) {
  'use strict';

  function apiFetch(url, opts) {
    opts = opts || {};
    var method = opts.method || 'GET';
    var headers = {};
    if (CONFIG.TOKEN) {
      headers['Authorization'] = 'Bearer ' + CONFIG.TOKEN;
    }
    var body;
    if (opts.form) {
      body = opts.form; // FormData，浏览器自动带 boundary
    } else if (opts.data !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(opts.data);
    }
    var ctrl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
    var timer = ctrl ? setTimeout(function () { ctrl.abort(); }, opts.timeoutMs || 20000) : null;

    return fetch(CONFIG.BASE_URL + url, {
      method: method,
      headers: headers,
      body: body,
      signal: ctrl ? ctrl.signal : undefined
    }).then(function (res) {
      return res.json().catch(function () { return null; });
    }).then(function (json) {
      if (json && json.code === 200) return json.data;
      var msg = (json && json.message) ? json.message : ('HTTP ' + (json ? json.code : '?'));
      throw new Error(msg);
    }).catch(function (err) {
      if (err && err.name === 'AbortError') {
        throw new Error('请求超时，后端没响应，请确认已启动');
      }
      if (err instanceof TypeError) {
        throw new Error('连不上后端（' + CONFIG.BASE_URL + '），请先启动后端（或改 config.js 的 BASE_URL 为隧道地址）');
      }
      throw err;
    }).finally(function () {
      if (timer) clearTimeout(timer);
    });
  }

  /** 把后端返回的相对/占位图片地址补全成可访问地址 */
  function absUrl(u) {
    if (!u) return '';
    // 后端联调时常把 localhost/127.0.0.1 写进图片 URL，这里统一改成同域，公网/本地都能显示
    u = u.replace(/^https?:\/\/localhost(:\d+)?/i, '').replace(/^https?:\/\/127\.0\.0\.1(:\d+)?/i, '').replace(/^https?:\/\/xiaoliao\.natapp1\.cc(:\d+)?/i, '');
    if (/^https?:\/\//i.test(u) || /^data:/i.test(u)) return u;
    if (u.charAt(0) === '/') return CONFIG.BASE_URL + u;
    return CONFIG.BASE_URL + '/' + u;
  }

  /** 上传一张图片（后端 /api/upload），返回 {url} */
  function uploadImageFile(file) {
    var fd = new FormData();
    fd.append('file', file);
    return apiFetch('/api/upload', { method: 'POST', form: fd, timeoutMs: 60000 });
  }

  /** M4 回忆与传承 — 对应后端 /api/v1/m4/* */
  var M4 = {
    timeline: function () { return apiFetch('/api/v1/m4/timeline'); },
    saveNode: function (d) { return apiFetch('/api/v1/m4/timeline/nodes', { method: 'POST', data: d }); },
    eras: function () { return apiFetch('/api/v1/m4/eras'); },
    saveEra: function (name) { return apiFetch('/api/v1/m4/eras', { method: 'POST', data: { name: name } }); },
    photos: function () { return apiFetch('/api/v1/m4/photos'); },
    savePhoto: function (d) { return apiFetch('/api/v1/m4/photos', { method: 'POST', data: d }); },
    stories: function (nodeId) {
      return apiFetch('/api/v1/m4/stories' + (nodeId ? '?nodeId=' + encodeURIComponent(nodeId) : ''));
    },
    saveStory: function (d) { return apiFetch('/api/v1/m4/stories', { method: 'POST', data: d }); },
    match: function (d) { return apiFetch('/api/v1/m4/match', { method: 'POST', data: d }); },
    moments: function () { return apiFetch('/api/v1/m4/moments'); },
    generateMoments: function (d) { return apiFetch('/api/v1/m4/moments/generate', { method: 'POST', data: d }); },
    selectMoment: function (id) { return apiFetch('/api/v1/m4/moments/' + id + '/select', { method: 'POST', data: {} }); },
    garden: function () { return apiFetch('/api/v1/m4/garden'); },
    shareHelp: function (nodeId) {
      return apiFetch('/api/v1/m4/timeline/nodes/' + nodeId + '/share-help', { method: 'POST', data: {} });
    }
  };

  /** 探测后端是否在线（/health 未开 CORS，改探测 /api/v1/m4/garden） */
  function pingBackend() {
    return apiFetch('/api/v1/m4/garden', { timeoutMs: 3000 }).then(function () { return true; }, function () { return false; });
  }

  global.apiFetch = apiFetch;
  global.absUrl = absUrl;
  global.uploadImageFile = uploadImageFile;
  global.M4 = M4;
  global.pingBackend = pingBackend;
})(window);

