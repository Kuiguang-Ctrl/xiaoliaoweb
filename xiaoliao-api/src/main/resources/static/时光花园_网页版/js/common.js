/**
 * 时光花园 · 网页版 共用逻辑
 * 画像/花园计数沿用 hgs_* 本地 key（与小程序/演示版一致），聊天走 DeepSeek。
 */
(function (global) {
  'use strict';

  // ---------- 小工具 ----------
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
    return d.getFullYear() + '.' + p(d.getMonth() + 1) + '.' + p(d.getDate());
  }

  // ---------- 用户画像（hgs_profile_v3，跨页共享） ----------
  var PROFILE_KEY = 'hgs_profile_v3';
  var DEFAULT_PROFILE = { name: '', era: '', gender: '', style: '' };

  function loadProfile() {
    var p = Object.assign({}, DEFAULT_PROFILE);
    try {
      var s = localStorage.getItem(PROFILE_KEY);
      if (s) p = Object.assign(p, JSON.parse(s));
    } catch (e) { /* 忽略 */ }
    return p;
  }
  function saveProfile(p) {
    try { localStorage.setItem(PROFILE_KEY, JSON.stringify(p)); } catch (e) { /* 忽略 */ }
  }
  function profileDone(p) { return !!(p && p.name && p.era && p.gender && p.style); }
  function eraLabel(e) { return { e40: '40后', e50: '50后', e60: '60后', e70: '70后', e80: '80后' }[e] || ''; }
  function styleLabel(s) { return { chatty: '话多热闹些', quiet: '话少安静些', any: '都行，你看着办' }[s] || ''; }
  function myName(p) { return (p && p.name) || '阿姨'; }
  function myTitle(p) { return p && p.gender === '男' ? '叔叔' : p.gender === '女' ? '阿姨' : '您'; }

  var CN_NUM = { 零: 0, 一: 1, 二: 2, 两: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9 };
  function cnToNum(s) {
    var out = '', cur = '';
    for (var i = 0; i < s.length; i++) {
      var ch = s.charAt(i);
      if (ch in CN_NUM) { cur += CN_NUM[ch]; } else if (cur) { out += cur; cur = ''; }
    }
    return out + cur;
  }
  function parseName(text) {
    if (!text) return '';
    var n = String(text).trim();
    var m = n.match(/(?:叫|喊|称呼|管)\s*(?:我|咱|您|你)?\s*([^\s，。,.！？!?、]{1,6})/);
    if (m) n = m[1];
    n = n.replace(/^(请|就|麻烦|想|要|以后|以后就|你|您|我|咱|可以|叫我|喊我|管我|管叫|叫)/, '');
    n = n.replace(/(就可以了|就行|就好|好了|就行啦|吧|了|呀|啊|呢|哦|的|哈|呗|嗯|呐|撒|啦|就行呗)[。，,.\s]*$/, '');
    if (!n) {
      n = String(text).replace(/^(我叫|我是|就叫我|叫我|喊我|请叫我|你叫我|名字叫|以后叫我|管我叫)/, '').trim();
    }
    return (n || '老张').slice(0, 6);
  }
  function parseEra(text) {
    if (!text) return '';
    var s = String(text).replace(/[零一二两三四五六七八九]+/g, cnToNum);
    var m = s.match(/19(\d{2})/);
    if (m) { var y = parseInt(m[1], 10); return y < 50 ? 'e40' : y < 60 ? 'e50' : y < 70 ? 'e60' : y < 80 ? 'e70' : 'e80'; }
    if (/四零|40后|四十后|4[05]年|4后/.test(s)) return 'e40';
    if (/五零|50后|五十后|五几年|5后/.test(s)) return 'e50';
    if (/六零|60后|六十后|六几年|6后/.test(s)) return 'e60';
    if (/七零|70后|七十后|七几年|7后/.test(s)) return 'e70';
    if (/八零|80后|八十后|八几年|8后/.test(s)) return 'e80';
    return '';
  }
  function parseGender(text) {
    if (/男|叔叔|大爷|老头|爷爷|大哥|先生/.test(text)) return '男';
    if (/女|阿姨|奶奶|大娘|婆婆|姐姐|妹子|大姐|姑娘/.test(text)) return '女';
    return '';
  }
  function parseStyle(text) {
    if (/话多|热闹|多说|多聊|能聊/.test(text)) return 'chatty';
    if (/话少|安静|少说|陪着我|不用多说/.test(text)) return 'quiet';
    if (/都行|随意|看着办|你决定/.test(text)) return 'any';
    return '';
  }

  // ---------- 花园（本地兜底计数，与演示版共用 hgs_flower_v1） ----------
  var FLOWERS = ['🌼', '🌷', '🌻', '🌸', '🌺', '🌹', '💐', '🏵️'];
  var FLOWER_KEY = 'hgs_flower_v1';
  function getLocalFlowerN() {
    try { return typeof localStorage.getItem(FLOWER_KEY) === 'string' ? parseInt(localStorage.getItem(FLOWER_KEY), 10) || 0 : 0; } catch (e) { return 0; }
  }

  // ---------- DeepSeek（演示聊天/润色，Key 可被 hgs_ds_key 覆盖） ----------
  function getDsKey() {
    return localStorage.getItem('hgs_ds_key') || CONFIG.DS_DEMO_KEY;
  }
  function askDeepSeek(system, user, history) {
    return new Promise(function (resolve, reject) {
      var key = getDsKey();
      if (!key) { reject(new Error('no-key')); return; }
      var msgs = (history || []).map(function (h) {
        return { role: h.role || 'user', content: h.content };
      });
      msgs.push({ role: 'user', content: user });
      if (system) msgs.unshift({ role: 'system', content: system });
      fetch(CONFIG.DS_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + key },
        body: JSON.stringify({
          model: CONFIG.DS_MODEL,
          messages: msgs,
          max_tokens: 300,
          temperature: 0.7,
          stream: false
        })
      }).then(function (r) {
        if (r.status !== 200) throw new Error('HTTP ' + r.status);
        return r.json();
      }).then(function (d) {
        var txt = d && d.choices && d.choices[0] && d.choices[0].message && d.choices[0].message.content;
        resolve((txt || '').trim());
      }).catch(function (err) {
        if (err && err.message === 'HTTP 401') {
          localStorage.removeItem('hgs_ds_key');
        }
        reject(err);
      });
    });
  }

  /** 润色一段口述回忆：instruction 为空是初稿，否则按用户要求改写 */
  function polishStory(rawText, instruction) {
    var sys = '你是"小辽"，帮老人把一段口语化的回忆整理成通顺的第一人称小段落。要求：保留细节和原意，语气温暖朴实，不添加没说过的事，100字左右，直接输出整理好的文字，不要任何解释。';
    var user = instruction ? ('老人希望你这样改：' + instruction + '\n\n原文：' + rawText) : ('请整理这段口述：\n' + rawText);
    return askDeepSeek(sys, user);
  }

  // ---------- RAG 知识库（拉不到就用通用回复兜底） ----------
  var ragCache = null;
  var ragLoading = null;
  function loadRag() {
    if (ragCache) return Promise.resolve(ragCache);
    if (ragLoading) return ragLoading;
    ragLoading = fetch(CONFIG.RAG_URL, { timeout: 6000 }).then(function (r) {
      if (r.status !== 200) return null;
      return r.json();
    }).then(function (d) {
      ragCache = (d && d.segments) ? d : null;
      return ragCache;
    }).catch(function () { ragCache = null; return null; });
    return ragLoading;
  }
  function ragSearch(text, maxSeg) {
    if (!ragCache || !ragCache.segments || !text) return [];
    maxSeg = maxSeg || 3;
    var segs = ragCache.segments;
    var idx = ragCache.index || {};
    var scores = {};
    Object.keys(idx).forEach(function (kw) {
      if (text.indexOf(kw) >= 0) {
        (idx[kw] || []).forEach(function (i) { scores[i] = (scores[i] || 0) + 1; });
      }
    });
    var ranked = Object.keys(scores).map(Number).sort(function (a, b) { return scores[b] - scores[a]; });
    if (!ranked.length) return [];
    var guide = ranked.filter(function (i) { return segs[i] && segs[i][0] === '对话指南'; });
    var other = ranked.filter(function (i) { return guide.indexOf(i) < 0; });
    return guide.concat(other).slice(0, maxSeg).map(function (i) { return segs[i]; }).filter(Boolean);
  }

  global.$ = $;
  global.esc = esc;
  global.toast = toast;
  global.fmtDate = fmtDate;
  global.PROFILE_KEY = PROFILE_KEY;
  global.DEFAULT_PROFILE = DEFAULT_PROFILE;
  global.loadProfile = loadProfile;
  global.saveProfile = saveProfile;
  global.profileDone = profileDone;
  global.eraLabel = eraLabel;
  global.styleLabel = styleLabel;
  global.myName = myName;
  global.myTitle = myTitle;
  global.parseName = parseName;
  global.parseEra = parseEra;
  global.parseGender = parseGender;
  global.parseStyle = parseStyle;
  global.FLOWERS = FLOWERS;
  global.getLocalFlowerN = getLocalFlowerN;
  global.getDsKey = getDsKey;
  global.askDeepSeek = askDeepSeek;
  global.polishStory = polishStory;
  global.loadRag = loadRag;
  global.ragSearch = ragSearch;
})(window);
