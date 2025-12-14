import { ref } from 'vue'
import type { Ref } from 'vue'

/**
 * 标签页管理组合式函数
 */
export function useTabs(tabBarRef: Ref<HTMLElement | undefined>) {
  const tabs = ref([
    { id: 'console.sql', name: 'console.sql', type: 'file', icon: 'fa-file-code', active: true }
  ])

  /**
   * 渲染标签页
   */
  const renderTabs = () => {
    if (!tabBarRef.value) return

    tabBarRef.value.innerHTML = ''
    tabs.value.forEach((tab, index) => {
      const tabElement = document.createElement('div')
      tabElement.className = `px-3 py-1 text-xs cursor-pointer border-r theme-border ${
        tab.active ? 'tab-active theme-text-primary' : 'theme-text-secondary hover:text-white'
      }`
      tabElement.innerHTML = `<i class="fa-solid ${tab.icon} mr-1"></i>${tab.name}`
      
      tabElement.addEventListener('click', () => {
        tabs.value.forEach(t => t.active = false)
        tab.active = true
        renderTabs()
      })

      tabBarRef.value!.appendChild(tabElement)
    })
  }

  return {
    tabs,
    renderTabs
  }
}

