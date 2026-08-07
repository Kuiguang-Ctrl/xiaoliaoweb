/**
 * AI 对话 API — 对接后端 POST /api/chat（Java 转发 Python AI 引擎）
 */

const BASE_URL = '/api/chat'

/**
 * 发送一条消息给 AI
 * @param {string} message - 用户消息
 * @param {Array} history - 历史消息 [{role, content}]，最多 30 条
 * @returns {Promise<{reply:string, intent:string, inspection:Object}>}
 */
export const sendChat = async (message, history = []) => {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), 60000)

  try {
    const res = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message,
        conversationHistory: history,
      }),
      signal: controller.signal,
    })
    clearTimeout(timeoutId)
    const data = await res.json()
    if (data.code === 200) {
      return data.data
    }
    throw new Error(data.message || '请求失败')
  } catch (e) {
    clearTimeout(timeoutId)
    if (e.name === 'AbortError') {
      throw new Error('小辽思考得有点久，请再试一次')
    }
    throw e
  }
}
