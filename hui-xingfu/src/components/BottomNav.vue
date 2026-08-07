<template>
  <teleport to="body">
    <footer class="bottom_nav" role="navigation" aria-label="底部主导航">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="tab_item"
        :class="{ active: isActive(tab.key), special: tab.special }"
        :aria-current="isActive(tab.key) ? 'page' : undefined"
        :aria-label="tab.label"
        @click="handleClick(tab.key)"
      >
        <span v-if="tab.special" class="special_circle" aria-hidden="true">
          <Icon :name="tab.icon" :size="30" color="#FFFFFF" />
        </span>
        <Icon
          v-else
          :name="tab.icon"
          :size="28"
          :color="isActive(tab.key) ? 'var(--color-primary)' : 'var(--color-text-light)'"
          aria-hidden="true"
        />
        <span class="tab_label">{{ tab.label }}</span>
      </button>
    </footer>
  </teleport>
</template>

<script>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Icon from './Icon.vue'
import { BOTTOM_TABS } from '@/views/home_page/home_config'

const TAB_ROUTE_MAP = {
  home: '/',
  ai: '/chat',
  profile: '/profile',
}

export default {
  name: 'BottomNav',

  components: { Icon },

  setup() {
    const router = useRouter()
    const route = useRoute()
    const tabs = BOTTOM_TABS

    const isActive = (key) => {
      return route.path === TAB_ROUTE_MAP[key]
    }

    const handleClick = (key) => {
      const target = TAB_ROUTE_MAP[key]
      if (!target) return
      if (route.path === target) return
      router.push(target)
    }

    return { tabs, isActive, handleClick }
  },
}
</script>

<style scoped>
.bottom_nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  background: var(--color-bg-card);
  padding: 8px 0 calc(10px + env(safe-area-inset-bottom));
  box-shadow: 0 -2px 12px rgba(180, 120, 80, 0.08);
  border-top: 1.5px solid var(--color-border);
  z-index: 100;
  max-width: 480px;
  margin: 0 auto;
}

.tab_item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  min-width: 64px;
  min-height: 56px;
  padding: 4px 8px 2px 8px;
  border-radius: var(--radius-md);
  transition: background 0.15s;
  background: transparent;
  border: none;
  font-family: inherit;
  cursor: pointer;
  gap: 4px;
  position: relative;
}

.tab_item:not(.special):active {
  background: var(--color-bg-warm);
}

/* 中间浮起的圆形 AI 按钮 */
.tab_item.special {
  background: transparent;
  position: relative;
}

.special_circle {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: var(--color-primary);
  border: 3px solid var(--color-bg-card);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(233, 126, 58, 0.32);
  transition: transform 0.18s, box-shadow 0.18s;
}

.tab_item.special:active .special_circle {
  transform: translateX(-50%) scale(0.94);
  box-shadow: 0 3px 10px rgba(233, 126, 58, 0.28);
}

.tab_label {
  font-size: 0.8125rem;
  color: var(--color-text-light);
  font-weight: 500;
  line-height: 1;
}

.tab_item.active .tab_label {
  color: var(--color-primary);
  font-weight: 600;
}

.tab_item.special .tab_label {
  color: var(--color-primary-deep);
  font-weight: 600;
}

/* AI 提示气泡 */
.ai_toast {
  position: absolute;
  bottom: 96px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-card);
  color: var(--color-primary-deep);
  padding: 10px 18px;
  border-radius: var(--radius-pill);
  border: 1.5px dashed var(--color-primary);
  box-shadow: var(--shadow-soft);
  font-size: 0.9375rem;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;
}

.ai_toast::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid var(--color-bg-card);
}

.toast_fade-enter-active,
.toast_fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.toast_fade-enter-from,
.toast_fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(6px);
}

/* 小屏适配 */
@media (max-width: 360px) {
  .special_circle {
    width: 48px;
    height: 48px;
    bottom: 28px;
  }
  .tab_label {
    font-size: 0.75rem;
  }
}
</style>
