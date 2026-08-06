<template>
  <div class="game-page">
    <div class="game-header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span class="title">分类小能手</span>
    </div>

    <div class="game-tip">把下面的物品拖到对应的分类里</div>

    <div class="category-area">
      <div class="category-box" @click="selectCategory('fruit')" :class="{ active: selectedCat === 'fruit' }">
        <div class="cat-icon">🍎</div>
        <div class="cat-name">水果</div>
        <div class="cat-count">{{ fruitList.length }} 个</div>
      </div>
      <div class="category-box" @click="selectCategory('vegetable')" :class="{ active: selectedCat === 'vegetable' }">
        <div class="cat-icon">🥬</div>
        <div class="cat-name">蔬菜</div>
        <div class="cat-count">{{ vegList.length }} 个</div>
      </div>
    </div>

    <div class="item-area">
      <div
          v-for="(item, index) in items"
          :key="index"
          class="item-card"
          :class="{ done: item.done }"
          @click="classifyItem(item)"
      >
        {{ item.emoji }}
        <div class="item-name">{{ item.name }}</div>
      </div>
    </div>

    <div class="hint-bar">{{ feedbackText }}</div>

    <div v-if="showFinish" class="finish-card">
      <div class="finish-icon">🎉</div>
      <div class="finish-text">{{ finishResult.cognitiveTip || '全部答对啦！锻炼了你的推理能力' }}</div>
      <div class="finish-score">正确率 {{ score }}%</div>
      <div v-if="finishResult.star !== undefined" class="stars">
        {{ '⭐'.repeat(finishResult.star) }}{{ '☆'.repeat(3 - finishResult.star) }}
      </div>
      <div v-if="finishResult.encourageMsg" class="encourage-msg">{{ finishResult.encourageMsg }}</div>
      <button class="finish-btn" @click="restart">再来一组</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { startGame, finishGame } from '@/api/game'

const selectedCat = ref('')
const fruitList = ref([])
const vegList = ref([])
const items = ref([])
const showFinish = ref(false)
const score = ref(100)
const feedbackText = ref('请选择一个分类，然后点击物品。')
const recordId = ref(null)
const startTime = ref(null)
const finishResult = ref({})
const serverConfig = ref(null)
const wrongCount = ref(0)
const submitting = ref(false)

const defaultItems = [
  { name: '苹果', emoji: '🍎', type: 'fruit' },
  { name: '香蕉', emoji: '🍌', type: 'fruit' },
  { name: '白菜', emoji: '🥬', type: 'vegetable' },
  { name: '葡萄', emoji: '🍇', type: 'fruit' },
  { name: '胡萝卜', emoji: '🥕', type: 'vegetable' },
  { name: '西红柿', emoji: '🍅', type: 'vegetable' }
]

const initGame = async () => {
  fruitList.value = []
  vegList.value = []
  selectedCat.value = ''
  showFinish.value = false
  finishResult.value = {}
  wrongCount.value = 0

  try {
    const data = await startGame(3)
    recordId.value = data.recordId
    startTime.value = Date.now()
    serverConfig.value = JSON.parse(data.configJson)
    items.value = serverConfig.value.items
        .sort(() => Math.random() - 0.5)
        .map(i => ({ ...i, done: false }))
  } catch (e) {
    console.error('[FindRule startGame Error]', e)
    items.value = [...defaultItems].sort(() => Math.random() - 0.5).map(i => ({ ...i, done: false }))
  }
}

const selectCategory = (cat) => {
  selectedCat.value = cat
  feedbackText.value = `当前选择：${cat === 'fruit' ? '水果' : '蔬菜'}，请继续分类。`
}

const classifyItem = (item) => {
  if (item.done) return
  if (!selectedCat.value) {
    feedbackText.value = '请先选择一个分类，再点击物品。'
    return
  }

  item.done = true
  if (item.type === selectedCat.value) {
    if (selectedCat.value === 'fruit') fruitList.value.push(item)
    else vegList.value.push(item)
    feedbackText.value = '分类正确，继续选择下一项。'
  } else {
    wrongCount.value++
    feedbackText.value = '判断错了也没关系，继续下一项吧。'
    const done = fruitList.value.length + vegList.value.length + 1
    score.value = Math.round((done - wrongCount.value) / items.value.length * 100)
    if (score.value < 0) score.value = 0
  }

  const allDone = items.value.every(i => i.done)
  if (allDone) {
    setTimeout(() => {
      submitResult(1)
    }, 500)
  }
}

const submitResult = async (gameStatus) => {
  if (submitting.value) return
  submitting.value = true
  if (!recordId.value) {
    showFinish.value = true
    submitting.value = false
    return
  }
  const costSecond = Math.max(1, Math.round((Date.now() - startTime.value) / 1000))
  const correct = items.value.length - wrongCount.value
  score.value = Math.round((correct / items.value.length) * 100)
  try {
    const result = await finishGame({
      recordId: recordId.value,
      totalQuestion: items.value.length,
      correctQuestion: correct > 0 ? correct : 0,
      wrongQuestion: wrongCount.value,
      costSecond,
      gameStatus,
    })
    finishResult.value = result
  } catch (e) {
    console.error('[FindRule finishGame Error]', e)
  }
  showFinish.value = true
  submitting.value = false
}

const restart = () => {
  initGame()
}

onMounted(() => {
  initGame()
})
</script>

<style scoped>
.game-page { min-height: 100vh; background: #f5f7fa; }
.game-header {
  display: flex; align-items: center; padding: 16px;
  background: #fff; position: sticky; top: 0; z-index: 10;
}
.back { font-size: 20px; margin-right: 12px; cursor: pointer; }
.title { flex: 1; font-size: 18px; font-weight: 600; }

.game-tip { text-align: center; padding: 20px; font-size: 18px; color: #666; }

.category-area {
  display: flex; gap: 16px; padding: 0 20px; margin-bottom: 24px;
}
.category-box {
  flex: 1; background: #fff; border-radius: 12px; padding: 20px;
  text-align: center; cursor: pointer; border: 2px solid transparent;
  transition: all 0.2s;
}
.category-box.active { border-color: #ff7d00; background: #fff7ed; }
.cat-icon { font-size: 40px; margin-bottom: 8px; }
.cat-name { font-size: 18px; font-weight: 500; margin-bottom: 4px; }
.cat-count { font-size: 14px; color: #999; }

.item-area {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 12px; padding: 0 20px;
}

.hint-bar {
  margin: 12px 20px 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff8d8;
  color: #8b5b1e;
  font-size: 0.95rem;
  text-align: center;
}
.item-card {
  background: #fff; border-radius: 12px; padding: 16px 8px;
  text-align: center; font-size: 32px; cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.item-card.done { opacity: 0.4; cursor: default; }
.item-name { font-size: 14px; color: #666; margin-top: 4px; }

.finish-content { text-align: center; padding: 20px 0; }
.finish-icon { font-size: 60px; margin-bottom: 16px; }
.finish-text { font-size: 18px; margin-bottom: 8px; }
.finish-score { font-size: 16px; color: #ff7d00; font-weight: 500; }
</style>