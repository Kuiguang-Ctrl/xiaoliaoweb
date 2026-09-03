/**
 * 我的时光页 — 人生时光（节点链）+ 年代记忆，直连后端 M4 接口
 * 所有请求失败都会 toast 后端原因，页面保持可用。
 */
(function () {
  'use strict';

  var NODE_ICON = { childhood: '👶', school: '📚', work: '🔧', marriage: '💍', parenting: '🍼', grandchildren: '👨‍👩‍👧', retirement: '🏠', custom: '✨' };
  var curNode = null;     // 当前打开的节点
  var curEra = null;      // 当前打开的年代
  var curStoryCtx = null; // storyMask 归属 {type:'node'|'era', id}
  var curMatchCtx = null; // matchMask 归属 {eraId, photoUrl, photoId}
  var matchFileEl = null;
  var shareLink = '';
  var curMomentText = ''; // 当前选中的朋友圈文案（复制用）
  var storyRec = null; // 讲故事弹窗的录音状态

  /* ---------------- 基础 ---------------- */
  function showMask(id) { $(id).classList.add('show'); }
  function hideMask(id) { $(id).classList.remove('show'); }
  function layer() { return $('detailLayer'); }
  function openDetail(title) { $('detailTitle').textContent = title; layer().style.display = 'flex'; layer().scrollTop = 0; }
  function closeDetail() {
    layer().style.display = 'none';
    curNode = null; curEra = null;
    loadAll(); // 回列表刷新计数
  }
  window.hideMask = hideMask;
  window.closeDetail = closeDetail;

  function errTip(e) { toast((e && e.message) || '操作失败，稍后再试'); }
  function busy(btn, on) {
    if (!btn) return;
    if (on) { btn._old = btn.textContent; btn.disabled = true; btn.textContent = '处理中…'; }
    else { btn.disabled = false; btn.textContent = btn._old || btn.textContent; }
  }

  /* ---------------- 时间/年份文案 ---------------- */
  function yearLabel(n) {
    if (!n) return '';
    return n.yearFrom && n.yearTo ? (n.yearFrom + ' — ' + n.yearTo) : (n.yearFrom || n.yearTo || '');
  }

  /* ---------------- Tab ---------------- */
  function switchTab(which) {
    Array.prototype.forEach.call($('segTop').children, function (d) { d.classList.toggle('on', d.dataset.tab === which); });
    $('viewLife').style.display = which === 'life' ? '' : 'none';
    $('viewEra').style.display = which === 'era' ? '' : 'none';
    location.hash = which;
  }
  window.switchTab = switchTab;

  /* ---------------- 列表加载 ---------------- */
  function loadAll() {
    var tab = (location.hash || '#life').replace('#', '');
    switchTab(tab === 'era' ? 'era' : 'life');
    return Promise.all([M4.timeline(), M4.eras(), M4.photos()])
      .then(function (rs) { PHOTOS = rs[2] || []; renderLife(rs[0]); renderEras(rs[1]); })
      .catch(function (e) {
        $('lifeList').innerHTML = '<div class="empty-tip"><span class="et-emoji">📡</span>连不上后端：' + esc(e.message) + '</div>';
        $('eraList').innerHTML = '';
      });
  }
  var PHOTOS = [];
  function renderLife(nodes) {
    var box = $('lifeList');
    if (!nodes || !nodes.length) { box.innerHTML = '<div class="empty-tip"><span class="et-emoji">🌱</span>时光轴还空着，点上面加一段吧</div>'; return; }
    box.innerHTML = nodes.map(function (n) {
      var done = (n.storyCount || 0) > 0;
      return '<div class="tl-item" data-id="' + n.id + '">' +
        '<div class="tl-dot' + (done ? ' done' : '') + '"></div>' +
        '<div style="flex:1"><div class="tl-name' + (done ? ' done' : '') + '">' + (NODE_ICON[n.stage] || '✨') + ' ' + esc(n.displayName || n.customName || '一段时光') + '</div>' +
        '<div class="tl-years">' + esc(yearLabel(n)) + '</div></div>' +
        '<span class="tl-count">' + (n.storyCount || 0) + ' 个故事</span></div>';
    }).join('');
    Array.prototype.forEach.call(box.querySelectorAll('.tl-item'), function (el) {
      el.onclick = function () { openNode(parseInt(el.dataset.id, 10)); };
    });
  }
  function renderEras(eras) {
    var box = $('eraList');
    if (!eras || !eras.length) { box.innerHTML = '<div class="empty-tip"><span class="et-emoji">🕰</span>还没有年代记忆，点上面新建一个吧</div>'; return; }
    box.innerHTML = eras.map(function (e) {
      var cover = (PHOTOS || []).find(function (p) { return p.id === e.coverPhotoId; });
      var ph = cover ? '<img src="' + esc(absUrl(cover.url)) + '" onerror="this.parentNode.innerHTML=\'🕰\'">' : '🕰';
      return '<div class="era-item" data-id="' + e.id + '">' +
        '<div class="ei-ph">' + ph + '</div>' +
        '<div style="flex:1"><div class="ei-name">' + esc(e.name) + '</div>' +
        '<div class="ei-date">' + (e.storyCount || 0) + ' 个故事</div></div>' +
        '<div class="nc-go">›</div></div>';
    }).join('');
    Array.prototype.forEach.call(box.querySelectorAll('.era-item'), function (el) {
      el.onclick = function () { openEra(parseInt(el.dataset.id, 10)); };
    });
  }

  /* ================= 人生时光：节点详情 ================= */
  function openNode(id) {
    M4.stories(id).then(function (stories) {
      M4.timeline().then(function (nodes) {
        var n = nodes.find(function (x) { return x.id === id; });
        if (!n) { toast('没找到这个节点'); return; }
        curNode = n;
        openDetail('人生时光 · ' + (n.displayName || n.customName || ''));
        var mine = (PHOTOS || []).filter(function (p) { return p.nodeId === id; });
        var imgs = mine.length ? mine.map(function (p) {
          return '<div style="position:relative"><img class="photo-thumb" src="' + esc(absUrl(p.url)) + '" onerror="this.style.display=\'none\'">' +
            (p.isIllustration ? '<div class="tag-badge" style="position:absolute;left:2px;bottom:2px">示意图</div>' : '') + '</div>';
        }).join('') : '';
        var storyHtml = (stories || []).length ? (stories || []).map(function (s) {
          return storyCard(s);
        }).join('') : '<div class="empty-tip">这站还没讲故事，点下面的按钮讲一段吧</div>';
        $('detailBody').innerHTML =
          '<div class="polish-card" style="margin-bottom:4px"><b>' + esc(n.displayName || n.customName || '') + '</b>' +
          ' <span class="muted">' + esc(yearLabel(n)) + '</span> · ' + (n.storyCount || 0) + ' 个故事</div>' +
          '<button class="btn btn-gold" onclick="pickPhoto(\'node\',' + n.id + ')">📷 传一张那时的照片</button>' +
          '<button class="btn btn-primary" onclick="openStory(\'node\',' + n.id + ')">✍️ 讲讲这站的故事</button>' +
          '<button class="btn btn-soft" onclick="openShare(' + n.id + ')">👨‍👩‍👧 请家人帮忙</button>' +
          '<div class="page-pad-title">🖼 这站的照片</div><div class="photo-grid">' + (imgs || '<div class="muted" style="font-size:12.5px">还没有照片</div>') + '</div>' +
          '<div class="page-pad-title">📖 这站的故事</div>' + storyHtml;
        afterDetailRender();
      }).catch(errTip);
    }).catch(errTip);
  }
  function storyCard(s) {
    return '<div class="story-card">' +
      '<div class="sc-date">' + fmtDate(s.createTime) + (s.mood ? ' · 情绪 ' + esc(s.mood) : '') + '</div>' +
      '<div class="sc-txt">' + esc(s.originalText || '') + '</div>' +
      (s.polishedText ? '<div class="sc-polish">✍️ 润色：' + esc(s.polishedText) + '</div>' : '') +
      '<button class="btn-mini gold" onclick="momentForStory(' + s.id + ')">💌 写条朋友圈文案</button></div>';
  }
  function afterDetailRender() { /* 预留 */ }

  /* ================= 年代记忆：详情 ================= */
  function openEra(id) {
    M4.stories().then(function (all) {
      M4.eras().then(function (eras) {
        var e = eras.find(function (x) { return x.id === id; });
        if (!e) { toast('没找到这个年代'); return; }
        curEra = e;
        openDetail('年代记忆 · ' + e.name);
        var stories = (all || []).filter(function (s) { return s.eraId === id; });
        var mine = (PHOTOS || []).filter(function (p) { return p.eraId === id; });
        var imgs = mine.length ? mine.map(function (p) {
          return '<div style="position:relative"><img class="photo-thumb" src="' + esc(absUrl(p.url)) + '" onerror="this.style.display=\'none\'">' +
            '<div class="tag-badge" style="position:absolute;left:2px;bottom:2px">' + (p.isIllustration ? '示意图' : '现在的') + '</div></div>';
        }).join('') : '';
        $('detailBody').innerHTML =
          '<div class="polish-card" style="margin-bottom:4px"><b>' + esc(e.name) + '</b> <span class="muted">' + (e.storyCount || 0) + ' 个故事</span></div>' +
          '<button class="btn btn-gold" onclick="pickPhoto(\'era\',' + e.id + ')">📷 拍一张现在的照片</button>' +
          '<button class="btn btn-primary" onclick="openMatch(' + e.id + ')">🖼 找当年的示意图（对照卡）</button>' +
          '<button class="btn btn-soft" onclick="openStory(\'era\',' + e.id + ')">✍️ 讲讲那时候的事</button>' +
          '<div class="page-pad-title">🖼 这个年代的照片</div><div class="photo-grid">' + (imgs || '<div class="muted" style="font-size:12.5px">还没有照片</div>') + '</div>' +
          '<div class="page-pad-title">📖 这个年代的故事</div>' +
          ((stories.length ? stories.map(function (s) { return storyCard(s); }).join('') : '<div class="empty-tip">还没讲过这个年代的事</div>'));
        afterDetailRender();
      }).catch(errTip);
    }).catch(errTip);
  }

  /* ================= 传照片 ================= */
  function pickPhoto(type, id) {
    var inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = function () {
      var f = inp.files && inp.files[0];
      if (!f) return;
      busyBtn('upload');
      uploadImageFile(f).then(function (r) {
        var body = { url: r.url, source: 'user', isIllustration: 0 };
        if (type === 'node') body.nodeId = id; else body.eraId = id;
        return M4.savePhoto(body);
      }).then(function () {
        return M4.photos().then(function (ps) { PHOTOS = ps || []; });
      }).then(function () {
        toast('收到，照片收好啦 😊');
        if (type === 'node') openNode(id); else openEra(id);
      }).catch(function (e) { toast(e.message || '上传失败'); }).then(function () { busyBtn('upload'); });
    };
    inp.click();
  }
  window.pickPhoto = pickPhoto;
  function busyBtn() { /* noop 占位 */ }

  /* ================= 讲故事 ================= */
  function openStory(type, id) {
    curStoryCtx = { type: type, id: id };
    $('storyTitle').textContent = type === 'node' ? '✍️ 讲讲「这站」的故事' : '✍️ 讲讲「那时候」的事';
    $('storyText').value = '';
    $('storyPolish').checked = true;
    if (storyRec) stopStoryMic(false);
    setMicLabel('🎙 点击说话');
    showMask('storyMask');
    setTimeout(function () { $('storyText').focus(); }, 100);
  }
  window.openStory = openStory;

  /* ================= 讲故事 · 录音转文字（点击说话 → /api/audio/asr → 填入输入框） ================= */
  var storyRec = null;
  var storySilentMs = 1500;   // 说完停口多久自动结束
  var storyNoise = 0.012;     // 声音阈值，低于它算"没在说话"
  function setMicLabel(txt) { var b = $('storyMicBtn'); if (b) b.textContent = txt; }
  function micDown() {        // 按下 / 点下：开始录音
    if (storyRec) { stopStoryMic(true); return false; }
    startStoryMic();
    return false;
  }
  function micUp() {          // 松开：结束并识别
    if (storyRec) stopStoryMic(true);
    return false;
  }
  function micCancel() {      // 滑出/取消：只结束不上传
    if (storyRec) stopStoryMic(false);
    return false;
  }
  window.micDown = micDown; window.micUp = micUp; window.micCancel = micCancel;
  function toggleMic() { if (storyRec) { stopStoryMic(true); } else { startStoryMic(); } return false; }
  window.toggleMic = toggleMic;
  function startStoryMic() {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      toast('当前浏览器不支持录音，请用 Chrome/Edge 打开'); return;
    }
    setMicLabel('⏺ 正在启动麦克风…');
    navigator.mediaDevices.getUserMedia({ audio: true }).then(function (stream) {
      var AC = window.AudioContext || window.webkitAudioContext;
      var ctx = new AC();
      if (ctx.state === 'suspended' && ctx.resume) { ctx.resume().catch(function () {}); }
      var src = ctx.createMediaStreamSource(stream);
      var proc = ctx.createScriptProcessor(4096, 1, 1);
      var chunks = [];
      var lastVoiceAt = Date.now();
      proc.onaudioprocess = function (e) {
        var d = e.inputBuffer.getChannelData(0);
        chunks.push(new Float32Array(d));
        var peak = 0, i;
        for (i = 0; i < d.length; i++) { var a = d[i]; if (a < 0) a = -a; if (a > peak) peak = a; }
        if (peak >= storyNoise) { lastVoiceAt = Date.now(); }
        else if (Date.now() - lastVoiceAt > storySilentMs) { stopStoryMic(true); } // 停口自动出字
      };
      src.connect(proc); proc.connect(ctx.destination);
      var rec = { stream: stream, ctx: ctx, src: src, proc: proc, chunks: chunks, rawRate: ctx.sampleRate || 16000, startT: Date.now(), sentN: 0, interimBusy: false };
      storyRec = rec;
      rec.tick = setInterval(function () {
        if (!storyRec) return;
        tryInterim(); // 说着话就分段出字（近似实时）
        var s = Math.floor((Date.now() - storyRec.startT) / 1000);
        setMicLabel('⏺ 正在听 ' + s + 's · 字在出来…');
      }, 1000);
      rec.timer = setTimeout(function () { stopStoryMic(true); }, 60000); // 最长 60s
      setMicLabel('⏺ 正在听 · 说完停一下');
    }).catch(function (err) {
      setMicLabel('🎙 点击说话');
      toast(err && err.name === 'NotAllowedError' ? '麦克风权限被拒绝：请允许麦克风后重试' : '无法打开麦克风，请重试');
    });
  }

  function stopStoryMic(uploadNow) {
    var rec = storyRec;
    if (!rec) return;
    storyRec = null;
    clearTimeout(rec.timer); clearInterval(rec.tick);
    try { rec.proc.disconnect(); rec.src.disconnect(); } catch (e) { /* 忽略 */ }
    try { rec.ctx.close(); } catch (e) { /* 忽略 */ }
    rec.stream.getTracks().forEach(function (tr) { tr.stop(); });
    if (!uploadNow) { setMicLabel('🎙 点击说话'); return; }
    var samples = mergeChunks(rec.chunks);
    var durSec = samples.length / (rec.rawRate || 16000);
    if (durSec < 0.4) { setMicLabel('🎙 点击说话'); toast('没听到声音，再点一次说？'); return; }
    setMicLabel('⏳ 识别中…');
    var pcm = resample16k(samples, rec.rawRate);
    uploadStoryAsr(encodeWav16(pcm, 16000));
  }

  function mergeChunks(arr) {
    var n = 0, i;
    for (i = 0; i < arr.length; i++) n += arr[i].length;
    var out = new Float32Array(n), p = 0;
    for (i = 0; i < arr.length; i++) { out.set(arr[i], p); p += arr[i].length; }
    return out;
  }
  function resample16k(samples, fromRate) {
    if (!fromRate || fromRate === 16000) return samples;
    var outLen = Math.max(1, Math.round(samples.length * 16000 / fromRate));
    var out = new Float32Array(outLen), ratio = samples.length / outLen, i;
    for (i = 0; i < outLen; i++) {
      var pos = i * ratio, i0 = Math.floor(pos), i1 = i0 + 1 < samples.length ? i0 + 1 : i0, frac = pos - i0;
      out[i] = samples[i0] + (samples[i1] - samples[i0]) * frac;
    }
    return out;
  }
  function encodeWav16(samples, rate) {
    var n = samples.length, buf = new ArrayBuffer(44 + n * 2), dv = new DataView(buf), i;
    function wstr(o, s) { for (i = 0; i < s.length; i++) dv.setUint8(o + i, s.charCodeAt(i)); }
    wstr(0, 'RIFF'); dv.setUint32(4, 36 + n * 2, true); wstr(8, 'WAVE');
    wstr(12, 'fmt '); dv.setUint32(16, 16, true); dv.setUint16(20, 1, true); dv.setUint16(22, 1, true);
    dv.setUint32(24, rate, true); dv.setUint32(28, rate * 2, true);
    dv.setUint16(32, 2, true); dv.setUint16(34, 16, true);
    wstr(36, 'data'); dv.setUint32(40, n * 2, true);
    var o = 44;
    for (i = 0; i < n; i++) {
      var s = Math.max(-1, Math.min(1, samples[i]));
      dv.setInt16(o, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
      o += 2;
    }
    return new Blob([buf], { type: 'audio/wav' });
  }
  function uploadStoryAsr(blob) {
    var fd = new FormData();
    fd.append('file', blob, 'story_' + Date.now() + '.wav');
    fetch(CONFIG.BASE_URL + '/api/audio/asr', { method: 'POST', body: fd })
      .then(function (r) { return r.json().catch(function () { return null; }); })
      .then(function (j) {
        if (j && j.code === 200 && j.data && j.data.text) {
          $('storyText').value = j.data.text;
          setMicLabel('🎙 点击说话');
          toast('已转成文字，检查一下就能收好啦 ✍️');
        } else {
          setMicLabel('🎙 点击说话');
          throw new Error((j && j.message) || '识别失败，请稍后再试');
        }
      })
      .catch(function (err) {
        setMicLabel('🎙 点击说话');
        toast(err && err.message ? err.message + '，请打字或重试' : '识别失败，请打字或重试');
      });
  }

  function storyTotalSamples() {
    var rec = storyRec; if (!rec || !rec.chunks || !rec.chunks.length) return 0;
    var n = 0, i; for (i = 0; i < rec.chunks.length; i++) n += rec.chunks[i].length;
    return n;
  }
  function tryInterim() {
    var rec = storyRec;
    if (!rec || rec.interimBusy) return;
    var rate = rec.rawRate || 16000;
    var total = storyTotalSamples();
    if (total < rate * 1.5) return;                 // 攒够约 1.5s 再送
    if (total - (rec.sentN || 0) < rate * 1.2) return; // 距上次送字又多了约 1.2s
    rec.sentN = total;
    rec.interimBusy = true;
    var merged = mergeChunks(rec.chunks);
    var pcm = resample16k(merged, rate);
    uploadAsrQuiet(encodeWav16(pcm, 16000)).then(function (txt) {
      rec.interimBusy = false;
      if (!storyRec || !txt) return; // 已松开/没字：最终结果会覆盖，不抢写
      $('storyText').value = txt;
    }, function () { rec.interimBusy = false; });
  }
  function uploadAsrQuiet(blob) {
    var fd = new FormData();
    fd.append('file', blob, 'story_' + Date.now() + '.wav');
    return fetch(CONFIG.BASE_URL + '/api/audio/asr', { method: 'POST', body: fd })
      .then(function (r) { return r.json().catch(function () { return null; }); })
      .then(function (j) {
        if (j && j.code === 200 && j.data && j.data.text) return j.data.text;
        return '';
      });
  }
  function saveStoryNow() {
var ctx = curStoryCtx;
    if (!ctx) return;
    var text = ($('storyText').value || '').trim();
    if (!text) { toast('先说说讲了什么吧～'); return; }
    var btn = $('storySaveBtn');
    busy(btn, true);
    var payload = {
      title: '',
      originalText: text,
      summary: text.length > 80 ? text.slice(0, 80) + '…' : text
    };
    if (ctx.type === 'node') payload.nodeId = ctx.id; else payload.eraId = ctx.id;
    var doPolish = $('storyPolish').checked;
    var chain;
    if (doPolish) {
      chain = polishStory(text, '').then(function (p) { payload.polishedText = p; }, function () { /* 润色失败只存原文 */ });
    } else {
      chain = Promise.resolve();
    }
    chain.then(function () { return M4.saveStory(payload); }).then(function (story) {
      hideMask('storyMask');
      toast('故事收好了 🌼');
      if (ctx.type === 'node') openNode(ctx.id); else openEra(ctx.id);
      // 顺手生成朋友圈文案（可关）
      momentForStory(story.id);
    }).catch(function (e) { toast(e.message || '保存失败，稍后再试'); })
      .then(function () { busy(btn, false); });
  }
  window.saveStoryNow = saveStoryNow;

  /* ================= 朋友圈文案 ================= */
  function momentForStory(storyId) {
    showMask('momentMask');
    $('momentBody').innerHTML = '<div class="empty-tip">正在写文案…</div>';
    M4.generateMoments({ storyId: storyId, style: 'simple' }).then(function (list) {
      if (!list || !list.length) { $('momentBody').innerHTML = '<div class="empty-tip">这次没写出来，稍后再试试</div>'; return; }
      $('momentBody').innerHTML = (list || []).map(function (m) {
        return '<div class="moment-item" data-id="' + m.id + '" data-txt="' + esc(m.content) + '">' +
          '<div class="mi-txt">' + esc(m.content) + '</div>' +
          '<div class="mi-tag">' + (m.selected ? '✅ 已选' : '点一下选这条') + '</div></div>';
      }).join('');
      Array.prototype.forEach.call($('momentBody').querySelectorAll('.moment-item'), function (el) {
        el.onclick = function () {
          M4.selectMoment(parseInt(el.dataset.id, 10)).then(function () {
            Array.prototype.forEach.call($('momentBody').querySelectorAll('.moment-item'), function (o) { o.classList.remove('selected'); });
            el.classList.add('selected');
            var t = el.querySelector('.mi-tag'); if (t) t.textContent = '✅ 已选好并复制';
            curMomentText = el.getAttribute('data-txt') || '';
            copyMomentText(curMomentText);
          }).catch(errTip);
        };
      });
    }).catch(function (e) { $('momentBody').innerHTML = '<div class="empty-tip">' + esc(e.message) + '</div>'; });
  }
  window.momentForStory = momentForStory;
  function copyMomentText(text) {
    if (!text) { toast('先点一条文案，再复制'); return Promise.resolve(); }
    function legacyCopy() {
      var ta = document.createElement('textarea');
      ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
      document.body.appendChild(ta); ta.select();
      var ok = false; try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
      document.body.removeChild(ta);
      toast(ok ? '文案已复制 ✅ 去微信朋友圈长按粘贴' : '复制失败，请长按文字手动复制');
      return ok;
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
      return navigator.clipboard.writeText(text).then(function () {
        toast('文案已复制 ✅ 去微信朋友圈长按粘贴');
      }, function () { legacyCopy(); });
    }
    legacyCopy();
    return Promise.resolve();
  }
  window.copySelectedMoment = function () {
    var first = document.querySelector('#momentBody .moment-item');
    var txt = curMomentText || (first ? (first.getAttribute('data-txt') || '') : '');
    if (!txt) { toast('还没有文案，先点一条'); return; }
    copyMomentText(txt);
  };

  /* ================= 年代对照（找示意图） ================= */
  function openMatch(eraId) {
    curMatchCtx = { eraId: eraId, photoUrl: '', photoId: null };
    var latest = (PHOTOS || []).filter(function (p) { return p.eraId === eraId && p.source === 'user' && !p.isIllustration; });
    if (latest.length) {
      curMatchCtx.photoUrl = latest[latest.length - 1].url;
      curMatchCtx.photoId = latest[latest.length - 1].id;
    }
    $('matchDesc').value = '';
    $('matchResult').innerHTML = '';
    var up = $('matchUploadBtn');
    up.textContent = curMatchCtx.photoUrl ? '📷 已有一张现在的照片（可重传）' : '📷 先传一张现在的照片（必需）';
    showMask('matchMask');
  }
  window.openMatch = openMatch;

  $('matchUploadBtn').addEventListener('click', function () {
    var inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = function () {
      var f = inp.files && inp.files[0];
      if (!f) return;
      uploadImageFile(f).then(function (r) {
        curMatchCtx.photoUrl = r.url;
        var p = { url: r.url, source: 'user', isIllustration: 0 };
        if (curMatchCtx.eraId) p.eraId = curMatchCtx.eraId;
        return M4.savePhoto(p);
      }).then(function (ph) {
        curMatchCtx.photoId = ph.id;
        $('matchUploadBtn').textContent = '📷 照片已上传，可以找图了';
        return M4.photos().then(function (ps) { PHOTOS = ps || []; });
      }).catch(errTip);
    };
    inp.click();
  });

  function runMatch() {
    var desc = ($('matchDesc').value || '').trim();
    if (desc.length < 2) { toast('先说说这里以前是什么样吧'); return; }
    if (!curMatchCtx.photoUrl) { toast('请先上传一张现在的照片'); return; }
    var btn = $('matchRunBtn');
    busy(btn, true);
    $('matchResult').innerHTML = '<div class="empty-tip">正在素材库里找…</div>';
    var req = { description: desc, photoId: curMatchCtx.photoId };
    M4.match(req).then(function (r) {
      var sug = (r && r.suggestions) || [];
      if (!r || !r.matched || !sug.length) {
        $('matchResult').innerHTML = '<div class="empty-tip"><span class="et-emoji">🔍</span>没找到太像的（素材库没命中、后端没配 Pexels Key 时不硬配图）</div>';
        return;
      }
      $('matchResult').innerHTML = '<div class="modal-sub" style="margin:8px 0 0">点一张最像记忆里的（年代示意图，仅供回忆参考）</div><div class="cand-grid">' +
        sug.map(function (s) {
          return '<div class="cand" data-url="' + esc(s.url) + '" data-kw="' + esc(s.keywords || s.scene || '') + '">' +
            '<img src="' + esc(absUrl(s.url)) + '" onerror="this.style.display=\'none\'"><div class="noimg" style="display:none">🖼</div>' +
            '<div class="cand-cap">' + esc(s.keywords || s.era || s.scene || '示意图') + '</div></div>';
        }).join('') + '</div>';
      Array.prototype.forEach.call($('matchResult').querySelectorAll('.cand'), function (el) {
        el.onclick = function () {
          saveIllustration(el.dataset.url, el.dataset.kw);
        };
      });
    }).catch(function (e) { $('matchResult').innerHTML = '<div class="empty-tip">' + esc(e.message) + '</div>'; })
      .then(function () { busy(btn, false); });
  }
  window.runMatch = runMatch;

  function saveIllustration(url, kw) {
    var body = { url: absUrl(url), source: 'stock', isIllustration: 1, eraId: curMatchCtx.eraId, eraTag: kw };
    M4.savePhoto(body).then(function () {
      return M4.photos().then(function (ps) { PHOTOS = ps || []; });
    }).then(function () {
      // 对照卡预览
      $('matchResult').innerHTML =
        '<div class="modal-sub" style="margin:10px 0 2px">⏳ 年代对照卡（已收进「' + esc(curEra ? curEra.name : '这个年代') + '」）</div>' +
        '<div class="contrast-pair"><div class="cp-side"><img src="' + esc(absUrl(curMatchCtx.photoUrl)) + '" onerror="this.style.display=\'none\';this.nextSibling.style.display=\'flex\'"><div class="noimg" style="display:none">📷</div><div class="cp-cap">现在的这里</div></div>' +
        '<div class="cp-vs">⇄</div>' +
        '<div class="cp-side"><img src="' + esc(url) + '" onerror="this.style.display=\'none\';this.nextSibling.style.display=\'flex\'"><div class="noimg" style="display:none">🏭</div><div class="cp-cap">当年的这里（示意图）</div></div></div>' +
        '<button class="btn btn-gold" onclick="hideMask(\'matchMask\')">收好了</button>';
      toast('对照卡收好了，花园会开一朵花 🌷');
      if (curEra) openEra(curEra.id);
    }).catch(errTip);
  }

  /* ================= 请家人帮忙 ================= */
  function openShare(nodeId) {
    M4.shareHelp(nodeId).then(function (r) {
      shareLink = CONFIG.BASE_URL + '/api/v1/m4/help/' + r.shareToken;
      $('shareBox').innerHTML =
        '<div class="pc-label">分享链接（7 天内有效）</div><div style="font-size:13px">' + esc(shareLink) + '</div>' +
        '<div class="hint" style="margin-top:8px">亲友点开可查看节点、补照片/提示（网页版暂未做亲友落地页，接口已通）。</div>' +
        '<div class="hint">有效期至：' + esc(r.expiredAt || '') + '</div>';
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

  /* ================= 添加节点/年代 ================= */
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
  Array.prototype.forEach.call($('eraPresets').querySelectorAll('.opt-chip'), function (c) {
    c.onclick = function () {
      Array.prototype.forEach.call($('eraPresets').querySelectorAll('.opt-chip'), function (o) { o.classList.remove('on'); });
      c.classList.add('on');
      $('eraName').value = c.dataset.v;
    };
  });

  /* ---------------- 启动 ---------------- */
  document.addEventListener('DOMContentLoaded', function () {
    var d = new Date();
    var p = function (n) { return (n < 10 ? '0' : '') + n; };
    $('sbTime').textContent = p(d.getHours()) + ':' + p(d.getMinutes());
    $('segTop').addEventListener('click', function (e) {
      var t = e.target.closest ? e.target.closest('[data-tab]') : null;
      if (t) switchTab(t.dataset.tab);
    });
    $('addNodeBtn').addEventListener('click', window.addNodeBtnHandler);
    $('addEraBtn').addEventListener('click', window.addEraBtnHandler);
    Array.prototype.forEach.call(document.querySelectorAll('.modal-mask'), function (m) {
      m.addEventListener('click', function (e) { if (e.target === m) hideMask(m.id); });
    });
    loadAll();
  });
})();
