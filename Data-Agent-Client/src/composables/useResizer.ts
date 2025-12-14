import type { Ref } from 'vue'

/**
 * 面板调整组合式函数
 */
export function useResizer() {
  /**
   * 设置调整器
   */
  const setupResizers = (
    resizerLeftRef: Ref<HTMLElement | undefined>,
    resizerBottomRef: Ref<HTMLElement | undefined>,
    resizerRightRef: Ref<HTMLElement | undefined>,
    sidebarRef: Ref<HTMLElement | undefined>,
    bottomPanelRef: Ref<HTMLElement | undefined>,
    aiSidebarRef: Ref<HTMLElement | undefined>
  ) => {
    // 左侧调整器
    if (resizerLeftRef.value && sidebarRef.value) {
      setupHorizontalResizer(resizerLeftRef.value, sidebarRef.value)
    }

    // 底部调整器
    if (resizerBottomRef.value && bottomPanelRef.value) {
      setupVerticalResizer(resizerBottomRef.value, bottomPanelRef.value)
    }

    // 右侧调整器
    if (resizerRightRef.value && aiSidebarRef.value) {
      setupHorizontalResizer(resizerRightRef.value, aiSidebarRef.value, true)
    }
  }

  /**
   * 设置水平调整器（左右调整）
   */
  const setupHorizontalResizer = (
    resizer: HTMLElement,
    target: HTMLElement,
    reverse: boolean = false
  ) => {
    let isResizing = false
    let startX = 0
    let startWidth = 0

    resizer.addEventListener('mousedown', (e) => {
      isResizing = true
      startX = e.clientX
      startWidth = target.getBoundingClientRect().width
      document.body.classList.add('is-resizing')
      resizer.classList.add('active')

      const handleMouseMove = (e: MouseEvent) => {
        if (!isResizing) return

        const diff = reverse ? startX - e.clientX : e.clientX - startX
        const newWidth = Math.max(200, Math.min(800, startWidth + diff))
        target.style.width = `${newWidth}px`
      }

      const handleMouseUp = () => {
        isResizing = false
        document.body.classList.remove('is-resizing')
        resizer.classList.remove('active')
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }

      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
    })
  }

  /**
   * 设置垂直调整器（上下调整）
   */
  const setupVerticalResizer = (
    resizer: HTMLElement,
    target: HTMLElement
  ) => {
    let isResizing = false
    let startY = 0
    let startHeight = 0

    resizer.addEventListener('mousedown', (e) => {
      isResizing = true
      startY = e.clientY
      startHeight = target.getBoundingClientRect().height
      document.body.classList.add('is-resizing')
      resizer.classList.add('active')

      const handleMouseMove = (e: MouseEvent) => {
        if (!isResizing) return

        const diff = startY - e.clientY
        const newHeight = Math.max(100, Math.min(600, startHeight + diff))
        target.style.height = `${newHeight}px`
      }

      const handleMouseUp = () => {
        isResizing = false
        document.body.classList.remove('is-resizing')
        resizer.classList.remove('active')
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }

      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
    })
  }

  return {
    setupResizers
  }
}

