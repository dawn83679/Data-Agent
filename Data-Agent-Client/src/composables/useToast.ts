import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

interface Toast {
  id: number
  type: ToastType
  message: string
}

/**
 * Toast提示组合式函数
 */
export function useToast() {
  const toasts = ref<Toast[]>([])
  let toastId = 0

  /**
   * 显示Toast
   */
  const showToast = (message: string, type: ToastType = 'info', duration: number = 3000) => {
    const id = ++toastId
    toasts.value.push({ id, type, message })

    // 自动移除
    if (duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }
  }

  /**
   * 移除Toast
   */
  const removeToast = (id: number) => {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index > -1) {
      toasts.value.splice(index, 1)
    }
  }

  return {
    toasts,
    showToast,
    removeToast
  }
}

