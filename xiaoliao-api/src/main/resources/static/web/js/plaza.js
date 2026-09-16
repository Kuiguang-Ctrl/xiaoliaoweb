/**
 * 广场页 — 小红书式双列瀑布流：看看大家做的小视频（含自己发布的），喜欢就点个赞。
 *
 * 数据走后端 /api/v1/m4/plaza/*（apiFetch 自动带当前测试用户身份，多用户数据按 user_id 隔离）：
 *   GET    /plaza/feed            作品流 + 我收到的赞 + 我的作品数
 *   POST   /plaza/works           把自己的视频作品挂到广场
 *   DELETE /plaza/works/{id}      从广场撤下
 *   POST   /plaza/works/{id}/like 点赞/取消点赞
 * 后端连不上（或本地静态打开）时退回本机演示数据 hgs_plaza_v1，保证没起后端也能演示。
 */
(function () {
  'use strict';

  var PLAZA_KEY = 'hgs_plaza_v1';

  /* 当前测试用户（与 api.js 的 X-Mock-User-Id 用同一个 key） */
  function curUser() {
    var uid = '';
    var name = '';
    try { uid = localStorage.getItem('hgs_test_cur_v1') || ''; } catch (e) {}
    try {
      var arr = JSON.parse(localStorage.getItem('hgs_test_users_v1') || '[]') || [];
      for (var i = 0; i < arr.length; i++) { if (arr[i] && arr[i].uid === uid) { name = arr[i].name || ''; break; } }
    } catch (e) {}
    if (!name) {
      try {
        var p = JSON.parse(localStorage.getItem('hgs_profile_v3') || 'null');
        if (p && p.name) name = p.name;
      } catch (e) {}
    }
    return { uid: uid || ('guest-' + Date.now()), name: name || '我' };
  }

  /* ============ 后端连不上时的本机演示数据 ============ */

  /* 当前测试用户第一次出现在广场时，默认有两件“我的作品”（供他人看到并点赞） */
  var MY_TITLES = [
    { t: '我家的一张老照片', e: '🖼️', s: 18 },
    { t: '那年夏天的故事', e: '🌻', s: 15 }
  ];

  var OTHERS = [
    { id: 'o1', ownerUid: 'demo-o1', owner: '老陈', cover: '🏮', title: '老厂门口的春天', sec: 21, likes: 8 },
    { id: 'o2', ownerUid: 'demo-o2', owner: '王姐', cover: '🌾', title: '下乡那年的麦收', sec: 17, likes: 12 },
    { id: 'o3', ownerUid: 'demo-o3', owner: '刘叔', cover: '🎣', title: '河边的夏天', sec: 14, likes: 5 },
    { id: 'o4', ownerUid: 'demo-o4', owner: '周姨', cover: '🍜', title: '大院的早饭摊', sec: 16, likes: 9 },
    { id: 'o5', ownerUid: 'demo-o5', owner: '赵师傅', cover: '🏭', title: '车间里的师徒', sec: 19, likes: 6 },
    { id: 'o6', ownerUid: 'demo-o6', owner: '孙姐', cover: '🎹', title: '老钢琴搬家记', sec: 13, likes: 4 }
  ];

  /* 封面高度错落，模拟真实照片比例，形成瀑布流 */
  var HEIGHTS = [150, 214, 168, 236, 158, 200];
  var GRADS = [
    'linear-gradient(150deg,#F3DFA6,#DFBC7D)',
    'linear-gradient(150deg,#D7E3B0,#9DBE7E)',
    'linear-gradient(150deg,#F6CDB0,#DD916F)',
    'linear-gradient(150deg,#CFE3DA,#8FB8AC)',
    'linear-gradient(150deg,#EFD0C8,#D3A08E)',
    'linear-gradient(150deg,#D9CFB5,#B4A37E)'
  ];

  function loadAll() {
    try { return JSON.parse(localStorage.getItem(PLAZA_KEY) || '[]') || []; } catch (e) { return []; }
  }
  function saveAll(list) {
    try { localStorage.setItem(PLAZA_KEY, JSON.stringify(list)); } catch (e) {}
  }

  /* 保证广场有基础作品；当前测试用户第一次来会补上“我的作品”（自己的不进自己的流） */
  function ensureSeed() {
    var list = loadAll();
    var c = curUser();
    if (!list.length) {
      list = OTHERS.map(function (o) {
        return { id: o.id, ownerUid: o.ownerUid, owner: o.owner, cover: o.cover, title: o.title, sec: o.sec, likes: o.likes, likedBy: [] };
      });
    }
    var mine = list.filter(function (x) { return x.ownerUid === c.uid; });
    if (!mine.length) {
      MY_TITLES.forEach(function (m, i) {
        list.push({ id: 'm-' + c.uid + '-' + i, ownerUid: c.uid, owner: c.name, cover: m.e, title: m.t, sec: m.s, likes: 0, likedBy: [] });
      });
    }
    saveAll(list);
    return list;
  }

  /* ============ 状态与数据 ============ */

  var state = { online: false, works: [], receivedLikes: 0, myWorkCount: 0, total: 0 };
  var playing = null;

  /** 本机演示数据 → 统一的作品结构（只放别人的，自己的作品在别人的广场里才看得到） */
  function localFeed() {
    var c = curUser();
    var list = ensureSeed();
    var works = [];
    var mineCount = 0;
    var got = 0;
    list.forEach(function (x) {
      if (x.ownerUid === c.uid) { mineCount++; got += (x.likes || 0); return; }
      works.push({
        id: x.id,
        plazaId: null,
        owner: x.owner,
        mine: false,
        title: x.title,
        cover: x.cover || '🎞️',
        coverUrl: '',
        videoUrl: '',
        sec: x.sec || 15,
        likes: x.likes || 0,
        liked: (x.likedBy || []).indexOf(c.uid) >= 0
      });
    });
    return { works: works, myWorkCount: mineCount, receivedLikes: got, total: list.length };
  }

  /** 后端返回 → 统一的作品结构（含自己发布的，自己的标 mine，可撤下） */
  function backendFeed(feed, uid) {
    var list = (feed && feed.works) ? feed.works : [];
    return list.map(function (w) {
      return {
        id: 'p' + w.id,
        plazaId: w.id,
        owner: w.ownerName || '小辽朋友',
        ownerId: w.ownerId,
        mine: !!w.mine || w.ownerId === uid,
        title: w.title || '时光里的故事',
        cover: '🎞️',
        coverUrl: w.coverUrl || '',
        videoUrl: w.videoUrl || '',
        sec: w.duration || 12,
        likes: w.likeCount || 0,
        liked: !!w.liked
      };
    });
  }

  function load() {
    if (typeof M4 === 'undefined' || !M4.plazaFeed) { loadLocal(); return; }
    M4.plazaFeed(30).then(function (feed) {
      state.online = true;
      state.works = backendFeed(feed, curUser().uid);
      state.receivedLikes = (feed && feed.receivedLikes) || 0;
      state.myWorkCount = (feed && feed.myWorkCount) || 0;
      state.total = (feed && feed.total) || 0;
      render();
    }).catch(function (e) {
      console.warn('广场接口没连上，先看本机演示数据：', e);
      loadLocal();
    });
  }

  function loadLocal() {
    var f = localFeed();
    state.online = false;
    state.works = f.works;
    state.receivedLikes = f.receivedLikes;
    state.myWorkCount = f.myWorkCount;
    state.total = f.total;
    render();
  }

  /* ============ 渲染 ============ */

  function render() {
    var sub = $('plazaSub');
    if (sub) {
      sub.textContent = state.online
        ? '看看大家做的小视频，喜欢就点个赞。'
        : '看看大家做的小视频，喜欢就点个赞。（后端没连上，现在看的是本机演示数据）';
    }

    var got = state.receivedLikes || 0;
    $('gotLikes').textContent = got;
    var note = $('likeNote');
    if (note) {
      note.textContent = got > 0
        ? '有 ' + got + ' 个赞啦，大家喜欢您的作品，心里暖暖的吧～'
        : '还没有人点赞。作品做好后，挂到广场，大家就能看到啦～';
    }

    var feed = $('plazaFeed');
    feed.innerHTML = '';
    if (!state.works.length) {
      feed.innerHTML = '<div class="empty-tip">' + (state.online
        ? '广场还空着，点下面「🎬 找小辽做新作品」，做好就能挂上来～'
        : '广场还空着，大家快来做作品吧～') + '</div>';
      return;
    }
    /* 双列瀑布流：按预估高度分配到较矮一列，避免左右长短失衡 */
    var colL = document.createElement('div');
    var colR = document.createElement('div');
    colL.className = 'pl-col';
    colR.className = 'pl-col';
    var colH = [0, 0];
    state.works.forEach(function (it, idx) {
      it.h = HEIGHTS[idx % HEIGHTS.length];
      it.grad = GRADS[idx % GRADS.length];
      var card = buildCard(it);
      var est = it.h + 104;
      if (colH[0] <= colH[1]) { colL.appendChild(card); colH[0] += est; }
      else { colR.appendChild(card); colH[1] += est; }
    });
    feed.appendChild(colL);
    feed.appendChild(colR);
  }

  function buildCard(it) {
    var card = document.createElement('div');
    card.className = 'pl-card';

    var cover = document.createElement('div');
    cover.className = 'pl-cover';
    cover.style.height = it.h + 'px';
    cover.style.background = it.grad;
    if (it.coverUrl) {
      var img = document.createElement('img');
      img.className = 'pl-img';
      img.loading = 'lazy';
      img.alt = '';
      img.src = absUrl(it.coverUrl);
      img.onerror = function () { img.style.display = 'none'; };
      cover.appendChild(img);
    } else {
      var emo = document.createElement('span');
      emo.className = 'pl-cov-emoji';
      emo.textContent = it.cover || '🎞️';
      cover.appendChild(emo);
    }
    if (it.mine) {
      var badge = document.createElement('span');
      badge.className = 'pl-badge';
      badge.textContent = '我';
      cover.appendChild(badge);
      if (it.plazaId) {
        var del = document.createElement('button');
        del.className = 'pl-del';
        del.type = 'button';
        del.title = '从广场撤下';
        del.textContent = '✕';
        del.onclick = function (e) { e.stopPropagation(); unpublish(it); };
        cover.appendChild(del);
      }
    }
    var dur = document.createElement('span');
    dur.className = 'pl-dur';
    dur.textContent = (it.sec || 12) + '″';
    cover.appendChild(dur);
    cover.onclick = function () { playWork(it, cover); };
    card.appendChild(cover);

    var txt = document.createElement('div');
    txt.className = 'pl-txt';
    txt.textContent = it.title || '时光里的故事';
    card.appendChild(txt);

    var user = document.createElement('div');
    user.className = 'pl-user';
    var ava = document.createElement('span');
    ava.className = 'pl-ava';
    ava.textContent = String(it.owner || '?').charAt(0);
    var name = document.createElement('span');
    name.className = 'pl-name';
    name.textContent = it.owner || '小辽朋友';
    var like = document.createElement('button');
    like.className = 'pl-like' + (it.liked ? ' on' : '');
    like.type = 'button';
    like.innerHTML = (it.liked ? '❤️ ' : '🤍 ') + '<b>' + (it.likes || 0) + '</b>';
    like.onclick = function () { toggleLike(it, like); };
    user.appendChild(ava);
    user.appendChild(name);
    user.appendChild(like);
    card.appendChild(user);
    return card;
  }

  /* ============ 交互 ============ */

  /* 点封面就地播放（再点一下收起来），后端作品才有视频地址 */
  function playWork(it, cover) {
    var old = cover.querySelector('video');
    if (old) {
      try { old.pause(); } catch (e) {}
      cover.removeChild(old);
      playing = null;
      return;
    }
    if (!it.videoUrl) { toast('这件作品还在本机，做好视频后就能看啦'); return; }
    if (playing) { try { playing.pause(); } catch (e) {} playing = null; }
    var v = document.createElement('video');
    v.className = 'pl-video';
    v.src = absUrl(it.videoUrl);
    v.controls = true;
    v.autoplay = true;
    v.setAttribute('playsinline', '');
    v.onerror = function () { toast('视频打不开，可能被清理了'); };
    cover.appendChild(v);
    playing = v;
  }

  function toggleLike(it, btn) {
    if (state.online && it.plazaId) {
      btn.disabled = true;
      var before = it.likes || 0;
      M4.togglePlazaLike(it.plazaId).then(function (vo) {
        it.likes = (vo && vo.likeCount) || 0;
        it.liked = !!(vo && vo.liked);
        if (it.mine) {
          state.receivedLikes = Math.max(0, (state.receivedLikes || 0) + (it.likes - before));
        }
        toast(it.liked ? '❤️ 谢谢您的点赞' : '已取消点赞');
        render();
      }).catch(function (e) {
        btn.disabled = false;
        toast('点赞没成功：' + ((e && e.message) || '请稍后再试'));
      });
      return;
    }
    /* 本机演示数据 */
    var c = curUser();
    var list = loadAll();
    var target = null;
    for (var i = 0; i < list.length; i++) { if (list[i].id === it.id) { target = list[i]; break; } }
    if (!target) return;
    var likedBy = target.likedBy || [];
    var idx = likedBy.indexOf(c.uid);
    if (idx >= 0) {
      likedBy.splice(idx, 1);
      target.likes = Math.max(0, (target.likes || 0) - 1);
      toast('已取消点赞');
    } else {
      likedBy.push(c.uid);
      target.likes = (target.likes || 0) + 1;
      toast('❤️ 谢谢您的点赞');
    }
    target.likedBy = likedBy;
    saveAll(list);
    it.likes = target.likes;
    it.liked = idx < 0;
    render();
  }

  function unpublish(it) {
    if (!state.online || !it.plazaId) return;
    if (!confirm('把《' + (it.title || '这件作品') + '》从广场撤下？撤下后大家就看不到了。')) return;
    M4.unpublishPlazaWork(it.plazaId).then(function () {
      toast('已经撤下了');
      load();
    }).catch(function (e) {
      toast('撤下没成功：' + ((e && e.message) || '请稍后再试'));
    });
  }

  function goMakeVideo() {
    try { sessionStorage.setItem('hgs_make_video_v1', '1'); } catch (e) {}
    location.href = '../chat.html?t=' + Date.now();
  }

  /* ============ 把我的作品发到广场 ============ */

  function showMask(id) { var m = $(id); if (m) m.classList.add('show'); }
  function hideMask(id) { var m = $(id); if (m) m.classList.remove('show'); }

  function openPubMask() {
    showMask('pubMask');
    loadPubList();
  }
  function hidePubMask() { hideMask('pubMask'); }

  function loadPubList() {
    var box = $('pubList');
    box.innerHTML = '<div class="empty-tip">正在找您的作品…</div>';
    if (typeof M4 === 'undefined' || !M4.videos) {
      box.innerHTML = '<div class="empty-tip">后端没连上，等会儿再试～</div>';
      return;
    }
    Promise.all([M4.videos(), M4.plazaFeed(60)]).then(function (res) {
      var videos = res[0] || [];
      var published = {};   // videoId → 广场作品 id（已发布的）
      ((res[1] && res[1].works) || []).forEach(function (w) {
        if (w.mine) published[w.videoId] = w.id;
      });
      renderPubList(videos, published);
    }).catch(function (e) {
      box.innerHTML = '<div class="empty-tip">找不到您的作品：' + esc((e && e.message) || '后端没连上') + '</div>';
    });
  }

  function renderPubList(videos, published) {
    var box = $('pubList');
    if (!videos.length) {
      box.innerHTML = '<div class="empty-tip">您还没做过视频呢。回聊天页点「🎬 做视频」，做好就能发上来～</div>';
      return;
    }
    box.innerHTML = videos.map(function (v) {
      var pubId = published[v.id];
      return '<div class="wkv-item">' +
        '<div class="wkv-head" style="cursor:default">' +
        '<div class="wkv-ph">' + (v.posterUrl
          ? '<img src="' + esc(absUrl(v.posterUrl)) + '" alt="" onerror="this.style.display=\'none\';this.parentNode.textContent=\'🎞️\'">'
          : '🎞️') + '</div>' +
        '<div style="flex:1;min-width:0"><div class="wkv-title">' + esc(v.title || '一段回忆视频') + '</div>' +
        '<div class="wkv-meta">' + fmtDate(v.createTime) + (v.duration ? ' · 约 ' + v.duration + ' 秒' : '') + '</div></div>' +
        (pubId
          ? '<button class="btn btn-ghost" style="width:auto;padding:7px 12px;margin:0" onclick="unpubWork(' + pubId + ')">已发布 · 撤下</button>'
          : '<button class="btn btn-gold" style="width:auto;padding:7px 12px;margin:0" onclick="pubWork(' + v.id + ')">发到广场</button>') +
        '</div></div>';
    }).join('');
  }

  /* 发布某支视频作品（后端会记到 m4_plaza_work，别人在广场就能看到） */
  function pubWork(videoId) {
    M4.publishPlazaWork({ videoId: videoId }).then(function () {
      toast('🎉 已经发到广场啦');
      load();
      loadPubList();
    }).catch(function (e) {
      toast('没发成功：' + ((e && e.message) || '稍后再试'));
    });
  }

  /* 从广场撤下（作品本身还在「我的作品」里） */
  function unpubWork(workId) {
    M4.unpublishPlazaWork(workId).then(function () {
      toast('已经撤下了');
      load();
      loadPubList();
    }).catch(function (e) {
      toast('没撤成：' + ((e && e.message) || '稍后再试'));
    });
  }

  function goTime(tab) {
    location.href = 'time.html?t=' + Date.now() + (tab ? '#' + tab : '');
  }

  document.addEventListener('DOMContentLoaded', function () {
    var d = new Date();
    var p = function (n) { return (n < 10 ? '0' : '') + n; };
    $('sbTime').textContent = p(d.getHours()) + ':' + p(d.getMinutes());
    window.goMakeVideo = goMakeVideo;
    window.goTime = goTime;
    window.openPubMask = openPubMask;
    window.hidePubMask = hidePubMask;
    window.pubWork = pubWork;
    window.unpubWork = unpubWork;
    render();   // 先用本机数据画一版，后端回来再刷新，避免白屏
    load();
  });
})();
