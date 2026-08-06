/**
 * 适老化工具：字号档位 / 高对比度 / 读屏开关
 * 通过修改 <html> 的 font-size 让 rem 单位跟随缩放
 */

const FONT_SCALE_KEY = 'huixingfu_font_scale'
const CONTRAST_KEY = 'huixingfu_high_contrast'

// 档位：1 = 标准（16px），1.15 = 大（~18px），1.3 = 超大（~21px）
export const FONT_SCALES = [
  { value: 1, label: '标准' },
  { value: 1.15, label: '大' },
  { value: 1.3, label: '超大' },
]

const BASE_FONT_SIZE = 16

export function getFontScale() {
  const stored = Number(localStorage.getItem(FONT_SCALE_KEY))
  return FONT_SCALES.some((item) => item.value === stored) ? stored : 1
}

export function setFontScale(scale) {
  localStorage.setItem(FONT_SCALE_KEY, String(scale))
  applyFontScale()
}

export function applyFontScale() {
  const scale = getFontScale()
  document.documentElement.style.fontSize = `${BASE_FONT_SIZE * scale}px`
}

export function getHighContrast() {
  return localStorage.getItem(CONTRAST_KEY) === '1'
}

export function setHighContrast(on) {
  localStorage.setItem(CONTRAST_KEY, on ? '1' : '0')
  applyHighContrast()
}

export function applyHighContrast() {
  const on = getHighContrast()
  document.documentElement.classList.toggle('high_contrast', on)
}

export function initAccessibility() {
  applyFontScale()
  applyHighContrast()
}
