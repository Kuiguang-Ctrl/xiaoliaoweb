<template>
  <transition name="modal_fade">
    <div
      v-if="visible"
      class="modal_mask"
      role="dialog"
      aria-modal="true"
      aria-labelledby="elder_settings_title"
      @click.self="close"
    >
      <div class="modal_card">
        <button
          class="modal_close"
          aria-label="关闭"
          @click="close"
        >×</button>

        <h2 id="elder_settings_title" class="modal_title">长辈版设置</h2>
        <p class="modal_hint">可放大字体，让屏幕上的字更容易看清。</p>

        <div class="setting_group">
          <span class="setting_label">字体大小</span>
          <div class="scale_options">
            <button
              v-for="item in fontScales"
              :key="item.value"
              class="scale_btn"
              :class="{ active: currentScale === item.value }"
              :aria-pressed="currentScale === item.value"
              @click="onPickScale(item.value)"
            >
              <span class="scale_preview" :style="{ fontSize: previewSize(item.value) }">字</span>
              <span class="scale_name">{{ item.label }}</span>
              <span
                v-if="currentScale === item.value"
                class="scale_check"
                aria-hidden="true"
              >✓ 已选中</span>
            </button>
          </div>
        </div>

        <div class="setting_group">
          <span class="setting_label">高对比度</span>
          <button
            class="toggle_btn"
            :class="{ on: highContrast }"
            :aria-pressed="highContrast"
            @click="toggleContrast"
          >
            <span class="toggle_text">{{ highContrast ? '已开启' : '已关闭' }}</span>
          </button>
        </div>

        <button class="modal_done" @click="close">完成</button>
      </div>
    </div>
  </transition>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import {
  FONT_SCALES,
  getFontScale,
  setFontScale,
  getHighContrast,
  setHighContrast,
} from '@/utils/accessibility'

export default {
  name: 'ElderSettingsModal',

  props: {
    visible: { type: Boolean, default: false },
  },

  emits: ['close'],

  setup(props, { emit }) {
    const fontScales = FONT_SCALES
    const currentScale = ref(getFontScale())
    const highContrast = ref(getHighContrast())

    const previewSize = (scale) => `${16 * scale}px`

    const onPickScale = (value) => {
      currentScale.value = value
      setFontScale(value)
    }

    const toggleContrast = () => {
      highContrast.value = !highContrast.value
      setHighContrast(highContrast.value)
    }

    const close = () => emit('close')

    const onKey = (e) => {
      if (e.key === 'Escape' && props.visible) close()
    }

    onMounted(() => window.addEventListener('keydown', onKey))
    onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

    return { fontScales, currentScale, highContrast, previewSize, onPickScale, toggleContrast, close }
  },
}
</script>

<style scoped>
.modal_mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 300;
}

.modal_card {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border-radius: 24px;
  padding: 28px 24px 24px 24px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}

.modal_close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 48px;
  height: 48px;
  border: none;
  background: transparent;
  font-size: 2rem;
  color: #5a4a3a;
  border-radius: 50%;
}

.modal_close:hover {
  background: #f5ede6;
}

.modal_title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #3d2c1e;
  margin-bottom: 6px;
}

.modal_hint {
  font-size: 1rem;
  color: #5a4a3a;
  margin-bottom: 20px;
}

.setting_group {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0e6dc;
}

.setting_group:last-of-type {
  border-bottom: none;
}

.setting_label {
  display: block;
  font-size: 1.125rem;
  font-weight: 600;
  color: #3d2c1e;
  margin-bottom: 12px;
}

.scale_options {
  display: flex;
  gap: 8px;
}

.scale_btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 8px 12px 8px;
  border: 2px solid #e8ddd0;
  border-radius: 16px;
  background: #fcf8f4;
  min-height: 96px;
  min-width: 88px;
}

.scale_btn:hover {
  border-color: #f0b090;
  background: #fff6f0;
}

.scale_btn.active {
  border-color: #e35f3a;
  background: #fef0e8;
  box-shadow: 0 4px 12px rgba(227, 95, 58, 0.2);
}

.scale_preview {
  color: #3d2c1e;
  margin-bottom: 4px;
}

.scale_name {
  font-size: 1rem;
  color: #5a4a3a;
  font-weight: 500;
}

.scale_check {
  font-size: 0.875rem;
  color: #2e7d32;
  margin-top: 4px;
  font-weight: 600;
}

.toggle_btn {
  width: 100%;
  padding: 14px 20px;
  border: 2px solid #e8ddd0;
  border-radius: 16px;
  background: #fcf8f4;
  font-size: 1.125rem;
  color: #5a4a3a;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
}

.toggle_btn.on {
  border-color: #2e7d32;
  background: #e8f5e9;
  color: #2e7d32;
}

.toggle_text {
  font-weight: 600;
}

.modal_done {
  width: 100%;
  padding: 16px;
  border-radius: 60px;
  font-size: 1.375rem;
  font-weight: 600;
  border: none;
  color: #ffffff;
  background: #e35f3a;
  margin-top: 8px;
  min-height: 56px;
}

.modal_done:active {
  transform: scale(0.96);
}

.modal_fade-enter-active {
  animation: fadeIn 0.25s ease;
}
.modal_fade-leave-active {
  animation: fadeIn 0.2s ease reverse;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
