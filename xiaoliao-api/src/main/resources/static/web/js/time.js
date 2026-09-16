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
  var PHOTOS = [];       // 当前用户全部照片缓存（含 AI 生成照片；按 nodeId/eraId 归位展示）
  var curNode = null;    // 当前打开的节点
  var curEra = null;     // 当前打开的年代
  var shareLink = '';

  /* ---------- 遮罩 / 详情层 ---------- */
  function showMask(id) { $(id).classList.add('show'); }
  function hideMask(id) { $(id).classList.remove('show'); }
  function layer() { return $('detailLayer'); }
  function openDetail(title) { $('detailTitle').textContent = title; layer().style.display = 'flex'; layer().scrollTop = 0; $('detailBody').innerHTML = ''; }
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
    return Promise.all([M4.timeline(), M4.eras(), M4.videos(), M4.photos()])
      .then(function (rs) {
        VIDEOS = rs[2] || [];
        PHOTOS = rs[3] || [];
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
      var nodePhotos = photosOf(n.id, null).length;
      var done = (n.videoCount || 0) > 0 || nodePhotos > 0;
      var displayName = n.displayName || n.customName || '一段时光';
      return '<div class="tl-item" data-id="' + n.id + '">' +
        '<div class="tl-dot' + (done ? ' done' : '') + '"></div>' +
        '<div style="flex:1;min-width:0"><div class="tl-name' + (done ? ' done' : '') + '">' + (NODE_ICON[n.stage] || '✨') + ' ' + esc(displayName) + '</div>' +
        '<div class="tl-years">' + esc(yearLabel(n)) + '</div>' +
        '<div class="tl-acts"><button class="tl-btn" onclick="event.stopPropagation();renameNode(' + n.id + ')">✏️ 改名</button>' +
        '<button class="tl-btn danger" onclick="event.stopPropagation();deleteNodeAsk(' + n.id + ')">🗑 删除</button></div></div>' +
        '<span class="tl-count">' + (n.videoCount || 0) + ' 视频 · ' + nodePhotos + ' 照片</span></div>';
    }).join('');
    Array.prototype.forEach.call(box.querySelectorAll('.tl-item'), function (el) {
      el.onclick = function () { openNode(parseInt(el.dataset.id, 10)); };
    });
  }

  function renderEras(eras) {
    var box = $('eraList');
    if (!eras || !eras.length) { box.innerHTML = '<div class="empty-tip"><span class="et-emoji">🕰</span>还没有年代记忆，去和小辽新建一个吧。</div>'; return; }
    box.innerHTML = eras.map(function (e) {
      var eraPhotos = photosOf(null, e.id);
      var first = (VIDEOS || []).filter(function (v) { return v.eraId === e.id; })[0] || eraPhotos[0];
      var coverUrl = first && (first.posterUrl || first.url);
      var ph = coverUrl
        ? '<img src="' + esc(absUrl(coverUrl)) + '" onerror="this.parentNode.innerHTML=\'🕰\';this.onerror=null">'
        : '🕰';
      return '<div class="era-item" data-id="' + e.id + '">' +
        '<div class="ei-ph">' + ph + '</div>' +
        '<div style="flex:1"><div class="ei-name">' + esc(e.name) + '</div>' +
        '<div class="ei-date">' + (e.videoCount || 0) + ' 视频 · ' + eraPhotos.length + ' 照片</div></div>' +
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
  /* ---------- 照片：按人生时光节点 / 年代记忆归位展示 ---------- */
  function photosOf(nodeId, eraId) {
    return (PHOTOS || []).filter(function (p) {
      return eraId ? p.eraId === eraId : p.nodeId === nodeId;
    });
  }
  function renderPhotos(list, emptyText) {
    if (!list || !list.length) return '<div class="empty-tip">' + emptyText + '</div>';
    var cells = list.map(function (p) {
      var abs = absUrl(p.url || '');
      return '<div style="display:inline-block;width:31.5%;margin:0 1.5% 8px 0;vertical-align:top;line-height:0">' +
        '<img src="' + esc(abs) + '" style="width:100%;border-radius:10px;display:block;cursor:zoom-in;background:#eee" ' +
        'onclick="window.open(\'' + String(abs).replace(/'/g, '%27') + '\',\'_blank\')" ' +
        'onerror="this.style.display=\'none\'"></div>';
    }).join('');
    return '<div>' + cells + '</div>';
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
    Promise.all([M4.timeline(), M4.stories(id).catch(function () { return []; })]).then(function (rs) {
      var nodes = rs[0];
      var nodeStories = rs[1] || [];
      var n = nodes.find(function (x) { return x.id === id; });
      if (!n) { toast('没找到这个节点'); return; }
      curNode = n; curEra = null;
      openDetail('人生时光 · ' + (n.displayName || n.customName || ''));
      var mine = (VIDEOS || []).filter(function (v) { return v.nodeId === id; });
      var minePics = photosOf(id, null);
      $('detailBody').innerHTML =
        '<div class="polish-card" style="margin-bottom:6px"><b>' + esc(n.displayName || n.customName || '') + '</b>' +
        ' <span class="muted">' + esc(yearLabel(n)) + '</span> · ' + (n.videoCount || 0) + ' 视频 · ' + minePics.length + ' 照片 · ' + nodeStories.length + ' 故事</div>' +
        '<button class="btn btn-gold" onclick="openNodePhoto()">📷 传张照片，让小辽写文案</button>' +
        '<button class="btn btn-soft" onclick="openNodeStory()">📖 讲讲这一站的故事</button>' +
        '<button class="btn btn-soft" onclick="goMakeVideo()">🎬 去和小辽做一段新视频</button>' +
        '<button class="btn btn-soft" onclick="goMakeImage()">🖼 去和小辽画张照片</button>' +
        '<button class="btn btn-soft" onclick="openShare(' + id + ')">👨‍👩‍👧 请家人帮忙</button>' +
        '<div class="page-pad-title">📖 这一站的故事</div>' +
        renderStories(nodeStories, '这一站还没有故事，点上面按钮讲一个吧') +
        '<div class="page-pad-title">🎞 这站的视频</div>' +
        renderVideos(mine, '这站还没有视频，点上面按钮去和小辽做一个吧') +
        '<div class="page-pad-title">🖼 这站的照片</div>' +
        renderPhotos(minePics, '这站还没有照片，点上面按钮去和小辽画一张吧');
    }).catch(function (e) {
      $('detailBody').innerHTML = '<div class="empty-tip"><span class="et-emoji">📡</span>没加载出来，稍后再试</div>';
      errTip(e);
    });
  }

  function openEra(id) {
    Promise.all([M4.eras(), M4.stories(null, id).catch(function () { return []; })]).then(function (rs) {
      var eras = rs[0];
      var eraStories = rs[1] || [];
      var e = eras.find(function (x) { return x.id === id; });
      if (!e) { toast('没找到这个年代'); return; }
      curEra = e; curNode = null;
      openDetail('年代记忆 · ' + e.name);
      var mine = (VIDEOS || []).filter(function (v) { return v.eraId === id; });
      var minePics = photosOf(null, id);
      $('detailBody').innerHTML =
        '<div class="polish-card" style="margin-bottom:6px"><b>' + esc(e.name) + '</b> <span class="muted">' + (e.videoCount || 0) + ' 视频 · ' + minePics.length + ' 照片 · ' + eraStories.length + ' 故事</span></div>' +
        '<button class="btn btn-gold" onclick="openEraPhoto()">📷 传张照片，让小辽写文案</button>' +
        '<button class="btn btn-soft" onclick="openEraStory()">📖 讲讲这个年代的故事</button>' +
        '<button class="btn btn-soft" onclick="goMakeVideo()">🎬 去和小辽做一段新视频</button>' +
        '<button class="btn btn-soft" onclick="goMakeImage()">🖼 去和小辽画张照片</button>' +
        '<div class="page-pad-title">📖 这个年代的故事</div>' +
        renderStories(eraStories, '这个年代还没有故事，点上面按钮讲一个吧') +
        '<div class="page-pad-title">🎞 这个年代的视频</div>' +
        renderVideos(mine, '这个年代还没有视频，点上面按钮去和小辽做一个吧') +
        '<div class="page-pad-title">🖼 这个年代的照片</div>' +
        renderPhotos(minePics, '这个年代还没有照片，点上面按钮去和小辽画一张吧');
    }).catch(function (e) {
      $('detailBody').innerHTML = '<div class="empty-tip"><span class="et-emoji">📡</span>没加载出来，稍后再试</div>';
      errTip(e);
    });
  }
  window.openNode = openNode;   // 供列表点击（列表已用 addEventListener，保留导出以便调试）
  window.openEra = openEra;

  /* ---------- 年代记忆 / 人生时光：每个年代、每一站里「传照片→小辽写文案」「讲个故事」 ---------- */
  var photoTarget = null, eraPhotoUrl = '', eraCopies = [], storyTarget = null;

  function targetName(t) { return (t && t.name) || '这段时光'; }
  function targetRefresh(t) { if (!t) return; if (t.kind === 'era') openEra(t.id); else openNode(t.id); }

  function copyToClipboard(text, okMsg) {
    var s = String(text || '').trim();
    if (!s) return;
    var fallback = function () {
      try {
        var ta = document.createElement('textarea');
        ta.value = s; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.select(); ta.setSelectionRange(0, s.length);
        var ok = document.execCommand('copy');
        document.body.removeChild(ta);
        toast(ok ? okMsg : '没复制上，长按上面的文字手动复制吧');
      } catch (e) { toast('没复制上，长按上面的文字手动复制吧'); }
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(s).then(function () { toast(okMsg); }, fallback);
    } else { fallback(); }
  }

  function openTargetPhoto(t) {
    photoTarget = t; eraPhotoUrl = ''; eraCopies = [];
    $('eraPhotoDesc').value = '';
    $('eraPhotoPrevBox').style.display = 'none';
    $('eraPhotoOut').innerHTML = '';
    $('eraPhotoSub').textContent = '挑一张「' + targetName(t) + '」的照片，再说两句，小辽帮您把话写顺、写暖。';
    showMask('eraPhotoMask');
  }
  window.openEraPhoto = function () {
    if (!curEra) { toast('先点进一个年代'); return; }
    openTargetPhoto({ kind: 'era', id: curEra.id, name: curEra.name });
  };
  window.openNodePhoto = function () {
    if (!curNode) { toast('先点进一段时光'); return; }
    openTargetPhoto({ kind: 'node', id: curNode.id, name: curNode.displayName || curNode.customName || '' });
  };

  window.pickEraPhoto = function () {
    var inp = $('eraPhotoFile');
    inp.value = '';
    inp.onchange = function () {
      var f = inp.files && inp.files[0];
      if (!f) return;
      toast('照片上传中…');
      uploadImageFile(f).then(function (r) {
        eraPhotoUrl = (r && r.url) || '';
        $('eraPhotoPrev').src = absUrl(eraPhotoUrl);
        $('eraPhotoPrevBox').style.display = '';
        toast('照片收到啦，说说照片里的故事');
      }).catch(errTip);
    };
    inp.click();
  };

  function genTargetPhotoCopies() {
    var desc = ($('eraPhotoDesc').value || '').trim();
    if (!eraPhotoUrl) { toast('先选一张照片吧'); return; }
    if (!desc) { toast('说两句这张照片里的故事吧'); return; }
    $('eraPhotoOut').innerHTML = '<div class="empty-tip">小辽正在写，稍等一下…</div>';
    var payload = { photoUrl: eraPhotoUrl, description: desc, title: targetName(photoTarget) };
    if (photoTarget && photoTarget.kind === 'era') payload.eraId = photoTarget.id;
    else if (photoTarget && photoTarget.kind === 'node') payload.nodeId = photoTarget.id;
    M4.momentsFromPhoto(payload).then(function (res) {
      eraCopies = ((res && res.moments) || []).slice(0, 3);
      if (!eraCopies.length) throw new Error('没写出来，再试一次吧');
      renderEraCopies();
    }).catch(function (e) {
      $('eraPhotoOut').innerHTML = '';
      errTip(e);
    });
  }
  window.genEraPhotoCopies = genTargetPhotoCopies;
  window.genNodePhotoCopies = genTargetPhotoCopies;

  function renderEraCopies() {
    var html = '<div class="page-pad-title">✍️ 挑一句顺眼的（点一下复制）</div>';
    html += eraCopies.map(function (m, i) {
      return '<div class="polish-card" style="cursor:pointer;margin-bottom:8px" onclick="copyEraCopy(' + i + ')">' +
        ['①', '②', '③'][i] + ' ' + esc(m.content) +
        '<div class="hint">📋 点一下复制</div></div>';
    }).join('');
    html += '<button class="btn btn-soft" onclick="genEraPhotoCopies()">🔄 换一组说法</button>';
    $('eraPhotoOut').innerHTML = html;
  }

  window.copyEraCopy = function (i) {
    var m = eraCopies[i];
    if (!m) return;
    copyToClipboard(m.content, '📋 文案已复制，去微信粘贴给家人朋友吧');
    if (m.id) M4.selectMoment(m.id).catch(function () {});
  };
  window.copyNodeCopy = window.copyEraCopy;

  function openTargetStory(t) {
    storyTarget = t;
    $('eraStoryTitle').value = '';
    $('eraStoryText').value = '';
    $('eraStorySub').textContent = '讲讲「' + targetName(t) + '」——想到哪儿说到哪儿，小辽帮您记下来。';
    showMask('eraStoryMask');
  }
  window.openEraStory = function () {
    if (!curEra) { toast('先点进一个年代'); return; }
    openTargetStory({ kind: 'era', id: curEra.id, name: curEra.name });
  };
  window.openNodeStory = function () {
    if (!curNode) { toast('先点进一段时光'); return; }
    openTargetStory({ kind: 'node', id: curNode.id, name: curNode.displayName || curNode.customName || '' });
  };

  var eraSpeech = null;
  /** 说话转文字：把识别结果追加到指定输入框（两个弹层共用） */
  function speakToInput(inputId, btnId) {
    var btn = $(btnId);
    var SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) { toast('这个浏览器不支持说话转文字，直接打字也行'); return; }
    if (eraSpeech) { try { eraSpeech.abort(); } catch (e) {} eraSpeech = null; }
    toast('🎙 我在听，您慢慢说…');
    var label = btn.textContent;
    btn.textContent = '⏺ 听着呢…';
    eraSpeech = new SR();
    eraSpeech.lang = 'zh-CN'; eraSpeech.interimResults = false; eraSpeech.continuous = true;
    eraSpeech.onresult = function (e) {
      var got = '';
      for (var i = e.resultIndex; i < e.results.length; i++) {
        if (e.results[i].isFinal) got += e.results[i][0].transcript;
      }
      if (got) $(inputId).value = (($(inputId).value || '') + got).slice(0, 5000);
    };
    eraSpeech.onerror = function () { btn.textContent = label; toast('没听清，您再说一遍？'); };
    eraSpeech.onend = function () { btn.textContent = label; eraSpeech = null; };
    eraSpeech.start();
  }
  window.micEraStory = function () { speakToInput('eraStoryText', 'eraStoryMicBtn'); };
  window.micEraPhoto = function () { speakToInput('eraPhotoDesc', 'eraPhotoMicBtn'); };

  function saveTargetStory() {
    var text = ($('eraStoryText').value || '').trim();
    if (!text) { toast('还没说故事呢'); return; }
    var title = ($('eraStoryTitle').value || '').trim() || (targetName(storyTarget) + '的故事');
    var payload = { title: title.slice(0, 64), originalText: text, polishedText: text };
    if (storyTarget && storyTarget.kind === 'era') payload.eraId = storyTarget.id;
    else if (storyTarget && storyTarget.kind === 'node') payload.nodeId = storyTarget.id;
    M4.saveStory(payload).then(function () {
      hideMask('eraStoryMask');
      toast('📖 故事收好啦');
      targetRefresh(storyTarget);
    }).catch(errTip);
  }
  window.saveEraStory = saveTargetStory;
  window.saveNodeStory = saveTargetStory;

  function renderStories(list, emptyText) {
    if (!list || !list.length) return '<div class="empty-tip">' + emptyText + '</div>';
    return list.map(function (s) {
      var txt = s.polishedText || s.originalText || '';
      return '<div class="polish-card" style="margin-bottom:8px">' +
        '<div style="font-weight:700">' + esc(s.title || '一段回忆') + '</div>' +
        '<div style="margin-top:6px;line-height:1.6;white-space:pre-wrap">' + esc(txt) + '</div>' +
        '<div class="hint">' + fmtDate(s.createTime) +
        ' · <a href="javascript:;" onclick="deleteEraStory(' + s.id + ')" style="color:#B4553F">删除</a></div>' +
        '</div>';
    }).join('');
  }

  window.deleteEraStory = function (id) {
    if (!window.confirm('确定删掉这条故事吗？删掉就找不回来了。')) return;
    M4.deleteStory(id).then(function () {
      toast('已删除');
      if (curEra) openEra(curEra.id);
      else if (curNode) openNode(curNode.id);
    }).catch(errTip);
  };
  window.deleteNodeStory = window.deleteEraStory;

  /* ---------- 请家人帮忙 ---------- */
  function openShare(nodeId) {
    M4.shareHelp(nodeId).then(function (r) {
      var base = (typeof window.activeBase === 'function') ? activeBase() : (window.CONFIG ? CONFIG.BASE_URL : '');
      shareLink = base + '/api/v1/m4/help/' + r.shareToken;
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
  window.goMakeImage = function () {
    location.href = '../chat.html?t=' + Date.now() + '&do=photo';
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
    // 支持从对话/首页带 #life/#era 直达对应 tab
    var h = (location.hash || '').replace('#', '').trim();
    if (h === 'era' || h === 'life') switchTab(h);
    loadAll();
  });
})();
