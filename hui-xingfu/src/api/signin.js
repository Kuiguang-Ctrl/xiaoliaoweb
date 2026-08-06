/**
 * 签到与情绪 API 服务
 *
 * 当前方案：localStorage 本地存储（模拟后端）
 * 后续对接真实后端时，只需替换函数体内的实现即可
 */

const STORAGE_KEY = 'signin_records'

/* ================================================================
 * 工具函数
 * ================================================================ */

/** 获取今天日期字符串 YYYY-MM-DD */
const todayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 读取本地所有签到记录 */
const loadRecords = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

/** 保存签到记录 */
const saveRecords = (records) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(records))
}

/* ================================================================
 * 情绪类型定义（对应 M1 需求的 5 档）
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

/* ================================================================
 * API 接口
 * ================================================================ */

/**
 * POST /api/signin
 * 执行签到
 * @param {string} emotion - 情绪值 sunny/cloudy/overcast/rainy/stormy
 * @returns {Promise} { code, data: { streak, negativeAlert } }
 */
export const doSignin = async (emotion = 'sunny') => {
  // 模拟网络延迟
  await new Promise((resolve) => setTimeout(resolve, 400))

  if (!EMOTION_TYPES[emotion]) {
    return { code: 400, message: '无效的情绪值' }
  }

  const records = loadRecords()
  const date = todayStr()

  // 检查今日是否已签到
  const exists = records.find((r) => r.date === date)
  if (exists) {
    // 允许更新今天的情绪
    exists.emotion = emotion
    saveRecords(records)
    return {
      code: 200,
      data: { streak: calcStreak(records), negativeAlert: isNegativeEmotion(emotion) ? checkNegativeAlert(records) : false, updated: true },
    }
  }

  // 新增签到记录
  records.push({
    date,
    emotion,
    timestamp: Date.now(),
  })
  saveRecords(records)

  const streak = calcStreak(records)

  return {
    code: 200,
    data: { streak, negativeAlert: checkNegativeAlert(records) },
  }
}

/**
 * GET /api/signin/today
 * 查询今日是否已签到
 * @returns {Promise} { code, data: { signed, emotion, streak } }
 */
export const checkTodaySignin = async () => {
  await new Promise((resolve) => setTimeout(resolve, 100))

  const records = loadRecords()
  const date = todayStr()
  const today = records.find((r) => r.date === date)

  return {
    code: 200,
    data: {
      signed: !!today,
      emotion: today?.emotion || null,
      streak: calcStreak(records),
    },
  }
}

/**
 * GET /api/signin/history
 * 获取指定月份的签到历史（用于情绪月历）
 * @param {number} year
 * @param {number} month - 1-12
 * @returns {Promise} { code, data: [{ date, emotion }] }
 */
export const getSigninHistory = async (year, month) => {
  await new Promise((resolve) => setTimeout(resolve, 100))

  const records = loadRecords()
  const prefix = `${year}-${String(month).padStart(2, '0')}-`

  const monthData = records
    .filter((r) => r.date.startsWith(prefix))
    .map((r) => ({
      date: r.date,
      emotion: r.emotion,
    }))

  return { code: 200, data: monthData }
}

/**
 * GET /api/signin/streak
 * 获取连续签到天数
 */
export const getStreak = async () => {
  await new Promise((resolve) => setTimeout(resolve, 100))

  const records = loadRecords()
  return { code: 200, data: { streak: calcStreak(records) } }
}

/* ================================================================
 * 内部计算函数
 * ================================================================ */

/** 计算连续签到天数（从今天往回数） */
function calcStreak(records) {
  if (records.length === 0) return 0

  const dateSet = new Set(records.map((r) => r.date))
  let count = 0

  const today = new Date()
  for (let i = 0; i < 365; i++) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const s =
      d.getFullYear() +
      '-' +
      String(d.getMonth() + 1).padStart(2, '0') +
      '-' +
      String(d.getDate()).padStart(2, '0')

    if (dateSet.has(s)) {
      count++
    } else if (i === 0) {
      // 今天没签到，从昨天开始算
      continue
    } else {
      break
    }
  }
  return count
}

/**
 * 检查是否需要触发负面情绪预警
 * 规则：连续最近签到日（含今天）中有 ≥3 天负面情绪 → 触发
 * @returns {boolean}
 */
function checkNegativeAlert(records) {
  if (records.length < 3) return false

  // 按日期降序排列
  const sorted = [...records].sort((a, b) => b.date.localeCompare(a.date))
  let count = 0

  for (let i = 0; i < sorted.length; i++) {
    if (isNegativeEmotion(sorted[i].emotion)) {
      count++
      if (count >= 3) return true
    } else {
      // 中间有非负面就中断
      break
    }
  }
  return false
}

/**
 * 获取最近 N 天负面情绪次数（供外部调用）
 */
export const getRecentNegativeCount = (records, days = 3) => {
  const sorted = [...records].sort((a, b) => b.date.localeCompare(a.date))
  return sorted.slice(0, days).filter((r) => isNegativeEmotion(r.emotion)).length
}
