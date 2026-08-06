<template>
  <div class="detail_page">
    <!-- 顶部导航 -->
    <div class="detail_header">
      <button class="back_btn" @click="goBack" aria-label="返回上一页">
        <Icon name="back" :size="22" color="var(--color-text)" aria-hidden="true" />
        <span>返回</span>
      </button>
      <span class="detail_title">脑力小游戏</span>
      <!-- 喇叭朗读图标（UI 占位，暂不实现朗读功能） -->
      <button class="speaker_btn" aria-label="朗读" title="朗读功能即将上线">
        <Icon name="speaker" :size="22" color="var(--color-text-light)" aria-hidden="true" />
      </button>
    </div>

    <!-- 游戏入口 -->
    <div class="action_area">
      <p class="small_tip">点击下面按钮，进入游戏列表</p>
      <button class="start_btn" @click="startGame">
        <Icon name="brain_games" :size="24" color="#FFFFFF" aria-hidden="true" />
        <span>点我，开始游戏</span>
      </button>
    </div>

    <BottomNav />
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import BottomNav from '@/components/BottomNav.vue'

export default {
  name: 'BrainGameDetail',

  components: { Icon, BottomNav },

  setup() {
    const router = useRouter()

    const goBack = () => router.push('/')
    const startGame = () => router.push('/brain-game/play')

    return { goBack, startGame }
  },
}
</script>

<style scoped>
.detail_page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 0 20px calc(110px + env(safe-area-inset-bottom)) 20px;
  max-width: 480px;
  margin: 0 auto;
}

.detail_header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
}

.back_btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  font-size: 1rem;
  color: var(--color-text);
  cursor: pointer;
  padding: 8px 12px;
  min-height: 44px;
  min-width: 44px;
  border-radius: var(--radius-sm);
  font-family: inherit;
}

.back_btn:hover { background: var(--color-bg-warm); }

.detail_title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
}

.speaker_btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-bg-card);
  border: 1.5px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
}

.speaker_btn:hover { background: var(--color-bg-warm); }

/* ---- 脑力图片区 ---- */
.image_section {
  margin-bottom: 16px;
}

/* ---- 游戏入口 ---- */
.action_area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px 20px;
  min-height: 60vh;
}

.small_tip {
  font-size: 1rem;
  color: var(--color-text-light);
  text-align: center;
}

.start_btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 220px;
  padding: 16px 24px;
  background: var(--color-primary);
  border: none;
  border-radius: 28px;
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(233, 126, 58, 0.18);
}

.start_btn:active {
  transform: scale(0.98);
}

.intro_icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.intro_text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.intro_title {
  font-size: 1.0625rem;
  font-weight: 600;
  color: var(--color-text);
}

.intro_desc {
  font-size: 0.875rem;
  color: var(--color-text-light);
}

/* ---- 开始按钮 ---- */
.action_area {
  padding: 8px 0 40px;
}

.start_btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 0;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: #ffffff;
  font-size: 1.25rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(233, 126, 58, 0.28);
  transition: transform 0.15s, box-shadow 0.15s;
  font-family: inherit;
}

.start_btn:active {
  transform: scale(0.97);
  box-shadow: 0 4px 12px rgba(233, 126, 58, 0.2);
}
</style>
