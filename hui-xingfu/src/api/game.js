const BASE_URL = '/api/v1/m2/games'

function getUserId() {
  return localStorage.getItem('user_id') || '1'
}

async function request(url, options = {}) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), 5000)

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
      throw new Error('请求超时，请检查后端是否启动')
    }
    throw e
  }
}

export const getGameList = () => {
  return request(`${BASE_URL}?userId=${getUserId()}`)
}

export const startGame = (gameId) => {
  return request(`${BASE_URL}/${gameId}/start?userId=${getUserId()}`, {
    method: 'POST',
  })
}

export const finishGame = (body) => {
  return request(`${BASE_URL}/finish?userId=${getUserId()}`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export const getDailyTrain = (date) => {
  const params = date ? `?userId=${getUserId()}&date=${date}` : `?userId=${getUserId()}`
  return request(`${BASE_URL}/train/daily${params}`)
}
