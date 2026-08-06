<template>
  <div class="find-diff-page">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <span class="back-btn" @click="goBack">← 返回</span>
      <span class="title">找不同 · 注意力训练</span>
      <span class="level">第 {{ level }} 关</span>
    </div>

    <!-- 分数和连击 -->
    <div class="info-bar">
      <div class="info-item">
        <span class="info-label">连击</span>
        <span class="info-value">{{ combo }} 🔥</span>
      </div>
      <div class="info-item">
        <span class="info-label">正确</span>
        <span class="info-value correct">{{ correctCount }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">关卡</span>
        <span class="info-value">{{ level }} / {{ maxLevel }}</span>
      </div>
    </div>

    <!-- 游戏说明 -->
    <div class="tip">
      👇 在下面的格子中，找出<strong>不一样</strong>的那一个
    </div>

    <!-- 游戏网格 -->
    <div class="game-grid" :style="gridStyle">
      <div
          class="grid-cell"
          v-for="(cell, index) in gridData"
          :key="index"
          @click="handleClick(index)"
      >
        {{ cell }}
      </div>
    </div>

    <div v-if="feedbackText" class="feedback-bar" :class="feedbackType">{{ feedbackText }}</div>

    <div v-if="showResult" class="finish-card">
      <div class="finish-icon">{{ resultIcon }}</div>
      <div class="finish-text">{{ resultTitle }}</div>
      <div class="finish-desc">{{ resultDesc }}</div>
      <div v-if="finishResult.star !== undefined" class="stars">
        {{ '⭐'.repeat(finishResult.star) }}{{ '☆'.repeat(3 - finishResult.star) }}
      </div>
      <div v-if="finishResult.encourageMsg" class="encourage-msg">{{ finishResult.encourageMsg }}</div>
      <button v-if="!gameFinished" class="result-btn" @click="nextLevel">
        下一关
      </button>
      <button v-else class="result-btn" @click="goBack">
        返回
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { startGame, finishGame } from '@/api/game'

const router = useRouter()

const level = ref(1)
const maxLevel = ref(4)
const combo = ref(0)
const correctCount = ref(0)
const wrongCount = ref(0)
const showResult = ref(false)
const resultIcon = ref('')
const resultTitle = ref('')
const resultDesc = ref('')
const isAllCorrect = ref(false)
const recordId = ref(null)
const startTime = ref(null)
const finishResult = ref({})
const serverConfig = ref(null)
const roundInLevel = ref(0)
const submitting = ref(false)
const gameFinished = ref(false)

const gridSizes = [3, 4, 5, 6]

const iconPairs = [
  { normal: '🍎', diff: '🍊' },
  { normal: '🌞', diff: '🌙' },
  { normal: '🐱', diff: '🐶' },
  { normal: '🌸', diff: '🌺' },
  { normal: '⭐', diff: '💫' },
  { normal: '🎈', diff: '🎁' },
  { normal: '🍕', diff: '🍔' },
  { normal: '🚗', diff: '🚕' }
]

const gridData = ref([])
const diffIndex = ref(0)

const gridStyle = computed(() => {
  let size
  if (serverConfig.value) {
    size = serverConfig.value.gridSize || (gridSizes[level.value - 1] || 3)
  } else {
    size = gridSizes[level.value - 1] || 3
  }
  return {
    gridTemplateColumns: `repeat(${size}, 1fr)`,
    gridTemplateRows: `repeat(${size}, 1fr)`
  }
})

function generateLevel() {
  const size = serverConfig.value
    ? (serverConfig.value.gridSize || gridSizes[level.value - 1] || 3)
    : (gridSizes[level.value - 1] || 3)
  const total = size * size
  const pair = iconPairs[Math.floor(Math.random() * iconPairs.length)]

  diffIndex.value = Math.floor(Math.random() * total)

  gridData.value = []
  for (let i = 0; i < total; i++) {
    gridData.value.push(i === diffIndex.value ? pair.diff : pair.normal)
  }

  showResult.value = false
}

const initGame = async () => {
  serverConfig.value = null
  roundInLevel.value = 0
  gameFinished.value = false
  submitting.value = false
  correctCount.value = 0
  wrongCount.value = 0
  combo.value = 0
  recordId.value = null
  finishResult.value = {}
  maxLevel.value = 4
  generateLevel()

  try {
    const data = await startGame(2)
    recordId.value = data.recordId
    startTime.value = Date.now()
    serverConfig.value = JSON.parse(data.configJson)
    level.value = data.levelNo || 1
    generateLevel()
  } catch (e) {
    console.error('[FindDiff] 后端未连接，使用本地模式:', e.message)
    recordId.value = null
    startTime.value = Date.now()
  }
}

function handleClick(index) {
  if (showResult.value) return

  if (index === diffIndex.value) {
    combo.value++
    correctCount.value++
    isAllCorrect.value = true
    resultIcon.value = '🎉'
    resultTitle.value = '太棒了！'
    resultDesc.value = `连击 ${combo.value} 次，继续加油！`
  } else {
    combo.value = 0
    wrongCount.value++
    isAllCorrect.value = false
    resultIcon.value = '😊'
    resultTitle.value = '没关系'
    resultDesc.value = '再仔细看看，你一定能找到！'
  }
  showResult.value = true
}

const totalRounds = computed(() => {
  return serverConfig.value ? serverConfig.value.roundCount : maxLevel.value
})

function nextLevel() {
  if (gameFinished.value) return
  roundInLevel.value++
  if (roundInLevel.value >= totalRounds.value) {
    submitResult(1)
    return
  }
  generateLevel()
}

const submitResult = async (gameStatus) => {
  if (submitting.value || gameFinished.value) return
  submitting.value = true
  gameFinished.value = true
  if (!recordId.value) {
    showResult.value = true
    submitting.value = false
    return
  }
  const costSecond = Math.max(1, Math.round((Date.now() - startTime.value) / 1000))
  const totalQ = correctCount.value + wrongCount.value
  try {
    const result = await finishGame({
      recordId: recordId.value,
      totalQuestion: totalQ || 1,
      correctQuestion: correctCount.value,
      wrongQuestion: wrongCount.value,
      costSecond,
      gameStatus,
    })
    finishResult.value = result
  } catch (e) {
    console.error('[FindDiff finishGame Error]', e)
    if (e.message && e.message.includes('已结束')) {
      recordId.value = null
      finishResult.value = {}
    }
  }
  showResult.value = true
  submitting.value = false
}

function goBack() {
  router.back()
}

onMounted(() => {
  initGame()
})
</script>

<style scoped>
.find-diff-page {
  min-height: 100vh;
  background: #fff5e6;
  padding: 16px;
  box-sizing: border-box;
}

/* 顶部栏 */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.back-btn {
  font-size: 18px;
  color: #ff7d00;
  cursor: pointer;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #8b4513;
}

.level {
  font-size: 16px;
  color: #ff7d00;
  font-weight: 600;
  padding: 6px 12px;
  background: #fff;
  border-radius: 8px;
}

/* 信息栏 */
.info-bar {
  display: flex;
  justify-content: space-around;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.info-label {
  font-size: 14px;
  color: #999;
}

.info-value {
  font-size: 22px;
  font-weight: 600;
  color: #333;
}

.info-value.correct {
  color: #52c41a;
}

/* 提示 */
.tip {
  text-align: center;
  font-size: 18px;
  color: #666;
  margin-bottom: 20px;
  line-height: 1.6;
}

.tip strong {
  color: #ff7d00;
}

/* 游戏网格 - 核心修复：自适应宽度 */
.game-grid {
  display: grid;
  gap: 8px;
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  aspect-ratio: 1 / 1;
  background: #fff;
  padding: 12px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  box-sizing: border-box;
}

.grid-cell {
  background: #fff5e6;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(24px, 6vw, 48px);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.grid-cell:active {
  transform: scale(0.92);
  background: #ffe4c4;
}

.feedback-bar {
  margin: 14px 20px 0;
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 0.95rem;
  text-align: center;
}
.feedback-bar.success {
  background: #e6f5eb;
  color: #25663a;
  border: 1px solid #9ed7b3;
}
.feedback-bar.error {
  background: #fdecea;
  color: #a22d2d;
  border: 1px solid #f3b0af;
}

.finish-card {
  margin: 16px 20px 0;
  padding: 18px 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #f0f0f0;
  box-shadow: 0 10px 24px rgba(0,0,0,0.07);
  text-align: center;
}

.finish-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.finish-text {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.finish-desc {
  font-size: 16px;
  color: #666;
  margin-bottom: 18px;
  line-height: 1.5;
}

.result-btn {
  width: min(220px, 100%);
  background: linear-gradient(135deg, #ffb347 0%, #ff7d00 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  padding: 14px 0;
  border-radius: 999px;
  cursor: pointer;
  border: none;
}

.result-btn:active {
  transform: scale(0.98);
}
</style>