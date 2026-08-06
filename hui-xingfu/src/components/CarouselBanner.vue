<template>
  <div class="carousel_banner">
    <div class="carousel_viewport" :style="{ transform: `translateX(-${activeIndex * 100}%)` }">
      <div
        v-for="item in items"
        :key="item.id"
        class="carousel_slide"
        @click="handleClick(item)"
      >
        <img
          v-if="item.image"
          :src="item.image"
          :alt="item.title || '轮播图'"
          class="carousel_image"
        />
        <div v-else class="carousel_placeholder" :style="{ background: item.bg || '#f6e9d9' }">
          <span class="carousel_number">{{ item.number || item.id }}</span>
        </div>
        <div class="carousel_content" v-if="item.title">
          <div class="carousel_title">{{ item.title }}</div>
          <div class="carousel_subtitle" v-if="item.subtitle">{{ item.subtitle }}</div>
        </div>
      </div>
    </div>

    <div class="carousel_dots" v-if="items.length > 1">
      <button
        v-for="(item, index) in items"
        :key="item.id"
        type="button"
        class="carousel_dot"
        :class="{ active: index === activeIndex }"
        @click="setIndex(index)"
        :aria-label="`切换到第 ${index + 1} 张`"
      />
    </div>
  </div>
</template>

<script>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

export default {
  name: 'CarouselBanner',

  props: {
    items: {
      type: Array,
      default: () => [],
    },
  },

  emits: ['item-click'],

  setup(props, { emit }) {
    const activeIndex = ref(0)
    let timer = null

    const setIndex = (index) => {
      activeIndex.value = index
    }

    const handleClick = (item) => {
      emit('item-click', item)
    }

    const startAuto = () => {
      if (timer) clearInterval(timer)
      if (props.items.length <= 1) return
      timer = setInterval(() => {
        activeIndex.value = (activeIndex.value + 1) % props.items.length
      }, 4500)
    }

    watch(
      () => props.items,
      (items) => {
        if (items && items.length > 0) {
          activeIndex.value = 0
          startAuto()
        }
      },
      { immediate: true }
    )

    onMounted(() => {
      if (props.items && props.items.length > 0) {
        startAuto()
      }
    })

    onBeforeUnmount(() => {
      if (timer) clearInterval(timer)
    })

    return {
      activeIndex,
      setIndex,
      handleClick,
    }
  },
}
</script>

<style scoped>
.carousel_banner {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 2px solid var(--color-border);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  margin-bottom: 16px;
}

.carousel_viewport {
  display: flex;
  transition: transform 0.35s ease;
}

.carousel_slide {
  min-width: 100%;
  cursor: pointer;
  user-select: none;
}

.carousel_image {
  width: 100%;
  height: 184px;
  object-fit: cover;
  display: block;
}

.carousel_placeholder {
  width: 100%;
  height: 184px;
  display: grid;
  place-items: center;
  position: relative;
}

.carousel_number {
  font-size: 4rem;
  font-weight: 900;
  color: rgba(58, 46, 37, 0.8);
}

.carousel_content {
  padding: 14px 16px 16px;
}

.carousel_title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.carousel_subtitle {
  font-size: 0.95rem;
  color: var(--color-text-light);
  line-height: 1.6;
}

.carousel_dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 8px 0 10px;
}

.carousel_dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: var(--color-border);
  cursor: pointer;
  transition: background 0.2s;
}

.carousel_dot.active {
  background: var(--color-primary);
}
</style>
