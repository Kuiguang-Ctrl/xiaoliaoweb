# 视频作品前端（素材上传 + 我的时光视频流）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对话端（chat.html）保存视频时改为向后端 `/api/v1/m4/videos` 上传素材包（照片+字幕+标题+文案+离线渲染的配乐音频）；「我的时光」（time.html/time.js）改为视频流展示（封面+标题，点开播放+文案），移除讲故事/传照片/年代对照等旧入口。

**Architecture:** 依赖后端计划 `2026-09-06-video-works-backend.md`（已完成 `/videos` 接口与 `videoCount` 字段）。前端复用 `web/js/api.js` 的 `apiFetch`（自动探测后端 + X-Mock-User-Id）。BGM 用 `OfflineAudioContext` 把与预览一致的旋律渲染成 wav 文件上传。time.js 整体重写为自足文件（旧 story/photo/match/moment/ASR 录音代码全量移除）。

**Tech Stack:** 原生 JS（chat.html 内嵌 + web/js/*.js）、后端探测（config.js/api.js）、ffmpeg 后端合成（本计划不涉及）

**关键路径（相对仓库根 `xiaoliaoweb-src-2026-09-06/`）**
- 改：`xiaoliao-api/src/main/resources/static/chat.html`
- 改：`xiaoliao-api/src/main/resources/static/web/js/api.js`
- 重写：`xiaoliao-api/src/main/resources/static/web/js/time.js`
- 改：`xiaoliao-api/src/main/resources/static/web/time.html`
- 改：`xiaoliao-api/src/main/resources/static/web/js/garden.js`
- 改：`xiaoliao-api/src/main/resources/static/web/css/style.css`
- 验证：后端须已运行（Plan A Task A7 通过），静态页面由 `http://127.0.0.1:8731`（或局域网 IP）提供

---

### Task B0: api.js 增加 videos 封装

**Files:**
- Modify: `xiaoliao-api/src/main/resources/static/web/js/api.js`

- [ ] **Step 1: 在 api.js 的 M4 对象里加三个方法**（在 `shareHelp` 行之后、`};` 之前插入）

```js
    videos: function (nodeId, eraId) {
      var q = [];
      if (nodeId) q.push('nodeId=' + encodeURIComponent(nodeId));
      if (eraId) q.push('eraId=' + encodeURIComponent(eraId));
      return apiFetch('/api/v1/m4/videos' + (q.length ? '?' + q.join('&') : ''));
    },
    getVideo: function (id) { return apiFetch('/api/v1/m4/videos/' + id); },
    deleteVideo: function (id) { return apiFetch('/api/v1/m4/videos/' + id, { method: 'DELETE' }); },
    saveVideo: function (fd) { return apiFetch('/api/v1/m4/videos', { method: 'POST', form: fd, timeoutMs: 120000 }); }
```

注意 `apiFetch` 的默认超时是 20s，视频合成可能 10–30s，这里 `saveVideo` 用 `timeoutMs: 120000`。

- [ ] **Step 2: 浏览器刷新验证无语法错误**（打开 http://127.0.0.1:8731/web/time.html，Console 无报错）

- [ ] **Step 3: Commit**

```bash
git add xiaoliao-api/src/main/resources/static/web/js/api.js
git commit -m "feat(web): api.js M4.videos 封装"
```

---

### Task B1: chat.html —— 配乐离线渲染 + 素材包上传

**Files:**
- Modify: `xiaoliao-api/src/main/resources/static/chat.html`

目标：新增 `vcBgmRenderFile(seconds)`（把 `vcMusicPat()` 的旋律用 OfflineAudioContext 渲染成 wav Blob）；把 `vcCloudSync` 替换为上传素材包到 `/api/v1/m4/videos`。

- [ ] **Step 1: 新增配乐渲染函数**（放在 `vcMusicStop` 函数之后）

```js
/* 配乐离线渲染：把选中的旋律渲染成 wav Blob（与实时预览同一套 vcMusicPat 音色），供后端合成 mp4 用 */
function vcNoteAt(ctx, out, midi, when, dur, vol, type) {
  const o = ctx.createOscillator(), g = ctx.createGain();
  o.type = type || 'triangle';
  o.frequency.value = 440 * Math.pow(2, (midi - 69) / 12);
  g.gain.setValueAtTime(0, when);
  g.gain.linearRampToValueAtTime(vol, when + 0.04);
  g.gain.exponentialRampToValueAtTime(0.0001, when + dur);
  o.connect(g); g.connect(out);
  o.start(when); o.stop(when + dur + 0.08);
}
function vcBgmRenderFile(seconds) {
  return new Promise(function (resolve) {
    if (!vcJob || !vcJob.bgm) { resolve(null); return; }
    const OAC = window.OfflineAudioContext || window.webkitOfflineAudioContext;
    if (!OAC) { resolve(null); return; }
    const sec = Math.max(1, Math.ceil(seconds || 12));
    const ctx = new OAC(1, Math.ceil(sec * 44100) + 22050, 44100);
    try {
      const pat = vcMusicPat();            // 与预览同一套音符/速度/音量
      const master = ctx.createGain(); master.gain.value = 0.9; master.connect(ctx.destination);
      const notes = pat.notes, lows = pat.lows;
      let t = 0.06, beat = 0;
      while (t < sec) {
        vcNoteAt(ctx, master, notes[beat % notes.length], t, pat.step * 0.92, pat.vol, 'triangle');
        if (beat % 4 === 0) {
          const low = lows[(beat / 4) % lows.length | 0];
          vcNoteAt(ctx, master, low, t, pat.step * 3.5, pat.vol * 0.45, 'sine');
        }
        t += pat.step; beat++;
      }
      ctx.startRendering().then(function (buf) {
        resolve(vcBufferToWavBlob(buf));
      }, function () { resolve(null); });
    } catch (e) { resolve(null); }
  });
}
function vcBufferToWavBlob(buf) {
  try {
    const ch = Math.min(2, buf.numberOfChannels);
    const len = buf.length * ch * 2 + 44;
    const ab = new ArrayBuffer(len), dv = new DataView(ab);
    const ws = function (o, s) { for (let i = 0; i < s.length; i++) dv.setUint8(o + i, s.charCodeAt(i)); };
    ws(0, 'RIFF'); dv.setUint32(4, len - 8, true); ws(8, 'WAVE');
    ws(12, 'fmt '); dv.setUint32(16, 16, true); dv.setUint16(20, 1, true);
    dv.setUint16(22, ch, true); dv.setUint32(24, buf.sampleRate, true);
    dv.setUint32(28, buf.sampleRate * ch * 2, true);
    dv.setUint16(32, ch * 2, true); dv.setUint16(34, 16, true);
    ws(36, 'data'); dv.setUint32(40, len - 44, true);
    let off = 44;
    const data = ch === 1 ? buf.getChannelData(0) : buf.getChannelData(0);
    for (let i = 0; i < buf.length; i++) {
      let s = Math.max(-1, Math.min(1, data[i]));
      dv.setInt16(off, s < 0 ? s * 0x8000 : s * 0x7FFF, true); off += 2;
      if (ch === 2) {
        s = Math.max(-1, Math.min(1, buf.getChannelData(1)[i]));
        dv.setInt16(off, s < 0 ? s * 0x8000 : s * 0x7FFF, true); off += 2;
      }
    }
    return new Blob([ab], { type: 'audio/wav' });
  } catch (e) { return null; }
}
```

- [ ] **Step 2: 把 `vcCloudSync(rec)` 整函数替换为素材包版本**

原函数（注释“云端同步：把做好的作品写进后端…”下方 `function vcCloudSync(rec){ ... }` 整段）替换为：

```js
function vcCloudSync(rec) {
  // 作品 = 后端合成的视频：上传照片+每张字幕+标题+完整文案+离线渲染配乐 → /api/v1/m4/videos
  if (!rec || !rec.dest || !curUid) return Promise.reject(new Error('还没有选择存放位置'));
  const dest = rec.dest;
  const photos = rec.photos || [];
  let photosOk = 0;
  const fd = new FormData();
  fd.append('destKind', dest.kind === 'era' ? 'era' : 'life');
  if (dest.kind === 'era') fd.append('eraName', String(dest.name || '那段时光').slice(0, 32));
  else if (dest.nodeId) fd.append('nodeId', String(dest.nodeId));
  else fd.append('nodeLabel', String(dest.node || '自定义').slice(0, 32));
  fd.append('title', String(rec.title || '时光里的故事').slice(0, 64));
  fd.append('caption', String(rec.script || (rec.caps || []).join('') || '这是我们想好好收起来的一段回忆。').slice(0, 5000));
  photos.forEach(function (url) {
    if (!/^data:/.test(String(url || ''))) return;
    const f = vcDataUrlToFile(url, 'photo' + (photosOk + 1) + '.jpg');
    if (f) { fd.append('photos', f); photosOk++; }
  });
  if (!photosOk) return Promise.reject(new Error('没有可上传的照片'));
  const caps = rec.caps && rec.caps.length ? rec.caps : [];
  for (let i = 0; i < photosOk; i++) fd.append('captions', String(caps[i] || '').slice(0, 200));
  // 配乐：前端按预览音色离线渲染（后端秒级合成较长，渲染同样长度音频）
  return vcBgmRenderFile(photosOk * 3.5).then(function (bgmBlob) {
    if (bgmBlob) fd.append('bgm', bgmBlob, 'bgm.wav');
    return vcCloudApi('/api/v1/m4/videos', { method: 'POST', form: fd, timeoutMs: 120000 }).then(function () {
      return dest;
    });
  });
}
```

- [ ] **Step 3: vcCloudApi 增加 timeoutMs 透传**（检查现有 vcCloudApi 调用处）；把该函数改为支持超时参数：

原：
```js
function vcCloudApi(path,opt){
  opt=opt||{};
  try{
    if(typeof apiFetch!=='function') throw new Error('云端连接组件未加载，请从网页入口打开');
    return apiFetch(path,{method:opt.method||'GET',form:opt.form,data:opt.body,timeoutMs:opt.timeoutMs||30000});
  }catch(e){ return Promise.reject(e); }
}
```
若与上文一致则无需改动（已支持 timeoutMs）。确认 vcCloudApi 的实现形如上面代码（apiFetch 调用），否则改为它。

- [ ] **Step 4: 语法自检**

```bash
# 提取 chat.html 主 script 做语法检查
python3 - <<'PY'
import re
html = open('xiaoliao-api/src/main/resources/static/chat.html', encoding='utf-8').read()
scripts = re.findall(r'<script>([\s\S]*?)</script>', html)
open('/tmp/chat_main.js', 'w', encoding='utf-8').write(scripts[-1])
PY
node --check /tmp/chat_main.js && echo OK
```

Expected: OK

- [ ] **Step 5: Commit**

```bash
git add xiaoliao-api/src/main/resources/static/chat.html
git commit -m "feat(chat): 视频保存改传素材包到 /m4/videos（配乐离线渲染 wav）"
```

---

### Task B2: 重写 time.js 为视频流页面

**Files:**
- Rewrite: `xiaoliao-api/src/main/resources/static/web/js/time.js`

整体替换为以下文件（保留：tab 切换、人生时光节点列表/改名/删除/新增、年代列表/新建、请家人帮忙、回主页做视频；删除：story 故事/录音 ASR/照片上传/年代对照 match/朋友圈文案的全部代码与状态；计数/文案改用 videoCount/视频）。注意本文件完全自包含 `$`/`esc`/`toast`/`fmtDate` 帮助函数（不依赖 common.js 内部未导出的 `$`）。

```js
/**
 * 我的时光页（视频流版）— 人生时光（节点链）+ 年代记忆，直连后端 M4 接口
 * 内容单元 = 视频作品（m4_video）：列表显示封面+标题，点开播放并看文案。
 */
(function () {
  'use strict';

  /* ---------- 自足小工具（不依赖其它脚本内部实现） ---------- */
  function $(id) { return document.getElementById(id); }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function toast(msg, ms) {
    var t = $('toast');
    if (!t) { alert(msg); return; }
    t.textContent = msg;
    t.classList.add('show');
    clearTimeout(t._h);
    t._h = setTimeout(function () { t.classList.remove('show'); }, ms || 1800);
  }
  function fmtDate(iso) {
    if (!iso) return '';
    var d = new Date(iso);
    if (isNaN(d.getTime())) return String(iso);
    var p = function (n) { return (n < 10 ? '0' : '') + n; };
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
  }

  var NODE_ICON = { childhood: '👶', school: '📚', work: '🔧', marriage: '💍', parenting: '🍼', grandchildren: '👨‍👩‍👧', retirement: '🏠', custom: '✨' };
  var VIDEOS = [];       // 当前用户全部视频作品缓存（量小，列表/详情共用）
  var curNode = null;    // 当前打开的节点
  var curEra = null;     // 当前打开的年代
  var shareLink = '';

  /* ---------- 遮罩 / 详情层 ---------- */
  function showMask(id) { $(id).classList.add('show'); }
  function hideMask(id) { $(id).classList.remove('show'); }
  function layer() { return $('detailLayer'); }
  function openDetail(title) { $('detailTitle').textContent = title; layer().style.display = 'flex'; layer().scrollTop = 0; }
  function closeDetail() {
    layer().style.display = 'none';
    curNode = null; curEra = null;
    loadAll();
  }
  window.hideMask = hideMask;
  window.closeDetail = closeDetail;

  function errTip(e) { toast((e && e.message) || '操作失败，稍后再试'); }

  function yearLabel(n) {
    var a = n.yearFrom, b = n.yearTo;
    if (a && b && a !== b) return a + ' — ' + b;
    if (a) return String(a);
    if (b) return String(b);
    return '';
  }

  /* ---------- Tab ---------- */
  function switchTab(which) {
    Array.prototype.forEach.call($('segTop').children, function (d) { d.classList.toggle('on', d.dataset.tab === which); });
    $('viewLife').style.display = which === 'life' ? '' : 'none';
    $('viewEra').style.display = which === 'era' ? '' : 'none';
  }
  window.switchTab = switchTab;

  /* ---------- 加载 & 渲染列表 ---------- */
  function loadAll() {
    return Promise.all([M4.timeline(), M4.eras(), M4.videos()])
      .then(function (rs) {
        VIDEOS = rs[2] || [];
        renderLife(rs[0]);
        renderEras(rs[1]);
      })
      .catch(function (e) {
        $('lifeList').innerHTML = '<div class="empty-tip"><span class="et-emoji">📡</span>连不上后端：' + esc(e.message) + '</div>';
        $('eraList').innerHTML = '';
      });
  }

  function renderLife(nodes) {
    var box = $('lifeList');
    if (!nodes || !nodes.length) { box.innerHTML = '<div class="empty-tip"><span class="et-emoji">🌱</span>时光轴还空着，点下面加一段吧</div>'; return; }
    box.innerHTML = nodes.map(function (n) {
      var done = (n.videoCount || 0) > 0;
      var displayName = n.displayName || n.customName || '一段时光';
      return '<div class="tl-item" data-id="' + n.id + '">' +
        '<div class="tl-dot' + (done ? ' done' : '') + '"></div>' +
        '<div style="flex:1;min-width:0"><div class="tl-name' + (done ? ' done' : '') + '">' + (NODE_ICON[n.stage] || '✨') + ' ' + esc(displayName) + '</div>' +
        '<div class="tl-years">' + esc(yearLabel(n)) + '</div>' +
        '<div class="tl-acts"><button class="tl-btn" onclick="event.stopPropagation();renameNode(' + n.id + ')">✏️ 改名</button>' +
        '<button class="tl-btn danger" onclick="event.stopPropagation();deleteNodeAsk(' + n.id + ')">🗑 删除</button></div></div>' +
        '<span class="tl-count">' + (n.videoCount || 0) + ' 个视频</span></div>';
    }).join('');
    Array.prototype.forEach.call(box.querySelectorAll('.tl-item'), function (el) {
      el.onclick = function () { openNode(parseInt(el.dataset.id, 10)); };
    });
  }

  function renderEras(eras) {
    var box = $('eraList');
    if (!eras || !eras.length) { box.innerHTML = '<div class="empty-tip"><span class="et-emoji">🕰</span>还没有年代记忆，去和小辽新建一个吧。</div>'; return; }
    box.innerHTML = eras.map(function (e) {
      var first = (VIDEOS || []).filter(function (v) { return v.eraId === e.id; })[0];
      var ph = first && first.posterUrl
        ? '<img src="' + esc(absUrl(first.posterUrl)) + '" onerror="this.innerHTML=\'🕰\';this.onerror=null">'
        : '🕰';
      return '<div class="era-item" data-id="' + e.id + '">' +
        '<div class="ei-ph">' + ph + '</div>' +
        '<div style="flex:1"><div class="ei-name">' + esc(e.name) + '</div>' +
        '<div class="ei-date">' + (e.videoCount || 0) + ' 个视频</div></div>' +
        '<div class="nc-go">›</div></div>';
    }).join('');
    Array.prototype.forEach.call(box.querySelectorAll('.era-item'), function (el) {
      el.onclick = function () { openEra(parseInt(el.dataset.id, 10)); };
    });
  }

  /* ---------- 视频作品卡 ---------- */
  function videoItem(v) {
    return '<div class="wkv-item" data-id="' + v.id + '">' +
      '<div class="wkv-head" onclick="toggleVideo(' + v.id + ')">' +
      '<div class="wkv-ph">' + (v.posterUrl ? '<img src="' + esc(absUrl(v.posterUrl)) + '" onerror="this.style.display=\'none\';this.parentNode.textContent=\'🎞️\'">' : '🎞️') + '</div>' +
      '<div style="flex:1;min-width:0"><div class="wkv-title">' + esc(v.title || '一段回忆视频') + '</div>' +
      '<div class="wkv-meta">' + fmtDate(v.createTime) + (v.duration ? ' · 约 ' + v.duration + ' 秒' : '') + '</div></div>' +
      '<button class="tl-btn danger" onclick="event.stopPropagation();deleteVideoAsk(' + v.id + ')" style="align-self:center">🗑</button>' +
      '</div>' +
      '<div class="wkv-body" id="wkvBody' + v.id + '">' +
      '<video class="wkv-video" src="' + esc(absUrl(v.videoUrl)) + '" controls playsinline webkit-playsinline preload="metadata"></video>' +
      (v.caption ? '<div class="wkv-caption">' + esc(v.caption) + '</div>' : '') +
      '</div></div>';
  }
  function renderVideos(list, emptyText) {
    if (!list || !list.length) return '<div class="empty-tip">' + emptyText + '</div>';
    return list.map(videoItem).join('');
  }
  window.toggleVideo = function (id) {
    var b = $('wkvBody' + id);
    if (!b) return;
    var open = b.classList.toggle('open');
    if (open) { var v = b.querySelector('video'); if (v) { var p = v.play(); if (p && p.catch) p.catch(function () {}); } }
    else { var v2 = b.querySelector('video'); if (v2) v2.pause(); }
  };
  window.deleteVideoAsk = function (id) {
    if (!window.confirm('确定删除这条视频作品吗？删掉就找不回来了。')) return;
    M4.deleteVideo(id).then(function () {
      toast('已删除');
      VIDEOS = VIDEOS.filter(function (v) { return v.id !== id; });
      if (curNode) { openNode(curNode.id); return; }
      if (curEra) { openEra(curEra.id); return; }
      loadAll();
    }).catch(errTip);
  };

  /* ---------- 节点 / 年代详情（视频流） ---------- */
  function openNode(id) {
    M4.timeline().then(function (nodes) {
      var n = nodes.find(function (x) { return x.id === id; });
      if (!n) { toast('没找到这个节点'); return; }
      curNode = n; curEra = null;
      openDetail('人生时光 · ' + (n.displayName || n.customName || ''));
      var mine = (VIDEOS || []).filter(function (v) { return v.nodeId === id; });
      $('detailBody').innerHTML =
        '<div class="polish-card" style="margin-bottom:6px"><b>' + esc(n.displayName || n.customName || '') + '</b>' +
        ' <span class="muted">' + esc(yearLabel(n)) + '</span> · ' + (n.videoCount || 0) + ' 个视频</div>' +
        '<button class="btn btn-gold" onclick="goMakeVideo()">🎬 去和小辽做一段新视频</button>' +
        '<button class="btn btn-soft" onclick="openShare(' + id + ')">👨‍👩‍👧 请家人帮忙</button>' +
        '<div class="page-pad-title">🎞 这站的视频</div>' +
        renderVideos(mine, '这站还没有视频，点上面按钮去和小辽做一个吧');
    }).catch(errTip);
  }

  function openEra(id) {
    M4.eras().then(function (eras) {
      var e = eras.find(function (x) { return x.id === id; });
      if (!e) { toast('没找到这个年代'); return; }
      curEra = e; curNode = null;
      openDetail('年代记忆 · ' + e.name);
      var mine = (VIDEOS || []).filter(function (v) { return v.eraId === id; });
      $('detailBody').innerHTML =
        '<div class="polish-card" style="margin-bottom:6px"><b>' + esc(e.name) + '</b> <span class="muted">' + (e.videoCount || 0) + ' 个视频</span></div>' +
        '<button class="btn btn-gold" onclick="goMakeVideo()">🎬 去和小辽做一段新视频</button>' +
        '<div class="page-pad-title">🎞 这个年代的视频</div>' +
        renderVideos(mine, '这个年代还没有视频，点上面按钮去和小辽做一个吧');
    }).catch(errTip);
  }
  window.openNode = openNode;   // 供列表点击（列表已用 addEventListener，保留导出以便调试）
  window.openEra = openEra;

  /* ---------- 请家人帮忙 ---------- */
  function openShare(nodeId) {
    M4.shareHelp(nodeId).then(function (r) {
      shareLink = (window.CONFIG ? CONFIG.BASE_URL : '') + '/api/v1/m4/help/' + r.shareToken;
      $('shareBox').innerHTML =
        '<div class="pc-label">分享链接（7 天内有效）</div><div style="font-size:13px">' + esc(shareLink) + '</div>' +
        '<div class="hint" style="margin-top:8px">亲友点开可查看这段人生时光、补照片或提示（网页版亲友落地页后续提供）。</div>';
      showMask('shareMask');
    }).catch(errTip);
  }
  window.openShare = openShare;
  function copyShare() {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(shareLink).then(function () { toast('链接已复制'); }, function () { toast('复制失败，请手动复制'); });
    } else { toast('请手动复制链接'); }
  }
  window.copyShare = copyShare;

  /* ---------- 回主页做视频 ---------- */
  window.goMakeVideo = function () {
    try { sessionStorage.setItem('hgs_make_video_v1', '1'); } catch (e) {}
    location.href = '../chat.html?t=' + Date.now();
  };

  /* ---------- 节点改名 / 删除 / 新增 ---------- */
  function getNodeById(id) {
    return M4.timeline().then(function (nodes) {
      var n = (nodes || []).filter(function (x) { return x.id === id; })[0];
      if (!n) { toast('没找到这个节点，稍后刷新再看'); return null; }
      return n;
    });
  }
  window.renameNode = function (id) {
    getNodeById(id).then(function (n) {
      if (!n) return null;
      var cur = n.displayName || n.customName || '';
      var v = window.prompt('给「' + cur + '」起个新名字：', cur);
      if (v === null) return null;
      v = (v || '').trim();
      if (!v) { toast('名字不能为空'); return null; }
      return M4.updateNode(id, { stage: n.stage, customName: v, sortOrder: n.sortOrder, yearFrom: n.yearFrom, yearTo: n.yearTo });
    }).then(function (r) {
      if (r) { toast('改好了'); loadAll(); }
    }).catch(errTip);
  };
  window.deleteNodeAsk = function (id) {
    getNodeById(id).then(function (n) {
      if (!n) return;
      var name = n.displayName || n.customName || '这段时光';
      var cnt = n.videoCount || 0;
      var msg = cnt > 0
        ? '「' + name + '」里已有 ' + cnt + ' 个视频，是否真的要删除？\n删除后这些视频作品也会一并删除。'
        : '确定删除「' + name + '」吗？';
      if (!window.confirm(msg)) return;
      return M4.deleteNode(id).then(function () {
        toast('已删除');
        loadAll();
      });
    }).catch(errTip);
  };

  /* ---------- 添加节点 / 年代 ---------- */
  window.addNodeBtnHandler = function () { $('nodeName').value = ''; $('nodeFrom').value = ''; $('nodeTo').value = ''; showMask('nodeMask'); };
  window.addEraBtnHandler = function () {
    $('eraName').value = '';
    Array.prototype.forEach.call($('eraPresets').querySelectorAll('.opt-chip'), function (c) { c.classList.remove('on'); });
    showMask('eraMask');
  };
  function saveNewNode() {
    var name = ($('nodeName').value || '').trim();
    if (!name) { toast('给这段起个名字吧'); return; }
    var from = $('nodeFrom').value ? parseInt($('nodeFrom').value, 10) : null;
    var to = $('nodeTo').value ? parseInt($('nodeTo').value, 10) : null;
    M4.saveNode({ stage: 'custom', customName: name, sortOrder: 99, yearFrom: from, yearTo: to })
      .then(function () { hideMask('nodeMask'); toast('节点加好了'); return loadAll(); })
      .catch(errTip);
  }
  window.saveNewNode = saveNewNode;
  function saveNewEra() {
    var name = ($('eraName').value || '').trim();
    if (!name) { toast('给这段日子起个名吧'); return; }
    M4.saveEra(name).then(function (era) {
      hideMask('eraMask');
      toast('年代记下了');
      return loadAll().then(function () {
        if (era && era.id) switchTab('era');
      });
    }).catch(errTip);
  }
  window.saveNewEra = saveNewEra;

  /* ---------- 启动 ---------- */
  document.addEventListener('DOMContentLoaded', function () {
    var d = new Date();
    var p = function (n) { return (n < 10 ? '0' : '') + n; };
    $('sbTime').textContent = p(d.getHours()) + ':' + p(d.getMinutes());
    $('segTop').addEventListener('click', function (e) {
      var t = e.target.closest ? e.target.closest('[data-tab]') : null;
      if (t) switchTab(t.dataset.tab);
    });
    $('addNodeBtn').addEventListener('click', window.addNodeBtnHandler);
    Array.prototype.forEach.call($('eraPresets').querySelectorAll('.opt-chip'), function (c) {
      c.onclick = function () {
        Array.prototype.forEach.call($('eraPresets').querySelectorAll('.opt-chip'), function (o) { o.classList.remove('on'); });
        c.classList.add('on');
        $('eraName').value = c.dataset.v;
      };
    });
    Array.prototype.forEach.call(document.querySelectorAll('.modal-mask'), function (m) {
      m.addEventListener('click', function (e) { if (e.target === m) hideMask(m.id); });
    });
    loadAll();
  });
})();
```

- [ ] **Step 1: 整文件替换**（删除旧 time.js 全部内容，写入上文）

- [ ] **Step 2: 语法检查**

```bash
node --check xiaoliao-api/src/main/resources/static/web/js/time.js && echo OK
```

Expected: OK

- [ ] **Step 3: 浏览器打开 time 页**（http://127.0.0.1:8731/web/time.html）确认无 Console 报错、两 tab 可切换、节点显示“0 个视频”。

- [ ] **Step 4: Commit**

```bash
git add xiaoliao-api/src/main/resources/static/web/js/time.js
git commit -m "feat(web): time.js 重写为视频流（移除故事/照片/对照/文案入口）"
```

---

### Task B3: time.html 移除旧弹窗 + style.css 增加视频流样式 + garden.js 文案

**Files:**
- Modify: `xiaoliao-api/src/main/resources/static/web/time.html`
- Modify: `xiaoliao-api/src/main/resources/static/web/css/style.css`
- Modify: `xiaoliao-api/src/main/resources/static/web/js/garden.js`

- [ ] **Step 1: time.html 删除三个弹窗块**

删除 `<!-- 讲故事 -->`（id=`storyMask` 的 `<div class="modal-mask" id="storyMask">…</div>` 整块）、`<!-- 年代对照：找当年的示意图 -->`（id=`matchMask` 整块）、`<!-- 朋友圈文案 -->`（id=`momentMask` 整块）。保留 `nodeMask`、`eraMask`、`shareMask`、`detailLayer`、toast。

验证：`grep -c 'storyMask\|matchMask\|momentMask' web/time.html` 输出 0。

- [ ] **Step 2: style.css 追加视频流样式**（文件末尾追加）

```css
/* ===== 视频作品（time 页视频流） ===== */
.wkv-item { background:#FBF7EC; border:1px solid #E3D9C2; border-radius:14px; padding:8px; margin-bottom:12px; }
.wkv-head { display:flex; gap:10px; cursor:pointer; align-items:center; }
.wkv-ph { width:112px; height:76px; border-radius:8px; overflow:hidden; flex-shrink:0; background:#E9DFC8; display:flex; align-items:center; justify-content:center; font-size:26px; }
.wkv-ph img { width:100%; height:100%; object-fit:cover; display:block; }
.wkv-title { font-size:16px; font-weight:700; color:#6B4F3A; }
.wkv-meta { font-size:12px; color:#8A6F55; margin-top:3px; }
.wkv-body { display:none; margin-top:8px; }
.wkv-body.open { display:block; }
.wkv-video { width:100%; max-height:320px; border-radius:10px; background:#000; display:block; }
.wkv-caption { font-size:14px; line-height:1.7; color:#6B4F3A; background:#F5EFE0; border-radius:10px; padding:10px 12px; margin-top:8px; white-space:pre-wrap; }
```

- [ ] **Step 3: garden.js 文案与字段**

把 META 第一项 `{ key: 'storyCount', ... }` 改为 `{ key: 'videoCount', icon: '🎞️', label: '视频作品' }`（图标改为 🎞️，标签“视频作品”）；

把 `load()` 中开花公式 `(g.storyCount || 0) + ...` 改为 `(g.videoCount || 0) + ...`；副标题文案改为 `'开了 ' + content + ' 朵花 · 每做一段视频、收一张照片，花园就开一朵花'`。

- [ ] **Step 4: 语法检查 + 页面验证**

```bash
node --check xiaoliao-api/src/main/resources/static/web/js/garden.js && echo OK
```

浏览器打开 garden 页 http://127.0.0.1:8731/web/garden.html 无报错。

- [ ] **Step 5: Commit**

```bash
git add xiaoliao-api/src/main/resources/static/web/time.html \
        xiaoliao-api/src/main/resources/static/web/css/style.css \
        xiaoliao-api/src/main/resources/static/web/js/garden.js
git commit -m "feat(web): 移除旧弹窗，视频流样式，garden 改 videoCount"
```

---

### Task B4: 端到端验收（浏览器 + 手机）

**Files:** 无（手动验收清单）

前置：后端运行中（含 Plan A Task A7 通过）；静态服务器提供 chat/time 页（http://127.0.0.1:8731 或局域网 IP）。

- [ ] **Step 1: 电脑浏览器走通主流程**
  1. 打开 http://127.0.0.1:8731/chat.html，若未画像先答两句；进入“做视频”，选 2 张测试图，回答问题/跳过，用默认名字与配乐，收进「人生时光 · 出生与童年」→ 保存。
  2. 观察：保存后应出现“✔ 已同步”toast（不是“云端同步没成功”）；约 10–30s 合成。
  3. 点「看作品」→ time 页出生与童年下出现视频卡（封面+标题），点开能播放且看到文案。
  4. 回 chat.html 再做一条收进「年代记忆」（自动建年代）→ time 页年代 tab 可见。

- [ ] **Step 2: 手机微信验证**
  1. 微信里打开 http://192.168.10.10:8731/chat.html（若 IP 变了用新局域网 IP 并把 config.js BASE_URL 同步更新后重新加载）。
  2. 做一条视频收进节点/年代，确认保存成功；「我的时光」可看到并播放视频（mp4 在 iOS/安卓微信内均应可播）。

- [ ] **Step 3: 边界检查**
  - 未选照片点保存 → 提示“没有可上传的照片/请先传照片”。
  - 后端停掉后保存 → 提示云端同步失败且作品仍在本机（hgs_works_*）。
  - 删除节点/年代前有视频 → 确认文案提到视频会一并删除。
  - 换一个测试用户 → 看不到上一用户的视频（隔离）。

- [ ] **Step 4: 清理验收产生的数据**（如不需要保留演示数据，删除 e2e/临时用户及其视频）

```bash
# 在页面右上角测试用户管理删除演示用户，或：
docker exec -i xiaoliao-pg bash -c "PGPASSWORD=xiaoliao_dev psql -U xiaoliao -d xiaoliao" <<'SQL'
-- 示例：清某个 uid（替换 <uid>）
DELETE FROM m4_video WHERE user_id='<uid>';
DELETE FROM m4_era WHERE user_id='<uid>';
DELETE FROM m4_timeline_node WHERE user_id='<uid>';
DELETE FROM users WHERE id='<uid>';
SQL
```
