<template>
  <div class="play_page">
    <!-- 顶部导航 -->
    <div class="play_header">
      <button class="back_btn" @click="goBack" aria-label="返回上一页">
        <Icon name="back" :size="22" color="var(--color-text)" aria-hidden="true" />
        <span>返回</span>
      </button>
      <span class="play_title">选择游戏</span>
    </div>

    <!-- 游戏列表 -->
    <div class="game_list">
      <button class="game_card" @click="openGame('flip')">
        <div class="game_icon" style="background: rgba(233, 126, 58, 0.12);">
          <Icon name="brain_games" :size="36" color="#E97E3A" />
        </div>
        <div class="game_info">
          <span class="game_name">翻牌配对</span>
          <span class="game_desc">记忆翻牌，锻炼记忆力</span>
        </div>
        <span class="game_arrow">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>

      <button class="game_card" @click="openGame('rule')">
        <div class="game_icon" style="background: rgba(107, 142, 90, 0.12);">
          <Icon name="search" :size="36" color="#6B8E5A" />
        </div>
        <div class="game_info">
          <span class="game_name">分类小能手</span>
          <span class="game_desc">逻辑分类，锻炼推理力</span>
        </div>
        <span class="game_arrow">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>

      <button class="game_card" @click="openGame('word')">
        <div class="game_icon" style="background: rgba(214, 79, 79, 0.12);">
          <Icon name="positive_practice" :size="36" color="#D64F4F" />
        </div>
        <div class="game_info">
          <span class="game_name">看图说词</span>
          <span class="game_desc">识别图片，锻炼语言力</span>
        </div>
        <span class="game_arrow">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>

      <button class="game_card" @click="openGame('diff')">
        <div class="game_icon" style="background: rgba(240, 199, 94, 0.12);">
          <Icon name="activities" :size="36" color="#F0C75E" />
        </div>
        <div class="game_info">
          <span class="game_name">找不同</span>
          <span class="game_desc">观察对比，锻炼注意力</span>
        </div>
        <span class="game_arrow">
          <Icon name="back" :size="18" color="var(--color-text-light)" class="arrow_flip" />
        </span>
      </button>
    </div>

    <BottomNav />
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import BottomNav from '@/components/BottomNav.vue'

export default {
  name: 'BrainGamePlay',

  components: { Icon, BottomNav },

  setup() {
    const router = useRouter()

    const goBack = () => router.push('/brain-game')

    const openGame = (type) => {
      const gameRoutes = { flip: 'flip', rule: 'rule', word: 'word', diff: 'diff' }
      const page = gameRoutes[type]
      if (page) {
        router.push(`/brain-game/play/${page}`)
      }
    }

    return { goBack, openGame }
  },
}
</script>

<style scoped>
.play_page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 0 20px calc(110px + env(safe-area-inset-bottom)) 20px;
  max-width: 480px;
  margin: 0 auto;
}

.play_header {
  display: flex;
  align-items: center;
  padding: 16px 0 20px;
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

.play_title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
  margin-left: 8px;
}

/* ---- 游戏列表 ---- */
.game_list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.game_card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 18px 16px;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}

.game_card:active {
  transform: scale(0.98);
}

.game_icon {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.game_info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.game_name {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text);
}

.game_desc {
  font-size: 0.875rem;
  color: var(--color-text-light);
}

.arrow_flip { transform: scaleX(-1); }
</style>
