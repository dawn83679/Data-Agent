import { ref, watch } from 'vue'

/**
 * 主题管理组合式函数
 */
export function useTheme() {
  // 从localStorage恢复主题或使用默认值
  const getInitialTheme = (): 'dark' | 'light' => {
    if (typeof window === 'undefined') return 'dark'
    const savedTheme = localStorage.getItem('theme') as 'dark' | 'light' | null
    return savedTheme || 'dark'
  }

  const currentTheme = ref<'dark' | 'light'>(getInitialTheme())

  /**
   * 设置主题
   */
  const setTheme = (theme: 'dark' | 'light') => {
    currentTheme.value = theme
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', theme)
      localStorage.setItem('theme', theme)
    }
  }

  /**
   * 切换主题
   */
  const toggleTheme = () => {
    const newTheme = currentTheme.value === 'dark' ? 'light' : 'dark'
    setTheme(newTheme)
  }

  /**
   * 初始化主题
   */
  const initTheme = () => {
    setTheme(currentTheme.value)
  }

  // 立即初始化
  if (typeof window !== 'undefined') {
    initTheme()
  }

  return {
    currentTheme,
    setTheme,
    toggleTheme,
    initTheme
  }
}

