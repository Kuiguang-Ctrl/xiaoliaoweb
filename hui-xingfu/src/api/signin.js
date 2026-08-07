/**
 * 签到与情绪 API 服务
 * 对接后端 /api/checkin（Java，统一 Result 返回）
 */

const BASE_URL = '/api/checkin'

/** 通用请求（后端 Result 格式，code=200 成功） */
async function request(url, options = {}) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), 10000)

  try {
    const res = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
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
      throw new Error('请求超时，请稍后再试')
    }
    throw e
  }
}

/* ================================================================
 * 情绪类型定义（对应 M1 需求的 5 档）
 * 前端用 rainy/stormy，后端枚举是 rain/storm，见下方映射
 * ================================================================ */

export const EMOTION_TYPES = {
  sunny: { label: '晴', icon: '😊', score: 5 },
  cloudy: { label: '多云', icon: '🙂', score: 4 },
  overcast: { label: '阴', icon: '😌', score: 3 },
  rainy: { label: '雨', icon: '😔', score: 2 },
  stormy: { label: '雷雨', icon: '😢', score: 1 },
}

/** 是否为负面情绪（雨、雷雨） */
export const isNegativeEmotion = (emotion) => {
  return emotion === 'rainy' || emotion === 'stormy'
}

/** 前端情绪 → 后端枚举（rainy→rain, stormy→storm） */
const toBackendMood = (emotion) => {
  if (emotion === 'rainy') return 'rain'
  if (emotion === 'stormy') return 'storm'
  return emotion
}

/** 后端枚举 → 前端情绪 */
const toFrontMood = (mood) => {
  if (mood === 'rain') return 'rainy'
  if (mood === 'storm') return 'stormy'
  return mood
}

/* ================================================================
 * API 接口（对接后端）
 * ================================================================ */

/**
 * POST /api/checkin 执行签到
 * @param {string} emotion - sunny/cloudy/overcast/rainy/stormy
 * @returns {Promise} { code, data: { streak, negativeAlert, feedback } }
 */
export const doSignin = async (emotion = 'sunny') => {
  const data = await request(BASE_URL, {
    method: 'POST',
    body: JSON.stringify({ mood: toBackendMood(emotion) }),
  })
  return {
    code: 200,
    data: {
      streak: data.consecutiveDays,
      negativeAlert: data.careAlert,
      feedback: data.feedback,
    },
  }
}

/**
 * GET /api/checkin/today 今日签到状态
 * @returns {Promise} { code, data: { signed, emotion, streak } }
 */
export const checkTodaySignin = async () => {
  const data = await request(`${BASE_URL}/today`)
  return {
    code: 200,
    data: {
      signed: data.checkedIn,
      emotion: toFrontMood(data.todayMood),
      streak: data.consecutiveDays,
    },
  }
}

/**
 * GET /api/checkin/calendar 情绪月历
 * @param {number} year
 * @param {number} month - 1-12
 * @returns {Promise} { code, data: [{ date, emotion }] }
 */
export const getSigninHistory = async (year, month) => {
  const monthStr = `${year}-${String(month).padStart(2, '0')}`
  const data = await request(`${BASE_URL}/calendar?month=${monthStr}`)
  return {
    code: 200,
    data: (data.checkins || []).map((c) => ({
      date: c.date,
      emotion: toFrontMood(c.mood),
    })),
  }
}

/**
 * GET /api/checkin/today 连续签到天数
 * @returns {Promise} { code, data: { streak } }
 */
export const getStreak = async () => {
  const data = await request(`${BASE_URL}/today`)
  return { code: 200, data: { streak: data.consecutiveDays } }
}

/**
 * 获取最近 N 天负面情绪次数（供外部调用）
 * @param {Array} records - 签到记录 [{date, emotion}]
 */
export const getRecentNegativeCount = (records, days = 3) => {
  const sorted = [...records].sort((a, b) => b.date.localeCompare(a.date))
  return sorted.slice(0, days).filter((r) => isNegativeEmotion(r.emotion)).length
}
