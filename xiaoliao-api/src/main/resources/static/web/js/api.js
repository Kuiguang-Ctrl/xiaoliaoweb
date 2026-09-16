/**
 * 后端接口封装 — 时光花园 · 网页版
 * 自动探测可用后端：手填的 CONFIG.BASE_URL → 本页同域 → http://127.0.0.1:8080 → http://localhost:8080 → natapp 隧道。
 * 统一处理 Result{code,message,data}、鉴权头、超时与友好报错。
 * 用法：
 *   const nodes = await apiFetch('/api/v1/m4/timeline');
 *   await uploadImageFile(fileInput.files[0]);   // 返回 {url}
 */
(function (global) {
  'use strict';

  /* ---------- 后端地址自动探测 ---------- */
  var baseCache = null;   // 已探测成功的地址；null=还没探完
  var baseProbe = null;   // 探测中的 promise（避免并发重复探测）
  var triedBases = [];    // 全部失败时用于报错提示

  function candidateBases() {
    var list = [];
    if (CONFIG.BASE_URL) list.push(CONFIG.BASE_URL);        // 1) 手填地址优先（例如隧道）
    list.push('');                                          // 2) 同域：页面由后端 8080 托管时最省事
    if (CONFIG.AUTO_FALLBACK !== false) {
      ['http://127.0.0.1:8080', 'http://localhost:8080', 'https://xiaoliao.natapp1.cc'].forEach(function (b) {
        if (list.indexOf(b) < 0) list.push(b);
      });
    }
    return list;
  }
  function displayBase(b) { return b === '' ? '本页地址(同域)' : b; }
  function probeOk(base) {
    var ctrl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
    var timer = ctrl ? setTimeout(function () { ctrl.abort(); }, 2200) : null;
    return fetch(base + '/api/v1/m4/garden', {
      method: 'GET',
      signal: ctrl ? ctrl.signal : undefined
    }).then(function (r) { return r.json().catch(function () { return null; }); })
      .then(function (json) { return !!(json && json.code === 200); })
      .catch(function () { return false; })
      .finally(function () { if (timer) clearTimeout(timer); });
  }
  function ensureBase() {
    if (baseCache !== null) return Promise.resolve(baseCache);
    if (baseProbe) return baseProbe;
    baseProbe = candidateBases().reduce(function (chain, b) {
      return chain.then(function (found) {
        // 同域后端用空串表示，必须用 typeof 判断，否则空串会被当成“还没找到”，
        // 继续往下探测到本机 8080 上的另一份后端（可能是旧版本）
        if (typeof found === 'string') return found;
        return probeOk(b).then(function (ok) {
          if (!ok) triedBases.push(displayBase(b));
          return ok ? b : null;
        });
      });
    }, Promise.resolve(null)).then(function (found) {
      baseProbe = null;
      // 注意：同域后端用空串表示，必须用 typeof 判断，否则空串会被当成“没找到”
      if (typeof found === 'string') { baseCache = found; return found; }
      // 全都连不上：退回默认地址，让真实请求给出更明确的报错
      baseCache = CONFIG.BASE_URL || '';
      return baseCache;
    });
    return baseProbe;
  }
  function activeBase() { return baseCache !== null ? baseCache : (CONFIG.BASE_URL || ''); }

  /* ---------- 请求 ---------- */
  function apiFetch(url, opts) {
    opts = opts || {};
    var method = opts.method || 'GET';
    return ensureBase().then(function (base) {
      var headers = {};
      if (CONFIG.TOKEN) {
        headers['Authorization'] = 'Bearer ' + CONFIG.TOKEN;
      }
      // 多测试用户：把当前测试用户身份带给后端（后端鉴权关闭时按此头隔离数据）
      var demoUid = '';
      try { demoUid = localStorage.getItem('hgs_test_cur_v1') || ''; } catch (e) { demoUid = ''; }
      if (demoUid && /^[A-Za-z0-9_-]{1,64}$/.test(demoUid)) {
        headers['X-Mock-User-Id'] = demoUid;
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

      return fetch(base + url, {
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
          throw new Error('连不上后端（已试：' + (triedBases.length ? triedBases.join('、') : displayBase(base)) + '）。请先启动后端：若页面不在 8080 上打开，会依次尝试本地 8080 和 natapp 隧道。');
        }
        throw err;
      }).finally(function () {
        if (timer) clearTimeout(timer);
      });
    });
  }

  /** 把后端返回的相对/占位图片地址补全成可访问地址 */
  function absUrl(u) {
    if (!u) return '';
    // 后端联调时常把 localhost/127.0.0.1 写进图片 URL，这里统一改成当前可用后端，公网/本地都能显示
    u = u.replace(/^https?:\/\/localhost(:\d+)?/i, '').replace(/^https?:\/\/127\.0\.0\.1(:\d+)?/i, '').replace(/^https?:\/\/xiaoliao\.natapp1\.cc(:\d+)?/i, '');
    if (/^https?:\/\//i.test(u) || /^data:/i.test(u)) return u;
    if (u.charAt(0) === '/') return activeBase() + u;
    return activeBase() + '/' + u;
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
    updateNode: function (id, d) { return apiFetch('/api/v1/m4/timeline/nodes/' + id, { method: 'PUT', data: d }); },
    deleteNode: function (id) { return apiFetch('/api/v1/m4/timeline/nodes/' + id, { method: 'DELETE' }); },
    eras: function () { return apiFetch('/api/v1/m4/eras'); },
    saveEra: function (name) { return apiFetch('/api/v1/m4/eras', { method: 'POST', data: { name: name } }); },
    photos: function () { return apiFetch('/api/v1/m4/photos'); },
    savePhoto: function (d) { return apiFetch('/api/v1/m4/photos', { method: 'POST', data: d }); },
    stories: function (nodeId, eraId) {
      var q = [];
      if (nodeId) q.push('nodeId=' + encodeURIComponent(nodeId));
      if (eraId) q.push('eraId=' + encodeURIComponent(eraId));
      return apiFetch('/api/v1/m4/stories' + (q.length ? '?' + q.join('&') : ''));
    },
    saveStory: function (d) { return apiFetch('/api/v1/m4/stories', { method: 'POST', data: d }); },
    deleteStory: function (id) { return apiFetch('/api/v1/m4/stories/' + id, { method: 'DELETE' }); },
    match: function (d) { return apiFetch('/api/v1/m4/match', { method: 'POST', data: d }); },
    moments: function () { return apiFetch('/api/v1/m4/moments'); },
    generateMoments: function (d) { return apiFetch('/api/v1/m4/moments/generate', { method: 'POST', data: d }); },
    /* 照片+老人描述 → 朋友圈文案（照片/故事/文案一起落库，其他页面共用这一个入口） */
    momentsFromPhoto: function (d) { return apiFetch('/api/v1/m4/moments/photo', { method: 'POST', data: d, timeoutMs: 60000 }); },
    selectMoment: function (id) { return apiFetch('/api/v1/m4/moments/' + id + '/select', { method: 'POST', data: {} }); },
    garden: function () { return apiFetch('/api/v1/m4/garden'); },
    shareHelp: function (nodeId) {
      return apiFetch('/api/v1/m4/timeline/nodes/' + nodeId + '/share-help', { method: 'POST', data: {} });
    },
    videos: function (nodeId, eraId) {
      var q = [];
      if (nodeId) q.push('nodeId=' + encodeURIComponent(nodeId));
      if (eraId) q.push('eraId=' + encodeURIComponent(eraId));
      return apiFetch('/api/v1/m4/videos' + (q.length ? '?' + q.join('&') : ''));
    },
    getVideo: function (id) { return apiFetch('/api/v1/m4/videos/' + id); },
    deleteVideo: function (id) { return apiFetch('/api/v1/m4/videos/' + id, { method: 'DELETE' }); },
    saveVideo: function (fd) { return apiFetch('/api/v1/m4/videos', { method: 'POST', form: fd, timeoutMs: 120000 }); },

    /* ---------- 广场 ---------- */
    plazaFeed: function (limit) {
      return apiFetch('/api/v1/m4/plaza/feed' + (limit ? '?limit=' + encodeURIComponent(limit) : ''));
    },
    /* 把自己的视频作品挂到广场（重复发布返回已有那条） */
    publishPlazaWork: function (d) { return apiFetch('/api/v1/m4/plaza/works', { method: 'POST', data: d }); },
    /* 从广场撤下（只撤展示，不影响「我的作品」） */
    unpublishPlazaWork: function (id) { return apiFetch('/api/v1/m4/plaza/works/' + id, { method: 'DELETE' }); },
    /* 点赞/取消点赞（toggle） */
    togglePlazaLike: function (id) { return apiFetch('/api/v1/m4/plaza/works/' + id + '/like', { method: 'POST', data: {} }); }
  };

  /** 探测后端是否在线（首探会走自动回退逻辑） */
  function pingBackend() {
    return apiFetch('/api/v1/m4/garden', { timeoutMs: 3000 }).then(function () { return true; }, function () { return false; });
  }

  global.apiFetch = apiFetch;
  global.absUrl = absUrl;
  global.uploadImageFile = uploadImageFile;
  global.M4 = M4;
  global.pingBackend = pingBackend;
  global.activeBase = activeBase;
})(window);
