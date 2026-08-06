<template>
  <div class="detail_page">
    <!-- 顶部导航 -->
    <div class="detail_header">
      <button class="back_btn" @click="goBack" aria-label="返回上一页">
        <Icon name="back" :size="22" color="var(--color-text)" aria-hidden="true" />
        <span>返回</span>
      </button>
      <span class="detail_title">{{ pageTitle }}</span>
      <!-- 喇叭朗读图标（UI 占位，暂不实现功能） -->
      <button class="speaker_btn" aria-label="朗读" title="朗读功能即将上线">
        <Icon name="speaker" :size="22" color="var(--color-text-light)" aria-hidden="true" />
      </button>
    </div>

    <!-- 内容区 -->
    <div class="content_area">
      <div class="content_card">
        <div class="content_illustration" :style="{ background: pageColor + '18' }">
          <Icon :name="pageIcon" :size="56" :color="pageColor" aria-hidden="true" />
        </div>
        <h2 class="content_title">{{ pageTitle }}</h2>
        <p class="content_desc">{{ pageDesc }}</p>
      </div>

      <div class="info_list">
        <div class="info_item" v-for="(item, idx) in infoItems" :key="idx">
          <span class="info_dot" :style="{ background: pageColor }"></span>
          <span class="info_text">{{ item }}</span>
        </div>
      </div>
    </div>

    <BottomNav />
  </div>
</template>

<script>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Icon from '@/components/Icon.vue'
import BottomNav from '@/components/BottomNav.vue'

const PAGE_MAP = {
  health: {
    title: '慢病管理',
    desc: '慢病管理，心中有数。规律监测血压、控制血糖、按时服药，让健康数据一目了然。',
    icon: 'leaf',
    color: '#6B8E5A',
    items: ['每日血压监测记录', '血糖控制目标管理', '用药提醒与记录', '定期复查提醒'],
  },
  brain: {
    title: '大脑不老',
    desc: '大脑不老，常用常灵。通过阅读、记忆训练与深度思考，保持大脑活力，延缓认知衰退。',
    icon: 'brain_games',
    color: '#E97E3A',
    items: ['每日阅读推荐', '记忆力训练游戏', '逻辑推理练习', '语言表达训练'],
  },
  companion: {
    title: '心有陪伴',
    desc: '心有陪伴，老有所安。多聊天、常联系、早提醒，让关爱不再缺席，让心灵始终温暖。',
    icon: 'community',
    color: '#7AA5B5',
    items: ['家人连线提醒', '社区活动通知', '心理咨询预约', '关怀回访记录'],
  },
}

export default {
  name: 'DetailPage',

  components: { Icon, BottomNav },

  props: {
    id: { type: String, default: '' },
  },

  setup(props) {
    const router = useRouter()
    const route = useRoute()
    const detailId = props.id || route.params.id || 'health'
    const page = PAGE_MAP[detailId] || PAGE_MAP.health

    const pageTitle = computed(() => page.title)
    const pageDesc = computed(() => page.desc)
    const pageIcon = computed(() => page.icon)
    const pageColor = computed(() => page.color)
    const infoItems = computed(() => page.items)

    const goBack = () => router.push('/')

    return { pageTitle, pageDesc, pageIcon, pageColor, infoItems, goBack }
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

/* ---- 内容区 ---- */
.content_area {
  padding-top: 4px;
}

.content_card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 28px 20px;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
  text-align: center;
  margin-bottom: 16px;
}

.content_illustration {
  width: 96px;
  height: 96px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content_title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  font-family: var(--font-serif);
  margin-bottom: 10px;
}

.content_desc {
  font-size: 0.9375rem;
  color: var(--color-text-light);
  line-height: 1.7;
}

/* ---- 详情列表 ---- */
.info_list {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 6px 0;
  border: 2px solid var(--color-border);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.info_item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px dashed var(--color-border);
}

.info_item:last-child { border-bottom: none; }

.info_dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.info_text {
  font-size: 1rem;
  color: var(--color-text);
}
</style>
