<template>
  <div class="game-page">
    <div class="game-header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span class="title">翻牌配对</span>
      <span class="level">难度 Lv.{{ level }}</span>
    </div>

    <div class="game-info">
      <div class="info-item">
        <div class="num">{{ steps }}</div>
        <div class="label">步数</div>
      </div>
      <div class="info-item">
        <div class="num">{{ matched }}/{{ total }}</div>
        <div class="label">配对</div>
      </div>
    </div>

    <div class="card-grid" :class="'grid-' + gridType">
      <div
          v-for="(card, index) in cards"
          :key="index"
          class="card-item"
          :class="{ flipped: card.flipped, matched: card.matched }"
          @click="flipCard(index)"
      >
        <div class="card-front">?</div>
        <div class="card-back">{{ card.emoji }}</div>
      </div>
    </div>

    <div v-if="feedbackText" class="feedback-bar" :class="feedbackType">
      {{ feedbackText }}
    </div>

    <div v-if="showResult" class="finish-card">
      <div class="finish-icon">🎉</div>
      <div class="finish-text">{{ finishResult.cognitiveTip || '今天锻炼了记忆力，真棒！' }}</div>
      <div class="finish-score">用时 {{ steps }} 步，正确率 {{ correctRate }}%</div>
      <div v-if="finishResult.star !== undefined" class="stars">
        {{ '⭐'.repeat(finishResult.star) }}{{ '☆'.repeat(3 - finishResult.star) }}
      </div>
      <div v-if="finishResult.encourageMsg" class="encourage-msg">{{ finishResult.encourageMsg }}</div>
      <div class="result-actions">
        <button class="result-btn" @click="restart">再玩一次</button>
        <button class="result-btn primary" @click="nextLevel">下一关</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { startGame, finishGame } from '@/api/game'

const level = ref(1)
const steps = ref(0)
const matched = ref(0)
const cards = ref([])
const flippedCards = ref([])
const showResult = ref(false)
const feedbackText = ref('')
const feedbackType = ref('')
const recordId = ref(null)
const startTime = ref(null)
const finishResult = ref({})
const serverConfig = ref(null)
const submitting = ref(false)

const gridType = computed(() => {
  if (serverConfig.value) {
    const { rows, cols } = serverConfig.value
    if (rows === 2 && cols === 2) return '2x2'
    if (rows === 2 && cols === 3) return '2x3'
    if (rows === 3 && cols === 4) return '3x4'
    return '4x4'
  }
  if (level.value === 1) return '2x2'
  if (level.value === 2) return '2x3'
  if (level.value === 3) return '3x4'
  return '4x4'
})

const total = computed(() => cards.value.length / 2)

const correctRate = computed(() => {
  if (steps.value === 0) return 100
  const wrong = steps.value - total.value
  if (level.value === 3) {
    if (wrong === 0) return 100
    if (wrong === 1) return 95
    if (wrong === 2) return 92
    if (wrong === 3) return 90
    if (wrong === 4) return 88
    if (wrong === 5) return 86
    if (wrong === 6) return 84
    if (wrong === 7) return 82
    return Math.max(40, 78 - (wrong - 7) * 5)
  } else if (level.value === 4) {
    if (wrong === 0) return 100
    if (wrong <= 2) return 92
    if (wrong <= 4) return 85
    if (wrong <= 6) return 78
    if (wrong <= 8) return 72
    return Math.max(40, 65 - (wrong - 8) * 5)
  } else {
    const base = Math.round((total.value / steps.value) * 100)
    if (wrong === 1) return Math.min(100, base + 10)
    if (wrong === 2) return Math.min(100, base + 30)
    if (wrong >= 3) return Math.min(100, base + 50)
    return base
  }
})

const emojiPool = ['🍎', '🍌', '🍋', '🍇', '🍓', '🍑', '🥝', '🍒']

function buildLocalCards(lv) {
  const pairCount = lv + 1 + (lv - 1)
  const selectedEmojis = emojiPool.slice(0, pairCount)
  return [...selectedEmojis, ...selectedEmojis]
      .sort(() => Math.random() - 0.5)
      .map(emoji => ({ emoji, flipped: false, matched: false }))
}

const initGame = async () => {
  cards.value = buildLocalCards(level.value)
  steps.value = 0
  matched.value = 0
  flippedCards.value = []
  showResult.value = false
  finishResult.value = {}
  feedbackText.value = ''
  recordId.value = null
  submitting.value = false

  try {
    const data = await startGame(1)
    recordId.value = data.recordId
    startTime.value = Date.now()
    serverConfig.value = JSON.parse(data.configJson)
    level.value = data.levelNo || level.value

    const { pairCount } = serverConfig.value
    const selectedEmojis = emojiPool.slice(0, pairCount)
    cards.value = [...selectedEmojis, ...selectedEmojis]
        .sort(() => Math.random() - 0.5)
        .map(emoji => ({ emoji, flipped: false, matched: false }))
    steps.value = 0
    matched.value = 0
    flippedCards.value = []
  } catch (e) {
    console.error('[FlipCard] 后端未连接，使用本地模式:', e.message)
    recordId.value = null
    startTime.value = Date.now()
  }
}

const flipCard = (index) => {
  const card = cards.value[index]
  if (card.flipped || card.matched || flippedCards.value.length >= 2) return

  card.flipped = true
  flippedCards.value.push(index)

  if (flippedCards.value.length === 2) {
    steps.value++
    const [first, second] = flippedCards.value
    if (cards.value[first].emoji === cards.value[second].emoji) {
      cards.value[first].matched = true
      cards.value[second].matched = true
      matched.value++
      flippedCards.value = []
      feedbackText.value = '配对成功！继续下一组。'
      feedbackType.value = 'success'

      if (matched.value === total.value) {
        setTimeout(() => {
          submitResult(1)
        }, 500)
      }
    } else {
      setTimeout(() => {
        cards.value[first].flipped = false
        cards.value[second].flipped = false
        flippedCards.value = []
        feedbackText.value = '没关系，再试试~'
        feedbackType.value = 'error'
      }, 1000)
    }
  }
}

const submitResult = async (gameStatus) => {
  if (submitting.value) return
  submitting.value = true
  if (!recordId.value) {
    showResult.value = true
    submitting.value = false
    return
  }
  const costSecond = Math.max(1, Math.round((Date.now() - startTime.value) / 1000))
  const wrong = steps.value - total.value
  let adjustedRate
  if (level.value === 3) {
    if (wrong === 0) adjustedRate = 100
    else if (wrong === 1) adjustedRate = 95
    else if (wrong === 2) adjustedRate = 92
    else if (wrong === 3) adjustedRate = 90
    else if (wrong === 4) adjustedRate = 88
    else if (wrong === 5) adjustedRate = 86
    else if (wrong === 6) adjustedRate = 84
    else if (wrong === 7) adjustedRate = 82
    else adjustedRate = Math.max(40, 78 - (wrong - 7) * 5)
  } else if (level.value === 4) {
    if (wrong === 0) adjustedRate = 100
    else if (wrong <= 2) adjustedRate = 92
    else if (wrong <= 4) adjustedRate = 85
    else if (wrong <= 6) adjustedRate = 78
    else if (wrong <= 8) adjustedRate = 72
    else adjustedRate = Math.max(40, 65 - (wrong - 8) * 5)
  } else {
    const base = Math.round((total.value / steps.value) * 100)
    if (wrong === 1) adjustedRate = Math.min(100, base + 10)
    else if (wrong === 2) adjustedRate = Math.min(100, base + 30)
    else adjustedRate = Math.min(100, base + 50)
  }
  try {
    const result = await finishGame({
      recordId: recordId.value,
      totalQuestion: 100,
      correctQuestion: adjustedRate,
      wrongQuestion: 100 - adjustedRate,
      costSecond,
      gameStatus,
    })
    finishResult.value = result
    if (result.nextLevelNo) {
      level.value = result.nextLevelNo
    }
  } catch (e) {
    console.error('[FlipCard finishGame Error]', e)
  }
  showResult.value = true
  submitting.value = false
}

const restart = () => {
  submitting.value = false
  showResult.value = false
  finishResult.value = {}
  initGame()
}

const nextLevel = () => {
  submitting.value = false
  showResult.value = false
  finishResult.value = {}
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
.level { font-size: 16px; color: #ff7d00; font-weight: 500; }

.game-info {
  display: flex; justify-content: space-around;
  padding: 20px; background: #fff; margin-bottom: 20px;
}
.info-item { text-align: center; }
.info-item .num { font-size: 24px; font-weight: 700; color: #ff7d00; }
.info-item .label { font-size: 14px; color: #999; }

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

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 18px;
}

.result-btn,
.finish-btn {
  border: none;
  border-radius: 999px;
  padding: 12px 18px;
  font-weight: 700;
  cursor: pointer;
}

.result-btn.primary {
  background: #ff7d00;
  color: #fff;
}

.result-btn:not(.primary),
.finish-btn {
  background: #f4f4f4;
  color: #333;
}

.card-grid {
  display: grid; gap: 12px; padding: 0 20px;
}
.grid-2x2 { grid-template-columns: repeat(2, 1fr); }
.grid-2x3 { grid-template-columns: repeat(3, 1fr); }
.grid-3x4 { grid-template-columns: repeat(4, 1fr); }
.grid-4x4 { grid-template-columns: repeat(4, 1fr); }

.card-item {
  aspect-ratio: 1; perspective: 1000px; cursor: pointer;
}
.card-front, .card-back {
  position: absolute; width: 100%; height: 100%;
  backface-visibility: hidden; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 32px;
}
.card-front {
  background: linear-gradient(135deg, #ff9a3c, #ff7d00);
  color: #fff; font-size: 28px; font-weight: 600;
}
.card-back {
  background: #fff; transform: rotateY(180deg);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.card-item.flipped .card-front { transform: rotateY(180deg); }
.card-item.flipped .card-back { transform: rotateY(0); }
.card-item.matched .card-back { background: #f0f9eb; border: 2px solid #67c23a; }

.result-content { text-align: center; padding: 20px 0; }
.stars { font-size: 40px; margin-bottom: 16px; }
.result-tip { font-size: 18px; margin-bottom: 8px; }
.result-info { font-size: 14px; color: #999; }
</style>