import { ref } from 'vue'

/**
 * 是否移动端布局（视口 ≤768px）。
 * 另支持在桌面浏览器访问任意页面时 URL 加 ?mobile=1，强制按移动端布局预览。
 */
const forced = new URLSearchParams(window.location.search).has('mobile')
const mq = window.matchMedia('(max-width: 768px)')

const isMobile = ref(mq.matches || forced)

function apply() {
  isMobile.value = mq.matches || forced
  // 同步到 <html> 根类名，供全局 CSS 使用（避免只用媒体查询导致强制预览失效）
  document.documentElement.classList.toggle('is-mobile', isMobile.value)
}

let bound = false

export function useResponsive() {
  if (!bound) {
    bound = true
    if (mq.addEventListener) {
      mq.addEventListener('change', apply)
    } else {
      mq.addListener(apply)
    }
    apply()
  }
  return { isMobile }
}
