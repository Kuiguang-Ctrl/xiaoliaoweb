<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App',
}
</script>

<style>
:root {
  /* 主色：暖橘黄 - 像初秋的太阳 */
  --color-primary: #E97E3A;
  --color-primary-deep: #C85A1E;
  --color-primary-soft: #FFE6CC;

  /* 副色：苔绿 - 像窗台的绿植 */
  --color-secondary: #6B8E5A;
  --color-secondary-soft: #DCE9D1;

  /* 辅助色 */
  --color-accent-yellow: #F0C75E;
  --color-accent-red: #D64F4F;
  --color-accent-blue: #7AA5B5;

  /* 中性色：奶油白系 */
  --color-bg: #FFF8EE;
  --color-bg-card: #FFFFFF;
  --color-bg-warm: #FDF1E0;
  --color-bg-soft: #FBF3E5;

  /* 文字色 */
  --color-text: #3A2E25;
  --color-text-soft: #6B5A4D;
  --color-text-light: #8B7B6D;
  --color-text-on-primary: #FFFFFF;

  /* 边线色（手绘感的虚线/实线） */
  --color-border: #E8D5BD;
  --color-border-deep: #C9B59A;
  --color-line: #6B5A4D;

  /* 字体 */
  --font-rounded: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei',
    'Helvetica Neue', sans-serif;
  --font-serif: 'STSong', 'Songti SC', 'SimSun', serif;

  /* 圆角：手绘风偏大但不规则 */
  --radius-sm: 12px;
  --radius-md: 20px;
  --radius-lg: 28px;
  --radius-pill: 60px;

  /* 阴影：柔和、不锐利 */
  --shadow-soft: 0 4px 16px rgba(180, 120, 80, 0.10);
  --shadow-hover: 0 8px 28px rgba(227, 95, 58, 0.18);
  --shadow-card: 0 2px 8px rgba(180, 120, 80, 0.08);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  font-family: var(--font-rounded);
  background: var(--color-bg);
  line-height: 1.5;
  -webkit-text-size-adjust: 100%;
  text-size-adjust: 100%;
  -webkit-font-smoothing: antialiased;
  color: var(--color-text);
  overflow-x: clip;
  width: 100%;
  max-width: 100vw;
  -webkit-tap-highlight-color: transparent;
  overscroll-behavior-y: contain;
  touch-action: manipulation;
}

body {
  padding-top: env(safe-area-inset-top);
  padding-right: env(safe-area-inset-right);
  padding-left: env(safe-area-inset-left);
}

#app {
  min-height: 100%;
}

button {
  font-family: inherit;
  cursor: pointer;
  touch-action: manipulation;
}

input,
button,
textarea,
select {
  font-family: inherit;
  font-size: 16px;
}

a {
  color: inherit;
}

img {
  max-width: 100%;
  height: auto;
  display: block;
}

/* 高对比度模式 */
html.high_contrast body {
  background: #ffffff;
}
html.high_contrast .home_page,
html.high_contrast .signin_page,
html.high_contrast .profile_page {
  background: #ffffff !important;
}
html.high_contrast .placeholder_text,
html.high_contrast .streak_label,
html.high_contrast .streak_unit,
html.high_contrast .calendar_month,
html.high_contrast .day_label,
html.high_contrast .legend_item,
html.high_contrast .tab_label,
html.high_contrast .emotion_name {
  color: #1f1f1f !important;
}
html.high_contrast input::placeholder {
  color: #555555 !important;
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

.sr_only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

:focus-visible {
  outline: 3px solid var(--color-secondary);
  outline-offset: 2px;
}
</style>
