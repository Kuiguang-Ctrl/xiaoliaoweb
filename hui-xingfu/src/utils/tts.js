/**
 * TTS 语音播报工具
 * 使用浏览器 Web Speech API，无需额外依赖
 * 符合老年人使用场景：语速慢、音量适中、中文优先
 */

/** 是否支持语音播报 */
export const isSpeechSupported = () => {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/** 挑选更自然的中文语音（优先神经语音，其次旧语音） */
function pickChineseVoice() {
  const voices = window.speechSynthesis.getVoices()
  if (!voices || voices.length === 0) return null
  const chinese = voices.filter((v) => v.lang && v.lang.toLowerCase().startsWith('zh'))
  if (chinese.length === 0) return null
  // 微软神经中文语音较自然（晓晓/云扬/云希）；旧语音（Huihui/Yaoyao）较机械
  const prefer = ['Xiaoxiao', 'Yunyang', 'Yunxi', 'Xiaoyi', 'Huihui', 'Yaoyao', 'Kangkang']
  for (const name of prefer) {
    const v = chinese.find((v) => v.name && v.name.includes(name))
    if (v) return v
  }
  return chinese[0]
}

/**
 * 语音播报
 * @param {string} text - 播报文本
 * @param {object} options
 * @param {number} options.rate - 语速 0.1~10，老年友好默认 0.85
 * @param {number} options.pitch - 音调 0~2，默认 1.0
 * @param {number} options.volume - 音量 0~1，默认 0.9
 * @param {string} options.lang - 语言，默认 zh-CN
 * @returns {Promise}
 */
export const speak = (text, options = {}) => {
  const { rate = 0.85, pitch = 1.0, volume = 0.9, lang = 'zh-CN' } = options

  return new Promise((resolve, reject) => {
    if (!isSpeechSupported()) {
      console.warn('[TTS] 当前浏览器不支持语音播报')
      resolve()
      return
    }

    // 取消正在播放的语音（避免重复）
    window.speechSynthesis.cancel()

    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = lang
    utterance.rate = rate
    utterance.pitch = pitch
    utterance.volume = volume

    // 选择更自然的中文语音（若语音列表已加载）
    const voice = pickChineseVoice()
    if (voice) {
      utterance.voice = voice
      utterance.lang = voice.lang
    }

    utterance.onend = resolve
    utterance.onerror = (e) => {
      console.warn('[TTS] 播报出错:', e)
      resolve() // 静默失败，不影响用户体验
    }

    // 延迟一下让 cancel 生效
    setTimeout(() => {
      window.speechSynthesis.speak(utterance)
    }, 100)
  })
}

/**
 * 签到成功后的正向反馈播报
 * 随机选择一条鼓励语
 */
const ENCOURAGE_MESSAGES = [
  '今天也要开开心心！',
  '又是美好的一天，继续保持哦！',
  '心情像阳光一样灿烂！',
  '保持好心情，健康常相伴！',
  '您的笑容，是我们最大的幸福！',
  '每天签到，快乐每一天！',
  '今天是美好的一天，加油！',
]

export const speakEncourage = async () => {
  const msg = ENCOURAGE_MESSAGES[Math.floor(Math.random() * ENCOURAGE_MESSAGES.length)]
  await speak(msg)
}

/**
 * 情绪反馈播报
 * @param {string} emotion
 */
const EMOTION_VOICE_MAP = {
  sunny: '看到您开心，我也很高兴！祝您今天一切顺利！',
  cloudy: '平平淡淡才是真，今天也是很棒的一天！',
  overcast: '明天会更好，我陪在您身边！',
  rainy: '没关系，谁都有不开心的时候，跟我说说心里话吧。',
  stormy: '来，深呼吸，我一直在您身边，一切都会好起来的。',
}

export const speakEmotionFeedback = async (emotion) => {
  const msg = EMOTION_VOICE_MAP[emotion]
  if (msg) {
    await speak(msg)
  }
}
