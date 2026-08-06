<template>
  <div class="profile_page">
    <div class="profile_header">
      <span class="profile_title">我的</span>
      <span class="profile_subtitle">小辽一直在这里陪你</span>
    </div>

    <!-- 用户信息 -->
    <div class="user_card">
      <div class="user_avatar" aria-hidden="true">
        <Icon name="profile" :size="44" color="var(--color-primary-deep)" />
      </div>
      <div class="user_info">
        <span class="user_name">{{ userNickname }}</span>
        <span class="user_phone">{{ userPhone }}</span>
      </div>
      <div class="user_decor" aria-hidden="true">
        <Icon name="leaf" :size="22" color="var(--color-secondary)" />
      </div>
    </div>

    <!-- 菜单列表 -->
    <div class="menu_list">
      <button
        type="button"
        class="menu_item elder_entry"
        @click="openElderSettings"
      >
        <span class="menu_icon" aria-hidden="true">
          <Icon name="search" :size="24" color="var(--color-primary-deep)" />
        </span>
        <span class="menu_label">
          长辈版
          <span class="menu_sub">放大字体 · 高对比度</span>
        </span>
        <span class="menu_arrow" aria-hidden="true">
          <Icon name="back" :size="18" color="var(--color-primary-deep)" class="arrow_flip" />
        </span>
      </button>
      <button
        type="button"
        class="menu_item"
        @click="handleMenuClick('个人信息')"
      >
        <span class="menu_icon" aria-hidden="true">
          <Icon name="profile" :size="24" color="var(--color-text-soft)" />
        </span>
        <span class="menu_label">个人信息</span>
        <span class="menu_arrow" aria-hidden="true">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>
      <button
        type="button"
        class="menu_item"
        @click="handleMenuClick('我的签到')"
      >
        <span class="menu_icon" aria-hidden="true">
          <Icon name="calendar" :size="24" color="var(--color-text-soft)" />
        </span>
        <span class="menu_label">我的签到</span>
        <span class="menu_arrow" aria-hidden="true">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>
      <button
        type="button"
        class="menu_item"
        @click="handleMenuClick('我的活动')"
      >
        <span class="menu_icon" aria-hidden="true">
          <Icon name="activities" :size="24" color="var(--color-text-soft)" />
        </span>
        <span class="menu_label">我的活动</span>
        <span class="menu_arrow" aria-hidden="true">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>
      <button
        type="button"
        class="menu_item"
        @click="handleMenuClick('设置')"
      >
        <span class="menu_icon" aria-hidden="true">
          <Icon name="settings" :size="24" color="var(--color-text-soft)" />
        </span>
        <span class="menu_label">设置</span>
        <span class="menu_arrow" aria-hidden="true">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>
    </div>

    <!-- 底部导航 -->
    <BottomNav />

    <!-- Toast -->
    <transition name="toast_fade">
      <div v-if="showToast" class="toast_message" role="status" aria-live="polite">
        <div class="toast_content">
          <span class="toast_icon" aria-hidden="true">
            <Icon name="check" :size="28" color="#FFFFFF" />
          </span>
          <span class="toast_text">{{ toastText }}</span>
        </div>
      </div>
    </transition>

    <!-- 长辈版设置弹窗 -->
    <ElderSettingsModal :visible="showElderSettings" @close="closeElderSettings" />
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { getUserInfo } from '@/api/auth'
import ElderSettingsModal from '@/components/ElderSettingsModal.vue'
import BottomNav from '@/components/BottomNav.vue'
import Icon from '@/components/Icon.vue'

export default {
  name: 'profile',

  components: { ElderSettingsModal, BottomNav, Icon },

  setup() {
    const userInfo = ref(null)
    const showToast = ref(false)
    const toastText = ref('')
    const showElderSettings = ref(false)
    let toastTimer = null

    const userNickname = computed(() => {
      return userInfo.value?.nickname || userInfo.value?.phone || '用户'
    })

    const userPhone = computed(() => {
      return userInfo.value?.phone || '未绑定手机'
    })

    const fetchUserInfo = async () => {
      try {
        const response = await getUserInfo()
        if (response.code === 200) {
          userInfo.value = response.data
        }
      } catch (error) {
        console.error('[FetchUserInfo Error]', error)
      }
    }

    const handleMenuClick = (label) => {
      toastText.value = `「${label}」功能开发中`
      showToast.value = true
      toastTimer = setTimeout(() => {
        showToast.value = false
      }, 2000)
    }

    const openElderSettings = () => {
      showElderSettings.value = true
    }

    const closeElderSettings = () => {
      showElderSettings.value = false
    }

    onMounted(() => {
      fetchUserInfo()
    })

    return {
      userInfo,
      userNickname,
      userPhone,
      showToast,
      toastText,
      showElderSettings,
      handleMenuClick,
      openElderSettings,
      closeElderSettings,
    }
  },
}
</script>

<style scoped>
.profile_page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 16px 20px calc(110px + env(safe-area-inset-bottom)) 20px;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
}

.profile_header {
  padding: 16px 4px 22px 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile_title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
  letter-spacing: 1px;
}

.profile_subtitle {
  font-size: 0.9375rem;
  color: var(--color-text-light);
}

/* ---- 用户卡片 ---- */
.user_card {
  position: relative;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 22px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
  margin-bottom: 18px;
  overflow: hidden;
}

.user_card::before {
  content: '';
  position: absolute;
  top: -20px;
  right: -20px;
  width: 80px;
  height: 80px;
  background: var(--color-primary-soft);
  border-radius: 50%;
  opacity: 0.6;
  z-index: 0;
}

.user_avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-primary-soft);
  border: 2px solid var(--color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.user_info {
  display: flex;
  flex-direction: column;
  flex: 1;
  position: relative;
  z-index: 1;
}

.user_name {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
}

.user_phone {
  font-size: 0.9375rem;
  color: var(--color-text-light);
  margin-top: 2px;
}

.user_decor {
  position: relative;
  z-index: 1;
  opacity: 0.85;
}

/* ---- 菜单列表 ---- */
.menu_list {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
  margin-bottom: 22px;
}

.menu_item {
  display: flex;
  align-items: center;
  padding: 16px 18px;
  border: none;
  border-bottom: 1.5px dashed var(--color-border);
  background: transparent;
  width: 100%;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s;
  min-height: 60px;
}

.menu_item:last-child {
  border-bottom: none;
}

.menu_item:hover {
  background: var(--color-bg-warm);
}

.menu_item:active {
  background: var(--color-bg-warm);
}

.menu_item.elder_entry {
  background: var(--color-primary-soft);
  min-height: 68px;
  border-bottom: 2px solid var(--color-border);
}

.menu_item.elder_entry .menu_label {
  color: var(--color-primary-deep);
  font-weight: 600;
}

.menu_sub {
  display: block;
  font-size: 0.875rem;
  color: var(--color-text-light);
  font-weight: 400;
  margin-top: 2px;
}

.menu_icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-soft);
  margin-right: 14px;
  flex-shrink: 0;
}

.menu_item.elder_entry .menu_icon {
  background: var(--color-bg-card);
}

.menu_label {
  flex: 1;
  font-size: 1.125rem;
  color: var(--color-text);
}

.menu_arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.arrow_flip {
  transform: scaleX(-1);
}

/* ---- Toast ---- */
.toast_message {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: var(--color-text);
  backdrop-filter: blur(6px);
  border-radius: var(--radius-pill);
  padding: 14px 28px;
  z-index: 200;
  pointer-events: none;
  box-shadow: var(--shadow-hover);
}

.toast_content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toast_icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-secondary);
}

.toast_text {
  font-size: 1.0625rem;
  font-weight: 600;
  color: var(--color-bg);
}

.toast_fade-enter-active {
  animation: popIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
.toast_fade-leave-active {
  animation: popOut 0.35s ease-in forwards;
}

@keyframes popIn {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.7); }
  100% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
}

@keyframes popOut {
  0% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
}
</style>
