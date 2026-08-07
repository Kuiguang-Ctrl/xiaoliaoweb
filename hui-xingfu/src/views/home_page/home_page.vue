<template>
  <div class="home_page">
    <!-- 顶部：日期 + 天气 -->
    <header class="header_area">
      <div class="datetime_group">
        <span class="date_text">{{ currentDate }}</span>
        <span class="weekday_text">{{ currentWeekday }}</span>
      </div>
      <div class="weather_group">
        <Icon
          :name="homeConfig.weatherKey"
          :size="20"
          color="var(--color-accent-yellow)"
          aria-hidden="true"
        />
        <span class="weather_text">{{ homeConfig.location }} · {{ homeConfig.weather }}</span>
      </div>
    </header>

    <!-- 今日大卡片：问候 + 关怀（包含快捷签到） -->
    <section class="today_card">
      <div class="today_greeting">
        <span class="season_badge">{{ homeConfig.season }} · 宜静养</span>
        <h1 class="greeting_text">{{ homeConfig.greeting }}</h1>
        <p class="today_quote">立秋已至，记得添件薄衫，喝杯温水。</p>
      </div>
      <div class="today_actions">
        <button type="button" class="signin_btn" @click="openQuickSignin">
          {{ todaySigninStatus.signed ? '已签到' : '签到' }}
        </button>
        <button type="button" class="detail_btn" @click="goToSigninDetail">
          查看详情
        </button>
      </div>
    </section>

    <!-- 轮播 Banner -->
    <CarouselBanner :items="carouselItems" @item-click="handleBannerClick" />

    <!-- 8个功能入口（小方块，不放大） -->
    <section class="function_grid" aria-label="核心功能">
      <button
        v-for="item in featureActions"
        :key="item.key"
        type="button"
        class="function_tile"
        :aria-label="item.label"
        @click="handleQuickAction(item.key)"
      >
        <span class="function_icon" :style="{ background: item.color + '18' }">
          <Icon :name="item.icon" :size="22" :color="item.color" />
        </span>
        <span class="function_label">{{ item.label }}</span>
      </button>
    </section>

    <BottomNav />

    <!-- 长辈版浮动按钮 -->
    <button
      type="button"
      class="elder_fab"
      aria-label="长辈版设置"
      @click="openElderSettings"
    >
      <span class="elder_fab_letter" aria-hidden="true">大</span>
      <span class="elder_fab_text">长辈版</span>
    </button>

    <!-- Toast -->
    <transition name="toast_fade">
      <div v-if="showToast" class="toast_message" role="status" aria-live="polite">
        <div class="toast_content">
          <span class="toast_icon" aria-hidden="true">
            <Icon name="check" :size="24" color="var(--color-secondary)" />
          </span>
          <span class="toast_text">{{ toastText }}</span>
        </div>
      </div>
    </transition>

    <!-- 长辈版设置弹窗 -->
    <ElderSettingsModal :visible="showElderSettings" @close="closeElderSettings" />

    <!-- 快速签到弹窗 -->
    <QuickSigninModal
      :visible="showQuickSignin"
      @close="showQuickSignin = false"
      @signed="handleSigned"
    />
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import ElderSettingsModal from '@/components/ElderSettingsModal.vue'
import QuickSigninModal from '@/components/QuickSigninModal.vue'
import BottomNav from '@/components/BottomNav.vue'
import Icon from '@/components/Icon.vue'
import CarouselBanner from '@/components/CarouselBanner.vue'
import { checkTodaySignin } from '@/api/signin'
import img1 from '@/assets/1.png'
import img2 from '@/assets/2.png'
import img3 from '@/assets/3.png'
import { HOME_CONFIG, QUICK_ACTIONS } from './home_config'

export default {
  name: 'home_page',

  components: { ElderSettingsModal, QuickSigninModal, BottomNav, Icon, CarouselBanner },

  setup() {
    const homeConfig = reactive({ ...HOME_CONFIG })
    const featureActions = QUICK_ACTIONS

    // 轮播图数据（使用assets中的真实图片）
    const carouselItems = [
      { id: 'health', image: img1, title: '慢病管理，心中有数', subtitle: '测血压 / 控血糖 / 规律服药' },
      { id: 'brain', image: img2, title: '大脑不老，常用常灵', subtitle: '多阅读 / 练记忆 / 常思考' },
      { id: 'companion', image: img3, title: '心有陪伴，老有所安', subtitle: '多聊天 / 常联系 / 早提醒' },
    ]

    const streakDays = ref(0)
    const showToast = ref(false)
    const toastText = ref('')
    const todaySigninStatus = reactive({ signed: false, emotion: null })
    const showElderSettings = ref(false)
    const showQuickSignin = ref(false)
    let toastTimer = null
    const router = useRouter()

    const currentDate = computed(() => {
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      const day = String(now.getDate()).padStart(2, '0')
      return `${year}年${month}月${day}日`
    })

    const currentWeekday = computed(() => {
      const weekdays = ['日', '一', '二', '三', '四', '五', '六']
      return `星期${weekdays[new Date().getDay()]}`
    })

    const openElderSettings = () => { showElderSettings.value = true }
    const closeElderSettings = () => { showElderSettings.value = false }

    const goToSigninDetail = () => {
      router.push('/signin')
    }

    const openQuickSignin = () => {
      showQuickSignin.value = true
    }

    const handleBannerClick = (item) => {
      router.push(`/detail/${item.id}`)
    }

    const handleQuickAction = (key) => {
      // 只有 M2 脑力小游戏实现真实跳转
      if (key === 'brain_games') {
        router.push('/brain-game')
        return
      }
      const action = QUICK_ACTIONS.find((a) => a.key === key)
      toastText.value = `「${action?.label || key}」模块正在建设中，敬请期待`
      showToast.value = true
      if (toastTimer) clearTimeout(toastTimer)
      toastTimer = setTimeout(() => { showToast.value = false }, 2000)
    }

    const handleSigned = ({ streak, emotion }) => {
      streakDays.value = streak
      todaySigninStatus.signed = true
      todaySigninStatus.emotion = emotion
    }

    const refreshSigninStatus = async () => {
      try {
        const res = await checkTodaySignin()
        todaySigninStatus.signed = res.data.signed
        todaySigninStatus.emotion = res.data.emotion
        streakDays.value = res.data.streak || 0
      } catch {
        todaySigninStatus.signed = false
        todaySigninStatus.emotion = null
      }
    }

    onMounted(() => { refreshSigninStatus() })

    onBeforeUnmount(() => {
      if (toastTimer) clearTimeout(toastTimer)
    })

    return {
      homeConfig,
      featureActions,
      carouselItems,
      streakDays,
      showToast,
      toastText,
      currentDate,
      currentWeekday,
      todaySigninStatus,
      showElderSettings,
      showQuickSignin,
      goToSigninDetail,
      openQuickSignin,
      handleSigned,
      handleQuickAction,
      handleBannerClick,
      openElderSettings,
      closeElderSettings,
    }
  },
}
</script>

<style scoped>
.home_page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 0 20px calc(110px + env(safe-area-inset-bottom)) 20px;
  max-width: 480px;
  margin: 0 auto;
  font-size: 1.125rem;
  color: var(--color-text);
  position: relative;
}

.home_page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 10%, rgba(240, 199, 94, 0.06), transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(107, 142, 90, 0.05), transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.home_page > * {
  position: relative;
  z-index: 1;
}

/* ---- 顶部 ---- */
.header_area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 4px 14px 4px;
  gap: 8px;
  flex-wrap: wrap;
}

.datetime_group {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.date_text {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.5px;
  font-family: var(--font-serif);
}

.weekday_text {
  font-size: 0.9375rem;
  color: var(--color-text-soft);
  background: var(--color-bg-card);
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  border: 1.5px solid var(--color-border);
}

.weather_group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--color-bg-card);
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  border: 1.5px solid var(--color-border);
}

.weather_text {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-soft);
}

/* ---- 今日大卡片（仅问候，不含签到） ---- */
.today_card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  margin: 0 0 14px 0;
  border: 2px solid var(--color-border);
  position: relative;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.today_card::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 90px;
  height: 90px;
  background: var(--color-primary-soft);
  border-radius: 50%;
  opacity: 0.5;
  z-index: 0;
}

.today_greeting {
  position: relative;
  z-index: 1;
}

.season_badge {
  display: inline-block;
  background: var(--color-secondary-soft);
  color: var(--color-secondary);
  font-size: 0.8125rem;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  border: 1.5px solid var(--color-secondary);
  font-weight: 500;
  margin-bottom: 10px;
}

.greeting_text {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
  line-height: 1.3;
  margin-bottom: 6px;
}

.today_quote {
  font-size: 0.9375rem;
  color: var(--color-text-soft);
  line-height: 1.6;
  font-family: var(--font-serif);
}

.today_actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
}

/* ---- 签到按钮（缩小版，内嵌在 today_card 中） ---- */
.signin_btn,
.detail_btn {
  border: none;
  border-radius: var(--radius-pill);
  padding: 8px 16px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  font-family: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}

.signin_btn {
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 3px 10px rgba(233, 126, 58, 0.22);
}

.signin_btn:active {
  transform: scale(0.96);
}

.detail_btn {
  background: var(--color-bg-soft);
  color: var(--color-text);
  border: 1.5px solid var(--color-border);
}

.detail_btn:active {
  background: var(--color-bg-warm);
}

/* ---- 功能区：8个小方块 ---- */
.function_grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 90px;
}

.function_tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 6px;
  min-height: 72px;
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  border: 1.5px solid var(--color-border);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  font-family: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}

.function_tile:active {
  transform: scale(0.96);
}

.function_icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.function_label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.3;
  text-align: center;
}

/* ---- Toast ---- */
.toast_message {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: var(--color-text);
  border-radius: var(--radius-md);
  padding: 14px 22px;
  z-index: 200;
  pointer-events: none;
  max-width: 80%;
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
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}

.toast_text {
  font-size: 1rem;
  font-weight: 500;
  color: #ffffff;
}

.toast_fade-enter-active {
  animation: popIn 0.3s ease forwards;
}
.toast_fade-leave-active {
  animation: popOut 0.25s ease forwards;
}

@keyframes popIn {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.85); }
  100% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
}

@keyframes popOut {
  0% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(0.9); }
}

/* ---- 长辈版浮动按钮 ---- */
.elder_fab {
  position: fixed;
  right: 16px;
  bottom: 92px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 18px;
  height: 52px;
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-pill);
  background: var(--color-bg-card);
  color: var(--color-primary-deep);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
  z-index: 90;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  font-family: inherit;
}

.elder_fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(233, 126, 58, 0.25);
}

.elder_fab:active {
  transform: scale(0.96);
}

.elder_fab_letter {
  font-size: 1.375rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-primary);
}

.elder_fab_text {
  font-size: 1rem;
  font-weight: 600;
}

/* ---- 响应式 ---- */
@media (max-width: 420px) {
  .date_text { font-size: 1.25rem; }
  .greeting_text { font-size: 1.25rem; }
  .function_grid { gap: 8px; }
  .function_tile { padding: 10px 4px; min-height: 72px; }
  .function_icon { width: 36px; height: 36px; }
  .function_label { font-size: 0.7rem; }
}

@media (max-width: 360px) {
  .date_text { font-size: 1.125rem; }
  .greeting_text { font-size: 1.125rem; }
  .today_quote { font-size: 0.875rem; }
  .function_label { font-size: 0.6875rem; }
  .signin_btn, .detail_btn { padding: 6px 12px; font-size: 0.8125rem; }
  .elder_fab { padding: 0 14px; height: 46px; }
  .elder_fab_letter { font-size: 1.25rem; }
}

.home_page::after {
  content: '';
  display: block;
  height: 20px;
  visibility: hidden;
}
</style>
