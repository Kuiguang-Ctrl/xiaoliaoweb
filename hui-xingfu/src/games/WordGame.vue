<template>
  <div class="game-page">
    <div class="game-header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span class="title">看图说词</span>
      <span class="progress" v-if="wordList.length">{{ current + 1 }}/{{ wordList.length }}</span>
    </div>

    <div class="word-card" v-if="wordList.length">
      <div class="word-emoji">{{ wordList[current].emoji }}</div>
      <div class="word-tip">这是什么？点击正确的词语</div>
    </div>

    <div class="option-list" v-if="wordList.length">
      <div
          v-for="opt in options"
          :key="opt"
          class="option-btn"
          @click="selectAnswer(opt)"
      >
        {{ opt }}
      </div>
    </div>

    <div v-if="feedbackMessage" class="feedback-bar" :class="feedbackType">
      {{ feedbackMessage }}
    </div>

    <div v-if="showFinish" class="finish-card">
      <div class="finish-icon">💬</div>
      <div class="finish-text">{{ finishResult.cognitiveTip || '语言流畅度练习完成' }}</div>
      <div class="finish-score">答对 {{ rightCount }} / {{ wordList.length }} 题</div>
      <div v-if="finishResult.star !== undefined" class="stars">
        {{ '⭐'.repeat(finishResult.star) }}{{ '☆'.repeat(3 - finishResult.star) }}
      </div>
      <div v-if="finishResult.encourageMsg" class="encourage-msg">{{ finishResult.encourageMsg }}</div>
      <button class="finish-btn" @click="restart">再练一次</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { startGame, finishGame } from '@/api/game'

const defaultWords = [
  { emoji: '🐱', answer: '小猫', options: ['小猫', '小狗', '兔子', '小鸟'] },
  { emoji: '🚗', answer: '汽车', options: ['自行车', '汽车', '火车', '飞机'] },
  { emoji: '🍚', answer: '米饭', options: ['面条', '馒头', '米饭', '饺子'] },
  { emoji: '🌞', answer: '太阳', options: ['月亮', '星星', '太阳', '云朵'] },
  { emoji: '🏠', answer: '房子', options: ['学校', '房子', '医院', '商店'] }
]

const wordList = ref([...defaultWords])
const current = ref(0)
const rightCount = ref(0)
const wrongCount = ref(0)
const options = ref([])
const showFinish = ref(false)
const feedbackMessage = ref('')
const feedbackType = ref('')
const recordId = ref(null)
const startTime = ref(null)
const finishResult = ref({})
const submitting = ref(false)
let answerTimer = null

const initOptions = () => {
  if (wordList.value.length === 0) return
  options.value = [...wordList.value[current.value].options].sort(() => Math.random() - 0.5)
  feedbackMessage.value = ''
  feedbackType.value = ''
}

const initGame = async () => {
  current.value = 0
  rightCount.value = 0
  wrongCount.value = 0
  showFinish.value = false
  finishResult.value = {}
  recordId.value = null
  submitting.value = false
  if (answerTimer) {
    clearTimeout(answerTimer)
    answerTimer = null
  }
  wordList.value = [...defaultWords].sort(() => Math.random() - 0.5)
  initOptions()

  try {
    const data = await startGame(4)
    recordId.value = data.recordId
    startTime.value = Date.now()
    const config = JSON.parse(data.configJson)
    wordList.value = [...config.questions].sort(() => Math.random() - 0.5)
    initOptions()
  } catch (e) {
    console.error('[WordGame] 后端未连接，使用本地模式:', e.message)
    recordId.value = null
    startTime.value = Date.now()
  }
}

const selectAnswer = (opt) => {
  if (submitting.value || showFinish.value) return
  if (opt === wordList.value[current.value].answer) {
    rightCount.value++
    feedbackMessage.value = '答对啦！真棒~'
    feedbackType.value = 'success'
  } else {
    wrongCount.value++
    feedbackMessage.value = `答案是「${wordList.value[current.value].answer}」，我们继续下一个。`
    feedbackType.value = 'error'
  }

  if (answerTimer) clearTimeout(answerTimer)
  answerTimer = setTimeout(() => {
    if (current.value < wordList.value.length - 1) {
      current.value++
      initOptions()
    } else {
      submitResult(1)
    }
  }, 800)
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
  try {
    const result = await finishGame({
      recordId: recordId.value,
      totalQuestion: wordList.value.length,
      correctQuestion: rightCount.value,
      wrongQuestion: wrongCount.value,
      costSecond,
      gameStatus,
    })
    finishResult.value = result
  } catch (e) {
    console.error('[WordGame finishGame Error]', e)
    if (e.message && e.message.includes('已结束')) {
      recordId.value = null
    }
  }
  showFinish.value = true
  submitting.value = false
}

const restart = () => {
  if (answerTimer) clearTimeout(answerTimer)
  submitting.value = false
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
.progress { font-size: 16px; color: #ff7d00; }

.word-card {
  margin: 30px 20px; padding: 40px 20px;
  background: #fff; border-radius: 16px; text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.word-emoji { font-size: 100px; margin-bottom: 20px; }
.word-tip { font-size: 18px; color: #666; }

.option-list {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 16px; padding: 0 20px;
}
.option-btn {
  padding: 18px; background: #fff; border-radius: 12px;
  text-align: center; font-size: 18px; cursor: pointer;
  border: 2px solid #eee; transition: all 0.2s;
}
.option-btn:active { background: #fff7ed; border-color: #ff7d00; }

.feedback-bar {
  margin: 12px 20px 0;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 0.95rem;
  text-align: center;
}
.feedback-bar.success {
  background: #e6f5eb;
  color: #3d8b5a;
  border: 1px solid #9ed7b3;
}
.feedback-bar.error {
  background: #f9e6e6;
  color: #a22d2d;
  border: 1px solid #e8b3b3;
}

.finish-content { text-align: center; padding: 20px 0; }
.finish-icon { font-size: 60px; margin-bottom: 16px; }
.finish-text { font-size: 18px; margin-bottom: 8px; }
.finish-score { font-size: 16px; color: #ff7d00; font-weight: 500; }
</style>