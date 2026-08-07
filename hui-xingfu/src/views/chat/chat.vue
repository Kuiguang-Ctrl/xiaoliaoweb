<template>
  <div class="chat_page">
    <header class="chat_header">
      <button class="back_btn" @click="goBack" aria-label="返回">‹</button>
      <div class="header_title">
        <span class="ai_avatar" aria-hidden="true">辽</span>
        <div>
          <div class="title">小辽 AI 助手</div>
          <div class="subtitle">随时陪您聊聊天</div>
        </div>
      </div>
    </header>

    <main class="msg_list" ref="listRef">
      <div v-if="messages.length === 0" class="welcome">
        <p>您好呀，我是小辽！</p>
        <p>有什么想聊的，尽管跟我说～</p>
      </div>
      <div
        v-for="(m, i) in messages"
        :key="i"
        class="msg_row"
        :class="m.role"
      >
        <span v-if="m.role === 'assistant'" class="mini_avatar" aria-hidden="true">辽</span>
        <div class="bubble">{{ m.content }}</div>
      </div>

      <!-- 意图建议卡片 -->
      <div v-if="intentHint" class="intent_card">
        <span>{{ intentHint.text }}</span>
        <button class="intent_btn" @click="goIntent">
          {{ intentHint.label }} →
        </button>
      </div>
    </main>

    <footer class="input_bar">
      <input
        v-model="input"
        type="text"
        class="msg_input"
        placeholder="说点什么…"
        :disabled="sending"
        maxlength="2000"
        @keyup.enter="send"
      />
      <button class="send_btn" :disabled="sending || !input.trim()" @click="send">
        {{ sending ? '…' : '发送' }}
      </button>
    </footer>
  </div>
</template>

<script>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { sendChat } from '@/api/chat'

/** 意图 → 页面跳转建议 */
const INTENT_ACTIONS = {
  checkin: { label: '去签到', path: '/signin', text: '您想签到打卡啦？' },
  game: { label: '去玩游戏', path: '/brain-game', text: '想玩个脑力小游戏？' },
}

export default {
  name: 'ChatPage',

  setup() {
    const router = useRouter()
    const messages = ref([])
    const input = ref('')
    const sending = ref(false)
    const listRef = ref(null)
    const lastIntent = ref('')

    const intentHint = computed(() => INTENT_ACTIONS[lastIntent.value] || null)

    const scrollToBottom = () => {
      nextTick(() => {
        if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
      })
    }

    const send = async () => {
      const text = input.value.trim()
      if (!text || sending.value) return

      messages.value.push({ role: 'user', content: text })
      input.value = ''
      sending.value = true
      lastIntent.value = ''
      scrollToBottom()

      // 历史（不含本条）传给后端，最多 29 条，加上本条共 30
      const history = messages.value.slice(0, -1).slice(-29).map((m) => ({
        role: m.role,
        content: m.content,
      }))

      try {
        const data = await sendChat(text, history)
        messages.value.push({ role: 'assistant', content: data.reply })
        lastIntent.value = data.intent
      } catch (e) {
        messages.value.push({
          role: 'assistant',
          content: e.message || '小辽走神了，请稍后再试～',
        })
      } finally {
        sending.value = false
        scrollToBottom()
      }
    }

    const goIntent = () => {
      const action = INTENT_ACTIONS[lastIntent.value]
      if (action) router.push(action.path)
    }

    const goBack = () => {
      if (window.history.length > 1) router.back()
      else router.push('/')
    }

    return { messages, input, sending, listRef, intentHint, send, goIntent, goBack }
  },
}
</script>

<style scoped>
.chat_page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 480px;
  margin: 0 auto;
  background: var(--color-bg-page, #FFF9F3);
  font-size: 1.0625rem; /* 17px 起步，适老化 */
}

/* 头部 */
.chat_header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: calc(14px + env(safe-area-inset-top)) 16px 12px;
  background: var(--color-bg-card, #FFFFFF);
  border-bottom: 1.5px solid var(--color-border);
}

.back_btn {
  font-size: 2rem;
  line-height: 1;
  width: 48px;
  height: 48px;
  border: none;
  background: transparent;
  color: var(--color-primary, #E97E3A);
  cursor: pointer;
  border-radius: var(--radius-md);
}

.header_title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai_avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-primary, #E97E3A);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  font-weight: 700;
}

.title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
}

.subtitle {
  font-size: 0.8125rem;
  color: var(--color-text-light);
}

/* 消息区 */
.msg_list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.welcome {
  text-align: center;
  color: var(--color-text-light);
  margin-top: 40px;
  line-height: 1.8;
}

.msg_row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 100%;
}

.msg_row.user {
  justify-content: flex-end;
}

.mini_avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-primary, #E97E3A);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
}

.bubble {
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.6;
  max-width: 78%;
  word-break: break-word;
}

.msg_row.assistant .bubble {
  background: var(--color-bg-card, #FFFFFF);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: 6px;
  color: var(--color-text);
}

.msg_row.user .bubble {
  background: var(--color-primary, #E97E3A);
  color: #fff;
  border-bottom-right-radius: 6px;
}

/* 意图建议 */
.intent_card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: var(--color-bg-warm, #FFF3E4);
  border: 1.5px dashed var(--color-primary, #E97E3A);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  margin-top: 4px;
  font-size: 0.9375rem;
  color: var(--color-text);
}

.intent_btn {
  flex-shrink: 0;
  min-height: 48px;
  padding: 0 18px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-primary, #E97E3A);
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

/* 输入栏 */
.input_bar {
  display: flex;
  gap: 10px;
  padding: 12px 14px calc(12px + env(safe-area-inset-bottom));
  background: var(--color-bg-card, #FFFFFF);
  border-top: 1.5px solid var(--color-border);
}

.msg_input {
  flex: 1;
  min-height: 48px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-pill);
  padding: 0 18px;
  font-size: 1rem;
  font-family: inherit;
  outline: none;
  background: var(--color-bg-page, #FFF9F3);
}

.msg_input:focus {
  border-color: var(--color-primary, #E97E3A);
}

.send_btn {
  flex-shrink: 0;
  min-width: 76px;
  min-height: 48px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-primary, #E97E3A);
  color: #fff;
  font-size: 1.0625rem;
  font-weight: 600;
  cursor: pointer;
}

.send_btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
