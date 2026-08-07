<template>
  <div class="signin_page">
    <!-- 顶部导航 -->
    <div class="signin_header">
      <button class="back_btn" @click="goBack" aria-label="返回上一页">
        <Icon name="back" :size="22" color="var(--color-text)" aria-hidden="true" />
        <span>返回</span>
      </button>
      <span class="signin_title">签到详情</span>
    </div>

    <!-- 签到状态 -->
    <div class="status_section">
      <div class="signin_card">
        <div class="signin_main">
          <div class="signed_badge" :class="{ done: isSignedToday }">
            <Icon v-if="isSignedToday" name="check" :size="28" color="#FFFFFF" aria-hidden="true" />
            <span>{{ isSignedToday ? '已签到' : '今日未签到' }}</span>
          </div>
          <div class="streak_group">
            <span class="streak_label">连续签到</span>
            <span class="streak_number" :class="{ bounce: streakChanged }">{{ streakDays }}</span>
            <span class="streak_unit">天</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 心情修改区 -->
    <div class="emotion_section">
      <div class="section_title_row">
        <h2 class="section_title">今天心情怎么样？</h2>
      </div>
      <div class="emotion_list" role="group" aria-label="选择今天的心情">
        <button
          v-for="item in emotionOptions"
          :key="item.value"
          class="emotion_item"
          :class="{ selected: selectedEmotion === item.value }"
          :aria-pressed="selectedEmotion === item.value"
          @click="handleEmotionSelect(item.value)"
        >
          <span class="emotion_icon" aria-hidden="true">
            <Icon :name="item.iconName" :size="32" :color="item.color" />
          </span>
          <span class="emotion_name">{{ item.label }}</span>
        </button>
      </div>
      <transition name="fade">
        <div v-if="emotionMessage" class="emotion_message">
          <span class="emotion_msg_text">{{ emotionMessage }}</span>
        </div>
      </transition>
    </div>

    <!-- 心情日历（含节假日 & 节气） -->
    <div class="calendar_section">
      <div class="calendar_header">
        <h2 class="calendar_title">
          <Icon name="calendar" :size="20" color="var(--color-primary)" aria-hidden="true" />
          情绪月历
        </h2>
        <div class="month_nav">
          <button class="month_btn" @click="changeMonth(-1)" aria-label="上个月">‹</button>
          <span class="calendar_month">{{ currentYearMonth }}</span>
          <button class="month_btn" @click="changeMonth(1)" aria-label="下个月">›</button>
        </div>
      </div>
      <div class="calendar_grid">
        <div class="day_label" v-for="d in weekDays" :key="d">{{ d }}</div>
        <div
          v-for="(day, idx) in calendarDays"
          :key="idx"
          class="day_cell"
          :class="dayCellClass(day)"
        >
          <span class="day_num">{{ day.label }}</span>
          <!-- 节假日标记 -->
          <span v-if="day.holiday" class="day_badge holiday_badge">{{ day.holiday }}</span>
          <!-- 节气标记 -->
          <span v-else-if="day.term" class="day_badge term_badge">{{ day.term }}</span>
          <!-- 情绪图标 -->
          <span v-if="day.emotion && !day.empty" class="day_icon" aria-hidden="true">
            <Icon :name="day.emotion" :size="18" :color="emotionColor(day.emotion)" />
          </span>
        </div>
      </div>
      <!-- 图例 -->
      <div class="calendar_footer">
        <div class="calendar_legend">
          <span class="legend_item"><span class="legend_dot holiday_dot"></span>节假日</span>
          <span class="legend_item"><span class="legend_dot term_dot"></span>节气</span>
        </div>
      </div>
    </div>

    <BottomNav />
  </div>
</template>

<script>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import BottomNav from '@/components/BottomNav.vue'
import { checkTodaySignin, getSigninHistory, doSignin } from '@/api/signin'

/* ---- 2026 年中国法定节假日 ---- */
const HOLIDAYS_2026 = {
  '01-01': '元旦', '01-02': '元旦',
  '02-16': '除夕', '02-17': '春节', '02-18': '春节', '02-19': '春节',
  '02-20': '春节', '02-21': '春节', '02-22': '春节', '02-23': '春节',
  '04-05': '清明', '04-06': '清明',
  '05-01': '劳动节', '05-02': '劳动节', '05-03': '劳动节', '05-04': '劳动节', '05-05': '劳动节',
  '05-31': '端午', '06-01': '端午',
  '10-01': '国庆', '10-02': '国庆', '10-03': '国庆', '10-04': '中秋',
  '10-05': '国庆', '10-06': '国庆', '10-07': '国庆', '10-08': '国庆',
}

/* ---- 2026 年二十四节气 ---- */
const SOLAR_TERMS_2026 = {
  '01-05': '小寒', '01-20': '大寒', '02-04': '立春', '02-18': '雨水',
  '03-05': '惊蛰', '03-20': '春分', '04-05': '清明', '04-20': '谷雨',
  '05-05': '立夏', '05-21': '小满', '06-05': '芒种', '06-21': '夏至',
  '07-07': '小暑', '07-22': '大暑', '08-07': '立秋', '08-23': '处暑',
  '09-07': '白露', '09-23': '秋分', '10-08': '寒露', '10-23': '霜降',
  '11-07': '立冬', '11-22': '小雪', '12-07': '大雪', '12-21': '冬至',
}

export default {
  name: 'signin',
  components: { Icon, BottomNav },

  setup() {
    const router = useRouter()

    const isSignedToday = ref(false)
    const streakDays = ref(0)
    const streakChanged = ref(false)
    const selectedEmotion = ref(null)
    const emotionMessage = ref('')
    const signinHistory = reactive({})
    const now = new Date()
    const currentYear = ref(now.getFullYear())
    const currentMonth = ref(now.getMonth() + 1)

    const weekDays = ['一', '二', '三', '四', '五', '六', '日']

    const emotionOptions = [
      { value: 'sunny', iconName: 'sunny', color: '#F0C75E', label: '很开心', message: '看到你开心，我也跟着高兴。' },
      { value: 'cloudy', iconName: 'cloudy', color: '#7AA5B5', label: '还不错', message: '平平淡淡才是真，今天也很好。' },
      { value: 'overcast', iconName: 'overcast', color: '#8B7B6D', label: '一般般', message: '明天会更好，我陪着你。' },
      { value: 'rainy', iconName: 'rainy', color: '#5A7886', label: '有点低落', message: '没关系，谁都有不开心的时候。' },
      { value: 'stormy', iconName: 'stormy', color: '#6B5B7A', label: '不开心', message: '来，跟我说说，我一直在。' },
    ]

    const emotionColorMap = {
      sunny: '#F0C75E', cloudy: '#7AA5B5', overcast: '#8B7B6D',
      rainy: '#5A7886', stormy: '#6B5B7A',
    }
    const emotionColor = (key) => emotionColorMap[key] || 'var(--color-text-light)'

    const currentYearMonth = computed(() => `${currentYear.value}年${currentMonth.value}月`)

    const calendarDays = computed(() => {
      const days = []
      const firstDay = new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
      const daysInMonth = new Date(currentYear.value, currentMonth.value, 0).getDate()
      const today = new Date().getDate()

      const offset = firstDay === 0 ? 6 : firstDay - 1
      for (let i = 0; i < offset; i++) {
        days.push({ label: '', empty: true, emotion: null, isToday: false, holiday: null, term: null })
      }

      const isCurrentMonth =
        currentMonth.value === new Date().getMonth() + 1 && currentYear.value === new Date().getFullYear()

      for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`
        const mmdd = `${String(currentMonth.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`
        const record = signinHistory[dateStr]

        days.push({
          label: d, empty: false,
          emotion: record?.emotion || null,
          isToday: isCurrentMonth && d === today,
          holiday: HOLIDAYS_2026[mmdd] || null,
          term: SOLAR_TERMS_2026[mmdd] || null,
        })
      }
      return days
    })

    const dayCellClass = (day) => {
      if (day.empty) return 'empty'
      const classes = []
      if (day.emotion) classes.push('emotion_' + day.emotion)
      if (day.holiday) classes.push('has_holiday')
      if (day.term) classes.push('has_term')
      if (day.isToday) classes.push('today')
      return classes
    }

    /* ---- 从后端加载数据（今日状态 + 当前月月历） ---- */
    const loadSigninData = async () => {
      try {
        const today = await checkTodaySignin()
        isSignedToday.value = today.data.signed
        selectedEmotion.value = today.data.emotion
        streakDays.value = today.data.streak
      } catch (e) {
        console.warn('加载签到状态失败:', e)
      }
      await loadMonthCalendar()
    }

    /* ---- 加载指定月份的情绪月历 ---- */
    const loadMonthCalendar = async () => {
      try {
        const history = await getSigninHistory(currentYear.value, currentMonth.value)
        Object.keys(signinHistory).forEach((k) => delete signinHistory[k])
        ;(history.data || []).forEach((h) => {
          signinHistory[h.date] = { date: h.date, emotion: h.emotion }
        })
      } catch (e) {
        console.warn('加载月历失败:', e)
      }
    }

    /* ---- 切换月份（上一月/下一月） ---- */
    const changeMonth = async (delta) => {
      let y = currentYear.value
      let m = currentMonth.value + delta
      if (m < 1) { m = 12; y -= 1 }
      if (m > 12) { m = 1; y += 1 }
      currentYear.value = y
      currentMonth.value = m
      await loadMonthCalendar()
    }

    /* ---- 选择/修改情绪（同步到后端） ---- */
    const handleEmotionSelect = async (value) => {
      selectedEmotion.value = value
      const selected = emotionOptions.find((item) => item.value === value)
      if (selected) {
        emotionMessage.value = selected.message
        setTimeout(() => { emotionMessage.value = '' }, 2800)
      }

      try {
        // 无论首次签到还是改情绪，都调后端记录
        const res = await doSignin(value)
        streakDays.value = res.data.streak
        isSignedToday.value = true
        const today = new Date()
        const dateStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
        signinHistory[dateStr] = { date: dateStr, emotion: value }
      } catch (e) {
        console.warn('更新情绪失败:', e)
      }
    }

    const goBack = () => router.push('/')

    onMounted(() => { loadSigninData() })

    return {
      isSignedToday, streakDays, streakChanged,
      selectedEmotion, emotionMessage,
      emotionOptions, weekDays, calendarDays, currentYearMonth,
      handleEmotionSelect, dayCellClass, emotionColor, goBack, changeMonth,
    }
  },
}
</script>

<style scoped>
.signin_page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 16px 20px calc(110px + env(safe-area-inset-bottom)) 20px;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
}

.signin_header {
  display: flex;
  align-items: center;
  padding: 12px 0 20px 0;
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

.signin_title {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  margin-left: 8px;
  font-family: var(--font-serif);
}

/* ---- 签到状态 ---- */
.status_section { margin-bottom: 14px; }

.signin_card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
}

.signin_main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.signed_badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 130px;
  min-height: 50px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-pill);
  font-size: 1.125rem;
  font-weight: 600;
  background: var(--color-bg-soft);
  color: var(--color-text-soft);
  font-family: inherit;
}

.signed_badge.done {
  background: var(--color-secondary-soft);
  color: var(--color-secondary);
  border-color: var(--color-secondary);
}

.streak_group {
  display: flex;
  align-items: baseline;
  gap: 4px;
  background: var(--color-bg-warm);
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  border: 1.5px solid var(--color-border);
}

.streak_label { font-size: 0.95rem; color: var(--color-text-soft); }
.streak_number { font-size: 1.75rem; font-weight: 700; color: var(--color-primary); line-height: 1; }
.streak_number.bounce { animation: numberBounce 0.6s ease; }
.streak_unit { font-size: 0.95rem; color: var(--color-text-soft); }

@keyframes numberBounce {
  0% { transform: scale(1); }
  30% { transform: scale(1.4); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

/* ---- 心情选择 ---- */
.emotion_section {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
  margin-bottom: 14px;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
}

.section_title_row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section_title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
}

.emotion_list { display: flex; gap: 6px; justify-content: space-between; }

.emotion_item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-soft);
  cursor: pointer;
  transition: transform 0.15s, border-color 0.15s, background 0.15s;
  flex: 1;
  min-width: 0;
  font-family: inherit;
}

.emotion_item:hover { border-color: var(--color-primary); }
.emotion_item.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  transform: scale(1.04);
  box-shadow: 0 3px 10px rgba(233, 126, 58, 0.14);
}

.emotion_name { font-size: 0.85rem; color: var(--color-text); font-weight: 500; }

.emotion_message {
  margin-top: 14px;
  padding: 10px 14px;
  background: var(--color-bg-warm);
  border-radius: var(--radius-sm);
  border-left: 4px solid var(--color-primary);
  text-align: center;
}
.emotion_msg_text { font-size: 1rem; color: var(--color-primary-deep); font-family: var(--font-serif); }

/* ---- 心情日历 ---- */
.calendar_section {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
}

.calendar_header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.calendar_title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
}

.calendar_month { font-size: 0.95rem; color: var(--color-text-light); }

.month_nav {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.month_btn {
  width: 34px;
  height: 34px;
  border: 1.5px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-bg-card);
  color: var(--color-primary);
  font-size: 1.1rem;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.month_btn:active {
  background: var(--color-bg-warm);
}

.calendar_grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px 3px;
  text-align: center;
}

.day_label { font-size: 0.85rem; color: var(--color-text-light); font-weight: 500; padding: 4px 0; }

.day_cell {
  padding: 3px 0;
  font-size: 0.9rem;
  border-radius: var(--radius-sm);
  background: var(--color-bg-soft);
  color: var(--color-text-soft);
  min-height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1.5px solid transparent;
  position: relative;
}

.day_cell.emotion_sunny { background: #FFF3D6; }
.day_cell.emotion_cloudy { background: #DDE9F0; }
.day_cell.emotion_overcast { background: #E8E0D8; }
.day_cell.emotion_rainy { background: #D6DEE5; }
.day_cell.emotion_stormy { background: #E5DCE5; }
.day_cell.today { border: 2px solid var(--color-primary); background: var(--color-primary-soft) !important; }
.day_cell.empty { background: transparent; opacity: 0.35; }

.day_num { font-weight: 500; }

.day_badge {
  font-size: 0.6rem;
  font-weight: 600;
  padding: 1px 4px;
  border-radius: 4px;
  line-height: 1.3;
  white-space: nowrap;
  margin-top: 2px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.holiday_badge { background: #FFE0E0; color: #C62828; }
.term_badge { background: #E0F0E8; color: #2E7D32; }
.day_icon { display: inline-flex; align-items: center; justify-content: center; margin-top: 1px; }

/* ---- 图例 ---- */
.calendar_footer {
  display: flex;
  justify-content: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1.5px dashed var(--color-border);
}
.calendar_legend { display: flex; gap: 16px; }
.legend_item { display: inline-flex; align-items: center; gap: 4px; font-size: 0.8rem; color: var(--color-text-light); }
.legend_dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.holiday_dot { background: #C62828; }
.term_dot { background: #2E7D32; }

.fade-enter-active { transition: all 0.3s ease; }
.fade-leave-active { transition: all 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
