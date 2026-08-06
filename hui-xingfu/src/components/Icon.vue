<template>
  <span
    class="icon_wrap"
    :class="[`icon_${name}`]"
    :style="{ color: color || 'currentColor', width: size + 'px', height: size + 'px' }"
    v-html="svg"
  ></span>
</template>

<script>
const ICONS = {
  // 底部 tab
  home: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M8 22 L24 8 L40 22" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M11 20 L11 40 L37 40 L37 20" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M19 40 L19 30 L29 30 L29 40" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="24" cy="13" r="1.5" fill="currentColor"/>
  </svg>`,
  checkin: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="8" y="11" width="32" height="30" rx="4" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M8 19 L40 19" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M16 7 L16 14" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M32 7 L32 14" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M17 30 L22 35 L32 25" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  profile: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="24" cy="18" r="8" stroke="currentColor" stroke-width="2.4"/>
    <path d="M11 41 C11 32 17 28 24 28 C31 28 37 32 37 41" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  devices: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="14" y="8" width="20" height="34" rx="4" stroke="currentColor" stroke-width="2.4"/>
    <path d="M19 12 L29 12" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <circle cx="24" cy="36" r="2" fill="currentColor"/>
    <path d="M22 20 L26 20 M20 24 L28 24" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
  </svg>`,

  // 快捷功能
  brain_games: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M18 12 C13 12 10 15 10 20 C10 23 12 25 12 25 C12 25 9 27 9 31 C9 36 13 38 17 38 C19 38 20 37 21 36 L21 14 C20 13 19 12 18 12 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M30 12 C35 12 38 15 38 20 C38 23 36 25 36 25 C36 25 39 27 39 31 C39 36 35 38 31 38 C29 38 28 37 27 36 L27 14 C28 13 29 12 30 12 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M17 22 L18 24 L17 26" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M31 22 L30 24 L31 26" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  positive_practice: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M24 38 C24 38 8 28 8 18 C8 13 12 9 17 9 C20 9 23 11 24 14 C25 11 28 9 31 9 C36 9 40 13 40 18 C40 28 24 38 24 38 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M16 19 L19 22 L24 16" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  community: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="16" cy="17" r="5" stroke="currentColor" stroke-width="2.4"/>
    <circle cx="32" cy="17" r="5" stroke="currentColor" stroke-width="2.4"/>
    <path d="M7 38 C7 30 11 27 16 27 C21 27 25 30 25 38" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M23 38 C23 30 27 27 32 27 C37 27 41 30 41 38" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  service: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M10 22 C10 14 16 9 24 9 C32 9 38 14 38 22" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M8 22 L40 22 L40 28 C40 32 37 35 33 35 L31 35 L31 40 L17 40 L17 35 L15 35 C11 35 8 32 8 28 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <circle cx="17" cy="28" r="1.6" fill="currentColor"/>
    <circle cx="31" cy="28" r="1.6" fill="currentColor"/>
  </svg>`,
  nostalgia: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M9 18 L24 9 L39 18 L39 38 L9 38 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M16 38 L16 26 L32 26 L32 38" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M14 22 L24 15 L34 22" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M5 38 L43 38" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  activities: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M14 30 C8 30 6 26 8 22 C10 18 14 19 16 22" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M34 30 C40 30 42 26 40 22 C38 18 34 19 32 22" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M16 24 C16 18 20 14 24 14 C28 14 32 18 32 24 L32 36 L16 36 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M20 28 L28 28" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
    <path d="M22 24 L22 26 M26 24 L26 26" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
  </svg>`,
  family: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="14" cy="14" r="4" stroke="currentColor" stroke-width="2.4"/>
    <circle cx="34" cy="14" r="4" stroke="currentColor" stroke-width="2.4"/>
    <path d="M8 26 L8 40 M14 22 L14 40 M20 26 L20 40" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
    <path d="M28 26 L28 40 M34 22 L34 40 M40 26 L40 40" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
    <path d="M22 18 C24 16 26 18 26 18" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
  </svg>`,
  shop: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M10 16 L38 16 L36 40 L12 40 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M17 16 C17 11 20 8 24 8 C28 8 31 11 31 16" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,

  // 天气
  sunny: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="24" cy="24" r="7" stroke="currentColor" stroke-width="2.4"/>
    <path d="M24 8 L24 12 M24 36 L24 40 M8 24 L12 24 M36 24 L40 24" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M12 12 L15 15 M33 33 L36 36 M12 36 L15 33 M33 15 L36 12" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  cloudy: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="32" cy="14" r="4" stroke="currentColor" stroke-width="2.2"/>
    <path d="M14 32 C9 32 6 28 8 23 C10 19 14 19 17 22 C18 17 23 16 27 19 C31 16 38 19 38 25 C38 30 34 32 30 32 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
  </svg>`,
  overcast: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 28 C7 28 4 24 6 19 C8 15 12 15 15 18 C16 13 21 12 25 15 C29 12 36 15 36 21 C36 26 32 28 28 28 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M16 38 C12 38 10 35 11 32 C12 29 16 30 18 32" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
  </svg>`,
  rainy: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 24 C7 24 4 20 6 15 C8 11 12 11 15 14 C16 9 21 8 25 11 C29 8 36 11 36 17 C36 22 32 24 28 24 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M14 30 L12 36 M22 30 L20 36 M30 30 L28 36 M38 30 L36 36" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  stormy: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 22 C7 22 4 18 6 13 C8 9 12 9 15 12 C16 7 21 6 25 9 C29 6 36 9 36 15 C36 20 32 22 28 22 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M22 26 L18 34 L24 34 L20 42" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,

  // UI 控件
  back: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M30 10 L16 24 L30 38" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  close: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M14 14 L34 34 M34 14 L14 34" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
  </svg>`,
  search: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="22" cy="22" r="11" stroke="currentColor" stroke-width="2.6"/>
    <path d="M30 30 L40 40" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"/>
  </svg>`,
  check: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M10 25 L20 35 L40 12" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  bell: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M14 32 L34 32 L31 27 L31 20 C31 14 28 11 24 11 C20 11 17 14 17 20 L17 27 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M20 36 C20 38 22 40 24 40 C26 40 28 38 28 36" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  calendar: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="8" y="11" width="32" height="30" rx="4" stroke="currentColor" stroke-width="2.4"/>
    <path d="M8 19 L40 19" stroke="currentColor" stroke-width="2.4"/>
    <path d="M16 7 L16 14 M32 7 L32 14" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  settings: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="24" cy="24" r="6" stroke="currentColor" stroke-width="2.4"/>
    <path d="M24 8 L24 12 M24 36 L24 40 M8 24 L12 24 M36 24 L40 24 M13 13 L16 16 M32 32 L35 35 M13 35 L16 32 M32 16 L35 13" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
  </svg>`,
  leaf: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M10 38 C10 22 22 10 38 10 C38 26 26 38 10 38 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M10 38 L24 24" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
  </svg>`,
  flower: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="24" cy="24" r="4" fill="currentColor"/>
    <ellipse cx="24" cy="13" rx="4" ry="6" stroke="currentColor" stroke-width="2.2"/>
    <ellipse cx="24" cy="35" rx="4" ry="6" stroke="currentColor" stroke-width="2.2"/>
    <ellipse cx="13" cy="24" rx="6" ry="4" stroke="currentColor" stroke-width="2.2"/>
    <ellipse cx="35" cy="24" rx="6" ry="4" stroke="currentColor" stroke-width="2.2"/>
  </svg>`,
  teapot: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M10 24 C10 18 14 16 20 16 L30 16 C36 16 40 18 40 24 L40 30 C40 35 35 38 30 38 L20 38 C15 38 10 35 10 30 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M40 22 C44 22 44 28 40 28" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M20 16 L20 10 M24 12 L24 16" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
  </svg>`,
  speaker: `<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M14 18 L22 12 L22 36 L14 30 L8 30 L8 18 Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/>
    <path d="M26 16 C30 18 32 22 32 24 C32 26 30 30 26 32" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M28 10 C34 12 38 18 38 24 C38 30 34 36 28 38" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
  </svg>`,
}

export default {
  name: 'Icon',

  props: {
    name: { type: String, required: true },
    size: { type: Number, default: 24 },
    color: { type: String, default: '' },
  },

  computed: {
    svg() {
      return ICONS[this.name] || ''
    },
  },
}
</script>

<style scoped>
.icon_wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  line-height: 0;
}

.icon_wrap >>> svg,
.icon_wrap ::v-deep svg {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
