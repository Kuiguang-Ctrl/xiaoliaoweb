<template>
  <teleport to="body">
    <transition name="sheet_fade">
      <div
        v-if="visible"
        class="sheet_mask"
        role="dialog"
        aria-modal="true"
        aria-labelledby="quick_signin_title"
        @click.self="close"
      >
        <div class="sheet_card" @click.stop>
          <button
            type="button"
            class="sheet_close"
            aria-label="关闭"
            @click="close"
          >
            <Icon name="close" :size="20" color="var(--color-text)" aria-hidden="true" />
          </button>

          <div class="sheet_body">
            <div class="modal_title_row">
              <h3 id="quick_signin_title" class="modal_title">今天的心情怎么样？</h3>
            </div>

            <div class="emotion_list" role="group" aria-label="选择今天的心情">
              <button
                v-for="item in emotionOptions"
                :key="item.value"
                type="button"
                class="emotion_item"
                :class="{ selected: selectedEmotion === item.value }"
                :aria-pressed="selectedEmotion === item.value"
                @click="selectedEmotion = item.value"
              >
                <Icon :name="item.iconName" :size="42" :color="item.color" aria-hidden="true" />
                <span class="emotion_name">{{ item.label }}</span>
              </button>
            </div>

            <button
              type="button"
              class="submit_btn"
              @click="handleQuickSignin"
            >
              签到
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import Icon from './Icon.vue'

const STORAGE_KEY = 'hui_signin_data'

const emotionOptions = [
  { value: 'sunny', iconName: 'sunny', color: '#F0C75E', label: '开心' },
  { value: 'cloudy', iconName: 'cloudy', color: '#7AA5B5', label: '还不错' },
  { value: 'overcast', iconName: 'overcast', color: '#8B7B6D', label: '一般般' },
  { value: 'rainy', iconName: 'rainy', color: '#5A7886', label: '有点低落' },
  { value: 'stormy', iconName: 'stormy', color: '#6B5B7A', label: '不开心' },
]

function getSigninData() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch { return {} }
}

function setSigninData(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export function saveSignin(emotion) {
  const data = getSigninData()
  if (!data.history) data.history = {}
  if (!data.streak) data.streak = 0

  const today = new Date()
  const dateStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  // 计算连续签到
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const yestStr = `${yesterday.getFullYear()}-${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`

  if (data.lastDate === yestStr) {
    data.streak += 1
  } else if (data.lastDate !== dateStr) {
    data.streak = 1
  }
  data.lastDate = dateStr

  data.history[dateStr] = { date: dateStr, emotion }
  setSigninData(data)
  return { streak: data.streak, emotion }
}

export default {
  name: 'QuickSigninModal',

  components: { Icon },

  props: {
    visible: { type: Boolean, default: false },
  },

  emits: ['close', 'signed'],

  setup(props, { emit }) {
    const selectedEmotion = ref(null)
    const isSigning = ref(false)

    const handleQuickSignin = () => {
      if (isSigning.value) return
      isSigning.value = true

      const emotion = selectedEmotion.value || 'sunny'
      const result = saveSignin(emotion)

      emit('signed', result)
      close()

      isSigning.value = false
    }

    const close = () => {
      selectedEmotion.value = null
      emit('close')
    }

    const onKey = (e) => {
      if (e.key === 'Escape' && props.visible) close()
    }

    watch(
      () => props.visible,
      (visible) => {
        selectedEmotion.value = null
        if (visible) {
          document.body.style.overflow = 'hidden'
        } else {
          document.body.style.overflow = ''
        }
      }
    )

    onMounted(() => {
      window.addEventListener('keydown', onKey)
    })

    onBeforeUnmount(() => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    })

    return {
      selectedEmotion,
      emotionOptions,
      isSigning,
      handleQuickSignin,
      close,
    }
  },
}
</script>

<style scoped>
.sheet_mask {
  position: fixed;
  inset: 0;
  background: rgba(50, 40, 30, 0.45);
  backdrop-filter: blur(4px);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.sheet_card {
  position: relative;
  background: var(--color-bg);
  width: min(320px, 100%);
  max-width: 320px;
  border-radius: 22px;
  border: 1.5px solid var(--color-border);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.16);
  overflow: hidden;
}

.sheet_close {
  position: absolute;
  right: 12px;
  top: 12px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
}

.sheet_body {
  padding: 28px 20px 24px;
}

.modal_title_row {
  margin-bottom: 22px;
  text-align: center;
}

.modal_title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.emotion_list {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.emotion_item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 10px;
  border: 2px solid var(--color-border);
  border-radius: 16px;
  background: var(--color-bg-card);
  color: var(--color-text);
  cursor: pointer;
  text-align: center;
  transition: transform 0.15s, border-color 0.15s, background 0.15s;
  min-width: 52px;
  font-family: inherit;
}

.emotion_item.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  transform: scale(1.05);
  box-shadow: 0 3px 10px rgba(233, 126, 58, 0.14);
}

.emotion_name {
  font-size: 0.8rem;
  font-weight: 600;
}

.submit_btn {
  width: 100%;
  border: none;
  border-radius: 18px;
  padding: 14px 0;
  font-size: 1.0625rem;
  font-weight: 700;
  color: #fff;
  background: var(--color-primary);
  cursor: pointer;
  transition: transform 0.15s, opacity 0.15s;
  box-shadow: 0 4px 14px rgba(233, 126, 58, 0.25);
}

.submit_btn:active {
  transform: scale(0.97);
}

.sheet_fade-enter-active,
.sheet_fade-leave-active {
  transition: opacity 0.22s ease;
}

.sheet_fade-enter-from,
.sheet_fade-leave-to {
  opacity: 0;
}
</style>
