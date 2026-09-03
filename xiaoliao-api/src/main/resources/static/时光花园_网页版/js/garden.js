/**
 * 我的花园页 — 从后端 M4 /garden 取五类计数并开花
 * 后端连不上时降级显示本地演示计数（hgs_flower_v1）并提示。
 */
(function () {
  'use strict';

  var META = [
    { key: 'storyCount', icon: '🌼', label: '故事' },
    { key: 'photoCount', icon: '🖼️', label: '照片' },
    { key: 'eraCount', icon: '📖', label: '年代记忆' },
    { key: 'momentCount', icon: '💌', label: '朋友圈文案' },
    { key: 'nodeCount', icon: '🌳', label: '人生时光节点' }
  ];
  var FLOOR_X = [10, 24, 38, 52, 66, 80, 94, 108, 122, 136, 150, 164, 178, 192, 206, 220, 234, 248, 262, 276, 290, 304, 318];

  function goTime(tab) { location.href = 'time.html#' + tab; }

  function drawScene(n) {
    var scene = $('gardenScene');
    var old = scene.querySelectorAll('.gs-flower,.gs-bud');
    for (var i = 0; i < old.length; i++) old[i].remove();
    if (n <= 0) {
      [8, 30, 52].forEach(function (i) {
        var b = document.createElement('div');
        b.className = 'gs-bud'; b.textContent = '🌱';
        b.style.left = FLOOR_X[i] + 'px'; b.style.top = (150 + (i % 3) * 16) + 'px';
        scene.appendChild(b);
      });
      return;
    }
    var max = Math.min(n, 40);
    for (var j = 0; j < max; j++) {
      var f = document.createElement('div');
      f.className = 'gs-flower';
      f.textContent = FLOWERS[j % FLOWERS.length];
      f.style.left = FLOOR_X[(j * 7) % FLOOR_X.length] + 'px';
      f.style.top = (130 + ((j * 13) % 55)) + 'px';
      scene.appendChild(f);
    }
  }

  function drawMetrics(g) {
    var box = $('gardenMetrics');
    var max = 1;
    META.forEach(function (m) { max = Math.max(max, g[m.key] || 0); });
    box.innerHTML = META.map(function (m) {
      var c = g[m.key] || 0;
      var pct = Math.max(4, Math.round((c / max) * 100));
      return '<div class="m-row"><div class="m-ico">' + m.icon + '</div>' +
        '<div class="m-label">' + m.label + '</div>' +
        '<div class="m-track"><div class="m-fill" style="width:' + pct + '%"></div></div>' +
        '<div class="m-num">' + c + '</div></div>';
    }).join('');
  }

  function load() {
    M4.garden().then(function (g) {
      g = g || {};
      // 开花数 = 故事 + 照片 + 年代 + 文案（预设 7 个时光节点不算花，明细里单独展示）
      var content = (g.storyCount || 0) + (g.photoCount || 0) + (g.eraCount || 0) + (g.momentCount || 0);
      $('gardenSub').textContent = '开了 ' + content + ' 朵花 · 每讲一个故事、收一张照片，花园就开一朵花';
      drawScene(content);
      drawMetrics(g);
      $('gardenEmpty').style.display = content > 0 ? 'none' : '';
    }).catch(function (e) {
      toast(e.message || '加载失败', 3200);
      var localN = getLocalFlowerN();
      $('gardenSub').textContent = '后端未连上，显示本地演示计数（' + localN + ' 朵）。接好后端后自动切换为真实花园。';
      drawScene(localN);
      $('gardenMetrics').innerHTML = '';
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    var d = new Date();
    var p = function (n) { return (n < 10 ? '0' : '') + n; };
    $('sbTime').textContent = p(d.getHours()) + ':' + p(d.getMinutes());
    window.goTime = goTime;
    load();
  });
})();
