/**
 * 首页配置常量
 */

export const HOME_CONFIG = {
  location: '沈阳',
  weather: '晴 32°C',
  weatherKey: 'sunny',
  season: '立秋',
  greeting: '今天，也要好好照顾自己',
}

export const SUCCESS_MESSAGES = [
  '今天也要开开心心！',
  '又是美好的一天！',
  '心情像阳光一样灿烂！',
  '保持好心情，健康常相伴！',
]

/**
 * 首页8个功能入口
 * M2 脑力小游戏：正常开发、真实跳转
 * 其余7项：静态UI占位，后续对接
 */
export const QUICK_ACTIONS = [
  { key: 'health_manage',     label: '健康管理',       icon: 'leaf',               color: '#6B8E5A' },
  { key: 'brain_games',       label: '脑力小游戏',     icon: 'brain_games',        color: '#E97E3A' },
  { key: 'psych_care',        label: '心理关怀',       icon: 'positive_practice',  color: '#D64F4F' },
  { key: 'social_interact',   label: '社交互动',       icon: 'community',          color: '#B07555' },
  { key: 'life_service',      label: '生活服务',       icon: 'service',            color: '#7AA5B5' },
  { key: 'nostalgia',         label: '怀旧故事',       icon: 'nostalgia',          color: '#C85A1E' },
  { key: 'activities',        label: '活动中心',       icon: 'activities',         color: '#F0C75E' },
  { key: 'family',            label: '家人连线',       icon: 'family',             color: '#8B7355' },
]

export const BOTTOM_TABS = [
  { key: 'home', icon: 'home', label: '首页' },
  { key: 'ai', icon: 'service', label: '小辽AI', special: true },
  { key: 'profile', icon: 'profile', label: '我的' },
]
